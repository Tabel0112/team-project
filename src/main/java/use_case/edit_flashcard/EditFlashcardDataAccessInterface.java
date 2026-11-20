package use_case.edit_flashcard;

import entity.StudySet;

/**
 * The storage operations the Edit Flashcard use case needs.
 */
public interface EditFlashcardDataAccessInterface {
  /**
   * Finds a study set by its id.
   *
   * @param studySetId the id of the study set
   * @return the study set, or null if there is no study set with that id
   */
  StudySet getStudySetById(String studySetId);
}
