package use_case.reorder_flashcards;

/**
 * The output boundary for the Reorder Flashcards use case.
 */
public interface ReorderFlashcardsOutputBoundary {
  /**
   * Prepares the success view for the use case.
   *
   * @param outputData the flashcards in their new order and where the moved one ended up
   */
  void prepareSuccessView(ReorderFlashcardsOutputData outputData);

  /**
   * Prepares the fail view for the use case.
   *
   * @param errorMessage a message explaining the problem, suitable for the user
   */
  void prepareFailView(String errorMessage);
}
