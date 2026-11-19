package use_case.edit_flashcard;

/**
 * The output boundary for the Edit Flashcard use case.
 */
public interface EditFlashcardOutputBoundary {
  /**
   * Prepares the success view for the use case.
   *
   * @param outputData the study set's id and the flashcard as saved
   */
  void prepareSuccessView(EditFlashcardOutputData outputData);

  /**
   * Prepares the fail view for the use case.
   *
   * @param errorMessage a message explaining the problem, suitable for the user
   */
  void prepareFailView(String errorMessage);
}
