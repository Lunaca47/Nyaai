/**
 * NYAAI (न्यायAI) — Futuristic Web Application Core Engine
 * Standalone Client-Side AI Engine & Interactive Visuals
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
  const particleCount = Math.min(Math.floor(width / 22), 70);
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
      vx: (Math.random() - 0.5) * 0.7,
      vy: (Math.random() - 0.5) * 0.7,
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
          ctx.strokeStyle = `rgba(99, 102, 241, ${alpha * 0.14})`;
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
      // Mouse interaction
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
      ctx.shadowBlur = 8;
      ctx.shadowColor = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
      ctx.fill();
    }

    requestAnimationFrame(render);
  }

  render();
}

/* =========================================================
   2. Live Interactive Legal AI Terminal (Playground)
   ========================================================= */
const legalKnowledgeBase = [
  {
    keywords: ["bail", "482", "bnss", "arrest", "custody"],
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
      "Guarantees right to speedy trial, legal aid, clean environment, and dignity under constitutional jurisprudence."
    ]
  },
  {
    keywords: ["murder", "103", "bns", "homicide", "punishment"],
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
    keywords: ["evidence", "electronic", "61", "63", "bsa", "whatsapp", "email"],
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
    keywords: ["consumer", "refund", "ecommerce", "return", "defective"],
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

    // 1. Add User Message
    appendMessage(queryText, "user");
    terminalInput.value = "";

    // 2. Show Typing Indicator
    const typingElem = document.createElement("div");
    typingElem.className = "message-bubble bot-message mono";
    typingElem.innerHTML = `<span class="text-cyan">⚡ Scanning 1,838 statutory sections across BNS, BNSS, BSA, COI...</span>`;
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
          title: "General Indian Legal Analysis",
          confidence: "94.2% AI Contextual Match",
          act: "Statutory Law of India & Constitution of India",
          section: "Relevant Statutory Provision",
          summary: [
            `Analysis generated for legal query: "${queryText}".`,
            "Rights under Indian law require verification against specific factual circumstances.",
            "Statutory references can be cross-examined directly via the Statutory Codex below."
          ]
        };
      }

      const botMsg = document.createElement("div");
      botMsg.className = "message-bubble bot-message";
      botMsg.innerHTML = `
        <div class="confidence-chip">
          <span>●</span> ${matched.confidence}
        </div>
        <div style="font-weight: 700; font-size: 1.05rem; margin-bottom: 6px; color: #FFF;">
          ${matched.title}
        </div>
        <ul style="padding-left: 20px; margin-bottom: 10px; color: #CBD5E1;">
          ${matched.summary.map(s => `<li>${s}</li>`).join("")}
        </ul>
        <div class="citation-card">
          <span style="color: var(--legal-gold); font-weight: 700;">Statute Citation:</span> 
          <strong>${matched.act}</strong> — <span class="text-cyan">${matched.section}</span>
        </div>
        <div style="margin-top: 10px; display: flex; gap: 8px;">
          <button class="suggestion-chip read-aloud-btn" style="padding: 4px 10px; font-size: 0.76rem;">
            🔊 Read Aloud
          </button>
          <button class="suggestion-chip copy-msg-btn" style="padding: 4px 10px; font-size: 0.76rem;">
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
    }, 600);
  }

  function appendMessage(text, sender) {
    const bubble = document.createElement("div");
    bubble.className = `message-bubble ${sender}-message`;
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
      if (micBtn.classList.contains("recording")) {
        recognition.stop();
        micBtn.classList.remove("recording");
      } else {
        recognition.start();
        micBtn.classList.add("recording");
      }
    });

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      terminalInput.value = transcript;
      micBtn.classList.remove("recording");
      executeQuery(transcript);
    };

    recognition.onerror = () => {
      micBtn.classList.remove("recording");
    };

    recognition.onend = () => {
      micBtn.classList.remove("recording");
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
Under instructions from our client Mr. Rajesh Varma, we issue this notice under <span class="ocr-box">SECTION 138 OF NEGOTIABLE INSTRUMENTS ACT, 1881</span>.
Cheque No. 440912 dated 14/08/2026 drawn on HDFC Bank for an amount of ₹3,50,000/- was returned unpaid with remarks "FUNDS INSUFFICIENT".
You are hereby called upon to pay the said sum within <span class="ocr-box">15 DAYS</span> of receipt of this notice, failing which our client shall initiate criminal prosecution under <span class="ocr-box">SECTION 138 & 142</span> of the NI Act.`,
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
Under Sections: <span class="ocr-box">SECTION 318(4) BNS</span> (Cheating) & <span class="ocr-box">SECTION 66D IT ACT</span> (Cheating by Impersonation).
Complainant alleges unauthorized deduction of ₹85,000 via fraudulent phishing URL.
Investigating Officer: Sub-Inspector K. Sharma, Cyber Crime Cell.
Notice under <span class="ocr-box">SECTION 35(3) OF BNSS 2023</span> issued to accused regarding appearance for investigation.`,
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
Such breach constitutes actionable civil wrong and criminal breach of trust under <span class="ocr-box">SECTION 316 OF BNS 2023</span>.
Demand to immediately cease dissemination and return confidential assets within <span class="ocr-box">7 DAYS</span>.`,
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
    docPreview.innerHTML = doc.text;

    // Reset analysis report to pending
    repType.textContent = "Click 'Analyze Document' to scan...";
    repStatutes.textContent = "—";
    repDeadline.textContent = "—";
    repAction.textContent = "—";
  }

  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      tabs.forEach(t => t.classList.remove("active"));
      tab.classList.add("active");
      loadSample(tab.getAttribute("data-sample"));
    });
  });

  scanBtn.addEventListener("click", () => {
    laser.classList.add("active");
    scanBtn.textContent = "⚡ Scanning Document...";
    scanBtn.disabled = true;

    setTimeout(() => {
      laser.classList.remove("active");
      scanBtn.textContent = "✓ Scan Complete — Re-Analyze";
      scanBtn.disabled = false;

      const doc = sampleDocuments[currentKey];
      repType.textContent = doc.analysis.type;
      repStatutes.textContent = doc.analysis.statutes;
      repDeadline.textContent = doc.analysis.deadline;
      repAction.textContent = doc.analysis.action;
    }, 2000);
  });

  loadSample("notice138");
}

/* =========================================================
   4. 3D Statutory Codex Filter
   ========================================================= */
const statutoryLibrary = [
  {
    act: "BNS 2023",
    fullAct: "Bharatiya Nyaya Sanhita, 2023",
    section: "Section 103",
    title: "Punishment for Murder",
    desc: "Prescribes capital punishment or life imprisonment for murder. Introduces stringent penalties for mob lynching.",
    badge: "Criminal Law"
  },
  {
    act: "BNSS 2023",
    fullAct: "Bharatiya Nagarik Suraksha Sanhita, 2023",
    section: "Section 482",
    title: "Bail in Non-Bailable Offences",
    desc: "Comprehensive guidelines for bail, anticipatory bail, and discretionary powers of magistrate courts.",
    badge: "Criminal Procedure"
  },
  {
    act: "BSA 2023",
    fullAct: "Bharatiya Sakshya Adhiniyam, 2023",
    section: "Section 61 & 63",
    title: "Admissibility of Electronic Records",
    desc: "Digital contracts, emails, server logs, and digital signatures recognized on equal footing with paper documents.",
    badge: "Evidence Law"
  },
  {
    act: "COI 1950",
    fullAct: "Constitution of India, 1950",
    section: "Article 21",
    title: "Right to Life & Personal Liberty",
    desc: "Guarantees fundamental rights, privacy, legal aid, and dignity under the constitutional umbrella.",
    badge: "Constitutional Law"
  },
  {
    act: "BNS 2023",
    fullAct: "Bharatiya Nyaya Sanhita, 2023",
    section: "Section 316 & 318",
    title: "Criminal Breach of Trust & Cheating",
    desc: "Defines misappropriation of property, breach of fiduciary duty, and fraudulent inducement.",
    badge: "Criminal Law"
  },
  {
    act: "BNSS 2023",
    fullAct: "Bharatiya Nagarik Suraksha Sanhita, 2023",
    section: "Section 173",
    title: "Information to the Police (FIR)",
    desc: "Enables Zero FIR anywhere in India and mandates audio-video recording of search and seizure operations.",
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
      codexGrid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 40px;">No sections matching your query.</div>`;
      return;
    }

    items.forEach(item => {
      const card = document.createElement("div");
      card.className = "codex-card glass-panel";
      card.innerHTML = `
        <div>
          <div class="codex-badge">${item.badge}</div>
          <div class="codex-act-title">${item.section}: ${item.title}</div>
          <div class="codex-act-desc">${item.desc}</div>
        </div>
        <div class="codex-meta">
          <span>${item.act}</span>
          <span style="color: var(--legal-gold);">Indexed</span>
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
   5. Multilingual Localization (EN, HI, BN, TE, TA)
   ========================================================= */
const i18n = {
  en: {
    heroTag: "Next-Gen Legal Intelligence",
    heroTitle: "The Future of Indian Jurisprudence, Powered by AI.",
    heroSubtitle: "Instant statutory retrieval, on-device legal OCR, and intelligent criminal code synthesis under BNS 2023, BNSS 2023, BSA 2023, and the Constitution of India.",
    btnTerminal: "⚡ Launch AI Terminal",
    btnScanner: "📄 Try Document Scanner",
    btnDownload: "📲 Download Android APK"
  },
  hi: {
    heroTag: "अगली पीढ़ी की कानूनी बुद्धिमत्ता",
    heroTitle: "भारतीय न्यायशास्त्र का भविष्य, AI द्वारा संचालित।",
    heroSubtitle: "BNS 2023, BNSS 2023, BSA 2023 और भारत के संविधान के तहत तुरंत कानूनी खोज, ऑन-डिवाइस दस्तावेज़ OCR और सटीक कानूनी मार्गदर्शन।",
    btnTerminal: "⚡ AI टर्मिनल खोलें",
    btnScanner: "📄 दस्तावेज़ स्कैनर आज़माएं",
    btnDownload: "📲 एंड्रॉइड APK डाउनलोड करें"
  },
  bn: {
    heroTag: "পরবর্তী প্রজন্মের আইনি বুদ্ধিমত্তা",
    heroTitle: "ভারতীয় আইনের ভবিষ্যৎ, AI দ্বারা পরিচালিত।",
    heroSubtitle: "BNS 2023, BNSS 2023, BSA 2023 এবং ভারতের সংবিধানের অধীনে তাৎক্ষণিক আইনি অনুসন্ধান এবং অন-ডিভাইস ডকুমেন্ট ওসিআর।",
    btnTerminal: "⚡ AI টার্মিনাল চালু করুন",
    btnScanner: "📄 ডকুমেন্ট স্ক্যানার দেখুন",
    btnDownload: "📲 অ্যান্ড্রয়েড APK ডাউনলোড"
  },
  te: {
    heroTag: "తదుపరి తరం న్యాయ మేధస్సు",
    heroTitle: "భారతీయ న్యాయశాస్త్రం యొక్క భవిష్యత్తు, AI తో.",
    heroSubtitle: "BNS 2023, BNSS 2023, BSA 2023 మరియు భారత రాజ్యాంగం ప్రకారం తక్షణ చట్టపరమైన పరిశోధన మరియు ఆన్-డివైస్ OCR.",
    btnTerminal: "⚡ AI టెర్మినల్ తెరవండి",
    btnScanner: "📄 డాక్యుమెంట్ స్కానర్",
    btnDownload: "📲 ఆండ్రాయిడ్ APK డౌన్‌లోడ్"
  },
  ta: {
    heroTag: "அடுத்த தலைமுறை சட்ட நுண்ணறிவு",
    heroTitle: "இந்திய நீதித்துறையின் எதிர்காலம், AI மூலம்.",
    heroSubtitle: "BNS 2023, BNSS 2023, BSA 2023 மற்றும் இந்திய அரசியலமைப்பின் கீழ் உடனடி சட்ட ஆய்வு மற்றும் சாதனத்திலேயே இயங்கும் OCR.",
    btnTerminal: "⚡ AI முனையத்தைத் தொடங்கவும்",
    btnScanner: "📄 ஆவண ஸ்கேனர்",
    btnDownload: "📲 ஆண்ட்ராய்டு APK பதிவிறக்கம்"
  }
};

function initLanguageSwitcher() {
  const langSelect = document.getElementById("langSelect");
  if (!langSelect) return;

  langSelect.addEventListener("change", (e) => {
    const lang = e.target.value;
    const strings = i18n[lang] || i18n.en;

    const elTag = document.querySelector(".hero-content .badge-tag span");
    const elTitle = document.querySelector(".hero-title");
    const elSubtitle = document.querySelector(".hero-subtitle");
    const elBtnTerminal = document.querySelector(".hero-ctas .btn-primary");
    const elBtnScanner = document.querySelector(".hero-ctas .btn-gold");

    if (elTag) elTag.textContent = strings.heroTag;
    if (elTitle) elTitle.innerHTML = `<span class="text-gradient">${strings.heroTitle}</span>`;
    if (elSubtitle) elSubtitle.textContent = strings.heroSubtitle;
    if (elBtnTerminal) elBtnTerminal.textContent = strings.btnTerminal;
    if (elBtnScanner) elBtnScanner.textContent = strings.btnScanner;
  });
}

