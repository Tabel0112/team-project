package org.example.studyapp.use_case.account;

public final class AccountInputData {
    public final String email;
    public final String password;
    public final boolean signUp;

    public AccountInputData(String email, String password, boolean signUp) {
        this.email = email;
        this.password = password;
        this.signUp = signUp;
    }
}
