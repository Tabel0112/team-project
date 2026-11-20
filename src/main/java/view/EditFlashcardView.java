package view;

import interface_adapter.delete_flashcard.DeleteFlashcardController;
import interface_adapter.edit_flashcard.EditFlashcardController;
import interface_adapter.edit_flashcard.EditFlashcardState;
import interface_adapter.edit_flashcard.EditFlashcardViewModel;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Supplier;

/**
 * The View with the form for editing a flashcard in the open study set. The
 * flashcard can also be deleted from here, after the user confirms.
 */
public class EditFlashcardView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = EditFlashcardViewModel.VIEW_NAME;

  private final EditFlashcardViewModel viewModel;
  private final JTextArea questionArea = new JTextArea(4, 40);
  private final JTextArea answerArea = new JTextArea(4, 40);
  private final JLabel errorLabel = new JLabel(" ");
  private EditFlashcardController controller;
  private DeleteFlashcardController deleteController;
  private String originalQuestion = "";
  private String originalAnswer = "";

  /**
   * Builds the view and connects its buttons.
   *
   * <p>The controllers are supplied afterwards with {@link #setController} and
   * {@link #setDeleteController}.
   *
   * @param viewModel         the view model to listen to
   * @param currentStudySetId supplies the id of the open study set
   * @param onCancel          run when the user clicks Cancel
   */
  public EditFlashcardView(EditFlashcardViewModel viewModel,
                           Supplier<String> currentStudySetId,
                           Runnable onCancel) {
    this.viewModel = viewModel;
    viewModel.addPropertyChangeListener(this);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    questionArea.setLineWrap(true);
    questionArea.setWrapStyleWord(true);
    answerArea.setLineWrap(true);
    answerArea.setWrapStyleWord(true);
    errorLabel.setForeground(Color.RED);

    final JButton save = new JButton("Apply changes locally");
    final JButton cancel = new JButton("Cancel");
    final JButton delete = new JButton("Delete");
    final JPanel buttons = new JPanel();
    buttons.add(save);
    buttons.add(cancel);
    buttons.add(delete);

    add(new JLabel("Question (front of the card):"));
    add(new JScrollPane(questionArea));
    add(new JLabel("Answer (back of the card):"));
    add(new JScrollPane(answerArea));
    add(errorLabel);
    add(buttons);

    save.addActionListener(e -> {
      final EditFlashcardState state = viewModel.getState();
      state.setQuestion(questionArea.getText());
      state.setAnswer(answerArea.getText());
      controller.execute(currentStudySetId.get(), state.getCardIndex(),
          state.getQuestion(), state.getAnswer());
    });
    cancel.addActionListener(e -> {          // extension 4a: the flashcard is unchanged
      viewModel.getState().clear();
      viewModel.firePropertyChanged();
      onCancel.run();
    });
    delete.addActionListener(e -> {          // extension 3a: delete after confirming
      final int choice = JOptionPane.showConfirmDialog(this,
          "Delete this flashcard? This cannot be undone.", "Delete Flashcard",
          JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      if (choice == JOptionPane.YES_OPTION) {
        deleteController.execute(currentStudySetId.get(), viewModel.getState().getCardIndex());
      }
    });
  }

  /** Includes unsubmitted edits, including clearing a previously nonempty field. */
  public boolean hasUnsubmittedInput() {
    return !questionArea.getText().equals(originalQuestion) || !answerArea.getText().equals(originalAnswer);
  }

  /**
   * Sets the controller that handles the apply button.
   *
   * @param controller the controller to call when the user applies changes locally
   */
  public void setController(EditFlashcardController controller) {
    this.controller = controller;
  }

  /**
   * Sets the controller that handles the Delete button.
   *
   * @param deleteController the controller to call when the user confirms a deletion
   */
  public void setDeleteController(DeleteFlashcardController deleteController) {
    this.deleteController = deleteController;
  }

  /**
   * Shows the form's text and any error from the current state.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final EditFlashcardState state = viewModel.getState();
    if (state.getErrorMessage() == null) {
      originalQuestion = state.getQuestion();
      originalAnswer = state.getAnswer();
    }
    questionArea.setText(state.getQuestion());
    answerArea.setText(state.getAnswer());
    errorLabel.setText(state.getErrorMessage() == null ? " " : state.getErrorMessage());
  }
}
