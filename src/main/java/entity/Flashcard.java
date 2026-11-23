package entity;

/**
 * A single flashcard with a question and its answer.
 */
public class Flashcard {
  private final String id;
  private final String question;
  private final String answer;

  /**
   * Creates a flashcard.
   *
   * @param question the prompt shown to the student
   * @param answer   the correct answer to the prompt
   */
  public Flashcard(String question, String answer) {
    this(java.util.UUID.randomUUID().toString(), question, answer);
  }

  /** Rebuilds an existing card without changing its identity. */
  public Flashcard(String id, String question, String answer) {
    this.id = java.util.Objects.requireNonNull(id);
    this.question = question;
    this.answer = answer;
  }

  /** Returns the stable identifier. */
  public String getId() { return id; }

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