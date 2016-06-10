package com.offerready.xslt;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class BuffereredHttpResponseDocumentGenerationDestination extends BufferedDocumentGenerationDestination {
    
    int statusCode = HttpServletResponse.SC_OK;
    
    public void setStatusCode(int r) { statusCode = r; }
    
    @Override
    public void setContentDispositionToDownload(String filename) {
        if ( ! filename.matches("[\\w\\.\\-]+")) throw new RuntimeException("Filename '" + filename + "' invalid");
        super.setContentDispositionToDownload(filename);
    }
    
    public void deliver(HttpServletResponse response) {
        try {
            response.setStatus(statusCode);
            response.setContentType(contentType);
    
            if (filenameOrNull != null) response.setHeader("content-disposition", "attachment; filename=\"" + filenameOrNull + "\"");
    
            IOUtils.write(body.toByteArray(), response.getOutputStream());
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }
}