package view;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import use_case.upload_notes.UploadNotesInteractor;

public final class UploadNotesPanel extends JPanel {
    private final JButton choose = new JButton("Choose PDF");
    private final JLabel status = new JLabel(" ");
    private final JTextArea preview = new JTextArea();

    public UploadNotesPanel(UploadNotesInteractor uploadNotes) {
        super(new BorderLayout());
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
        status.setText("Reading " + file.getFileName() + "...");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return uploadNotes.execute(file);
            }

            @Override
            protected void done() {
                try {
                    preview.setText(get());
                    preview.setCaretPosition(0);
                    status.setText("Extracted text from " + file.getFileName());
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
