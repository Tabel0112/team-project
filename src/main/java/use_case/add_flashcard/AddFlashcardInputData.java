package use_case.add_flashcard;

/**
 * The data the Add Flashcard use case needs to run.
 */
public class AddFlashcardInputData {
  private final String studySetId;
  private final String question;
  private final String answer;

  /**
   * Creates input data.
   *
   * @param studySetId the id of the study set to add the flashcard to
   * @param question   the question for the front of the flashcard, as typed
   * @param answer     the answer for the back of the flashcard, as typed
   */
  public AddFlashcardInputData(String studySetId, String question, String answer) {
    this.studySetId = studySetId;
    this.question = question;
    this.answer = answer;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set to add the flashcard to
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns the question.
   *
   * @return the question, as typed
   */
  public String getQuestion() { return question; }

  /**
   * Returns the answer.
   *
   * @return the answer, as typed
   */
  public String getAnswer() { return answer; }
}
