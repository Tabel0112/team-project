package view;

import interface_adapter.create_study_set.CreateStudySetViewModel;
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
