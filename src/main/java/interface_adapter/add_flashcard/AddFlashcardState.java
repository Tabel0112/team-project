package interface_adapter.add_flashcard;

/**
 * The data shown by the Add Flashcard view.
 */
public class AddFlashcardState {
  private String question = "";
  private String answer = "";
  private String errorMessage = null;

  /**
   * Creates a state with empty fields and no error.
   */
  public AddFlashcardState() { }

  /**
   * Returns the question.
   *
   * @return the question currently in the form
   */
  public String getQuestion() { return question; }

  /**
   * Sets the question.
   *
   * @param question the question to show in the form
   */
  public void setQuestion(String question) { this.question = question; }

  /**
   * Returns the answer.
   *
   * @return the answer currently in the form
   */
  public String getAnswer() { return answer; }

  /**
   * Sets the answer.
   *
   * @param answer the answer to show in the form
   */
  public void setAnswer(String answer) { this.answer = answer; }

  /**
   * Returns the error message.
   *
   * @return the error to display, or null if there is none
   */
  public String getErrorMessage() { return errorMessage; }

  /**
   * Sets the error message.
   *
   * @param errorMessage the error to display, or null to clear it
   */
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

  /**
   * Empties the form and clears the error.
   */
  public void clear() {
    question = "";
    answer = "";
    errorMessage = null;
  }
}
