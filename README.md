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
- Create and edit flashcards manually or generate them from course notes and PDFs with AI.
- Review question/answer cards at your own pace.
- Organize multiple study sets and save them locally when closing the app.

# User Stories
1) As a student, I want to create flashcards manually so that I can study my own questions.
2) As a student, I want to generate flashcards from PDFs and notes so that I can prepare materials quickly.
3) As a student, I want to edit and review flashcards so that my study materials stay accurate.
4) As a student, I want multiple study sets so that I can study different subjects separately.
5) As a student, I want to save my study sets so that I can return to them later.

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
Manual card creation, flashcards, and local saves work without credentials.

## Local saving

Changes save automatically when you close the app. If saving fails or a card is
incomplete, the window stays open so you can correct the problem and close again.
The desktop interface has no account, cloud transfer, or manual save buttons.
The existing Supabase integration code and schema remain available for future use.

## Study workflow

1. Choose **New set** and enter a subject title. In **Create & edit cards**, choose **Add card**
   and enter a question and answer to create flashcards manually, without an AI key.
2. For AI creation, open **PDF & AI generation**. Choose **Import PDF / notes** to import a
   text-based PDF (up to 10 MB) or UTF-8 `.txt` / `.md` file (up to 400 KB), or paste notes.
   Review the extracted text before generating. Notes are limited to 100,000 characters.
   Scanned/image-only and password-protected PDFs need a text-based, unlocked copy.
3. Select difficulty and 1–30 questions, then **Generate with AI**. This sends the notes to
   [Gemini's Generate Content API](https://ai.google.dev/api/generate-content).
   The generated cards go into a new set, preserving the original. Review AI answers before studying.
4. In **Create & edit cards**, add, select, edit, or delete question/answer pairs.
   No AI key is needed to enter your own cards.
5. In **Flashcards**, choose **Review flashcards** to flip between questions and answers.
6. Close the app to save changes automatically on this device.

Local JSON writes replace the previous file atomically where supported. Invalid or unreadable
saved data stops startup with an error and is preserved rather than silently overwritten.
Local files contain your notes in plain text. Keep private course materials on trusted devices.

## Implementation and validation

The packages follow the layers in the `ca-login` reference project. Each named class
and interface has its own Java file under `org.example.studyapp`:

- `entity`: `StudySet` and `Card`.
- `use_case/generate`: generation input/output boundaries, input/output data,
  interactor, and the generator data-access interface.
- `use_case/account`: authentication input boundary, input data, interactor,
  and account data-access interface.
- `use_case/storage`: load/save input boundary, interactor, and study-set data-access interface.
- `interface_adapter`: feature controllers and the generation presenter.
- `data_access`: local JSON storage, Supabase accounts/storage, Gemini generation,
  HTTP transport, and environment configuration.
- `view`: Swing components, user interaction, background tasks, and dialog lifecycle.
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

Existing saved study sets remain readable; obsolete retry-status fields are ignored.

Run `mvn test` for storage round trips, legacy-data compatibility, invalid-data preservation,
configuration parsing, PDF/text import, input validation, and use-case isolation with fake
data-access implementations. Live API integration requires your own configured services.
Team member ownership for user stories remains to be assigned.
