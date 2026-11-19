package use_case.reorder_flashcards;

import java.util.List;

/**
 * The Output Data for the Reorder Flashcards use case. It holds plain copies of the
 * flashcards' text, so outer layers never see the entities themselves.
 */
public class ReorderFlashcardsOutputData {
  private final List<Card> cards;
  private final int movedIndex;

  /**
   * Creates output data.
   *
   * @param cards      the study set's flashcards, in their new order
   * @param movedIndex the new position of the flashcard that was moved
   */
  public ReorderFlashcardsOutputData(List<Card> cards, int movedIndex) {
    this.cards = List.copyOf(cards);
    this.movedIndex = movedIndex;
  }

  /**
   * Returns the flashcards.
   *
   * @return the study set's flashcards, in their new order
   */
  public List<Card> getCards() { return cards; }

  /**
   * Returns the moved flashcard's new position.
   *
   * @return the new position of the flashcard that was moved, starting at 0
   */
  public int getMovedIndex() { return movedIndex; }

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
