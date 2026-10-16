---
name: smartreply-gmail-restart
description: Restarts the SmartReply AI Spring Boot app (at /Users/macbookpro/Downloads/trabliss-demo/smartreply-ai) and verifies its Google OAuth2/Gmail sign-in is wired correctly, catching the exact failure modes hit while building it - a stale process still holding port 8080, and a trailing newline/whitespace character accidentally picked up when copy-pasting GOOGLE_CLIENT_ID or GOOGLE_CLIENT_SECRET out of Google Cloud Console. Use this whenever the user wants to restart, relaunch, re-run, or rerun SmartReply AI; mentions the app not starting, port 8080 being in use, "Sign in with Google" not working, a Google OAuth client_id error, "invalid_client", or seeing %0A in an authorization URL; or asks to re-verify the Gmail login flow after changing credentials. Also trigger for short phrases like "restart smartreply", "run the gmail skill", or "check if oauth is working".
---

# SmartReply AI: restart + verify Google OAuth2/Gmail login

## Why this exists

Over several rounds of debugging, two issues kept recurring when starting this app:

1. **Port 8080 held by a previous run.** The app silently fails to start (or an old instance
   keeps answering requests) if something else is already listening there.
2. **A trailing newline baked into `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET`.** Copy-pasting
   the value out of Google Cloud Console's UI sometimes carries an invisible line break with
   it. Spring Boot happily accepts it at startup, builds a `client_id=...%0A` into the
   authorization URL, and Google silently rejects the sign-in with `invalid_client` - a
   confusing failure that looks nothing like its actual cause.

Credentials never pass through this conversation. You (the assistant) cannot set these
environment variables for the user - they live in the user's own terminal session, not yours.
Your job is to (a) give the user the exact commands to run themselves, in one terminal, and
(b) verify the *result* purely over HTTP, which needs no access to the credentials at all.

## Step 1 - Give the user the restart commands

Tell the user to run this in **one single terminal tab** - re-emphasize this if they have
multiple terminal tabs open, since running `export` in one tab and `mvn spring-boot:run` in
another is the single most common way this goes wrong:

```bash
lsof -ti :8080 | xargs kill -9 2>/dev/null; echo ready
```
Wait for `ready`, then (only needed if credentials changed or weren't set yet this session):
```bash
export GOOGLE_CLIENT_ID='paste-the-client-id-here'
export GOOGLE_CLIENT_ID=$(printf '%s' "$GOOGLE_CLIENT_ID" | tr -d '\n\r')
export GOOGLE_CLIENT_SECRET='paste-the-client-secret-here'
export GOOGLE_CLIENT_SECRET=$(printf '%s' "$GOOGLE_CLIENT_SECRET" | tr -d '\n\r')
```
Then start it:
```bash
cd /Users/macbookpro/Downloads/trabliss-demo/smartreply-ai
mvn spring-boot:run
```

If the user says the credentials are already exported and just needs a plain restart (e.g.
after a code change), the `export` lines can be skipped - just the kill + `mvn spring-boot:run`.

Wait for the user to confirm the app started (look for `Started SmartReplyApplication` in
their terminal), or for enough time to pass that it's reasonable to check.

## Step 2 - Run the verification script yourself

This part you *can* do directly - it only ever talks to `http://localhost:8080` over HTTP and
never touches the actual credential values:

```bash
bash "/Users/macbookpro/Downloads/trabliss-demo/smartreply-ai/.claude/skills/smartreply-gmail-restart/scripts/verify.sh"
```

It checks, in order:
1. The app answers at `/login` with a "Sign in with Google" button.
2. `/oauth2/authorization/google` redirects to `accounts.google.com` with a **clean** `client_id`
   (no encoded newline/whitespace).
3. An unauthenticated call to `/api/emails/stats` gets a clean 401 JSON body, not a redirect -
   proving the browser-vs-API authentication entry points in `SecurityConfig` are both still
   correctly wired (a real regression hit once while developing this: a single-mapping
   `defaultAuthenticationEntryPointFor` call silently became the *global* default and broke
   browser redirects everywhere, not just for the API).

## Step 3 - Report back plainly

Relay each PASS/FAIL line from the script's output. For any FAIL, the script's own detail
message already explains the likely cause and fix (e.g. "re-export with the `tr -d` sanitizer"
or "check for a port 8080 conflict") - pass that explanation along rather than re-deriving it,
and suggest the specific terminal command to fix it.

If everything passes, tell the user they're clear to open `http://localhost:8080` and click
"Sign in with Google" themselves - signing in is an interactive consent-screen flow that can't
be scripted or verified from here.
