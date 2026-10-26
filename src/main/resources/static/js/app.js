SmartReplyTheme.initTheme();

const FOLDERS = ["INBOX", "SENT", "DRAFTS", "SPAM", "ARCHIVE", "TRASH"];
const TONES = ["PROFESSIONAL", "FRIENDLY", "FORMAL", "CONCISE", "APOLOGETIC", "DETAILED"];

const state = {
  folder: "INBOX",
  search: "",
  emails: [],
  selectedEmailId: null,
  selectedEmail: null,
  currentReply: null,
  selectedTone: "PROFESSIONAL",
  selectedLanguage: "ENGLISH",
  languages: [{ value: "ENGLISH", label: "English" }],
  keywords: [],
  priorityOnly: false,
  userEmail: "",
};

const listEl = document.getElementById("email-list");
const readerEl = document.getElementById("reader-pane");

function emptyState(emoji, text) {
  const div = document.createElement("div");
  div.className = "empty-state";
  const e = document.createElement("div");
  e.className = "emoji";
  e.textContent = emoji;
  const t = document.createElement("div");
  t.textContent = text;
  div.append(e, t);
  return div;
}

function formatDate(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  if (sameDay) {
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }
  return date.toLocaleDateString([], { month: "short", day: "numeric" });
}

// ---------- Bootstrapping ----------

async function init() {
  wireSidebar();
  wireSearch();
  wireCompose();
  wireLogout();
  wireResetDemo();
  wireSyncInbox();
  wirePriorityKeywords();
  wireMailGenerator();
  await Promise.all([loadMailOptions(), loadPriorityKeywords()]);
  genEl("assist-compose").appendChild(buildAssistBar(() => composeBodyInput.value, (t) => { composeBodyInput.value = t; }));
  await Promise.all([loadStats(), loadList()]);
}

// ---------- Priority keywords ----------
// The server keeps each signed-in user's keywords for the length of their session (there is no
// database). A copy is also kept in this browser, per Google account, and restored on the next
// visit so the list is not lost when the user signs out or the server restarts.

function keywordStorageKey() {
  return "smartreply.priorityKeywords." + (state.userEmail || "unknown");
}

function readSavedKeywords() {
  try {
    const saved = JSON.parse(localStorage.getItem(keywordStorageKey()) || "[]");
    return Array.isArray(saved) ? saved.filter((k) => typeof k === "string") : [];
  } catch (err) {
    return [];
  }
}

function saveKeywordsLocally(keywords) {
  try {
    localStorage.setItem(keywordStorageKey(), JSON.stringify(keywords));
  } catch (err) {
    // Storage can be blocked (private window). Keywords still work for this session.
  }
}

async function loadPriorityKeywords() {
  try {
    const me = await api.get("/api/auth/me");
    state.userEmail = me.email || "";
    let result = await api.get("/api/priority-keywords");
    const saved = readSavedKeywords();
    if (result.keywords.length === 0 && saved.length > 0) {
      result = await api.put("/api/priority-keywords", { keywords: saved });
    }
    setKeywords(result.keywords);
  } catch (err) {
    showToast(err.message, "error");
  }
}

function setKeywords(keywords) {
  state.keywords = keywords;
  saveKeywordsLocally(keywords);
  renderKeywordChips();
}

function renderKeywordChips() {
  const chips = document.getElementById("priority-chips");
  chips.innerHTML = "";
  state.keywords.forEach((keyword) => {
    const chip = document.createElement("span");
    chip.className = "keyword-chip";
    chip.textContent = keyword;
    const remove = document.createElement("button");
    remove.type = "button";
    remove.textContent = "×";
    remove.title = `Remove "${keyword}"`;
    remove.setAttribute("aria-label", `Remove keyword ${keyword}`);
    remove.addEventListener("click", () => removeKeyword(keyword));
    chip.appendChild(remove);
    chips.appendChild(chip);
  });
}

async function refreshAfterKeywordChange() {
  await Promise.all([loadStats(), loadList()]);
}

async function addKeyword(raw) {
  try {
    const result = await api.post("/api/priority-keywords", { keyword: raw });
    setKeywords(result.keywords);
    await refreshAfterKeywordChange();
    return true;
  } catch (err) {
    showToast(err.message, "error");
    return false;
  }
}

async function removeKeyword(keyword) {
  try {
    const result = await api.delete("/api/priority-keywords?keyword=" + encodeURIComponent(keyword));
    setKeywords(result.keywords);
    await refreshAfterKeywordChange();
  } catch (err) {
    showToast(err.message, "error");
  }
}

function wirePriorityKeywords() {
  const input = document.getElementById("priority-input");
  document.getElementById("priority-form").addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const value = input.value.trim();
    if (!value) return;
    if (await addKeyword(value)) {
      input.value = "";
      input.focus();
    }
  });
  document.getElementById("priority-only").addEventListener("change", (ev) => {
    state.priorityOnly = ev.target.checked;
    loadList();
  });
}

function wireSyncInbox() {
  document.getElementById("sync-inbox-btn").addEventListener("click", async () => {
    try {
      const result = await api.post("/api/emails/sync-real-inbox");
      await Promise.all([loadStats(), loadList()]);
      showToast(
        result.imported > 0
          ? `Imported ${result.imported} new real message${result.imported === 1 ? "" : "s"} from Gmail.`
          : "No new real messages since the last sync.",
        "success"
      );
    } catch (err) {
      showToast(err.message, "error");
    }
  });
}

/** Every send is real now (there is no demo/simulated mode) - always ask for one explicit confirmation. */
function confirmRealSend(recipient) {
  return confirm(`This will send a REAL email to ${recipient} through your Gmail account.\n\nContinue?`);
}

function wireSidebar() {
  document.querySelectorAll(".nav-item").forEach((item) => {
    item.addEventListener("click", () => selectFolder(item.dataset.folder));
  });
}

function wireSearch() {
  const input = document.getElementById("search-input");
  let timer = null;
  input.addEventListener("input", () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      state.search = input.value.trim();
      loadList();
    }, 250);
  });
}

function wireLogout() {
  // Spring Security's /logout endpoint needs a real POST + CSRF token, which a form
  // submit provides correctly (simpler and more robust than reimplementing that in fetch).
  document.getElementById("logout-btn").addEventListener("click", () => {
    document.getElementById("logout-form").submit();
  });
}

function wireResetDemo() {
  document.getElementById("reset-demo-btn").addEventListener("click", async () => {
    if (!confirm("Reset all demo emails, drafts and generated replies?")) return;
    try {
      await api.post("/api/demo/reset");
      state.selectedEmailId = null;
      state.selectedEmail = null;
      renderEmptyReader();
      await Promise.all([loadStats(), loadList()]);
      showToast("Demo data has been reset.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });
}

function selectFolder(folder) {
  state.folder = folder;
  state.selectedEmailId = null;
  state.selectedEmail = null;
  document.querySelectorAll(".nav-item").forEach((item) => {
    item.classList.toggle("active", item.dataset.folder === folder);
  });
  document.getElementById("search-input").value = "";
  state.search = "";
  renderEmptyReader();
  loadList();
}

// ---------- Stats ----------

async function loadStats() {
  try {
    const stats = await api.get("/api/emails/stats");
    document.getElementById("stat-unread").textContent = stats.unreadCount;
    document.getElementById("stat-priority").textContent = stats.priorityCount;
    document.getElementById("stat-replies").textContent = stats.repliesGenerated;
    document.getElementById("stat-time").textContent = stats.timeSavedMinutes + "m";
    document.getElementById("count-INBOX").textContent = stats.inboxCount;
    document.getElementById("count-SENT").textContent = stats.sentCount;
    document.getElementById("count-DRAFTS").textContent = stats.draftsCount;
    document.getElementById("count-SPAM").textContent = stats.spamCount;
  } catch (err) {
    showToast(err.message, "error");
  }
}

// ---------- Email list ----------

async function loadList() {
  listEl.innerHTML = "";
  listEl.appendChild(emptyState("⏳", "Loading..."));
  try {
    const params = new URLSearchParams({ folder: state.folder });
    if (state.search) params.set("search", state.search);
    if (state.priorityOnly) params.set("priorityOnly", "true");
    const emails = await api.get("/api/emails?" + params.toString());
    state.emails = emails;
    renderList(emails);
  } catch (err) {
    listEl.innerHTML = "";
    listEl.appendChild(emptyState("⚠️", err.message));
  }
}

function renderList(emails) {
  listEl.innerHTML = "";
  if (emails.length === 0) {
    const emptyText = state.priorityOnly
      ? "No emails match your priority keywords"
      : "No emails in this folder" + (state.search ? " matching your search" : "");
    listEl.appendChild(emptyState("📭", emptyText));
    return;
  }
  emails.forEach((email) => listEl.appendChild(renderListItem(email)));
}

function renderListItem(email) {
  const item = document.createElement("div");
  const matched = email.matchedKeywords || [];
  item.className = "email-item" + (email.read ? "" : " unread") + (matched.length ? " priority" : "")
    + (email.id === state.selectedEmailId ? " selected" : "");
  item.dataset.id = email.id;

  const main = document.createElement("div");
  main.className = "email-item-main";

  const row1 = document.createElement("div");
  row1.className = "email-item-row1";
  const from = document.createElement("div");
  from.className = "email-from";
  from.textContent = (email.directionLabel === "to" ? "To: " : "") + email.displayName;
  if (email.fromRealGmail) {
    const liveTag = document.createElement("span");
    liveTag.className = "badge badge-urgent";
    liveTag.style.marginLeft = "6px";
    liveTag.textContent = "LIVE";
    liveTag.title = "Imported from your real Gmail account";
    from.appendChild(liveTag);
  }
  const date = document.createElement("div");
  date.className = "email-date";
  date.textContent = formatDate(email.date);
  row1.append(from, date);

  const subject = document.createElement("div");
  subject.className = "email-subject";
  subject.textContent = email.subject || "(no subject)";

  const snippet = document.createElement("div");
  snippet.className = "email-snippet";
  snippet.textContent = email.snippet || "";

  main.append(row1, subject, snippet);
  if (matched.length) {
    const tag = document.createElement("span");
    tag.className = "priority-tag";
    tag.textContent = "★ Priority: " + matched.join(", ");
    tag.title = "This email mentions your priority keyword" + (matched.length > 1 ? "s" : "");
    main.appendChild(tag);
  }

  const star = document.createElement("span");
  star.className = "email-item-star" + (email.starred ? " starred" : "");
  star.textContent = email.starred ? "★" : "☆";
  star.title = "Toggle star";
  star.addEventListener("click", async (ev) => {
    ev.stopPropagation();
    try {
      await api.post(`/api/emails/${email.id}/star`);
      await loadList();
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  item.append(main, star);
  item.addEventListener("click", () => {
    if (state.folder === "DRAFTS") {
      openComposeForDraft(email.id);
    } else {
      selectEmail(email.id);
    }
  });
  return item;
}

// ---------- Reader ----------

function renderEmptyReader() {
  readerEl.innerHTML = "";
  readerEl.appendChild(emptyState("✉️", "Select an email to read it"));
}

async function selectEmail(id) {
  state.selectedEmailId = id;
  readerEl.innerHTML = "";
  readerEl.appendChild(emptyState("⏳", "Loading..."));
  try {
    const email = await api.get(`/api/emails/${id}`);
    state.selectedEmail = email;
    state.currentReply = null;
    renderReader(email);
    await Promise.all([loadList(), loadStats()]);
  } catch (err) {
    readerEl.innerHTML = "";
    readerEl.appendChild(emptyState("⚠️", err.message));
  }
}

function makeIconButton(icon, title, onClick) {
  const btn = document.createElement("button");
  btn.className = "icon-btn";
  btn.title = title;
  btn.textContent = icon;
  btn.addEventListener("click", onClick);
  return btn;
}

function renderReader(email) {
  readerEl.innerHTML = "";

  const header = document.createElement("div");
  header.className = "reader-header";

  const metaWrap = document.createElement("div");
  const subject = document.createElement("h2");
  subject.className = "reader-subject";
  subject.textContent = email.subject || "(no subject)";
  if (email.fromRealGmail) {
    const liveTag = document.createElement("span");
    liveTag.className = "badge badge-urgent";
    liveTag.style.marginLeft = "8px";
    liveTag.textContent = "LIVE - real Gmail message";
    subject.appendChild(liveTag);
  }
  const meta = document.createElement("div");
  meta.className = "reader-meta";
  meta.textContent = `${email.directionLabel} ${email.displayName} · ${new Date(email.date).toLocaleString()}`;
  metaWrap.append(subject, meta);
  if (email.fromRealGmail) {
    const note = document.createElement("div");
    note.className = "reader-meta";
    note.style.marginTop = "4px";
    note.textContent = "Star/archive/spam/delete on this message only affect this dashboard view - your real Gmail account is unchanged.";
    metaWrap.append(note);
  }

  const actions = document.createElement("div");
  actions.className = "reader-actions";

  const starBtn = makeIconButton(email.starred ? "★" : "☆", "Toggle star", async () => {
    await runAction(`/api/emails/${email.id}/star`);
  });
  if (email.starred) starBtn.classList.add("active");
  actions.appendChild(starBtn);

  if (email.folder !== "ARCHIVE") {
    actions.appendChild(makeIconButton("🗄", "Archive", async () => {
      await runAction(`/api/emails/${email.id}/archive`, true);
    }));
  }
  if (email.folder !== "SPAM") {
    actions.appendChild(makeIconButton("🚫", "Mark as spam", async () => {
      await runAction(`/api/emails/${email.id}/spam`, true);
    }));
  }
  if (["SPAM", "ARCHIVE", "TRASH"].includes(email.folder)) {
    actions.appendChild(makeIconButton("↩", "Restore", async () => {
      await runAction(`/api/emails/${email.id}/restore`, true);
    }));
  }
  actions.appendChild(makeIconButton("🗑", "Delete", async () => {
    if (!confirm("Delete this email?")) return;
    try {
      await deleteEmail(email.id);
    } catch (err) {
      showToast(err.message, "error");
    }
  }));

  header.append(metaWrap, actions);

  const body = document.createElement("div");
  body.className = "reader-body";
  appendBodyWithLinks(body, email.body || "");

  readerEl.append(header, body);
  readerEl.appendChild(buildKeywordPanel(email));
  readerEl.appendChild(buildReplyPanel(email));
}

// Long tracking links (LinkedIn, newsletters) bury the real message. Show each web address as a short
// clickable label and keep the full address in the link. Only http/https links are made clickable and
// everything is added as text nodes, so nothing in an email can inject HTML.
const URL_PATTERN = /https?:\/\/[^\s<>"]+/g;

function shortLinkLabel(url) {
  try {
    const parsed = new URL(url);
    const host = parsed.hostname.replace(/^www\./, "");
    const path = parsed.pathname === "/" ? "" : parsed.pathname;
    const label = host + path;
    return label.length > 48 ? label.slice(0, 47) + "…" : label;
  } catch (err) {
    return url.length > 48 ? url.slice(0, 47) + "…" : url;
  }
}

function appendBodyWithLinks(container, text) {
  let last = 0;
  for (const match of text.matchAll(URL_PATTERN)) {
    // Trailing punctuation belongs to the sentence, not to the address.
    const url = match[0].replace(/[.,;:!?)\]]+$/, "");
    if (match.index > last) {
      container.appendChild(document.createTextNode(text.slice(last, match.index)));
    }
    const link = document.createElement("a");
    link.href = url;
    link.target = "_blank";
    link.rel = "noopener noreferrer nofollow";
    link.title = url;
    link.textContent = "🔗 " + shortLinkLabel(url);
    container.appendChild(link);
    last = match.index + url.length;
  }
  if (last < text.length) {
    container.appendChild(document.createTextNode(text.slice(last)));
  }
}

async function deleteEmail(emailId) {
  await api.delete(`/api/emails/${emailId}`);
  state.selectedEmailId = null;
  state.selectedEmail = null;
  renderEmptyReader();
  await Promise.all([loadList(), loadStats()]);
  showToast("Email deleted.", "success");
}

async function runAction(url, clearReaderAfter) {
  try {
    await api.post(url);
    if (clearReaderAfter) {
      state.selectedEmailId = null;
      state.selectedEmail = null;
      renderEmptyReader();
    }
    await Promise.all([loadList(), loadStats()]);
    if (!clearReaderAfter && state.selectedEmailId) {
      const refreshed = await api.get(`/api/emails/${state.selectedEmailId}`);
      state.selectedEmail = refreshed;
      renderReader(refreshed);
    }
  } catch (err) {
    showToast(err.message, "error");
  }
}

// ---------- Keyword intelligence ----------

function buildKeywordPanel(email) {
  const panel = document.createElement("div");
  panel.className = "panel";
  const title = document.createElement("div");
  title.className = "panel-title";
  title.textContent = "Keyword Intelligence";
  panel.appendChild(title);

  const body = document.createElement("div");
  body.textContent = "Analyzing...";
  panel.appendChild(body);

  api.get(`/api/emails/${email.id}/keywords`).then((analysis) => {
    panel.removeChild(body);
    panel.insertBefore(renderKeywordAnalysis(analysis), null);
  }).catch((err) => {
    body.textContent = "Could not analyze this email: " + err.message;
  });

  return panel;
}

function renderKeywordAnalysis(analysis) {
  const wrap = document.createElement("div");

  const priorityRow = document.createElement("div");
  priorityRow.style.marginBottom = "10px";
  const badge = document.createElement("span");
  badge.className = "badge badge-" + analysis.priority.toLowerCase();
  badge.textContent = analysis.priority + " priority";
  priorityRow.appendChild(badge);
  wrap.appendChild(priorityRow);

  wrap.appendChild(chipSection("Keywords", analysis.keywords, "No notable keywords detected."));
  wrap.appendChild(chipSection("Dates / deadlines", analysis.dates, "No dates detected."));

  const actionTitle = document.createElement("div");
  actionTitle.style.fontSize = "12px";
  actionTitle.style.color = "var(--text-muted)";
  actionTitle.style.marginBottom = "4px";
  actionTitle.textContent = "Action items";
  wrap.appendChild(actionTitle);

  if (analysis.actionItems.length === 0) {
    const none = document.createElement("div");
    none.className = "action-item-row";
    none.textContent = "No action items detected.";
    wrap.appendChild(none);
  } else {
    analysis.actionItems.forEach((item) => {
      const row = document.createElement("div");
      row.className = "action-item-row";
      row.textContent = "• " + item;
      wrap.appendChild(row);
    });
  }

  return wrap;
}

function chipSection(label, items, emptyText) {
  const section = document.createElement("div");
  const title = document.createElement("div");
  title.style.fontSize = "12px";
  title.style.color = "var(--text-muted)";
  title.style.marginBottom = "4px";
  title.textContent = label;
  section.appendChild(title);

  const row = document.createElement("div");
  row.className = "chip-row";
  if (items.length === 0) {
    const span = document.createElement("span");
    span.style.fontSize = "12px";
    span.style.color = "var(--text-muted)";
    span.textContent = emptyText;
    row.appendChild(span);
  } else {
    items.forEach((it) => {
      const chip = document.createElement("span");
      chip.className = "chip";
      chip.textContent = it;
      row.appendChild(chip);
    });
  }
  section.appendChild(row);
  return section;
}

// ---------- Reply generator ----------

function buildReplyPanel(email) {
  const panel = document.createElement("div");
  panel.className = "panel";

  const title = document.createElement("div");
  title.className = "panel-title";
  title.textContent = "Smart Reply Generator";
  panel.appendChild(title);

  const toneRow = document.createElement("div");
  toneRow.className = "tone-row";
  TONES.forEach((tone) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "tone-btn" + (tone === state.selectedTone ? " active" : "");
    btn.dataset.tone = tone;
    btn.textContent = tone.charAt(0) + tone.slice(1).toLowerCase();
    btn.addEventListener("click", () => setTone(tone));
    toneRow.appendChild(btn);
  });
  panel.appendChild(toneRow);

  const optionRow = document.createElement("div");
  optionRow.className = "reply-options";
  const langSelect = document.createElement("select");
  langSelect.className = "lang-select";
  langSelect.setAttribute("aria-label", "Reply language");
  fillSelect(langSelect, state.languages, state.selectedLanguage);
  const noteInput = document.createElement("input");
  noteInput.type = "text";
  noteInput.maxLength = 300;
  noteInput.placeholder = "Add a line of your own (optional)";
  noteInput.setAttribute("aria-label", "Your own line");
  optionRow.append(langSelect, noteInput);
  panel.appendChild(optionRow);

  const generateBtn = document.createElement("button");
  generateBtn.type = "button";
  generateBtn.className = "btn btn-primary";
  generateBtn.style.marginBottom = "14px";
  generateBtn.textContent = "✨ Generate Reply";
  panel.appendChild(generateBtn);

  const textarea = document.createElement("textarea");
  textarea.className = "reply-textarea";
  textarea.placeholder = "Your generated reply will appear here - feel free to edit it before sending.";
  textarea.style.display = "none";
  panel.appendChild(textarea);

  const replyActions = document.createElement("div");
  replyActions.className = "reply-actions";
  replyActions.style.display = "none";

  const copyBtn = actionButton("📋 Copy", "btn-secondary", async () => {
    await navigator.clipboard.writeText(textarea.value);
    showToast("Reply copied to clipboard.", "success");
  });
  const saveEditsBtn = actionButton("💾 Save edits", "btn-secondary", async () => {
    if (!state.currentReply) return;
    try {
      await api.put(`/api/replies/${state.currentReply.id}`, { content: textarea.value });
      showToast("Edits saved.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });
  const draftBtn = actionButton("📝 Save as draft", "btn-secondary", async () => {
    await saveReplyAsEmail(email, textarea.value, false);
  });
  const sendBtn = actionButton("📨 Send reply", "btn-primary", async () => {
    await saveReplyAsEmail(email, textarea.value, true);
  });

  replyActions.append(copyBtn, saveEditsBtn, draftBtn, sendBtn);

  const enhanceRow = document.createElement("div");
  enhanceRow.className = "chip-row";
  enhanceRow.style.display = "none";
  const enhanceLabel = document.createElement("span");
  enhanceLabel.className = "chip-label";
  enhanceLabel.textContent = "Not quite right?";
  enhanceRow.appendChild(enhanceLabel);
  [["More formal", "FORMAL"], ["Friendlier", "FRIENDLY"], ["Shorter", "CONCISE"],
   ["More detailed", "DETAILED"], ["Add an apology", "APOLOGETIC"]].forEach(([label, tone]) => {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "chip";
    chip.textContent = label;
    chip.addEventListener("click", async () => {
      if (replyEdited() && !confirm("Replace the reply you edited with a new version?")) return;
      setTone(tone);
      await runGenerate();
    });
    enhanceRow.appendChild(chip);
  });
  panel.appendChild(enhanceRow);
  panel.appendChild(replyActions);
  const replyAssist = buildAssistBar(() => textarea.value, (t) => { textarea.value = t; });
  replyAssist.style.display = "none";
  panel.appendChild(replyAssist);

  let lastGenerated = "";
  const replyEdited = () => textarea.style.display !== "none" && textarea.value !== lastGenerated;
  const setTone = (tone) => {
    state.selectedTone = tone;
    toneRow.querySelectorAll(".tone-btn").forEach((b) => {
      b.classList.toggle("active", b.dataset.tone === tone);
    });
  };
  langSelect.addEventListener("change", async () => {
    state.selectedLanguage = langSelect.value;
    if (textarea.style.display === "none") return;
    if (replyEdited() && !confirm("Rewrite the reply in this language? Your edits will be replaced.")) {
      langSelect.value = state.currentReply ? state.currentReply.language : "ENGLISH";
      state.selectedLanguage = langSelect.value;
      return;
    }
    await runGenerate();
  });

  async function runGenerate() {
    generateBtn.disabled = true;
    generateBtn.textContent = "Generating...";
    try {
      const reply = await api.post("/api/replies/generate", {
        emailId: email.id,
        tone: state.selectedTone,
        language: state.selectedLanguage,
        extraNote: noteInput.value.trim() || null,
      });
      state.currentReply = reply;
      lastGenerated = reply.content;
      textarea.value = reply.content;
      textarea.style.display = "block";
      replyActions.style.display = "flex";
      enhanceRow.style.display = "flex";
      replyAssist.style.display = "flex";
      await loadStats();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      generateBtn.disabled = false;
      generateBtn.textContent = "✨ Generate Reply";
    }
  }
  generateBtn.addEventListener("click", runGenerate);

  return panel;
}

function actionButton(label, cls, onClick) {
  const btn = document.createElement("button");
  btn.type = "button";
  btn.className = "btn " + cls + " btn-sm";
  btn.textContent = label;
  btn.addEventListener("click", onClick);
  return btn;
}

async function saveReplyAsEmail(originalEmail, content, send) {
  const recipient = originalEmail.directionLabel === "from" ? originalEmail.fromAddress : originalEmail.toAddress;
  if (send && !confirmRealSend(recipient)) return;
  const subject = originalEmail.subject && originalEmail.subject.toLowerCase().startsWith("re:")
    ? originalEmail.subject
    : "Re: " + originalEmail.subject;
  const payload = {
    to: recipient,
    subject: subject,
    body: content,
    repliedToEmailId: originalEmail.id,
  };
  try {
    await api.post(send ? "/api/emails/send" : "/api/emails/draft", payload);
    showToast(send ? "Reply sent." : "Reply saved as draft.", "success");
    await Promise.all([loadList(), loadStats()]);
  } catch (err) {
    showToast(err.message, "error");
  }
}

// ---------- Compose modal ----------

const composeModal = document.getElementById("compose-modal");
const composeForm = document.getElementById("compose-form");
const composeIdInput = document.getElementById("compose-id");
const composeRepliedToInput = document.getElementById("compose-replied-to");
const composeToInput = document.getElementById("compose-to");
const composeSubjectInput = document.getElementById("compose-subject");
const composeBodyInput = document.getElementById("compose-body");
const composeDeleteBtn = document.getElementById("compose-delete-btn");

function wireCompose() {
  document.getElementById("compose-btn").addEventListener("click", () => openCompose(null));
  document.getElementById("compose-cancel-btn").addEventListener("click", closeCompose);
  document.getElementById("compose-save-draft-btn").addEventListener("click", async () => {
    await submitCompose(false);
  });
  composeForm.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    await submitCompose(true);
  });
  composeDeleteBtn.addEventListener("click", async () => {
    const id = composeIdInput.value;
    if (!id || !confirm("Delete this draft?")) return;
    try {
      await api.delete(`/api/emails/${id}`);
      closeCompose();
      await Promise.all([loadList(), loadStats()]);
      showToast("Draft deleted.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });
  composeModal.addEventListener("click", (ev) => {
    if (ev.target === composeModal) closeCompose();
  });
}

function openCompose(draft) {
  resetMailGenerator(!draft);
  document.getElementById("compose-modal-title").textContent = draft ? "Edit draft" : "New message";
  composeIdInput.value = draft ? draft.id : "";
  composeRepliedToInput.value = "";
  composeToInput.value = draft ? draft.toAddress : "";
  composeSubjectInput.value = draft ? draft.subject : "";
  composeBodyInput.value = draft ? draft.body : "";
  composeDeleteBtn.style.display = draft ? "inline-flex" : "none";
  clearComposeErrors();
  composeModal.classList.add("open");
}

async function openComposeForDraft(id) {
  try {
    const draft = await api.get(`/api/emails/${id}`);
    openCompose(draft);
  } catch (err) {
    showToast(err.message, "error");
  }
}

function closeCompose() {
  composeModal.classList.remove("open");
  composeForm.reset();
  resetMailGenerator(true);
}

function clearComposeErrors() {
  document.getElementById("compose-to-error").textContent = "";
  document.getElementById("compose-subject-error").textContent = "";
}

async function submitCompose(send) {
  clearComposeErrors();
  let hasError = false;
  if (!composeToInput.value.trim()) {
    document.getElementById("compose-to-error").textContent = "Recipient is required";
    hasError = true;
  }
  if (!composeSubjectInput.value.trim()) {
    document.getElementById("compose-subject-error").textContent = "Subject is required";
    hasError = true;
  }
  if (hasError) return;

  const recipient = composeToInput.value.trim();
  if (send && !confirmRealSend(recipient)) return;

  const payload = {
    id: composeIdInput.value || null,
    to: recipient,
    subject: composeSubjectInput.value.trim(),
    body: composeBodyInput.value,
    repliedToEmailId: composeRepliedToInput.value || null,
  };

  try {
    await api.post(send ? "/api/emails/send" : "/api/emails/draft", payload);
    closeCompose();
    await Promise.all([loadList(), loadStats()]);
    showToast(send ? "Email sent." : "Draft saved.", "success");
  } catch (err) {
    showToast(err.message, "error");
  }
}

// ---------- Mail generator (compose) ----------

const genEl = (id) => document.getElementById(id);
let genState = { generated: false, subject: "", body: "" };

function fillSelect(select, options, selected) {
  select.replaceChildren();
  options.forEach((o) => {
    const opt = document.createElement("option");
    opt.value = o.value;
    opt.textContent = o.label;
    select.appendChild(opt);
  });
  if (selected) select.value = selected;
}

async function loadMailOptions() {
  try {
    const opts = await api.get("/api/mail/options");
    state.languages = opts.languages;
    fillSelect(genEl("gen-kind"), opts.kinds);
    fillSelect(genEl("gen-language"), opts.languages, "ENGLISH");
  } catch (err) {
    // The generator still works in English with the built-in lists if the options cannot be loaded.
    fillSelect(genEl("gen-kind"), [
      { value: "INVITATION", label: "Invitation" },
      { value: "REMINDER", label: "Reminder" },
      { value: "MEETING_REQUEST", label: "Meeting request" },
    ]);
    fillSelect(genEl("gen-language"), state.languages, "ENGLISH");
  }
}

function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  return d.getFullYear() + "-" + pad(d.getMonth() + 1) + "-" + pad(d.getDate());
}

/** Clears the generator. `open` shows it (new messages) or folds it away (editing a draft). */
function resetMailGenerator(open) {
  ["gen-title", "gen-date", "gen-time", "gen-venue", "gen-recipient", "gen-points"].forEach((id) => {
    genEl(id).value = "";
  });
  genEl("gen-date").min = todayIso();
  genEl("gen-tone").value = "FORMAL";
  genEl("gen-language").value = "ENGLISH";
  genEl("gen-error").textContent = "";
  genEl("gen-enhance").style.display = "none";
  genEl("mailgen").open = !!open;
  genState = { generated: false, subject: "", body: "" };
}

function setGenTone(tone) {
  genEl("gen-tone").value = tone;
}

async function runMailGenerator() {
  const errorEl = genEl("gen-error");
  errorEl.textContent = "";
  const title = genEl("gen-title").value.trim();
  const date = genEl("gen-date").value;
  if (!title) { errorEl.textContent = "Enter the event title."; return; }
  if (!date) { errorEl.textContent = "Choose the event date."; return; }

  const edited = genState.generated &&
    (composeSubjectInput.value !== genState.subject || composeBodyInput.value !== genState.body);
  const hasText = composeSubjectInput.value.trim() || composeBodyInput.value.trim();
  if ((edited || (!genState.generated && hasText)) &&
      !confirm("This will replace the subject and message you have written. Continue?")) {
    return;
  }

  const btn = genEl("gen-btn");
  btn.disabled = true;
  btn.textContent = "Generating...";
  try {
    const mail = await api.post("/api/mail/generate", {
      kind: genEl("gen-kind").value,
      eventTitle: title,
      date: date,
      time: genEl("gen-time").value || null,
      venue: genEl("gen-venue").value.trim() || null,
      keyPoints: genEl("gen-points").value.trim() || null,
      recipientName: genEl("gen-recipient").value.trim() || null,
      tone: genEl("gen-tone").value,
      language: genEl("gen-language").value,
    });
    composeSubjectInput.value = mail.subject;
    composeBodyInput.value = mail.body;
    genState = { generated: true, subject: mail.subject, body: mail.body };
    genEl("gen-enhance").style.display = "flex";
    composeBodyInput.scrollIntoView({ block: "nearest", behavior: "smooth" });
  } catch (err) {
    errorEl.textContent = err.message;
  } finally {
    btn.disabled = false;
    btn.textContent = "✨ Generate mail";
  }
}

/**
 * "Improve / translate my own text": a small bar under a text box. The AI service rewrites what the user
 * typed (fixing mistakes, or changing the style) and can translate it; names and links are left alone.
 */
function buildAssistBar(getText, setText) {
  const bar = document.createElement("div");
  bar.className = "assist-bar";

  const label = document.createElement("span");
  label.className = "chip-label";
  label.textContent = "✨ Improve my own text:";

  const style = document.createElement("select");
  style.setAttribute("aria-label", "Style");
  fillSelect(style, [
    { value: "", label: "Keep my style (fix mistakes)" },
    { value: "FORMAL", label: "More formal" },
    { value: "FRIENDLY", label: "Friendlier" },
    { value: "CONCISE", label: "Shorter" },
    { value: "DETAILED", label: "More detailed" },
  ]);

  const lang = document.createElement("select");
  lang.setAttribute("aria-label", "Translate to");
  fillSelect(lang, [{ value: "", label: "Keep its language" }].concat(
    state.languages.map((l) => ({ value: l.value, label: "Translate to " + l.label }))));

  const go = document.createElement("button");
  go.type = "button";
  go.className = "btn btn-secondary btn-sm";
  go.textContent = "Improve / translate";

  const undo = document.createElement("button");
  undo.type = "button";
  undo.className = "btn btn-ghost btn-sm";
  undo.textContent = "↩ Undo";
  undo.style.display = "none";

  let previous = null;
  go.addEventListener("click", async () => {
    const text = getText();
    if (!text.trim()) { showToast("Type or generate some text first.", "error"); return; }
    if (!style.value && !lang.value) { showToast("Pick a style or a language first.", "error"); return; }
    go.disabled = true;
    go.textContent = "Working...";
    try {
      const res = await api.post("/api/mail/assist", {
        text: text,
        style: style.value || null,
        language: lang.value || null,
      });
      previous = text;
      setText(res.text);
      undo.style.display = "inline-flex";
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      go.disabled = false;
      go.textContent = "Improve / translate";
    }
  });
  undo.addEventListener("click", () => {
    if (previous === null) return;
    setText(previous);
    previous = null;
    undo.style.display = "none";
  });

  bar.append(label, style, lang, go, undo);
  return bar;
}

function wireMailGenerator() {
  genEl("gen-btn").addEventListener("click", runMailGenerator);
  genEl("gen-enhance").querySelectorAll(".chip").forEach((chip) => {
    chip.addEventListener("click", async () => {
      setGenTone(chip.dataset.tone);
      await runMailGenerator();
    });
  });
  genEl("gen-language").addEventListener("change", async () => {
    if (genState.generated) await runMailGenerator();
  });
}

init();
