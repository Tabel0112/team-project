package use_case.edit_study_set;

import data_access.InMemoryStudySetDataAccessObject;
import interface_adapter.edit_study_set.EditStudySetController;
import org.junit.Test;
import use_case.cloud.SaveRequest;
import use_case.cloud.SetSnapshot;
import use_case.cloud.SetSummary;
import use_case.load_study_sets.LoadStudySetsDataAccessInterface;
import use_case.load_study_sets.LoadStudySetsInteractor;
import use_case.load_study_sets.LoadStudySetsOutputBoundary;
import use_case.save_study_set.SaveAllStudySetsDataAccessInterface;
import use_case.save_study_set.SaveAllStudySetsInteractor;
import use_case.save_study_set.SaveAllStudySetsOutputBoundary;
import use_case.save_study_set.SaveAllStudySetsOutputData;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SaveAllEditStudySetIntegrationTest {
  @Test public void refreshCancelsEditMenuDeletionAndLaterConfirmedDeleteStaysGone() {
    String user = "student";
    String deletedSet = "physics";
    InMemoryStudySetDataAccessObject local = new InMemoryStudySetDataAccessObject();
    Cloud cloud = new Cloud();
    Loads initialList = new Loads();
    LoadStudySetsInteractor load = new LoadStudySetsInteractor(local, cloud, initialList);
    load.list(user);
    assertEquals(2, initialList.listed.size());
    assertFalse(local.contains(user, deletedSet));

    EditStudySetController edit = new EditStudySetController(
        new EditStudySetInteractor(local, new EditStudySetOutputBoundary() {
          @Override public void prepareSuccessView(EditStudySetOutputData output) {
            assertEquals(EditStudySetAction.DELETE, output.getAction());
            assertEquals(deletedSet, output.getStudySetId());
            assertEquals(1, output.getStudySets().size());
          }

          @Override public void prepareFailView(String message) { fail(message); }
        }));
    edit.delete(user, deletedSet);
    assertTrue(local.hasUnsaved(user));
    assertEquals(2, cloud.list(user).size());
    load.list(user);
    assertEquals(2, initialList.listed.size());
    assertEquals(deletedSet, initialList.listed.get(0).id);
    assertTrue(local.pendingDeletions(user).isEmpty()); assertFalse(local.hasUnsaved(user));

    List<SaveAllStudySetsOutputData> outcomes = new ArrayList<>();
    SaveAllStudySetsInteractor save = new SaveAllStudySetsInteractor(local, cloud, new SaveAllStudySetsOutputBoundary() {
      @Override public void savedAll(SaveAllStudySetsOutputData output) {
        outcomes.add(output);
        assertEquals(0, output.savedCount);
        assertTrue(output.failures.isEmpty());
      }

      @Override public void failed(String message) { fail(message); }
    });
    save.execute(user);
    assertEquals(0, outcomes.get(0).deletedCount); assertTrue(cloud.deleted.isEmpty());
    assertEquals(2, cloud.list(user).size());

    edit.delete(user, deletedSet);
    assertTrue(local.hasUnsaved(user));
    save.execute(user);
    assertEquals(1, outcomes.get(1).deletedCount);
    assertEquals(List.of(deletedSet), cloud.deleted);
    assertFalse(local.hasUnsaved(user));
    load.list(user);
    assertEquals(1, initialList.listed.size());
    assertEquals("chemistry", initialList.listed.get(0).id);
    save.execute(user); assertEquals(List.of(deletedSet), cloud.deleted);

    Loads nextSession = new Loads();
    new LoadStudySetsInteractor(new InMemoryStudySetDataAccessObject(), cloud, nextSession)
        .list(user);
    assertEquals(1, nextSession.listed.size());
    assertEquals("chemistry", nextSession.listed.get(0).id);
  }

  private static final class Cloud implements SaveAllStudySetsDataAccessInterface,
      LoadStudySetsDataAccessInterface {
    private final List<SetSummary> saved = new ArrayList<>(List.of(
        new SetSummary("physics", "Physics"), new SetSummary("chemistry", "Chemistry")));
    private final List<String> deleted = new ArrayList<>();

    @Override public List<SetSummary> list(String userId) { return List.copyOf(saved); }

    @Override public SetSnapshot load(String userId, String setId) {
      throw new AssertionError("Deleting from the list should not fetch cards.");
    }

    @Override public long save(SaveRequest request) {
      throw new AssertionError("Deleting a set should not save its old cards.");
    }

    @Override public void delete(String userId, String setId) {
      assertEquals("student", userId);
      deleted.add(setId);
      saved.removeIf(set -> set.id.equals(setId));
    }
  }

  private static final class Loads implements LoadStudySetsOutputBoundary {
    private List<SetSummary> listed;

    @Override public void listed(List<SetSummary> sets) { listed = sets; }

    @Override public void opened(SetSnapshot set) { fail("No set should be opened."); }

    @Override public void failed(String message) { fail(message); }
  }
}
