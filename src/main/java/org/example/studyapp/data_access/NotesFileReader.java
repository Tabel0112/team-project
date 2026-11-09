package org.example.studyapp.data_access;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.example.studyapp.use_case.generate.NotesReader;

public final class NotesFileReader implements NotesReader {
    @Override public String read(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        boolean pdf = name.endsWith(".pdf");
        if (!pdf && !name.endsWith(".txt") && !name.endsWith(".md"))
            throw new IOException("Choose a PDF, .txt, or .md file.");
        if (Files.size(path) > (pdf ? 10_000_000 : 400_000))
            throw new IOException(pdf ? "Choose a PDF smaller than 10 MB." : "Choose a text file smaller than 400 KB.");
        String text;
        if (pdf) {
            try (PDDocument document = Loader.loadPDF(path.toFile())) {
                if (!document.getCurrentAccessPermission().canExtractContent())
                    throw new IOException("This PDF does not allow text extraction. Choose an unrestricted copy.");
                text = new PDFTextStripper().getText(document);
            } catch (InvalidPasswordException e) {
                throw new IOException("This PDF requires a password. Import an unlocked copy.", e);
            }
        } else {
            text = Files.readString(path);
        }
        // shortcut: text extraction only; add OCR when scanned course notes are required.
        if (text.isBlank())
            throw new IOException("No readable text found. For scanned PDFs, use a text-based PDF or paste your notes.");
        if (text.length() > 100_000)
            throw new IOException("Use a shorter document with at most 100,000 characters of notes.");
        return text;
    }
}
