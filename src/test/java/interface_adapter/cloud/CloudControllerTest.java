package interface_adapter.cloud;

import data_access.InMemoryStudySetDataAccessObject;
import entity.StudySet;
import entity.User;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;
import use_case.cloud.CloudException;
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
  private static class Tasks implements Executor {
    final Queue<Runnable> tasks = new ArrayDeque<>();
    public void execute(Runnable task) { tasks.add(task); }
    void drain() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
  }
  private static class LoadStub implements LoadStudySetsInputBoundary {
    public void list(String user) { }
    public void open(String user, String set) { }
  }
  private static class Auth implements LoginDataAccessInterface {
    public User login(String email, char[] password, String required) throws CloudException { return null; }
    public void signup(String email, char[] password) { }
    public void forgetSession() { }
  }
}
