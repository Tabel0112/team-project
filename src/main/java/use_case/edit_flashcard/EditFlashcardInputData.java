package use_case.edit_flashcard;

/**
 * The data the Edit Flashcard use case needs to run.
 */
public class EditFlashcardInputData {
  private final String studySetId;
  private final int cardIndex;
  private final String question;
  private final String answer;

  /**
   * Creates input data.
   *
   * @param studySetId the id of the study set the flashcard belongs to
   * @param cardIndex  the position of the flashcard in the study set, starting at 0
   * @param question   the new question for the front of the flashcard, as typed
   * @param answer     the new answer for the back of the flashcard, as typed
   */
  public EditFlashcardInputData(String studySetId, int cardIndex, String question, String answer) {
    this.studySetId = studySetId;
    this.cardIndex = cardIndex;
    this.question = question;
    this.answer = answer;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set the flashcard belongs to
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns the flashcard's position.
   *
   * @return the position of the flashcard in the study set, starting at 0
   */
  public int getCardIndex() { return cardIndex; }

  /**
   * Returns the new question.
   *
   * @return the question, as typed
   */
  public String getQuestion() { return question; }

  /**
   * Returns the new answer.
   *
   * @return the answer, as typed
   */
  public String getAnswer() { return answer; }
}
