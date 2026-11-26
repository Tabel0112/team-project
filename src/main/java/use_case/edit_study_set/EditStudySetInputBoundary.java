package use_case.edit_study_set;

/**
 * The input boundary for the Edit Study Set use case.
 */
public interface EditStudySetInputBoundary {
  /**
   * Runs the use case.
   *
   * @param inputData the study set, the chosen action, and the new name if renaming
   */
  void execute(EditStudySetInputData inputData);
}