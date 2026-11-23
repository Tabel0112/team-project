package interface_adapter.create_study_set;

import interface_adapter.ViewManagerModel;
import interface_adapter.study_set.StudySetState;
import interface_adapter.study_set.StudySetViewModel;
import use_case.create_study_set.CreateStudySetOutputBoundary;
import use_case.create_study_set.CreateStudySetOutputData;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the use case's results into updates to the {@link CreateStudySetViewModel},
 * and opens a newly created study set in the study set view.
 */
public class CreateStudySetPresenter implements CreateStudySetOutputBoundary {
  private final CreateStudySetViewModel viewModel;
  private final StudySetViewModel studySetViewModel;
  private final ViewManagerModel viewManagerModel;

  /**
   * Creates the presenter.
   *
   * @param viewModel         the create screen's view model to update
   * @param studySetViewModel the study set screen's view model, used to open the new set
   * @param viewManagerModel  used to switch to the study set view
   */
  public CreateStudySetPresenter(CreateStudySetViewModel viewModel,
                                 StudySetViewModel studySetViewModel,
                                 ViewManagerModel viewManagerModel) {
    this.viewModel = viewModel;
    this.studySetViewModel = studySetViewModel;
    this.viewManagerModel = viewManagerModel;
  }

  /**
   * Records the user's updated list of study sets, then opens the new study set:
   * marks it as the open study set and switches to the study set view.
   *
   * @param outputData the new study set and all of the user's study sets
   */
  @Override
  public void prepareSuccessView(CreateStudySetOutputData outputData) {
    final List<CreateStudySetState.Item> items = new ArrayList<>();
    int newSetCardCount = 0;
    for (CreateStudySetOutputData.Item item : outputData.getStudySets()) {
      items.add(new CreateStudySetState.Item(
          item.getId(), item.getName(), item.getCardCount()));
      if (item.getId().equals(outputData.getStudySetId())) {
        newSetCardCount = item.getCardCount();
      }
    }

    final CreateStudySetState state = viewModel.getState();
    state.setCreatedStudySetId(outputData.getStudySetId());
    state.setName(outputData.getName());
    state.setStudySets(items);
    state.setErrorMessage(null);
    viewModel.firePropertyChanged();

    final StudySetState studySetState = studySetViewModel.getState();
    studySetState.open(outputData.getStudySetId(), outputData.getName(), newSetCardCount);
    studySetViewModel.firePropertyChanged();
    viewManagerModel.setActiveView(StudySetViewModel.VIEW_NAME);
  }

  /**
   * Records the error in the state and notifies the view. The list of study sets
   * and the open study set are left as they were.
   *
   * @param errorMessage the message to show the user
   */
  @Override
  public void prepareFailView(String errorMessage) {
    final CreateStudySetState state = viewModel.getState();
    state.setCreatedStudySetId(null);
    state.setErrorMessage(errorMessage);
    viewModel.firePropertyChanged();
  }
}