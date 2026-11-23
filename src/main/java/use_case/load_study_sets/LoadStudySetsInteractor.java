package use_case.load_study_sets;

import use_case.cloud.*;
import java.util.concurrent.Executor;

/** Opens local copies on demand; explicit refresh can replace acknowledged copies. */
public final class LoadStudySetsInteractor implements LoadStudySetsInputBoundary {
  private final WorkingStudySets local;
  private final LoadStudySetsDataAccessInterface cloud;
  private final LoadStudySetsOutputBoundary output;
  private final Executor applyRefresh;
  public LoadStudySetsInteractor(WorkingStudySets local, LoadStudySetsDataAccessInterface cloud,
                                LoadStudySetsOutputBoundary output) {
    this(local, cloud, output, Runnable::run);
  }
  /** The handoff must finish before returning, so the controller stays busy through replacement. */
  public LoadStudySetsInteractor(WorkingStudySets local, LoadStudySetsDataAccessInterface cloud,
      LoadStudySetsOutputBoundary output, Executor applyRefresh) {
    this.local = local; this.cloud = cloud; this.output = output;
    this.applyRefresh = applyRefresh;
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
  @Override public SetSnapshot captureRefresh(String userId, String setId) {
    return local.hasBaseline(userId, setId) ? local.local(userId, setId) : null;
  }
  @Override public void refresh(String userId, SetSnapshot expectedLocal, boolean reloadAllowed) {
    try {
      loadList(userId);
    } catch (CloudException exception) { output.failed(exception.getMessage()); return; }
    if (expectedLocal == null || !reloadAllowed) { return; }
    try {
      SetSnapshot snapshot = cloud.load(userId, expectedLocal.id);
      applyRefresh.execute(() -> {
        try {
          local.replace(userId, snapshot, expectedLocal);
          output.refreshed(snapshot);
        } catch (CloudException exception) { reloadFailed(exception); }
      });
    } catch (CloudException exception) { reloadFailed(exception); }
  }
  private void reloadFailed(CloudException exception) {
    output.failed("Study set list refreshed. The open set was kept: " + exception.getMessage());
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
