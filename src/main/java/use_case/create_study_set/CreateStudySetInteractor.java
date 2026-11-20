package use_case.create_study_set;

import entity.Flashcard;
import entity.StudySet;
import entity.StudySetFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Interactor for the Create Study Set use case.
 */
public class CreateStudySetInteractor implements CreateStudySetInputBoundary {
  private final CreateStudySetDataAccessInterface dataAccess;
  private final CreateStudySetOutputBoundary presenter;
  private final StudySetFactory factory;

  /**
   * Creates the interactor.
   *
   * @param dataAccess storage for study sets
   * @param presenter  receives the success or failure result
   * @param factory    builds new study sets
   */
  public CreateStudySetInteractor(CreateStudySetDataAccessInterface dataAccess,
                                  CreateStudySetOutputBoundary presenter,
                                  StudySetFactory factory) {
    this.dataAccess = dataAccess;
    this.presenter = presenter;
    this.factory = factory;
  }

  /**
   * Creates a study set for the user if the request is valid. Fails if the
   * user is not logged in, the name is empty, or the user already has a
   * study set with that name.
   *
   * @param inputData the user id and requested name
   */
  @Override
  public void execute(CreateStudySetInputData inputData) {
    final String userId = inputData.getUserId();
    if (userId == null || userId.isBlank()) {
      presenter.prepareFailView("You must be logged in to create a study set.");
      return;
    }

    final String name = inputData.getName() == null ? "" : inputData.getName().trim();
    if (name.isEmpty()) {                                   // extension 4a
      presenter.prepareFailView("Please enter a name for your study set.");
      return;
    }
    if (dataAccess.existsByName(userId, name)) {            // extension 4b
      presenter.prepareFailView("You already have a study set named \"" + name
          + "\". Please choose a different name.");
      return;
    }

    save(userId, name, List.of());
  }

  public void createFromNotes(String userId, String baseName, List<Flashcard> cards) {
    if (userId == null || userId.isBlank()) {
      presenter.prepareFailView("Log in before creating a generated study set.");
      return;
    }
    if (cards.isEmpty()) {
      throw new IllegalArgumentException("A generated set needs at least one flashcard.");
    }
    String name = baseName;
    for (int suffix = 2; dataAccess.existsByName(userId, name); suffix++) {
      name = baseName + " (" + suffix + ")";
    }
    save(userId, name, cards);
  }

  private void save(String userId, String name, List<Flashcard> cards) {
    final StudySet studySet = factory.create(name);         // step 5
    cards.forEach(studySet.getCardsList()::addCard);
    dataAccess.save(userId, studySet);

    final List<CreateStudySetOutputData.Item> items = new ArrayList<>();
    for (StudySet existing : dataAccess.getStudySets(userId)) {
      items.add(new CreateStudySetOutputData.Item(
          existing.getId(), existing.getName(), existing.getCardsList().size()));
    }
    presenter.prepareSuccessView(new CreateStudySetOutputData(
        studySet.getId(), studySet.getName(), items));
  }
}