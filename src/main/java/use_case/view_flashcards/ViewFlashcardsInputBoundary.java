package use_case.view_flashcards;

/**
 * The input boundary for the View Flashcards use case.
 */
public interface ViewFlashcardsInputBoundary {
  /**
   * Executes the use case.
   *
   * @param inputData the id of the study set whose flashcards to show
   */
  void execute(ViewFlashcardsInputData inputData);
}
