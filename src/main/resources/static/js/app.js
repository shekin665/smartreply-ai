SmartReplyTheme.initTheme();

const FOLDERS = ["INBOX", "SENT", "DRAFTS", "SPAM", "ARCHIVE", "TRASH"];
const TONES = ["PROFESSIONAL", "FRIENDLY", "FORMAL", "CONCISE", "APOLOGETIC"];

const state = {
  folder: "INBOX",
  search: "",
  emails: [],
  selectedEmailId: null,
  selectedEmail: null,
  currentReply: null,
  selectedTone: "PROFESSIONAL",
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
  await loadPriorityKeywords();
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
  title.textContent = "Simulated AI Reply Generator";
  panel.appendChild(title);

  const toneRow = document.createElement("div");
  toneRow.className = "tone-row";
  TONES.forEach((tone) => {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "tone-btn" + (tone === state.selectedTone ? " active" : "");
    btn.textContent = tone.charAt(0) + tone.slice(1).toLowerCase();
    btn.addEventListener("click", () => {
      state.selectedTone = tone;
      toneRow.querySelectorAll(".tone-btn").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
    });
    toneRow.appendChild(btn);
  });
  panel.appendChild(toneRow);

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
  panel.appendChild(replyActions);

  generateBtn.addEventListener("click", async () => {
    generateBtn.disabled = true;
    generateBtn.textContent = "Generating...";
    try {
      const reply = await api.post("/api/replies/generate", { emailId: email.id, tone: state.selectedTone });
      state.currentReply = reply;
      textarea.value = reply.content;
      textarea.style.display = "block";
      replyActions.style.display = "flex";
      await loadStats();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      generateBtn.disabled = false;
      generateBtn.textContent = "✨ Generate Reply";
    }
  });

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
    showToast(send ? "Reply sent (simulated - no real email is sent)." : "Reply saved as draft.", "success");
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
    showToast(send ? "Email sent (simulated - no real email is sent)." : "Draft saved.", "success");
  } catch (err) {
    showToast(err.message, "error");
  }
}

init();
