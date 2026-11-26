package app;

import data_access.InMemoryStudySetDataAccessObject;
import entity.Flashcard;
import entity.User;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.edit_flashcard.EditFlashcardViewModel;
import interface_adapter.login.LoginViewModel;
import interface_adapter.ViewManagerModel;
import interface_adapter.add_flashcard.AddFlashcardViewModel;
import interface_adapter.cloud.CloudController;
import interface_adapter.cloud.CloudPresenter;
import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.practice_study_set.PracticeStudySetState;
import interface_adapter.practice_study_set.PracticeStudySetViewModel;
import interface_adapter.study_set.StudySetViewModel;
import interface_adapter.view_flashcards.ViewFlashcardsViewModel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;
import org.junit.Test;
import use_case.upload_notes.UploadNotesInteractor;
import use_case.cloud.CloudException;
import use_case.cloud.SetSnapshot;
import use_case.cloud.SetSummary;
import use_case.load_study_sets.LoadStudySetsDataAccessInterface;
import use_case.load_study_sets.LoadStudySetsInteractor;
import view.UploadNotesPanel;
import view.CreateStudySetView;
import view.EditFlashcardView;
import view.PracticeStudySetView;
import view.ReviewStudySetsView;
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
  @Test public void refreshCancelAndFailureKeepSignedInViewsWhileSuccessResetsAndRejectsOldPdfResults() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      try {
        AppBuilder builder = new AppBuilder().addCreateStudySetView().addStudySetView()
            .addAddFlashcardView().addViewFlashcardsView().addEditFlashcardView()
            .addCreateStudySetUseCase().addViewFlashcardsUseCase().addReviewViews();
        JTabbedPane pages = builder.buildPages(new UploadNotesInteractor(path -> "fixture"));
        LoginViewModel login = field(builder, "loginModel", LoginViewModel.class);
        User user = new User("uid", "a@example.test"); login.setUser(user); String scope = login.scope();
        InMemoryStudySetDataAccessObject dao = field(builder, "studySetDao", InMemoryStudySetDataAccessObject.class);
        entity.StudySet draft = new entity.StudySet("draft", "Local draft"); dao.save("uid", draft);
        CreateStudySetViewModel list = field(builder, "createStudySetViewModel", CreateStudySetViewModel.class);
        list.getState().setStudySets(List.of(new CreateStudySetState.Item("draft", "Local draft", 3)));
        StudySetViewModel set = field(builder, "studySetViewModel", StudySetViewModel.class);
        set.getState().open("draft", "Local draft", 3);
        EditFlashcardViewModel edit = field(builder, "editFlashcardViewModel", EditFlashcardViewModel.class);
        edit.getState().open(0, "Typed edit", "Typed answer"); edit.firePropertyChanged();
        AddFlashcardViewModel add = field(builder, "addFlashcardViewModel", AddFlashcardViewModel.class);
        add.getState().setQuestion("Typed add"); add.firePropertyChanged();
        JTextField name = field(field(builder, "createStudySetView", CreateStudySetView.class), "nameField", JTextField.class);
        name.setText("Typed name");
        ViewManagerModel views = field(builder, "viewManagerModel", ViewManagerModel.class);
        views.setActiveView(EditFlashcardView.VIEW_NAME); pages.setSelectedIndex(2);
        PracticeStudySetViewModel practice = field(builder, "practiceStudySetViewModel", PracticeStudySetViewModel.class);
        practice.getState().start("Old practice", List.of(new PracticeStudySetState.Item("Q", "A")));
        ViewManagerModel review = field(builder, "reviewViewManagerModel", ViewManagerModel.class);
        review.setActiveView(PracticeStudySetView.VIEW_NAME);
        UploadNotesPanel oldPanel = field(builder, "uploadNotesPanel", UploadNotesPanel.class);
        ViewFlashcardsViewModel oldPreview = field(builder, "viewFlashcardsViewModel", ViewFlashcardsViewModel.class);
        @SuppressWarnings("unchecked")
        BiConsumer<String, List<Flashcard>> pdf = field(oldPanel, "onGenerated", BiConsumer.class);
        Tasks worker = new Tasks(), ui = new Tasks(); final boolean[] offline = {true};
        final int[] lists = {0}, loads = {0};
        LoadStudySetsDataAccessInterface cloud = new LoadStudySetsDataAccessInterface() {
          public List<SetSummary> list(String userId) throws CloudException {
            lists[0]++; assertSame(draft, dao.getStudySetById("draft"));
            if (offline[0]) { throw new CloudException("offline"); }
            return List.of(new SetSummary("fresh", "Saved set"));
          }
          public SetSnapshot load(String userId, String id) {
            loads[0]++; return new SetSnapshot(id, "Saved set", 4, List.of());
          }
        };
        Method reset = AppBuilder.class.getDeclaredMethod("resetAfterRefresh"); reset.setAccessible(true);
        CloudPresenter presenter = new CloudPresenter(login, list, set, views, ui, () -> {
          assertTrue(SwingUtilities.isEventDispatchThread()); assertTrue(login.isBusy());
          try { reset.invoke(builder); } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
        });
        CloudController controller = new CloudController(input -> fail(),
            new LoadStudySetsInteractor(dao, cloud, presenter), login, worker, ui);
        controller.refresh(() -> false);
        assertTrue(worker.tasks.isEmpty()); assertEquals(0, lists[0]);
        assertSame(draft, dao.getStudySetById("draft")); assertEquals("Typed name", name.getText());
        controller.refresh(() -> true); worker.drain(); ui.drain();
        assertEquals(1, lists[0]); assertSame(draft, dao.getStudySetById("draft"));
        assertSame(oldPanel, field(builder, "uploadNotesPanel", UploadNotesPanel.class));
        assertEquals(EditFlashcardView.VIEW_NAME, views.getActiveView()); assertEquals(2, pages.getSelectedIndex());
        assertEquals("Typed edit", edit.getState().getQuestion()); assertEquals("Typed name", name.getText());
        offline[0] = false;
        controller.refresh(() -> { pdf.accept("During prompt", List.of(new Flashcard("Q", "A"))); return true; });
        assertEquals(1, field(builder, "deferredGenerated", List.class).size());
        worker.drain(); assertEquals(EditFlashcardView.VIEW_NAME, views.getActiveView());
        ui.drain();
        assertSame(user, login.getUser()); assertEquals(scope, login.scope()); assertFalse(login.isBusy());
        assertEquals(CreateStudySetView.VIEW_NAME, views.getActiveView()); assertEquals(0, pages.getSelectedIndex());
        assertEquals(ReviewStudySetsView.VIEW_NAME, review.getActiveView()); assertEquals(0, practice.getState().getCardCount());
        assertEquals("", set.getState().getStudySetId()); assertEquals("", edit.getState().getQuestion());
        assertEquals("", add.getState().getQuestion()); assertEquals("", name.getText());
        assertNotSame(oldPreview, field(builder, "viewFlashcardsViewModel", ViewFlashcardsViewModel.class));
        assertNotSame(oldPanel, field(builder, "uploadNotesPanel", UploadNotesPanel.class));
        assertTrue(field(builder, "deferredGenerated", List.class).isEmpty());
        pdf.accept("Late result", List.of(new Flashcard("Old", "A")));
        assertFalse(dao.hasUnsaved("uid")); assertEquals(1, list.getState().getStudySets().size());
        assertEquals("fresh", list.getState().getStudySets().get(0).getId());
        assertEquals(-1, list.getState().getStudySets().get(0).getCardCount()); assertEquals(0, loads[0]);
        controller.open("fresh"); worker.drain(); ui.drain();
        assertEquals(1, loads[0]); assertEquals("fresh", set.getState().getStudySetId());
      } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    });
  }
  private static final class Tasks implements Executor {
    final Queue<Runnable> tasks = new ArrayDeque<>();
    public void execute(Runnable task) { tasks.add(task); }
    void drain() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
  }
  private static <T> T field(Object target, String name, Class<T> type) throws ReflectiveOperationException {
    Field field = target.getClass().getDeclaredField(name); field.setAccessible(true);
    return type.cast(field.get(target));
  }
}
