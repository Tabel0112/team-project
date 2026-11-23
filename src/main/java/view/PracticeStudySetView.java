package view;

import interface_adapter.practice_study_set.PracticeStudySetState;
import interface_adapter.practice_study_set.PracticeStudySetViewModel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * The View for practising a study set: shows one flashcard at a time, which the
 * user clicks to flip, and a Next button to move to the following flashcard.
 */
public class PracticeStudySetView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = PracticeStudySetViewModel.VIEW_NAME;

  private final PracticeStudySetViewModel viewModel;
  private final JLabel titleLabel = new JLabel(" ", SwingConstants.CENTER);
  private final JLabel progressLabel = new JLabel(" ", SwingConstants.CENTER);
  private final JLabel hintLabel = new JLabel(" ", SwingConstants.CENTER);
  private final FlashcardPanel flashcardPanel;
  private final JButton nextButton = new JButton("Next flashcard");

  /**
   * Builds the view.
   *
   * @param viewModel the view model to listen to
   * @param onBack    run when the user clicks Back to study sets
   */
  public PracticeStudySetView(PracticeStudySetViewModel viewModel, Runnable onBack) {
    this.viewModel = viewModel;
    viewModel.addPropertyChangeListener(this);

    flashcardPanel = new FlashcardPanel(() -> {      // steps 3-4: flip to reveal the answer
      viewModel.getState().flip();
      viewModel.firePropertyChanged();
    });
    nextButton.addActionListener(e -> {               // step 6: show the next flashcard
      viewModel.getState().next();
      viewModel.firePropertyChanged();
    });
    final JButton backButton = new JButton("Back to study sets");
    backButton.addActionListener(e -> onBack.run());

    setLayout(new BorderLayout(0, 10));
    setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));

    final JPanel header = new JPanel(new BorderLayout());
    header.add(titleLabel, BorderLayout.NORTH);
    header.add(progressLabel, BorderLayout.SOUTH);

    final JPanel cardHolder = new JPanel(new GridBagLayout());   // keeps the card centred
    cardHolder.add(flashcardPanel);

    final JPanel footer = new JPanel(new BorderLayout());
    footer.add(hintLabel, BorderLayout.NORTH);
    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttons.add(backButton);
    buttons.add(nextButton);
    footer.add(buttons, BorderLayout.SOUTH);

    add(header, BorderLayout.NORTH);
    add(cardHolder, BorderLayout.CENTER);
    add(footer, BorderLayout.SOUTH);
  }

  /**
   * Shows the current flashcard, which side of it is up, and the user's progress.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final PracticeStudySetState state = viewModel.getState();
    final PracticeStudySetState.Item card = state.getCurrentCard();
    if (card == null) {                               // no session, e.g. after logging out
      titleLabel.setText(" ");
      progressLabel.setText(" ");
      return;
    }
    titleLabel.setText(state.getStudySetName());
    progressLabel.setText("Flashcard " + (state.getCurrentIndex() + 1)
        + " of " + state.getCardCount());
    flashcardPanel.showSide(state.isAnswerShowing() ? card.getAnswer() : card.getQuestion(),
        state.isAnswerShowing());
    hintLabel.setText(state.isAnswerShowing()
        ? "Click the card to see the question again."
        : "Click the card to reveal the answer.");
    nextButton.setEnabled(state.hasNext());
    nextButton.setText(state.hasNext() ? "Next flashcard" : "Last flashcard");
  }
}
