package interface_adapter.cloud;

import data_access.InMemoryStudySetDataAccessObject;
import entity.*;
import interface_adapter.ViewManagerModel;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.study_set.StudySetViewModel;
import java.util.*;
import java.util.concurrent.Executor;
import org.junit.Test;
import use_case.login.*;
import use_case.save_study_set.*;
import use_case.load_study_sets.*;
import use_case.cloud.*;
import static org.junit.Assert.*;

public class CloudControllerTest {
  @Test public void dispatchIsAsyncAndBusyRejectsDuplicateSaveAndLogout() {
    CloudViewModel model = new CloudViewModel(); model.setUser(new User("user", "email"));
    QueueExecutor worker = new QueueExecutor(), ui = new QueueExecutor();
    InMemoryStudySetDataAccessObject local = new InMemoryStudySetDataAccessObject();
    local.save("user", new StudySet("set", "Course"));
    final int[] calls = {0};
    CloudController controller = new CloudController(input -> fail(), input -> { calls[0]++; },
        new LoadStub(), new AuthStub(), local, model, worker, ui);
    controller.save("set");
    assertTrue(model.isBusy()); assertEquals(0, calls[0]);
    controller.save("different"); controller.logout();
    assertEquals(1, worker.tasks.size()); assertTrue(local.hasUnsaved("user"));
    worker.drain(); assertEquals(1, calls[0]); assertTrue(model.isBusy());
    ui.drain(); assertFalse(model.isBusy()); assertEquals("user", model.getUser().getId());
  }
  @Test public void successfulLoginUsesPresenterUiQueueThenLoadsListAndFailedReauthDoesNotHideError() {
    CloudViewModel model = new CloudViewModel();
    QueueExecutor worker = new QueueExecutor(), ui = new QueueExecutor();
    CloudPresenter presenter = new CloudPresenter(model, new CreateStudySetViewModel(),
        new StudySetViewModel(), new ViewManagerModel(), ui);
    final int[] lists = {0}; final boolean[] failAuth = {false};
    AuthStub auth = new AuthStub() {
      @Override public User login(String email, char[] password, String required) throws CloudException {
        if (failAuth[0]) { throw new CloudException("Reauthentication failed; drafts kept"); }
        return new User("user", email);
      }
    };
    CloudController controller = new CloudController(new LoginInteractor(auth, presenter), input -> { },
        new LoadStub() { @Override public void list(String user) { lists[0]++; } },
        auth, new InMemoryStudySetDataAccessObject(), model, worker, ui);
    controller.login("student@example.test", "example".toCharArray(), false);
    worker.drain(); assertNull(model.getUser()); ui.drain();
    assertEquals("user", model.getUser().getId()); assertTrue(model.isBusy());
    worker.drain(); ui.drain(); assertEquals(1, lists[0]); assertFalse(model.isBusy());
    failAuth[0] = true;
    controller.login("student@example.test", "example".toCharArray(), false);
    worker.drain(); ui.drain();
    assertEquals(1, lists[0]); assertEquals("Reauthentication failed; drafts kept", model.getMessage());
  }
  private static class QueueExecutor implements Executor {
    final Queue<Runnable> tasks = new ArrayDeque<>();
    public void execute(Runnable runnable) { tasks.add(runnable); }
    void drain() { while (!tasks.isEmpty()) { tasks.remove().run(); } }
  }
  private static class LoadStub implements LoadStudySetsInputBoundary {
    public void list(String user) { }
    public void open(String user, String set) { }
  }
  private static class AuthStub implements LoginDataAccessInterface {
    public User login(String email, char[] password, String required) throws CloudException { return null; }
    public void signup(String email, char[] password) { }
    public void forgetSession() { }
  }
}
