package interface_adapter.reorder_flashcards;

import interface_adapter.view_flashcards.ViewFlashcardsState;
import interface_adapter.view_flashcards.ViewFlashcardsViewModel;
import use_case.reorder_flashcards.ReorderFlashcardsOutputBoundary;
import use_case.reorder_flashcards.ReorderFlashcardsOutputData;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the Reorder Flashcards use case's results into updates to the flashcard
 * list's view model, so the open pop-up shows the new order straight away.
 */
public class ReorderFlashcardsPresenter implements ReorderFlashcardsOutputBoundary {
  private final ViewFlashcardsViewModel viewModel;

  /**
   * Creates the presenter.
   *
   * @param viewModel the flashcard list's view model
   */
  public ReorderFlashcardsPresenter(ViewFlashcardsViewModel viewModel) {
    this.viewModel = viewModel;
  }

  /**
   * Shows the flashcards in their new order, with the moved one still selected.
   *
   * @param outputData the flashcards in their new order and where the moved one ended up
   */
  @Override
  public void prepareSuccessView(ReorderFlashcardsOutputData outputData) {
    final List<ViewFlashcardsState.Item> items = new ArrayList<>();
    for (ReorderFlashcardsOutputData.Card card : outputData.getCards()) {
      items.add(new ViewFlashcardsState.Item(card.getQuestion(), card.getAnswer()));
    }

    final ViewFlashcardsState state = viewModel.getState();
    state.setCards(items);
    state.setSelectedIndex(outputData.getMovedIndex());
    state.setErrorMessage(null);
    viewModel.firePropertyChanged();
  }

  /**
   * Puts the error into the state for the view to show. The order is unchanged.
   *
   * @param errorMessage the message to show the user
   */
  @Override
  public void prepareFailView(String errorMessage) {
    viewModel.getState().setErrorMessage(errorMessage);
    viewModel.firePropertyChanged();
  }
}
