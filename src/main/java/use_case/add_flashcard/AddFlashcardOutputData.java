package use_case.add_flashcard;

/**
 * The Output Data for the Add Flashcard use case.
 */
public class AddFlashcardOutputData {
  private final String studySetId;
  private final int cardCount;

  /**
   * Creates output data.
   *
   * @param studySetId the id of the study set the flashcard was added to
   * @param cardCount  the number of flashcards in the study set after adding
   */
  public AddFlashcardOutputData(String studySetId, int cardCount) {
    this.studySetId = studySetId;
    this.cardCount = cardCount;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set the flashcard was added to
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns the new number of flashcards.
   *
   * @return how many flashcards the study set now contains
   */
  public int getCardCount() { return cardCount; }
}
