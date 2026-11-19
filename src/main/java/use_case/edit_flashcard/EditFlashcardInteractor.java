package use_case.edit_flashcard;

import entity.CardsList;
import entity.Flashcard;
import entity.StudySet;

/**
 * The Interactor for the Edit Flashcard use case.
 */
public class EditFlashcardInteractor implements EditFlashcardInputBoundary {
  private final EditFlashcardDataAccessInterface dataAccess;
  private final EditFlashcardOutputBoundary presenter;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public EditFlashcardInteractor(EditFlashcardDataAccessInterface dataAccess,
                                 EditFlashcardOutputBoundary presenter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
  }

  /**
   * Replaces the flashcard's question and answer if the request is valid. Fails if
   * the question or answer is empty, or the study set or flashcard cannot be found.
   *
   * @param inputData the study set id, the flashcard's position, and its new question and answer
   */
  @Override
  public void execute(EditFlashcardInputData inputData) {
    final String question = trim(inputData.getQuestion());
    final String answer = trim(inputData.getAnswer());
    if (question.isEmpty()) {                               // extension 5a
      presenter.prepareFailView("Please enter a question for the front of the flashcard.");
      return;
    }
    if (answer.isEmpty()) {                                 // extension 5a
      presenter.prepareFailView("Please enter an answer for the back of the flashcard.");
      return;
    }

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

    cardsList.replaceCard(index, new Flashcard(question, answer));     // step 6
    presenter.prepareSuccessView(
        new EditFlashcardOutputData(studySet.getId(), index, question, answer));
  }

  private static String trim(String text) {
    return text == null ? "" : text.trim();
  }
}
