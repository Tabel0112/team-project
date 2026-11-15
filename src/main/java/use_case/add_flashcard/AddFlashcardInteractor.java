package use_case.add_flashcard;

import entity.Flashcard;
import entity.StudySet;

/**
 * The Interactor for the Add Flashcard use case.
 */
public class AddFlashcardInteractor implements AddFlashcardInputBoundary {
  private final AddFlashcardDataAccessInterface dataAccess;
  private final AddFlashcardOutputBoundary presenter;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public AddFlashcardInteractor(AddFlashcardDataAccessInterface dataAccess,
                                AddFlashcardOutputBoundary presenter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
  }

  /**
   * Adds a flashcard to the study set if the request is valid. Fails if the
   * question or answer is empty, or the study set cannot be found.
   *
   * @param inputData the study set id, question and answer
   */
  @Override
  public void execute(AddFlashcardInputData inputData) {
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

    studySet.getCardsList().addCard(new Flashcard(question, answer));   // step 6
    presenter.prepareSuccessView(
        new AddFlashcardOutputData(studySet.getId(), studySet.getCardsList().size()));
  }

  private static String trim(String text) {
    return text == null ? "" : text.trim();
  }
}
