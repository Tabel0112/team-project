package interface_adapter.add_flashcard;

import use_case.add_flashcard.AddFlashcardInputBoundary;
import use_case.add_flashcard.AddFlashcardInputData;

/**
 * The Controller for the Add Flashcard use case.
 */
public class AddFlashcardController {
  private final AddFlashcardInputBoundary interactor;

  /**
   * Creates the controller.
   *
   * @param interactor the use case to call
   */
  public AddFlashcardController(AddFlashcardInputBoundary interactor) {
    this.interactor = interactor;
  }

  /**
   * Asks the use case to add a flashcard to a study set.
   *
   * @param studySetId the id of the open study set
   * @param question   the question, as typed
   * @param answer     the answer, as typed
   */
  public void execute(String studySetId, String question, String answer) {
    interactor.execute(new AddFlashcardInputData(studySetId, question, answer));
  }
}
