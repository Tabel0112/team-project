# Flashcards

## Team Information

### Team Members
- Wes
- Ryan
- Elia
- Olivia
- Abel
  
---
## Project Description

### Domain

The Flashcards application turns course materials into practice quizzes and flashcards to help students study. Students may upload notes and chooses what to practice, and the application creates flashcards based off the notes. The user can move and copy existing flashcards into another, new or already existing group of flashcards. Users' tests can be saved on an account which saves the previously created study materials to a cloud Users can also create multiple sets of study materials on each account.

---
## User Stories

### User Story 1: As a student, I want to be able to create flashcards, so that I can review course material.

### User Story 2: As a student, I want to upload lecture notes and have flashcards made for me, to make studying more efficient.

### User Story 3: As a student, I want to be able to edit my existing practice flashcards, so that I can keep them up to date and accurate.

### User Story 4: As a student, I want to be able to create multiple sets of study materials, so that I can study different subjects at the same time.

### User Story 5: As a student, I want to be able to save my study sets, so that I can go back to them whenever I want.

---
## Use Cases

### Use Case 1: Create a Set of Flashcards

**Name:** Create a Set of Flashcards

**Primary actor:** Student (User)

**Preconditions:**
- The user has opened the application.

**Trigger:** The user selects "New Set".

**Main success scenario:**
1. The user selects "New Set".
2. The system prompts the user for a name for the set.
3. The user enters a name and confirms.
4. The system creates an empty study set and opens it, showing an empty cards list.
5. The user selects "Add Flashcard", enters a question and an answer, and confirms.
6. The system validates that neither field is empty and adds the flashcard to the set's cards list.
7. The user repeats steps 5 and 6 until all the flashcards they want are added.
8. The user finishes, and the system displays the completed set with all of its flashcards.

**Extensions:**
- 3a. The set name is empty: the system shows an error message and asks for a name (return to step 3).
- 3b. The user cancels: no set is created.
- 6a. The question or answer field is empty: the system shows an error message and asks the user to fill in the missing field (return to step 5).

**Success outcome:** A new study set exists containing the flashcards the user created, ready for review.

---

### Use Case 2: Generate Flashcards from Lecture Notes

**Name:** Generate Flashcards from Lecture Notes

**Primary actor:** Student (User)

**Preconditions:**
- The user has a text-based copy of their lecture notes to upload.
- The AI API (OpenAI/Gemini) is reachable.

**Trigger:** The user selects "Upload Notes" and chooses a file.

**Main success scenario:**
1. The user selects "Upload Notes" and chooses a file containing lecture notes.
2. The system reads the text from the file.
3. The system sends the text to the AI API and requests question and answer pairs.
4. The AI API returns the generated question and answer pairs.
5. The system creates a flashcard for each pair and displays them to the user for review.
6. The user accepts the generated flashcards into a study set.

**Extensions:**
- 2a. The file is empty, unreadable, or in an unsupported format: the system shows an error message and asks the user to upload a different file.
- 4a. The AI API fails or times out: the system shows an error message and lets the user retry or create flashcards manually.
- 5a. The generated flashcards are inaccurate: the user can edit or delete them before accepting (see Use Case 3).

**Success outcome:** Flashcards based on the uploaded notes are created and added to the user's chosen study set.

---

### Use Case 3: Edit Flashcard

**Name:** Edit Flashcard

**Primary actor:** Student (User)

**Preconditions:**
- The user has a study set containing at least one flashcard.

**Trigger:** The user selects the "Edit" option on an existing flashcard.

**Main success scenario:**
1. The user opens a study set and selects "Edit" on a flashcard.
2. The system displays the flashcard's current question and answer in editable fields.
3. The user changes the question, the answer, or both.
4. The user confirms the changes.
5. The system validates that neither field is empty.
6. The system updates the flashcard and displays the updated version.

**Extensions:**
- 5a. A field is empty: the system shows an error message and asks the user to fix it (return to step 3).
- 4a. The user cancels: the system discards the changes and the flashcard stays as it was.
- 3a. The user chooses to delete the flashcard instead: the system asks for confirmation, then removes it from the study set.

**Success outcome:** The flashcard in the study set reflects the user's edits.

---

### Use Case 4: Create Multiple Sets of Flashcards

**Name:** Create Multiple Sets of Flashcards

**Primary actor:** Student (User)

**Preconditions:**
- The user is logged in to their account.
- The user already has at least one study set.

**Trigger:** The user selects "New Set" from their list of study sets.

**Main success scenario:**
1. The user views their list of existing study sets.
2. The user selects "New Set" to start another one for a different subject or course.
3. The system prompts the user for a name for the new set.
4. The user enters a name and confirms.
5. The system validates that the name is not empty and not already used by another of the user's sets.
6. The system creates the new empty set and adds it to the user's list alongside the existing sets.
7. The user returns to the list and opens any set, and the system displays that set's flashcards without affecting the others.

**Extensions:**
- 5a. The name is empty: the system shows an error message and asks for a name (return to step 4).
- 5b. The user already has a set with that name: the system asks the user to choose a different name (return to step 4).
- 4a. The user cancels: no new set is created and the existing sets are unchanged.

**Success outcome:** The user's account holds multiple separate study sets, each with its own flashcards, so they can study different subjects at the same time.

---

### Use Case 5: Save Study Set

**Name:** Save Study Set

**Primary actor:** Student (User)

**Preconditions:**
- The user has a study set with new or changed flashcards.
- The user is logged in (signed up and authenticated through Supabase Auth).

**Trigger:** The user selects "Save", or the system auto-saves after a change.

**Main success scenario:**
1. The user selects "Save" on a study set.
2. The system sends the study set and its flashcards to the Supabase database, linked to the user's account.
3. Supabase stores the data and confirms.
4. The system shows a confirmation that the study set was saved.
5. Later, the user logs in and opens the study set, and the system loads it from Supabase with all its flashcards.

**Extensions:**
- 1a. The user is not logged in: the system prompts the user to log in or sign up, then continues from step 2.
- 3a. The save fails (for example, no network connection): the system shows an error message, keeps the unsaved changes locally, and lets the user retry.
- 5a. Login fails because of invalid credentials: the system shows an error message and lets the user try again.

**Success outcome:** The study set and its flashcards are stored in the cloud under the user's account and can be retrieved in later sessions.

---
## Minimal Viable Product (MVP)

| Team Member | MVP Use Case |
|-------------|--------------|
| Wes | Create a Set of Flashcards |
| Ryan | Generate Flashcards from Lecture Notes |
| Elia | Edit Flashcard |
| Olivia | Create Multiple Sets of Flashcards |
| Abel | Save Study Set |

---
## Entities

### Flashcard

### StudySet

### CardsList

### User

---
## API Usage

**APIs:** OpenAI/Gemini for flashcard generation, and Supabase for data storage and authentication.

**Documentation:**

- [Gemini API — Content Generation Reference](https://aistudio.google.com/api-reference/generate-content?hl=en)
- [Supabase API — Database Documentation](https://supabase.com/docs/guides/api)
- [Supabase Auth — Authentication Documentation](https://supabase.com/docs/guides/auth)

**How it is used in the program:**

- **AI API (OpenAI/Gemini):** The application will send text from uploaded lecture notes to the AI API to generate questions and answers, which will be used to create flashcards for the user to review and edit.
- **Supabase Database:** The application will save and retrieve each user's study sets and flashcards. Changes to saved cards and sets will be stored so users can access their updated study materials later.
- **Supabase Auth:** Users will be able to sign up and log in to their accounts. Their saved study sets and flashcards will be linked to their accounts so they can return to them in later study sessions.

---
## Design Diagrams

### UML Class Diagram

### Sequence Diagrams

---
## How to Run

[Instructions for running the program.]

---
## Notes

[Any additional project information.]
