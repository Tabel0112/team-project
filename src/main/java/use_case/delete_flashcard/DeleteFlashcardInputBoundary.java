package use_case.delete_flashcard;

/**
 * The input boundary for the Delete Flashcard use case.
 */
public interface DeleteFlashcardInputBoundary {
  /**
   * Executes the use case.
   *
   * @param inputData the study set id and the position of the flashcard to delete
   */
  void execute(DeleteFlashcardInputData inputData);
}
