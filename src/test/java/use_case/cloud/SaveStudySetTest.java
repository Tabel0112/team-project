package use_case.cloud;

import data_access.InMemoryStudySetDataAccessObject;
import entity.*;
import org.junit.Test;
import use_case.save_study_set.*;
import use_case.load_study_sets.*;
import use_case.create_study_set.*;
import use_case.add_flashcard.*;
import use_case.view_flashcards.*;
import use_case.edit_flashcard.*;
import use_case.delete_flashcard.*;
import java.util.*;
import static org.junit.Assert.*;

public class SaveStudySetTest {
  private final String user = UUID.randomUUID().toString();
  private final String id = UUID.randomUUID().toString();
  private final InMemoryStudySetDataAccessObject local = new InMemoryStudySetDataAccessObject();
  private final Output output = new Output();
  private final List<SaveRequest> calls = new ArrayList<>();

  private SaveStudySetInteractor saver(SaveStudySetDataAccessInterface gateway) {
    return new SaveStudySetInteractor(local, gateway, output);
  }
  private StudySet draft() {
    StudySet set = new StudySet(id, "Course"); local.save(user, set); return set;
  }
  @Test public void firstSaveThenRepeatedSaveDoesNotDuplicateOrCallCloudAgain() {
    StudySet set = draft(); set.getCardsList().addCard(new Flashcard("q", "a"));
    SaveStudySetInteractor save = saver(request -> { calls.add(request); return 1; });
    assertTrue(local.isDirty(user, id));
    save.execute(new SaveStudySetInputData(user, id));
    assertFalse(local.isDirty(user, id)); assertFalse(output.saved.stillDirty);
    save.execute(new SaveStudySetInputData(user, id));
    assertEquals(1, calls.size()); assertEquals(1, calls.get(0).upserts.size());
    assertEquals(set.getCardsList().getCards().get(0).getId(), calls.get(0).upserts.get(0).id);
  }
  @Test public void editsDeletionAndOrderAreComparedToAcknowledgedLocalBaseline() throws Exception {
    Flashcard a = new Flashcard("q1", "a1"), b = new Flashcard("q2", "a2"), c = new Flashcard("q3", "a3");
    StudySet set = draft(); set.getCardsList().addCard(a); set.getCardsList().addCard(b); set.getCardsList().addCard(c);
    SaveRequest initial = local.pendingOrCapture(user, id); local.acknowledge(initial, 1);
    set.getCardsList().removeCard(a); set.getCardsList().removeCard(b); set.getCardsList().removeCard(c);
    set.getCardsList().addCard(c);
    set.getCardsList().addCard(new Flashcard(b.getId(), "changed", "answer"));
    SaveRequest update = local.pendingOrCapture(user, id);
    assertEquals(List.of(a.getId()), update.deletedIds);
    assertEquals(2, update.upserts.size()); assertEquals(1, update.expectedRevision);
    assertEquals(c.getId(), update.upserts.get(0).id); assertEquals(0, update.upserts.get(0).position);
    assertEquals("changed", update.upserts.get(1).question);
  }
  @Test public void orderingOnlyChangeIsDirty() throws Exception {
    StudySet set = draft(); Flashcard a = new Flashcard("a", "1"), b = new Flashcard("b", "2");
    set.getCardsList().addCard(a); set.getCardsList().addCard(b);
    local.acknowledge(local.pendingOrCapture(user, id), 1);
    set.getCardsList().removeCard(a); set.getCardsList().addCard(a);
    assertTrue(local.isDirty(user, id)); assertEquals(2, local.pendingOrCapture(user, id).upserts.size());
  }
  @Test public void responseLossRetryUsesSameOperationAndNeverClearsLaterEdits() throws Exception {
    StudySet set = draft(); set.getCardsList().addCard(new Flashcard("first", "answer"));
    SaveStudySetInteractor failing = saver(request -> { calls.add(request); throw new CloudException("offline"); });
    failing.execute(new SaveStudySetInputData(user, id));
    assertEquals("offline", output.error); assertTrue(local.isDirty(user, id));
    set.getCardsList().addCard(new Flashcard("later", "answer"));
    saver(request -> { calls.add(request); return 1; }).execute(new SaveStudySetInputData(user, id));
    assertSame(calls.get(0), calls.get(1)); assertTrue(output.saved.stillDirty);
    assertEquals(1, calls.get(1).snapshot.cards.size());
    SaveRequest later = local.pendingOrCapture(user, id);
    assertNotEquals(calls.get(0).operationId, later.operationId);
    assertEquals(1, later.expectedRevision); assertEquals(1, later.upserts.size());
  }
  @Test public void renameIsSavedAndStaleCompletionAfterClearCannotAcknowledgeNewSession() throws Exception {
    draft(); SaveRequest old = local.pendingOrCapture(user, id); local.clear(); draft();
    local.acknowledge(old, 1); assertTrue(local.isDirty(user, id));
    SaveRequest initial = local.pendingOrCapture(user, id); local.acknowledge(initial, 1);
    local.save(user, new StudySet(id, "New name"));
    SaveRequest rename = local.pendingOrCapture(user, id);
    assertEquals("New name", rename.snapshot.name); assertTrue(rename.upserts.isEmpty());
  }
  @Test public void loginAndOwnershipAreRequiredBeforeSave() {
    draft();
    SaveStudySetInteractor save = saver(request -> { fail("No cloud call permitted"); return 1; });
    save.execute(new SaveStudySetInputData(null, id)); assertNotNull(output.error);
    save.execute(new SaveStudySetInputData("other-user", id)); assertNotNull(output.error);
    assertTrue(local.isDirty(user, id));
  }
  @Test public void restartLoadRestoresIdsTextNameAndOrder() throws Exception {
    StudySet set = draft(); set.getCardsList().addCard(new Flashcard("Q", "A"));
    SetSnapshot cloud = SetSnapshot.capture(set, 3);
    local.clear(); local.install(user, cloud);
    assertFalse(local.isDirty(user, id));
    StudySet loaded = local.getStudySetById(id);
    assertEquals("Course", loaded.getName());
    assertEquals(set.getCardsList().getCards().get(0).getId(), loaded.getCardsList().getCards().get(0).getId());
    assertEquals("A", loaded.getCardsList().getCards().get(0).getAnswer());
  }
  @Test public void loadFailureAndListRefreshKeepDraftAndOpenLocalDoesNotRefetch() {
    StudySet set = draft(); set.getCardsList().addCard(new Flashcard("local", "answer"));
    LoadStudySetsDataAccessInterface gateway = new LoadStudySetsDataAccessInterface() {
      public List<SetSummary> list(String userId) throws CloudException { throw new CloudException("offline"); }
      public SetSnapshot load(String userId, String setId) throws CloudException { throw new CloudException("offline"); }
    };
    LoadOutput out = new LoadOutput(); LoadStudySetsInteractor load = new LoadStudySetsInteractor(local, gateway, out);
    load.list(user); assertEquals("offline", out.error); assertTrue(local.hasUnsaved(user));
    load.open(user, id); assertEquals("local", out.opened.cards.get(0).question);
    load.open(user, UUID.randomUUID().toString()); assertEquals("offline", out.error);
    assertSame(set, local.getStudySetById(id));
  }
  @Test public void createGeneratedAddAndViewUseOneWorkingRepositoryWithoutCloudWrites() throws Exception {
    final String[] created = {null};
    CreateStudySetInteractor create = new CreateStudySetInteractor(local, new CreateStudySetOutputBoundary() {
      public void prepareSuccessView(CreateStudySetOutputData data) { created[0] = data.getStudySetId(); }
      public void prepareFailView(String error) { fail(error); }
    }, new StudySetFactory());
    create.createFromNotes(user, "Lecture", List.of(new Flashcard("generated", "answer")));
    new AddFlashcardInteractor(local, new AddFlashcardOutputBoundary() {
      public void prepareSuccessView(AddFlashcardOutputData data) { assertEquals(2, data.getCardCount()); }
      public void prepareFailView(String error) { fail(error); }
    }).execute(new AddFlashcardInputData(created[0], "manual", "answer"));
    new ViewFlashcardsInteractor(local, new ViewFlashcardsOutputBoundary() {
      public void prepareSuccessView(ViewFlashcardsOutputData data) { assertEquals(2, data.getCards().size()); }
      public void prepareFailView(String error) { fail(error); }
    }).execute(new ViewFlashcardsInputData(created[0]));
    assertEquals(2, local.pendingOrCapture(user, created[0]).upserts.size());
    assertTrue(local.hasUnsaved(user));
  }
  @Test public void teammateEditAndDeleteCommitStableIdsTombstonesAndShiftedOrder() throws Exception {
    StudySet set = draft();
    Flashcard first = new Flashcard("first", "answer1");
    Flashcard edited = new Flashcard("second", "answer2");
    Flashcard last = new Flashcard("third", "answer3");
    set.getCardsList().addCard(first); set.getCardsList().addCard(edited); set.getCardsList().addCard(last);
    local.acknowledge(local.pendingOrCapture(user, id), 1);

    new EditFlashcardInteractor(local, new EditFlashcardOutputBoundary() {
      public void prepareSuccessView(EditFlashcardOutputData data) { }
      public void prepareFailView(String error) { fail(error); }
    }).execute(new EditFlashcardInputData(id, 1, "second updated", "new answer"));
    assertTrue(local.isDirty(user, id));
    saver(request -> { calls.add(request); return 2; }).execute(new SaveStudySetInputData(user, id));
    SaveRequest edit = calls.get(0);
    assertEquals(1, edit.upserts.size()); assertTrue(edit.deletedIds.isEmpty());
    assertEquals(edited.getId(), edit.upserts.get(0).id);
    assertEquals("second updated", edit.upserts.get(0).question);
    assertFalse(local.isDirty(user, id));

    new DeleteFlashcardInteractor(local, new DeleteFlashcardOutputBoundary() {
      public void prepareSuccessView(DeleteFlashcardOutputData data) { assertEquals(2, data.getCardCount()); }
      public void prepareFailView(String error) { fail(error); }
    }).execute(new DeleteFlashcardInputData(id, 0));
    saver(request -> { calls.add(request); return 3; }).execute(new SaveStudySetInputData(user, id));
    SaveRequest deletion = calls.get(1);
    assertEquals(List.of(first.getId()), deletion.deletedIds);
    assertEquals(2, deletion.upserts.size());
    assertEquals(edited.getId(), deletion.upserts.get(0).id); assertEquals(0, deletion.upserts.get(0).position);
    assertEquals(last.getId(), deletion.upserts.get(1).id); assertEquals(1, deletion.upserts.get(1).position);
    assertFalse(output.saved.stillDirty);
    local.clear();
    local.install(user, new SetSnapshot(id, deletion.snapshot.name, 3, deletion.snapshot.cards));
    assertEquals("new answer", local.local(user, id).cards.get(0).answer);
    assertEquals(edited.getId(), local.getStudySetById(id).getCardsList().getCards().get(0).getId());
    assertFalse(local.isDirty(user, id));
  }
  private static class Output implements SaveStudySetOutputBoundary {
    String error; SaveStudySetOutputData saved;
    public void saved(SaveStudySetOutputData data) { saved = data; }
    public void failed(String message) { error = message; }
  }
  private static class LoadOutput implements LoadStudySetsOutputBoundary {
    String error; SetSnapshot opened;
    public void listed(List<SetSummary> sets) { }
    public void refreshed(List<SetSummary> sets) { }
    public void opened(SetSnapshot set) { opened = set; }
    public void failed(String message) { error = message; }
  }
}
