package org.example.studyapp.interface_adapter.account;

import org.example.studyapp.use_case.account.AccountInputBoundary;
import org.example.studyapp.use_case.account.AccountInputData;

public final class AccountController {
    private final AccountInputBoundary interactor;

    public AccountController(AccountInputBoundary interactor) { this.interactor = interactor; }

    public void authenticate(String email, String password, boolean signUp) throws Exception {
        interactor.authenticate(new AccountInputData(email, password, signUp));
    }

    public void signOut() { interactor.signOut(); }
}
