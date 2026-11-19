package use_case.reorder_flashcards;

import entity.CardsList;
import entity.Flashcard;
import entity.StudySet;

import java.util.ArrayList;
import java.util.List;

/**
 * The Interactor for the Reorder Flashcards use case (Use Case 3, extension 3b).
 */
public class ReorderFlashcardsInteractor implements ReorderFlashcardsInputBoundary {
  private final ReorderFlashcardsDataAccessInterface dataAccess;
  private final ReorderFlashcardsOutputBoundary presenter;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public ReorderFlashcardsInteractor(ReorderFlashcardsDataAccessInterface dataAccess,
                                     ReorderFlashcardsOutputBoundary presenter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
  }

  /**
   * Moves a flashcard to a new position in its study set. Fails if the study set
   * cannot be found or either position is outside the study set.
   *
   * @param inputData the study set id, and the flashcard's current and new positions
   */
  @Override
  public void execute(ReorderFlashcardsInputData inputData) {
    final StudySet studySet = dataAccess.getStudySetById(inputData.getStudySetId());
    if (studySet == null) {
      presenter.prepareFailView("Could not find the study set. Please open it and try again.");
      return;
    }
    final CardsList cardsList = studySet.getCardsList();
    final int from = inputData.getFromIndex();
    final int to = inputData.getToIndex();
    if (!isPosition(from, cardsList) || !isPosition(to, cardsList)) {
      presenter.prepareFailView("That flashcard cannot be moved there.");
      return;
    }

    cardsList.moveCard(from, to);

    final List<ReorderFlashcardsOutputData.Card> cards = new ArrayList<>();
    for (Flashcard flashcard : cardsList.getCards()) {
      cards.add(new ReorderFlashcardsOutputData.Card(flashcard.getQuestion(), flashcard.getAnswer()));
    }
    presenter.prepareSuccessView(new ReorderFlashcardsOutputData(cards, to));
  }

  private static boolean isPosition(int index, CardsList cardsList) {
    return index >= 0 && index < cardsList.size();
  }
}
