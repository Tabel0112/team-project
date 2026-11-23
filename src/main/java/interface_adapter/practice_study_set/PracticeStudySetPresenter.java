package interface_adapter.practice_study_set;

import interface_adapter.ViewManagerModel;
import use_case.practice_study_set.PracticeStudySetOutputBoundary;
import use_case.practice_study_set.PracticeStudySetOutputData;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the Practice Study Set use case's results into updates to the view model.
 * On success it switches the Review tab to the practice view.
 */
public class PracticeStudySetPresenter implements PracticeStudySetOutputBoundary {
  private final PracticeStudySetViewModel viewModel;
  private final ViewManagerModel reviewViewManagerModel;

  /**
   * Creates the presenter.
   *
   * @param viewModel              the practice view's view model
   * @param reviewViewManagerModel switches between the views in the Review tab
   */
  public PracticeStudySetPresenter(PracticeStudySetViewModel viewModel,
                                   ViewManagerModel reviewViewManagerModel) {
    this.viewModel = viewModel;
    this.reviewViewManagerModel = reviewViewManagerModel;
  }

  /**
   * Starts the session on the first flashcard and shows the practice view.
   *
   * @param outputData the study set's name and its flashcards
   */
  @Override
  public void prepareSuccessView(PracticeStudySetOutputData outputData) {
    final List<PracticeStudySetState.Item> items = new ArrayList<>();
    for (PracticeStudySetOutputData.Card card : outputData.getCards()) {
      items.add(new PracticeStudySetState.Item(card.getQuestion(), card.getAnswer()));
    }
    viewModel.getState().start(outputData.getStudySetName(), items);
    viewModel.firePropertyChanged();
    reviewViewManagerModel.setActiveView(PracticeStudySetViewModel.VIEW_NAME);
  }

  /**
   * Puts the error into the state for the study set list to show.
   *
   * @param errorMessage the message to show the user
   */
  @Override
  public void prepareFailView(String errorMessage) {
    viewModel.getState().setErrorMessage(errorMessage);
    viewModel.firePropertyChanged();
  }
}
