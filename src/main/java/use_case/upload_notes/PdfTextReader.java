package use_case.upload_notes;

import java.io.IOException;
import java.nio.file.Path;

public interface PdfTextReader {
    String read(Path file) throws IOException;
}
