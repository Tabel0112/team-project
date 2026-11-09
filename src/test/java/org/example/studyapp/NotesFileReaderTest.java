package org.example.studyapp;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.example.studyapp.data_access.NotesFileReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class NotesFileReaderTest {
    @TempDir Path directory;
    private final NotesFileReader reader = new NotesFileReader();

    @Test void extractsPdfText() throws Exception {
        Path file = directory.resolve("notes.PDF");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Photosynthesis converts light into chemical energy.");
                stream.endText();
            }
            document.save(file.toFile());
        }
        assertTrue(reader.read(file).contains("Photosynthesis converts light into chemical energy."));
    }

    @Test void rejectsEmptyAndMalformedPdfs() throws Exception {
        Path file = directory.resolve("empty.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(file.toFile());
        }
        assertTrue(assertThrows(IOException.class, () -> reader.read(file)).getMessage().contains("No readable text"));
        Files.writeString(file, "not a pdf");
        assertThrows(IOException.class, () -> reader.read(file));
    }

    @Test void preservesUtf8AndRejectsUnsupportedOrOversizedNotes() throws Exception {
        Path file = directory.resolve("notes.md");
        Files.writeString(file, "# Étudier\nQuestion and answer");
        assertEquals("# Étudier\nQuestion and answer", reader.read(file));
        Files.writeString(file, "x".repeat(100001));
        assertThrows(IOException.class, () -> reader.read(file));
        Files.writeString(file, "x".repeat(400001));
        assertThrows(IOException.class, () -> reader.read(file));
        assertThrows(IOException.class, () -> reader.read(directory.resolve("notes.docx")));
    }
}
