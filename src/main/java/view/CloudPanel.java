package view;

import interface_adapter.cloud.CloudController;
import interface_adapter.cloud.CloudViewModel;
import java.awt.BorderLayout;
import java.util.function.Supplier;
import javax.swing.*;

/** Login controls and the explicit Save trigger. No network or persistence rules in the view. */
public final class CloudPanel extends JPanel {
  public CloudPanel(CloudViewModel model, CloudController controller, Supplier<String> setId,
                    Runnable onLogout) {
    super(new BorderLayout());
    JTextField email = new JTextField(22);
    JPasswordField password = new JPasswordField(14);
    JButton login = new JButton("Log in / Reauthenticate");
    JButton signup = new JButton("Sign up");
    JButton logout = new JButton("Log out");
    JButton refresh = new JButton("Refresh set list");
    JButton save = new JButton("Save open set");
    JLabel account = new JLabel("Not logged in");
    JTextArea status = new JTextArea(model.getMessage(), 2, 70);
    status.setEditable(false); status.setLineWrap(true); status.setWrapStyleWord(true);
    status.setOpaque(false); status.setFocusable(false);
    JPanel credentials = new JPanel();
    credentials.add(new JLabel("Email")); credentials.add(email);
    credentials.add(new JLabel("Password")); credentials.add(password);
    credentials.add(login); credentials.add(signup);
    JPanel actions = new JPanel();
    actions.add(account); actions.add(refresh); actions.add(save); actions.add(logout);
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
    refresh.addActionListener(event -> controller.refresh());
    save.addActionListener(event -> controller.save(setId.get()));
    Runnable update = () -> {
      boolean signedIn = model.getUser() != null;
      account.setText(signedIn ? model.getUser().getEmail() : "Not logged in");
      status.setText(model.getMessage());
      login.setEnabled(!model.isBusy()); signup.setEnabled(!model.isBusy() && !signedIn);
      email.setEnabled(!model.isBusy()); password.setEnabled(!model.isBusy());
      logout.setEnabled(!model.isBusy() && signedIn);
      refresh.setEnabled(!model.isBusy() && signedIn);
      save.setEnabled(!model.isBusy() && signedIn);
    };
    model.addPropertyChangeListener(event -> update.run()); update.run();
  }
}
