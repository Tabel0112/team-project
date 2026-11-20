package use_case.load_study_sets;
import java.util.List;
import use_case.cloud.*;
public interface LoadStudySetsDataAccessInterface {
  List<SetSummary> list(String userId) throws CloudException;
  SetSnapshot load(String userId, String setId) throws CloudException;
}
