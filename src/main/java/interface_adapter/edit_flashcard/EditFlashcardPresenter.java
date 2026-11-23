package interface_adapter.edit_flashcard;

import interface_adapter.ViewManagerModel;
import interface_adapter.study_set.StudySetState;
import interface_adapter.study_set.StudySetViewModel;
import use_case.edit_flashcard.EditFlashcardOutputBoundary;
import use_case.edit_flashcard.EditFlashcardOutputData;

/**
 * Turns the Edit Flashcard use case's results into updates to the view models.
 * On success it returns to the study set view with a confirmation message.
 */
public class EditFlashcardPresenter implements EditFlashcardOutputBoundary {
  private final EditFlashcardViewModel viewModel;
  private final StudySetViewModel studySetViewModel;
  private final ViewManagerModel viewManagerModel;

  /**
   * Creates the presenter.
   *
   * @param viewModel         the edit flashcard form's view model
   * @param studySetViewModel the study set screen's view model, to show the confirmation
   * @param viewManagerModel  used to switch back to the study set view
   */
  public EditFlashcardPresenter(EditFlashcardViewModel viewModel,
                                StudySetViewModel studySetViewModel,
                                ViewManagerModel viewManagerModel) {
    this.viewModel = viewModel;
    this.studySetViewModel = studySetViewModel;
    this.viewManagerModel = viewManagerModel;
  }

  /**
   * Empties the form, shows a confirmation message, and switches back to the
   * study set view.
   *
   * @param outputData the study set's id and the flashcard as saved
   */
  @Override
  public void prepareSuccessView(EditFlashcardOutputData outputData) {
    viewModel.getState().clear();
    viewModel.firePropertyChanged();

    final StudySetState studySetState = studySetViewModel.getState();
    studySetState.setMessage("Flashcard updated: " + outputData.getQuestion());
    studySetViewModel.firePropertyChanged();

    viewManagerModel.setActiveView(StudySetViewModel.VIEW_NAME);
  }

  /**
   * Shows the error on the form. The question and answer the user typed are kept.
   *
   * @param errorMessage the message to show the user
   */
  @Override
  public void prepareFailView(String errorMessage) {
    viewModel.getState().setErrorMessage(errorMessage);
    viewModel.firePropertyChanged();
  }
}
