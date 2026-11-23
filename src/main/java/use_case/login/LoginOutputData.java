package use_case.login;

/** Authenticated identity fields intended for presentation. No credentials or tokens. */
public final class LoginOutputData {
  public final String userId;
  public final String email;
  public LoginOutputData(String userId, String email) { this.userId = userId; this.email = email; }
}
