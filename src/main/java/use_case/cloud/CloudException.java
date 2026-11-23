package use_case.cloud;

/** A deliberately sanitized, user-facing boundary failure; never a response body. */
public class CloudException extends Exception {
  public CloudException(String message) { super(message); }
}
