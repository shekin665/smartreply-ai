# SmartReply AI

A college project: an email productivity dashboard with a rule-based smart-reply generator,
backed by **your real Gmail account**. Every user signs in with "Sign in with Google" - there
is no demo login - and the app reads/sends mail through the real Gmail REST API using the
OAuth2 access token Google issues after you grant consent.

> **Important:** the "AI" here is a rule-based Java engine (`ReplyStrategy` implementations
> + `KeywordService`), not a genuine LLM. It never invents a date, fact or promise - it only
> reuses text already present in the original email. A real AI provider could be dropped in
> later behind the same `ReplyStrategy` interface without touching the UI or controllers.
>
> SmartReply AI never sees or stores your Google password. Google itself handles sign-in; the
> app only ever receives a limited-scope OAuth2 access token (read Inbox + send mail), which
> you can revoke at any time from <https://myaccount.google.com/permissions>.

---

## 1. Tech stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 (Spring MVC, Spring Security OAuth2 Client, Spring Validation) |
| Build tool | Maven |
| Auth | Google OAuth2 "Sign in with Google" (Spring Security's `oauth2Login`) - no passwords handled by this app |
| Mail integration | Gmail REST API (`users.messages.list/get/send`) over HTTPS, using the signed-in user's own OAuth2 access token - no SMTP/IMAP, no App Password |
| View layer | Thymeleaf (login/dashboard shell) + vanilla JavaScript (all dynamic behaviour via `fetch`) |
| Styling | Plain CSS with CSS variables (light/dark theme), no CSS framework |
| Data | Demo/fictional emails live in memory (`ArrayList`) and reset on request; real Gmail messages are fetched live, never stored to disk |
| Tests | JUnit 5 (`spring-boot-starter-test`, `spring-security-test`) |

---

## 2. Package structure

```
com.smartreply.ai
├── SmartReplyApplication.java        Spring Boot entry point
├── model/                            Plain Java domain classes (no Spring annotations)
│   ├── Email.java                    abstract base class
│   ├── ReceivedEmail.java            subclass: mail received by the user (demo or real Gmail)
│   ├── SentEmail.java                subclass: mail authored by the user (sent or still a draft)
│   ├── Reply.java                    a generated/edited reply draft
│   ├── KeywordAnalysis.java          result of the keyword engine
│   └── Folder.java / Tone.java / Priority.java   enums
├── strategy/                         Reply generation (Strategy + Template Method patterns)
│   ├── ReplyStrategy.java            interface
│   ├── AbstractReplyStrategy.java    abstract class implementing the interface
│   ├── ProfessionalReplyStrategy.java
│   ├── FriendlyReplyStrategy.java
│   ├── FormalReplyStrategy.java
│   ├── ConciseReplyStrategy.java
│   ├── ApologeticReplyStrategy.java
│   └── ReplyStrategyFactory.java     picks the right strategy bean for a Tone
├── service/                          Business logic (Spring @Service beans)
│   ├── EmailService.java             owns the in-memory demo mailbox + folder operations + real Gmail send/sync
│   ├── ReplyService.java             generates/edits replies, tracks dashboard stats
│   ├── KeywordService.java           rule-based keyword/date/action-item/priority extraction
│   └── mail/                         real Gmail integration (no simulated/demo mode - every signed-in user is real)
│       ├── OutboundMailGateway.java      interface EmailService.send() depends on
│       ├── GmailApiMailGateway.java       sends via the Gmail REST API with the caller's OAuth2 token
│       ├── InboundMailGateway.java        interface EmailService.syncRealInbox() depends on
│       └── GmailApiInboundGateway.java    reads (read-only) via the Gmail REST API with the caller's OAuth2 token
├── controller/                       REST + Thymeleaf controllers
│   ├── AuthController.java           GET /api/auth/me - who's currently signed in
│   ├── EmailController.java          /api/emails/*
│   ├── ReplyController.java          /api/replies/*
│   ├── DemoController.java           /api/demo/reset (resets the fictional demo data only)
│   └── PageController.java           /, /login, /dashboard (Thymeleaf)
├── dto/                               Request/response shapes exposed over REST
├── exception/                        Custom exceptions + GlobalExceptionHandler
└── config/
    ├── SecurityConfig.java           Spring Security: Google OAuth2 login, CSRF, API 401 vs browser redirect
    └── DemoDataSeeder.java           builds the 20+ fictional demo emails (always present, alongside real Gmail data)
```

Frontend assets live under `src/main/resources/`:

```
templates/login.html, dashboard.html        Thymeleaf shells
static/css/base.css, login.css, dashboard.css
static/js/theme.js, api.js, login.js, app.js
```

---

## 3. Google Cloud Console setup (required - do this first)

Unlike many simple demos, this app has **no offline/no-setup mode** - every sign-in is a real
Google account, so you need a Google Cloud OAuth 2.0 Client ID before the app will even start.
This only needs to be done once.

1. Go to **<https://console.cloud.google.com/>** and create a new project (or pick an existing
   one) - top-left project switcher → "New Project".
2. **Enable the Gmail API**: left sidebar → "APIs & Services" → "Library" → search "Gmail API" →
   click it → **Enable**.
3. **Configure the OAuth consent screen**: "APIs & Services" → "OAuth consent screen".
   - User type: **External** (unless you have a Google Workspace org - then **Internal** is fine).
   - Fill in an app name (e.g. "SmartReply AI"), your email as support contact, and developer
     contact email.
   - Add scopes: `.../auth/gmail.readonly`, `.../auth/gmail.send`, `openid`, `email`, `profile`.
   - Under "Test users", **add your own Google account's email**. Since this app won't go
     through Google's verification process, only accounts explicitly listed as test users can
     sign in - trying to sign in with any other account will be rejected by Google.
4. **Create the OAuth Client ID**: "APIs & Services" → "Credentials" → "Create Credentials" →
   "OAuth client ID".
   - Application type: **Web application**.
   - Name: anything (e.g. "SmartReply AI local").
   - **Authorized redirect URIs** - add exactly:
     ```
     http://localhost:8080/login/oauth2/code/google
     ```
     (This must match exactly - Spring Security's default callback path for a registration
     named `google`. No trailing slash, no `https`, no different port.)
   - Click **Create**. Google shows you a **Client ID** and **Client Secret** - copy both.
5. **Set them as environment variables** before starting the app (never hardcode them in
   `application.properties` or commit them anywhere):
   ```bash
   export GOOGLE_CLIENT_ID="xxxxxxxxxx.apps.googleusercontent.com"
   export GOOGLE_CLIENT_SECRET="xxxxxxxxxx"
   ```

That's the one-time setup. From here on, starting the app and signing in works like any normal
"Sign in with Google" flow.

---

## 4. How to run it

### Prerequisites
Java 17+ and Maven 3.6+, plus the Google Cloud Console setup above. (This machine didn't have
Java/Maven pre-installed; a local Java 17 + Maven were downloaded into `~/tools` with no
`sudo`/Homebrew needed - see the appendix if you need to repeat that.)

### From the terminal

```bash
cd smartreply-ai
export GOOGLE_CLIENT_ID="xxxxxxxxxx.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="xxxxxxxxxx"
mvn clean test          # runs the full JUnit test suite (uses dummy OAuth2 config, contacts nothing real)
mvn spring-boot:run     # starts the app on http://localhost:8080
```

Then open **http://localhost:8080** - you'll be redirected to `/login`, where a single
"Sign in with Google" button starts the real OAuth2 flow (Google's own consent screen, not
anything rendered by this app).

### From an IDE (IntelliJ IDEA / Eclipse / VS Code)
1. Open the `smartreply-ai` folder as a Maven project.
2. Set the `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` environment variables in your run
   configuration.
3. Run `SmartReplyApplication.main()`.
4. Open `http://localhost:8080`.

### Resetting the demo
Click **"Reset demo data"** in the sidebar at any time, or call:
```bash
curl -X POST http://localhost:8080/api/demo/reset
```
This re-seeds the 21 fictional demo emails and clears any generated replies/drafts/sent items
created during the demo - it does **not** touch your real Gmail account, and any real messages
previously pulled in via Sync are cleared from view (sync again any time to bring them back).

---

## 5. How real Gmail sending/reading works

- **Sending** (`EmailController.send` → `EmailService.send` → `GmailApiMailGateway`): builds a
  standard RFC 2822 message (using `jakarta.mail`'s `MimeMessage` purely as a local message
  *builder* - no SMTP connection is opened), base64url-encodes it, and `POST`s it to
  `https://gmail.googleapis.com/gmail/v1/users/me/messages/send` with the signed-in user's
  OAuth2 access token as a Bearer header. If Gmail rejects it (bad token, invalid recipient),
  `EmailService` throws *before* touching any in-memory state, so a failed send never looks
  like a successful one.
- **Reading** (`EmailController.syncRealInbox` → `EmailService.syncRealInbox` →
  `GmailApiInboundGateway`): calls `users.messages.list` (INBOX, most recent N) then
  `users.messages.get` per message, and converts the result into `ReceivedEmail` objects tagged
  `fromRealGmail=true`. This is **strictly read-only** - no Gmail endpoint that modifies a
  label, read-state, or message is ever called, so syncing can never change your actual
  mailbox. Fetched messages are de-duplicated by Gmail's own message id, so clicking **"📥 Sync
  real inbox"** repeatedly only ever adds genuinely new messages.
- The fictional demo mailbox (`DemoDataSeeder`) is always present alongside real Gmail data -
  useful for a self-contained viva walkthrough that doesn't depend on what's actually in your
  inbox that day. Real messages show a **LIVE** tag; folder actions (star/archive/spam/delete)
  on a LIVE message only change this dashboard's local copy, never your real Gmail account.
- CSRF protection is on (cookie-based) specifically because this app can send real email - see
  `SecurityConfig` for why a forged cross-site request matters more here than in a purely
  read-only demo.

---

## 6. OOP concepts - exactly where to find them

| Concept | Where | How |
|---|---|---|
| **Encapsulation** | `model/Email.java` | Every field is `private`; only reachable via getters/setters. `EmailService`'s `List<Email> emails` is private - all mutation goes through its methods. |
| **Inheritance** | `model/ReceivedEmail.java`, `model/SentEmail.java` extend `model/Email.java`. `strategy/ProfessionalReplyStrategy.java` (and 4 siblings) extend `strategy/AbstractReplyStrategy.java`. | Subclasses reuse all base fields/behaviour and add only what's specific to them (`sent`/`generatedByAi` on `SentEmail`). |
| **Abstraction** | `model/Email.java` (abstract class with abstract methods `getDisplayName()`, `getDirectionLabel()`, `homeFolder()`, `isReceivedEmail()`) and `strategy/ReplyStrategy.java` / `service/mail/OutboundMailGateway.java` / `InboundMailGateway.java` (interfaces). | Callers (`EmailService`, `ReplyService`, DTOs) only ever program against the abstraction, never caring which concrete subclass/implementation they're holding. |
| **Polymorphism (overriding)** | Same four abstract `Email` methods, overridden differently in `ReceivedEmail` vs `SentEmail`. `AbstractReplyStrategy.generate()` calls `greeting()`/`bodyText()`/`closing()` which each of the 5 tone classes overrides differently - **Template Method pattern**. | `EmailSummaryDto.from(Email email)` calls `email.getDisplayName()` without knowing/caring about the runtime type - the JVM dispatches to the right override. |
| **Interfaces** | `strategy/ReplyStrategy.java`, `service/mail/OutboundMailGateway.java`, `service/mail/InboundMailGateway.java`. | Each defines a contract (generate a reply / send an email / fetch an inbox) that exactly one concrete class satisfies today, but swapping in a different implementation later (e.g. a real LLM, or a different mail provider) needs no change to the callers. |
| **Dependency Injection** | Every `@Service`/`@Component`/`@RestController` constructor (e.g. `ReplyService(EmailService, KeywordService, ReplyStrategyFactory)`, `EmailService(DemoDataSeeder, OutboundMailGateway, InboundMailGateway)`). `ReplyStrategyFactory(List<ReplyStrategy> strategies)` - Spring auto-collects all 5 `@Component` strategy beans into that list. | Keeps classes decoupled from `new`-ing their own dependencies and easy to unit test with hand-built fakes (see the tests). |
| **Collections** | `EmailService` (`ArrayList<Email>`, `EnumMap<Folder, Long>`, `Set<String>` for dedup), `ReplyService` (`ArrayList<Reply>`), `KeywordService` (`List`, `LinkedHashSet`, `LinkedHashMap`). | Chosen per need: `LinkedHashSet` for de-duplicated dates that keep first-seen order, `EnumMap` for compact folder counts, plain `ArrayList` for the ordered mailbox. |
| **Exception handling** | `exception/EmailNotFoundException`, `ReplyNotFoundException`, `MailDeliveryException`, `MailSyncException` (all `RuntimeException`), caught centrally by `exception/GlobalExceptionHandler` (`@RestControllerAdvice`). | Every service throws a specific, meaningful exception instead of returning null/booleans; the handler turns each into the right HTTP status + JSON body instead of leaking a stack trace. |
| **Declarative validation** | `jakarta.validation` annotations on DTOs (`@NotBlank`, `@NotNull`) checked via `@Valid` in controllers; `GlobalExceptionHandler.handleValidation()`. | Demonstrates declarative input validation separate from business-rule validation (which stays in the services, e.g. `EmailService.send()` checking for a blank recipient). |

---

## 7. Demo test cases

These can be run by hand in the browser, or scripted with `curl` once you have a session
cookie from a real browser-completed Google sign-in (the OAuth2 consent screen itself can't be
scripted with `curl` - it requires a real browser and a real Google account listed as a test
user).

1. **Sign-in flow** - Go to `/login`, click "Sign in with Google". You're redirected to
   Google's own consent screen (not anything this app renders), listing the exact scopes
   requested (read Inbox, send mail, basic profile). After granting consent, you land on
   `/dashboard` showing your real name/photo in the top bar.
2. **Reading mail marks it read** - In Inbox, click the first unread (bold) email. The
   "Unread" counter in the top bar should decrease by 1, and the email's row stops being bold.
3. **Keyword Intelligence on an urgent demo email** - Open "Overdue Book Notice - Immediate
   Action Required" in Inbox. The Keyword Intelligence panel should show priority **URGENT**, a
   detected date like `today`, and at least one action item mentioning "return the book".
4. **Generate a reply in two tones** - On any inbox email, click tone **Formal**, then
   "✨ Generate Reply" - the draft should start with "Dear <Name>,". Switch to tone
   **Friendly** and generate again - it should start with "Hey <Name>!" instead. Both replies
   reuse the same subject/sender - nothing is invented.
5. **Sync your real inbox** - Click "📥 Sync real inbox". Your most recent actual Gmail messages
   appear in the Inbox list tagged **LIVE**, interleaved with the fictional demo ones. Click it
   again immediately - it should report "No new real messages" (dedup working).
6. **Send a real reply to yourself** - Generate a reply, click "📨 Send reply", confirm the "this
   will send a REAL email" prompt, addressed to your own secondary email. Confirm it actually
   arrives in that inbox.
7. **Spam → Restore round-trip** - In Inbox, click 🚫 on a demo email to mark it spam; it
   disappears from Inbox and appears under Spam. Open it there and click ↩ Restore - it goes
   back to Inbox.
8. **Delete is a two-step (soft, then permanent) action** - Click 🗑 on an inbox email; it
   moves to Trash (not gone). Open it in Trash and click 🗑 again - it is now permanently
   removed and a repeat `GET` for that id returns 404.
9. **Reset demo data** - After making several changes, click "↺ Reset demo data". Fictional
   counts return to the original seeded numbers and any synced real messages disappear from
   view (your actual Gmail account is untouched either way).
10. **Unauthenticated API access is rejected** - `curl http://localhost:8080/api/emails/stats`
    without a session cookie returns a clean HTTP 401 JSON body; a plain browser visit to
    `/dashboard` while signed out instead redirects to `/login` - proving both the API and the
    page routes are properly protected, with the right behaviour for each.

---

## 8. Viva guide

**Q: Why is `Email` abstract instead of a normal class?**
Because "an email" on its own is incomplete - the base class can't say what name to show in
the UI (sender vs. recipient) or where it goes when restored. Making `getDisplayName()`,
`getDirectionLabel()` and `homeFolder()` abstract forces every subclass to supply that
missing behaviour, which is the textbook definition of abstraction.

**Q: Why two subclasses (`ReceivedEmail`/`SentEmail`) instead of one `Email` class with an
`isReceived` boolean flag?**
A boolean flag would need `if/else` branches scattered through the codebase every time
behaviour differs (display name, home folder). Subclassing lets each class answer "what's my
display name?" for itself - that logic lives in exactly one place per type, and the compiler
guarantees every subclass answers it (because the methods are abstract).

**Q: Where does polymorphism actually matter at runtime?**
`EmailSummaryDto.from(Email email)` and `EmailDetailDto.from(Email email)` both call
`email.getDisplayName()` through an `Email` reference, but the JVM runs
`ReceivedEmail.getDisplayName()` or `SentEmail.getDisplayName()` depending on the real object -
the DTO code never uses `instanceof` to decide. Same story for `AbstractReplyStrategy.generate()`
calling `greeting()`/`bodyText()`/`closing()`: one method body, five different outputs depending
on which concrete `ReplyStrategy` is plugged in.

**Q: What's the difference between the `ReplyStrategy` interface and `AbstractReplyStrategy`?**
The interface is the *contract* (what every tone must be able to do: `getTone()`,
`generate()`). The abstract class is a *partial implementation* of that contract - it fixes
`generate()`'s overall shape (greeting → body → closing, the Template Method pattern) as
`final`, so no tone subclass can accidentally change the structure, only the wording.

**Q: Why collections instead of arrays?**
`ArrayList` grows/shrinks as emails are created, drafted, sent or deleted - arrays are fixed
size. `EnumMap<Folder, Long>` in `EmailService.getFolderCounts()` is both type-safe (only
`Folder` values as keys) and compact/fast since `Folder` is an enum. `LinkedHashSet` in
`KeywordService.extractDates()` removes duplicate date matches while still preserving the
order they were first seen in the email text.

**Q: Why custom exceptions instead of returning `null`?**
Returning `null` pushes the "did this fail?" check onto every caller and is easy to forget.
Throwing `EmailNotFoundException`/`ReplyNotFoundException`/`MailDeliveryException`/
`MailSyncException` makes the failure impossible to silently ignore, and
`GlobalExceptionHandler` centralises turning each one into the correct HTTP status -
controllers stay free of `try/catch` noise.

**Q: Where is dependency injection used, and why?**
Every service/controller receives its dependencies through its constructor (Spring wires them
automatically). For example `ReplyStrategyFactory(List<ReplyStrategy> strategies)` never
names `ProfessionalReplyStrategy` etc. directly - Spring finds every `@Component` that
implements `ReplyStrategy` and hands the factory the whole list. It also makes testing easy:
the unit tests build an `EmailService` with hand-written lambda/anonymous-class fakes for
`OutboundMailGateway`/`InboundMailGateway`, no real network call or Spring container required.

**Q: Why is the "AI" not a real AI?**
`KeywordService` and the `ReplyStrategy` classes are deliberately simple, explainable Java
(regex + word frequency + template strings) so you can trace exactly why a given reply or
keyword came out the way it did - useful for a viva, and honest about not being a genuine LLM.
Because generation is hidden behind the `ReplyStrategy` interface, a real LLM-backed
implementation could be substituted later without changing `ReplyService`, the controllers, or
the UI - exactly the same reasoning that lets the Gmail integration sit behind
`OutboundMailGateway`/`InboundMailGateway` instead.

**Q: Why Spring Security's `oauth2Login` instead of a hand-rolled login form?**
A hand-rolled form would mean this app handling passwords directly - exactly what "Sign in
with Google" is meant to avoid. Spring Security's OAuth2 client support handles the entire
Authorization Code flow (redirect to Google, consent, callback, token exchange, session
creation) so the app only ever deals with a short-lived access token, never a credential.

**Q: Why does `/api/**` get a different 401 behaviour than `/dashboard`?**
A browser navigating to `/dashboard` while signed out should get a normal web redirect to
`/login`. A `fetch()` call from `app.js` to `/api/emails/stats` can't usefully follow an HTML
redirect - it needs a JSON body it can read and show as a toast. `SecurityConfig` wires a
`DelegatingAuthenticationEntryPoint` with an explicit default (the login-page redirect) plus
one override for `/api/**` (the JSON 401), rather than relying on Spring's
`defaultAuthenticationEntryPointFor` shortcut, which - with only one registered mapping -
would have silently made the JSON entry point the *global* default and broken the browser
redirect everywhere.

---

## 9. Known limitations (by design, for a college demo)

- There is no database - the fictional demo mailbox is in memory and resets on restart or via
  "Reset demo data"; real Gmail data is fetched live each time and never persisted.
- This is a single-page-app-per-session design with a server-side session (Spring Security's
  default) - no "remember me" across browser restarts, no refresh-token persistence beyond the
  session.
- Because the Google Cloud OAuth consent screen isn't submitted for verification, only Google
  accounts explicitly added as "test users" in step 3 of the Cloud Console setup can sign in -
  this is normal/expected for an unpublished OAuth app, not a bug.
- Sending/reading really does touch your real Gmail account the moment you sign in - there is
  no separate offline/simulated toggle left in this version (an earlier iteration had one; it
  was removed when the project moved to real-Google-account-only login).

---

## 10. Deploying to Render

The app ships with a `Dockerfile` (multi-stage: Maven build → slim JRE runtime) and a
`render.yaml` Blueprint, so Render can build and run it with no manual dashboard configuration
beyond the two secret values.

### One-time setup
1. **Push this repo to GitHub** (Render deploys from a connected Git repo - see the note on
   this below if `smartreply-ai` isn't on GitHub yet).
2. **Create a Render account** at <https://render.com> (free tier is enough for a demo) and
   connect your GitHub account.
3. **New → Blueprint**, pick the `smartreply-ai` repo. Render reads `render.yaml` and proposes
   the `smartreply-ai` web service automatically - review and click **Apply**.
4. Render will ask for the two secrets `render.yaml` marks `sync: false` - paste in your
   **`GOOGLE_CLIENT_ID`** and **`GOOGLE_CLIENT_SECRET`** (same values used locally).
5. Wait for the first deploy to finish - Render shows you the live URL, something like
   `https://smartreply-ai.onrender.com`.
6. **Add the new redirect URI in Google Cloud Console** (the one you set up in section 3):
   "APIs & Services" → "Credentials" → your OAuth Client ID → **Authorized redirect URIs** →
   add:
   ```
   https://smartreply-ai.onrender.com/login/oauth2/code/google
   ```
   (use your actual Render URL) - keep the `localhost` one too, Google allows multiple. Save.
7. Open the Render URL and sign in with Google exactly as you did locally.

### Notes
- The free Render tier spins the service down after inactivity and takes ~30-60s to wake back
  up on the next request - normal for a free-tier demo, not a bug.
- Nothing about the Gmail integration changes between local and deployed - it's the same
  `GmailApiMailGateway`/`GmailApiInboundGateway` calling the same Gmail REST API either way, just
  reached from a different `redirect_uri`.
- If you only added yourself (and maybe classmates) as OAuth "test users" in section 3, only
  those exact Google accounts can sign in on the deployed version either - that's expected for
  an unverified OAuth app, not specific to Render.

### If the project isn't on GitHub yet
```bash
cd smartreply-ai
git init
git add .
git commit -m "Initial commit"
```
Then create a new (empty) repository on GitHub's website, and follow the "push an existing
repository" instructions GitHub shows you (`git remote add origin ...`, `git push -u origin main`).

---

## 11. Appendix: installing Java 17 + Maven without Homebrew/sudo

If `java -version` shows an older JDK and `mvn` isn't installed (and you can't/don't want to
use Homebrew or sudo), you can install both locally:

```bash
mkdir -p ~/tools && cd ~/tools
curl -L -o jdk17.tar.gz "<a Temurin 17 tar.gz download URL for your OS/arch>"
curl -L -o maven.tar.gz "https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz"
tar -xzf jdk17.tar.gz && tar -xzf maven.tar.gz

cat > ~/tools/env.sh << 'EOF'
export JAVA_HOME=~/tools/<extracted-jdk-folder>/Contents/Home   # drop "/Contents/Home" on Linux
export MAVEN_HOME=~/tools/<extracted-maven-folder>
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
EOF
source ~/tools/env.sh
java -version && mvn -version
```
To make this automatic in every new terminal (instead of re-running `source` each time), add
`source ~/tools/env.sh` to the end of your `~/.zshrc` (or `~/.bashrc`).

---

## Priority keywords (personalised inbox)

Each signed-in user can save up to 20 personal keywords or short phrases (for example `exam`,
`invoice`, `purchase order`, a professor's name) in the **Priority keywords** panel in the sidebar.

- An email that mentions a keyword in its subject, body or sender is tagged **★ Priority** and listed
  first in the Inbox. Emails matching more keywords come first; the rest stay newest-first.
- Matching ignores upper/lower case and uses whole words, so `exam` matches "Exam schedule" but not
  "example". A phrase matches even if the words are separated by extra spaces or a line break.
- **Show matching mail only** filters the Inbox to emails that matched. The top bar shows how many
  Inbox emails match.
- Code: `service/PriorityKeywordService` (matching and ordering, session-scoped so users never see each
  other's keywords), `controller/PriorityKeywordController` (`/api/priority-keywords`), and
  `PriorityKeywordServiceTest`. The list is also saved in the browser (per Google account) and restored
  on the next visit, because the app has no database.

## Mail generator, enhance options and languages

- **Compose → Mail generator.** Choose the type of mail (invitation, reminder, meeting request), then enter the event title, date, time, venue and a few key points. The app writes the subject and message. Nothing is sent until you press Send.
- **Not quite right?** The chips (More formal, Friendlier, Shorter, More detailed) rewrite the mail in that style. The reply generator has the same chips, plus "Add an apology" and a box for a line of your own.
- **Languages.** English, Tamil, Hindi, Telugu, Malayalam, French, Spanish and German, in both the mail generator and the reply generator. The fixed wording lives in `src/main/resources/i18n/phrases_<code>.properties`; add a language with one new file and one line in `Language.java`. `PhraseBookTest` checks that no language is missing a line.
- The engine is rule-based, not an AI translator. Text you typed (titles, key points, quoted action items) is copied as written. Have a native speaker review the wording before formal use.
- Replies are signed with the signed-in user's name.
