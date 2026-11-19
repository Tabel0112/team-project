package data_access;

import entity.StudySet;
import use_case.add_flashcard.AddFlashcardDataAccessInterface;
import use_case.create_study_set.CreateStudySetDataAccessInterface;
import use_case.delete_flashcard.DeleteFlashcardDataAccessInterface;
import use_case.edit_flashcard.EditFlashcardDataAccessInterface;
import use_case.view_flashcards.ViewFlashcardsDataAccessInterface;
import use_case.cloud.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared local working repository. Its legacy save method registers a draft, never a cloud write. */
public class InMemoryStudySetDataAccessObject implements CreateStudySetDataAccessInterface,
    AddFlashcardDataAccessInterface, ViewFlashcardsDataAccessInterface, WorkingStudySets {
  private final Map<String, Map<String, StudySet>> sets = new LinkedHashMap<>();
  private final Map<String, Map<String, SetSummary>> indexes = new LinkedHashMap<>();
  private final Map<String, SetSnapshot> baselines = new LinkedHashMap<>();
  private final Map<String, SaveRequest> pending = new LinkedHashMap<>();
        AddFlashcardDataAccessInterface, ViewFlashcardsDataAccessInterface,
        EditFlashcardDataAccessInterface, DeleteFlashcardDataAccessInterface {
  private final Map<String, List<StudySet>> setsByUser = new HashMap<>();

  private String key(String user, String id) { return user + "/" + id; }

  @Override public synchronized boolean existsByName(String userId, String name) {
    return summaries(userId).stream().anyMatch(set -> set.name.equalsIgnoreCase(name));
  }
  @Override public synchronized void save(String userId, StudySet set) {
    if (userId == null || userId.isBlank()) { throw new IllegalArgumentException("Login required."); }
    sets.computeIfAbsent(userId, ignored -> new LinkedHashMap<>()).put(set.getId(), set);
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
    return sets.getOrDefault(user, Map.of()).keySet().stream().anyMatch(id -> isDirty(user, id));
  }
  @Override public synchronized SaveRequest pendingOrCapture(String user, String id) throws CloudException {
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
    if (contains(user, snapshot.id)) { throw new CloudException("A local copy is already open; it was kept."); }
    save(user, snapshot.toEntity());
    baselines.put(key(user, snapshot.id), snapshot);
  }
  @Override public synchronized void mergeSummaries(String user, List<SetSummary> summaries) {
    Map<String, SetSummary> replacement = new LinkedHashMap<>();
    for (SetSummary summary : summaries) { replacement.put(summary.id, summary); }
    indexes.put(user, replacement);
  }
  @Override public synchronized List<SetSummary> summaries(String user) {
    Map<String, SetSummary> result = new LinkedHashMap<>(indexes.getOrDefault(user, Map.of()));
    for (StudySet set : sets.getOrDefault(user, Map.of()).values()) {
      result.put(set.getId(), new SetSummary(set.getId(), set.getName()));
    }
    return List.copyOf(result.values());
  }
  @Override public synchronized void clear() {
    sets.clear(); indexes.clear(); baselines.clear(); pending.clear();
  }
}
