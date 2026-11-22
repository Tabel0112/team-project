package use_case.practice_study_set;

/**
 * The data the Practice Study Set use case needs to run.
 */
public class PracticeStudySetInputData {
  private final String studySetId;

  /**
   * Creates input data.
   *
   * @param studySetId the id of the study set to practice
   */
  public PracticeStudySetInputData(String studySetId) {
    this.studySetId = studySetId;
  }

  /**
   * Returns the study set id.
   *
   * @return the id of the study set to practice
   */
  public String getStudySetId() { return studySetId; }
}
