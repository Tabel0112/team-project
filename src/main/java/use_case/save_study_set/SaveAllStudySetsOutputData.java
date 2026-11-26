package use_case.save_study_set;

import java.util.List;

/** Batch outcome. Each successful set is committed independently of failed work. */
public final class SaveAllStudySetsOutputData {
  public final int savedCount;
  public final int deletedCount;
  public final List<String> failures;
  public final boolean stillDirty;

  public SaveAllStudySetsOutputData(int savedCount, int deletedCount, List<String> failures,
      boolean stillDirty) {
    this.savedCount = savedCount; this.deletedCount = deletedCount;
    this.failures = List.copyOf(failures); this.stillDirty = stillDirty;
  }
}
