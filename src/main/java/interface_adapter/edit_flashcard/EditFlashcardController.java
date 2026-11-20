package interface_adapter.edit_flashcard;

import use_case.edit_flashcard.EditFlashcardInputBoundary;
import use_case.edit_flashcard.EditFlashcardInputData;

/**
 * The Controller for the Edit Flashcard use case.
 */
public class EditFlashcardController {
  private final EditFlashcardInputBoundary interactor;

  /**
   * Creates the controller.
   *
   * @param interactor the use case to call
   */
  public EditFlashcardController(EditFlashcardInputBoundary interactor) {
    this.interactor = interactor;
  }

  /**
   * Asks the use case to save changes to a flashcard.
   *
   * @param studySetId the id of the open study set
   * @param cardIndex  the position of the flashcard being edited, starting at 0
   * @param question   the new question, as typed
   * @param answer     the new answer, as typed
   */
  public void execute(String studySetId, int cardIndex, String question, String answer) {
    interactor.execute(new EditFlashcardInputData(studySetId, cardIndex, question, answer));
  }
}
