package org.example.studyapp.ui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/** Empty pages for the initial frontend structure. */
public final class StudyAppPanel extends JPanel {
    public StudyAppPanel() {
        super(new BorderLayout());
        JTabbedPane pages = new JTabbedPane();
        pages.addTab("Study sets", new JPanel());
        pages.addTab("Upload PDF", new JPanel());
        pages.addTab("Review", new JPanel());
        add(pages, BorderLayout.CENTER);
    }
}
