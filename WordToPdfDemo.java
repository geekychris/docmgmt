import com.docmgmt.model.Content;
import com.docmgmt.transformer.impl.WordToPdfTransformer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Standalone demo program showing how to use WordToPdfTransformer.
 * 
 * This can be run independently without Spring context.
 * 
 * Usage:
 *   javac -cp "target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" WordToPdfDemo.java
 *   java -cp ".:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)" WordToPdfDemo
 */
public class WordToPdfDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Word to PDF Transformer Demo ===\n");
            
            // Create a sample Word document
            System.out.println("Creating sample Word document...");
            byte[] wordBytes = createSampleWordDocument();
            System.out.println("✓ Created " + wordBytes.length + " bytes of Word content\n");
            
            // Create Content object
            Content wordContent = new Content();
            wordContent.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            wordContent.setContentBytes(wordBytes);
            
            // Create transformer
            WordToPdfTransformer transformer = new WordToPdfTransformer();
            System.out.println("Transformer: " + transformer.getName());
            System.out.println("Source Type: " + transformer.getSourceContentType());
            System.out.println("Target Type: " + transformer.getTargetContentType());
            System.out.println("Produces Indexable: " + transformer.producesIndexableContent() + "\n");
            
            // Check if can transform
            if (!transformer.canTransform(wordContent)) {
                System.err.println("✗ Transformer cannot handle this content type!");
                System.exit(1);
            }
            System.out.println("✓ Transformer can handle Word content\n");
            
            // Transform
            System.out.println("Transforming Word to PDF...");
            long startTime = System.currentTimeMillis();
            byte[] pdfBytes = transformer.transform(wordContent);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("✓ Transformation complete in " + duration + "ms");
            System.out.println("✓ Generated " + pdfBytes.length + " bytes of PDF content\n");
            
            // Save to file
            String outputFile = "demo-output.pdf";
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(pdfBytes);
            }
            System.out.println("✓ Saved PDF to: " + outputFile);
            System.out.println("\nYou can now open " + outputFile + " to view the result!");
            
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Create a sample Word document with some content
     */
    private static byte[] createSampleWordDocument() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Title
            XWPFParagraph title = document.createParagraph();
            title.createRun().setText("Word to PDF Transformer Demo");
            
            // Introduction
            XWPFParagraph intro = document.createParagraph();
            intro.createRun().setText("This document was created programmatically and converted to PDF.");
            
            // Sample content
            XWPFParagraph content = document.createParagraph();
            content.createRun().setText("The WordToPdfTransformer is a Spring component that converts Microsoft Word documents (.doc and .docx) to PDF format using Apache POI and PDFBox.");
            
            // Features list
            XWPFParagraph features = document.createParagraph();
            features.createRun().setText("Key Features:");
            
            String[] featureList = {
                "- Supports both .doc and .docx formats",
                "- Automatic page breaks and text wrapping",
                "- Handles empty documents gracefully",
                "- Multiple MIME type support",
                "- Comprehensive error handling"
            };
            
            for (String feature : featureList) {
                XWPFParagraph p = document.createParagraph();
                p.createRun().setText(feature);
            }
            
            // Conclusion
            XWPFParagraph conclusion = document.createParagraph();
            conclusion.createRun().setText("\nThis is a basic text-based conversion. Complex formatting, images, and tables are not preserved.");
            
            document.write(out);
            return out.toByteArray();
        }
    }
}
