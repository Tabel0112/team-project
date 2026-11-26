package use_case.save_study_set;

import use_case.cloud.CloudException;
import use_case.cloud.WorkingStudySets;

/** UC6: commit one immutable delta; acknowledge only the snapshot confirmed by cloud. */
public final class SaveStudySetInteractor implements SaveStudySetInputBoundary {
  private final StudySetPersistence persistence;
  private final SaveStudySetOutputBoundary output;
  public SaveStudySetInteractor(WorkingStudySets local, SaveStudySetDataAccessInterface cloud,
                               SaveStudySetOutputBoundary output) {
    this.persistence = new StudySetPersistence(local, cloud); this.output = output;
  }
  @Override public void execute(SaveStudySetInputData input) {
    try {
      output.saved(persistence.save(input));
    } catch (CloudException exception) {
      output.failed(exception.getMessage());
    }
  }
}
