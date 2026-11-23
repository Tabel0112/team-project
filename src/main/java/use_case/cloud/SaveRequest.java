package use_case.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One retryable atomic commit. Stable operation ID survives ambiguous network failures. */
public final class SaveRequest {
  public final String operationId;
  public final String userId;
  public final SetSnapshot snapshot;
  public final long expectedRevision;
  public final List<SetSnapshot.Card> upserts;
  public final List<String> deletedIds;

  public SaveRequest(String userId, SetSnapshot snapshot, SetSnapshot baseline) {
    this.operationId = UUID.randomUUID().toString();
    this.userId = userId;
    this.snapshot = snapshot;
    this.expectedRevision = baseline == null ? 0 : baseline.revision;
    List<SetSnapshot.Card> changed = new ArrayList<>();
    for (SetSnapshot.Card card : snapshot.cards) {
      if (baseline == null || !baseline.cards.contains(card)) { changed.add(card); }
    }
    List<String> removed = new ArrayList<>();
    if (baseline != null) {
      for (SetSnapshot.Card card : baseline.cards) {
        if (snapshot.cards.stream().noneMatch(current -> current.id.equals(card.id))) {
          removed.add(card.id);
        }
      }
    }
    this.upserts = List.copyOf(changed);
    this.deletedIds = List.copyOf(removed);
  }
}
