package org.example.studyapp.view;

import org.example.studyapp.entity.StudySet;
import org.example.studyapp.entity.Card;
import org.example.studyapp.interface_adapter.storage.StorageController;
import org.example.studyapp.interface_adapter.generate.GenerateController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import org.example.studyapp.use_case.generate.NotesReader;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Desktop workspace: library, notes/editor, and focused practice sessions. */
public final class StudyWindow extends JFrame {
    private final NotesReader notesReader;
    private final StorageController local;
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
    private final JLabel status = new JLabel("Create or open a study set. Changes save automatically when you close the app.");
    private StudySet selected;
    private Card selectedCard;
    private boolean rendering;
    private boolean dirty;
    private boolean busy;

    public StudyWindow(StorageController local, GenerateController generator, NotesReader notesReader, List<StudySet> initial) {
        super("Study App — your study workspace");
        this.notesReader = notesReader;
        this.local = local; this.generator = generator;
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
        tabs.addTab("Create & edit cards", editorPanel());
        tabs.addTab("PDF & AI generation", notesPanel());
        tabs.addTab("Flashcards", practicePanel());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, workspace(tabs));
        split.setResizeWeight(0.23); split.setBorder(null);
        root.add(split, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(12, 12));
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
                if (dirty && !save()) return;
                dispose();
            }
        });
        for (StudySet set : initial) sets.addElement(set);
        if (!sets.isEmpty()) library.setSelectedIndex(0); else render();
        setLocationRelativeTo(null);
    }

    private JPanel workspace(JTabbedPane tabs) {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(labeled("Set title", title), BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel notesPanel() {
        JPanel panel = page();
        panel.add(new JLabel("Import a PDF or paste notes, then generate flashcards with AI."), BorderLayout.NORTH);
        notes.setToolTipText("Review or edit imported notes before sending them to Gemini.");
        panel.add(new JScrollPane(notes), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 8));
        bottom.add(new JLabel("Text PDFs up to 10 MB; .txt / .md up to 400 KB. AI creates a new set."));
        bottom.add(actions(button("Import PDF / notes", this::importNotes)));
        bottom.add(actions(new JLabel("Difficulty"), difficulty,
                new JLabel("Questions"), count, button("Generate with AI", this::generate)));
        bottom.add(new JLabel("Generation sends these notes to Google Gemini. Review generated answers for accuracy."));
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel editorPanel() {
        JPanel panel = page();
        panel.add(new JLabel("Add a card, then enter its question and answer. No AI key needed."), BorderLayout.NORTH);
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
        intro.setText("Review at your pace\n\nFlip through your flashcards to review each question and answer.\n\n"
                + "Create cards manually or generate them from your notes, then select Review flashcards.");
        panel.add(intro, BorderLayout.CENTER);
        panel.add(actions(button("Review flashcards", this::flashcards)), BorderLayout.SOUTH);
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
        if (JOptionPane.showConfirmDialog(this, "Delete this study set?", "Delete set",
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
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF and text notes (.pdf, .txt, .md)", "pdf", "txt", "md"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        if (!notes.getText().isBlank() && JOptionPane.showConfirmDialog(this, "Replace the notes in this set?",
                "Import notes", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        java.nio.file.Path path = chooser.getSelectedFile().toPath();
        work("Reading notes…", () -> notesReader.read(path), text -> {
            notes.setText(text);
            notes.setCaretPosition(0);
            status.setText("Imported " + path.getFileName() + ". Review the notes, then choose Generate with AI.");
        });
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
}
