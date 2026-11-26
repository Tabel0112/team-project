package data_access;

import entity.StudySet;
import use_case.add_flashcard.AddFlashcardDataAccessInterface;
import use_case.create_study_set.CreateStudySetDataAccessInterface;
import use_case.edit_study_set.EditStudySetDataAccessInterface;
import use_case.delete_flashcard.DeleteFlashcardDataAccessInterface;
import use_case.edit_flashcard.EditFlashcardDataAccessInterface;
import use_case.practice_study_set.PracticeStudySetDataAccessInterface;
import use_case.reorder_flashcards.ReorderFlashcardsDataAccessInterface;
import use_case.view_flashcards.ViewFlashcardsDataAccessInterface;
import use_case.cloud.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared local working repository. Its legacy save method registers a draft, never a cloud write. */
public class InMemoryStudySetDataAccessObject implements CreateStudySetDataAccessInterface,
    AddFlashcardDataAccessInterface, ViewFlashcardsDataAccessInterface,
    EditFlashcardDataAccessInterface, DeleteFlashcardDataAccessInterface,
    PracticeStudySetDataAccessInterface, ReorderFlashcardsDataAccessInterface,
    EditStudySetDataAccessInterface, WorkingStudySets {
  private final Map<String, Map<String, StudySet>> sets = new LinkedHashMap<>();
  private final Map<String, Map<String, SetSummary>> indexes = new LinkedHashMap<>();
  private final Map<String, SetSnapshot> baselines = new LinkedHashMap<>();
  private final Map<String, SaveRequest> pending = new LinkedHashMap<>();
  // Presentation indexes may contain drafts after local reorder. Only cloud list
  // responses establish that an unopened set may need a remote deletion.
  private final Map<String, Set<String>> cloudListedIds = new LinkedHashMap<>();
  private final Map<String, Set<String>> deletions = new LinkedHashMap<>();
  private final Map<String, SaveRequest> deletionCreates = new LinkedHashMap<>();

  /** Whether any loaded/local sets exist, retained for the login session boundary. */
  public synchronized boolean hasStudySets() {
    return sets.values().stream().anyMatch(owned -> !owned.isEmpty());
  }

  private String key(String user, String id) { return user + "/" + id; }

  @Override public synchronized boolean existsByName(String userId, String name) {
    return summaries(userId).stream().anyMatch(set -> set.name.equalsIgnoreCase(name));
  }
  @Override public synchronized void save(String userId, StudySet set) {
    if (userId == null || userId.isBlank()) { throw new IllegalArgumentException("Login required."); }
    if (queuedForDeletion(userId, set.getId())) { throw new IllegalStateException("This study set is queued for deletion."); }
    sets.computeIfAbsent(userId, ignored -> new LinkedHashMap<>()).put(set.getId(), set);
  }
  /** Removes local state immediately and defers any remote deletion until Save All. */
  @Override public synchronized void delete(String userId, String studySetId) {
    if (userId == null || userId.isBlank()) { throw new IllegalArgumentException("Login required."); }
    String key = key(userId, studySetId);
    if (baselines.containsKey(key) || pending.containsKey(key)
        || cloudListedIds.getOrDefault(userId, Set.of()).contains(studySetId)) {
      deletions.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(studySetId);
    }
    SaveRequest request = pending.get(key);
    if (request != null && request.expectedRevision == 0 && !baselines.containsKey(key)) {
      // Its first transaction may still be in flight. Resolve this same receipt
      // before deleting, so a late create cannot resurrect the removed set.
      deletionCreates.put(key, request);
    }
    Map<String, StudySet> owned = sets.get(userId);
    if (owned != null) { owned.remove(studySetId); }
    Map<String, SetSummary> index = indexes.get(userId);
    if (index != null) { index.remove(studySetId); }
    baselines.remove(key); pending.remove(key);
  }
  private boolean queuedForDeletion(String userId, String studySetId) {
    return deletions.getOrDefault(userId, Set.of()).contains(studySetId);
  }
  @Override public synchronized List<String> pendingDeletions(String userId) {
    return List.copyOf(deletions.getOrDefault(userId, Set.of()));
  }
  @Override public synchronized SaveRequest pendingCreationForDeletion(String userId, String setId) {
    return deletionCreates.get(key(userId, setId));
  }
  @Override public synchronized void acknowledgeCreationForDeletion(SaveRequest request) {
    String key = key(request.userId, request.snapshot.id);
    if (deletionCreates.get(key) == request) { deletionCreates.remove(key); }
  }
  @Override public synchronized void acknowledgeDeletion(String userId, String setId) {
    Set<String> queued = deletions.get(userId);
    if (queued == null || !queued.remove(setId)) { return; }
    Set<String> listed = cloudListedIds.get(userId);
    if (listed != null) { listed.remove(setId); }
    deletionCreates.remove(key(userId, setId));
  }
  @Override public synchronized List<StudySet> getStudySets(String userId) {
    List<StudySet> result = new ArrayList<>();
    for (SetSummary summary : summaries(userId)) {
      StudySet loaded = sets.getOrDefault(userId, Map.of()).get(summary.id);
      result.add(loaded == null ? new StudySet(summary.id, summary.name) : loaded);
    }
    return List.copyOf(result);
  }
  @Override public synchronized StudySet getStudySetById(String setId) {
    for (Map<String, StudySet> owned : sets.values()) {
      if (owned.containsKey(setId)) { return owned.get(setId); }
    }
    return null;
  }
  @Override public synchronized boolean contains(String user, String id) {
    return sets.getOrDefault(user, Map.of()).containsKey(id);
  }
  @Override public synchronized SetSnapshot local(String user, String id) {
    StudySet set = sets.getOrDefault(user, Map.of()).get(id);
    if (set == null) { return null; }
    SetSnapshot base = baselines.get(key(user, id));
    return SetSnapshot.capture(set, base == null ? 0 : base.revision);
  }
  @Override public synchronized boolean isDirty(String user, String id) {
    SetSnapshot current = local(user, id);
    return current != null && (!current.sameContents(baselines.get(key(user, id)))
        || pending.containsKey(key(user, id)));
  }
  @Override public synchronized boolean hasUnsaved(String user) {
    return !pendingDeletions(user).isEmpty() || !dirtySetIds(user).isEmpty();
  }
  @Override public synchronized List<String> dirtySetIds(String user) {
    List<String> dirty = new ArrayList<>();
    for (String id : sets.getOrDefault(user, Map.of()).keySet()) {
      if (!queuedForDeletion(user, id) && isDirty(user, id)) { dirty.add(id); }
    }
    return List.copyOf(dirty);
  }
  @Override public synchronized SaveRequest pendingOrCapture(String user, String id) throws CloudException {
    if (queuedForDeletion(user, id)) { throw new CloudException("This study set is queued for deletion. Click Save All to finish."); }
    String key = key(user, id);
    if (pending.containsKey(key)) { return pending.get(key); }
    SetSnapshot snapshot = local(user, id);
    if (snapshot == null) { throw new CloudException("Open a study set before saving."); }
    if (!isDirty(user, id)) { return null; }
    SaveRequest request = new SaveRequest(user, snapshot, baselines.get(key));
    pending.put(key, request);
    return request;
  }
  @Override public synchronized void acknowledge(SaveRequest request, long revision) {
    String key = key(request.userId, request.snapshot.id);
    // Ignore a completion from a cleared/replaced working session.
    if (pending.get(key) != request) { return; }
    baselines.put(key, new SetSnapshot(request.snapshot.id, request.snapshot.name,
        revision, request.snapshot.cards));
    pending.remove(key);
  }
  @Override public synchronized void install(String user, SetSnapshot snapshot) throws CloudException {
    if (queuedForDeletion(user, snapshot.id)) { throw new CloudException("This study set is queued for deletion. Click Save All to finish."); }
    if (contains(user, snapshot.id)) { throw new CloudException("A local copy is already open; it was kept."); }
    save(user, snapshot.toEntity());
    baselines.put(key(user, snapshot.id), snapshot);
  }
  @Override public synchronized void mergeSummaries(String user, List<SetSummary> summaries) {
    Map<String, SetSummary> replacement = new LinkedHashMap<>();
    Set<String> listed = new LinkedHashSet<>();
    for (SetSummary summary : summaries) {
      listed.add(summary.id);
      if (!queuedForDeletion(user, summary.id)) { replacement.put(summary.id, summary); }
    }
    cloudListedIds.put(user, listed);
    indexes.put(user, replacement);
  }
  @Override public synchronized List<SetSummary> summaries(String user) {
    Map<String, SetSummary> result = new LinkedHashMap<>(indexes.getOrDefault(user, Map.of()));
    for (StudySet set : sets.getOrDefault(user, Map.of()).values()) {
      result.put(set.getId(), new SetSummary(set.getId(), set.getName()));
    }
    result.keySet().removeIf(id -> queuedForDeletion(user, id));
    return List.copyOf(result.values());
  }
  @Override public synchronized void clear() {
    sets.clear(); indexes.clear(); baselines.clear(); pending.clear();
    cloudListedIds.clear(); deletions.clear(); deletionCreates.clear();
  }
  @Override public synchronized void rename(String userId, String studySetId, String newName) {
    Map<String, StudySet> owned = sets.get(userId);
    StudySet loaded = owned == null ? null : owned.get(studySetId);
    if (loaded != null) {
      // Same key, so the set keeps its position in the map.
      owned.put(studySetId, new StudySet(loaded.getId(), newName, loaded.getCardsList()));
    }
    Map<String, SetSummary> index = indexes.get(userId);
    if (index != null && index.containsKey(studySetId)) {
      index.put(studySetId, new SetSummary(studySetId, newName));
    }
  }
  @Override public synchronized void move(String userId, String studySetId, int newIndex) {
    List<SetSummary> order = new ArrayList<>(summaries(userId));
    int from = -1;
    for (int i = 0; i < order.size(); i++) {
      if (order.get(i).id.equals(studySetId)) { from = i; break; }
    }
    if (from < 0) { return; }
    SetSummary moved = order.remove(from);
    order.add(Math.max(0, Math.min(newIndex, order.size())), moved);
    // summaries() lists the index first, so writing the new order there fixes the order
    // for every set, loaded or not.
    Map<String, SetSummary> replacement = new LinkedHashMap<>();
    for (SetSummary summary : order) { replacement.put(summary.id, summary); }
    indexes.put(userId, replacement);
  }
}

