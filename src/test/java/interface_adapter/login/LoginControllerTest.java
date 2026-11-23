package interface_adapter.login;

import entity.User;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.Test;
import use_case.login.LoginDataAccessInterface;
import use_case.login.LoginInteractor;
import static org.junit.Assert.*;

public class LoginControllerTest {
  @Test public void authRunsOnWorkerAndResultsRunOnUiWhileBusyRejectsDuplicate() {
    Tasks worker = new Tasks(), ui = new Tasks();
    LoginViewModel model = new LoginViewModel();
    Auth auth = new Auth();
    LoginController controller = new LoginController(new LoginInteractor(auth, new LoginPresenter(model, ui)),
        auth, model, worker, ui, () -> { });
    char[] password = "fixture".toCharArray();
    controller.login("a@example.test", password, false);
    assertTrue(model.isBusy()); assertEquals(0, auth.attempts); assertNull(model.getUser());
    char[] duplicate = "discard".toCharArray();
    controller.login("a@example.test", duplicate, false);
    assertEquals('\0', duplicate[0]); assertEquals(1, worker.tasks.size());
    controller.logout(); assertFalse(auth.forgot);
    worker.next(); assertEquals(1, auth.attempts); assertNull(model.getUser());
    assertEquals('\0', password[0]);
    ui.next(); assertEquals("uid", model.getUser().getId()); assertTrue(model.isBusy());
    ui.next(); assertFalse(model.isBusy());
    controller.login("a@example.test", "again".toCharArray(), false);
    worker.next(); assertEquals("uid", auth.required); ui.drain();
  }
  @Test public void logoutForgetsSessionAndClearsLocalWorkBeforePublishingAnonymousState() {
    LoginViewModel model = new LoginViewModel(); model.setUser(new User("uid", "a@example.test"));
    String oldScope = model.scope(); Auth auth = new Auth(); boolean[] cleared = {false};
    LoginController controller = new LoginController(input -> { }, auth, model,
        Runnable::run, Runnable::run, () -> { assertNotNull(model.getUser()); cleared[0] = true; });
    controller.logout();
    assertTrue(auth.forgot); assertTrue(cleared[0]); assertNull(model.getUser()); assertNull(model.scope());
    model.setUser(new User("uid", "a@example.test")); assertNotEquals(oldScope, model.scope());
  }
  private static class Auth implements LoginDataAccessInterface {
    int attempts; String required; boolean forgot;
    public User login(String email, char[] password, String required) {
      attempts++; this.required = required; return new User("uid", email);
    }
    public void signup(String email, char[] password) { }
    public void forgetSession() { forgot = true; }
  }
  private static class Tasks implements Executor {
    final Queue<Runnable> tasks = new ArrayDeque<>();
    public void execute(Runnable task) { tasks.add(task); }
    void next() { tasks.remove().run(); }
    void drain() { while (!tasks.isEmpty()) { next(); } }
  }
}
