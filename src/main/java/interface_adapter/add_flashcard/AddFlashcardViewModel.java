package interface_adapter.add_flashcard;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * The View Model for the Add Flashcard use case.
 */
public class AddFlashcardViewModel {
  public static final String VIEW_NAME = "add flashcard";
  public static final String STATE_PROPERTY = "state";

  private final PropertyChangeSupport support = new PropertyChangeSupport(this);
  private final AddFlashcardState state = new AddFlashcardState();

  /**
   * Creates a view model with an empty form.
   */
  public AddFlashcardViewModel() { }

  /**
   * Returns the current state.
   *
   * @return the current state
   */
  public AddFlashcardState getState() { return state; }

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
