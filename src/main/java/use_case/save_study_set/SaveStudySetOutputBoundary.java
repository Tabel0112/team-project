package use_case.save_study_set;
public interface SaveStudySetOutputBoundary {
  void saved(SaveStudySetOutputData output);
  void failed(String message);
}
