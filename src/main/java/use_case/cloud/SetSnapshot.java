package use_case.cloud;

import entity.CardsList;
import entity.Flashcard;
import entity.StudySet;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/** Immutable local commit/load data, independent of HTTP and JSON. */
public final class SetSnapshot {
  public final String id;
  public final String name;
  public final long revision;
  public final List<Card> cards;

  public SetSnapshot(String id, String name, long revision, List<Card> cards) {
    this.id = Objects.requireNonNull(id);
    this.name = Objects.requireNonNull(name);
    this.revision = revision;
    this.cards = List.copyOf(cards);
  }

  public static SetSnapshot capture(StudySet set, long revision) {
    List<Card> cards = new ArrayList<>();
    for (Flashcard card : set.getCardsList().getCards()) {
      cards.add(new Card(card.getId(), card.getQuestion(), card.getAnswer(), cards.size()));
    }
    return new SetSnapshot(set.getId(), set.getName(), revision, cards);
  }

  public StudySet toEntity() {
    CardsList list = new CardsList();
    for (Card card : cards) { list.addCard(new Flashcard(card.id, card.question, card.answer)); }
    return new StudySet(id, name, list);
  }

  public boolean sameContents(SetSnapshot other) {
    return other != null && id.equals(other.id) && name.equals(other.name) && cards.equals(other.cards);
  }

  public static final class Card {
    public final String id;
    public final String question;
    public final String answer;
    public final int position;

    public Card(String id, String question, String answer, int position) {
      this.id = id; this.question = question; this.answer = answer; this.position = position;
    }
    @Override public boolean equals(Object obj) {
      if (!(obj instanceof Card)) { return false; }
      Card other = (Card) obj;
      return id.equals(other.id) && question.equals(other.question)
          && answer.equals(other.answer) && position == other.position;
    }
    @Override public int hashCode() { return Objects.hash(id, question, answer, position); }
  }
}
