package interface_adapter.cloud;

import interface_adapter.login.LoginViewModel;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import use_case.load_study_sets.LoadStudySetsInputBoundary;
import use_case.save_study_set.SaveAllStudySetsInputBoundary;
import use_case.save_study_set.SaveStudySetInputBoundary;
import use_case.save_study_set.SaveStudySetInputData;

/** Captures identity/set input and serializes cloud requests with authentication. */
public final class CloudController {
  private final SaveStudySetInputBoundary save;
  private final SaveAllStudySetsInputBoundary saveAll;
  private final LoadStudySetsInputBoundary load;
  private final LoginViewModel model;
  private final Executor worker;
  private final Executor ui;

  public CloudController(SaveStudySetInputBoundary save, LoadStudySetsInputBoundary load,
      LoginViewModel model, Executor worker, Executor ui) {
    this(save, user -> { throw new IllegalStateException("Save All is not configured."); },
        load, model, worker, ui);
  }
  public CloudController(SaveStudySetInputBoundary save, SaveAllStudySetsInputBoundary saveAll,
      LoadStudySetsInputBoundary load, LoginViewModel model, Executor worker, Executor ui) {
    this.save = save; this.saveAll = saveAll; this.load = load;
    this.model = model; this.worker = worker; this.ui = ui;
  }
  private String userId() { return model.getUser() == null ? null : model.getUser().getId(); }
  public void list() {
    String user = userId();
    submit("Loading study sets...", () -> load.list(user));
  }
  public void refresh(BooleanSupplier confirmDiscard) {
    String user = userId();
    submit("Reloading study sets from your account...", confirmDiscard, () -> load.refresh(user));
  }
  public void open(String setId) {
    String user = userId();
    submit("Opening study set...", () -> load.open(user, setId));
  }
  public void save(String setId) {
    SaveStudySetInputData input = new SaveStudySetInputData(userId(), setId);
    submit("Saving... Keep this window open until the request finishes.", () -> save.execute(input));
  }
  public void saveAll() {
    String user = userId();
    submit("Saving all study changes... Keep this window open until the request finishes.",
        () -> saveAll.execute(user));
  }
  private void submit(String message, Runnable task) {
    submit(message, () -> true, task);
  }
  private void submit(String message, BooleanSupplier confirm, Runnable task) {
    if (model.isBusy()) { return; }
    // The modal confirmation can run a nested EDT loop. Defer PDF completions
    // and block other cloud actions before asking, including when the user cancels.
    model.setBusy(true);
    try {
      if (!confirm.getAsBoolean()) { model.setBusy(false); return; }
      model.message(message);
      worker.execute(() -> {
        try { task.run(); }
        catch (RuntimeException exception) {
          ui.execute(() -> model.message("The operation could not finish. Local changes were kept; retry when ready."));
        } finally { ui.execute(() -> model.setBusy(false)); }
      });
    } catch (RuntimeException exception) {
      model.setBusy(false);
      model.message("The operation could not start. Local changes were kept; retry when ready.");
    }
  }
}
