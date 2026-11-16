package interface_adapter.view_flashcards;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * The View Model for the View Flashcards use case.
 */
public class ViewFlashcardsViewModel {
  public static final String STATE_PROPERTY = "state";

  private final PropertyChangeSupport support = new PropertyChangeSupport(this);
  private final ViewFlashcardsState state = new ViewFlashcardsState();

  /**
   * Creates a view model with no flashcards.
   */
  public ViewFlashcardsViewModel() { }

  /**
   * Returns the current state.
   *
   * @return the current state
   */
  public ViewFlashcardsState getState() { return state; }

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
