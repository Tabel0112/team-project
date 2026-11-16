package use_case.view_flashcards;

/**
 * The data the View Flashcards use case needs to run.
 */
public class ViewFlashcardsInputData {
  private final String studySetId;

  /**
   * Creates input data.
   *
   * @param studySetId the id of the study set whose flashcards to show
   */
  public ViewFlashcardsInputData(String studySetId) {
    this.studySetId = studySetId;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set whose flashcards to show
   */
  public String getStudySetId() { return studySetId; }
}
