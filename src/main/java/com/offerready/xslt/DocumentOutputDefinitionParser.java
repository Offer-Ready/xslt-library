package com.offerready.xslt;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import lombok.val;
import org.w3c.dom.Element;

import com.databasesandlife.util.gwtsafe.ConfigurationException;
import com.offerready.xslt.config.OfferReadyDomParser;
import com.offerready.xslt.DocumentOutputDefinition.OutputConversion;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;


/**
 * Parsers which parses document-definition files for any XSLT-style document generation application.
 */
public class DocumentOutputDefinitionParser extends OfferReadyDomParser {

    public static @CheckForNull String parseOptionalDownloadFilename(@Nonnull Element outputDefnElement) throws ConfigurationException {
        String downloadFilename = null;
        val downloadFilenameElement = getOptionalSingleSubElement(outputDefnElement, "download-filename");
        if (downloadFilenameElement != null) {
            downloadFilename = downloadFilenameElement.getTextContent();
            if ( ! downloadFilename.matches("[\\w\\.\\-]+"))
                throw new ConfigurationException("<download-filename>" + downloadFilename + "</download-filename> is invalid: " +
                    "only A-Z, a-z, 0-9, '_', '-', '.' are allowed, due to problems with inexact HTTP specification " +
                    "w.r.t. character sets and download file names");
        }
        return downloadFilename;
    }
    
    /**
     * @param templateContainerDirectory the directory which will contain "xyz/report.xslt" for "xslt-directory"="xyz"
     */
    protected static DocumentOutputDefinition parseOutputDefinition(
        @Nonnull File templateContainerDirectory, @Nonnull Element outputDefnElement
    ) throws ConfigurationException {
        // xslt-directory is legacy, prefer xslt-file
        assertNoOtherElements(outputDefnElement, 
            "xslt-file", "xslt-directory", "placeholder-value", "convert-output-xml-to-json",
            "convert-output-xml-fo-to-pdf", "convert-output-xsl-fo-to-pdf", "convert-output-xml-to-excel", 
            "content-type", "download-filename");
        
        final File xsltFileOrNull;
        val xsltFileEl = getOptionalSingleSubElement(outputDefnElement, "xslt-file");
        val xsltDirEl = getOptionalSingleSubElement(outputDefnElement, "xslt-directory");
        if (xsltFileEl != null) {
            xsltFileOrNull = new File(templateContainerDirectory, getMandatoryAttribute(xsltFileEl, "name"));
            if ( ! xsltFileOrNull.isFile()) throw new ConfigurationException("XSLT File '" + xsltFileOrNull + "' not found");
        } else if (xsltDirEl != null) {
            File dir = new File(templateContainerDirectory, getMandatoryAttribute(xsltDirEl, "name"));
            xsltFileOrNull = new File(dir, "report.xslt");
            if ( ! xsltFileOrNull.isFile()) throw new ConfigurationException("XSLT File '" + xsltFileOrNull + "' not found");
        } else xsltFileOrNull = null;
        
        val placeholderValues = new HashMap<String, String>();
        for (Element p : getSubElements(outputDefnElement, "placeholder-value")) {
            String key = getMandatoryAttribute(p, "placeholder-name");
            String value = getMandatoryAttribute(p, "value");
            placeholderValues.put(key, value);
        }

        String contentType = null;
        val contentTypeElement = getOptionalSingleSubElement(outputDefnElement, "content-type");
        if (contentTypeElement != null) contentType = getMandatoryAttribute(contentTypeElement, "type");
        
        val result = new DocumentOutputDefinition(placeholderValues);
        result.xsltFileOrNull = xsltFileOrNull;
        result.outputConversion =
            getSubElements(outputDefnElement, "convert-output-xml-to-json").size() > 0 ? OutputConversion.xmlToJson :
            getSubElements(outputDefnElement, "convert-output-xml-fo-to-pdf").size() > 0 ? OutputConversion.xslFoToPdf :  // deprecated
            getSubElements(outputDefnElement, "convert-output-xsl-fo-to-pdf").size() > 0 ? OutputConversion.xslFoToPdf :
            getSubElements(outputDefnElement, "convert-output-xml-to-excel").size()  > 0 ? 
                ( Boolean.parseBoolean(getOptionalAttribute(getMandatorySingleSubElement(outputDefnElement, "convert-output-xml-to-excel"), "magic-numbers")) ? 
                    OutputConversion.excelXmlToExcelBinaryMagicNumbers : OutputConversion.excelXmlToExcelBinary ) :
            OutputConversion.none;
        result.contentType = contentType;

        return result;
    }
}


