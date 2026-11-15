package use_case.add_flashcard;

import entity.StudySet;

/**
 * The storage operations the Add Flashcard use case needs.
 */
public interface AddFlashcardDataAccessInterface {
  /**
   * Finds a study set by its id.
   *
   * @param studySetId the id of the study set
   * @return the study set, or null if there is no study set with that id
   */
  StudySet getStudySetById(String studySetId);
}
