package com.supos.uns.filter;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.*;

public class BodyReaderHttpServletResponseWrapper extends HttpServletResponseWrapper {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ServletOutputStream outputStream;
    private PrintWriter printWriter;
    private ServletOutputStreamBackup backup;

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response The response to be wrapped
     * @throws IllegalArgumentException if the response is null
     */
    public BodyReaderHttpServletResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
    }

    @Override
    public void setContentType(String type) {
        super.setContentType(type);
        try {
            ((ServletOutputStreamBackup)getOutputStream()).setNeedBackUp(type == null || type.contains("application/json"));
        } catch (IOException e) {
            log.error("发生异常",e);
        }
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (printWriter != null) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }

        if (outputStream == null) {
            outputStream = getResponse().getOutputStream();
            backup = new ServletOutputStreamBackup(outputStream);
        }

        return backup;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
//        if (outputStream != null) {
//            throw new IllegalStateException("getOutputStream() has already been called on this response.");
//        }

        if (printWriter == null) {
            backup = new ServletOutputStreamBackup(getResponse().getOutputStream());
            printWriter = new PrintWriter(new OutputStreamWriter(backup, getResponse().getCharacterEncoding()), true);
        }

        return printWriter;
    }

    @Override
    public void flushBuffer() throws IOException {

        if (printWriter != null){
            printWriter.flush();
        }
        if (outputStream != null){
            outputStream.flush();
        }
    }

    public byte[] getBackup() {
        if (backup != null) {
            return backup.getBackup();
        } else {
            return new byte[0];
        }
    }

    public byte[] getResponseData() throws IOException {
        return getBackup();
    }


    public String getContent() throws IOException {
        String contentType = getResponse().getContentType();
        if (contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            byte[] b = getResponseData();
            if (b != null) {
                return StrUtil.str(b, CharsetUtil.CHARSET_UTF_8);
            }
        }
        return null;
    }


    private static class ServletOutputStreamBackup extends ServletOutputStream{

        private boolean needBackUp = true;

        private final OutputStream outputStream;

        private final ByteArrayOutputStream buckup;

        public ServletOutputStreamBackup(OutputStream outputStream) {
            this.outputStream = outputStream;
            buckup = new ByteArrayOutputStream(1024);
        }

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
            if (needBackUp){
                buckup.write(b);
            }
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener listener) {

        }

        public byte[] getBackup(){
            return buckup.toByteArray();
        }

        public void setNeedBackUp(boolean needBackUp) {
            this.needBackUp = needBackUp;
        }
    }
}
