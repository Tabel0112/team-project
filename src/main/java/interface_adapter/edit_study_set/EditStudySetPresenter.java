package interface_adapter.edit_study_set;

import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.study_set.StudySetState;
import interface_adapter.study_set.StudySetViewModel;
import use_case.edit_study_set.EditStudySetAction;
import use_case.edit_study_set.EditStudySetOutputBoundary;
import use_case.edit_study_set.EditStudySetOutputData;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the use case's results into updates to the edit screen's state, the list of
 * study sets shown in the create screen's panel, and the open study set.
 */
public class EditStudySetPresenter implements EditStudySetOutputBoundary {
  private final EditStudySetViewModel viewModel;
  private final CreateStudySetViewModel createStudySetViewModel;
  private final StudySetViewModel studySetViewModel;

  /**
   * Creates the presenter.
   *
   * @param viewModel               the edit screen's view model to update
   * @param createStudySetViewModel the view model whose state holds the list of study sets
   * @param studySetViewModel       the view model holding which study set is open
   */
  public EditStudySetPresenter(EditStudySetViewModel viewModel,
                               CreateStudySetViewModel createStudySetViewModel,
                               StudySetViewModel studySetViewModel) {
    this.viewModel = viewModel;
    this.createStudySetViewModel = createStudySetViewModel;
    this.studySetViewModel = studySetViewModel;
  }

  /**
   * Shows the updated list of study sets, keeps the open study set in step with a rename or
   * delete, and marks the edit as completed.
   *
   * @param outputData what was done and the user's updated list of study sets
   */
  @Override
  public void prepareSuccessView(EditStudySetOutputData outputData) {
    final List<CreateStudySetState.Item> items = new ArrayList<>();
    for (EditStudySetOutputData.Item item : outputData.getStudySets()) {
      items.add(new CreateStudySetState.Item(
          item.getId(), item.getName(), item.getCardCount()));
    }
    createStudySetViewModel.getState().setStudySets(items);
    createStudySetViewModel.firePropertyChanged();

    final StudySetState open = studySetViewModel.getState();
    if (open.getStudySetId().equals(outputData.getStudySetId())) {
      if (outputData.getAction() == EditStudySetAction.DELETE) {
        open.open("", "", 0);
        studySetViewModel.firePropertyChanged();
      } else if (outputData.getAction() == EditStudySetAction.RENAME) {
        open.open(outputData.getStudySetId(), outputData.getName(), cardCountOf(outputData));
        studySetViewModel.firePropertyChanged();
      }
    }

    final EditStudySetState state = viewModel.getState();
    state.setName(outputData.getName());
    state.setErrorMessage(null);
    state.setCompleted(true);
    viewModel.firePropertyChanged();
  }

  private static int cardCountOf(EditStudySetOutputData outputData) {
    for (EditStudySetOutputData.Item item : outputData.getStudySets()) {
      if (item.getId().equals(outputData.getStudySetId())) {
        return item.getCardCount();
      }
    }
    return 0;
  }

  /**
   * Records the error in the state and notifies the view. The list is left as it was.
   *
   * @param errorMessage the message to show the user
   */
  @Override
  public void prepareFailView(String errorMessage) {
    final EditStudySetState state = viewModel.getState();
    state.setErrorMessage(errorMessage);
    state.setCompleted(false);
    viewModel.firePropertyChanged();
  }
}