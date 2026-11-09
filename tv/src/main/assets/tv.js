/*
 * Ophelia on Fire TV: remote-control play.
 *
 * The TV app loads the same game page as the phone app, then runs this script on top of it.
 * Nothing here changes how the game plays; it adds a highlight you move with the arrows,
 * OK to select, the media buttons as shortcuts, TV-friendly margins, and wording for a remote.
 *
 *   Arrows      move the highlight between your card, the playable line cards and the buttons
 *   OK          select: on your card it lays it down (or sends a King to the pile);
 *               on a line card it picks it, then OK on your card (or another card) pairs them
 *   Play/Pause  lay down your card, send a King to the pile, or finish the game
 *   Rewind      undo
 *   Menu        show or hide the rules
 *   Back        close a screen, or unpick a card
 */
(function () {
  if (window.__opheliaTv) return;
  window.__opheliaTv = true;
  var doc = document;
  doc.documentElement.classList.add("tv");
  var byId = function (id) { return doc.getElementById(id); };

  // ---------- Look: TV-safe margins, a clear highlight, no repaint-heavy pulsing ----------
  var css = [
    // TVs can crop the picture edges, so keep everything inside a safe margin.
    "html.tv body { padding: 5vh 5vw 5vh 5vw !important; }",
    // Fallbacks for older TV browser engines that lack the 'inset' shorthand.
    "html.tv #intro, html.tv .overlay, html.tv .solveskip, html.tv .card .mid, html.tv #intro .ic b { top: 0; right: 0; bottom: 0; left: 0; }",
    "html.tv .icon-btn::after { top: -7px; right: -7px; bottom: -7px; left: -7px; }",
    // The highlight: a thick white ring, and cards lift slightly.
    "html.tv button:focus { outline: 4px solid #ffffff; outline-offset: 3px; }",
    "html.tv .card:focus { outline: none; box-shadow: 0 0 0 5px #ffffff, 0 0 22px 6px rgba(255,255,255,.45) !important; transform: translateY(-4px) scale(1.07); z-index: 3; }",
    "html.tv .card.sel:focus { box-shadow: 0 0 0 5px #ffffff, 0 0 0 10px var(--sel) !important; }",
    "html.tv .toggle input:focus + .sw, html.tv .seg input:focus + span { outline: 4px solid #ffffff; outline-offset: 3px; }",
    // Pulsing glows repaint every frame, which is slow on TV sticks; keep them steady instead.
    "html.tv .glow, html.tv .pairglow, html.tv .stat.bump, html.tv .card.target { animation: none !important; }",
    // A TV doesn't turn, and nothing here is tapped.
    "html.tv .screenrow { display: none !important; }",
    "html.tv .msg { cursor: default; }",
    "html.tv .tvkeys { margin: 10px 0 0; color: var(--muted); font-size: .85rem; }",
  ].join("\n");
  var style = doc.createElement("style");
  style.textContent = css;
  doc.head.appendChild(style);

  // ---------- Words for a remote instead of a touch screen ----------
  var swaps = [
    [" Tap it to lay it down, or drag it onto a match.", " Press OK to lay it down, or pick its match first."],
    ["Tap it to send it to the pile.", "Press OK to send it to the pile."],
    ["picked. Tap your card or another card that makes 13 or matches it.", "picked. Now pick your card, or another card that makes 13 or matches it."],
    ["tap Finish game", "select Finish game"],
    [", then tap it to lay it down", ", then press OK to lay it down"],
    ["Tap anywhere for a new game", "Press OK for a new game"],
  ];
  function reword(el) {
    if (!el) return;
    var t = el.textContent, n = t;
    swaps.forEach(function (s) { n = n.split(s[0]).join(s[1]); });
    if (n !== t) el.textContent = n;
  }
  var msg = byId("msg");
  reword(msg);
  new MutationObserver(function () { reword(msg); }).observe(msg, { childList: true, characterData: true, subtree: true });
  doc.querySelectorAll(".tapnew").forEach(reword);
  // The stack in the middle of the line says "tap to show"; it's redrawn each turn.
  var lineBox = byId("line");
  new MutationObserver(function () {
    lineBox.querySelectorAll(".stack small").forEach(function (s) { if (s.textContent !== "select to show") s.textContent = "select to show"; });
  }).observe(lineBox, { childList: true, subtree: true });

  var helpList = doc.querySelector("#help ul");
  if (helpList) {
    helpList.innerHTML = [
      "Ace 1, number cards face value, Jack 11, Queen 12, King 13.",
      "A new card turns over by itself. A King is 13 alone: press OK on it to send it to the pile.",
      "If your card makes 13 with, or matches, the top or bottom card of the line, select that line card, then your card. Both go to the pile.",
      "If it fits nowhere, press OK on your card. It goes at the bottom of the line.",
      "The top and bottom cards together, or the two cards side by side at either end, also clear when they make 13 or a pair: select one, then the other.",
      "When a match is waiting, the cards glow green.",
      "The card count under the line shows every card: bright = still in the deck, gold outline = in the line, blue = your card, dot = on the pile.",
      "Win by emptying the line once the deck runs out. If matches are left at the ends, clear them, then select Finish game.",
      "<b>Remote:</b> arrows move the white highlight, OK selects, Play/Pause lays down your card, Rewind undoes, Menu shows these rules, Back closes a screen or unpicks a card.",
    ].map(function (t) { return "<li>" + t + "</li>"; }).join("");
  }

  // Version line in settings: add the TV browser engine, which helps if something looks wrong.
  var engine = (navigator.userAgent.match(/Chrome\/(\d+)/) || [])[1];
  doc.querySelectorAll("#settings .version .vtext").forEach(function (v) {
    v.textContent = (v.textContent ? v.textContent + " · " : "") + "Fire TV" + (engine ? " · Chrome " + engine : "");
  });

  // ---------- The highlight: which things can be selected, and moving between them ----------
  var OVERLAYS = ["settings", "help", "endScreen"];
  function openOverlay() {
    for (var i = 0; i < OVERLAYS.length; i++) { var o = byId(OVERLAYS[i]); if (o && !o.hidden) return o; }
    return null;
  }
  // A settings switch is a hidden checkbox; its visible part is the row around it.
  function shape(el) {
    if (el.tagName === "INPUT") {
      var v = el.type === "checkbox" ? el.closest(".toggle") : el.nextElementSibling;
      if (v) return v;
    }
    return el;
  }
  function rectOf(el) { return shape(el).getBoundingClientRect(); }
  function usable(el) {
    if (el.disabled) return false;
    var r = rectOf(el);
    if (r.width < 2 || r.height < 2) return false;
    return getComputedStyle(shape(el)).visibility !== "hidden";
  }
  function candidates() {
    var ov = openOverlay();
    var all = Array.prototype.slice.call((ov || doc).querySelectorAll("button, input"));
    return all.filter(function (el) { return (ov || !el.closest(".overlay, #intro")) && usable(el); });
  }
  function focusOn(el) { if (el) { try { el.focus({ preventScroll: true }); } catch (e) { el.focus(); } } }
  function defaultTarget() {
    var ov = openOverlay();
    if (ov) {
      var first = ov.id === "endScreen" ? byId("endNew") : ov.id === "help" ? byId("helpBack") : byId("optReal");
      if (first && usable(first)) return first;
      return candidates()[0] || null;
    }
    var picks = ['#handSlot button', '#line [data-key="0"]', "#newBtn"];
    for (var i = 0; i < picks.length; i++) { var el = doc.querySelector(picks[i]); if (el && usable(el)) return el; }
    return null;
  }
  function move(dir) {
    var list = candidates(), cur = doc.activeElement;
    if (list.indexOf(cur) < 0) { focusOn(defaultTarget()); return; }
    var a = rectOf(cur), ax = a.left + a.width / 2, ay = a.top + a.height / 2;
    // First look only at things really in that direction: within 45 degrees of the arrow.
    // (Before, the Undo button, a little right of your card but mostly below it, could win a
    // Right press over the line, depending on where the line's cards happened to sit.)
    // Only if nothing is there, look at anything on that side.
    function pick(cone) {
      var best = null, bestScore = Infinity;
      list.forEach(function (el) {
        if (el === cur) return;
        var r = rectOf(el), dx = r.left + r.width / 2 - ax, dy = r.top + r.height / 2 - ay;
        var ahead = dir === "left" ? -dx : dir === "right" ? dx : dir === "up" ? -dy : dy;
        var aside = dir === "left" || dir === "right" ? Math.abs(dy) : Math.abs(dx);
        if (ahead <= 4) return;
        if (cone && aside > ahead) return;
        var score = ahead + aside * 2.2;   // prefer things straight ahead over things off to the side
        if (score < bestScore) { bestScore = score; best = el; }
      });
      return best;
    }
    var best = pick(true) || pick(false);
    if (best) focusOn(best);
  }

  // The game redraws the cards after every move, which drops the highlight. Put it back on the
  // same card or button when it still exists, otherwise on your card.
  function identify(el) {
    if (!el || el === doc.body) return null;
    if (el.dataset && el.dataset.key !== undefined) return { key: el.dataset.key };
    if (el.classList && el.classList.contains("finish")) return { finish: true };
    if (el.classList && el.classList.contains("stack")) return { stack: true };
    if (el.id) return { id: el.id };
    return null;
  }
  function locate(id) {
    if (!id) return null;
    if (id.key !== undefined) return doc.querySelector('[data-key="' + id.key + '"]');
    if (id.finish) return doc.querySelector(".card.finish");
    if (id.stack) return doc.querySelector(".card.stack");
    return id.id ? byId(id.id) : null;
  }
  function settleFocus(id) {
    if (openOverlay()) return;
    var cur = doc.activeElement;
    if (cur && cur !== doc.body && doc.contains(cur) && usable(cur)) return;
    var el = locate(id);
    focusOn(el && usable(el) ? el : defaultTarget());
  }
  var gameRender = window.render;
  window.render = function () {
    var id = identify(doc.activeElement);
    gameRender.apply(this, arguments);
    settleFocus(id);
  };
  // After picking a line card, the next thing is almost always your card: move there.
  var gameTap = window.tap;
  window.tap = function (k) {
    gameTap.apply(this, arguments);
    if (k !== "hand" && sel.length === 1 && sel[0] === k) {
      var mine = doc.querySelector('#handSlot button[data-key="hand"]');
      if (mine) focusOn(mine);
    }
  };

  // Opening and closing a screen moves the highlight into it, and back to the table after.
  OVERLAYS.forEach(function (name) {
    var o = byId(name);
    if (!o) return;
    new MutationObserver(function () {
      if (!o.hidden) setTimeout(function () { focusOn(defaultTarget()); }, 30);
      else setTimeout(function () { if (!openOverlay()) settleFocus(null); }, 30);
    }).observe(o, { attributes: true, attributeFilter: ["hidden"] });
  });

  // Back also unpicks a picked card.
  var gameBack = window.handleBack;
  window.handleBack = function () {
    if (!(typeof solving !== "undefined" && solving) && !openOverlay() && sel.length) {
      sel = [];
      render();
      say("Nothing picked.");
      return true;
    }
    return gameBack ? gameBack() : false;
  };

  // ---------- Keys ----------
  var DIRS = { ArrowLeft: "left", ArrowRight: "right", ArrowUp: "up", ArrowDown: "down", 37: "left", 39: "right", 38: "up", 40: "down" };
  doc.addEventListener("keydown", function (e) {
    var ok = e.key === "Enter" || e.key === " " || e.keyCode === 13 || e.keyCode === 23;
    var dir = DIRS[e.key] || DIRS[e.keyCode];
    if (!ok && !dir) return;
    e.preventDefault();
    var intro = byId("intro");
    if (intro) { intro.dispatchEvent(new PointerEvent("pointerdown", { bubbles: true })); return; }
    if (typeof solving !== "undefined" && solving) { if (ok) solving.finish(); return; }
    if (dir) { move(dir); return; }
    var el = doc.activeElement;
    if (!el || el === doc.body || candidates().indexOf(el) < 0) { focusOn(defaultTarget()); return; }
    el.click();
  }, true);

  // Media buttons, passed in by the app.
  window.tvKey = function (name) {
    if (byId("intro")) return;
    if (typeof solving !== "undefined" && solving) { solving.finish(); return; }
    var ov = openOverlay();
    if (name === "menu") {
      if (ov && ov.id !== "help") return;
      byId("help").hidden = !byId("help").hidden;
    } else if (name === "rewind") {
      if (!ov) undo();
    } else if (name === "play") {
      if (over) { newGame(); return; }
      if (ov) return;
      if (hand) tap("hand");
      else if (!deck.length) finishGame();
    }
  };

  // Start with the highlight on your card once the opening has played.
  var waitIntro = setInterval(function () {
    if (byId("intro")) return;
    clearInterval(waitIntro);
    if (!doc.activeElement || doc.activeElement === doc.body) settleFocus(null);
  }, 100);
})();
