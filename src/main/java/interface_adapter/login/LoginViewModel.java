package interface_adapter.login;

import entity.User;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Authentication display state. Mutated only on Swing's event thread. */
public final class LoginViewModel {
  private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
  private User user;
  private boolean busy;
  private String message = "Log in to load your study sets. Local changes are saved only when you click Save All.";
  private long generation;
  public User getUser() { return user; }
  public boolean isBusy() { return busy; }
  public String getMessage() { return message; }
  public String scope() { return user == null ? null : user.getId() + ":" + generation; }
  public void setUser(User user) {
    // Reauthentication still invalidates callbacks started in the earlier session.
    this.user = user; generation++; fire();
  }
  public void setBusy(boolean busy) { this.busy = busy; fire(); }
  public void message(String message) { this.message = message; fire(); }
  private void fire() { changes.firePropertyChange("state", null, this); }
  public void addPropertyChangeListener(PropertyChangeListener listener) { changes.addPropertyChangeListener(listener); }
}
