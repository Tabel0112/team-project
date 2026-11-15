package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Starts the Flashcards application.
 */
public class Main {
  /**
   * Builds the application and shows its window.
   *
   * @param args unused command-line arguments
   */
  public static void main(String[] args) {
    SwingUtilities.invokeLater(Main::createAndShowWindow);
  }

  private static void createAndShowWindow() {
    setSystemLookAndFeel();

    final AppBuilder appBuilder = new AppBuilder();
    final JFrame application = appBuilder
            .addCreateStudySetView()
            .addStudySetView()
            .addAddFlashcardView()
            .addCreateStudySetUseCase()
            .addAddFlashcardUseCase()
            .build();

    application.setLocationRelativeTo(null);
    application.setVisible(true);
  }

  private static void setSystemLookAndFeel() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (ReflectiveOperationException | UnsupportedLookAndFeelException exception) {
      // The default Swing look and feel remains available.
    }
  }
}