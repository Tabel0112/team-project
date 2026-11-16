package view;

import data_access.GeminiQuestions;
import entity.Flashcard;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.BiConsumer;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import use_case.upload_notes.UploadNotesInteractor;

public final class UploadNotesPanel extends JPanel {
    private final JButton choose = new JButton("Generate questions from PDF");
    private final JLabel status = new JLabel(" ");
    private final JTextArea preview = new JTextArea();

    private final BiConsumer<String, List<Flashcard>> onGenerated;

    public UploadNotesPanel(UploadNotesInteractor uploadNotes, BiConsumer<String, List<Flashcard>> onGenerated) {
        super(new BorderLayout());
        this.onGenerated = onGenerated;
        JPanel controls = new JPanel();
        controls.add(new JLabel("PDF with selectable text, up to " + UploadNotesInteractor.MAX_MIB + " MiB"));
        controls.add(choose);
        add(controls, BorderLayout.NORTH);
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        add(new JScrollPane(preview), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        choose.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PDF documents", "pdf"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                read(uploadNotes, chooser.getSelectedFile().toPath());
            }
        });
    }

    private void read(UploadNotesInteractor uploadNotes, Path file) {
        choose.setEnabled(false);
        preview.setText("");
        status.setText("Generating questions with Gemini...");
        new SwingWorker<List<Flashcard>, Void>() {
            @Override
            protected List<Flashcard> doInBackground() throws Exception {
                return new GeminiQuestions().generate(uploadNotes.execute(file));
            }

            @Override
            protected void done() {
                try {
                    List<Flashcard> cards = get();
                    StringBuilder display = new StringBuilder();
                    for (int i = 0; i < cards.size(); i++) {
                        display.append(i + 1).append(". ").append(cards.get(i).getQuestion()).append("\n\n");
                    }
                    preview.setText(display.toString());
                    String filename = file.getFileName().toString();
                    String title = filename.substring(0, filename.length() - 4).strip();
                    onGenerated.accept(title.isEmpty() ? "Uploaded notes" : title, cards);
                    preview.setCaretPosition(0);
                    status.setText("Questions from " + file.getFileName());
                } catch (ExecutionException e) {
                    status.setText(e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    status.setText("Reading interrupted. Choose the file again.");
                } finally {
                    choose.setEnabled(true);
                }
            }
        }.execute();
    }
}
