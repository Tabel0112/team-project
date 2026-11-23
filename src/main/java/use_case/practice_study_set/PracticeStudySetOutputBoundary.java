package use_case.practice_study_set;

/**
 * The output boundary for the Practice Study Set use case.
 */
public interface PracticeStudySetOutputBoundary {
  /**
   * Prepares the practice view, starting on the first flashcard.
   *
   * @param outputData the study set's name and its flashcards
   */
  void prepareSuccessView(PracticeStudySetOutputData outputData);

  /**
   * Prepares the fail view for the use case.
   *
   * @param errorMessage a message explaining the problem, suitable for the user
   */
  void prepareFailView(String errorMessage);
}
