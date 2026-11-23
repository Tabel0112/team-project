package use_case.save_study_set;

/** Confirmation for the exact acknowledged snapshot, including any newer local changes. */
public final class SaveStudySetOutputData {
  public final String setId;
  public final boolean stillDirty;
  public SaveStudySetOutputData(String setId, boolean stillDirty) {
    this.setId = setId; this.stillDirty = stillDirty;
  }
}
