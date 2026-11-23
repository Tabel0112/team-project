package use_case.cloud;

import java.util.List;

/** Local working copies and acknowledged baselines shared with create/add/view. */
public interface WorkingStudySets {
  SaveRequest pendingOrCapture(String userId, String setId) throws CloudException;
  void acknowledge(SaveRequest request, long revision);
  boolean isDirty(String userId, String setId);
  boolean hasUnsaved(String userId);
  boolean contains(String userId, String setId);
  boolean hasBaseline(String userId, String setId);
  void install(String userId, SetSnapshot snapshot) throws CloudException;
  void replace(String userId, SetSnapshot snapshot, SetSnapshot expectedLocal) throws CloudException;
  void mergeSummaries(String userId, List<SetSummary> summaries);
  List<SetSummary> summaries(String userId);
  SetSnapshot local(String userId, String setId);
  void clear();
}
