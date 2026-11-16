package view;

import interface_adapter.study_set.StudySetState;
import interface_adapter.study_set.StudySetViewModel;
import interface_adapter.view_flashcards.ViewFlashcardsController;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * The View of the user's study sets.
 */
public class StudySetView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = StudySetViewModel.VIEW_NAME;

  private final StudySetViewModel viewModel;
  private final JLabel nameLabel = new JLabel(" ");
  private final JLabel countLabel = new JLabel(" ");
  private final JLabel messageLabel = new JLabel(" ");
  private ViewFlashcardsController viewFlashcardsController;

  /**
   * Builds the view.
   *
   * @param viewModel      the view model to listen to
   * @param onAddFlashcard run when the user clicks Add Flashcard
   * @param onBack         run when the user clicks Back
   */
  public StudySetView(StudySetViewModel viewModel, Runnable onAddFlashcard, Runnable onBack) {
    this.viewModel = viewModel;
    viewModel.addPropertyChangeListener(this);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 18f));
    messageLabel.setForeground(new Color(0, 128, 0));

    final JButton addFlashcard = new JButton("Add Flashcard");
    addFlashcard.addActionListener(e -> onAddFlashcard.run());

    final JButton viewFlashcards = new JButton("View Flashcards");
    viewFlashcards.addActionListener(
        e -> viewFlashcardsController.execute(viewModel.getState().getStudySetId()));

    final JButton back = new JButton("Back");
    back.addActionListener(e -> onBack.run());

    add(nameLabel);
    add(countLabel);
    add(messageLabel);
    add(addFlashcard);
    add(viewFlashcards);
    add(back);
  }

  /**
   * Sets the controller that handles the View Flashcards button.
   *
   * @param viewFlashcardsController the controller to call when the user clicks View Flashcards
   */
  public void setViewFlashcardsController(ViewFlashcardsController viewFlashcardsController) {
    this.viewFlashcardsController = viewFlashcardsController;
  }

  /**
   * Shows the open study set's name, number of flashcards and any confirmation message.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final StudySetState state = viewModel.getState();
    nameLabel.setText(state.getName());
    countLabel.setText(state.getCardCount() == 1
            ? "1 flashcard" : state.getCardCount() + " flashcards");
    messageLabel.setText(state.getMessage().isEmpty() ? " " : state.getMessage());
  }
}