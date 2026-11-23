package interface_adapter.practice_study_set;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * The View Model for the Practice Study Set use case.
 */
public class PracticeStudySetViewModel {
  public static final String VIEW_NAME = "practice study set";
  public static final String STATE_PROPERTY = "state";

  private final PropertyChangeSupport support = new PropertyChangeSupport(this);
  private final PracticeStudySetState state = new PracticeStudySetState();

  /**
   * Creates a view model with no session started.
   */
  public PracticeStudySetViewModel() { }

  /**
   * Returns the current state.
   *
   * @return the current state
   */
  public PracticeStudySetState getState() { return state; }

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
