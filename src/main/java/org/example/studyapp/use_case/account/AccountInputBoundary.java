package org.example.studyapp.use_case.account;

public interface AccountInputBoundary {
    void authenticate(AccountInputData input) throws Exception;
    void signOut();
}
