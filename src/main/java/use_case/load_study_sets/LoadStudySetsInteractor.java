package use_case.load_study_sets;

import use_case.cloud.*;

/** Opens local copies on demand; explicit refresh can replace acknowledged copies. */
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
      loadList(userId);
    } catch (CloudException exception) { output.failed(exception.getMessage()); }
  }
  @Override public void open(String userId, String setId) {
    try {
      requireUser(userId);
      if (!local.contains(userId, setId)) { local.install(userId, cloud.load(userId, setId)); }
      output.opened(local.local(userId, setId));
    } catch (CloudException exception) { output.failed(exception.getMessage()); }
  }
  @Override public boolean needsRefreshConfirmation(String userId, String setId) {
    return local.hasBaseline(userId, setId) && local.isDirty(userId, setId);
  }
  @Override public void refresh(String userId, String setId, boolean discardChanges) {
    try {
      loadList(userId);
      if (setId == null || !local.hasBaseline(userId, setId)
          || (local.isDirty(userId, setId) && !discardChanges)) { return; }
      SetSnapshot snapshot = cloud.load(userId, setId);
      local.replace(userId, snapshot);
      output.refreshed(snapshot);
    } catch (CloudException exception) { output.failed(exception.getMessage()); }
  }
  private void loadList(String userId) throws CloudException {
    requireUser(userId);
    local.mergeSummaries(userId, cloud.list(userId));
    output.listed(local.summaries(userId));
  }
  private void requireUser(String userId) throws CloudException {
    if (userId == null || userId.isBlank()) { throw new CloudException("Log in to load your study sets."); }
  }
}
