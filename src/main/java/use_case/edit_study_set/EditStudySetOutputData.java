package use_case.edit_study_set;

import java.util.List;

/**
 * The data the Edit Study Set use case reports after a successful edit: what was done,
 * and the user's updated list of study sets.
 */
public class EditStudySetOutputData {
  private final EditStudySetAction action;
  private final String studySetId;
  private final String name;
  private final List<Item> studySets;

  /**
   * Creates output data.
   *
   * @param action     the action that was performed
   * @param studySetId the id of the study set that was edited
   * @param name       the study set's name after the edit (its last name, if it was deleted)
   * @param studySets  an entry for each of the user's study sets, in their new order
   */
  public EditStudySetOutputData(EditStudySetAction action, String studySetId,
                                String name, List<Item> studySets) {
    this.action = action;
    this.studySetId = studySetId;
    this.name = name;
    this.studySets = studySets;
  }

  /**
   * Returns the action that was performed.
   *
   * @return the action
   */
  public EditStudySetAction getAction() {
    return action;
  }

  /**
   * Returns the id of the edited study set.
   *
   * @return the study set's id
   */
  public String getStudySetId() {
    return studySetId;
  }

  /**
   * Returns the study set's name after the edit.
   *
   * @return the name after the edit, or its last name if it was deleted
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the user's updated study sets.
   *
   * @return an entry for each study set, in their new order
   */
  public List<Item> getStudySets() {
    return studySets;
  }

  /**
   * A summary of one study set: its id, name and number of flashcards.
   */
  public static class Item {
    private final String id;
    private final String name;
    private final int cardCount;

    /**
     * Creates an entry.
     *
     * @param id        the study set's id
     * @param name      the study set's name
     * @param cardCount the number of flashcards it contains
     */
    public Item(String id, String name, int cardCount) {
      this.id = id;
      this.name = name;
      this.cardCount = cardCount;
    }

    /**
     * Returns the id.
     *
     * @return the study set's id
     */
    public String getId() {
      return id;
    }

    /**
     * Returns the name.
     *
     * @return the study set's name
     */
    public String getName() {
      return name;
    }

    /**
     * Returns the number of flashcards.
     *
     * @return how many flashcards the study set contains
     */
    public int getCardCount() {
      return cardCount;
    }
  }
}