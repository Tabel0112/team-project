package use_case.save_study_set;

import java.util.ArrayList;
import java.util.List;
import use_case.cloud.CloudException;
import use_case.cloud.SaveRequest;
import use_case.cloud.WorkingStudySets;

/** UC6: process deletions first, then dirty sets, preserving each failed operation for retry. */
public final class SaveAllStudySetsInteractor implements SaveAllStudySetsInputBoundary {
  private final WorkingStudySets local;
  private final SaveAllStudySetsDataAccessInterface cloud;
  private final SaveAllStudySetsOutputBoundary output;
  private final StudySetPersistence persistence;

  public SaveAllStudySetsInteractor(WorkingStudySets local, SaveAllStudySetsDataAccessInterface cloud,
      SaveAllStudySetsOutputBoundary output) {
    this.local = local; this.cloud = cloud; this.output = output;
    this.persistence = new StudySetPersistence(local, cloud);
  }

  @Override public void execute(String userId) {
    if (userId == null || userId.isBlank()) {
      output.failed("Log in before saving. Your local changes are still here."); return;
    }
    int saved = 0, deleted = 0;
    List<String> failures = new ArrayList<>();
    for (String id : local.pendingDeletions(userId)) {
      try {
        SaveRequest creation = local.pendingCreationForDeletion(userId, id);
        if (creation != null) {
          cloud.save(creation);
          // Persist this resolution before DELETE. Its retry must never recreate
          // a row if DELETE committed but its response was lost.
          local.acknowledgeCreationForDeletion(creation);
        }
        cloud.delete(userId, id);
        local.acknowledgeDeletion(userId, id); deleted++;
      } catch (CloudException exception) { failures.add("Delete: " + exception.getMessage()); }
    }
    for (String id : local.dirtySetIds(userId)) {
      try {
        persistence.save(new SaveStudySetInputData(userId, id)); saved++;
      } catch (CloudException exception) { failures.add("Save: " + exception.getMessage()); }
    }
    output.savedAll(new SaveAllStudySetsOutputData(saved, deleted, failures, local.hasUnsaved(userId)));
  }
}
