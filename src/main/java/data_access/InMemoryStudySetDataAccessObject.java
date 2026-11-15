package data_access;

import entity.StudySet;
import use_case.add_flashcard.AddFlashcardDataAccessInterface;
import use_case.create_study_set.CreateStudySetDataAccessInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Temporary in-memory storage for study sets. The link between a user and
 * their study sets lives here, not in the {@link StudySet} entity.
 */
public class InMemoryStudySetDataAccessObject implements CreateStudySetDataAccessInterface,
        AddFlashcardDataAccessInterface {
  private final Map<String, List<StudySet>> setsByUser = new HashMap<>();

  /**
   * Checks whether the user already has a study set with the given name.
   *
   * @param userId the id of the user
   * @param name   the name to look for, compared ignoring case
   * @return true if the user already has a study set with that name
   */
  @Override
  public boolean existsByName(String userId, String name) {
    return setsByUser.getOrDefault(userId, List.of()).stream()
            .anyMatch(s -> s.getName().equalsIgnoreCase(name));
  }

  /**
   * Saves a study set under the given user.
   *
   * @param userId   the id of the user who owns the study set
   * @param studySet the study set to save
   */
  @Override
  public void save(String userId, StudySet studySet) {
    setsByUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(studySet);
  }

  /**
   * Returns all study sets belonging to a user.
   *
   * @param userId the id of the user
   * @return the user's study sets, in the order they were created (empty if none)
   */
  @Override
  public List<StudySet> getStudySets(String userId) {
    return List.copyOf(setsByUser.getOrDefault(userId, List.of()));
  }

  /**
   * Finds a study set by its id, among every user's study sets.
   *
   * @param studySetId the id of the study set
   * @return the study set, or null if there is no study set with that id
   */
  @Override
  public StudySet getStudySetById(String studySetId) {
    for (List<StudySet> sets : setsByUser.values()) {
      for (StudySet studySet : sets) {
        if (studySet.getId().equals(studySetId)) {
          return studySet;
        }
      }
    }
    return null;
  }
}