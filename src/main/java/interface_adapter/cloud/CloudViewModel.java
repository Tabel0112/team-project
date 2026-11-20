package interface_adapter.cloud;

import entity.User;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Authentication and UC5 display state. Mutated only on Swing's event thread. */
public final class CloudViewModel {
  private final PropertyChangeSupport changes = new PropertyChangeSupport(this);
  private User user;
  private boolean busy;
  private String message = "Log in to load your study sets. Local drafts are saved only when you click Save.";
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
