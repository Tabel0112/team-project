package interface_adapter.add_flashcard;

import interface_adapter.ViewManagerModel;
import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.study_set.StudySetState;
import interface_adapter.study_set.StudySetViewModel;
import use_case.add_flashcard.AddFlashcardOutputBoundary;
import use_case.add_flashcard.AddFlashcardOutputData;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns the Add Flashcard use case's results into updates to the view models.
 * On success it returns to the study set view with a confirmation message.
 */
public class AddFlashcardPresenter implements AddFlashcardOutputBoundary {
  private final AddFlashcardViewModel viewModel;
  private final StudySetViewModel studySetViewModel;
  private final CreateStudySetViewModel createStudySetViewModel;
  private final ViewManagerModel viewManagerModel;

  /**
   * Creates the presenter.
   *
   * @param viewModel               the add flashcard form's view model
   * @param studySetViewModel       the study set screen's view model, to show the new count
   * @param createStudySetViewModel the study set list's view model, to keep its counts current
   * @param viewManagerModel        used to switch back to the study set view
   */
  public AddFlashcardPresenter(AddFlashcardViewModel viewModel,
                               StudySetViewModel studySetViewModel,
                               CreateStudySetViewModel createStudySetViewModel,
                               ViewManagerModel viewManagerModel) {
    this.viewModel = viewModel;
    this.studySetViewModel = studySetViewModel;
    this.createStudySetViewModel = createStudySetViewModel;
    this.viewManagerModel = viewManagerModel;
  }

  /**
   * Empties the form, shows the study set's new flashcard count with a
   * confirmation message, and switches back to the study set view.
   *
   * @param outputData the study set's id and its new number of flashcards
   */
  @Override
  public void prepareSuccessView(AddFlashcardOutputData outputData) {
    viewModel.getState().clear();
    viewModel.firePropertyChanged();

    final StudySetState studySetState = studySetViewModel.getState();
    studySetState.setCardCount(outputData.getCardCount());
    studySetState.setMessage("Flashcard added.");
    studySetViewModel.firePropertyChanged();

    updateCountInStudySetList(outputData.getStudySetId(), outputData.getCardCount());
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

  /**
   * Updates the card count shown for one study set in the list of study sets,
   * so reopening it from the list shows the right number.
   */
  private void updateCountInStudySetList(String studySetId, int cardCount) {
    final CreateStudySetState listState = createStudySetViewModel.getState();
    final List<CreateStudySetState.Item> items = new ArrayList<>();
    for (CreateStudySetState.Item item : listState.getStudySets()) {
      if (item.getId().equals(studySetId)) {
        items.add(new CreateStudySetState.Item(item.getId(), item.getName(), cardCount));
      }
      else {
        items.add(item);
      }
    }
    listState.setStudySets(items);
    createStudySetViewModel.firePropertyChanged();
  }
}
