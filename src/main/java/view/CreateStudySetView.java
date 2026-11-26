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
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * The View for creating a new study set.
 */
public class CreateStudySetView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = "create study set";
  public static final String ARROW_ENABLED = "arrowEnabled";

  private final CreateStudySetViewModel viewModel;
  private final JTextField nameField = new JTextField(20);
  private final JLabel errorLabel = new JLabel(" ");
  private final JPanel itemsPanel = new JPanel();
  private final Consumer<CreateStudySetState.Item> onOpen;
  private Consumer<CreateStudySetState.Item> onEdit = item -> { };
  private Consumer<CreateStudySetState.Item> onMoveUp = item -> { };
  private Consumer<CreateStudySetState.Item> onMoveDown = item -> { };
  private BiConsumer<CreateStudySetState.Item, Integer> onMove = (item, delta) -> { };
  private final Color rowColor = new JPanel().getBackground();
  private static final Color DROP_COLOR = new Color(200, 220, 255);
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

    create.addActionListener(e -> {
      controller.execute(currentUserId.get(), nameField.getText());
      // The controller runs synchronously, so the state already holds the result.
      final CreateStudySetState state = viewModel.getState();
      if (state.getCreatedStudySetId() != null && state.getErrorMessage() == null) {
        clearInput();
      }
    });
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
   * Sets what happens when the user clicks the Edit button of a study set.
   *
   * @param onEdit called with the study set's entry when its Edit button is clicked
   */
  public void setEditHandler(Consumer<CreateStudySetState.Item> onEdit) {
    this.onEdit = onEdit;
  }

  /**
   * Sets what happens when the user clicks the up or down arrow beside a study set.
   *
   * @param onMoveUp   called with the study set's entry when its up arrow is clicked
   * @param onMoveDown called with the study set's entry when its down arrow is clicked
   */
  public void setMoveHandlers(Consumer<CreateStudySetState.Item> onMoveUp,
                              Consumer<CreateStudySetState.Item> onMoveDown) {
    this.onMoveUp = onMoveUp;
    this.onMoveDown = onMoveDown;
  }

  /**
   * Sets what happens when the user drags a study set to a new position.
   *
   * @param onMove called with the study set's entry and how many places to move it
   *               (negative is up, positive is down)
   */
  public void setMoveHandler(BiConsumer<CreateStudySetState.Item, Integer> onMove) {
    this.onMove = onMove;
  }

  /** Returns the row index under the mouse, clamped to the list. */
  private int rowIndexAt(JComponent source, MouseEvent e) {
    final int y = SwingUtilities.convertPoint(source, e.getPoint(), itemsPanel).y;
    final java.awt.Component[] rows = itemsPanel.getComponents();
    for (int i = 0; i < rows.length; i++) {
      if (y < rows[i].getY() + rows[i].getHeight()) {
        return Math.max(i, 0);
      }
    }
    return rows.length - 1;
  }

  /** Highlights the row a drag would drop onto; pass -1 to clear. */
  private void highlightRow(int index) {
    final java.awt.Component[] rows = itemsPanel.getComponents();
    for (int i = 0; i < rows.length; i++) {
      ((JPanel) rows[i]).setBackground(i == index ? DROP_COLOR : rowColor);
    }
  }

  /**
   * Clears the study set name the user has typed.
   */
  public void clearInput() {
    nameField.setText("");
  }

  /**
   * Updates the error label and the study set panel.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final CreateStudySetState state = viewModel.getState();
    errorLabel.setText(state.getErrorMessage() == null ? " " : state.getErrorMessage());
    refreshList();
  }

  /**
   * Rebuilds the study set panel from the current state.
   */
  private void refreshList() {
    itemsPanel.removeAll();
    final List<CreateStudySetState.Item> studySets = viewModel.getState().getStudySets();
    if (studySets.isEmpty()) {
      itemsPanel.add(new JLabel("No study sets yet."));
    }
    for (int i = 0; i < studySets.size(); i++) {
      final int index = i;
      final CreateStudySetState.Item item = studySets.get(i);

      final JLabel handle = new JLabel("\u2630");
      handle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
      handle.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      handle.setToolTipText("Drag to reorder");
      final MouseAdapter drag = new MouseAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          if (handle.isEnabled()) {
            highlightRow(rowIndexAt(handle, e));
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          if (!handle.isEnabled()) {
            return;
          }
          final int target = rowIndexAt(handle, e);
          highlightRow(-1);
          if (target != index) {
            onMove.accept(item, target - index);
          }
        }
      };
      handle.addMouseListener(drag);
      handle.addMouseMotionListener(drag);

      final JButton button = new JButton(item.getName());
      button.addActionListener(e -> onOpen.accept(item));
      final JButton editButton = new JButton("Edit");
      editButton.addActionListener(e -> onEdit.accept(item));

      final JPanel row = new JPanel(new BorderLayout());
      row.setAlignmentX(LEFT_ALIGNMENT);
      row.add(handle, BorderLayout.WEST);
      row.add(button, BorderLayout.CENTER);
      row.add(editButton, BorderLayout.EAST);
      row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
      itemsPanel.add(row);
    }
    itemsPanel.revalidate();
    itemsPanel.repaint();
  }
}