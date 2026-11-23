package view;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A clickable flashcard that shows one side at a time: the question or the answer.
 */
public class FlashcardPanel extends JPanel {
  private static final Color QUESTION_COLOR = new Color(255, 255, 255);
  private static final Color ANSWER_COLOR = new Color(232, 244, 255);

  private final JLabel sideLabel = new JLabel(" ", SwingConstants.CENTER);
  private final JLabel textLabel = new JLabel(" ", SwingConstants.CENTER);

  /**
   * Builds an empty card.
   *
   * @param onClick run when the user clicks anywhere on the card
   */
  public FlashcardPanel(Runnable onClick) {
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(520, 280));
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.GRAY, 2, true),
        BorderFactory.createEmptyBorder(16, 24, 16, 24)));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    sideLabel.setFont(sideLabel.getFont().deriveFont(Font.BOLD, 13f));
    sideLabel.setForeground(Color.GRAY);
    textLabel.setFont(textLabel.getFont().deriveFont(20f));

    add(sideLabel, BorderLayout.NORTH);
    add(textLabel, BorderLayout.CENTER);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        onClick.run();
      }
    });
  }

  /**
   * Shows one side of a flashcard.
   *
   * @param text          the question or answer to show
   * @param answerShowing true to show it as the answer side, false as the question side
   */
  public void showSide(String text, boolean answerShowing) {
    sideLabel.setText(answerShowing ? "ANSWER" : "QUESTION");
    setBackground(answerShowing ? ANSWER_COLOR : QUESTION_COLOR);
    // HTML lets long text wrap inside the card; escaping keeps symbols like < as typed.
    textLabel.setText("<html><div style='text-align:center'>" + escapeHtml(text) + "</div></html>");
  }

  private static String escapeHtml(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\n", "<br>");
  }
}
