package use_case.create_study_set;

import data_access.InMemoryStudySetDataAccessObject;
import entity.StudySetFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the Create Study Set use case through its interactor, using the in-memory
 * data access object and a presenter that records what it is told.
 */
public class CreateStudySetInteractorTest {
  private static final String USER = "user-1";

  private InMemoryStudySetDataAccessObject dataAccess;
  private RecordingPresenter presenter;
  private CreateStudySetInteractor interactor;

  @Before
  public void setUp() {
    dataAccess = new InMemoryStudySetDataAccessObject();
    presenter = new RecordingPresenter();
    interactor = new CreateStudySetInteractor(dataAccess, presenter, new StudySetFactory());
  }

  private void create(String userId, String name) {
    interactor.execute(new CreateStudySetInputData(userId, name));
  }

  @Test
  public void validNameCreatesAndSavesStudySet() {
    create(USER, "CSC207");

    assertNotNull(presenter.success);
    assertNull(presenter.error);
    assertEquals("CSC207", presenter.success.getName());
    assertFalse(presenter.success.getStudySetId().isBlank());
    assertTrue(dataAccess.existsByName(USER, "CSC207"));
  }

  @Test
  public void nameIsTrimmed() {
    create(USER, "  CSC207  ");

    assertEquals("CSC207", presenter.success.getName());
    assertTrue(dataAccess.existsByName(USER, "CSC207"));
  }

  @Test
  public void newStudySetHasNoCards() {
    create(USER, "CSC207");

    assertEquals(0, presenter.success.getStudySets().get(0).getCardCount());
    assertTrue(dataAccess.getStudySets(USER).get(0).getCardsList().isEmpty());
  }

  @Test
  public void successReportsAllOfTheUsersStudySetsInOrder() {
    create(USER, "CSC207");
    create(USER, "MAT237");

    assertEquals(2, presenter.success.getStudySets().size());
    assertEquals("CSC207", presenter.success.getStudySets().get(0).getName());
    assertEquals("MAT237", presenter.success.getStudySets().get(1).getName());
  }

  @Test
  public void eachStudySetGetsAUniqueId() {
    create(USER, "CSC207");
    final String firstId = presenter.success.getStudySetId();
    create(USER, "MAT237");

    assertNotEquals(firstId, presenter.success.getStudySetId());
  }

  @Test
  public void emptyNameFails() {
    create(USER, "");

    assertNull(presenter.success);
    assertEquals("Please enter a name for your study set.", presenter.error);
    assertTrue(dataAccess.getStudySets(USER).isEmpty());
  }

  @Test
  public void blankNameFails() {
    create(USER, "   ");

    assertNull(presenter.success);
    assertEquals("Please enter a name for your study set.", presenter.error);
    assertTrue(dataAccess.getStudySets(USER).isEmpty());
  }

  @Test
  public void nullNameFails() {
    create(USER, null);

    assertNull(presenter.success);
    assertEquals("Please enter a name for your study set.", presenter.error);
  }

  @Test
  public void missingUserFails() {
    create(null, "CSC207");
    assertEquals("You must be logged in to create a study set.", presenter.error);
    assertNull(presenter.success);

    create("  ", "CSC207");
    assertEquals("You must be logged in to create a study set.", presenter.error);
    assertNull(presenter.success);
  }

  @Test
  public void duplicateNameFails() {
    create(USER, "CSC207");
    presenter.reset();

    create(USER, "CSC207");

    assertNull(presenter.success);
    assertEquals("You already have a study set named \"CSC207\". Please choose a different name.",
        presenter.error);
    assertEquals(1, dataAccess.getStudySets(USER).size());
  }

  @Test
  public void duplicateNameIgnoresCaseAndSpaces() {
    create(USER, "CSC207");
    presenter.reset();

    create(USER, "  csc207 ");

    assertNull(presenter.success);
    assertNotNull(presenter.error);
    assertEquals(1, dataAccess.getStudySets(USER).size());
  }

  @Test
  public void differentUsersCanUseTheSameName() {
    create("user-1", "CSC207");
    create("user-2", "CSC207");

    assertNull(presenter.error);
    assertNotNull(presenter.success);
    assertEquals(1, presenter.success.getStudySets().size());
    assertEquals(1, dataAccess.getStudySets("user-1").size());
    assertEquals(1, dataAccess.getStudySets("user-2").size());
  }

  private static class RecordingPresenter implements CreateStudySetOutputBoundary {
    private CreateStudySetOutputData success;
    private String error;

    @Override
    public void prepareSuccessView(CreateStudySetOutputData outputData) {
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