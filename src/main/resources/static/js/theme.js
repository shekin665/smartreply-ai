// Shared dark/light theme toggle + toast helper used by both the login and dashboard pages.

(function () {
  const THEME_KEY = "smartreply-theme";

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    try { localStorage.setItem(THEME_KEY, theme); } catch (e) { /* private mode */ }
    document.querySelectorAll("[data-theme-icon]").forEach((el) => {
      el.textContent = theme === "dark" ? "☀️" : "🌙";
    });
  }

  function initTheme() {
    let saved = "dark";
    try { saved = localStorage.getItem(THEME_KEY) || "dark"; } catch (e) { /* private mode */ }
    applyTheme(saved);
  }

  function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme") || "dark";
    applyTheme(current === "dark" ? "light" : "dark");
  }

  window.SmartReplyTheme = { initTheme, toggleTheme };
})();

function showToast(message, type) {
  const container = document.getElementById("toast-container");
  if (!container) return;
  const toast = document.createElement("div");
  toast.className = "toast" + (type ? " " + type : "");
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}
