package use_case.delete_flashcard;

import entity.CardsList;
import entity.Flashcard;
import entity.StudySet;

/**
 * The Interactor for the Delete Flashcard use case.
 */
public class DeleteFlashcardInteractor implements DeleteFlashcardInputBoundary {
  private final DeleteFlashcardDataAccessInterface dataAccess;
  private final DeleteFlashcardOutputBoundary presenter;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public DeleteFlashcardInteractor(DeleteFlashcardDataAccessInterface dataAccess,
                                   DeleteFlashcardOutputBoundary presenter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
  }

  /**
   * Removes the flashcard from the study set. Fails if the study set or the
   * flashcard cannot be found. The user has already confirmed the deletion.
   *
   * @param inputData the study set id and the position of the flashcard to delete
   */
  @Override
  public void execute(DeleteFlashcardInputData inputData) {
    final StudySet studySet = dataAccess.getStudySetById(inputData.getStudySetId());
    if (studySet == null) {
      presenter.prepareFailView("Could not find the study set. Please open it and try again.");
      return;
    }
    final CardsList cardsList = studySet.getCardsList();
    final int index = inputData.getCardIndex();
    if (index < 0 || index >= cardsList.size()) {
      presenter.prepareFailView("Could not find the flashcard. It may have been removed.");
      return;
    }

    final Flashcard card = cardsList.getCards().get(index);
    cardsList.removeCard(card);
    presenter.prepareSuccessView(
        new DeleteFlashcardOutputData(studySet.getId(), card.getQuestion(), cardsList.size()));
  }
}
