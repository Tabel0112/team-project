package use_case.edit_study_set;

import entity.StudySet;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the Edit Study Set use case: deletes a study set, renames it, or moves it
 * up or down the user's list, then reports the updated list.
 */
public class EditStudySetInteractor implements EditStudySetInputBoundary {
  private final EditStudySetDataAccessInterface dataAccess;
  private final EditStudySetOutputBoundary presenter;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   */
  public EditStudySetInteractor(EditStudySetDataAccessInterface dataAccess,
                                EditStudySetOutputBoundary presenter) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
  }

  /**
   * Performs the chosen edit if it is valid. Fails if the user is not logged in, no action
   * was chosen, the study set is not one of the user's, a new name is empty or already used
   * by another of the user's study sets, or the study set cannot move further up or down.
   *
   * @param inputData the study set, the chosen action, and the new name if renaming
   */
  @Override
  public void execute(EditStudySetInputData inputData) {
    final String userId = inputData.getUserId();
    if (userId == null || userId.isBlank()) {
      presenter.prepareFailView("You must be logged in to edit a study set.");
      return;
    }
    if (inputData.getAction() == null) {
      presenter.prepareFailView("Please choose an editing option.");
      return;
    }

    final List<StudySet> studySets = dataAccess.getStudySets(userId);
    final int index = indexOf(studySets, inputData.getStudySetId());
    if (index < 0) {
      presenter.prepareFailView("That study set could not be found.");
      return;
    }
    final StudySet target = studySets.get(index);
    String resultName = target.getName();

    switch (inputData.getAction()) {
      case DELETE:
        dataAccess.delete(userId, target.getId());
        break;
      case RENAME: {
        final String newName = inputData.getNewName() == null
            ? "" : inputData.getNewName().trim();
        final String problem = nameProblem(studySets, target, newName);
        if (problem != null) {
          presenter.prepareFailView(problem);
          return;
        }
        dataAccess.rename(userId, target.getId(), newName);
        resultName = newName;
        break;
      }
      case MOVE_UP:
        if (index == 0) {
          presenter.prepareFailView("This study set is already at the top of the list.");
          return;
        }
        dataAccess.move(userId, target.getId(), index - 1);
        break;
      case MOVE_DOWN:
        if (index == studySets.size() - 1) {
          presenter.prepareFailView("This study set is already at the bottom of the list.");
          return;
        }
        dataAccess.move(userId, target.getId(), index + 1);
        break;
      default:
        presenter.prepareFailView("Please choose an editing option.");
        return;
    }

    final List<EditStudySetOutputData.Item> items = new ArrayList<>();
    for (StudySet studySet : dataAccess.getStudySets(userId)) {
      items.add(new EditStudySetOutputData.Item(
          studySet.getId(), studySet.getName(), studySet.getCardsList().size()));
    }
    presenter.prepareSuccessView(new EditStudySetOutputData(
        inputData.getAction(), target.getId(), resultName, items));
  }

  private static int indexOf(List<StudySet> studySets, String studySetId) {
    for (int i = 0; i < studySets.size(); i++) {
      if (studySets.get(i).getId().equals(studySetId)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Checks a new name for the given study set.
   *
   * @return a message describing the problem, or null if the name is fine
   */
  private static String nameProblem(List<StudySet> studySets, StudySet target, String newName) {
    if (newName.isEmpty()) {
      return "Please enter a name for your study set.";
    }
    for (StudySet other : studySets) {
      if (!other.getId().equals(target.getId()) && other.getName().equalsIgnoreCase(newName)) {
        return "You already have a study set named \"" + newName
            + "\". Please choose a different name.";
      }
    }
    return null;
  }
}