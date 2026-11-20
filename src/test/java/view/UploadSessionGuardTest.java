package view;

import entity.Flashcard;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.SwingUtilities;
import org.junit.Test;
import use_case.upload_notes.UploadNotesInteractor;
import static org.junit.Assert.*;

public class UploadSessionGuardTest {
  @Test public void clearingSessionRemovesPreviousAccountPreviewAndStatus() throws Exception {
    Path file = Files.createTempFile("account-a-notes", ".pdf");
    Files.writeString(file, "fixture");
    try {
      AtomicReference<String> scope = new AtomicReference<>("user-a:1");
      AtomicReference<UploadNotesPanel> panel = new AtomicReference<>();
      CountDownLatch done = new CountDownLatch(1);
      UploadNotesInteractor upload = new UploadNotesInteractor(path -> "fixture text");
      SwingUtilities.invokeAndWait(() -> {
        panel.set(new UploadNotesPanel(upload, (name, cards) -> { }, scope::get,
            busy -> { if (!busy) { done.countDown(); } },
            text -> List.of(new Flashcard("Private account A question", "Answer"))));
        panel.get().read(upload, file);
      });
      assertTrue(done.await(5, TimeUnit.SECONDS));
      SwingUtilities.invokeAndWait(() -> {
        javax.swing.JScrollPane scroll = (javax.swing.JScrollPane) panel.get().getComponent(1);
        javax.swing.JTextArea preview = (javax.swing.JTextArea) scroll.getViewport().getView();
        assertTrue(preview.getText().contains("Private account A question"));
        scope.set("user-b:2"); panel.get().clearSession();
        assertEquals("", preview.getText());
        assertEquals(" ", ((javax.swing.JLabel) panel.get().getComponent(2)).getText());
      });
    } finally { Files.deleteIfExists(file); }
  }
  @Test public void asynchronousUploadRejectsResultsFromEarlierSession() throws Exception {
    Path file = Files.createTempFile("upload-session", ".pdf");
    Files.writeString(file, "fixture");
    try {
      AtomicReference<String> scope = new AtomicReference<>("user-a:1");
      AtomicInteger delivered = new AtomicInteger();
      CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
      UploadNotesInteractor upload = new UploadNotesInteractor(path -> "fixture text");
      SwingUtilities.invokeAndWait(() -> {
        UploadNotesPanel panel = new UploadNotesPanel(upload, (name, cards) -> delivered.incrementAndGet(),
            scope::get, busy -> { if (!busy) { done.countDown(); } }, text -> {
              started.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS));
              return List.of(new Flashcard("Q", "A"));
            });
        panel.read(upload, file);
      });
      assertTrue(started.await(5, TimeUnit.SECONDS));
      scope.set("user-b:2"); release.countDown(); assertTrue(done.await(5, TimeUnit.SECONDS));
      assertEquals(0, delivered.get());
    } finally { Files.deleteIfExists(file); }
  }
}
