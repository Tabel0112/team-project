package use_case.login;
public interface LoginOutputBoundary {
  void loggedIn(LoginOutputData user);
  void signupSubmitted(String message);
  void failed(String message);
}
