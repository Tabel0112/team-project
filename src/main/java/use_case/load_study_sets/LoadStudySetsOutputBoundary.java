package use_case.load_study_sets;
import java.util.List;
import use_case.cloud.SetSummary;
import use_case.cloud.SetSnapshot;
public interface LoadStudySetsOutputBoundary {
  void listed(List<SetSummary> sets);
  void refreshed(List<SetSummary> sets);
  void opened(SetSnapshot set);
  void failed(String message);
}
