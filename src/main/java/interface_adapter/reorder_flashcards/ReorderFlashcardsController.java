package interface_adapter.reorder_flashcards;

import use_case.reorder_flashcards.ReorderFlashcardsInputBoundary;
import use_case.reorder_flashcards.ReorderFlashcardsInputData;

/**
 * The Controller for the Reorder Flashcards use case.
 */
public class ReorderFlashcardsController {
  private final ReorderFlashcardsInputBoundary interactor;

  /**
   * Creates the controller.
   *
   * @param interactor the use case to call
   */
  public ReorderFlashcardsController(ReorderFlashcardsInputBoundary interactor) {
    this.interactor = interactor;
  }

  /**
   * Asks the use case to move a flashcard to a new position.
   *
   * @param studySetId the id of the open study set
   * @param fromIndex  the flashcard's current position, starting at 0
   * @param toIndex    the position to move it to, starting at 0
   */
  public void execute(String studySetId, int fromIndex, int toIndex) {
    interactor.execute(new ReorderFlashcardsInputData(studySetId, fromIndex, toIndex));
  }
}
