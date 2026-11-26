package use_case.save_study_set;

public interface SaveAllStudySetsOutputBoundary {
  void savedAll(SaveAllStudySetsOutputData output);
  void failed(String message);
}
