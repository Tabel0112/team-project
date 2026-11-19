package use_case.delete_flashcard;

/**
 * The data the Delete Flashcard use case needs to run.
 */
public class DeleteFlashcardInputData {
  private final String studySetId;
  private final int cardIndex;

  /**
   * Creates input data.
   *
   * @param studySetId the id of the study set the flashcard belongs to
   * @param cardIndex  the position of the flashcard in the study set, starting at 0
   */
  public DeleteFlashcardInputData(String studySetId, int cardIndex) {
    this.studySetId = studySetId;
    this.cardIndex = cardIndex;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set the flashcard belongs to
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns the flashcard's position.
   *
   * @return the position of the flashcard in the study set, starting at 0
   */
  public int getCardIndex() { return cardIndex; }
}
