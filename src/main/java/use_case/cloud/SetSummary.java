package use_case.cloud;

/** Cloud list entry; cards are loaded only when the set is opened. */
public final class SetSummary {
  public final String id;
  public final String name;
  public SetSummary(String id, String name) { this.id = id; this.name = name; }
}
