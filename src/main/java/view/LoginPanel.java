package view;

import interface_adapter.login.LoginController;
import interface_adapter.login.LoginViewModel;
import java.awt.BorderLayout;
import javax.swing.*;

/** Login controls. Network and session rules stay outside the view. */
public final class LoginPanel extends JPanel {
  public LoginPanel(LoginViewModel model, LoginController controller, Runnable onLogout) {
    super(new BorderLayout());
    JTextField email = new JTextField(22);
    JPasswordField password = new JPasswordField(14);
    JButton login = new JButton("Log in / Reauthenticate");
    JButton signup = new JButton("Sign up");
    JButton logout = new JButton("Log out");
    JLabel account = new JLabel("Not logged in");
    JTextArea status = new JTextArea(model.getMessage(), 2, 70);
    status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true);
    status.setOpaque(false); status.setFocusable(false);
    JPanel credentials = new JPanel();
    credentials.add(new JLabel("Email")); credentials.add(email);
    credentials.add(new JLabel("Password")); credentials.add(password);
    credentials.add(login); credentials.add(signup);
    JPanel actions = new JPanel();
    actions.add(account); actions.add(logout);
    JPanel rows = new JPanel(new BorderLayout());
    rows.add(credentials, BorderLayout.NORTH); rows.add(actions, BorderLayout.SOUTH);
    add(rows, BorderLayout.NORTH); add(status, BorderLayout.SOUTH);
    login.addActionListener(event -> {
      char[] chars = password.getPassword(); password.setText("");
      controller.login(email.getText(), chars, false);
    });
    signup.addActionListener(event -> {
      char[] chars = password.getPassword(); password.setText("");
      controller.login(email.getText(), chars, true);
    });
    logout.addActionListener(event -> onLogout.run());
    Runnable update = () -> {
      boolean signedIn = model.getUser() != null;
      account.setText(signedIn ? model.getUser().getEmail() : "Not logged in");
      status.setText(model.getMessage());
      login.setEnabled(!model.isBusy()); signup.setEnabled(!model.isBusy() && !signedIn);
      email.setEnabled(!model.isBusy()); password.setEnabled(!model.isBusy());
      logout.setEnabled(!model.isBusy() && signedIn);
    };
    model.addPropertyChangeListener(event -> update.run()); update.run();
  }
}
