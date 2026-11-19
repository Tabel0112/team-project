package interface_adapter.view_flashcards;

import use_case.view_flashcards.ViewFlashcardsOutputBoundary;
import use_case.view_flashcards.ViewFlashcardsOutputData;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the View Flashcards use case's results into updates to the view model.
 */
public class ViewFlashcardsPresenter implements ViewFlashcardsOutputBoundary {
  private final ViewFlashcardsViewModel viewModel;

  /**
   * Creates the presenter.
   *
   * @param viewModel the flashcard list's view model
   */
  public ViewFlashcardsPresenter(ViewFlashcardsViewModel viewModel) {
    this.viewModel = viewModel;
  }

  /**
   * Puts the study set's name and flashcards into the state for the view to show.
   *
   * @param outputData the study set's name and its flashcards
   */
  @Override
  public void prepareSuccessView(ViewFlashcardsOutputData outputData) {
    final List<ViewFlashcardsState.Item> items = new ArrayList<>();
    for (ViewFlashcardsOutputData.Card card : outputData.getCards()) {
      items.add(new ViewFlashcardsState.Item(card.getQuestion(), card.getAnswer()));
    }

    final ViewFlashcardsState state = viewModel.getState();
    state.setStudySetName(outputData.getStudySetName());
    state.setCards(items);
    state.setSelectedIndex(-1);
    state.setErrorMessage(null);
    viewModel.firePropertyChanged();
  }

  /**
   * Puts the error into the state for the view to show.
   *
   * @param errorMessage the message to show the user
   */
  @Override
  public void prepareFailView(String errorMessage) {
    viewModel.getState().setErrorMessage(errorMessage);
    viewModel.firePropertyChanged();
  }
}
