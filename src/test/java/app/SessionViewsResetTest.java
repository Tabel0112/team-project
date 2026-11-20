package app;

import interface_adapter.edit_flashcard.EditFlashcardViewModel;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.view_flashcards.ViewFlashcardsState;
import interface_adapter.view_flashcards.ViewFlashcardsViewModel;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.*;
import org.junit.Test;
import use_case.upload_notes.UploadNotesInteractor;
import view.EditFlashcardView;
import view.ViewFlashcardsView;
import static org.junit.Assert.*;

public class SessionViewsResetTest {
  @Test public void confirmedAccountResetClearsEditAndPreviewAndCancelsDelayedPopup() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        AppBuilder builder = new AppBuilder().addCreateStudySetView().addStudySetView()
            .addAddFlashcardView().addViewFlashcardsView().addEditFlashcardView();
        builder.buildPages(new UploadNotesInteractor(path -> "fixture"));
        CreateStudySetViewModel list = field(builder, "createStudySetViewModel", CreateStudySetViewModel.class);
        list.getState().setName("Account A set");
        EditFlashcardViewModel edit = field(builder, "editFlashcardViewModel", EditFlashcardViewModel.class);
        edit.getState().open(0, "Account A question", "Account A answer"); edit.firePropertyChanged();
        ViewFlashcardsViewModel preview = field(builder, "viewFlashcardsViewModel", ViewFlashcardsViewModel.class);
        preview.getState().setStudySetName("Account A set");
        preview.getState().setCards(List.of(new ViewFlashcardsState.Item("Account A question", "Account A answer")));
        preview.firePropertyChanged(); // Queues a popup after this event, as the real View use case does.
        Method clear = AppBuilder.class.getDeclaredMethod("clearSessionViews");
        clear.setAccessible(true); clear.invoke(builder);
        assertEquals("", list.getState().getName());
        assertEquals(-1, edit.getState().getCardIndex());
        assertEquals("", edit.getState().getQuestion()); assertEquals("", edit.getState().getAnswer());
        assertFalse(field(builder, "editFlashcardView", EditFlashcardView.class).hasUnsubmittedInput());
        assertEquals("", preview.getState().getStudySetName()); assertTrue(preview.getState().getCards().isEmpty());
        ViewFlashcardsView view = field(builder, "viewFlashcardsView", ViewFlashcardsView.class);
        assertTextEmpty(view);
        assertNull(field(view, "dialog", JDialog.class));
      } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    });
    SwingUtilities.invokeAndWait(() -> { }); // Drain the canceled display callback without opening a modal dialog.
  }
  private static <T> T field(Object target, String name, Class<T> type) throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(name); field.setAccessible(true);
    return type.cast(field.get(target));
  }
  private static void assertTextEmpty(Container parent) {
    for (Component child : parent.getComponents()) {
      if (child instanceof JTextArea) { assertEquals("", ((JTextArea) child).getText()); }
      if (child instanceof Container) { assertTextEmpty((Container) child); }
    }
  }
}
