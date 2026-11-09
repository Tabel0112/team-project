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

### Use Case 1

### Use Case 2

### Use Case 3
(Add additional use cases as needed.)

---
## Minimal Viable Product (MVP)

| Team Member | MVP Use Case |
|-------------|--------------|
| Wes | Use Case |
| Ryan | Use Case |
| Elia | Use Case |
| Olivia | Use Case |
| Abel | Use Case |

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
