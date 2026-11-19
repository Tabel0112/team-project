package view;

import interface_adapter.reorder_flashcards.ReorderFlashcardsController;
import interface_adapter.view_flashcards.ViewFlashcardsState;
import interface_adapter.view_flashcards.ViewFlashcardsViewModel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * A pop-up window listing the questions in the open study set. Clicking a
 * question shows that flashcard's question and answer, Edit opens it for editing,
 * and Move Up / Move Down change its position in the study set.
 */
public class ViewFlashcardsView extends JPanel implements PropertyChangeListener {
  private final ViewFlashcardsViewModel viewModel;
  private final Component parent;
  private final DefaultListModel<ViewFlashcardsState.Item> listModel = new DefaultListModel<>();
  private final JList<ViewFlashcardsState.Item> questionList = new JList<>(listModel);
  private final JLabel hintLabel = new JLabel(" ");
  private final JTextArea questionArea = createCardArea();
  private final JTextArea answerArea = createCardArea();
  private final JButton editButton = new JButton("Edit");
  private final JButton moveUpButton = new JButton("Move Up");
  private final JButton moveDownButton = new JButton("Move Down");
  private ReorderFlashcardsController reorderController;
  private JDialog dialog;

  /**
   * Builds the view.
   *
   * @param viewModel the view model to listen to
   * @param parent    the component the pop-up is shown over
   * @param onEdit    run with the selected flashcard and its position when the user clicks Edit
   * @param currentStudySetId supplies the id of the open study set
   */
  public ViewFlashcardsView(ViewFlashcardsViewModel viewModel, Component parent,
                            ObjIntConsumer<ViewFlashcardsState.Item> onEdit,
                            Supplier<String> currentStudySetId) {
    this.viewModel = viewModel;
    this.parent = parent;
    viewModel.addPropertyChangeListener(this);

    setLayout(new BorderLayout(0, 8));
    setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    questionList.addListSelectionListener(e -> showSelectedCard());

    editButton.addActionListener(e -> {
      final ViewFlashcardsState.Item selected = questionList.getSelectedValue();
      final int index = questionList.getSelectedIndex();
      dialog.setVisible(false);
      onEdit.accept(selected, index);
    });
    moveUpButton.addActionListener(e -> {      // extension 3b: change the order
      final int index = questionList.getSelectedIndex();
      reorderController.execute(currentStudySetId.get(), index, index - 1);
    });
    moveDownButton.addActionListener(e -> {    // extension 3b: change the order
      final int index = questionList.getSelectedIndex();
      reorderController.execute(currentStudySetId.get(), index, index + 1);
    });

    final JPanel cardPanel = new JPanel(new GridLayout(2, 1, 0, 6));
    cardPanel.add(labelled("Question", questionArea));
    cardPanel.add(labelled("Answer", answerArea));
    final JPanel buttons = new JPanel();
    buttons.add(moveUpButton);
    buttons.add(moveDownButton);
    buttons.add(editButton);
    final JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(cardPanel, BorderLayout.CENTER);
    southPanel.add(buttons, BorderLayout.SOUTH);

    add(hintLabel, BorderLayout.NORTH);
    add(new JScrollPane(questionList), BorderLayout.CENTER);
    add(southPanel, BorderLayout.SOUTH);
  }

  /**
   * Sets the controller that handles the Move Up and Move Down buttons.
   *
   * @param reorderController the controller to call when the user moves a flashcard
   */
  public void setReorderController(ReorderFlashcardsController reorderController) {
    this.reorderController = reorderController;
  }

  /**
   * Shows the flashcards from the view model in the pop-up, or shows the error
   * if the use case failed.
   *
   * @param evt the change event from the view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final ViewFlashcardsState state = viewModel.getState();
    if (state.getErrorMessage() != null) {
      JOptionPane.showMessageDialog(parent, state.getErrorMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    listModel.clear();
    for (ViewFlashcardsState.Item item : state.getCards()) {
      listModel.addElement(item);
    }
    hintLabel.setText(state.getCards().isEmpty()
        ? "This study set has no flashcards yet."
        : "Click a question to see its flashcard. Move it up or down, or Edit to change it.");
    if (state.getSelectedIndex() >= 0) {
      questionList.setSelectedIndex(state.getSelectedIndex());
      questionList.ensureIndexIsVisible(state.getSelectedIndex());
    }
    showSelectedCard();

    // Shown after the use case finishes, so the modal pop-up doesn't block it.
    // When the pop-up is already open (after a move), it just shows the new order.
    if (dialog == null || !dialog.isVisible()) {
      SwingUtilities.invokeLater(() -> showDialog(state.getStudySetName()));
    }
  }

  /**
   * Shows the question and answer of the selected flashcard, or nothing if no
   * flashcard is selected. Edit is only enabled while a flashcard is selected, and
   * Move Up / Move Down only when the flashcard can move that way.
   */
  private void showSelectedCard() {
    final ViewFlashcardsState.Item selected = questionList.getSelectedValue();
    questionArea.setText(selected == null ? "" : selected.getQuestion());
    answerArea.setText(selected == null ? "" : selected.getAnswer());
    editButton.setEnabled(selected != null);
    final int index = questionList.getSelectedIndex();
    moveUpButton.setEnabled(index > 0);
    moveDownButton.setEnabled(index >= 0 && index < listModel.size() - 1);
  }

  /**
   * Opens the pop-up over the parent component, creating it the first time.
   *
   * @param studySetName the name to show in the pop-up's title bar
   */
  private void showDialog(String studySetName) {
    if (dialog == null) {
      dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "",
          Dialog.ModalityType.APPLICATION_MODAL);
      dialog.setContentPane(this);
      dialog.setSize(new Dimension(500, 450));
    }
    dialog.setTitle("Flashcards in " + studySetName);
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }

  private static JTextArea createCardArea() {
    final JTextArea area = new JTextArea(3, 30);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    return area;
  }

  private static JPanel labelled(String title, JTextArea area) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel(title + ":"), BorderLayout.NORTH);
    panel.add(new JScrollPane(area), BorderLayout.CENTER);
    return panel;
  }
}
