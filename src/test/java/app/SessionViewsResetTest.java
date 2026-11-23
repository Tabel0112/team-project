package app;

import data_access.InMemoryStudySetDataAccessObject;
import entity.Flashcard;
import entity.User;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.edit_flashcard.EditFlashcardViewModel;
import interface_adapter.login.LoginViewModel;
import interface_adapter.view_flashcards.ViewFlashcardsViewModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.SwingUtilities;
import org.junit.Test;
import use_case.upload_notes.UploadNotesInteractor;
import view.UploadNotesPanel;
import view.CreateStudySetView;
import javax.swing.JTextField;
import java.awt.Component;
import static org.junit.Assert.*;

public class SessionViewsResetTest {
  @Test public void resetReplacesAccountPanelsAndRejectsDetachedPdfResult() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        AppBuilder builder = new AppBuilder().addCreateStudySetView().addStudySetView()
            .addAddFlashcardView().addViewFlashcardsView().addEditFlashcardView()
            .addCreateStudySetUseCase().addViewFlashcardsUseCase();
        builder.buildPages(new UploadNotesInteractor(path -> "fixture"));
        LoginViewModel login = field(builder, "loginModel", LoginViewModel.class);
        login.setUser(new User("account-a", "a@example.test"));
        UploadNotesPanel oldPanel = field(builder, "uploadNotesPanel", UploadNotesPanel.class);
        @SuppressWarnings("unchecked")
        BiConsumer<String, List<Flashcard>> result = field(oldPanel, "onGenerated", BiConsumer.class);
        result.accept("Account A notes", List.of(new Flashcard("Private Q", "Private A")));
        InMemoryStudySetDataAccessObject dao = field(builder, "studySetDao", InMemoryStudySetDataAccessObject.class);
        assertEquals(1, dao.getStudySets("account-a").size());
        EditFlashcardViewModel edit = field(builder, "editFlashcardViewModel", EditFlashcardViewModel.class);
        edit.getState().open(0, "Private Q", "Private A"); edit.firePropertyChanged();
        ViewFlashcardsViewModel oldPreview = field(builder, "viewFlashcardsViewModel", ViewFlashcardsViewModel.class);
        oldPreview.getState().setStudySetName("Account A notes");
        dao.clear(); login.setUser(null);
        Method reset = AppBuilder.class.getDeclaredMethod("clearSessionViews");
        reset.setAccessible(true); reset.invoke(builder);
        assertNotSame(oldPanel, field(builder, "uploadNotesPanel", UploadNotesPanel.class));
        assertNotSame(oldPreview, field(builder, "viewFlashcardsViewModel", ViewFlashcardsViewModel.class));
        assertEquals("", oldPreview.getState().getStudySetName());
        assertEquals(-1, edit.getState().getCardIndex());
        assertEquals("", edit.getState().getQuestion());
        assertTrue(field(builder, "createStudySetViewModel", CreateStudySetViewModel.class)
            .getState().getStudySets().isEmpty());
        login.setUser(new User("account-b", "b@example.test"));
        result.accept("Stale account A notes", List.of(new Flashcard("Old Q", "Old A")));
        assertFalse(dao.hasStudySets());
        assertTrue(dao.getStudySets("account-b").isEmpty());
        UploadNotesPanel currentPanel = field(builder, "uploadNotesPanel", UploadNotesPanel.class);
        @SuppressWarnings("unchecked")
        BiConsumer<String, List<Flashcard>> current = field(currentPanel, "onGenerated", BiConsumer.class);
        current.accept("Account B notes", List.of(new Flashcard("New Q", "New A")));
        assertEquals(1, dao.getStudySets("account-b").size());
        assertTrue(dao.getStudySets("account-a").isEmpty());
      } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    });
  }
  @Test public void typedNameAloneLeavesQuietlyAndIsClearedAtSessionReset() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        AppBuilder builder = new AppBuilder().addCreateStudySetView().addStudySetView()
            .addAddFlashcardView().addViewFlashcardsView().addEditFlashcardView()
            .addCreateStudySetUseCase().addViewFlashcardsUseCase();
        builder.buildPages(new UploadNotesInteractor(path -> "fixture"));
        field(builder, "loginModel", LoginViewModel.class).setUser(new User("uid", "a@example.test"));
        CreateStudySetView view = field(builder, "createStudySetView", CreateStudySetView.class);
        JTextField name = field(view, "nameField", JTextField.class);
        name.setText("Unsubmitted name");
        Method confirm = AppBuilder.class.getDeclaredMethod("confirmDiscard", Component.class, String.class);
        confirm.setAccessible(true);
        // A headless test would throw if the typed name caused a confirmation dialog.
        assertEquals(true, confirm.invoke(builder, null, "log out"));
        Method reset = AppBuilder.class.getDeclaredMethod("clearSessionViews");
        reset.setAccessible(true); reset.invoke(builder);
        assertEquals("", name.getText());
      } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    });
  }
  @Test public void pdfCompletionDuringCloudWorkWaitsForIdleAndRemainsUnsaved() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        AppBuilder builder = new AppBuilder().addCreateStudySetView().addStudySetView()
            .addAddFlashcardView().addViewFlashcardsView().addEditFlashcardView()
            .addCreateStudySetUseCase().addViewFlashcardsUseCase();
        builder.buildPages(new UploadNotesInteractor(path -> "fixture"));
        LoginViewModel model = field(builder, "loginModel", LoginViewModel.class);
        model.setUser(new User("uid", "a@example.test"));
        InMemoryStudySetDataAccessObject dao = field(builder, "studySetDao", InMemoryStudySetDataAccessObject.class);
        model.setBusy(true);
        UploadNotesPanel panel = field(builder, "uploadNotesPanel", UploadNotesPanel.class);
        @SuppressWarnings("unchecked")
        BiConsumer<String, List<Flashcard>> result = field(panel, "onGenerated", BiConsumer.class);
        result.accept("Deferred PDF", List.of(new Flashcard("Q", "A")));
        assertTrue(dao.getStudySets("uid").isEmpty());
        model.setBusy(false);
        assertEquals(1, dao.getStudySets("uid").size());
        assertTrue(dao.hasUnsaved("uid"));
      } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    });
  }
  @Test public void acknowledgedWorkingCopyDoesNotPromptToDiscardAndLaterEditIsDirty() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        AppBuilder builder = new AppBuilder().addCreateStudySetView().addStudySetView()
            .addAddFlashcardView().addViewFlashcardsView().addEditFlashcardView()
            .addCreateStudySetUseCase().addViewFlashcardsUseCase();
        builder.buildPages(new UploadNotesInteractor(path -> "fixture"));
        field(builder, "loginModel", LoginViewModel.class).setUser(new User("uid", "a@example.test"));
        InMemoryStudySetDataAccessObject dao = field(builder, "studySetDao", InMemoryStudySetDataAccessObject.class);
        entity.StudySet set = new entity.StudySet("id", "Saved"); dao.save("uid", set);
        dao.acknowledge(dao.pendingOrCapture("uid", "id"), 1);
        Method confirm = AppBuilder.class.getDeclaredMethod("confirmDiscard", Component.class, String.class);
        confirm.setAccessible(true); assertEquals(true, confirm.invoke(builder, null, "log out"));
        set.getCardsList().addCard(new Flashcard("Changed", "A"));
        assertTrue(dao.hasUnsaved("uid"));
      } catch (ReflectiveOperationException | use_case.cloud.CloudException exception) {
        throw new AssertionError(exception);
      }
    });
  }
  private static <T> T field(Object target, String name, Class<T> type) throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(name); field.setAccessible(true);
    return type.cast(field.get(target));
  }
}
