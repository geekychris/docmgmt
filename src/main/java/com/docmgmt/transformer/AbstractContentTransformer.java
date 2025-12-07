package com.docmgmt.transformer;

import com.docmgmt.model.Content;

import java.io.IOException;

/**
 * Abstract base class for content transformers that provides common functionality
 */
public abstract class AbstractContentTransformer implements ContentTransformer {
    
    private final String sourceContentType;
    private final String targetContentType;
    private final String name;
    
    protected AbstractContentTransformer(String sourceContentType, String targetContentType, String name) {
        this.sourceContentType = sourceContentType;
        this.targetContentType = targetContentType;
        this.name = name;
    }
    
    @Override
    public String getSourceContentType() {
        return sourceContentType;
    }
    
    @Override
    public String getTargetContentType() {
        return targetContentType;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Validate that the content can be transformed
     * @param content the content to validate
     * @throws TransformationException if the content is invalid
     */
    protected void validateContent(Content content) throws TransformationException {
        if (content == null) {
            throw new TransformationException("Content cannot be null");
        }
        if (content.getContentType() == null) {
            throw new TransformationException("Content type cannot be null");
        }
        if (!canTransform(content)) {
            throw new TransformationException(
                String.format("Cannot transform content of type '%s' with transformer '%s'", 
                    content.getContentType(), getName())
            );
        }
    }
    
    /**
     * Get the content bytes from the content object
     * @param content the content to read
     * @return the content bytes
     * @throws IOException if there's an error reading the content
     * @throws TransformationException if the content is empty
     */
    protected byte[] getContentBytes(Content content) throws IOException, TransformationException {
        byte[] bytes = content.getContentBytes();
        if (bytes == null || bytes.length == 0) {
            throw new TransformationException("Content is empty");
        }
        return bytes;
    }
}
