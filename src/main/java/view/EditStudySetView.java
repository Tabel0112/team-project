package view;

import interface_adapter.edit_study_set.EditStudySetController;
import interface_adapter.edit_study_set.EditStudySetState;
import interface_adapter.edit_study_set.EditStudySetViewModel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The View for editing a study set: rename it or delete it.
 * After a successful edit it hands control back so the updated list can be shown.
 */
public final class EditStudySetView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = EditStudySetViewModel.VIEW_NAME;

  private final EditStudySetViewModel viewModel;
  private final JLabel titleLabel = new JLabel(" ");
  private final JTextField nameField = new JTextField(20);
  private final JLabel errorLabel = new JLabel(" ");
  private final Runnable onDone;
  private EditStudySetController controller;

  /**
   * Builds the view and connects its buttons.
   *
   * <p>The controller is supplied afterwards with {@link #setController}.
   *
   * @param viewModel     the view model to listen to
   * @param currentUserId supplies the id of the logged-in user
   * @param confirmDelete asked with the study set's name before deleting; returns true if the
   *                      user confirms the deletion
   * @param onDone        run after a successful edit, to show the updated list
   * @param onCancel      run when the user clicks Cancel
   */
  public EditStudySetView(EditStudySetViewModel viewModel,
                          Supplier<String> currentUserId,
                          Predicate<String> confirmDelete,
                          Runnable onDone,
                          Runnable onCancel) {
    this.viewModel = viewModel;
    this.onDone = onDone;
    viewModel.addPropertyChangeListener(this);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
    errorLabel.setForeground(Color.RED);
    nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameField.getPreferredSize().height));

    final JButton rename = new JButton("Rename");
    final JButton delete = new JButton("Delete");
    final JButton cancel = new JButton("Cancel");

    final JPanel otherButtons = new JPanel();
    otherButtons.add(delete);
    otherButtons.add(cancel);

    final JLabel nameLabel = new JLabel("New name:");
    titleLabel.setAlignmentX(LEFT_ALIGNMENT);
    nameLabel.setAlignmentX(LEFT_ALIGNMENT);
    nameField.setAlignmentX(LEFT_ALIGNMENT);
    rename.setAlignmentX(LEFT_ALIGNMENT);
    errorLabel.setAlignmentX(LEFT_ALIGNMENT);
    otherButtons.setAlignmentX(LEFT_ALIGNMENT);

    add(titleLabel);
    add(nameLabel);
    add(nameField);
    add(rename);
    add(errorLabel);
    add(otherButtons);

    rename.addActionListener(e ->
        controller.rename(currentUserId.get(), selectedId(), nameField.getText()));
    delete.addActionListener(e -> {
      if (confirmDelete.test(viewModel.getState().getName())) {
        controller.delete(currentUserId.get(), selectedId());
      }
    });
    cancel.addActionListener(e -> onCancel.run());
  }

  /**
   * Sets the controller that handles the edit buttons.
   *
   * @param controller the controller to call when the user chooses an editing option
   */
  public void setController(EditStudySetController controller) {
    this.controller = controller;
  }

  private String selectedId() {
    return viewModel.getState().getStudySetId();
  }

  /**
   * Shows the study set being edited and any error. When a new study set has just been
   * selected, fills the name field with its name; after a failed edit, keeps what the user
   * typed. After a successful edit, hands control back.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final EditStudySetState state = viewModel.getState();
    titleLabel.setText("Editing: " + state.getName());
    errorLabel.setText(state.getErrorMessage() == null ? " " : state.getErrorMessage());
    if (state.isCompleted()) {
      onDone.run();
    } else if (state.getErrorMessage() == null) {
      nameField.setText(state.getName());
    }
  }
}