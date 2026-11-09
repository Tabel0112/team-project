package org.example.studyapp.app;

import javax.swing.JFrame;
import org.example.studyapp.ui.StudyAppPanel;
import java.awt.Dimension;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Starts the Study App desktop application. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowWindow);
    }

    private static void createAndShowWindow() {
        setSystemLookAndFeel();

        JFrame frame = new JFrame("Study App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new StudyAppPanel());
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setSize(1100, 720);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException exception) {
            // The default Swing look and feel remains available.
        }
    }
}
