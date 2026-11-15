package use_case.add_flashcard;

/**
 * The output boundary for the Add Flashcard use case.
 */
public interface AddFlashcardOutputBoundary {
  /**
   * Prepares the success view for the use case.
   *
   * @param outputData the study set's id and its new number of flashcards
   */
  void prepareSuccessView(AddFlashcardOutputData outputData);

  /**
   * Prepares the fail view for the use case.
   *
   * @param errorMessage a message explaining the problem, suitable for the user
   */
  void prepareFailView(String errorMessage);
}
