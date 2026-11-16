package data_access;

import java.io.IOException;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import use_case.upload_notes.PdfTextReader;

public final class PdfBoxTextReader implements PdfTextReader {
    @Override
    public String read(Path file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            if (!document.getCurrentAccessPermission().canExtractContent()) {
                throw new IOException("Text extraction is restricted.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (InvalidPasswordException e) {
            throw new IOException("Choose a PDF without password protection.", e);
        } catch (IOException e) {
            throw new IOException("Unable to extract text. Choose a readable, unrestricted PDF.", e);
        }
    }
}
