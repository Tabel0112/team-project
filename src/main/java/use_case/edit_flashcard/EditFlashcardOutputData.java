package use_case.edit_flashcard;

/**
 * The Output Data for the Edit Flashcard use case.
 */
public class EditFlashcardOutputData {
  private final String studySetId;
  private final int cardIndex;
  private final String question;
  private final String answer;

  /**
   * Creates output data.
   *
   * @param studySetId the id of the study set the flashcard belongs to
   * @param cardIndex  the position of the edited flashcard, starting at 0
   * @param question   the question as saved
   * @param answer     the answer as saved
   */
  public EditFlashcardOutputData(String studySetId, int cardIndex, String question, String answer) {
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
   * @return the position of the edited flashcard, starting at 0
   */
  public int getCardIndex() { return cardIndex; }

  /**
   * Returns the saved question.
   *
   * @return the question as saved
   */
  public String getQuestion() { return question; }

  /**
   * Returns the saved answer.
   *
   * @return the answer as saved
   */
  public String getAnswer() { return answer; }
}
