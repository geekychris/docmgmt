package com.docmgmt.transformer;

import com.docmgmt.model.Content;

import java.io.IOException;

/**
 * Interface for content transformation plugins.
 * Implementations can convert content from one format to another (e.g., PDF to text).
 */
public interface ContentTransformer {
    
    /**
     * Get the source content type this transformer supports (e.g., "application/pdf")
     * @return the MIME type of content this transformer can process
     */
    String getSourceContentType();
    
    /**
     * Get the target content type this transformer produces (e.g., "text/plain")
     * @return the MIME type of the transformed content
     */
    String getTargetContentType();
    
    /**
     * Checks if this transformer can handle the given content
     * @param content the content to check
     * @return true if this transformer can transform the content
     */
    default boolean canTransform(Content content) {
        return content != null && 
               content.getContentType() != null && 
               content.getContentType().equalsIgnoreCase(getSourceContentType());
    }
    
    /**
     * Transform the content from source format to target format
     * @param sourceContent the content to transform
     * @return the transformed content as a byte array
     * @throws IOException if there's an error reading or transforming the content
     * @throws TransformationException if the transformation fails
     */
    byte[] transform(Content sourceContent) throws IOException, TransformationException;
    
    /**
     * Get a descriptive name for this transformer
     * @return the transformer name (e.g., "PDF to Text Transformer")
     */
    String getName();
    
    /**
     * Indicates whether the output of this transformer is indexable text
     * @return true if the output is indexable text, false otherwise
     */
    default boolean producesIndexableContent() {
        return getTargetContentType().startsWith("text/");
    }
}
