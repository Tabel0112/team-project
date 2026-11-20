package view;

import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.edit_flashcard.EditFlashcardViewModel;
import javax.swing.*;
import java.awt.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class UnsubmittedFormTest {
  @Test public void refreshingListCannotClearAnUnsubmittedNameAfterPriorCreation() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      CreateStudySetViewModel model = new CreateStudySetViewModel();
      CreateStudySetView view = new CreateStudySetView(model, () -> "user", item -> { });
      model.getState().setCreatedStudySetId("prior-created-set");
      textField(view).setText("Unfinished name");
      model.firePropertyChanged();
      assertTrue(view.hasUnsubmittedInput());
      assertEquals("Unfinished name", textField(view).getText());
    });
  }
  @Test public void editFormOnlyWarnsForChangesAndKeepsBaselineAfterValidationFailure() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      EditFlashcardViewModel model = new EditFlashcardViewModel();
      EditFlashcardView view = new EditFlashcardView(model, () -> "set", () -> { });
      model.getState().open(0, "Original question", "Original answer"); model.firePropertyChanged();
      assertFalse(view.hasUnsubmittedInput());
      JTextArea question = textArea(view);
      question.setText("");
      assertTrue(view.hasUnsubmittedInput());
      model.getState().setQuestion(""); model.getState().setErrorMessage("Enter a question");
      model.firePropertyChanged();
      assertTrue(view.hasUnsubmittedInput());
      model.getState().clear(); model.firePropertyChanged();
      assertFalse(view.hasUnsubmittedInput()); assertEquals("", question.getText());
    });
  }
  private JTextArea textArea(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JTextArea) { return (JTextArea) child; }
      if (child instanceof Container) {
        JTextArea found = textArea((Container) child);
        if (found != null) { return found; }
      }
    }
    return null;
  }
  private JTextField textField(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JTextField) { return (JTextField) child; }
      if (child instanceof Container) {
        JTextField found = textField((Container) child);
        if (found != null) { return found; }
      }
    }
    return null;
  }
}
