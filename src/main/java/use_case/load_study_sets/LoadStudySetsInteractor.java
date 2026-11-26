package use_case.load_study_sets;

import java.util.List;
import use_case.cloud.*;

/** Lists without discarding work; explicit refresh resets it only after a successful cloud list. */
public final class LoadStudySetsInteractor implements LoadStudySetsInputBoundary {
  private final WorkingStudySets local;
  private final LoadStudySetsDataAccessInterface cloud;
  private final LoadStudySetsOutputBoundary output;
  public LoadStudySetsInteractor(WorkingStudySets local, LoadStudySetsDataAccessInterface cloud,
                                LoadStudySetsOutputBoundary output) {
    this.local = local; this.cloud = cloud; this.output = output;
  }
  @Override public void list(String userId) {
    try {
      requireUser(userId);
      local.mergeSummaries(userId, cloud.list(userId));
      output.listed(local.summaries(userId));
    } catch (CloudException exception) { output.failed(exception.getMessage()); }
  }
  @Override public void open(String userId, String setId) {
    try {
      requireUser(userId);
      if (!local.contains(userId, setId)) { local.install(userId, cloud.load(userId, setId)); }
      output.opened(local.local(userId, setId));
    } catch (CloudException exception) { output.failed(exception.getMessage()); }
  }
  @Override public void refresh(String userId) {
    try {
      requireUser(userId);
      List<SetSummary> summaries = cloud.list(userId);
      local.resetToSummaries(userId, summaries);
      output.refreshed(summaries);
    } catch (CloudException exception) { output.failed(exception.getMessage()); }
  }
  private void requireUser(String userId) throws CloudException {
    if (userId == null || userId.isBlank()) { throw new CloudException("Log in to load your study sets."); }
  }
}
