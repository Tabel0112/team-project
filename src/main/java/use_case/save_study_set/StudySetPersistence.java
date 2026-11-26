package use_case.save_study_set;

import use_case.cloud.CloudException;
import use_case.cloud.SaveRequest;
import use_case.cloud.WorkingStudySets;

/** Shared per-set persistence without presentation, used by single-set and batch saves. */
final class StudySetPersistence {
  private final WorkingStudySets local;
  private final SaveStudySetDataAccessInterface cloud;

  StudySetPersistence(WorkingStudySets local, SaveStudySetDataAccessInterface cloud) {
    this.local = local; this.cloud = cloud;
  }

  SaveStudySetOutputData save(SaveStudySetInputData input) throws CloudException {
    if (input.userId == null || input.userId.isBlank()) {
      throw new CloudException("Log in before saving. Your local changes are still here.");
    }
    SaveRequest request = local.pendingOrCapture(input.userId, input.setId);
    if (request != null) { local.acknowledge(request, cloud.save(request)); }
    // A retry can acknowledge an older snapshot while newer edits remain dirty.
    return new SaveStudySetOutputData(input.setId, local.isDirty(input.userId, input.setId));
  }
}
