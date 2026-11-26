package interface_adapter.cloud;

import interface_adapter.ViewManagerModel;
import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.login.LoginViewModel;
import interface_adapter.study_set.StudySetViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import use_case.cloud.SetSnapshot;
import use_case.cloud.SetSummary;
import use_case.load_study_sets.LoadStudySetsOutputBoundary;
import use_case.save_study_set.SaveAllStudySetsOutputBoundary;
import use_case.save_study_set.SaveAllStudySetsOutputData;
import use_case.save_study_set.SaveStudySetOutputBoundary;
import use_case.save_study_set.SaveStudySetOutputData;

/** Marshals boundary results onto the UI thread and formats their visible state. */
public final class CloudPresenter implements LoadStudySetsOutputBoundary,
    SaveStudySetOutputBoundary, SaveAllStudySetsOutputBoundary {
  private final LoginViewModel cloud;
  private final CreateStudySetViewModel list;
  private final StudySetViewModel set;
  private final ViewManagerModel views;
  private final Executor ui;
  private final Runnable resetViews;
  public CloudPresenter(LoginViewModel cloud, CreateStudySetViewModel list,
      StudySetViewModel set, ViewManagerModel views, Executor ui, Runnable resetViews) {
    this.cloud = cloud; this.list = list; this.set = set; this.views = views; this.ui = ui;
    this.resetViews = resetViews;
  }
  @Override public void failed(String message) { ui.execute(() -> cloud.message(message)); }
  @Override public void saved(SaveStudySetOutputData output) {
    ui.execute(() -> {
      String message = output.stillDirty ? "Previous save confirmed. Newer local changes remain; click Save All again."
          : "Saved to your account.";
      cloud.message(message);
      if (output.setId.equals(set.getState().getStudySetId())) {
        set.getState().setMessage(message); set.firePropertyChanged();
      }
    });
  }
  @Override public void savedAll(SaveAllStudySetsOutputData output) {
    ui.execute(() -> {
      String message = "Saved " + studySets(output.savedCount) + ". Deleted " + studySets(output.deletedCount) + ".";
      if (!output.failures.isEmpty()) {
        message += " " + output.failures.size()
            + (output.failures.size() == 1 ? " operation failed. " : " operations failed. ") + output.failures.get(0)
            + " Pending work was kept. Click Save All to retry.";
      } else if (output.stillDirty) {
        message += " Newer local changes remain. Click Save All again.";
      } else if (output.savedCount == 0 && output.deletedCount == 0) {
        message = "No unsaved study changes.";
      }
      cloud.message(message);
      set.getState().setMessage(message); set.firePropertyChanged();
    });
  }
  private static String studySets(int count) { return count + (count == 1 ? " study set" : " study sets"); }
  @Override public void listed(List<SetSummary> summaries) {
    ui.execute(() -> {
      List<CreateStudySetState.Item> items = new ArrayList<>();
      for (SetSummary summary : summaries) {
        int count = -1; // Cards are deliberately not loaded by the cloud list request.
        for (CreateStudySetState.Item prior : list.getState().getStudySets()) {
          if (prior.getId().equals(summary.id)) { count = prior.getCardCount(); break; }
        }
        items.add(new CreateStudySetState.Item(summary.id, summary.name, count));
      }
      list.getState().setStudySets(items); list.firePropertyChanged();
      cloud.message("Study sets loaded. Unsaved local drafts were kept. Open a set to load its cards.");
    });
  }
  @Override public void refreshed(List<SetSummary> summaries) {
    ui.execute(() -> {
      resetViews.run();
      List<CreateStudySetState.Item> items = new ArrayList<>();
      for (SetSummary summary : summaries) {
        items.add(new CreateStudySetState.Item(summary.id, summary.name, -1));
      }
      list.getState().setStudySets(items); list.firePropertyChanged();
      cloud.message("Study sets reloaded from your account. Open a set to load its saved cards.");
    });
  }
  @Override public void opened(SetSnapshot snapshot) {
    ui.execute(() -> {
      set.getState().open(snapshot.id, snapshot.name, snapshot.cards.size());
      set.firePropertyChanged(); views.setActiveView(StudySetViewModel.VIEW_NAME);
      cloud.message("Working locally. Click Save All to commit changes to your account.");
    });
  }
}
