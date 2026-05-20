package com.jobskillsmatcher.cv.impl;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;


@Component
public class CvTextExtractor {

    private static final int MAX_CHARS = 1_000_000;

    public String extract(InputStream input) {
        try {
            BodyContentHandler handler = new BodyContentHandler(MAX_CHARS);
            Metadata metadata = new Metadata();
            new AutoDetectParser().parse(input, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (IOException | SAXException | TikaException ex) {
            throw new CvParseException("Failed to parse CV: " + ex.getMessage(), ex);
        }
    }

    public static class CvParseException extends RuntimeException {
        public CvParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
