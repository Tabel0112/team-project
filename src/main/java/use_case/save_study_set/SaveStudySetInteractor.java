package use_case.save_study_set;

import use_case.cloud.CloudException;
import use_case.cloud.SaveRequest;
import use_case.cloud.WorkingStudySets;

/** UC6: commit one immutable delta; acknowledge only the snapshot confirmed by cloud. */
public final class SaveStudySetInteractor implements SaveStudySetInputBoundary {
  private final WorkingStudySets local;
  private final SaveStudySetDataAccessInterface cloud;
  private final SaveStudySetOutputBoundary output;
  public SaveStudySetInteractor(WorkingStudySets local, SaveStudySetDataAccessInterface cloud,
                               SaveStudySetOutputBoundary output) {
    this.local = local; this.cloud = cloud; this.output = output;
  }
  @Override public void execute(SaveStudySetInputData input) {
    if (input.userId == null || input.userId.isBlank()) {
      output.failed("Log in before saving. Your local changes are still here."); return;
    }
    try {
      SaveRequest request = local.pendingOrCapture(input.userId, input.setId);
      if (request != null) { local.acknowledge(request, cloud.save(request)); }
      // A retry may acknowledge an older snapshot. Never mark newer edits saved.
      output.saved(new SaveStudySetOutputData(input.setId, local.isDirty(input.userId, input.setId)));
    } catch (CloudException exception) {
      output.failed(exception.getMessage());
    }
  }
}
