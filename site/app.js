(function () {
  // Hero: tone selector with sample drafts for the example email
  var drafts = {
    formal: "Dear Professor Meena,\n\nThank you for your message. I confirm that our team will be ready for the project review on Friday at 10 AM. Please let me know if any additional documents are required before the review.\n\nRegards,\nRam Kumar",
    friendly: "Hi Professor Meena,\n\nThanks for checking in! Yes, our team will be ready for the project review on Friday at 10 AM. Just tell me if you'd like anything extra from us beforehand.\n\nBest wishes,\nRam Kumar",
    concise: "Dear Professor Meena,\n\nConfirmed. Our team will be ready on Friday at 10 AM.\n\nRegards,\nRam Kumar"
  };
  var draft = document.getElementById("draft");
  var tones = document.querySelectorAll(".tones button");
  function setTone(t) {
    tones.forEach(function (b) { b.setAttribute("aria-selected", String(b.dataset.tone === t)); });
    draft.textContent = drafts[t];
  }
  tones.forEach(function (b) { b.addEventListener("click", function () { setTone(b.dataset.tone); }); });
  setTone("formal");

  // Screens viewer
  var captions = {
    home: ["Smart Reply home page with a headline, a sample email and its drafted reply", "The home page explains the service in three steps and leads to sign-in."],
    login: ["Login page with an email and password form", "The sign-in page as designed in the report."],
    dashboard: ["Dashboard with usage statistics and a list of recent messages", "The dashboard shows usage figures and recent mail. Newsletters and no-reply senders are marked not suitable."],
    composer: ["Reply composer with the original email on the left and an editable draft on the right", "The composer shows the original email beside the draft, with tone options, regenerate and send."],
    history: ["Reply history table with status labels", "History lists every generated, sent and discarded reply with its status."],
    settings: ["Settings page with default tone, signature and connected mailbox", "Settings hold the default tone, signature, language and connected mailbox."]
  };
  var shot = document.getElementById("shot");
  var cap = document.getElementById("cap");
  var tabs = document.querySelectorAll(".vtabs button");
  tabs.forEach(function (b) {
    b.addEventListener("click", function () {
      var k = b.dataset.img;
      tabs.forEach(function (x) { x.setAttribute("aria-selected", String(x === b)); });
      shot.src = "assets/" + k + ".jpg";
      shot.alt = captions[k][0];
      cap.textContent = captions[k][1];
    });
  });
})();
