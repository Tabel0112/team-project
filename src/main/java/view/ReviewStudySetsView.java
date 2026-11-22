package view;

import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.practice_study_set.PracticeStudySetController;
import interface_adapter.practice_study_set.PracticeStudySetViewModel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * The first view in the Review tab: lists the user's study sets, each with a
 * Review button that starts practising it.
 *
 * <p>The list comes from the study set list's view model, which the create, add
 * and delete use cases already keep up to date.
 */
public class ReviewStudySetsView extends JPanel implements PropertyChangeListener {
  public static final String VIEW_NAME = "review study sets";

  private final CreateStudySetViewModel studySetListViewModel;
  private final PracticeStudySetViewModel practiceViewModel;
  private final JPanel rows = new JPanel();
  private final JLabel errorLabel = new JLabel(" ");
  private PracticeStudySetController controller;

  /**
   * Builds the view. The controller is supplied afterwards with {@link #setController}.
   *
   * @param studySetListViewModel holds the user's study sets
   * @param practiceViewModel     holds any error from starting a practice session
   */
  public ReviewStudySetsView(CreateStudySetViewModel studySetListViewModel,
                             PracticeStudySetViewModel practiceViewModel) {
    this.studySetListViewModel = studySetListViewModel;
    this.practiceViewModel = practiceViewModel;
    studySetListViewModel.addPropertyChangeListener(this);
    practiceViewModel.addPropertyChangeListener(this);

    setLayout(new BorderLayout(0, 8));
    setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    final JLabel title = new JLabel("Choose a study set to review");
    title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
    errorLabel.setForeground(Color.RED);

    final JPanel header = new JPanel(new BorderLayout());
    header.add(title, BorderLayout.NORTH);
    header.add(errorLabel, BorderLayout.SOUTH);

    rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
    final JPanel rowsHolder = new JPanel(new BorderLayout());    // keeps rows at the top
    rowsHolder.add(rows, BorderLayout.NORTH);

    add(header, BorderLayout.NORTH);
    add(new JScrollPane(rowsHolder), BorderLayout.CENTER);
    showStudySets();
  }

  /**
   * Sets the controller that handles the Review buttons.
   *
   * @param controller the controller to call when the user clicks Review
   */
  public void setController(PracticeStudySetController controller) {
    this.controller = controller;
  }

  /**
   * Refreshes the list of study sets, and shows any error from starting a session.
   *
   * @param evt the change event from either view model
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    final String error = practiceViewModel.getState().getErrorMessage();
    errorLabel.setText(error == null ? " " : error);
    showStudySets();
  }

  private void showStudySets() {
    rows.removeAll();
    if (studySetListViewModel.getState().getStudySets().isEmpty()) {
      rows.add(new JLabel("You have no study sets yet. Create one in the Study sets tab."));
    }
    for (CreateStudySetState.Item item : studySetListViewModel.getState().getStudySets()) {
      rows.add(createRow(item));
      rows.add(Box.createVerticalStrut(6));
    }
    rows.revalidate();
    rows.repaint();
  }

  private Component createRow(CreateStudySetState.Item item) {
    final int count = item.getCardCount();
    final JLabel label = new JLabel(item.getName() + "  (" + count
        + (count == 1 ? " flashcard)" : " flashcards)"));

    final JButton review = new JButton("Review");
    review.addActionListener(e -> controller.execute(item.getId()));   // step 1

    final JPanel row = new JPanel(new BorderLayout(12, 0));
    row.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(Color.LIGHT_GRAY),
        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    row.add(label, BorderLayout.CENTER);
    row.add(review, BorderLayout.EAST);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
    return row;
  }
}
