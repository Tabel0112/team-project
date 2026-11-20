package view;

import interface_adapter.create_study_set.CreateStudySetController;
import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The View for creating a new study set.
 */
public class CreateStudySetView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = "create study set";

  private final CreateStudySetViewModel viewModel;
  private final JTextField nameField = new JTextField(20);
  private final JLabel errorLabel = new JLabel(" ");
  private final JPanel itemsPanel = new JPanel();
  private final Consumer<CreateStudySetState.Item> onOpen;
  private CreateStudySetController controller;

  /**
   * Builds the view and connects its buttons.
   *
   * <p>The controller is supplied afterwards with {@link #setController}.
   *
   * @param viewModel     the view model to listen to
   * @param currentUserId supplies the id of the logged-in user
   * @param onOpen        called with a study set's entry when the user clicks it
   */
  public CreateStudySetView(CreateStudySetViewModel viewModel,
                            Supplier<String> currentUserId,
                            Consumer<CreateStudySetState.Item> onOpen) {
    this.viewModel = viewModel;
    this.onOpen = onOpen;
    viewModel.addPropertyChangeListener(this);

    setLayout(new BorderLayout());
    errorLabel.setForeground(Color.RED);
    nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameField.getPreferredSize().height));

    final JButton create = new JButton("Create");
    final JButton cancel = new JButton("Cancel");
    final JPanel buttons = new JPanel();
    buttons.add(create);
    buttons.add(cancel);

    final JPanel form = new JPanel();
    form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
    form.add(new JLabel("Study set name (e.g. a course or subject):"));
    form.add(nameField);
    form.add(errorLabel);
    form.add(buttons);

    itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
    final JScrollPane scrollPane = new JScrollPane(itemsPanel);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Your study sets"));

    add(form, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);

    create.addActionListener(e ->
        controller.execute(currentUserId.get(), nameField.getText()));
    cancel.addActionListener(e -> {          // extension 3a: nothing is created
      nameField.setText("");
      errorLabel.setText(" ");
    });

    refreshList();
  }

  /**
   * Sets the controller that handles the Create button.
   *
   * @param controller the controller to call when the user clicks Create
   */
  public void setController(CreateStudySetController controller) {
    this.controller = controller;
  }

  /**
   * Updates the error label and the study set panel. After a successful creation,
   * also clears the name field.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final CreateStudySetState state = viewModel.getState();
    errorLabel.setText(state.getErrorMessage() == null ? " " : state.getErrorMessage());
    if (state.getCreatedStudySetId() != null) {
      nameField.setText("");
    }
    refreshList();
  }

  /**
   * Rebuilds the study set panel from the current state.
   */
  private void refreshList() {
    itemsPanel.removeAll();
    if (viewModel.getState().getStudySets().isEmpty()) {
      itemsPanel.add(new JLabel("No study sets yet."));
    }
    for (CreateStudySetState.Item item : viewModel.getState().getStudySets()) {
      final JButton button = new JButton(item.getName());
      button.setAlignmentX(LEFT_ALIGNMENT);
      button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
      button.addActionListener(e -> onOpen.accept(item));
      itemsPanel.add(button);
    }
    itemsPanel.revalidate();
    itemsPanel.repaint();
  }
}