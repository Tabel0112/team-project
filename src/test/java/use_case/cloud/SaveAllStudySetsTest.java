package use_case.cloud;

import data_access.InMemoryStudySetDataAccessObject;
import entity.Flashcard;
import entity.StudySet;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import use_case.save_study_set.*;
import static org.junit.Assert.*;

public class SaveAllStudySetsTest {
  private final String user = "account-a";
  private final InMemoryStudySetDataAccessObject local = new InMemoryStudySetDataAccessObject();
  private final Gateway gateway = new Gateway();
  private final Output output = new Output();
  private final SaveAllStudySetsInteractor save = new SaveAllStudySetsInteractor(local, gateway, output);

  private StudySet draft(String id, String name) {
    StudySet set = new StudySet(id, name); local.save(user, set); return set;
  }
  private void saved(String id, long revision) throws CloudException {
    local.install(user, new SetSnapshot(id, "Saved " + id, revision, List.of()));
  }

  @Test public void batchSavesAllDirtySetsAndRetryOnlyRepeatsFailedReceipt() throws Exception {
    draft("new", "New"); draft("failed", "Failed"); saved("edited", 2); saved("clean", 1);
    local.getStudySetById("edited").getCardsList().addCard(new Flashcard("Q", "A"));
    local.mergeSummaries(user, List.of(new SetSummary("unopened", "Unopened")));
    local.save("account-b", new StudySet("other", "Other account"));
    gateway.failedSaves.add("failed");
    save.execute(user);
    assertEquals(2, output.result.savedCount); assertEquals(0, output.result.deletedCount);
    assertEquals(1, output.result.failures.size()); assertTrue(output.result.stillDirty);
    assertFalse(local.isDirty(user, "new")); assertFalse(local.isDirty(user, "edited"));
    assertTrue(local.isDirty(user, "failed")); assertTrue(local.hasUnsaved("account-b"));
    SaveRequest retry = local.pendingOrCapture(user, "failed");
    assertEquals(0, retry.expectedRevision);
    assertEquals(2, gateway.saves.stream().filter(request -> request.snapshot.id.equals("edited"))
        .findFirst().get().expectedRevision);
    gateway.failedSaves.clear(); save.execute(user);
    assertEquals(1, output.result.savedCount); assertTrue(output.result.failures.isEmpty());
    assertFalse(output.result.stillDirty); assertSame(retry, gateway.saves.get(3));
    assertEquals(List.of("save:new", "save:failed", "save:edited", "save:failed"), gateway.calls);
    save.execute(user); assertEquals(0, output.result.savedCount); assertEquals(4, gateway.calls.size());
    assertEquals(3, output.completed);
  }

  @Test public void deletionsRunFirstAndPartialFailureKeepsOnlyFailedWorkHiddenForRetry() throws Exception {
    saved("cached", 3);
    local.mergeSummaries(user, List.of(new SetSummary("unopened", "Reusable")));
    local.getStudySetById("cached").getCardsList().addCard(new Flashcard("local", "edit"));
    local.pendingOrCapture(user, "cached");
    local.delete(user, "cached"); local.delete(user, "unopened");
    draft("replacement", "Reusable");
    gateway.failedDeletes.add("cached");
    save.execute(user);
    assertEquals(List.of("delete:cached", "delete:unopened", "save:replacement"), gateway.calls);
    assertEquals(1, output.result.deletedCount); assertEquals(1, output.result.savedCount);
    assertEquals(1, output.result.failures.size()); assertTrue(output.result.stillDirty);
    assertEquals(List.of("cached"), local.pendingDeletions(user));
    assertEquals(1, gateway.saves.size()); // Never replay an ordinary update for a queued deletion.
    local.mergeSummaries(user, List.of(new SetSummary("cached", "Stale cloud result")));
    assertFalse(local.summaries(user).stream().anyMatch(summary -> summary.id.equals("cached")));
    assertNull(local.local(user, "cached"));
    assertThrows(CloudException.class, () -> local.pendingOrCapture(user, "cached"));
    gateway.failedDeletes.clear(); save.execute(user);
    assertEquals(1, output.result.deletedCount); assertEquals(0, output.result.savedCount);
    assertFalse(local.hasUnsaved(user)); assertTrue(local.pendingDeletions(user).isEmpty());
    assertEquals("delete:cached", gateway.calls.get(3)); assertEquals(1, gateway.saves.size());
  }
  @Test public void batchRetryAcknowledgesOlderSnapshotAndReportsNewerEditsStillPending() throws Exception {
    StudySet working = draft("retry", "Retry");
    working.getCardsList().addCard(new Flashcard("first", "answer"));
    gateway.failedSaves.add("retry"); save.execute(user);
    SaveRequest original = gateway.saves.get(0);
    working.getCardsList().addCard(new Flashcard("later", "answer"));
    gateway.failedSaves.clear(); save.execute(user);
    assertSame(original, gateway.saves.get(1)); assertTrue(output.result.stillDirty);
    assertEquals(1, output.result.savedCount); assertTrue(output.result.failures.isEmpty());
    save.execute(user);
    SaveRequest next = gateway.saves.get(2);
    assertNotEquals(original.operationId, next.operationId); assertEquals(1, next.expectedRevision);
    assertEquals(1, next.upserts.size()); assertEquals("later", next.upserts.get(0).question);
    assertFalse(output.result.stillDirty);
  }

  @Test public void draftInPresentationIndexIsLocalOnlyButUnopenedCloudIdQueuesPerAccount() throws Exception {
    local.mergeSummaries(user, List.of(new SetSummary("remote", "Remote")));
    draft("draft", "Local");
    // Olivia's presentation reorder can place drafts in this index. It is not
    // evidence that a draft ever existed remotely.
    Field field = InMemoryStudySetDataAccessObject.class.getDeclaredField("indexes");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, Map<String, SetSummary>> indexes = (Map<String, Map<String, SetSummary>>) field.get(local);
    indexes.get(user).put("draft", new SetSummary("draft", "Local"));
    local.delete(user, "draft"); assertTrue(local.pendingDeletions(user).isEmpty());
    local.delete("account-b", "remote"); assertFalse(local.hasUnsaved("account-b"));
    assertEquals(1, local.summaries(user).size());
    local.delete(user, "remote"); assertEquals(List.of("remote"), local.pendingDeletions(user));
    assertTrue(local.hasUnsaved(user)); assertFalse(local.contains(user, "remote"));
    assertTrue(local.summaries(user).isEmpty());
    local.mergeSummaries(user, List.of(new SetSummary("remote", "Remote")));
    assertTrue(local.summaries(user).isEmpty());
    local.clear(); assertFalse(local.hasUnsaved(user)); assertTrue(local.pendingDeletions(user).isEmpty());
  }

  @Test public void ambiguousFirstCreateResolvesExactReceiptOnceBeforeDeletingAndNeverResurrectsOnRetry() throws Exception {
    StudySet set = draft("first", "First");
    SaveRequest first = local.pendingOrCapture(user, "first");
    set.getCardsList().addCard(new Flashcard("later", "edit"));
    local.delete(user, "first");
    assertSame(first, local.pendingCreationForDeletion(user, "first"));
    assertThrows(CloudException.class, () -> local.pendingOrCapture(user, "first"));
    gateway.failedDeletes.add("first"); save.execute(user);
    assertEquals(List.of("save:first", "delete:first"), gateway.calls);
    assertSame(first, gateway.saves.get(0)); assertTrue(gateway.saves.get(0).snapshot.cards.isEmpty());
    assertNull(local.pendingCreationForDeletion(user, "first"));
    assertNull(local.local(user, "first")); assertTrue(local.hasUnsaved(user));
    gateway.failedDeletes.clear(); save.execute(user);
    assertEquals(List.of("save:first", "delete:first", "delete:first"), gateway.calls);
    assertEquals(1, gateway.saves.size()); assertFalse(local.hasUnsaved(user));
    local.acknowledge(first, 1); assertNull(local.local(user, "first"));
  }

  @Test public void failedFirstCreateResolutionRetainsReceiptAndSkipsDeleteUntilRetry() throws Exception {
    draft("first", "First"); SaveRequest first = local.pendingOrCapture(user, "first");
    local.delete(user, "first"); gateway.failedSaves.add("first");
    save.execute(user);
    assertEquals(List.of("save:first"), gateway.calls); assertEquals(1, output.result.failures.size());
    assertSame(first, local.pendingCreationForDeletion(user, "first")); assertTrue(local.hasUnsaved(user));
    gateway.failedSaves.clear(); save.execute(user);
    assertEquals(List.of("save:first", "save:first", "delete:first"), gateway.calls);
    assertSame(first, gateway.saves.get(1)); assertFalse(local.hasUnsaved(user));
  }

  @Test public void unsignedInBatchDoesNotSendOrClearQueuedWork() throws Exception {
    saved("delete", 1); local.delete(user, "delete");
    save.execute(null); assertNotNull(output.error); assertEquals(0, output.completed);
    assertTrue(gateway.calls.isEmpty()); assertTrue(local.hasUnsaved(user));
    save.execute("account-b"); assertTrue(gateway.calls.isEmpty()); assertTrue(local.hasUnsaved(user));
    assertEquals(0, output.result.deletedCount);
  }

  private static final class Gateway implements SaveAllStudySetsDataAccessInterface {
    final List<String> calls = new ArrayList<>();
    final List<SaveRequest> saves = new ArrayList<>();
    final Set<String> failedSaves = new HashSet<>(), failedDeletes = new HashSet<>();
    public long save(SaveRequest request) throws CloudException {
      calls.add("save:" + request.snapshot.id); saves.add(request);
      if (failedSaves.contains(request.snapshot.id)) { throw new CloudException("offline"); }
      return request.expectedRevision + 1;
    }
    public void delete(String userId, String setId) throws CloudException {
      calls.add("delete:" + setId);
      if (failedDeletes.contains(setId)) { throw new CloudException("offline"); }
    }
  }
  private static final class Output implements SaveAllStudySetsOutputBoundary {
    SaveAllStudySetsOutputData result; String error; int completed;
    public void savedAll(SaveAllStudySetsOutputData result) { this.result = result; completed++; }
    public void failed(String message) { error = message; }
  }
}
