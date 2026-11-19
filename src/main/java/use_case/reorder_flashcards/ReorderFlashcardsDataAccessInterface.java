package use_case.reorder_flashcards;

import entity.StudySet;

/**
 * The storage operations the Reorder Flashcards use case needs.
 */
public interface ReorderFlashcardsDataAccessInterface {
  /**
   * Finds a study set by its id.
   *
   * @param studySetId the id of the study set
   * @return the study set, or null if there is no study set with that id
   */
  StudySet getStudySetById(String studySetId);
}
