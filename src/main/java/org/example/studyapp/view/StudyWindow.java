package org.example.studyapp.view;

import org.example.studyapp.entity.StudySet;
import org.example.studyapp.entity.Card;
import org.example.studyapp.entity.QuizSession;
import org.example.studyapp.interface_adapter.storage.StorageController;
import org.example.studyapp.interface_adapter.account.AccountController;
import org.example.studyapp.interface_adapter.generate.GenerateController;
import org.example.studyapp.interface_adapter.quiz.QuizController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Desktop workspace: library, notes/editor, and focused practice sessions. */
public final class StudyWindow extends JFrame {
    private final StorageController local;
    private final StorageController cloud;
    private final AccountController accounts;
    private final QuizController quizzes;
    private final GenerateController generator;
    private final DefaultListModel<StudySet> sets = new DefaultListModel<>();
    private final JList<StudySet> library = new JList<>(sets);
    private final DefaultListModel<Card> cards = new DefaultListModel<>();
    private final JList<Card> cardList = new JList<>(cards);
    private final JTextField title = new JTextField();
    private final JTextArea notes = area(15, 55);
    private final JTextArea question = area(4, 45);
    private final JTextArea answer = area(5, 45);
    private final JComboBox<String> difficulty = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(8, 1, 30, 1));
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(0, 0, 180, 1));
    private final JLabel status = new JLabel("Create a study set or open one from your library.");
    private StudySet selected;
    private Card selectedCard;
    private boolean rendering;
    private boolean dirty;
    private boolean busy;

    public StudyWindow(StorageController local, StorageController cloud, GenerateController generator, AccountController accounts, QuizController quizzes, List<StudySet> initial) {
        super("Study App — your study workspace");
        this.local = local; this.cloud = cloud; this.generator = generator; this.accounts = accounts; this.quizzes = quizzes;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(960, 700));
        setSize(1150, 800);
        JPanel root = new JPanel(new BorderLayout(20, 18));
        root.setBorder(new EmptyBorder(24, 24, 18, 24));
        JLabel heading = new JLabel("Your study workspace");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 27f));
        root.add(heading, BorderLayout.NORTH);

        JPanel sidebar = new JPanel(new BorderLayout(8, 12));
        sidebar.setPreferredSize(new Dimension(240, 500));
        sidebar.add(new JLabel("Study sets"), BorderLayout.NORTH);
        library.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sidebar.add(new JScrollPane(library), BorderLayout.CENTER);
        JPanel libraryActions = new JPanel(new GridLayout(0, 1, 0, 6));
        libraryActions.add(button("New set", this::newSet));
        libraryActions.add(button("Delete set", this::deleteSet));
        sidebar.add(libraryActions, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Notes & generation", notesPanel());
        tabs.addTab("Edit cards", editorPanel());
        tabs.addTab("Practice", practicePanel());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, tabs);
        split.setResizeWeight(0.23); split.setBorder(null);
        root.add(split, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 12));
        footer.add(actions(button("Save locally", this::save), button("Account", this::account),
                button("Upload to cloud", this::upload), button("Download from cloud", this::download)), BorderLayout.NORTH);
        status.setBorder(new EmptyBorder(4, 0, 0, 0));
        footer.add(status, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        library.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !rendering) {
                commit(); selected = library.getSelectedValue(); selectedCard = null; render();
            }
        });
        cardList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !rendering) {
                commitCard(); selectedCard = cardList.getSelectedValue(); renderCard();
            }
        });
        for (JTextArea field : new JTextArea[]{notes, question, answer}) watch(field);
        watch(title);
        difficulty.addActionListener(e -> { if (!rendering) dirty = true; });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (busy) { message("Wait for the current operation to finish before closing."); return; }
                if (dirty) {
                    int choice = JOptionPane.showConfirmDialog(StudyWindow.this,
                            "Save your changes before closing?", "Unsaved study sets", JOptionPane.YES_NO_CANCEL_OPTION);
                    if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;
                    if (choice == JOptionPane.YES_OPTION && !save()) return;
                }
                dispose();
            }
        });
        for (StudySet set : initial) sets.addElement(set);
        if (!sets.isEmpty()) library.setSelectedIndex(0); else render();
        setLocationRelativeTo(null);
    }

    private JPanel notesPanel() {
        JPanel panel = page();
        JPanel fields = new JPanel(new GridLayout(0, 1, 0, 6));
        fields.add(new JLabel("Set title")); fields.add(title);
        panel.add(fields, BorderLayout.NORTH);
        notes.setToolTipText("Paste course notes here, or import a UTF-8 text file.");
        panel.add(new JScrollPane(notes), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 8));
        bottom.add(new JLabel("Paste notes above, or import a .txt / .md file. AI creates a new set."));
        bottom.add(actions(button("Import notes", this::importNotes), new JLabel("Difficulty"), difficulty,
                new JLabel("Questions"), count, button("Generate with AI", this::generate)));
        bottom.add(new JLabel("Generation sends these notes to Google Gemini. Review generated answers for accuracy."));
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel editorPanel() {
        JPanel panel = page();
        cardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane list = new JScrollPane(cardList);
        list.setPreferredSize(new Dimension(200, 200));
        JPanel editor = new JPanel(new GridLayout(0, 1, 8, 8));
        editor.add(labeled("Question / flashcard front", new JScrollPane(question)));
        editor.add(labeled("Reference answer / flashcard back", new JScrollPane(answer)));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, list, editor);
        split.setResizeWeight(0.35); split.setBorder(null);
        panel.add(split, BorderLayout.CENTER);
        panel.add(actions(button("Add card", this::addCard), button("Delete card", this::deleteCard)), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel practicePanel() {
        JPanel panel = page();
        JTextArea intro = area(8, 45);
        intro.setEditable(false); intro.setOpaque(false);
        intro.setText("Practice at your pace\n\nFlip through flashcards, take a short-answer quiz, or retry missed questions.\n\n"
                + "Quizzes are self-assessed: write your answer, reveal the reference, then mark whether you got it right. "
                + "Missed and timed-out questions stay in Re-quiz until answered correctly.\n\n"
                + "Difficulty controls AI generation. A time limit of 0 means untimed.");
        panel.add(intro, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 12));
        bottom.add(actions(new JLabel("Time limit (minutes)"), minutes));
        bottom.add(actions(button("Review flashcards", this::flashcards), button("Start quiz", () -> quiz(false)),
                button("Re-quiz missed", () -> quiz(true))));
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(12, 14));
        panel.setBorder(new EmptyBorder(18, 18, 18, 18)); return panel;
    }
    private static JPanel labeled(String label, Component component) {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.add(new JLabel(label), BorderLayout.NORTH); panel.add(component); return panel;
    }
    private static JPanel actions(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        for (Component component : components) panel.add(component); return panel;
    }
    private static JButton button(String label, Runnable action) {
        JButton button = new JButton(label); button.addActionListener(e -> action.run()); return button;
    }
    private static JTextArea area(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setMargin(new Insets(10, 10, 10, 10)); return area;
    }
    private void watch(javax.swing.text.JTextComponent component) {
        component.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { if (!rendering) dirty = true; }
        });
    }
    private void message(String text) { JOptionPane.showMessageDialog(this, text); }
    private void error(Exception e) { message(e.getMessage() == null ? "Operation failed. Please try again." : e.getMessage()); }
    private void commitCard() {
        if (selectedCard != null) { selectedCard.question = question.getText().trim(); selectedCard.answer = answer.getText().trim(); }
    }
    private void commit() {
        commitCard();
        if (selected != null) {
            selected.title = title.getText().trim(); selected.notes = notes.getText();
            selected.difficulty = (String) difficulty.getSelectedItem();
        }
        library.repaint(); cardList.repaint();
    }
    private void renderCard() {
        rendering = true;
        question.setText(selectedCard == null ? "" : selectedCard.question);
        answer.setText(selectedCard == null ? "" : selectedCard.answer);
        question.setEnabled(selectedCard != null); answer.setEnabled(selectedCard != null);
        rendering = false;
    }
    private void render() {
        rendering = true;
        title.setText(selected == null ? "" : selected.title);
        notes.setText(selected == null ? "" : selected.notes);
        difficulty.setSelectedItem(selected == null ? "Medium" : selected.difficulty);
        title.setEnabled(selected != null); notes.setEnabled(selected != null);
        cards.clear();
        if (selected != null) for (Card card : selected.cards) cards.addElement(card);
        selectedCard = null;
        rendering = false;
        renderCard();
        if (!cards.isEmpty()) cardList.setSelectedIndex(0);
    }
    private boolean requireSet() {
        if (selected != null) return true;
        message("Create or select a study set first."); return false;
    }
    private void newSet() {
        commit(); StudySet set = new StudySet(); sets.addElement(set); library.setSelectedValue(set, true); dirty = true;
    }
    private void deleteSet() {
        if (!requireSet()) return;
        if (JOptionPane.showConfirmDialog(this, "Delete this local set? Cloud copies are kept.", "Delete set",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        int index = library.getSelectedIndex(); sets.remove(index); dirty = true;
        if (!sets.isEmpty()) library.setSelectedIndex(Math.min(index, sets.size() - 1));
    }
    private void addCard() {
        if (!requireSet()) return;
        commit(); Card card = new Card("New question", "Reference answer");
        selected.cards.add(card); cards.addElement(card); cardList.setSelectedValue(card, true); dirty = true;
    }
    private void deleteCard() {
        if (selectedCard == null) return;
        commit(); Card removed = selectedCard; selectedCard = null;
        selected.cards.remove(removed); cards.removeElement(removed); dirty = true; library.repaint(); renderCard();
    }
    private List<StudySet> snapshot() {
        commit(); List<StudySet> result = new ArrayList<>();
        for (int i = 0; i < sets.size(); i++) result.add(sets.get(i));
        for (StudySet set : result) set.validate();
        return result;
    }
    private boolean save() {
        try { local.save(snapshot()); dirty = false; status.setText("Saved on this device."); return true; }
        catch (Exception e) { error(e); return false; }
    }
    private <T> void work(String text, Callable<T> task, Consumer<T> done) {
        if (busy) return;
        busy = true; status.setText(text);
        // Disable the workspace during operations so returned data cannot race local edits.
        setEnabled(false);
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception { return task.call(); }
            protected void done() {
                busy = false; setEnabled(true); toFront();
                try { done.accept(get()); }
                catch (Exception e) { status.setText("Operation failed; your local changes are kept.");
                    Throwable cause = e.getCause(); error(cause instanceof Exception ? (Exception) cause : e); }
            }
        }.execute();
    }
    private void importNotes() {
        if (!requireSet()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text notes (.txt, .md)", "txt", "md"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        if (!notes.getText().isBlank() && JOptionPane.showConfirmDialog(this, "Replace the notes in this set?",
                "Import notes", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            if (Files.size(chooser.getSelectedFile().toPath()) > 400000) throw new IllegalArgumentException("Choose a text file smaller than 400 KB.");
            notes.setText(Files.readString(chooser.getSelectedFile().toPath()));
        } catch (Exception e) { error(e); }
    }
    private void generate() {
        if (!requireSet()) return;
        commit(); String name = title.getText().trim(); String source = notes.getText();
        String level = (String) difficulty.getSelectedItem(); int size = (Integer) count.getValue();
        work("Generating study materials…", () -> generator.generate(name, source, level, size), output -> {
            StudySet set = output.studySet;
            sets.addElement(set); library.setSelectedValue(set, true); dirty = true;
            status.setText(output.message);
        });
    }
    private void account() {
        JTextField email = new JTextField(25); JPasswordField password = new JPasswordField(25);
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("Email")); form.add(email); form.add(new JLabel("Password")); form.add(password);
        int choice = JOptionPane.showOptionDialog(this, form, "Cloud account", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"Sign in", "Create account", "Sign out", "Cancel"}, "Sign in");
        if (choice == 2) { accounts.signOut(); status.setText("Signed out. Local sets remain on this device."); return; }
        if (choice != 0 && choice != 1) return;
        char[] characters = password.getPassword(); String secret = new String(characters);
        java.util.Arrays.fill(characters, '\0'); password.setText("");
        String address = email.getText().trim();
        work("Connecting to your account…", () -> { accounts.authenticate(address, secret, choice == 1); return address; },
                user -> status.setText("Signed in as " + user + ". Cloud upload/download is manual."));
    }
    private void upload() {
        try {
            if (!save()) return;
            List<StudySet> snapshot = snapshot();
            work("Uploading study sets…", () -> { cloud.save(snapshot); return snapshot.size(); },
                    size -> status.setText("Uploaded " + size + " sets to your account."));
        } catch (Exception e) { error(e); }
    }
    private void download() {
        if (!save()) return;
        if (JOptionPane.showConfirmDialog(this, "Download cloud sets? Cloud versions replace local sets with the same ID. "
                + "Other local sets are kept.", "Download study sets", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        work("Downloading study sets…", cloud::load, downloaded -> {
            commit(); rendering = true; selected = null; selectedCard = null;
            for (StudySet set : downloaded) {
                for (int i = sets.size() - 1; i >= 0; i--) if (sets.get(i).id.equals(set.id)) sets.remove(i);
                sets.addElement(set);
            }
            library.clearSelection(); rendering = false; render(); dirty = true;
            if (!sets.isEmpty()) library.setSelectedIndex(0);
            if (save()) status.setText("Downloaded " + downloaded.size() + " sets and saved locally.");
        });
    }
    private boolean canPractice() {
        if (!requireSet()) return false;
        try { commit(); selected.validate();
            if (selected.cards.isEmpty()) throw new IllegalArgumentException("Add a card or generate materials first.");
            return true;
        } catch (Exception e) { error(e); return false; }
    }
    private void flashcards() {
        if (!canPractice()) return;
        JDialog dialog = new JDialog(this, "Flashcards — " + selected.title, true);
        JPanel panel = page(); JLabel progress = new JLabel(); JTextArea face = area(12, 45); face.setEditable(false);
        int[] index = {0}; boolean[] back = {false};
        Runnable show = () -> {
            Card card = selected.cards.get(index[0]);
            progress.setText("Card " + (index[0] + 1) + " of " + selected.cards.size() + (back[0] ? " · Answer" : " · Question"));
            face.setText(back[0] ? card.answer : card.question); face.setCaretPosition(0);
        };
        panel.add(progress, BorderLayout.NORTH); panel.add(new JScrollPane(face), BorderLayout.CENTER);
        panel.add(actions(button("Previous", () -> { index[0] = Math.floorMod(index[0] - 1, selected.cards.size()); back[0] = false; show.run(); }),
                button("Flip card", () -> { back[0] = !back[0]; show.run(); }),
                button("Next", () -> { index[0] = (index[0] + 1) % selected.cards.size(); back[0] = false; show.run(); })), BorderLayout.SOUTH);
        show.run(); dialog.setContentPane(panel); dialog.pack(); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }
    private void quiz(boolean missedOnly) {
        if (!canPractice()) return;
        QuizSession session;
        try { session = quizzes.start(selected, missedOnly); }
        catch (IllegalArgumentException e) { error(e); return; }
        JDialog dialog = new JDialog(this, missedOnly ? "Re-quiz" : "Practice quiz", true);
        JPanel panel = page(); JLabel progress = new JLabel(); JTextArea prompt = area(5, 48);
        prompt.setEditable(false); JTextArea response = area(5, 48); JTextArea reference = area(5, 48); reference.setEditable(false);
        JPanel body = new JPanel(new GridLayout(0, 1, 8, 10));
        body.add(labeled("Question", new JScrollPane(prompt))); body.add(labeled("Your answer", new JScrollPane(response)));
        body.add(labeled("Reference answer", new JScrollPane(reference)));
        JButton reveal = new JButton("Reveal answer"); JButton right = new JButton("I got it right"); JButton wrong = new JButton("Needs practice");
        int limit = (Integer) minutes.getValue(); long deadline = System.nanoTime() + limit * 60L * 1000000000L;
        javax.swing.Timer timer = new javax.swing.Timer(250, null);
        Runnable finish = () -> {
            timer.stop(); dirty = true; dialog.dispose();
            status.setText(session.correct() + " / " + session.total() + " correct. Missed questions are ready in Re-quiz.");
            save();
            message("Quiz complete: " + session.correct() + " / " + session.total()
                    + " correct.\nMissed questions are available in Re-quiz.");
        };
        Runnable show = () -> {
            prompt.setText(session.current().question); prompt.setCaretPosition(0);
            response.setText(""); reference.setText("Write your answer, then reveal the reference.");
            right.setEnabled(false); wrong.setEnabled(false); reveal.setEnabled(true);
            progress.setText("Question " + (session.position() + 1) + " of " + session.total());
            response.requestFocusInWindow();
        };
        Consumer<Boolean> assess = correct -> {
            session.answer(correct); dirty = true;
            if (session.finished()) finish.run(); else show.run();
        };
        reveal.addActionListener(e -> {
            reference.setText(session.current().answer); reference.setCaretPosition(0);
            reveal.setEnabled(false); right.setEnabled(true); wrong.setEnabled(true);
        });
        right.addActionListener(e -> assess.accept(true)); wrong.addActionListener(e -> assess.accept(false));
        timer.addActionListener(e -> {
            long remaining = Math.max(0, (deadline - System.nanoTime() + 999999999L) / 1000000000L);
            if (remaining == 0) { session.expire(); finish.run(); }
            else progress.setText("Question " + (session.position() + 1) + " of " + session.total()
                    + " · " + remaining / 60 + ":" + String.format("%02d", remaining % 60) + " remaining");
        });
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (JOptionPane.showConfirmDialog(dialog, "End this quiz? Unanswered questions will be marked for Re-quiz.",
                        "End quiz", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) { session.expire(); finish.run(); }
            }
        });
        panel.add(progress, BorderLayout.NORTH); panel.add(body, BorderLayout.CENTER);
        panel.add(actions(reveal, right, wrong), BorderLayout.SOUTH);
        dialog.setContentPane(panel); dialog.pack(); dialog.setLocationRelativeTo(this);
        show.run(); if (limit > 0) timer.start(); dialog.setVisible(true);
    }
}
