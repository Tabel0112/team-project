package use_case.edit_study_set;

/**
 * The data the Edit Study Set use case needs to run.
 */
public class EditStudySetInputData {
  private final String userId;
  private final String studySetId;
  private final EditStudySetAction action;
  private final String newName;

  /**
   * Creates input data.
   *
   * @param userId     the id of the logged-in user
   * @param studySetId the id of the study set to edit
   * @param action     the editing option the user chose
   * @param newName    the new name, as typed; only used by {@link EditStudySetAction#RENAME}
   */
  public EditStudySetInputData(String userId, String studySetId,
                               EditStudySetAction action, String newName) {
    this.userId = userId;
    this.studySetId = studySetId;
    this.action = action;
    this.newName = newName;
  }

  /**
   * Returns the user id.
   *
   * @return the id of the logged-in user
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Returns the id of the study set to edit.
   *
   * @return the study set's id
   */
  public String getStudySetId() {
    return studySetId;
  }

  /**
   * Returns the chosen editing option.
   *
   * @return the action to perform
   */
  public EditStudySetAction getAction() {
    return action;
  }

  /**
   * Returns the new name.
   *
   * @return the new name as typed, or null if the action is not a rename
   */
  public String getNewName() {
    return newName;
  }
}