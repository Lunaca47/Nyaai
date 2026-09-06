/**
 * NYAAI (न्यायAI) — Futuristic Web Application Core Engine
 * Standalone Client-Side AI Engine & Interactive Visuals
 * Pure UTF-8 (No BOM) | Version 2.2
 */

document.addEventListener("DOMContentLoaded", () => {
  initNeuralCanvas();
  initAITerminal();
  initDocumentScanner();
  initStatutoryCodex();
  initLanguageSwitcher();
});

/* =========================================================
   1. Dynamic Neural Constellation & Scales Canvas
   ========================================================= */
function initNeuralCanvas() {
  const canvas = document.getElementById("neuralCanvas");
  if (!canvas) return;
  const ctx = canvas.getContext("2d");

  let width = (canvas.width = window.innerWidth);
  let height = (canvas.height = window.innerHeight);

  window.addEventListener("resize", () => {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
  });

  const particles = [];
  const particleCount = Math.min(Math.floor(width / 24), 60);
  const maxDistance = 140;

  let mouse = { x: null, y: null, radius: 150 };
  window.addEventListener("mousemove", (e) => {
    mouse.x = e.clientX;
    mouse.y = e.clientY;
  });
  window.addEventListener("mouseout", () => {
    mouse.x = null;
    mouse.y = null;
  });

  for (let i = 0; i < particleCount; i++) {
    particles.push({
      x: Math.random() * width,
      y: Math.random() * height,
      vx: (Math.random() - 0.5) * 0.6,
      vy: (Math.random() - 0.5) * 0.6,
      radius: Math.random() * 2 + 1,
      color: Math.random() > 0.3 ? "#818CF8" : "#F59E0B"
    });
  }

  function render() {
    ctx.clearRect(0, 0, width, height);

    // Render connecting lines
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < maxDistance) {
          const alpha = 1 - dist / maxDistance;
          ctx.strokeStyle = `rgba(99, 102, 241, ${alpha * 0.12})`;
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.stroke();
        }
      }
    }

    // Render particles
    for (let p of particles) {
      if (mouse.x !== null && mouse.y !== null) {
        const dx = mouse.x - p.x;
        const dy = mouse.y - p.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < mouse.radius) {
          const force = (mouse.radius - dist) / mouse.radius;
          p.x -= (dx / dist) * force * 2;
          p.y -= (dy / dist) * force * 2;
        }
      }

      p.x += p.vx;
      p.y += p.vy;

      if (p.x < 0 || p.x > width) p.vx *= -1;
      if (p.y < 0 || p.y > height) p.vy *= -1;

      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.fill();
    }

    requestAnimationFrame(render);
  }

  render();
}

/* =========================================================
   2. Intelligent Legal AI Assistant Simulator (Terminal)
   ========================================================= */
const legalKnowledgeBase = [
  {
    keywords: ["bail", "482", "bnss", "arrest", "custody", "anticipatory"],
    title: "Bail Provisions under Section 482 of BNSS, 2023",
    confidence: "98.7% High Confidence",
    act: "Bharatiya Nagarik Suraksha Sanhita, 2023 (Act No. 46 of 2023)",
    section: "Section 482 (Bail in Non-Bailable Offenses)",
    summary: [
      "Empowers Sessions Courts and High Courts to grant bail in non-bailable offences subject to judicial discretion.",
      "Mandatory considerations: gravity of offence, character of accused, reasonable apprehension of tampering with witnesses or evidence.",
      "Conditions can be imposed preventing the applicant from leaving the jurisdiction or committing similar offences while on bail."
    ]
  },
  {
    keywords: ["article 21", "privacy", "liberty", "life", "fundamental right"],
    title: "Right to Life & Personal Liberty under Article 21",
    confidence: "99.4% High Confidence",
    act: "Constitution of India, 1950 (Part III)",
    section: "Article 21 (Protection of Life & Personal Liberty)",
    summary: [
      "No person shall be deprived of his life or personal liberty except according to procedure established by law.",
      "Encompasses the fundamental Right to Privacy (K.S. Puttaswamy v. Union of India, 2017).",
      "Guarantees right to speedy trial, legal aid, clean environment, and human dignity under constitutional jurisprudence."
    ]
  },
  {
    keywords: ["murder", "103", "bns", "homicide", "punishment", "lynching"],
    title: "Punishment for Murder under Section 103 of BNS, 2023",
    confidence: "99.1% High Confidence",
    act: "Bharatiya Nyaya Sanhita, 2023 (Act No. 45 of 2023)",
    section: "Section 103 (Replaces IPC Section 302)",
    summary: [
      "Whoever commits murder shall be punished with death or imprisonment for life, and shall also be liable to fine.",
      "Section 103(2) introduces strict penal provisions for mob lynching: murder committed by a group of five or more persons on grounds of race, caste, or religion is punishable with death or life imprisonment."
    ]
  },
  {
    keywords: ["evidence", "electronic", "61", "63", "bsa", "whatsapp", "email", "digital"],
    title: "Admissibility of Electronic Records under Section 61 & 63 of BSA, 2023",
    confidence: "97.5% High Confidence",
    act: "Bharatiya Sakshya Adhiniyam, 2023 (Act No. 47 of 2023)",
    section: "Sections 61 & 63 (Replaces IEA 65A & 65B)",
    summary: [
      "Electronic or digital records shall have the same legal effect, validity, and enforceability as paper documents.",
      "Section 63 outlines required certificate criteria for admissibility of digital data, emails, CCTV, server logs, and smartphone messages without requiring original server custody."
    ]
  },
  {
    keywords: ["consumer", "refund", "ecommerce", "return", "defective", "service"],
    title: "Consumer Rights & E-Commerce Refunds",
    confidence: "96.4% High Confidence",
    act: "Consumer Protection Act, 2019 & E-Commerce Rules, 2020",
    section: "Section 2(7) & Rule 5 (Unfair Trade Practices)",
    summary: [
      "Consumers have the statutory right to seek full refund or replacement for defective products or deficient services.",
      "E-commerce platforms are legally prohibited from levying arbitrary cancellation charges unless similar charges are borne by the platform.",
      "Complaints can be filed digitally via the National Consumer Helpline (NCH) or e-Daakhil portal."
    ]
  }
];

function initAITerminal() {
  const chatHistory = document.getElementById("chatHistory");
  const terminalInput = document.getElementById("terminalInput");
  const sendBtn = document.getElementById("sendBtn");
  const micBtn = document.getElementById("micBtn");
  const suggestionChips = document.querySelectorAll(".suggestion-chip");

  if (!chatHistory || !terminalInput || !sendBtn) return;

  function executeQuery(queryText) {
    if (!queryText || queryText.trim() === "") return;

    // 1. Append User Message
    appendMessage(queryText, "user");
    terminalInput.value = "";

    // 2. Append Typing Indicator
    const typingElem = document.createElement("div");
    typingElem.className = "chat-bubble chat-ai mono";
    typingElem.innerHTML = `<span style="color: #4F46E5; font-weight: 600;">⚡ Scanning 1,838 statutory sections across BNS, BNSS, BSA, COI...</span>`;
    chatHistory.appendChild(typingElem);
    chatHistory.scrollTop = chatHistory.scrollHeight;

    setTimeout(() => {
      typingElem.remove();

      // Find best match in knowledge base
      const q = queryText.toLowerCase();
      let matched = legalKnowledgeBase.find(item =>
        item.keywords.some(k => q.includes(k))
      );

      if (!matched) {
        matched = {
          title: "Statutory Legal Assessment",
          confidence: "94.2% AI Contextual Match",
          act: "Statutory Law of India & Constitution of India",
          section: "Relevant Legal Provisions",
          summary: [
            `Analysis generated for legal inquiry: "${queryText}".`,
            "Rights and liabilities under Indian jurisprudence depend on verified factual circumstances and police documentation.",
            "You can cross-reference relevant codified provisions in the Statutory Codex page."
          ]
        };
      }

      const botMsg = document.createElement("div");
      botMsg.className = "chat-bubble chat-ai";
      botMsg.innerHTML = `
        <div class="badge-pastel-sage" style="margin-bottom: 8px; font-size: 0.74rem;">
          <span>●</span> ${matched.confidence}
        </div>
        <div style="font-weight: 700; font-size: 1.05rem; margin-bottom: 6px; color: #0F172A;">
          ${matched.title}
        </div>
        <ul style="padding-left: 20px; margin-bottom: 10px; color: #334155; line-height: 1.6;">
          ${matched.summary.map(s => `<li>${s}</li>`).join("")}
        </ul>
        <div class="citation-pastel-box">
          <strong style="color: #92400E;">Statute Citation:</strong> 
          <strong>${matched.act}</strong> — <span style="color: #3730A3; font-weight: 600;">${matched.section}</span>
        </div>
        <div style="margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap;">
          <button class="chip-btn read-aloud-btn" style="padding: 4px 10px; font-size: 0.76rem;">
            🔊 Read Aloud
          </button>
          <button class="chip-btn copy-msg-btn" style="padding: 4px 10px; font-size: 0.76rem;">
            📋 Copy Citation
          </button>
        </div>
      `;

      // Read aloud click
      botMsg.querySelector(".read-aloud-btn").addEventListener("click", () => {
        const textToSpeak = `${matched.title}. ${matched.summary.join(". ")}. Statute Citation: ${matched.act}, ${matched.section}`;
        speakText(textToSpeak);
      });

      // Copy click
      botMsg.querySelector(".copy-msg-btn").addEventListener("click", (e) => {
        navigator.clipboard.writeText(`${matched.title}\n${matched.act} - ${matched.section}`);
        e.target.textContent = "✓ Copied!";
        setTimeout(() => (e.target.textContent = "📋 Copy Citation"), 2000);
      });

      chatHistory.appendChild(botMsg);
      chatHistory.scrollTop = chatHistory.scrollHeight;
    }, 550);
  }

  function appendMessage(text, sender) {
    const bubble = document.createElement("div");
    bubble.className = `chat-bubble chat-${sender}`;
    bubble.textContent = text;
    chatHistory.appendChild(bubble);
    chatHistory.scrollTop = chatHistory.scrollHeight;
  }

  sendBtn.addEventListener("click", () => executeQuery(terminalInput.value));
  terminalInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") executeQuery(terminalInput.value);
  });

  suggestionChips.forEach(chip => {
    chip.addEventListener("click", () => {
      const q = chip.getAttribute("data-query");
      executeQuery(q);
    });
  });

  // Speech Recognition (Microphone)
  if (micBtn && ("webkitSpeechRecognition" in window || "SpeechRecognition" in window)) {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.interimResults = false;

    micBtn.addEventListener("click", () => {
      try {
        recognition.start();
        micBtn.style.background = "#FEE2E2";
        micBtn.style.borderColor = "#F87171";
      } catch (err) {
        recognition.stop();
      }
    });

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      terminalInput.value = transcript;
      micBtn.style.background = "";
      micBtn.style.borderColor = "";
      executeQuery(transcript);
    };

    recognition.onerror = () => {
      micBtn.style.background = "";
      micBtn.style.borderColor = "";
    };

    recognition.onend = () => {
      micBtn.style.background = "";
      micBtn.style.borderColor = "";
    };
  } else if (micBtn) {
    micBtn.title = "Voice recognition not supported in this browser.";
  }
}

function speakText(text) {
  if (!("speechSynthesis" in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.rate = 1.0;
  utterance.pitch = 1.0;
  window.speechSynthesis.speak(utterance);
}

/* =========================================================
   3. Interactive Document OCR Scanner Simulator
   ========================================================= */
const sampleDocuments = {
  notice138: {
    title: "Legal Notice: Dishonour of Cheque",
    text: `LEGAL DEMAND NOTICE
To: M/s Apex Tech Solutions, Hyderabad
Under instructions from our client Mr. Rajesh Varma, we issue this notice under SECTION 138 OF NEGOTIABLE INSTRUMENTS ACT, 1881.
Cheque No. 440912 dated 14/08/2026 drawn on HDFC Bank for an amount of ₹3,50,000/- was returned unpaid with remarks "FUNDS INSUFFICIENT".
You are hereby called upon to pay the said sum within 15 DAYS of receipt of this notice, failing which our client shall initiate criminal prosecution under SECTION 138 & 142 of the NI Act.`,
    analysis: {
      type: "Statutory Demand Notice (Cheque Dishonour)",
      statutes: "Section 138 & 142, Negotiable Instruments Act, 1881",
      deadline: "15 Days from notice receipt to tender payment",
      action: "Reply within 15 days or clear payment; cause of action arises on 16th day to file complaint before Magistrate."
    }
  },
  firReport: {
    title: "First Information Report (FIR Extract)",
    text: `POLICE DEPARTMENT — CYBER CRIME PS
FIR No: 0412/2026 | Date: 02/09/2026
Under Sections: SECTION 318(4) BNS (Cheating) & SECTION 66D IT ACT (Cheating by Impersonation).
Complainant alleges unauthorized deduction of ₹85,000 via fraudulent phishing URL.
Investigating Officer: Sub-Inspector K. Sharma, Cyber Crime Cell.
Notice under SECTION 35(3) OF BNSS 2023 issued to accused regarding appearance for investigation.`,
    analysis: {
      type: "First Information Report (Cognizable Offence)",
      statutes: "Section 318(4) BNS 2023 & Section 66D Information Technology Act",
      deadline: "Immediate compliance with Notice u/s 35(3) BNSS",
      action: "Accused entitled to legal representation during interrogation; apply for anticipatory bail if arrest is reasonably apprehended."
    }
  },
  ndaBreach: {
    title: "Non-Disclosure Agreement (Breach Notice)",
    text: `CEASE AND DESIST NOTICE
Re: Breach of Confidentiality obligations under Master Services Agreement dated 12/01/2025.
Clause 7 prohibits unauthorized disclosure of Proprietary Source Code and Client Trade Secrets.
Such breach constitutes actionable civil wrong and criminal breach of trust under SECTION 316 OF BNS 2023.
Demand to immediately cease dissemination and return confidential assets within 7 DAYS.`,
    analysis: {
      type: "Commercial Cease & Desist / Breach Notice",
      statutes: "Section 316 BNS 2023 (Criminal Breach of Trust) & Indian Contract Act, 1872",
      deadline: "7 Days to comply or face injunction suit",
      action: "Review NDA terms with an advocate; formulate point-by-point rebuttal to contest liquidated damages."
    }
  }
};

function initDocumentScanner() {
  const laser = document.getElementById("scannerLaser");
  const docPreview = document.getElementById("docPreview");
  const scanBtn = document.getElementById("scanDocBtn");
  const tabs = document.querySelectorAll(".sample-tab");

  const repType = document.getElementById("repType");
  const repStatutes = document.getElementById("repStatutes");
  const repDeadline = document.getElementById("repDeadline");
  const repAction = document.getElementById("repAction");

  if (!docPreview || !scanBtn) return;

  let currentKey = "notice138";

  function loadSample(key) {
    currentKey = key;
    const doc = sampleDocuments[key];
    docPreview.textContent = doc.text;

    if (repType) repType.textContent = "Click 'Analyze Document' to scan...";
    if (repStatutes) repStatutes.textContent = "—";
    if (repDeadline) repDeadline.textContent = "—";
    if (repAction) repAction.textContent = "—";
  }

  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      tabs.forEach(t => t.classList.remove("active"));
      tab.classList.add("active");
      loadSample(tab.getAttribute("data-sample"));
    });
  });

  scanBtn.addEventListener("click", () => {
    if (laser) laser.classList.add("active");
    scanBtn.textContent = "⚡ Scanning Document...";
    scanBtn.disabled = true;

    setTimeout(() => {
      if (laser) laser.classList.remove("active");
      scanBtn.textContent = "✓ Scan Complete — Re-Analyze";
      scanBtn.disabled = false;

      const doc = sampleDocuments[currentKey];
      if (repType) repType.textContent = doc.analysis.type;
      if (repStatutes) repStatutes.textContent = doc.analysis.statutes;
      if (repDeadline) repDeadline.textContent = doc.analysis.deadline;
      if (repAction) repAction.textContent = doc.analysis.action;
    }, 1800);
  });

  loadSample("notice138");
}

/* =========================================================
   4. Statutory Codex Filter
   ========================================================= */
const statutoryLibrary = [
  {
    act: "BNS 2023",
    fullAct: "Bharatiya Nyaya Sanhita, 2023",
    section: "Section 103",
    title: "Punishment for Murder",
    desc: "Prescribes capital punishment or life imprisonment for murder. Section 103(2) introduces stringent penalties for mob lynching.",
    badge: "Criminal Law"
  },
  {
    act: "BNSS 2023",
    fullAct: "Bharatiya Nagarik Suraksha Sanhita, 2023",
    section: "Section 482",
    title: "Bail in Non-Bailable Offences",
    desc: "Comprehensive judicial discretion guidelines for bail, conditions of release, and preservation of personal liberty.",
    badge: "Criminal Procedure"
  },
  {
    act: "BSA 2023",
    fullAct: "Bharatiya Sakshya Adhiniyam, 2023",
    section: "Section 61 & 63",
    title: "Admissibility of Electronic Records",
    desc: "Digital contracts, emails, server logs, CCTV, and messaging trails recognized on equal footing with paper documents.",
    badge: "Evidence Law"
  },
  {
    act: "COI 1950",
    fullAct: "Constitution of India, 1950",
    section: "Article 21",
    title: "Right to Life & Personal Liberty",
    desc: "Guarantees fundamental rights, right to privacy, legal representation, and dignity under constitutional umbrella.",
    badge: "Constitutional Law"
  },
  {
    act: "BNS 2023",
    fullAct: "Bharatiya Nyaya Sanhita, 2023",
    section: "Section 316 & 318",
    title: "Criminal Breach of Trust & Cheating",
    desc: "Defines misappropriation of entrusted property, breach of fiduciary duty, and fraudulent inducement with enhanced penalties.",
    badge: "Criminal Law"
  },
  {
    act: "BNSS 2023",
    fullAct: "Bharatiya Nagarik Suraksha Sanhita, 2023",
    section: "Section 173",
    title: "Information to the Police (FIR)",
    desc: "Enables Zero FIR anywhere across India regardless of police station boundaries and mandates electronic record keeping.",
    badge: "Criminal Procedure"
  }
];

function initStatutoryCodex() {
  const codexGrid = document.getElementById("codexGrid");
  const searchInput = document.getElementById("codexSearch");

  if (!codexGrid || !searchInput) return;

  function renderCodex(items) {
    codexGrid.innerHTML = "";
    if (items.length === 0) {
      codexGrid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: #64748B; padding: 40px;">No statutory sections matching your query.</div>`;
      return;
    }

    items.forEach(item => {
      const card = document.createElement("div");
      card.className = "codex-card";
      card.innerHTML = `
        <div>
          <div class="codex-badge">${item.badge}</div>
          <div class="codex-act-title">${item.section}: ${item.title}</div>
          <div class="codex-act-desc">${item.desc}</div>
        </div>
        <div class="codex-meta">
          <span>${item.act}</span>
          <span style="color: #D97706; font-weight: 600;">Indexed</span>
        </div>
      `;
      codexGrid.appendChild(card);
    });
  }

  searchInput.addEventListener("input", (e) => {
    const term = e.target.value.toLowerCase().trim();
    if (!term) {
      renderCodex(statutoryLibrary);
      return;
    }
    const filtered = statutoryLibrary.filter(item =>
      item.act.toLowerCase().includes(term) ||
      item.section.toLowerCase().includes(term) ||
      item.title.toLowerCase().includes(term) ||
      item.desc.toLowerCase().includes(term)
    );
    renderCodex(filtered);
  });

  renderCodex(statutoryLibrary);
}

/* =========================================================
   5. Multilingual Localization
   ========================================================= */
const i18n = {
  en: {
    heroTitle: "The Future of Indian Jurisprudence, Powered by AI.",
    heroSubtitle: "Instant statutory retrieval, on-device legal OCR, and intelligent criminal code synthesis under BNS 2023, BNSS 2023, BSA 2023, and the Constitution of India."
  },
  hi: {
    heroTitle: "भारतीय न्यायशास्त्र का भविष्य, AI द्वारा संचालित।",
    heroSubtitle: "BNS 2023, BNSS 2023, BSA 2023 और भारत के संविधान के तहत तुरंत कानूनी खोज, ऑन-डिवाइस दस्तावेज़ OCR और सटीक कानूनी मार्गदर्शन।"
  }
};

function initLanguageSwitcher() {
  const langSelect = document.getElementById("langSelect");
  if (!langSelect) return;

  langSelect.addEventListener("change", (e) => {
    const lang = e.target.value;
    const heroTitle = document.querySelector(".hero-title");
    const heroSubtitle = document.querySelector(".hero-subtitle");

    if (i18n[lang]) {
      if (heroTitle && i18n[lang].heroTitle) heroTitle.textContent = i18n[lang].heroTitle;
      if (heroSubtitle && i18n[lang].heroSubtitle) heroSubtitle.textContent = i18n[lang].heroSubtitle;
    }
  });
}
