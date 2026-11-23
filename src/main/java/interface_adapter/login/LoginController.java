package interface_adapter.login;

import java.util.Arrays;
import java.util.concurrent.Executor;
import use_case.login.LoginDataAccessInterface;
import use_case.login.LoginInputBoundary;
import use_case.login.LoginInputData;

/** Captures login input on the UI thread and runs authentication on a serial worker. */
public final class LoginController {
  private final LoginInputBoundary login;
  private final LoginDataAccessInterface auth;
  private final LoginViewModel model;
  private final Executor worker;
  private final Executor ui;
  private final Runnable clearLocal;
  private final Runnable authenticated;

  public LoginController(LoginInputBoundary login, LoginDataAccessInterface auth,
      LoginViewModel model, Executor worker, Executor ui, Runnable clearLocal) {
    this(login, auth, model, worker, ui, clearLocal, () -> { });
  }

  public LoginController(LoginInputBoundary login, LoginDataAccessInterface auth,
      LoginViewModel model, Executor worker, Executor ui, Runnable clearLocal, Runnable authenticated) {
    this.authenticated = authenticated;
    this.login = login;
    this.auth = auth;
    this.model = model;
    this.worker = worker;
    this.ui = ui;
    this.clearLocal = clearLocal;
  }

  public void login(String email, char[] password, boolean signup) {
    if (model.isBusy()) {
      if (password != null) { Arrays.fill(password, '\0'); }
      return;
    }
    String previousScope = model.scope();
    String requiredUserId = model.getUser() == null ? null : model.getUser().getId();
    LoginInputData input = new LoginInputData(email, password, signup, requiredUserId);
    model.message("Contacting authentication...");
    model.setBusy(true);
    try {
      worker.execute(() -> {
        try { login.execute(input); }
        catch (RuntimeException exception) {
          ui.execute(() -> model.message("Authentication could not finish. Retry when ready."));
        } finally {
          if (password != null) { Arrays.fill(password, '\0'); }
          ui.execute(() -> {
            model.setBusy(false);
            if (!signup && !java.util.Objects.equals(previousScope, model.scope())) {
              authenticated.run();
            }
          });
        }
      });
    } catch (RuntimeException exception) {
      if (password != null) { Arrays.fill(password, '\0'); }
      model.setBusy(false);
      model.message("Authentication could not start. Retry when ready.");
    }
  }

  /** Called only after the application has confirmed discarding local work. */
  public void logout() {
    if (model.isBusy()) { return; }
    auth.forgetSession();
    clearLocal.run();
    model.setUser(null);
    model.message("Logged out. Log in to load your saved study sets.");
  }
}
