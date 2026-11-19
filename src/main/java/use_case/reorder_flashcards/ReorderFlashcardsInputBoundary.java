package use_case.reorder_flashcards;

/**
 * The input boundary for the Reorder Flashcards use case.
 */
public interface ReorderFlashcardsInputBoundary {
  /**
   * Executes the use case.
   *
   * @param inputData the study set id, and the flashcard's current and new positions
   */
  void execute(ReorderFlashcardsInputData inputData);
}
