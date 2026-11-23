package interface_adapter.practice_study_set;

import use_case.practice_study_set.PracticeStudySetInputBoundary;
import use_case.practice_study_set.PracticeStudySetInputData;

/**
 * The Controller for the Practice Study Set use case.
 */
public class PracticeStudySetController {
  private final PracticeStudySetInputBoundary interactor;

  /**
   * Creates the controller.
   *
   * @param interactor the use case to call
   */
  public PracticeStudySetController(PracticeStudySetInputBoundary interactor) {
    this.interactor = interactor;
  }

  /**
   * Asks the use case to start practising a study set.
   *
   * @param studySetId the id of the study set the user chose to review
   * @param shuffle    true to practise the flashcards in a random order
   */
  public void execute(String studySetId, boolean shuffle) {
    interactor.execute(new PracticeStudySetInputData(studySetId, shuffle));
  }
}
