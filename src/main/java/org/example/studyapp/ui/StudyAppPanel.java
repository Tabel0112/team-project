package org.example.studyapp.ui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import use_case.upload_notes.UploadNotesInteractor;
import view.UploadNotesPanel;

/** Main navigation for the study app. */
public final class StudyAppPanel extends JPanel {
    public StudyAppPanel(UploadNotesInteractor uploadNotes) {
        super(new BorderLayout());
        JTabbedPane pages = new JTabbedPane();
        pages.addTab("Study sets", new JPanel());
        pages.addTab("Upload PDF", new UploadNotesPanel(uploadNotes));
        pages.addTab("Review", new JPanel());
        add(pages, BorderLayout.CENTER);
    }
}
