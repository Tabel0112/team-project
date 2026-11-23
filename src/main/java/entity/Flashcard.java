package entity;

/**
 * A single flashcard with a question and its answer.
 */
public class Flashcard {
  private final String question;
  private final String answer;

  /**
   * Creates a flashcard.
   *
   * @param question the prompt shown to the student
   * @param answer   the correct answer to the prompt
   */
  public Flashcard(String question, String answer) {
    this.question = question;
    this.answer = answer;
  }

  /**
   * Returns the question.
   *
   * @return the prompt shown to the student
   */
  public String getQuestion() { return question; }

  /**
   * Returns the answer.
   *
   * @return the correct answer to the question
   */
  public String getAnswer() { return answer; }
}