package view;

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

/**
 * A pop-up window listing the questions in the open study set. Clicking a
 * question shows that flashcard's question and answer, and Edit opens it for editing.
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
  private JDialog dialog;

  /**
   * Builds the view.
   *
   * @param viewModel the view model to listen to
   * @param parent    the component the pop-up is shown over
   * @param onEdit    run with the selected flashcard and its position when the user clicks Edit
   */
  public ViewFlashcardsView(ViewFlashcardsViewModel viewModel, Component parent,
                            ObjIntConsumer<ViewFlashcardsState.Item> onEdit) {
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

    final JPanel cardPanel = new JPanel(new GridLayout(2, 1, 0, 6));
    cardPanel.add(labelled("Question", questionArea));
    cardPanel.add(labelled("Answer", answerArea));
    final JPanel buttons = new JPanel();
    buttons.add(editButton);
    final JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(cardPanel, BorderLayout.CENTER);
    southPanel.add(buttons, BorderLayout.SOUTH);

    add(hintLabel, BorderLayout.NORTH);
    add(new JScrollPane(questionList), BorderLayout.CENTER);
    add(southPanel, BorderLayout.SOUTH);
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
        : "Click a question to see its flashcard, then Edit to change it.");
    showSelectedCard();

    // Shown after the use case finishes, so the modal pop-up doesn't block it.
    SwingUtilities.invokeLater(() -> showDialog(state.getStudySetName()));
  }

  /**
   * Shows the question and answer of the selected flashcard, or nothing if no
   * flashcard is selected. Edit is only enabled while a flashcard is selected.
   */
  private void showSelectedCard() {
    final ViewFlashcardsState.Item selected = questionList.getSelectedValue();
    questionArea.setText(selected == null ? "" : selected.getQuestion());
    answerArea.setText(selected == null ? "" : selected.getAnswer());
    editButton.setEnabled(selected != null);
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
