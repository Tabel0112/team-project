package use_case.edit_study_set;

import entity.StudySet;

import java.util.List;

/**
 * The storage operations the Edit Study Set use case needs.
 */
public interface EditStudySetDataAccessInterface {
  /**
   * Returns all study sets belonging to a user.
   *
   * @param userId the id of the user
   * @return the user's study sets, in list order (empty if none)
   */
  List<StudySet> getStudySets(String userId);

  /**
   * Deletes a study set.
   *
   * @param userId     the id of the user who owns the study set
   * @param studySetId the id of the study set to delete
   */
  void delete(String userId, String studySetId);

  /**
   * Changes a study set's name, keeping its id, position and flashcards.
   *
   * @param userId     the id of the user who owns the study set
   * @param studySetId the id of the study set to rename
   * @param newName    the new name
   */
  void rename(String userId, String studySetId, String newName);

  /**
   * Moves a study set to another position in the user's list.
   *
   * @param userId     the id of the user who owns the study set
   * @param studySetId the id of the study set to move
   * @param newIndex   the position it should have afterwards, counting from 0
   */
  void move(String userId, String studySetId, int newIndex);
}