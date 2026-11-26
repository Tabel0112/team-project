package use_case.cloud;

import java.util.List;

/** Local working copies and acknowledged baselines shared with create/add/view. */
public interface WorkingStudySets {
  SaveRequest pendingOrCapture(String userId, String setId) throws CloudException;
  void acknowledge(SaveRequest request, long revision);
  boolean isDirty(String userId, String setId);
  boolean hasUnsaved(String userId);
  List<String> dirtySetIds(String userId);
  List<String> pendingDeletions(String userId);
  /** Exact unresolved first-save receipt of a queued deletion, or null. */
  SaveRequest pendingCreationForDeletion(String userId, String setId);
  /** Resolves only that receipt without restoring a deleted working copy. */
  void acknowledgeCreationForDeletion(SaveRequest request);
  void acknowledgeDeletion(String userId, String setId);
  boolean contains(String userId, String setId);
  void install(String userId, SetSnapshot snapshot) throws CloudException;
  void mergeSummaries(String userId, List<SetSummary> summaries);
  /** Discards only this account's work after a successful complete cloud list. */
  void resetToSummaries(String userId, List<SetSummary> summaries);
  List<SetSummary> summaries(String userId);
  SetSnapshot local(String userId, String setId);
  void clear();
}
