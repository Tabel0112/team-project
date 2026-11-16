package use_case.upload_notes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class UploadNotesInteractor {
    public static final int MAX_MIB = 10;
    private final PdfTextReader reader;

    public UploadNotesInteractor(PdfTextReader reader) {
        this.reader = reader;
    }

    public String execute(Path file) throws IOException {
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IOException("Choose a readable file.");
        }
        if (!file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IOException("Choose a PDF file.");
        }
        long size = Files.size(file);
        if (size == 0) {
            throw new IOException("The selected file is empty.");
        }
        if (size > MAX_MIB * 1024L * 1024) {
            throw new IOException("Choose a PDF of " + MAX_MIB + " MiB or smaller.");
        }
        String text = reader.read(file).strip();
        if (text.isEmpty()) {
            throw new IOException("No readable text was found. Choose a PDF with selectable text.");
        }
        return text;
    }
}
