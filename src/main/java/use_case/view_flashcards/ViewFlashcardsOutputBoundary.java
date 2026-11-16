package use_case.view_flashcards;

/**
 * The output boundary for the View Flashcards use case.
 */
public interface ViewFlashcardsOutputBoundary {
  /**
   * Prepares the success view for the use case.
   *
   * @param outputData the study set's name and its flashcards
   */
  void prepareSuccessView(ViewFlashcardsOutputData outputData);

  /**
   * Prepares the fail view for the use case.
   *
   * @param errorMessage a message explaining the problem, suitable for the user
   */
  void prepareFailView(String errorMessage);
}
