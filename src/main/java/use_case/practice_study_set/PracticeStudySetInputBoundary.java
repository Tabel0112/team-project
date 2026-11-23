package use_case.practice_study_set;

/**
 * The input boundary for the Practice Study Set use case.
 */
public interface PracticeStudySetInputBoundary {
  /**
   * Starts a practice session.
   *
   * @param inputData the id of the study set to practice
   */
  void execute(PracticeStudySetInputData inputData);
}
