package interface_adapter.edit_flashcard;

/**
 * The data shown by the Edit Flashcard view.
 */
public class EditFlashcardState {
  private int cardIndex = -1;
  private String question = "";
  private String answer = "";
  private String errorMessage = null;

  /**
   * Creates a state with no flashcard being edited.
   */
  public EditFlashcardState() { }

  /**
   * Starts editing a flashcard, filling the form with its current text.
   *
   * @param cardIndex the position of the flashcard in the open study set, starting at 0
   * @param question  the flashcard's current question
   * @param answer    the flashcard's current answer
   */
  public void open(int cardIndex, String question, String answer) {
    this.cardIndex = cardIndex;
    this.question = question;
    this.answer = answer;
    this.errorMessage = null;
  }

  /**
   * Returns the position of the flashcard being edited.
   *
   * @return the flashcard's position in the open study set, or -1 if none is being edited
   */
  public int getCardIndex() { return cardIndex; }

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
   * Empties the form, clears the error, and stops editing.
   */
  public void clear() {
    cardIndex = -1;
    question = "";
    answer = "";
    errorMessage = null;
  }
}
