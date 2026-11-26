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
import use_case.load_study_sets.*;
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
    local.mergeSummaries(user, List.of()); // An absent ID stays queued after an ambiguous delete failure.
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

  @Test public void listKeepsQueuedDeletionsWhileExplicitRefreshDiscardsThemAndPresentationOnlyDrafts() throws Exception {
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
    gateway.listed = List.of(new SetSummary("remote", "Remote"));
    Loads out = new Loads(); LoadStudySetsInteractor load = new LoadStudySetsInteractor(local, gateway, out);
    load.list(user);
    assertEquals(List.of("remote"), local.pendingDeletions(user)); assertTrue(out.listed.isEmpty());
    load.refresh(user);
    assertEquals("remote", local.summaries(user).get(0).id);
    assertTrue(local.pendingDeletions(user).isEmpty()); assertFalse(local.hasUnsaved(user));
    assertNull(local.local(user, "remote")); assertFalse(local.contains(user, "draft"));
    save.execute(user); assertTrue(gateway.calls.isEmpty());
    local.clear(); assertFalse(local.hasUnsaved(user)); assertTrue(local.pendingDeletions(user).isEmpty());
  }

  @Test public void successfulRefreshDiscardsAllCurrentAccountWorkAndKeepsOtherAccounts() throws Exception {
    saved("restore", 3);
    local.getStudySetById("restore").getCardsList().addCard(new Flashcard("discarded", "edit"));
    local.delete(user, "restore");
    draft("confirmed", "Saved first create"); local.pendingOrCapture(user, "confirmed");
    local.delete(user, "confirmed");
    draft("absent", "Same name"); SaveRequest absent = local.pendingOrCapture(user, "absent");
    local.delete(user, "absent");
    saved("edited", 2);
    StudySet edited = local.getStudySetById("edited");
    edited.getCardsList().addCard(new Flashcard("keep", "edit"));
    SaveRequest pendingEdit = local.pendingOrCapture(user, "edited");
    saved("clean", 4);
    draft("draft", "Discard draft");
    draft("discarded", "Discard draft"); local.delete(user, "discarded");
    local.install("account-b", new SetSnapshot("restore", "Other account", 1, List.of()));
    local.delete("account-b", "restore");
    local.save("account-b", new StudySet("other", "Other account draft"));
    SaveRequest otherPending = local.pendingOrCapture("account-b", "other");
    local.install("account-b", new SetSnapshot("other-clean", "Other saved", 7, List.of()));
    local.save("account-b", new StudySet("other-first", "Other first save"));
    SaveRequest otherCreation = local.pendingOrCapture("account-b", "other-first");
    local.delete("account-b", "other-first");
    gateway.listed = List.of(new SetSummary("restore", "Saved version"),
        new SetSummary("confirmed", "Saved first create"), new SetSummary("different-id", "Same name"));
    Loads out = new Loads(); LoadStudySetsInteractor load = new LoadStudySetsInteractor(local, gateway, out);
    load.refresh(user);
    assertNull(out.error); assertTrue(local.pendingDeletions(user).isEmpty());
    assertEquals(List.of("restore", "other-first"), local.pendingDeletions("account-b"));
    assertSame(otherCreation, local.pendingCreationForDeletion("account-b", "other-first"));
    assertEquals(7, local.local("account-b", "other-clean").revision);
    assertFalse(local.isDirty("account-b", "other-clean"));
    assertNull(local.pendingCreationForDeletion(user, "confirmed"));
    assertNull(local.pendingCreationForDeletion(user, "absent"));
    assertNull(local.local(user, "edited")); assertNull(local.local(user, "clean"));
    assertNull(local.local(user, "draft")); assertNull(local.local(user, "discarded"));
    assertFalse(local.hasUnsaved(user)); assertSame(otherPending, local.pendingOrCapture("account-b", "other"));
    assertEquals(gateway.listed, out.refreshed);
    assertNull(local.local(user, "restore"));
    load.open(user, "restore");
    assertEquals("Saved version", out.opened.name); assertEquals(9, out.opened.revision);
    assertTrue(out.opened.cards.isEmpty()); assertFalse(local.isDirty(user, "restore"));
    save.execute(user);
    assertTrue(gateway.calls.isEmpty());
    local.acknowledge(pendingEdit, 99); local.acknowledge(absent, 99);
    draft("edited", "New draft reusing its ID");
    SaveRequest replacement = local.pendingOrCapture(user, "edited");
    assertEquals(0, replacement.expectedRevision); assertNotEquals(pendingEdit.operationId, replacement.operationId);
  }

  @Test public void failedRefreshKeepsQueuedDeletionsAndUnresolvedCreateReceipts() throws Exception {
    saved("saved", 1); local.delete(user, "saved");
    draft("first", "First"); SaveRequest first = local.pendingOrCapture(user, "first");
    local.delete(user, "first");
    saved("clean", 4); saved("dirty", 2);
    StudySet dirty = local.getStudySetById("dirty"); dirty.getCardsList().addCard(new Flashcard("keep", "edit"));
    SaveRequest pendingSave = local.pendingOrCapture(user, "dirty"); StudySet draft = draft("draft", "Keep draft");
    List<SetSummary> prior = local.summaries(user);
    gateway.listed = List.of(new SetSummary("saved", "Saved"), new SetSummary("first", "First"));
    gateway.failedList = true;
    Loads out = new Loads(); new LoadStudySetsInteractor(local, gateway, out).refresh(user);
    assertEquals("offline", out.error); assertNull(out.listed);
    assertEquals(List.of("saved", "first"), local.pendingDeletions(user));
    assertSame(first, local.pendingCreationForDeletion(user, "first"));
    assertNull(out.refreshed); assertSame(dirty, local.getStudySetById("dirty"));
    assertSame(draft, local.getStudySetById("draft")); assertEquals(4, local.local(user, "clean").revision);
    assertSame(pendingSave, local.pendingOrCapture(user, "dirty"));
    assertEquals(prior.size(), local.summaries(user).size()); assertTrue(local.hasUnsaved(user));
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

  private static final class Gateway implements SaveAllStudySetsDataAccessInterface, LoadStudySetsDataAccessInterface {
    final List<String> calls = new ArrayList<>();
    final List<SaveRequest> saves = new ArrayList<>();
    final Set<String> failedSaves = new HashSet<>(), failedDeletes = new HashSet<>();
    List<SetSummary> listed = List.of(); boolean failedList;
    public List<SetSummary> list(String userId) throws CloudException {
      if (failedList) { throw new CloudException("offline"); } return listed;
    }
    public SetSnapshot load(String userId, String setId) { return new SetSnapshot(setId, "Saved version", 9, List.of()); }
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
  private static final class Loads implements LoadStudySetsOutputBoundary {
    List<SetSummary> listed; List<SetSummary> refreshed; SetSnapshot opened; String error;
    public void listed(List<SetSummary> sets) { listed = sets; }
    public void refreshed(List<SetSummary> sets) { refreshed = sets; }
    public void opened(SetSnapshot set) { opened = set; }
    public void failed(String message) { error = message; }
  }
  private static final class Output implements SaveAllStudySetsOutputBoundary {
    SaveAllStudySetsOutputData result; String error; int completed;
    public void savedAll(SaveAllStudySetsOutputData result) { this.result = result; completed++; }
    public void failed(String message) { error = message; }
  }
}
