package use_case.upload_notes;

import data_access.PdfBoxTextReader;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class UploadNotesTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final UploadNotesInteractor upload = new UploadNotesInteractor(new PdfBoxTextReader());

    @Test public void extractsBothPagesInOrder() throws IOException {
        File file = folder.newFile("lecture.PDF");
        try (PDDocument document = new PDDocument()) {
            for (String line : new String[]{"First lecture", "Second lecture"}) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(50, 700);
                    stream.showText(line);
                    stream.endText();
                }
            }
            document.save(file);
        }
        assertEquals("First lecture\nSecond lecture", upload.execute(file.toPath()).replace("\r\n", "\n"));
    }

    @Test public void rejectsInvalidInputs() throws IOException {
        reject(folder.newFile("lecture.txt"), "Choose a PDF");
        File file = folder.newFile("lecture.pdf");
        reject(file, "empty");
        try (RandomAccessFile bytes = new RandomAccessFile(file, "rw")) {
            bytes.writeUTF("Invalid PDF");
            reject(file, "Unable to extract");
            bytes.setLength(UploadNotesInteractor.MAX_MIB * 1024L * 1024 + 1);
        }
        reject(file, "or smaller");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file);
        }
        reject(file, "selectable text");
    }

    private void reject(File file, String message) {
        IOException error = assertThrows(IOException.class, () -> upload.execute(file.toPath()));
        assertTrue(error.getMessage(), error.getMessage().contains(message));
    }
}
