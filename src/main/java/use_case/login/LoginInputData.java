package use_case.login;

/** Password exists only for the duration of an authentication attempt. */
public final class LoginInputData {
  public final String email;
  public final char[] password;
  public final boolean signup;
  public final String requiredUserId;

  public LoginInputData(String email, char[] password, boolean signup, String requiredUserId) {
    this.email = email == null ? "" : email.trim();
    this.password = password;
    this.signup = signup;
    this.requiredUserId = requiredUserId;
  }
}
