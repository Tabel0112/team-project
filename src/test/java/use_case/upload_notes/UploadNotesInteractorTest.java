package use_case.upload_notes;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class UploadNotesInteractorTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();
    private final UploadNotesInteractor validating = new UploadNotesInteractor(file -> {
        throw new AssertionError("Invalid file reached the reader: " + file);
    });

    @Test public void rejectsMissingFileBeforeReading() {
        reject(folder.getRoot().toPath().resolve("missing.pdf"), "readable file");
    }

    @Test public void rejectsDirectoryBeforeReading() throws IOException {
        reject(folder.newFolder("directory.pdf").toPath(), "readable file");
    }

    @Test public void rejectsWrongExtensionBeforeReading() throws IOException {
        reject(Files.writeString(folder.newFile("lecture.pdf.txt").toPath(), "notes"), "PDF file");
    }

    @Test public void rejectsEmptyFileBeforeReading() throws IOException {
        reject(folder.newFile("empty.pdf").toPath(), "empty");
    }

    @Test public void rejectsOneByteAboveSizeLimitBeforeReading() throws IOException {
        reject(sizedPdf(UploadNotesInteractor.MAX_MIB * 1024L * 1024 + 1), "or smaller");
    }

    @Test public void acceptsExactSizeLimitAndMixedCaseExtension() throws IOException {
        Path file = sizedPdf(UploadNotesInteractor.MAX_MIB * 1024L * 1024);
        UploadNotesInteractor upload = new UploadNotesInteractor(path -> {
            assertEquals(file, path);
            return "  First\nSecond  ";
        });
        assertEquals("First\nSecond", upload.execute(file));
    }

    @Test public void rejectsWhitespaceOnlyText() throws IOException {
        Path file = sizedPdf(1);
        UploadNotesInteractor upload = new UploadNotesInteractor(path -> "\r\n \t");
        IOException error = assertThrows(IOException.class, () -> upload.execute(file));
        assertTrue(error.getMessage(), error.getMessage().contains("selectable text"));
    }

    private Path sizedPdf(long size) throws IOException {
        Path file = folder.newFile("lecture.PdF").toPath();
        try (RandomAccessFile bytes = new RandomAccessFile(file.toFile(), "rw")) {
            bytes.setLength(size);
        }
        return file;
    }

    private void reject(Path file, String message) {
        IOException error = assertThrows(IOException.class, () -> validating.execute(file));
        assertTrue(error.getMessage(), error.getMessage().contains(message));
    }
}
