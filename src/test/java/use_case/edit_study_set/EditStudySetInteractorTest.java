package use_case.edit_study_set;

import data_access.InMemoryStudySetDataAccessObject;
import entity.Flashcard;
import entity.StudySet;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests the Edit Study Set use case through its interactor, using the in-memory data
 * access object and a presenter that records what it is told.
 */
public class EditStudySetInteractorTest {
  private static final String USER = "user-1";

  private InMemoryStudySetDataAccessObject dataAccess;
  private RecordingPresenter presenter;
  private EditStudySetInteractor interactor;

  /** Starts each test with the user's list as Alpha, Beta, Gamma (in that order). */
  @Before
  public void setUp() {
    dataAccess = new InMemoryStudySetDataAccessObject();
    dataAccess.save(USER, new StudySet("id-a", "Alpha"));
    dataAccess.save(USER, new StudySet("id-b", "Beta"));
    dataAccess.save(USER, new StudySet("id-c", "Gamma"));
    presenter = new RecordingPresenter();
    interactor = new EditStudySetInteractor(dataAccess, presenter);
  }

  private void edit(String userId, String id, EditStudySetAction action, String newName) {
    interactor.execute(new EditStudySetInputData(userId, id, action, newName));
  }

  private List<String> names(String userId) {
    final List<String> names = new ArrayList<>();
    for (StudySet studySet : dataAccess.getStudySets(userId)) {
      names.add(studySet.getName());
    }
    return names;
  }

  private static List<String> namesOf(EditStudySetOutputData output) {
    final List<String> names = new ArrayList<>();
    for (EditStudySetOutputData.Item item : output.getStudySets()) {
      names.add(item.getName());
    }
    return names;
  }

  // ---- rename ----

  /** Renaming changes the name, keeps the position, and reports the updated list. */
  @Test
  public void renameChangesNameAndKeepsPosition() {
    edit(USER, "id-b", EditStudySetAction.RENAME, "Delta");

    assertNull(presenter.error);
    assertEquals(EditStudySetAction.RENAME, presenter.success.getAction());
    assertEquals("id-b", presenter.success.getStudySetId());
    assertEquals("Delta", presenter.success.getName());
    assertEquals(List.of("Alpha", "Delta", "Gamma"), names(USER));
    assertEquals(List.of("Alpha", "Delta", "Gamma"), namesOf(presenter.success));
  }

  /** Renaming trims the new name. */
  @Test
  public void renameTrimsTheName() {
    edit(USER, "id-b", EditStudySetAction.RENAME, "  Delta  ");

    assertEquals("Delta", presenter.success.getName());
    assertEquals(List.of("Alpha", "Delta", "Gamma"), names(USER));
  }

  /** Renaming keeps the study set's id and flashcards. */
  @Test
  public void renameKeepsIdAndFlashcards() {
    final StudySet withCards = new StudySet("id-d", "Delta");
    withCards.getCardsList().addCard(new Flashcard("Q", "A"));
    dataAccess.save(USER, withCards);

    edit(USER, "id-d", EditStudySetAction.RENAME, "Epsilon");

    final StudySet renamed = dataAccess.getStudySets(USER).get(3);
    assertEquals("id-d", renamed.getId());
    assertEquals("Epsilon", renamed.getName());
    assertEquals(1, renamed.getCardsList().size());
    assertEquals(1, presenter.success.getStudySets().get(3).getCardCount());
  }

  /** A study set can be renamed to a different capitalisation of its own name. */
  @Test
  public void renameToOwnNameWithDifferentCaseIsAllowed() {
    edit(USER, "id-a", EditStudySetAction.RENAME, "ALPHA");

    assertNull(presenter.error);
    assertEquals(List.of("ALPHA", "Beta", "Gamma"), names(USER));
  }

  /** An empty, blank or null new name is rejected and nothing changes. */
  @Test
  public void renameToEmptyNameFails() {
    for (String bad : new String[] {"", "   ", null}) {
      presenter.reset();

      edit(USER, "id-a", EditStudySetAction.RENAME, bad);

      assertNull(presenter.success);
      assertEquals("Please enter a name for your study set.", presenter.error);
    }
    assertEquals(List.of("Alpha", "Beta", "Gamma"), names(USER));
  }

  /** A name already used by another of the user's study sets is rejected. */
  @Test
  public void renameToAnotherSetsNameFails() {
    edit(USER, "id-a", EditStudySetAction.RENAME, "beta ");

    assertNull(presenter.success);
    assertEquals("You already have a study set named \"beta\". Please choose a different name.",
        presenter.error);
    assertEquals(List.of("Alpha", "Beta", "Gamma"), names(USER));
  }

  // ---- delete ----

  /** Deleting removes the study set and reports the remaining list. */
  @Test
  public void deleteRemovesTheStudySet() {
    edit(USER, "id-b", EditStudySetAction.DELETE, null);

    assertNull(presenter.error);
    assertEquals(EditStudySetAction.DELETE, presenter.success.getAction());
    assertEquals("id-b", presenter.success.getStudySetId());
    assertEquals("Beta", presenter.success.getName());
    assertEquals(List.of("Alpha", "Gamma"), names(USER));
    assertEquals(List.of("Alpha", "Gamma"), namesOf(presenter.success));
  }

  /** Deleting the last remaining study set leaves an empty list. */
  @Test
  public void deletingEverythingLeavesEmptyList() {
    edit(USER, "id-a", EditStudySetAction.DELETE, null);
    edit(USER, "id-b", EditStudySetAction.DELETE, null);
    edit(USER, "id-c", EditStudySetAction.DELETE, null);

    assertTrue(presenter.success.getStudySets().isEmpty());
    assertTrue(dataAccess.getStudySets(USER).isEmpty());
  }

  // ---- move ----

  /** Moving up swaps a study set with the one above it. */
  @Test
  public void moveUpMovesOnePlaceUp() {
    edit(USER, "id-c", EditStudySetAction.MOVE_UP, null);

    assertNull(presenter.error);
    assertEquals(List.of("Alpha", "Gamma", "Beta"), names(USER));
    assertEquals(List.of("Alpha", "Gamma", "Beta"), namesOf(presenter.success));
  }

  /** Moving down swaps a study set with the one below it. */
  @Test
  public void moveDownMovesOnePlaceDown() {
    edit(USER, "id-a", EditStudySetAction.MOVE_DOWN, null);

    assertNull(presenter.error);
    assertEquals(List.of("Beta", "Alpha", "Gamma"), names(USER));
    assertEquals(List.of("Beta", "Alpha", "Gamma"), namesOf(presenter.success));
  }

  /** A study set already at the top cannot be moved up. */
  @Test
  public void moveUpAtTopFails() {
    edit(USER, "id-a", EditStudySetAction.MOVE_UP, null);

    assertNull(presenter.success);
    assertEquals("This study set is already at the top of the list.", presenter.error);
    assertEquals(List.of("Alpha", "Beta", "Gamma"), names(USER));
  }

  /** A study set already at the bottom cannot be moved down. */
  @Test
  public void moveDownAtBottomFails() {
    edit(USER, "id-c", EditStudySetAction.MOVE_DOWN, null);

    assertNull(presenter.success);
    assertEquals("This study set is already at the bottom of the list.", presenter.error);
    assertEquals(List.of("Alpha", "Beta", "Gamma"), names(USER));
  }

  /** A single study set can be moved neither up nor down. */
  @Test
  public void onlyStudySetCannotMove() {
    edit(USER, "id-a", EditStudySetAction.DELETE, null);
    edit(USER, "id-b", EditStudySetAction.DELETE, null);

    edit(USER, "id-c", EditStudySetAction.MOVE_UP, null);
    assertNotNull(presenter.error);
    presenter.reset();

    edit(USER, "id-c", EditStudySetAction.MOVE_DOWN, null);
    assertNotNull(presenter.error);
  }

  // ---- general failures ----

  /** Without a logged-in user nothing is edited. */
  @Test
  public void missingUserFails() {
    edit(null, "id-a", EditStudySetAction.DELETE, null);
    assertEquals("You must be logged in to edit a study set.", presenter.error);
    assertNull(presenter.success);

    edit("  ", "id-a", EditStudySetAction.DELETE, null);
    assertEquals("You must be logged in to edit a study set.", presenter.error);
    assertEquals(3, dataAccess.getStudySets(USER).size());
  }

  /** An unknown study set id fails. */
  @Test
  public void unknownStudySetFails() {
    edit(USER, "no-such-id", EditStudySetAction.DELETE, null);

    assertNull(presenter.success);
    assertEquals("That study set could not be found.", presenter.error);
    assertEquals(3, dataAccess.getStudySets(USER).size());
  }

  /** A user cannot edit another user's study set. */
  @Test
  public void cannotEditAnotherUsersStudySet() {
    edit("user-2", "id-a", EditStudySetAction.DELETE, null);

    assertEquals("That study set could not be found.", presenter.error);
    assertEquals(List.of("Alpha", "Beta", "Gamma"), names(USER));
  }

  /** No action chosen fails. */
  @Test
  public void missingActionFails() {
    edit(USER, "id-a", null, null);

    assertNull(presenter.success);
    assertEquals("Please choose an editing option.", presenter.error);
  }

  /** Editing one user's study sets leaves other users' lists alone. */
  @Test
  public void editingDoesNotAffectOtherUsers() {
    dataAccess.save("user-2", new StudySet("id-x", "Alpha"));

    edit(USER, "id-a", EditStudySetAction.DELETE, null);

    assertEquals(List.of("Alpha"), names("user-2"));
  }

  /** Remembers the last result the interactor reported. */
  private static class RecordingPresenter implements EditStudySetOutputBoundary {
    private EditStudySetOutputData success;
    private String error;

    @Override
    public void prepareSuccessView(EditStudySetOutputData outputData) {
      success = outputData;
      error = null;
    }

    @Override
    public void prepareFailView(String errorMessage) {
      error = errorMessage;
      success = null;
    }

    private void reset() {
      success = null;
      error = null;
    }
  }
}