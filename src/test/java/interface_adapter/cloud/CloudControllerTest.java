package interface_adapter.cloud;

import data_access.InMemoryStudySetDataAccessObject;
import entity.StudySet;
import entity.Flashcard;
import entity.User;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.ViewManagerModel;
import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.study_set.StudySetViewModel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.Test;
import use_case.cloud.CloudException;
import use_case.cloud.SetSnapshot;
import use_case.load_study_sets.LoadStudySetsInputBoundary;
import use_case.load_study_sets.LoadStudySetsInteractor;
import use_case.load_study_sets.LoadStudySetsDataAccessInterface;
import use_case.login.LoginDataAccessInterface;
import use_case.login.LoginInteractor;
import static org.junit.Assert.*;

public class CloudControllerTest {
  @Test public void asyncCloudRequestBlocksDuplicateSaveLogoutAndAuthOnSharedModel() {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
    Tasks worker = new Tasks(), ui = new Tasks();
    InMemoryStudySetDataAccessObject local = new InMemoryStudySetDataAccessObject();
    local.save("user", new StudySet("set", "Course"));
    final int[] calls = {0}; Auth auth = new Auth();
    CloudController controller = new CloudController(input -> { calls[0]++; }, new LoadStub(), model, worker, ui);
    LoginController login = new LoginController(input -> fail(), auth, model, worker, ui, local::clear);
    controller.save("set");
    assertTrue(model.isBusy()); assertEquals(0, calls[0]);
    controller.save("different"); login.logout();
    char[] password = "discard".toCharArray(); login.login("email", password, false);
    assertEquals('\0', password[0]);
    assertEquals(1, worker.tasks.size()); assertTrue(local.hasUnsaved("user"));
    worker.drain(); assertEquals(1, calls[0]); assertTrue(model.isBusy());
    ui.drain(); assertFalse(model.isBusy()); assertEquals("user", model.getUser().getId());
  }
  @Test public void successfulLoginLoadsListAfterUiResultsAndFailedReauthKeepsErrorAndDrafts() {
    LoginViewModel model = new LoginViewModel();
    Tasks worker = new Tasks(), ui = new Tasks();
    final int[] lists = {0}; final boolean[] failAuth = {false};
    Auth auth = new Auth() {
      @Override public User login(String email, char[] password, String required) throws CloudException {
        if (failAuth[0]) { throw new CloudException("Reauthentication failed; drafts kept"); }
        return new User("user", email);
      }
    };
    CloudController cloud = new CloudController(input -> { },
        new LoadStub() {
          @Override public void list(String user) { lists[0]++; }
          @Override public void refresh(String user, SetSnapshot set, boolean reload) { fail("Login only lists sets"); }
        }, model, worker, ui);
    LoginController controller = new LoginController(new LoginInteractor(auth, new LoginPresenter(model, ui)),
        auth, model, worker, ui, () -> { }, cloud::refresh);
    controller.login("student@example.test", "example".toCharArray(), false);
    cloud.refresh(); assertEquals(1, worker.tasks.size());
    worker.drain(); assertNull(model.getUser()); ui.drain();
    assertEquals("user", model.getUser().getId()); assertTrue(model.isBusy());
    worker.drain(); ui.drain(); assertEquals(1, lists[0]); assertFalse(model.isBusy());
    failAuth[0] = true;
    controller.login("student@example.test", "example".toCharArray(), false);
    worker.drain(); ui.drain();
    assertEquals(1, lists[0]); assertEquals("Reauthentication failed; drafts kept", model.getMessage());
  }
  @Test public void refreshConfirmsWhileBusyThenSerializesAcceptedAndDeclinedRequests() {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
    Tasks worker = new Tasks(), ui = new Tasks();
    final int[] confirmations = {0}, refreshes = {0}; final boolean[] discarded = {false};
    CloudController controller = new CloudController(input -> fail(), new LoadStub() {
      @Override public boolean needsRefreshConfirmation(String user, String set) {
        assertEquals("user", user); assertEquals("set", set); return true;
      }
      @Override public void refresh(String user, SetSnapshot set, boolean reload) {
        assertEquals("user", user); assertEquals("set", set.id); refreshes[0]++; discarded[0] = reload;
      }
    }, model, worker, ui);
    controller.refresh(() -> "set", () -> {
      assertTrue(model.isBusy()); confirmations[0]++; return false;
    });
    controller.refresh(() -> "set", () -> { fail("No duplicate confirmation while busy"); return true; });
    assertEquals(1, confirmations[0]); assertEquals(0, refreshes[0]);
    worker.drain(); ui.drain(); assertEquals(1, refreshes[0]); assertFalse(discarded[0]);
    controller.refresh(() -> "set", () -> { confirmations[0]++; return true; });
    worker.drain(); ui.drain(); assertEquals(2, refreshes[0]); assertTrue(discarded[0]);
    assertEquals(2, confirmations[0]); assertFalse(model.isBusy());
  }
  @Test public void listOnlyRefreshNeverPromptsAndFreshResultUpdatesInPlaceOnUiExecutor() {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
    Tasks worker = new Tasks(), ui = new Tasks(); final int[] refreshes = {0};
    CloudController controller = new CloudController(input -> fail(), new LoadStub() {
      @Override public void refresh(String user, SetSnapshot set, boolean reload) {
        assertNull(set); assertFalse(reload); refreshes[0]++;
      }
    }, model, worker, ui);
    controller.refresh(() -> null, () -> { fail("No open saved set to discard"); return false; });
    worker.drain(); ui.drain(); assertEquals(1, refreshes[0]);
    CreateStudySetViewModel list = new CreateStudySetViewModel();
    list.getState().setStudySets(List.of(new CreateStudySetState.Item("set", "Old", 8)));
    StudySetViewModel set = new StudySetViewModel(); set.getState().open("set", "Old", 8);
    ViewManagerModel views = new ViewManagerModel(); views.setActiveView("unchanged");
    CloudPresenter presenter = new CloudPresenter(model, list, set, views, ui);
    presenter.refreshed(new SetSnapshot("set", "Fresh", 3, List.of(
        new SetSnapshot.Card("card", "Fresh Q", "Fresh A", 0))));
    assertEquals("Old", set.getState().getName());
    ui.drain(); assertEquals("Fresh", set.getState().getName()); assertEquals(1, set.getState().getCardCount());
    assertEquals("Fresh", list.getState().getStudySets().get(0).getName());
    assertEquals(1, list.getState().getStudySets().get(0).getCardCount());
    assertEquals("unchanged", views.getActiveView());
  }
  @Test public void refreshCapturesBeforeConfirmationAndKeepsEditsMadeInsideModalLoop() throws Exception {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
    Tasks worker = new Tasks(), ui = new Tasks();
    InMemoryStudySetDataAccessObject local = new InMemoryStudySetDataAccessObject();
    local.install("user", new SetSnapshot("set", "Saved", 2, List.of()));
    StudySet working = local.getStudySetById("set");
    working.getCardsList().addCard(new Flashcard("before confirmation", "answer"));
    CloudPresenter presenter = new CloudPresenter(model, new CreateStudySetViewModel(),
        new StudySetViewModel(), new ViewManagerModel(), ui);
    LoadStudySetsInteractor load = new LoadStudySetsInteractor(local, new LoadStudySetsDataAccessInterface() {
      public List<use_case.cloud.SetSummary> list(String user) { return List.of(); }
      public SetSnapshot load(String user, String set) { return new SetSnapshot("set", "Fresh", 3, List.of()); }
    }, presenter);
    CloudController controller = new CloudController(input -> fail(), load, model, worker, ui);
    controller.refresh(() -> "set", () -> {
      assertTrue(model.isBusy());
      working.getCardsList().addCard(new Flashcard("during confirmation", "kept"));
      return true;
    });
    worker.drain(); assertTrue(model.isBusy()); ui.drain();
    assertFalse(model.isBusy()); assertSame(working, local.getStudySetById("set"));
    assertEquals(2, local.local("user", "set").cards.size());
    assertEquals(2, local.local("user", "set").revision);
    assertTrue(model.getMessage().contains("newer local changes were kept"));
  }
  @Test public void refreshDropsSelectionChangedDuringConfirmation() {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
    Tasks worker = new Tasks(), ui = new Tasks(); String[] visible = {"set"};
    CloudController controller = new CloudController(input -> fail(), new LoadStub() {
      @Override public boolean needsRefreshConfirmation(String user, String set) { return true; }
      @Override public void refresh(String user, SetSnapshot expected, boolean reload) { assertNull(expected); }
    }, model, worker, ui);
    controller.refresh(() -> visible[0], () -> { visible[0] = null; return true; });
    worker.drain(); ui.drain(); assertFalse(model.isBusy());
  }
  private static class Tasks implements Executor {
    final Queue<Runnable> tasks = new ArrayDeque<>();
    public void execute(Runnable task) { tasks.add(task); }
    void drain() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
  }
  private static class LoadStub implements LoadStudySetsInputBoundary {
    public void list(String user) { }
    public void open(String user, String set) { }
    public boolean needsRefreshConfirmation(String user, String set) { return false; }
    public SetSnapshot captureRefresh(String user, String set) {
      return set == null ? null : new SetSnapshot(set, "Saved", 1, List.of());
    }
    public void refresh(String user, SetSnapshot set, boolean reload) { }
  }
  private static class Auth implements LoginDataAccessInterface {
    public User login(String email, char[] password, String required) throws CloudException { return null; }
    public void signup(String email, char[] password) { }
    public void forgetSession() { }
  }
}
