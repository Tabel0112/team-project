package use_case.load_study_sets;
public interface LoadStudySetsInputBoundary {
  void list(String userId);
  void open(String userId, String setId);
}
