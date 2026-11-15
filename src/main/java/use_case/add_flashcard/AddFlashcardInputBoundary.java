package use_case.add_flashcard;

/**
 * The input boundary for the Add Flashcard use case.
 */
public interface AddFlashcardInputBoundary {
  /**
   * Executes the use case.
   *
   * @param inputData the study set id, question and answer
   */
  void execute(AddFlashcardInputData inputData);
}
