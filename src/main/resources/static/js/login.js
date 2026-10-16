SmartReplyTheme.initTheme();

// Spring Security redirects back here with ?error if Google sign-in failed
// or you declined the consent screen - show that plainly instead of silently
// doing nothing.
const params = new URLSearchParams(window.location.search);
if (params.has("error")) {
  const errorBox = document.getElementById("login-error");
  errorBox.textContent = "Sign-in with Google didn't complete - please try again.";
  errorBox.style.display = "block";
}
