package org.example.studyapp.use_case.account;

public final class AccountInteractor implements AccountInputBoundary {
    private final AccountDataAccessInterface accounts;

    public AccountInteractor(AccountDataAccessInterface accounts) { this.accounts = accounts; }

    @Override public void authenticate(AccountInputData input) throws Exception {
        if (input.email == null || input.email.isBlank() || input.password == null || input.password.isBlank())
            throw new IllegalArgumentException("Enter an email and password.");
        accounts.authenticate(input.email, input.password, input.signUp);
    }

    @Override public void signOut() { accounts.signOut(); }
}
