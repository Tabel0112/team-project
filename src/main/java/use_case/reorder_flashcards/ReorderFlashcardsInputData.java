package use_case.reorder_flashcards;

/**
 * The data the Reorder Flashcards use case needs to run.
 */
public class ReorderFlashcardsInputData {
  private final String studySetId;
  private final int fromIndex;
  private final int toIndex;

  /**
   * Creates input data.
   *
   * @param studySetId the id of the study set the flashcard belongs to
   * @param fromIndex  the flashcard's current position, starting at 0
   * @param toIndex    the position to move the flashcard to, starting at 0
   */
  public ReorderFlashcardsInputData(String studySetId, int fromIndex, int toIndex) {
    this.studySetId = studySetId;
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set the flashcard belongs to
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns the flashcard's current position.
   *
   * @return the flashcard's current position, starting at 0
   */
  public int getFromIndex() { return fromIndex; }

  /**
   * Returns the flashcard's new position.
   *
   * @return the position to move the flashcard to, starting at 0
   */
  public int getToIndex() { return toIndex; }
}
