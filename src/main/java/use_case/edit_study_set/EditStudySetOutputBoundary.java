package use_case.edit_study_set;

/**
 * The Output Boundary for the Edit Study Set use case.
 */
public interface EditStudySetOutputBoundary {
  /**
   * Reports that the edit was done.
   *
   * @param outputData what was done and the user's updated list of study sets
   */
  void prepareSuccessView(EditStudySetOutputData outputData);

  /**
   * Reports that the edit could not be done.
   *
   * @param errorMessage a message explaining the problem, suitable for the user
   */
  void prepareFailView(String errorMessage);
}