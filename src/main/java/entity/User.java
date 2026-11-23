package entity;

/** Authenticated identity. Credentials and tokens never belong to this entity. */
public final class User {
  private final String id;
  private final String email;

  public User(String id, String email) {
    this.id = java.util.Objects.requireNonNull(id);
    this.email = email == null ? "" : email;
  }

  public String getId() { return id; }
  public String getEmail() { return email; }
}
