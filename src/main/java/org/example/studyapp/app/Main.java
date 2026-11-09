package org.example.studyapp.app;

import javax.swing.*;

/** Starts the Java 11 desktop application. */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new AppBuilder().build().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "Study App could not start", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
