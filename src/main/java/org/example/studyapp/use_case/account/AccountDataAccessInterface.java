package org.example.studyapp.use_case.account;

public interface AccountDataAccessInterface {
    void authenticate(String email, String password, boolean signUp) throws Exception;
    void signOut();
}
