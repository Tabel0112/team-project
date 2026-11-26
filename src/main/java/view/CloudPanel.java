package view;

import interface_adapter.cloud.CloudController;
import interface_adapter.login.LoginViewModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Explicit cloud actions; local study-set controls stay in their existing views. */
public final class CloudPanel extends JPanel {
  public CloudPanel(LoginViewModel model, CloudController controller) {
    JButton refresh = new JButton("Refresh set list");
    JButton save = new JButton("Save All");
    add(new JLabel("Work locally, then click Save All to commit changes to your account."));
    add(refresh); add(save);
    refresh.addActionListener(event -> controller.refresh());
    save.addActionListener(event -> controller.saveAll());
    Runnable update = () -> {
      boolean enabled = model.getUser() != null && !model.isBusy();
      refresh.setEnabled(enabled); save.setEnabled(enabled);
    };
    model.addPropertyChangeListener(event -> update.run()); update.run();
  }
}
