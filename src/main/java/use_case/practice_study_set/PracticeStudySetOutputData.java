package use_case.practice_study_set;

import java.util.List;

/**
 * The Output Data for the Practice Study Set use case. It holds plain copies of
 * the flashcards' text, so outer layers never see the entities themselves.
 */
public class PracticeStudySetOutputData {
  private final String studySetName;
  private final List<Card> cards;

  /**
   * Creates output data.
   *
   * @param studySetName the name of the study set
   * @param cards        the study set's flashcards, in order
   */
  public PracticeStudySetOutputData(String studySetName, List<Card> cards) {
    this.studySetName = studySetName;
    this.cards = List.copyOf(cards);
  }

  /**
   * Returns the study set's name.
   *
   * @return the name of the study set
   */
  public String getStudySetName() { return studySetName; }

  /**
   * Returns the flashcards.
   *
   * @return the study set's flashcards, in order
   */
  public List<Card> getCards() { return cards; }

  /**
   * The text of one flashcard.
   */
  public static class Card {
    private final String question;
    private final String answer;

    /**
     * Creates a card.
     *
     * @param question the front of the flashcard
     * @param answer   the back of the flashcard
     */
    public Card(String question, String answer) {
      this.question = question;
      this.answer = answer;
    }

    /**
     * Returns the question.
     *
     * @return the front of the flashcard
     */
    public String getQuestion() { return question; }

    /**
     * Returns the answer.
     *
     * @return the back of the flashcard
     */
    public String getAnswer() { return answer; }
  }
}
