package use_case.login;

import java.util.Arrays;
import use_case.cloud.CloudException;

/** Validates credentials and delegates password verification to Auth. */
public final class LoginInteractor implements LoginInputBoundary {
  private final LoginDataAccessInterface auth;
  private final LoginOutputBoundary output;
  public LoginInteractor(LoginDataAccessInterface auth, LoginOutputBoundary output) {
    this.auth = auth; this.output = output;
  }
  @Override public void execute(LoginInputData input) {
    try {
      if (input.email.isEmpty() || input.password == null || input.password.length == 0) {
        output.failed("Enter your email and password.");
      } else if (input.signup) {
        auth.signup(input.email, input.password);
        output.signupSubmitted("If registration is available, check your email for confirmation, then log in.");
      } else {
        entity.User user = auth.login(input.email, input.password, input.requiredUserId);
        output.loggedIn(new LoginOutputData(user.getId(), user.getEmail()));
      }
    } catch (CloudException exception) {
      output.failed(exception.getMessage());
    } finally {
      if (input.password != null) { Arrays.fill(input.password, '\0'); }
    }
  }
}
