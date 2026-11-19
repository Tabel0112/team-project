package use_case.delete_flashcard;

/**
 * The Output Data for the Delete Flashcard use case.
 */
public class DeleteFlashcardOutputData {
  private final String studySetId;
  private final String deletedQuestion;
  private final int cardCount;

  /**
   * Creates output data.
   *
   * @param studySetId      the id of the study set the flashcard was removed from
   * @param deletedQuestion the question of the flashcard that was removed
   * @param cardCount       the number of flashcards in the study set after removing
   */
  public DeleteFlashcardOutputData(String studySetId, String deletedQuestion, int cardCount) {
    this.studySetId = studySetId;
    this.deletedQuestion = deletedQuestion;
    this.cardCount = cardCount;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set the flashcard was removed from
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns the deleted flashcard's question.
   *
   * @return the question of the flashcard that was removed
   */
  public String getDeletedQuestion() { return deletedQuestion; }

  /**
   * Returns the new number of flashcards.
   *
   * @return how many flashcards the study set now contains
   */
  public int getCardCount() { return cardCount; }
}
