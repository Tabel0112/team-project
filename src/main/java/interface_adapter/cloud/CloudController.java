package interface_adapter.cloud;

import use_case.login.*;
import use_case.save_study_set.*;
import use_case.load_study_sets.*;
import use_case.cloud.WorkingStudySets;
import java.util.Arrays;
import java.util.concurrent.Executor;

/** Serial async entry point. Inputs are captured before dispatch; busy UI forbids account/set changes. */
public final class CloudController {
  private final LoginInputBoundary login;
  private final SaveStudySetInputBoundary save;
  private final LoadStudySetsInputBoundary load;
  private final LoginDataAccessInterface auth;
  private final WorkingStudySets local;
  private final CloudViewModel model;
  private final Executor worker;
  private final Executor ui;
  public CloudController(LoginInputBoundary login, SaveStudySetInputBoundary save,
      LoadStudySetsInputBoundary load, LoginDataAccessInterface auth, WorkingStudySets local,
      CloudViewModel model, Executor worker, Executor ui) {
    this.login = login; this.save = save; this.load = load; this.auth = auth;
    this.local = local; this.model = model; this.worker = worker; this.ui = ui;
  }
  private String userId() { return model.getUser() == null ? null : model.getUser().getId(); }
  public void login(String email, char[] password, boolean signup) {
    if (model.isBusy()) { Arrays.fill(password, '\0'); return; }
    // Existing drafts and summaries belong to this identity until explicit logout.
    String previousScope = model.scope();
    LoginInputData input = new LoginInputData(email, password, signup, userId());
    submit("Contacting authentication...", () -> login.execute(input), () -> {
      if (!signup && !java.util.Objects.equals(previousScope, model.scope())) { refresh(); }
    });
  }
  public void refresh() {
    String user = userId(); submit("Loading study sets...", () -> load.list(user), () -> { });
  }
  public void open(String setId) {
    String user = userId(); submit("Opening study set...", () -> load.open(user, setId), () -> { });
  }
  public void save(String setId) {
    SaveStudySetInputData input = new SaveStudySetInputData(userId(), setId);
    submit("Saving... Keep this window open until the request finishes.", () -> save.execute(input), () -> { });
  }
  public boolean hasUnsaved() { return local.hasUnsaved(userId()); }
  public void logout() {
    if (model.isBusy()) { return; }
    auth.forgetSession(); local.clear(); model.setUser(null);
    model.message("Logged out. Log in to load your saved study sets.");
  }
  private void submit(String message, Runnable task, Runnable after) {
    if (model.isBusy()) { return; }
    model.message(message); model.setBusy(true);
    worker.execute(() -> {
      try { task.run(); }
      catch (RuntimeException exception) {
        ui.execute(() -> model.message("The operation could not finish. Local changes were kept; retry when ready."));
      } finally {
        ui.execute(() -> { model.setBusy(false); after.run(); });
      }
    });
  }
}
