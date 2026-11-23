package use_case.login;

import entity.User;
import use_case.cloud.CloudException;

public interface LoginDataAccessInterface {
  User login(String email, char[] password, String requiredUserId) throws CloudException;
  void signup(String email, char[] password) throws CloudException;
  void forgetSession();
}
