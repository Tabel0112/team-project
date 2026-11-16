package org.example.studyapp.ui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import use_case.upload_notes.UploadNotesInteractor;
import app.AppBuilder;

/** Main navigation for the study app. */
public final class StudyAppPanel extends JPanel {
    public StudyAppPanel(UploadNotesInteractor uploadNotes) {
        super(new BorderLayout());
        JTabbedPane pages = new AppBuilder()
                .addCreateStudySetView().addStudySetView().addAddFlashcardView().addViewFlashcardsView()
                .addCreateStudySetUseCase().addAddFlashcardUseCase().addViewFlashcardsUseCase()
                .buildPages(uploadNotes);
        add(pages, BorderLayout.CENTER);
    }
}
