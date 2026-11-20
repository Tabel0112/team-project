package interface_adapter.cloud;

import entity.User;
import interface_adapter.ViewManagerModel;
import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.study_set.StudySetViewModel;
import use_case.cloud.SetSnapshot;
import use_case.cloud.SetSummary;
import use_case.login.LoginOutputBoundary;
import use_case.login.LoginOutputData;
import use_case.load_study_sets.LoadStudySetsOutputBoundary;
import use_case.save_study_set.SaveStudySetOutputBoundary;
import use_case.save_study_set.SaveStudySetOutputData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/** Marshals boundary results onto the UI thread and formats their visible state. */
public final class CloudPresenter implements LoginOutputBoundary, LoadStudySetsOutputBoundary,
    SaveStudySetOutputBoundary {
  private final CloudViewModel cloud;
  private final CreateStudySetViewModel list;
  private final StudySetViewModel set;
  private final ViewManagerModel views;
  private final Executor ui;
  public CloudPresenter(CloudViewModel cloud, CreateStudySetViewModel list,
      StudySetViewModel set, ViewManagerModel views, Executor ui) {
    this.cloud = cloud; this.list = list; this.set = set; this.views = views; this.ui = ui;
  }
  @Override public void loggedIn(LoginOutputData user) {
    ui.execute(() -> { cloud.setUser(new User(user.userId, user.email)); cloud.message("Logged in. Loading your study sets..."); });
  }
  @Override public void signupSubmitted(String message) { ui.execute(() -> cloud.message(message)); }
  @Override public void failed(String message) { ui.execute(() -> cloud.message(message)); }
  @Override public void saved(SaveStudySetOutputData output) {
    ui.execute(() -> {
      String message = output.stillDirty ? "Previous save confirmed. Newer local changes remain; click Save again."
          : "Saved to your account.";
      cloud.message(message);
      if (output.setId.equals(set.getState().getStudySetId())) {
        set.getState().setMessage(message); set.firePropertyChanged();
      }
    });
  }
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
  @Override public void opened(SetSnapshot snapshot) {
    ui.execute(() -> {
      set.getState().open(snapshot.id, snapshot.name, snapshot.cards.size());
      set.firePropertyChanged(); views.setActiveView(StudySetViewModel.VIEW_NAME);
      cloud.message("Working locally. Click Save to commit changes to your account.");
    });
  }
}
