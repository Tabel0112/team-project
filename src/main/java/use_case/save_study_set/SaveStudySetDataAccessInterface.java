package use_case.save_study_set;
import use_case.cloud.CloudException;
import use_case.cloud.SaveRequest;
public interface SaveStudySetDataAccessInterface {
  long save(SaveRequest request) throws CloudException;
}
