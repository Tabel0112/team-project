package use_case.edit_flashcard;

/**
 * The input boundary for the Edit Flashcard use case.
 */
public interface EditFlashcardInputBoundary {
  /**
   * Executes the use case.
   *
   * @param inputData the study set id, the flashcard's position, and its new question and answer
   */
  void execute(EditFlashcardInputData inputData);
}
