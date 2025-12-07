package com.docmgmt.transformer;

import com.docmgmt.model.Content;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registry for managing content transformer plugins.
 * Automatically discovers and registers all ContentTransformer beans.
 */
@Component
public class TransformerRegistry {
    
    private final List<ContentTransformer> transformers;
    
    /**
     * Constructor that auto-wires all ContentTransformer implementations
     * @param transformers list of all ContentTransformer beans in the application context
     */
    public TransformerRegistry(List<ContentTransformer> transformers) {
        this.transformers = transformers != null ? new ArrayList<>(transformers) : new ArrayList<>();
    }
    
    /**
     * Register a transformer manually (useful for testing)
     * @param transformer the transformer to register
     */
    public void registerTransformer(ContentTransformer transformer) {
        if (transformer != null && !transformers.contains(transformer)) {
            transformers.add(transformer);
        }
    }
    
    /**
     * Unregister a transformer
     * @param transformer the transformer to unregister
     */
    public void unregisterTransformer(ContentTransformer transformer) {
        transformers.remove(transformer);
    }
    
    /**
     * Find a transformer that can handle the given content
     * @param content the content to transform
     * @return an Optional containing the transformer if found, empty otherwise
     */
    public Optional<ContentTransformer> findTransformer(Content content) {
        return transformers.stream()
                .filter(t -> t.canTransform(content))
                .findFirst();
    }
    
    /**
     * Find a transformer by source and target content types
     * @param sourceType the source content type
     * @param targetType the target content type
     * @return an Optional containing the transformer if found, empty otherwise
     */
    public Optional<ContentTransformer> findTransformer(String sourceType, String targetType) {
        return transformers.stream()
                .filter(t -> t.getSourceContentType().equalsIgnoreCase(sourceType) &&
                            t.getTargetContentType().equalsIgnoreCase(targetType))
                .findFirst();
    }
    
    /**
     * Get all registered transformers
     * @return a list of all registered transformers
     */
    public List<ContentTransformer> getAllTransformers() {
        return new ArrayList<>(transformers);
    }
    
    /**
     * Get the number of registered transformers
     * @return the count of registered transformers
     */
    public int getTransformerCount() {
        return transformers.size();
    }
}
