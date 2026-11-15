package view;

import interface_adapter.add_flashcard.AddFlashcardController;
import interface_adapter.add_flashcard.AddFlashcardState;
import interface_adapter.add_flashcard.AddFlashcardViewModel;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Supplier;

/**
 * The View with the form for adding a flashcard to the open study set.
 */
public class AddFlashcardView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = AddFlashcardViewModel.VIEW_NAME;

  private final AddFlashcardViewModel viewModel;
  private final JTextArea questionArea = new JTextArea(4, 40);
  private final JTextArea answerArea = new JTextArea(4, 40);
  private final JLabel errorLabel = new JLabel(" ");
  private AddFlashcardController controller;

  /**
   * Builds the view and connects its buttons.
   *
   * <p>The controller is supplied afterwards with {@link #setController}.
   *
   * @param viewModel         the view model to listen to
   * @param currentStudySetId supplies the id of the open study set
   * @param onCancel          run when the user clicks Cancel
   */
  public AddFlashcardView(AddFlashcardViewModel viewModel,
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

    final JButton add = new JButton("Add");
    final JButton cancel = new JButton("Cancel");
    final JPanel buttons = new JPanel();
    buttons.add(add);
    buttons.add(cancel);

    add(new JLabel("Question (front of the card):"));
    add(new JScrollPane(questionArea));
    add(new JLabel("Answer (back of the card):"));
    add(new JScrollPane(answerArea));
    add(errorLabel);
    add(buttons);

    add.addActionListener(e -> {
      final AddFlashcardState state = viewModel.getState();
      state.setQuestion(questionArea.getText());
      state.setAnswer(answerArea.getText());
      controller.execute(currentStudySetId.get(), state.getQuestion(), state.getAnswer());
    });
    cancel.addActionListener(e -> {          // extension 4a: nothing is added
      viewModel.getState().clear();
      viewModel.firePropertyChanged();
      onCancel.run();
    });
  }

  /**
   * Sets the controller that handles the Add button.
   *
   * @param controller the controller to call when the user clicks Add
   */
  public void setController(AddFlashcardController controller) {
    this.controller = controller;
  }

  /**
   * Shows the form's text and any error from the current state.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final AddFlashcardState state = viewModel.getState();
    questionArea.setText(state.getQuestion());
    answerArea.setText(state.getAnswer());
    errorLabel.setText(state.getErrorMessage() == null ? " " : state.getErrorMessage());
  }
}
