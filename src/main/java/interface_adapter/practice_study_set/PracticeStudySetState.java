package interface_adapter.practice_study_set;

import java.util.List;

/**
 * The data shown while practising a study set: its flashcards, which one is on
 * screen, and whether that card's answer is showing.
 */
public class PracticeStudySetState {
  private String studySetName = "";
  private List<Item> cards = List.of();
  private int currentIndex = 0;
  private boolean answerShowing = false;
  private String errorMessage = null;

  /**
   * Creates an empty state.
   */
  public PracticeStudySetState() { }

  /**
   * Starts a new session on the first flashcard, with its answer hidden.
   *
   * @param studySetName the name of the study set being practised
   * @param cards        the flashcards to practise, in order
   */
  public void start(String studySetName, List<Item> cards) {
    this.studySetName = studySetName;
    this.cards = List.copyOf(cards);
    this.currentIndex = 0;
    this.answerShowing = false;
    this.errorMessage = null;
  }

  /**
   * Ends any session and clears any error, for example when the user logs out.
   */
  public void clear() {
    start("", List.of());
  }

  /**
   * Shows the current flashcard's answer if it is hidden, or hides it if it is showing.
   */
  public void flip() { answerShowing = !answerShowing; }

  /**
   * Checks whether there is a flashcard after the current one.
   *
   * @return true if the current flashcard is not the last one
   */
  public boolean hasNext() { return currentIndex < cards.size() - 1; }

  /**
   * Moves to the next flashcard, with its answer hidden. Does nothing on the last flashcard.
   */
  public void next() {
    if (hasNext()) {
      currentIndex++;
      answerShowing = false;
    }
  }

  /**
   * Returns the study set's name.
   *
   * @return the name of the study set being practised
   */
  public String getStudySetName() { return studySetName; }

  /**
   * Returns the flashcard on screen.
   *
   * @return the current flashcard, or null if no session has started
   */
  public Item getCurrentCard() { return cards.isEmpty() ? null : cards.get(currentIndex); }

  /**
   * Returns the current flashcard's position.
   *
   * @return the position of the current flashcard, starting at 0
   */
  public int getCurrentIndex() { return currentIndex; }

  /**
   * Returns the number of flashcards in the session.
   *
   * @return how many flashcards are being practised
   */
  public int getCardCount() { return cards.size(); }

  /**
   * Checks whether the current flashcard's answer is showing.
   *
   * @return true if the answer is showing, false if only the question is
   */
  public boolean isAnswerShowing() { return answerShowing; }

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
   * One flashcard as the view shows it.
   */
  public static class Item {
    private final String question;
    private final String answer;

    /**
     * Creates an item.
     *
     * @param question the front of the flashcard
     * @param answer   the back of the flashcard
     */
    public Item(String question, String answer) {
      this.question = question;
      this.answer = answer;
    }

    /**
     * Returns the question.
     *
     * @return the front of the flashcard
     */
    public String getQuestion() { return question; }

    /**
     * Returns the answer.
     *
     * @return the back of the flashcard
     */
    public String getAnswer() { return answer; }
  }
}
