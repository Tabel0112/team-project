package use_case.practice_study_set;

import entity.StudySet;

/**
 * The storage operations the Practice Study Set use case needs.
 */
public interface PracticeStudySetDataAccessInterface {
  /**
   * Finds a study set by its id.
   *
   * @param studySetId the id of the study set
   * @return the study set, or null if there is no study set with that id
   */
  StudySet getStudySetById(String studySetId);
}
