package interface_adapter.study_set;

/**
 * The state of the study set screen: which study set is currently open.
 *
 * <p>Other use cases that work on the open study set (for example adding a
 * flashcard) should read {@link #getStudySetId()} to know which one it is.
 */
public class StudySetState {
  private String studySetId = "";
  private String name = "";
  private int cardCount = 0;
  private String message = "";

  /**
   * Creates a state with no study set open.
   */
  public StudySetState() { }

  /**
   * Marks a study set as the one that is open.
   *
   * @param studySetId the id of the study set
   * @param name       the name of the study set
   * @param cardCount  the number of flashcards it contains
   */
  public void open(String studySetId, String name, int cardCount) {
    this.studySetId = studySetId;
    this.name = name;
    this.cardCount = cardCount;
    this.message = "";
  }

  /**
   * Returns the id of the open study set.
   *
   * @return the open study set's id, or an empty string if none is open
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns the name of the open study set.
   *
   * @return the open study set's name
   */
  public String getName() { return name; }

  /**
   * Sets the name of the open study set.
   *
   * @param name the open study set's name
   */
  public void setName(String name) { this.name = name; }

  /**
   * Returns the number of flashcards in the open study set.
   *
   * @return how many flashcards the open study set contains
   */
  public int getCardCount() { return cardCount; }

  /**
   * Sets the number of flashcards in the open study set.
   *
   * @param cardCount how many flashcards the open study set contains
   */
  public void setCardCount(int cardCount) { this.cardCount = cardCount; }

  /**
   * Returns the confirmation message to show, such as after adding a flashcard.
   *
   * @return the message, or an empty string if there is none
   */
  public String getMessage() { return message; }

  /**
   * Sets the confirmation message to show.
   *
   * @param message the message, or an empty string to clear it
   */
  public void setMessage(String message) { this.message = message; }
}