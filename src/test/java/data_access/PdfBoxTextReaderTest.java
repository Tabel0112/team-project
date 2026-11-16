package data_access;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class PdfBoxTextReaderTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final PdfBoxTextReader reader = new PdfBoxTextReader();

    @Test public void readsLinesInVisualOrderWhenWrittenBottomFirst() throws IOException {
        Path file = folder.getRoot().toPath().resolve("layout.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 650);
                stream.showText("Bottom");
                stream.newLineAtOffset(0, 50);
                stream.showText("Top");
                stream.endText();
            }
            document.save(file.toFile());
        }
        assertEquals("Top\nBottom", reader.read(file).strip().replace("\r\n", "\n"));
    }

    @Test public void rejectsPdfRequiringAnOpenPassword() throws IOException {
        Path file = encryptedPdf("reader", true);
        IOException error = assertThrows(IOException.class, () -> reader.read(file));
        assertTrue(error.getMessage(), error.getMessage().contains("password protection"));
    }

    @Test public void rejectsOpenablePdfWithExtractionDisabled() throws IOException {
        Path file = encryptedPdf("", false);
        IOException error = assertThrows(IOException.class, () -> reader.read(file));
        assertTrue(error.getMessage(), error.getMessage().contains("unrestricted PDF"));
    }

    private Path encryptedPdf(String openPassword, boolean allowExtraction) throws IOException {
        Path file = folder.getRoot().toPath().resolve("protected.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            AccessPermission permissions = new AccessPermission();
            permissions.setCanExtractContent(allowExtraction);
            document.protect(new StandardProtectionPolicy("fixture-owner", openPassword, permissions));
            document.save(file.toFile());
        }
        return file;
    }
}
