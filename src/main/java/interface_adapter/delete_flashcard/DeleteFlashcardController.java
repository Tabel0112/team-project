package interface_adapter.delete_flashcard;

import use_case.delete_flashcard.DeleteFlashcardInputBoundary;
import use_case.delete_flashcard.DeleteFlashcardInputData;

/**
 * The Controller for the Delete Flashcard use case.
 */
public class DeleteFlashcardController {
  private final DeleteFlashcardInputBoundary interactor;

  /**
   * Creates the controller.
   *
   * @param interactor the use case to call
   */
  public DeleteFlashcardController(DeleteFlashcardInputBoundary interactor) {
    this.interactor = interactor;
  }

  /**
   * Asks the use case to delete a flashcard from a study set.
   *
   * @param studySetId the id of the open study set
   * @param cardIndex  the position of the flashcard to delete, starting at 0
   */
  public void execute(String studySetId, int cardIndex) {
    interactor.execute(new DeleteFlashcardInputData(studySetId, cardIndex));
  }
}
