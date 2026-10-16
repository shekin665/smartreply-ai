#!/usr/bin/env bash
# Verifies that the SmartReply AI app is up and that Google OAuth2 login is
# wired correctly. Pure read-only HTTP checks against localhost:8080 - never
# needs GOOGLE_CLIENT_ID/SECRET itself, since it only inspects what the
# already-running app exposes over HTTP.
#
# Exit code 0 = everything looks right. Non-zero = at least one check failed;
# stdout explains which one and what it usually means, based on the exact
# failure modes hit while building this app.

set -u
BASE_URL="${1:-http://localhost:8080}"
FAILED=0

check() {
  local name="$1"
  local ok="$2"
  local detail="$3"
  if [ "$ok" = "true" ]; then
    echo "PASS - $name"
  else
    echo "FAIL - $name"
    echo "       $detail"
    FAILED=1
  fi
}

echo "Checking $BASE_URL ..."
echo

# 1. App is up at all
LOGIN_CODE=$(curl -s -o /tmp/smartreply_login_body.html -w "%{http_code}" "$BASE_URL/login" 2>/dev/null)
if [ "$LOGIN_CODE" = "200" ] && grep -q "Sign in with Google" /tmp/smartreply_login_body.html 2>/dev/null; then
  check "App is up and /login renders" "true" ""
else
  check "App is up and /login renders" "false" \
    "Got HTTP $LOGIN_CODE from $BASE_URL/login (expected 200 with 'Sign in with Google' in the body). \
Is 'mvn spring-boot:run' actually running? Check for a port 8080 conflict with: lsof -i :8080"
  echo
  echo "Stopping further checks - nothing else will work until the app itself is reachable."
  rm -f /tmp/smartreply_login_body.html
  exit 1
fi

# 2. OAuth2 authorization redirect builds correctly, with a clean client_id
OAUTH_HEADERS=$(curl -s -D - -o /dev/null "$BASE_URL/oauth2/authorization/google" 2>/dev/null)
OAUTH_CODE=$(echo "$OAUTH_HEADERS" | head -1 | grep -o '[0-9][0-9][0-9]')
LOCATION=$(echo "$OAUTH_HEADERS" | grep -i '^location:' | sed 's/^[Ll]ocation: //' | tr -d '\r')

if [ "$OAUTH_CODE" != "302" ] || [ -z "$LOCATION" ]; then
  check "Google OAuth2 redirect" "false" \
    "Expected a 302 redirect to accounts.google.com, got HTTP $OAUTH_CODE with no Location header. \
This usually means GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET weren't set (or were empty) in the terminal \
that ran 'mvn spring-boot:run' - check the startup log for 'Client id of registration google must not be empty'."
elif echo "$LOCATION" | grep -qi "accounts.google.com"; then
  CLIENT_ID_PARAM=$(echo "$LOCATION" | grep -oE 'client_id=[^&]*')
  if echo "$CLIENT_ID_PARAM" | grep -qE '%0A|%0D|%20$'; then
    check "Google OAuth2 redirect has a clean client_id" "false" \
      "The client_id parameter contains an encoded newline/space ($CLIENT_ID_PARAM). \
This is the classic copy-paste-from-Google-Cloud-Console artifact. Fix it in the SAME terminal running the app: \
  export GOOGLE_CLIENT_ID=\$(printf '%s' \"\$GOOGLE_CLIENT_ID\" | tr -d '\\n\\r') \
then Ctrl+C and restart 'mvn spring-boot:run'."
  else
    check "Google OAuth2 redirect has a clean client_id" "true" ""
  fi
else
  check "Google OAuth2 redirect" "false" \
    "Got a redirect, but not to accounts.google.com (Location: $LOCATION). Something is misconfigured in SecurityConfig."
fi

# 3. API auth entry point returns clean JSON 401, not a redirect
API_HEADERS=$(curl -s -D - -o /tmp/smartreply_api_body.json "$BASE_URL/api/emails/stats" 2>/dev/null)
API_CODE=$(echo "$API_HEADERS" | head -1 | grep -o '[0-9][0-9][0-9]')
if [ "$API_CODE" = "401" ] && grep -q '"status":401' /tmp/smartreply_api_body.json 2>/dev/null; then
  check "Unauthenticated API call returns clean 401 JSON (not a redirect)" "true" ""
else
  check "Unauthenticated API call returns clean 401 JSON (not a redirect)" "false" \
    "Got HTTP $API_CODE instead of a 401 JSON body. If this is a 302/redirect, SecurityConfig's \
DelegatingAuthenticationEntryPoint for /api/** may have regressed back to the single-mapping bug \
(see SecurityConfig's class comment for why that matters)."
fi
rm -f /tmp/smartreply_login_body.html /tmp/smartreply_api_body.json

echo
if [ "$FAILED" = "0" ]; then
  echo "All checks passed. Ready to open $BASE_URL in a browser and click 'Sign in with Google'."
else
  echo "One or more checks failed - see details above before trying to sign in."
fi
exit $FAILED
