package interface_adapter.edit_study_set;

import use_case.edit_study_set.EditStudySetAction;
import use_case.edit_study_set.EditStudySetInputBoundary;
import use_case.edit_study_set.EditStudySetInputData;

/**
 * Receives requests from the view and passes them to the Edit Study Set use case.
 */
public class EditStudySetController {
  private final EditStudySetInputBoundary interactor;

  /**
   * Creates the controller.
   *
   * @param interactor the use case to call
   */
  public EditStudySetController(EditStudySetInputBoundary interactor) {
    this.interactor = interactor;
  }

  /**
   * Asks the use case to rename a study set.
   *
   * @param userId     the id of the logged-in user
   * @param studySetId the id of the study set
   * @param newName    the new name, as typed
   */
  public void rename(String userId, String studySetId, String newName) {
    execute(userId, studySetId, EditStudySetAction.RENAME, newName);
  }

  /**
   * Asks the use case to move a study set up one place.
   *
   * @param userId     the id of the logged-in user
   * @param studySetId the id of the study set
   */
  public void moveUp(String userId, String studySetId) {
    execute(userId, studySetId, EditStudySetAction.MOVE_UP, null);
  }

  /**
   * Asks the use case to move a study set down one place.
   *
   * @param userId     the id of the logged-in user
   * @param studySetId the id of the study set
   */
  public void moveDown(String userId, String studySetId) {
    execute(userId, studySetId, EditStudySetAction.MOVE_DOWN, null);
  }

  /**
   * Asks the use case to delete a study set. Confirming with the user is the view's job.
   *
   * @param userId     the id of the logged-in user
   * @param studySetId the id of the study set
   */
  public void delete(String userId, String studySetId) {
    execute(userId, studySetId, EditStudySetAction.DELETE, null);
  }

  private void execute(String userId, String studySetId,
                       EditStudySetAction action, String newName) {
    interactor.execute(new EditStudySetInputData(userId, studySetId, action, newName));
  }
}