# Team Project - Study App (placeholder name)

Please keep this up-to-date with information about your project throughout the term.

The readme should include information such as:
- a summary of what your application is all about
- a list of the user stories, along with who is responsible for each one
- information about the API(s) that your project uses 
- screenshots or animations demonstrating current functionality

By keeping this README up-to-date,
your team will find it easier to prepare for the final presentation
at the end of the term.

# Summary
- Turns course materials into practice quizzes and flashcards to help students study.
- A student uploads notes and chooses what to practice, and it creates flashcards and a practice quiz based off the notes.
- Incorrectly answered questions on the practice quizzes will be stored in a "Re-quiz" section, so the user can practice the questions they got wrong.
- Users' tests can be saved on an account which saves the previously created study materials to a cloud.
- Users can create multiple sets of study materials on each account.

# User Stories
1) As a student, I want to be able to take practice quizzes based off my notes so that I can study my course materials.
2) As a student, I want to be able to review my notes on flashcards, so that I am ready to take a practice quiz.
3) As a student, I want to be able to edit my existing practice quizzes and flashcards, so that I can keep them up to date and accurate.
4) As a student, I want to be able to create multiple sets of study materials, so that I can study different subjects at the same time.
5) As a student, I want to be able to save my study sets, so that I can go back to them whenever I want. 
6) As a student, I want to be able to go back and retry missed quiz questions, so that I can learn from my mistakes.
7) As a student, I want to be able to customize my practice quiz's difficulty and time limit, so that I can practice at a preferred level.

# APIs
- Google Gemini Generate Content API for note-based question/answer generation.
- Supabase Auth and REST API for account-based cloud storage.

# Running the application

This repository now contains a Java Swing desktop implementation of the stories above.
Requires JDK 11+ and Maven. From the project directory:

```sh
mvn test
mvn compile exec:java
```

In IntelliJ, import `pom.xml`, use JDK 11 or newer, set the working directory to the
project root, and run `org.example.studyapp.app.Main`.

## Credentials

A local `.env` file has been created with blank credential fields. It is ignored by Git.
For other machines, copy `.env.example` to `.env`. Fill in:

```dotenv
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash
SUPABASE_URL=
SUPABASE_ANON_KEY=
STUDY_APP_DATA_FILE=
```

- `GEMINI_API_KEY`: get a key from [Google AI Studio](https://aistudio.google.com/apikey).
  Required only for AI generation. The model is configurable for your account's availability.
- `SUPABASE_URL`: your Supabase project's HTTPS URL, such as `https://your-project.supabase.co`.
- `SUPABASE_ANON_KEY`: the project's public anon key or publishable API key.
  **Never use a service-role or secret key in this desktop client.**
- `STUDY_APP_DATA_FILE`: optional file path; leave blank to use
  `~/.study-app/study-sets.json`. Use separate paths for different people sharing a device.

System environment variables override `.env`. Restart after changing credentials.
Manual card creation, flashcards, quizzes, and local saves work without credentials.

## Cloud setup

1. Create a Supabase project and run [`supabase/schema.sql`](supabase/schema.sql) in its SQL editor.
2. Enable email/password authentication. Set email confirmation preferences in Supabase Auth.
3. Fill in the two Supabase values in `.env`, then restart.
4. Choose **Account → Create account**. If confirmation is enabled, confirm the email,
   then choose **Account → Sign in**.
5. **Upload to cloud** saves all local sets to that account. **Download from cloud** merges
   cloud sets into the local library; cloud versions replace sets with matching IDs after confirmation.

Cloud sync is explicit rather than automatic. Sign in again if the access token expires.
Sign out clears the in-memory session; local study files remain on the device. Local deletion
keeps cloud copies, so downloading can restore them. There is no cloud-delete control yet.
The schema enforces ownership using [Supabase row-level security](https://supabase.com/docs/guides/database/postgres/row-level-security).

## Study workflow

1. Choose **New set**, enter a subject title, and paste notes or import a UTF-8 `.txt` / `.md` file.
   PDF, Word, and image extraction are not included.
2. Select difficulty and 1–30 questions, then **Generate with AI**. This sends the notes to
   [Gemini's Generate Content API](https://ai.google.dev/api/generate-content).
   The generated cards go into a new set, preserving the original. Review AI answers before studying.
3. In **Edit cards**, add, select, edit, or delete question/answer pairs. The same pairs power
   both flashcards and practice quizzes. No AI key is needed to enter your own cards.
4. In **Practice**, review flashcards or start a quiz. Set the time limit to 0 for untimed practice.
5. Quizzes use short answers and self-assessment: write your answer, reveal the reference,
   then choose **I got it right** or **Needs practice**. This avoids pretending to grade prose reliably.
6. **Re-quiz missed** includes questions marked incorrect or left unanswered when time expires
   or a quiz is ended early. A correct retry clears that question from Re-quiz.
7. Choose **Save locally** after edits. Completed/ended quizzes save automatically. Closing with
   unsaved changes prompts you to save. Upload separately when you want a cloud copy.

Local JSON writes replace the previous file atomically where supported. Invalid or unreadable
saved data stops startup with an error and is preserved rather than silently overwritten.
Local files contain your notes in plain text; cloud accounts do not isolate the shared local library.
Cloud uploads contain the notes and cards. Keep private course materials on trusted devices/accounts.

## Implementation and validation

The packages follow the layers in the `ca-login` reference project. Each named class
and interface has its own Java file under `org.example.studyapp`:

- `entity`: `StudySet`, `Card`, and the framework-independent `QuizSession`.
- `use_case/generate`: generation input/output boundaries, input/output data,
  interactor, and the generator data-access interface.
- `use_case/account`: authentication input boundary, input data, interactor,
  and account data-access interface.
- `use_case/storage`: load/save input boundary, interactor, and study-set data-access interface.
- `use_case/quiz`: quiz input boundary and interactor for full quizzes and missed-card retries.
- `interface_adapter`: feature controllers and the generation presenter.
- `data_access`: local JSON storage, Supabase accounts/storage, Gemini generation,
  HTTP transport, and environment configuration.
- `view`: Swing components, user interaction, background tasks, and dialog/timer lifecycle.
- `app`: `Main` launches the app; `AppBuilder` selects and connects concrete dependencies.

The dependency direction is `view → controller → input boundary/interactor → entity`.
Data-access implementations implement interfaces owned by `use_case`, so the core
has no dependency on Swing, Gson, HTTP, or concrete stores. Local and cloud storage
share a load/save interface; account operations use a separate interface.

Generation follows `controller → interactor → output boundary → presenter → output data`.
The presenter returns a synchronous result containing the generated set and display
message. Swing's existing worker delivers it to the UI thread. Load/save and account
operations use direct synchronous results/exceptions; they do not need shared mutable
view models. Exceptions continue through the worker to the existing error dialogs.

Quiz rules and missed-card selection live in the entity/use-case layers; Swing owns
only the timer and interaction. Extracting `Card` does not change the saved JSON format.

Run `mvn test` for storage round trips, invalid-data preservation, configuration parsing,
quiz scoring, successful retries, timeout behavior, input validation, and use-case
isolation with fake data-access implementations. Live API integration
requires your own configured services. Team member ownership for user stories remains to be assigned.
