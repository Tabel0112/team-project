package interface_adapter.login;

import entity.User;
import java.util.concurrent.Executor;
import use_case.login.LoginOutputBoundary;
import use_case.login.LoginOutputData;

/** Marshals authentication results onto the UI thread. */
public final class LoginPresenter implements LoginOutputBoundary {
  private final LoginViewModel model;
  private final Executor ui;

  public LoginPresenter(LoginViewModel model, Executor ui) {
    this.model = model;
    this.ui = ui;
  }
  @Override public void loggedIn(LoginOutputData output) {
    ui.execute(() -> {
      model.setUser(new User(output.userId, output.email));
      model.message("Logged in. Local working copies are ready.");
    });
  }
  @Override public void signupSubmitted(String message) { ui.execute(() -> model.message(message)); }
  @Override public void failed(String message) { ui.execute(() -> model.message(message)); }
}
