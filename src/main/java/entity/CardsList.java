package entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The flashcards section of a {@link StudySet}.
 */
public class CardsList {
  private final List<Flashcard> cards = new ArrayList<>();

  /**
   * Creates an empty cards list.
   */
  public CardsList() { }

  /**
   * Adds a flashcard to the end of the list.
   *
   * @param card the flashcard to add
   */
  public void addCard(Flashcard card) { cards.add(card); }

  /**
   * Removes a flashcard from the list, if present.
   *
   * @param card the flashcard to remove
   */
  public void removeCard(Flashcard card) { cards.remove(card); }

  /**
   * Replaces a flashcard's text, keeping its identity and place in the list.
   *
   * @param index the position of the flashcard to replace, starting at 0
   * @param card  the flashcard to put in its place
   * @throws IndexOutOfBoundsException if there is no flashcard at that position
   */
  public void replaceCard(int index, Flashcard card) {
    final String id = cards.get(index).getId();
    cards.set(index, new Flashcard(id, card.getQuestion(), card.getAnswer()));
  }

  /**
   * Moves a flashcard to a new position. The flashcards in between shift by one
   * place to make room.
   *
   * @param fromIndex the current position of the flashcard, starting at 0
   * @param toIndex   the position to move it to, starting at 0
   * @throws IndexOutOfBoundsException if either position is outside the list
   */
  public void moveCard(int fromIndex, int toIndex) {
    cards.add(toIndex, cards.remove(fromIndex));
  }

  /**
   * Returns the flashcards in this list.
   *
   * @return an unmodifiable view of the flashcards
   */
  public List<Flashcard> getCards() { return Collections.unmodifiableList(cards); }

  /**
   * Returns the number of flashcards.
   *
   * @return the number of flashcards in the list
   */
  public int size() { return cards.size(); }

  /**
   * Checks whether the list has no flashcards.
   *
   * @return true if the list is empty
   */
  public boolean isEmpty() { return cards.isEmpty(); }
}