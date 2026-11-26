package interface_adapter.edit_study_set;

/**
 * The state of the edit study set screen: which study set is being edited, and how the
 * last edit went.
 */
public class EditStudySetState {
  private String studySetId = "";
  private String name = "";
  private String errorMessage = null;
  private boolean completed = false;

  /**
   * Starts editing a study set: clears any earlier error and completion.
   *
   * @param studySetId the id of the study set to edit
   * @param name       the study set's current name
   */
  public void select(String studySetId, String name) {
    this.studySetId = studySetId;
    this.name = name;
    this.errorMessage = null;
    this.completed = false;
  }

  /**
   * Returns the id of the study set being edited.
   *
   * @return the study set's id
   */
  public String getStudySetId() {
    return studySetId;
  }

  /**
   * Returns the name of the study set being edited.
   *
   * @return the study set's current name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the study set being edited.
   *
   * @param name the study set's current name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the error message.
   *
   * @return the error to display, or null if there is none
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Sets the error message.
   *
   * @param errorMessage the error to display, or null to clear it
   */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * Tells whether the last edit succeeded.
   *
   * @return true if the last edit succeeded
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * Sets whether the last edit succeeded.
   *
   * @param completed true if the last edit succeeded
   */
  public void setCompleted(boolean completed) {
    this.completed = completed;
  }
}