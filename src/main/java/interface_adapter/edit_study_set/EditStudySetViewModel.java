package interface_adapter.edit_study_set;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Holds the {@link EditStudySetState} and notifies the edit study set view when it changes.
 */
public class EditStudySetViewModel {
  public static final String VIEW_NAME = "edit study set";
  public static final String STATE_PROPERTY = "state";

  private final PropertyChangeSupport support = new PropertyChangeSupport(this);
  private final EditStudySetState state = new EditStudySetState();

  /**
   * Returns the current state.
   *
   * @return the current state
   */
  public EditStudySetState getState() {
    return state;
  }

  /**
   * Notifies all listeners that the state has changed.
   */
  public void firePropertyChanged() {
    support.firePropertyChange(STATE_PROPERTY, null, state);
  }

  /**
   * Registers a listener for state changes.
   *
   * @param listener the listener to notify when the state changes
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    support.addPropertyChangeListener(listener);
  }
}