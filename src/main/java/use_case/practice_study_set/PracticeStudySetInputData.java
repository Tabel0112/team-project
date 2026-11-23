package use_case.practice_study_set;

/**
 * The data the Practice Study Set use case needs to run.
 */
public class PracticeStudySetInputData {
  private final String studySetId;
  private final boolean shuffle;

  /**
   * Creates input data.
   *
   * @param studySetId the id of the study set to practice
   * @param shuffle    true to practise the flashcards in a random order
   */
  public PracticeStudySetInputData(String studySetId, boolean shuffle) {
    this.studySetId = studySetId;
    this.shuffle = shuffle;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set to practice
   */
  public String getStudySetId() { return studySetId; }

  /**
   * Returns whether to shuffle the flashcards.
   *
   * @return true to practise the flashcards in a random order, false for their saved order
   */
  public boolean isShuffle() { return shuffle; }
}
