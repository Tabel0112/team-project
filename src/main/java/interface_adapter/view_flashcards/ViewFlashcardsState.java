package interface_adapter.view_flashcards;

import java.util.List;

/**
 * The data shown by the flashcard list.
 */
public class ViewFlashcardsState {
  private String studySetName = "";
  private List<Item> cards = List.of();
  private String errorMessage = null;
  private int selectedIndex = -1;

  /**
   * Creates an empty state.
   */
  public ViewFlashcardsState() { }

  /**
   * Returns the study set's name.
   *
   * @return the name of the study set whose flashcards are shown
   */
  public String getStudySetName() { return studySetName; }

  /**
   * Sets the study set's name.
   *
   * @param studySetName the name of the study set whose flashcards are shown
   */
  public void setStudySetName(String studySetName) { this.studySetName = studySetName; }

  /**
   * Returns the flashcards.
   *
   * @return the flashcards to show, in order
   */
  public List<Item> getCards() { return cards; }

  /**
   * Sets the flashcards.
   *
   * @param cards the flashcards to show, in order
   */
  public void setCards(List<Item> cards) { this.cards = List.copyOf(cards); }

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
   * Returns the position of the flashcard to select, such as one that was just moved.
   *
   * @return the position to select, or -1 to select nothing
   */
  public int getSelectedIndex() { return selectedIndex; }

  /**
   * Sets the position of the flashcard to select.
   *
   * @param selectedIndex the position to select, or -1 to select nothing
   */
  public void setSelectedIndex(int selectedIndex) { this.selectedIndex = selectedIndex; }

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

    /**
     * Returns the question, which is what the list displays.
     *
     * @return the front of the flashcard
     */
    @Override
    public String toString() { return question; }
  }
}
