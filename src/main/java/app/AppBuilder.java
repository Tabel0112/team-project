package app;

import data_access.InMemoryStudySetDataAccessObject;
import data_access.SupabaseConfig;
import data_access.SupabaseGateway;
import interface_adapter.cloud.CloudController;
import interface_adapter.cloud.CloudPresenter;
import interface_adapter.cloud.CloudViewModel;
import use_case.login.LoginInteractor;
import use_case.save_study_set.SaveStudySetInteractor;
import use_case.load_study_sets.LoadStudySetsInteractor;
import view.CloudPanel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import data_access.PdfBoxTextReader;
import entity.StudySetFactory;
import interface_adapter.ViewManagerModel;
import interface_adapter.add_flashcard.AddFlashcardController;
import interface_adapter.add_flashcard.AddFlashcardPresenter;
import interface_adapter.add_flashcard.AddFlashcardViewModel;
import interface_adapter.create_study_set.CreateStudySetController;
import interface_adapter.create_study_set.CreateStudySetPresenter;
import interface_adapter.create_study_set.CreateStudySetState;
import interface_adapter.create_study_set.CreateStudySetViewModel;
import interface_adapter.delete_flashcard.DeleteFlashcardController;
import interface_adapter.delete_flashcard.DeleteFlashcardPresenter;
import interface_adapter.edit_flashcard.EditFlashcardController;
import interface_adapter.edit_flashcard.EditFlashcardPresenter;
import interface_adapter.edit_flashcard.EditFlashcardViewModel;
import interface_adapter.study_set.StudySetViewModel;
import interface_adapter.view_flashcards.ViewFlashcardsController;
import interface_adapter.view_flashcards.ViewFlashcardsPresenter;
import interface_adapter.view_flashcards.ViewFlashcardsState;
import interface_adapter.view_flashcards.ViewFlashcardsViewModel;
import use_case.add_flashcard.AddFlashcardInteractor;
import use_case.create_study_set.CreateStudySetInteractor;
import use_case.delete_flashcard.DeleteFlashcardInteractor;
import use_case.edit_flashcard.EditFlashcardInteractor;
import use_case.upload_notes.UploadNotesInteractor;
import view.AddFlashcardView;
import view.CreateStudySetView;
import view.EditFlashcardView;
import view.StudySetView;
import view.UploadNotesPanel;
import use_case.view_flashcards.ViewFlashcardsInteractor;
import view.ViewFlashcardsView;
import view.ViewManager;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.CardLayout;

import javax.swing.JTabbedPane;
import java.awt.Dimension;

/**
 * Builds the application by creating each layer's objects and connecting them.
 */
public class AppBuilder {
  private final CardLayout cardLayout = new CardLayout();
  private final JPanel cardPanel = new JPanel(cardLayout);
  private final ViewManagerModel viewManagerModel = new ViewManagerModel();
  private final ViewManager viewManager = new ViewManager(cardPanel, cardLayout, viewManagerModel);
  private final InMemoryStudySetDataAccessObject studySetDao =
          new InMemoryStudySetDataAccessObject();

  private final CloudViewModel cloudModel = new CloudViewModel();
  private CloudController cloudController;
  private ExecutorService cloudWorker;
  private CreateStudySetInteractor createStudySet;
  private CreateStudySetViewModel createStudySetViewModel;
  private StudySetViewModel studySetViewModel;
  private CreateStudySetView createStudySetView;
  private StudySetView studySetView;
  private AddFlashcardViewModel addFlashcardViewModel;
  private AddFlashcardView addFlashcardView;
  private ViewFlashcardsViewModel viewFlashcardsViewModel;
  private ViewFlashcardsView viewFlashcardsView;
  private UploadNotesPanel uploadNotesPanel;
  private EditFlashcardViewModel editFlashcardViewModel;
  private EditFlashcardView editFlashcardView;

  /**
   * Creates an empty builder.
   */
  public AppBuilder() { }

  /**
   * Returns the id of the logged-in user.
   *
   * @return the current user's id
   */
  private String currentUserId() {
    return cloudModel.getUser() == null ? null : cloudModel.getUser().getId();
  }

  /**
   * Opens a study set that the user clicked in the panel: marks it as the open
   * study set and switches to the study set view.
   *
   * @param item the study set that was clicked
   */
  private void openStudySet(CreateStudySetState.Item item) {
    if (cloudController != null) { cloudController.open(item.getId()); }
  }

  /**
   * Creates the create study set view and its view model. Clicking a study set
   * in its panel opens it.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addCreateStudySetView() {
    createStudySetViewModel = new CreateStudySetViewModel();
    createStudySetView = new CreateStudySetView(
            createStudySetViewModel,
            this::currentUserId,
            this::openStudySet);
    cardPanel.add(createStudySetView, CreateStudySetView.VIEW_NAME);
    return this;
  }

  /**
   * Creates the study set view and its view model. Its Add Flashcard button opens
   * the add flashcard view, and its Back button returns to the create study set view.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addStudySetView() {
    studySetViewModel = new StudySetViewModel();
    studySetView = new StudySetView(
            studySetViewModel,
            this::openAddFlashcardForm,
            () -> viewManagerModel.setActiveView(CreateStudySetView.VIEW_NAME));
    cardPanel.add(studySetView, StudySetView.VIEW_NAME);
    return this;
  }

  /**
   * Clears the study set view's confirmation message and shows the add flashcard form.
   */
  private void openAddFlashcardForm() {
    studySetViewModel.getState().setMessage("");
    studySetViewModel.firePropertyChanged();
    viewManagerModel.setActiveView(AddFlashcardView.VIEW_NAME);
  }

  /**
   * Creates the add flashcard view and its view model. It adds flashcards to the
   * study set that is open in the study set view, and its Cancel button returns there.
   * Requires {@link #addStudySetView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addAddFlashcardView() {
    addFlashcardViewModel = new AddFlashcardViewModel();
    addFlashcardView = new AddFlashcardView(
            addFlashcardViewModel,
            () -> studySetViewModel.getState().getStudySetId(),
            () -> viewManagerModel.setActiveView(StudySetView.VIEW_NAME));
    cardPanel.add(addFlashcardView, AddFlashcardView.VIEW_NAME);
    return this;
  }

  /**
   * Creates the Add Flashcard interactor, presenter and controller, and gives the
   * controller to the view. Requires {@link #addCreateStudySetView()},
   * {@link #addStudySetView()} and {@link #addAddFlashcardView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addAddFlashcardUseCase() {
    final AddFlashcardPresenter presenter = new AddFlashcardPresenter(
            addFlashcardViewModel, studySetViewModel, createStudySetViewModel, viewManagerModel);
    final AddFlashcardInteractor interactor = new AddFlashcardInteractor(studySetDao, presenter);
    final AddFlashcardController controller = new AddFlashcardController(interactor);
    addFlashcardView.setController(controller);
    return this;
  }

  /**
   * Creates the view flashcards pop-up and its view model. The pop-up is shown
   * over the study set view, and its Edit button opens the edit flashcard form.
   * Requires {@link #addStudySetView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addViewFlashcardsView() {
    viewFlashcardsViewModel = new ViewFlashcardsViewModel();
    viewFlashcardsView = new ViewFlashcardsView(viewFlashcardsViewModel, studySetView, this::openEditFlashcardForm);
    return this;
  }

  /**
   * Clears the study set view's confirmation message and shows the edit flashcard
   * form, filled with the chosen flashcard's current question and answer.
   *
   * @param card      the flashcard the user chose to edit
   * @param cardIndex the flashcard's position in the open study set
   */
  private void openEditFlashcardForm(ViewFlashcardsState.Item card, int cardIndex) {
    studySetViewModel.getState().setMessage("");
    studySetViewModel.firePropertyChanged();
    editFlashcardViewModel.getState().open(cardIndex, card.getQuestion(), card.getAnswer());
    editFlashcardViewModel.firePropertyChanged();
    viewManagerModel.setActiveView(EditFlashcardView.VIEW_NAME);
  }

  /**
   * Creates the edit flashcard view and its view model. It edits flashcards in the
   * study set that is open in the study set view, and its Cancel button returns there.
   * Requires {@link #addStudySetView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addEditFlashcardView() {
    editFlashcardViewModel = new EditFlashcardViewModel();
    editFlashcardView = new EditFlashcardView(
            editFlashcardViewModel,
            () -> studySetViewModel.getState().getStudySetId(),
            () -> viewManagerModel.setActiveView(StudySetView.VIEW_NAME));
    cardPanel.add(editFlashcardView, EditFlashcardView.VIEW_NAME);
    return this;
  }

  /**
   * Creates the Edit Flashcard interactor, presenter and controller, and gives the
   * controller to the view. Requires {@link #addStudySetView()} and
   * {@link #addEditFlashcardView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addEditFlashcardUseCase() {
    final EditFlashcardPresenter presenter = new EditFlashcardPresenter(
            editFlashcardViewModel, studySetViewModel, viewManagerModel);
    final EditFlashcardInteractor interactor = new EditFlashcardInteractor(studySetDao, presenter);
    final EditFlashcardController controller = new EditFlashcardController(interactor);
    editFlashcardView.setController(controller);
    return this;
  }

  /**
   * Creates the Delete Flashcard interactor, presenter and controller, and gives the
   * controller to the edit flashcard view, where its Delete button lives. Requires
   * {@link #addCreateStudySetView()}, {@link #addStudySetView()} and
   * {@link #addEditFlashcardView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addDeleteFlashcardUseCase() {
    final DeleteFlashcardPresenter presenter = new DeleteFlashcardPresenter(
            editFlashcardViewModel, studySetViewModel, createStudySetViewModel, viewManagerModel);
    final DeleteFlashcardInteractor interactor =
            new DeleteFlashcardInteractor(studySetDao, presenter);
    final DeleteFlashcardController controller = new DeleteFlashcardController(interactor);
    editFlashcardView.setDeleteController(controller);
    return this;
  }

  /**
   * Creates the View Flashcards interactor, presenter and controller, and gives the
   * controller to the study set view. Requires {@link #addStudySetView()} and
   * {@link #addViewFlashcardsView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addViewFlashcardsUseCase() {
    final ViewFlashcardsPresenter presenter = new ViewFlashcardsPresenter(viewFlashcardsViewModel);
    final ViewFlashcardsInteractor interactor = new ViewFlashcardsInteractor(studySetDao, presenter);
    final ViewFlashcardsController controller = new ViewFlashcardsController(interactor);
    studySetView.setViewFlashcardsController(controller);
    return this;
  }

  /**
   * Creates the Create Study Set interactor, presenter and controller, and gives the
   * controller to the view. Requires {@link #addCreateStudySetView()} and
   * {@link #addStudySetView()} to have been called.
   *
   * @return this builder, for chaining
   */
  public AppBuilder addCreateStudySetUseCase() {
    final CreateStudySetPresenter presenter = new CreateStudySetPresenter(
            createStudySetViewModel, studySetViewModel, viewManagerModel);
    createStudySet =
            new CreateStudySetInteractor(studySetDao, presenter, new StudySetFactory());
    final CreateStudySetController controller = new CreateStudySetController(createStudySet);
    createStudySetView.setController(controller);
    return this;
  }

  /**
   * Builds the application window, starting on the create study set view.
   *
   * @return the window, not yet visible
   */
  public JFrame build() {
    final JFrame frame = new JFrame("Study App");
    final SupabaseGateway gateway = new SupabaseGateway(SupabaseConfig.load());
    final CloudPresenter presenter = new CloudPresenter(cloudModel, createStudySetViewModel,
        studySetViewModel, viewManagerModel, SwingUtilities::invokeLater);
    cloudWorker = Executors.newSingleThreadExecutor(task -> {
      Thread thread = new Thread(task, "supabase-worker"); thread.setDaemon(true); return thread;
    });
    cloudController = new CloudController(new LoginInteractor(gateway, presenter),
        new SaveStudySetInteractor(studySetDao, gateway, presenter),
        new LoadStudySetsInteractor(studySetDao, gateway, presenter), gateway, studySetDao,
        cloudModel, cloudWorker, SwingUtilities::invokeLater);
    JTabbedPane pages = buildPages(new UploadNotesInteractor(new PdfBoxTextReader()));
    JPanel root = new JPanel(new BorderLayout());
    root.add(new CloudPanel(cloudModel, cloudController,
        () -> studySetViewModel.getState().getStudySetId(), () -> logout(frame)), BorderLayout.NORTH);
    root.add(pages, BorderLayout.CENTER);
    cloudModel.addPropertyChangeListener(event ->
        setTreeEnabled(pages, cloudModel.getUser() != null && !cloudModel.isBusy()));
    setTreeEnabled(pages, false);
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent event) {
        if (cloudModel.isBusy()) {
          cloudModel.message("Please wait for the current request before closing. Local changes are still here.");
        } else if (confirmDiscard(frame, "close the application")) {
          cloudController.logout(); clearSessionViews(); cloudWorker.shutdown(); frame.dispose();
        }
      }
    });
    frame.add(root);
    frame.setMinimumSize(new Dimension(1100, 700));
    frame.setSize(1250, 760);
    return frame;
  }

  private boolean confirmDiscard(Component parent, String action) {
    if (!cloudController.hasUnsaved() && !createStudySetView.hasUnsubmittedInput()
        && !addFlashcardView.hasUnsubmittedInput() && !editFlashcardView.hasUnsubmittedInput()) { return true; }
    return JOptionPane.showConfirmDialog(parent,
        "There are unsaved local changes or unfinished form entries. They will be lost if you "
        + action + ". Choose Cancel to return and Save or retry first.",
        "Unsaved changes", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
  }

  private void logout(Component parent) {
    if (!cloudModel.isBusy() && confirmDiscard(parent, "log out")) {
      cloudController.logout();
      clearSessionViews();
    }
  }

  /** Clears both visible fields and cached account data after a confirmed logout/close. */
  private void clearSessionViews() {
    uploadNotesPanel.clearSession();
    createStudySetViewModel.getState().setStudySets(java.util.List.of());
    createStudySetViewModel.getState().setCreatedStudySetId(null);
    createStudySetViewModel.getState().setName("");
    createStudySetViewModel.getState().setErrorMessage(null);
    createStudySetView.clearInput(); createStudySetViewModel.firePropertyChanged();
    addFlashcardViewModel.getState().clear(); addFlashcardViewModel.firePropertyChanged();
    editFlashcardViewModel.getState().clear(); editFlashcardViewModel.firePropertyChanged();
    viewFlashcardsView.clearSession();
    studySetViewModel.getState().open("", "", 0); studySetViewModel.firePropertyChanged();
    viewManagerModel.setActiveView(CreateStudySetView.VIEW_NAME);
  }

  private static void setTreeEnabled(Component component, boolean enabled) {
    component.setEnabled(enabled);
    if (component instanceof Container) {
      for (Component child : ((Container) component).getComponents()) { setTreeEnabled(child, enabled); }
    }
  }

  public JTabbedPane buildPages(UploadNotesInteractor uploadNotes) {
    final JTabbedPane pages = new JTabbedPane();
    pages.addTab("Study sets", cardPanel);
    uploadNotesPanel = new UploadNotesPanel(uploadNotes, (title, cards) -> {
      createStudySet.createFromNotes(currentUserId(), title, cards);
      viewManagerModel.setActiveView(CreateStudySetView.VIEW_NAME);
      pages.setSelectedComponent(cardPanel);
    }, cloudModel::scope, cloudModel::setBusy);
    pages.addTab("Upload PDF", uploadNotesPanel);
    pages.addTab("Review", new JPanel());       // placeholder for a future use case

    viewManagerModel.setActiveView(CreateStudySetView.VIEW_NAME);
    return pages;
  }
}
