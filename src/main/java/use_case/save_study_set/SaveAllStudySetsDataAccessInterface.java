package use_case.save_study_set;

import use_case.cloud.CloudException;

public interface SaveAllStudySetsDataAccessInterface extends SaveStudySetDataAccessInterface {
  void delete(String userId, String setId) throws CloudException;
}
