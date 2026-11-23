package use_case.load_study_sets;
import use_case.cloud.SetSnapshot;
public interface LoadStudySetsInputBoundary {
  void list(String userId);
  void open(String userId, String setId);
  boolean needsRefreshConfirmation(String userId, String setId);
  SetSnapshot captureRefresh(String userId, String setId);
  void refresh(String userId, SetSnapshot expectedLocal, boolean reloadAllowed);
}
