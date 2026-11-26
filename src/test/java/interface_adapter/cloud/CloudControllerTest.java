package interface_adapter.cloud;

import data_access.InMemoryStudySetDataAccessObject;
import entity.StudySet;
import entity.User;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.ViewManagerModel;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.study_set.StudySetViewModel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.List;
import java.util.concurrent.Executor;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Test;
import use_case.cloud.CloudException;
import use_case.cloud.SetSummary;
import use_case.save_study_set.SaveAllStudySetsOutputData;
import view.CloudPanel;
import use_case.load_study_sets.LoadStudySetsInputBoundary;
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
        new LoadStub() { @Override public void list(String user) { lists[0]++; } }, model, worker, ui);
    LoginController controller = new LoginController(new LoginInteractor(auth, new LoginPresenter(model, ui)),
        auth, model, worker, ui, () -> { }, cloud::list);
    controller.login("student@example.test", "example".toCharArray(), false);
    cloud.list(); assertEquals(1, worker.tasks.size());
    worker.drain(); assertNull(model.getUser()); ui.drain();
    assertEquals("user", model.getUser().getId()); assertTrue(model.isBusy());
    worker.drain(); ui.drain(); assertEquals(1, lists[0]); assertFalse(model.isBusy());
    failAuth[0] = true;
    controller.login("student@example.test", "example".toCharArray(), false);
    worker.drain(); ui.drain();
    assertEquals(1, lists[0]); assertEquals("Reauthentication failed; drafts kept", model.getMessage());
  }
  @Test public void saveAllButtonNeedsNoOpenSetAndBusyWorkerGuardsQueuedDeletionUntilExplicitLogout() throws Exception {
    SwingUtilities.invokeAndWait(() -> {
      LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
      Tasks worker = new Tasks(), ui = new Tasks();
      InMemoryStudySetDataAccessObject local = new InMemoryStudySetDataAccessObject();
      local.mergeSummaries("user", List.of(new SetSummary("remote", "Remote")));
      local.delete("user", "remote");
      assertTrue(local.hasUnsaved("user")); assertTrue(local.summaries("user").isEmpty());
      final int[] batches = {0};
      CloudController controller = new CloudController(input -> fail("No single-set save"), user -> {
        assertEquals("user", user); batches[0]++;
      }, new LoadStub(), model, worker, ui);
      LoginController login = new LoginController(input -> fail(), new Auth(), model, worker, ui, local::clear);
      CloudPanel panel = new CloudPanel(model, controller, () -> true);
      JButton saveAll = null;
      for (java.awt.Component child : panel.getComponents()) {
        if (child instanceof JButton && "Save All".equals(((JButton) child).getText())) {
          saveAll = (JButton) child;
        }
      }
      assertNotNull(saveAll); saveAll.doClick(); assertFalse(saveAll.isEnabled());
      controller.saveAll(); login.logout();
      assertTrue(model.isBusy()); assertEquals(1, worker.tasks.size()); assertEquals(0, batches[0]);
      assertTrue(local.hasUnsaved("user")); assertNotNull(model.getUser());
      worker.drain(); assertTrue(model.isBusy()); ui.drain();
      assertEquals(1, batches[0]); assertFalse(model.isBusy()); assertTrue(saveAll.isEnabled());
      assertTrue(local.hasUnsaved("user"));
      login.logout(); // AppBuilder calls this only after explicit discard confirmation.
      assertNull(model.getUser()); assertFalse(local.hasUnsaved("user"));
      assertTrue(local.pendingDeletions("user").isEmpty());
    });
  }
  @Test public void batchPresenterReportsPartialCountsOnceOnUiWithoutNavigation() {
    LoginViewModel model = new LoginViewModel(); Tasks ui = new Tasks();
    StudySetViewModel set = new StudySetViewModel(); ViewManagerModel views = new ViewManagerModel();
    views.setActiveView("list");
    CloudPresenter presenter = new CloudPresenter(model, new CreateStudySetViewModel(), set, views, ui, () -> { });
    String previous = model.getMessage();
    presenter.savedAll(new SaveAllStudySetsOutputData(2, 1, List.of("Delete: offline"), true));
    assertEquals(previous, model.getMessage()); assertEquals(1, ui.tasks.size());
    ui.drain();
    assertTrue(model.getMessage().contains("Saved 2 study sets. Deleted 1 study set."));
    assertTrue(model.getMessage().contains("1 operation failed"));
    assertTrue(model.getMessage().contains("Pending work was kept"));
    assertEquals(model.getMessage(), set.getState().getMessage()); assertEquals("list", views.getActiveView());
    presenter.savedAll(new SaveAllStudySetsOutputData(0, 0, List.of(), false)); ui.drain();
    assertEquals("No unsaved study changes.", model.getMessage());
  }
  @Test public void cancelledRefreshMarksBusyBeforePromptAndDoesNotChangeStatusOrQueueAnyRequest() {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
    Tasks worker = new Tasks(), ui = new Tasks(); String previous = model.getMessage();
    CloudController controller = new CloudController(input -> fail(), new LoadStub() {
      @Override public void list(String user) { fail("No list on cancellation"); }
      @Override public void refresh(String user) { fail("No refresh on cancellation"); }
    }, model, worker, ui);
    controller.refresh(() -> {
      assertTrue(model.isBusy());
      controller.list(); controller.saveAll();
      controller.refresh(() -> { fail("No nested confirmation while busy"); return true; });
      return false;
    });
    assertFalse(model.isBusy()); assertEquals(previous, model.getMessage());
    assertTrue(worker.tasks.isEmpty()); assertTrue(ui.tasks.isEmpty());
    assertEquals("user", model.getUser().getId());
  }
  @Test public void acceptedRefreshUsesSeparateBackgroundBoundaryUntilUiResultsFinish() {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("user", "email"));
    Tasks worker = new Tasks(), ui = new Tasks(); final int[] refreshes = {0};
    CloudController controller = new CloudController(input -> fail(), new LoadStub() {
      @Override public void list(String user) { fail("Explicit refresh must reset through its own boundary"); }
      @Override public void refresh(String user) { assertEquals("user", user); refreshes[0]++; }
    }, model, worker, ui);
    controller.refresh(() -> true); assertTrue(model.isBusy()); assertEquals(0, refreshes[0]);
    controller.refresh(() -> { fail("No duplicate prompt"); return true; });
    assertEquals(1, worker.tasks.size()); worker.drain();
    assertTrue(model.isBusy()); assertEquals(1, refreshes[0]);
    ui.drain(); assertFalse(model.isBusy()); assertEquals("user", model.getUser().getId());
  }
  private static class Tasks implements Executor {
    final Queue<Runnable> tasks = new ArrayDeque<>();
    public void execute(Runnable task) { tasks.add(task); }
    void drain() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
  }
  private static class LoadStub implements LoadStudySetsInputBoundary {
    public void list(String user) { }
    public void refresh(String user) { }
    public void open(String user, String set) { }
  }
  private static class Auth implements LoginDataAccessInterface {
    public User login(String email, char[] password, String required) throws CloudException { return null; }
    public void signup(String email, char[] password) { }
    public void forgetSession() { }
  }
}
