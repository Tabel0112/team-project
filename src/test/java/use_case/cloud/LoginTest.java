package use_case.cloud;

import entity.User;
import org.junit.Test;
import use_case.login.*;
import static org.junit.Assert.*;

public class LoginTest {
  @Test public void passesRequiredIdentityAndWipesPasswordAfterSuccess() {
    char[] password = "example-only".toCharArray();
    Output output = new Output();
    new LoginInteractor(new Auth() {
      @Override public User login(String email, char[] chars, String required) {
        assertEquals("student@example.test", email); assertEquals("uid", required);
        assertEquals("example-only", new String(chars)); return new User("uid", email);
      }
    }, output).execute(new LoginInputData(" student@example.test ", password, false, "uid"));
    assertEquals("uid", output.user.userId); assertEquals('\0', password[0]);
  }
  @Test public void validationAndBoundaryFailureWipePasswordAndKeepErrorSanitized() {
    Output output = new Output(); char[] password = "example".toCharArray();
    new LoginInteractor(new Auth() {
      @Override public User login(String email, char[] chars, String required) throws CloudException {
        throw new CloudException("Try again");
      }
    }, output).execute(new LoginInputData("a@example.test", password, false, null));
    assertEquals("Try again", output.error); assertEquals('\0', password[0]);
    new LoginInteractor(new Auth(), output).execute(new LoginInputData("", password, false, null));
    assertTrue(output.error.contains("Enter"));
  }
  private static class Auth implements LoginDataAccessInterface {
    public User login(String email, char[] password, String required) throws CloudException { fail(); return null; }
    public void signup(String email, char[] password) { }
    public void forgetSession() { }
  }
  private static class Output implements LoginOutputBoundary {
    LoginOutputData user; String error;
    public void loggedIn(LoginOutputData data) { user = data; }
    public void signupSubmitted(String message) { }
    public void failed(String message) { error = message; }
  }
}
