package com.docmgmt.transformer;

/**
 * Exception thrown when content transformation fails
 */
public class TransformationException extends Exception {
    
    public TransformationException(String message) {
        super(message);
    }
    
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
