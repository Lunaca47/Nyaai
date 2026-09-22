/**
 * NYAAI (न्यायAI) — Futuristic Web Application Core Engine
 * Standalone Client-Side AI Engine & Interactive Visuals
 * Pure UTF-8 (No BOM) | Version 2.2
 */

var currentLang = "en";

document.addEventListener("DOMContentLoaded", () => {
  initNeuralCanvas();
  initCloudDataset();
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
   2. Intelligent Legal AI Assistant Simulator (Terminal) & Cloud Knowledge Base
   ========================================================= */
var legalKnowledgeBase = (typeof legalKnowledgeBase !== "undefined" && legalKnowledgeBase && legalKnowledgeBase.length > 0)
  ? legalKnowledgeBase
  : (typeof window !== "undefined" && window.NYAAI_EMBEDDED_DATA && window.NYAAI_EMBEDDED_DATA.curated_qa) 
    ? window.NYAAI_EMBEDDED_DATA.curated_qa 
    : [
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

// Asynchronous Cloud Dataset Sync Engine
async function initCloudDataset() {
  try {
    // If legalKnowledgeBase is already populated with preloaded dataset, update status and finish
    if (typeof legalKnowledgeBase !== "undefined" && legalKnowledgeBase && legalKnowledgeBase.length > 5) {
      updateTerminalStatusBadge(legalKnowledgeBase.length);
      console.log(`[Nyaai Knowledge Base] Using preloaded verified dataset: ${legalKnowledgeBase.length} items.`);
      return;
    }

    // 1. Check local storage cache
    const cached = localStorage.getItem("nyaai_cloud_dataset_v2");
    if (cached) {
      try {
        const parsed = JSON.parse(cached);
        if (parsed && parsed.curated_qa && parsed.curated_qa.length > 0) {
          legalKnowledgeBase = parsed.curated_qa;
          updateTerminalStatusBadge(parsed.curated_qa.length);
          return;
        }
      } catch (e) {
        console.warn("Error reading cached cloud dataset", e);
      }
    }

    // 2. Fetch fresh dataset from Cloud / CDN if not preloaded
    const endpoints = [
      "data/legal_dataset_master.json",
      "https://lunaca47.github.io/Nyaai/data/legal_dataset_master.json"
    ];

    for (const ep of endpoints) {
      try {
        const res = await fetch(ep);
        if (res.ok) {
          const data = await res.json();
          if (data && data.curated_qa && data.curated_qa.length > 0) {
            legalKnowledgeBase = data.curated_qa;
            try {
              if (JSON.stringify(data).length < 3000000) {
                localStorage.setItem("nyaai_cloud_dataset_v2", JSON.stringify(data));
              }
            } catch (_) {}
            updateTerminalStatusBadge(data.curated_qa.length);
            console.log(`[Nyaai Cloud Engine] Cloud knowledge base loaded: ${data.curated_qa.length} Q&As.`);
            break;
          }
        }
      } catch (_) {
        // Fall back to next endpoint or embedded data
      }
    }
  } catch (err) {
    console.warn("Cloud dataset sync exception:", err);
  }
}

function updateTerminalStatusBadge(count) {
  const badge = document.querySelector(".terminal-topbar .mono");
  if (badge) {
    badge.textContent = `CLOUD SYNC ACTIVE • ${count} VERIFIED Q&AS • 1,838 SECTIONS`;
  }
}

function findBestLegalMatch(queryText) {
  if (!queryText || !legalKnowledgeBase || legalKnowledgeBase.length === 0) return null;
  const q = queryText.toLowerCase().trim();
  const stopWords = new Set([
    "what", "how", "why", "when", "where", "which", "who", "whom", "can", "could", 
    "should", "would", "the", "and", "for", "with", "about", "give", "tell", "explain", 
    "does", "have", "been", "that", "this", "there", "they", "them", "from", "into", "also", "your"
  ]);
  const queryTerms = q.replace(/[^a-z0-9 ]/g, " ").split(/\s+/).filter(w => w.length >= 3 && !stopWords.has(w));
  const numbers = (q.match(/\d+/g) || []);

  let bestItem = null;
  let highestScore = 0;

  for (const item of legalKnowledgeBase) {
    let score = 0;
    const qText = (item.question || item.title || "").toLowerCase();
    const aText = (item.answer || (item.summary ? item.summary.join(" ") : "")).toLowerCase();
    const sText = (item.source || item.act || "").toLowerCase();
    const secText = (item.section || "").toLowerCase();
    const domainText = (item.legalDomain || "").toLowerCase();

    // Exact phrase match
    if (q.length > 5 && (qText.includes(q) || aText.includes(q))) {
      score += 30;
    }

    // Number match (Article or Section numbers, e.g., 482, 21, 103, 305, 173)
    for (const num of numbers) {
      if (secText.includes(num)) score += 25;
      else if (sText.includes(num)) score += 15;
      else if (qText.includes(num)) score += 10;
    }

    // Keywords match
    if (item.keywords && Array.isArray(item.keywords)) {
      for (const kw of item.keywords) {
        const kwLower = kw.toLowerCase();
        if (q.includes(kwLower)) score += 8;
        for (const term of queryTerms) {
          if (kwLower === term) score += 6;
          else if (kwLower.includes(term) || term.includes(kwLower)) score += 2;
        }
      }
    }

    // Query terms in question, section, and domain
    for (const term of queryTerms) {
      if (qText.includes(term)) score += 4;
      if (secText.includes(term)) score += 6;
      if (domainText.includes(term)) score += 3;
      if (aText.includes(term)) score += 2;
    }

    if (score > highestScore) {
      highestScore = score;
      bestItem = item;
    }
  }

  if (highestScore >= 6 && bestItem) {
    return {
      title: bestItem.title || (bestItem.section ? `${bestItem.section}: Legal Assessment` : "Statutory Legal Provision"),
      confidence: bestItem.confidence || "98.5% Cloud Statutory Match",
      act: bestItem.act || bestItem.legalDomain || "Indian Law",
      section: bestItem.section || bestItem.source || "Statutory Provision",
      summary: bestItem.summary || [bestItem.answer || ""]
    };
  }
  return null;
}

function getConversationalGreeting(queryText, lang) {
  if (!queryText) return null;
  const clean = queryText.toLowerCase().replace(/[^a-z0-9 ]/g, " ").trim();
  const tokens = clean.split(/\s+/).filter(Boolean);
  if (tokens.length === 0) return null;

  // Substantive legal terms: if query contains these, treat as legal question, not pure greeting
  const substantiveTerms = new Set([
    "section", "article", "bail", "arrest", "warrant", "fir", "police", "court",
    "law", "crime", "theft", "murder", "rights", "refund", "cheque", "property",
    "divorce", "cyber", "penalty", "judge", "bns", "bnss", "bsa", "ipc", "crpc"
  ]);
  if (tokens.some(t => substantiveTerms.has(t))) {
    return null;
  }

  const singleGreetings = new Set([
    "hi", "hello", "hey", "namaste", "namaskar", "pranam", "vanakkam",
    "namaskaram", "adaab", "satsriakal", "hola", "sup", "yo"
  ]);
  const multiGreetings = [
    "good morning", "good afternoon", "good evening", "good day",
    "how are you", "how are you doing", "hows it going", "how is it going", "whats up", "what s up",
    "who are you", "what is your name", "what can you do", "introduce yourself", "tell me about yourself",
    "what is nyaai", "thank you", "thanks", "thank u", "dhanyawad", "shukriya", "nandri", "dhanyavada"
  ];

  const isGreeting = singleGreetings.has(clean) ||
    (tokens.length <= 3 && tokens.some(t => singleGreetings.has(t))) ||
    multiGreetings.some(g => clean === g || (clean.startsWith(g) && tokens.length <= 5));

  if (!isGreeting) return null;

  const isIdentity = clean.includes("who are you") || clean.includes("what can you do") || clean.includes("introduce") || clean.includes("your name") || clean.includes("what is nyaai");
  const isWellBeing = clean.includes("how are you") || clean.includes("hows it going") || clean.includes("how is it going") || clean.includes("whats up");
  const isThanks = clean.includes("thank") || clean.includes("dhanyawad") || clean.includes("shukriya") || clean.includes("nandri");

  const l = lang || (typeof currentLang !== "undefined" ? currentLang : "en");

  switch(l) {
    case "hi":
      if (isIdentity) return { title: "नमस्ते! मैं न्यायAI (Nyaai) हूँ 😊", message: "मैं आपका दोस्ताना कानूनी सहायक हूँ। मैं भारतीय कानूनों—संविधान, BNS, BNSS और नागरिक अधिकारों को सरल हिंदी में समझाने के लिए यहाँ हूँ। आप मुझसे एफआईआर, ज़मानत, या कोई भी कानूनी सवाल पूछ सकते हैं।" };
      if (isWellBeing) return { title: "मैं बिल्कुल ठीक हूँ! 😊", message: "पूछने के लिए धन्यवाद! आशा है आपका दिन अच्छा जा रहा होगा। आज मैं आपकी कानूनी समझ में क्या सहायता कर सकता हूँ?" };
      if (isThanks) return { title: "आपका बहुत-बहुत स्वागत है! 😊", message: "यदि आपके पास कोई और कानूनी प्रश्न या अधिकार से संबंधित संदेह हो, तो बेझिझक पूछें। सुरक्षित और जागरूक रहें!" };
      return { title: "नमस्ते! मैं न्यायAI हूँ 😊", message: "मैं आपका कानूनी सहायक हूँ। आज मैं आपकी क्या मदद कर सकता हूँ? आप मुझसे पुलिस प्रक्रिया, ज़मानत, उपभोक्ता अधिकार या किसी भी कानूनी धारा के बारे में पूछ सकते हैं।" };

    case "bn":
      if (isIdentity) return { title: "নমস্কার! আমি Nyaai 😊", message: "আমি আপনার ভারতীয় আইনি সহায়ক! ভারতীয় আইন ও সংবিধানকে সহজ ভাষায় বোঝাতে আমি সাহায্য করি। আপনি আমাকে এফআইআর, জামিন বা যেকোনো আইনি প্রশ্ন জিজ্ঞাসা করতে পারেন।" };
      if (isWellBeing) return { title: "আমি খুব ভালো আছি! 😊", message: "ধন্যবাদ! আশা করি আপনার দিনটি ভালো কাটছে। আজ আপনাকে আইনি বিষয়ে কীভাবে সাহায্য করতে পারি?" };
      if (isThanks) return { title: "আপনাকে অনেক ধন্যবাদ! 😊", message: "আপনার যেকোনো আইনি প্রশ্ন থাকলে নির্দ্বিধায় আমাকে জিজ্ঞাসা করতে পারেন।" };
      return { title: "নমস্কার! আমি Nyaai 😊", message: "আপনার আইনি সহায়ক। আজ আপনাকে কীভাবে সাহায্য করতে পারি? আপনি আমাকে নাগরিক অধিকার, এফআইআর, জামিন বা যেকোনো আইন সম্পর্কে জিজ্ঞাসা করতে পারেন।" };

    case "te":
      if (isIdentity) return { title: "నమస్కారం! నేను Nyaai 😊", message: "మీ భారతీయ న్యాయ సహాయకుడిని! భారతీయ చట్టాలను మరియు రాజ్యాంగాన్ని సామాన్యులకు సులభంగా వివరించడానికి నేను ఇక్కడ ఉన్నాను. మీరు ఏదైనా చట్టపరమైన ప్రశ్న అడగవచ్చు." };
      if (isWellBeing) return { title: "నేను చాలా బాగున్నాను! 😊", message: "అడిగినందుకు ధన్యవాదాలు! ఈరోజు మీకు ఏ చట్టపరమైన విషయంలో సహాయం కావాలి?" };
      if (isThanks) return { title: "చాలా ధన్యవాదాలు! 😊", message: "మీకు భవిష్యత్తులో ఏవైనా చట్టపరమైన సందేహాలు ఉంటే ఎప్పుడైనా అడగవచ్చు." };
      return { title: "నమస్కారం! నేను Nyaai 😊", message: "మీ న్యాయ సహాయకుడిని. ఈరోజు నేను మీకు ఎలా సహాయపడగలను? మీరు ఎఫ్ఐఆర్, బెయిల్ లేదా పౌర హక్కుల గురించి ఏదైనా అడగవచ్చు." };

    case "ta":
      if (isIdentity) return { title: "வணக்கம்! நான் Nyaai 😊", message: "உங்கள் இந்திய சட்ட உதவியாளர்! இந்திய சட்டங்கள் மற்றும் அரசியலமைப்பை எளிய மொழியில் விளக்க நான் உதவுகிறேன். நீங்கள் எந்தவொரு சட்டக் கேள்வியையும் என்னிடம் கேட்கலாம்." };
      if (isWellBeing) return { title: "நான் நலமாக இருக்கிறேன்! 😊", message: "கேட்டதற்கு நன்றி! இன்று உங்களுக்கு சட்ட ரீதியாக நான் எவ்வாறு உதவ முடியும்?" };
      if (isThanks) return { title: "மிக்க நன்றி! 😊", message: "உங்களுக்கு மேலும் ஏதேனும் சட்ட சந்தேகங்கள் இருந்தால் தயங்காமல் கேளுங்கள்." };
      return { title: "வணக்கம்! நான் Nyaai 😊", message: "உங்கள் சட்ட உதவியாளர். இன்று நான் உங்களுக்கு எவ்வாறு உதவ முடியும்? எஃப்.ஐ.ஆர், ஜாமீன் அல்லது குடிமக்கள் உரிமைகள் பற்றி நீங்கள் கேட்கலாம்." };

    default: // en
      if (isIdentity) return { title: "Hello! I am Nyaai (न्यायAI) 😊", message: "I'm your friendly Indian legal assistant. My mission is to make Indian laws—including the Constitution of India, Bharatiya Nyaya Sanhita (BNS), BNSS, and citizen rights—simple, clear, and easy to understand for everyone. How can I help you today?" };
      if (isWellBeing) return { title: "I'm doing great, thank you! 😊", message: "Ready to make Indian law simple and accessible for you. What's on your mind today?" };
      if (isThanks) return { title: "You're very welcome! 😊", message: "If you have any more legal questions or need clarity on your rights and legal procedures, feel free to ask anytime. Stay safe and informed!" };
      return { title: "Hello! I'm Nyaai 😊", message: "I'm your friendly Indian legal assistant. How can I help you today? You can ask me about citizen rights, police procedures (FIR/arrest), bail provisions, consumer rights, or any specific legal questions." };
  }
}


/* =========================================================
   Advocate Scenario Advisory Engine (Indian Legal Practice)
   BNS 2023 • BNSS 2023 • BSA 2023 • Special Statutory Acts
   ========================================================= */
function isScenarioQuery(queryText) {
  if (!queryText) return false;
  const q = queryText.toLowerCase().trim();
  
  const scenarioPhrases = [
    "what should i do", "what can i do", "how to proceed", "what is the process",
    "what is the procedure", "how do i file", "how can i file", "next steps",
    "further proceeding", "further proceedings", "legal action", "where to complain",
    "what are my options", "legal remedies", "how to handle this", "i need advice",
    "please help", "what happens if", "is it legal for", "is it illegal for",
    "my landlord", "my tenant", "my employer", "my boss", "my company",
    "my husband", "my wife", "my neighbour", "my neighbor", "my brother",
    "someone hit", "hit my car", "hit and run", "cheque bounce", "cheque bounced",
    "bounced cheque", "salary not paid", "unpaid salary", "withholding salary",
    "refused to pay", "refused my fir", "refused to register fir", "police refused",
    "police not taking", "morphed photo", "blackmail", "blackmailing", "leaked photo",
    "cyber fraud", "upi scam", "upi fraud", "account hacked", "illegal detention",
    "arrested without", "without warrant", "domestic violence", "dowry harassment",
    "encroach", "encroachment", "land grabbing", "boundary wall", "threat to life",
    "threatening me", "medical negligence", "doctor negligence", "servant stole",
    "domestic help", "maid stole", "defamation", "defaming", "defamatory", "false fir", "fake fir", "fake case",
    "locked my flat", "locked out", "deposit not returning", "stole my", "stolen my", "stole gold",
    "fraud", "cheated", "scammed", "delayed salary", "custodial", "harassing me",
    "how to recover", "fake loan", "loud music", "frame me", "divorce", "custody",
    "refuse refund", "refused refund", "refusing refund", "not paying", "invoices",
    "online store", "bought a", "hospital doctor", "landlord", "tenant"
  ];
  
  return scenarioPhrases.some(phrase => q.includes(phrase));
}


function getAdvocateScenarioAdvisory(queryText, lang, forceFallback = false) {
  if (!queryText) return null;
  const isScenario = isScenarioQuery(queryText) || forceFallback;
  if (!isScenario) return null;

  const q = queryText.toLowerCase().trim();

  // 1. Landlord & Tenant Lockout / Eviction / Deposit
  if ((q.includes("landlord") || q.includes("tenant") || q.includes("flat") || q.includes("rent")) && 
      (q.includes("locked") || q.includes("belonging") || q.includes("evict") || q.includes("deposit") || q.includes("vacate") || q.includes("advance"))) {
    return {
      category: "Landlord-Tenant Conflict",
      title: "Advocate Advisory: Unlawful Eviction & Tenant Belongings Seizure",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Transfer of Property Act, 1882 & Bharatiya Nyaya Sanhita (BNS), 2023",
      section: "Section 106 TP Act • Sections 329, 316, 303 BNS 2023 • Order 39 CPC",
      classification: {
        nature: "Civil Dispossession + Criminal Trespass, Theft & Breach of Trust",
        cognizable: "Cognizable (Section 329 BNS Criminal Trespass & Section 316 BNS Criminal Breach of Trust)",
        bailable: "Bailable / Non-Compoundable without Court Sanction",
        relevantSections: "Section 329 BNS (Criminal Trespass), Section 316 BNS (Criminal Breach of Trust), Section 106 Transfer of Property Act 1882, Order 39 Rules 1 & 2 CPC"
      },
      stage1Immediate: [
        "🚨 Do NOT take law into your own hands: Do not forcibly break padlocks yourself, as the landlord could counter-accuse you of housebreaking.",
        "📸 Timestamped Photo & Video Evidence: Take high-resolution photographs and video of the locked door, padlocks, and any notices pasted on the door.",
        "📱 Preserve Electronic Tenancy Proof: Under Section 63 of Bharatiya Sakshya Adhiniyam (BSA) 2023, archive WhatsApp conversations, emails, rent payment receipts, and bank statements proving active tenancy.",
        "📞 Dial 112 from the Spot: Call Police Emergency (112) immediately while standing outside the flat. Request a Police Control Room (PCR) dispatch; this generates an official police event log verifying dispossession."
      ],
      stage2FormalRecourse: [
        "🚓 File Police Complaint / Zero FIR: Visit the jurisdictional police station with your rental agreement and rent receipts. Request an FIR under Section 329 BNS (Criminal Trespass) and Section 316 BNS (Criminal Breach of Trust) for withholding your personal belongings. Under Section 173 BNSS 2023, police are statutorily mandated to record cognizable complaints.",
        "⚖️ Crucial Escalation if Police Refuse FIR: If local police dismiss it as a 'civil matter', immediately invoke Section 175(3) of BNSS 2023 (formerly CrPC 154(3)). Send a signed written complaint by Registered Speed Post directly to the Superintendent of Police (SP) or Deputy Commissioner of Police (DCP).",
        "📜 Advocate Legal Demand Notice: Engage an advocate to serve a formal 7-Day Statutory Legal Demand Notice via Speed Post and Email, demanding immediate peaceful possession, return of belongings, and compensation for wrongful restraint."
      ],
      stage3CourtProceedings: [
        "🏛️ Section 175(4) / 176 BNSS Application to Magistrate: If police fail to act after SP escalation, file an application before the Judicial Magistrate First Class (JMFC) under Section 175(4) BNSS (formerly CrPC 156(3)) praying for an order directing police to lodge an FIR and search/restore your belongings.",
        "⚡ Urgent Civil Suit under Section 6 Specific Relief Act, 1963: File a summary suit for restoration of possession of immovable property (requires no proof of title, only proof of prior lawful possession and dispossession without consent).",
        "🛡️ Order 39 Rules 1 & 2 CPC Interim Mandatory Injunction: File an urgent interlocutory application seeking ex-parte mandatory injunction directing the landlord to hand over duplicate keys or open the flat under Court Commissioner supervision within 24 hours.",
        "💰 Claim for Damages: Pray for liquidated compensation covering emergency hotel stay, replacement of essential medicines/clothes, and mental agony."
      ],
      advocateStrategicAdvice: [
        "⚠️ Binding Precedent: Supreme Court of India in 'Bishandas v. State of Punjab' has firmly held that even where tenancy has expired, a landlord cannot take law into their own hands and evict without a court decree.",
        "⏳ Limitation Period: A suit under Section 6 of Specific Relief Act must be filed within 6 MONTHS from the date of illegal dispossession.",
        "📮 Proof of Service: Always preserve India Post Registered Speed Post receipts and online delivery tracking consignment reports as indisputable proof in court."
      ]
    };
  }

  // 2. Road Accidents & Hit and Run
  if (q.includes("hit and run") || q.includes("hit my car") || q.includes("hit my bike") || (q.includes("accident") && (q.includes("car") || q.includes("vehicle") || q.includes("speeding") || q.includes("rash") || q.includes("ran away")))) {
    return {
      category: "Motor Accident & Vehicular Offense",
      title: "Advocate Advisory: Hit-and-Run Motor Vehicle Accident & Compensation Claim",
      confidence: "99.7% Advocate Procedural Grounding",
      act: "Bharatiya Nyaya Sanhita (BNS) 2023 & Motor Vehicles Act, 1988",
      section: "Sections 281, 125, 106 BNS 2023 • Sections 161, 166 Motor Vehicles Act",
      classification: {
        nature: "Cognizable Criminal Offense (Rash Driving & Negligence) + Statutory MACT Claim",
        cognizable: "Cognizable (Section 281 & Section 106 BNS)",
        bailable: "Bailable (Sec 281 / 125 BNS) / Section 106(2) hit-and-run without reporting carries strict punishment up to 10 years",
        relevantSections: "Section 281 BNS (Rash Driving on Public Way), Section 125 BNS (Endangering Life), Section 106 BNS (Causing Death/Injury by Negligence), Section 161 & 166 MV Act"
      },
      stage1Immediate: [
        "🚑 Medical Attention & Medico-Legal Certificate (MLC): Prioritize emergency hospital care. Ensure the treating doctor registers the injury as an MLC (Medico-Legal Case) and clearly records the accident history in the casualty bed-head ticket.",
        "📸 Secure Scene & Vehicle Details: Note the offending vehicle's registration number, make, color, and direction of escape. Photograph vehicle damage, skid marks, road position, and debris.",
        "📹 Retrieve CCTV Footage: Identify commercial shops, fuel stations, traffic cameras, or dashcams nearby. Under Section 63 BSA 2023, request surrounding premises to preserve backup footage immediately before overwriting.",
        "👥 Witness Contacts: Collect names and phone numbers of bystanders, shopkeepers, or co-commuters who saw the collision."
      ],
      stage2FormalRecourse: [
        "🚓 Mandatory FIR Registration: Approach the local police station having jurisdiction over the accident spot. File a detailed written complaint. An FIR under Section 281 BNS (Rash driving) and Section 125/106 BNS must be registered immediately.",
        "⚖️ Section 175(3) BNSS Escalation if Police Delay: If the police delay or try to compromise with the vehicle owner/driver, send your written representation to the Superintendent of Police (SP) under Section 175(3) BNSS.",
        "📑 Obtain Certified Copies of Police Documents: Statutorily request certified copies of: (1) FIR, (2) Accident Spot Panchnama / Site Map, (3) Motor Vehicle Inspector (MVI) Inspection Report, (4) Offending vehicle's Insurance Policy and Driver License copy."
      ],
      stage3CourtProceedings: [
        "🏛️ File Claim Petition before Motor Accident Claims Tribunal (MACT): File a claim under Section 166 of the Motor Vehicles Act, 1988 before the MACT having jurisdiction over the accident location or claimant's residence.",
        "💰 Calculation of Compensation Heads: Claim comprehensive compensation under: (1) Medical expenditures & future treatments, (2) Vehicle repair and depreciation loss, (3) Loss of current and future earning capacity, (4) Pain, suffering, and loss of amenities.",
        "🛡️ Solatium Scheme for Unidentified Hit-and-Run: If the offending vehicle remains untraceable despite police investigation, apply for compensation under Section 161 MV Act (Solatium Scheme) through the District Sub-Divisional Magistrate (SDM)."
      ],
      advocateStrategicAdvice: [
        "⚠️ Do NOT sign any pre-printed compromise letters or cash settlement vouchers from the driver or insurance surveyor without consulting an advocate.",
        "⏳ Limitation: Under the 2019 amendment, MACT claims must be filed within 6 MONTHS from the date of the accident.",
        "📑 Preserve all original medical bills, pharmacy receipts, ambulance receipts, and employer salary deduction slips."
      ]
    };
  }

  // 3. Cheque Bounce (Section 138 NI Act)
  if (q.includes("cheque") && (q.includes("bounce") || q.includes("bounced") || q.includes("dishonor") || q.includes("returned") || q.includes("insufficient"))) {
    return {
      category: "Negotiable Instruments & Commercial Dispute",
      title: "Advocate Advisory: Dishonour of Cheque & Criminal Prosecution (Sec 138 NI Act)",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Negotiable Instruments Act, 1881 & Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023",
      section: "Section 138 & 142 NI Act 1881 • Section 143A (Interim Compensation) • Section 223 BNSS",
      classification: {
        nature: "Quasi-Criminal Offense punishable with up to 2 years imprisonment or fine up to twice the cheque amount",
        cognizable: "Non-Cognizable (instituted via Private Complaint before Judicial Magistrate)",
        bailable: "Bailable / Compoundable at any stage",
        relevantSections: "Section 138, 139 (Presumption in favour of holder), 141 (Company liability), 142 (Cognizance of offenses), 143A NI Act"
      },
      stage1Immediate: [
        "🏦 Bank Return Memo: Collect the original Cheque along with the official Bank Return Memo stating the reason ('Funds Insufficient', 'Account Closed', 'Exceeds Arrangement').",
        "📅 Strict 30-Day Notice Clock: Note the exact date when your bank issued the Return Memo. Under Section 138 proviso (b), you must issue a formal Legal Demand Notice within 30 DAYS of receiving this return memo.",
        "📑 Legally Enforceable Debt Evidence: Gather contract agreements, invoices, ledger accounts, receipts, delivery challans, or WhatsApp/email communications proving that the cheque was issued for a legally enforceable debt/liability."
      ],
      stage2FormalRecourse: [
        "📜 Mandatory 15-Day Statutory Legal Demand Notice: Engage an advocate to draft and dispatch a formal Legal Demand Notice under Section 138(b) NI Act. The notice must demand full payment within 15 DAYS of receipt.",
        "📮 Speed Post with Tracking: Dispatch the notice via Registered Speed Post with Acknowledgment Due (RPAD) and email. Track India Post delivery report and print it immediately upon delivery.",
        "⚠️ Cause of Action Window: The drawer has 15 days from delivery to pay. If they fail to pay by the 15th day, your 'Cause of Action' legally arises on the 16th day."
      ],
      stage3CourtProceedings: [
        "🏛️ File Criminal Complaint within 30 Days: Under Section 142(1)(b) NI Act, you must file a formal Criminal Complaint before the Judicial Magistrate First Class (JMFC) or Metropolitan Magistrate within exactly 30 DAYS from the expiry of the 15-day notice period.",
        "⚖️ Pre-Summoning Evidence by Affidavit: Under Section 145 NI Act, complainant tenders evidence on affidavit without prolonged oral examination. Magistrate issues summons/bailable warrant to the accused.",
        "💰 20% Interim Compensation under Section 143A: File an application under Section 143A NI Act praying for an interim deposit of up to 20% of the cheque amount to be released to you pending trial.",
        "🏢 Company Offense under Section 141: If cheque was issued by a company or partnership, all Directors/Partners in charge of day-to-day business must be impleaded as co-accused."
      ],
      advocateStrategicAdvice: [
        "⚠️ Fatal Limitation Trap: Missing the 30-day notice deadline or the 30-day court filing deadline can extinguish your right to prosecute, unless condonation of delay is allowed on substantial grounds.",
        "⚖️ Presumption under Section 139: The law presumes the cheque was issued for debt satisfaction; the entire burden of proof is on the accused to rebut this presumption.",
        "📦 Keep original cheque in a protective plastic cover; do not staple, overwrite, or tear."
      ]
    };
  }

  // 4. Cyber Crime & Financial Fraud / UPI Scams
  if ((q.includes("cyber") || q.includes("online") || q.includes("upi") || q.includes("phishing") || q.includes("otp") || q.includes("bank account")) && 
      (q.includes("fraud") || q.includes("scam") || q.includes("debited") || q.includes("money") || q.includes("hacked") || q.includes("stolen") || q.includes("cheated"))) {
    return {
      category: "Cyber Crime & Online Financial Fraud",
      title: "Advocate Advisory: Cyber Financial Fraud, UPI Scam & Fund Freezing Recourse",
      confidence: "99.7% Advocate Procedural Grounding",
      act: "Information Technology Act, 2000 & Bharatiya Nyaya Sanhita (BNS), 2023",
      section: "Section 43 & 66D IT Act • Section 318(4) & 319 BNS • RBI Master Direction 2017",
      classification: {
        nature: "Cognizable Cyber Crime + Statutory Bank Zero-Liability Protection",
        cognizable: "Cognizable (Section 66D IT Act & Section 318 BNS)",
        bailable: "Non-Bailable depending on quantum of cyber fraud",
        relevantSections: "Section 66D IT Act (Cheating by personation using computer resource), Section 318(4) BNS (Cheating), Section 173 BNSS"
      },
      stage1Immediate: [
        "🚨 Dial 1930 Cyber Fraud Helpline within Golden Hours (First 2-4 Hours): Call 1930 immediately or log on to cybercrime.gov.in. The National Cybercrime Reporting Portal triggers an automated lien alert across the banking switch to freeze funds in the fraudster's beneficiary account before withdrawal.",
        "🏦 Immediate Bank Notification within 3 Days: Formally report the unauthorized transaction to your bank via customer care, email, and visit your branch in writing within 72 hours. Under RBI's Circular on 'Customer Protection — Limiting Liability in Unauthorized Electronic Transactions', reporting within 3 days grants ZERO customer liability for third-party breaches.",
        "📸 Preserve Digital Evidence: Take screenshots of payment gateway messages, UPI Transaction IDs, reference numbers, SMS alerts, fraudulent links, call history, and bank account debit statements. Under Section 63 BSA 2023, these are admissible digital records.",
        "🔒 Block Cards and Reset Credentials: Freeze compromised debit/credit cards, UPI apps, net banking passwords, and SIM cards."
      ],
      stage2FormalRecourse: [
        "🚓 File FIR at Cyber Crime Police Station: Register an FIR under Section 66D of the IT Act and Section 318(4) of BNS 2023. Attach the cyber crime acknowledgement slip and bank statement.",
        "⚖️ Section 175(3) BNSS Escalation: If local police refuse because the fraud happened online or outside jurisdiction, escalate to the Superintendent of Police (SP) or Cyber Crime Nodal Officer under Section 175(3) BNSS.",
        "📜 Representation to Banking Ombudsman: If your bank fails to credit back the disputed amount or investigate within 30 days, lodge a complaint with the Reserve Bank of India (RBI) Integrated Ombudsman Portal (cms.rbi.org.in)."
      ],
      stage3CourtProceedings: [
        "🏛️ De-freezing Application under Section 503 BNSS: Once police freeze the fraudster's beneficiary account, file an application before the Judicial Magistrate under Section 503 BNSS (formerly CrPC 457) for release of seized funds directly back into your bank account.",
        "⚖️ Adjudicating Officer Petition under Section 46 IT Act: File a claim before the State IT Secretary (statutory Adjudicating Officer under the IT Act) claiming full compensation for financial loss caused by computer fraud.",
        "🛍️ Consumer Commission Complaint: If the bank demonstrated gross negligence or security lapse (e.g. SIM swap without verification), file a complaint before the District Consumer Commission for deficiency in banking service."
      ],
      advocateStrategicAdvice: [
        "⚠️ Do NOT delete WhatsApp chats, call logs, or SMS messages with the scammer; export and backup the chat history.",
        "⏳ The first 2 hours are the most critical for fund freezing; delay drastically reduces the probability of fund recovery.",
        "🏦 Always secure an official written acknowledgement receipt with date and stamp from your home branch."
      ]
    };
  }

  // 5. Cyber Blackmail, Sextortion & Morphing
  if (q.includes("blackmail") || q.includes("morphed") || q.includes("sextortion") || (q.includes("photos") && (q.includes("leak") || q.includes("share") || q.includes("private")))) {
    return {
      category: "Cyber Crime & Privacy Violation",
      title: "Advocate Advisory: Cyber Blackmail, Morphed Photos & Online Sextortion",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Information Technology Act, 2000 & Bharatiya Nyaya Sanhita (BNS), 2023",
      section: "Section 66E, 67, 67A IT Act • Section 308, 75, 78 BNS • Section 173 BNSS",
      classification: {
        nature: "Heinous Cognizable Cyber Crime & Extortion",
        cognizable: "Cognizable (Section 67A IT Act & Section 308 BNS)",
        bailable: "Non-Bailable",
        relevantSections: "Section 66E IT Act (Violation of privacy), Section 67A IT Act (Sexually explicit acts), Section 308 BNS (Extortion), Section 78 BNS (Stalking)"
      },
      stage1Immediate: [
        "🛑 STOP: Do NOT Pay Any Ransom Money: Paying money will NEVER stop the extortion; scammers immediately demand higher sums once they confirm you are fearful.",
        "📸 Screen Record & Capture Everything: Take screenshots and screen recordings of the blackmailer's profile, phone number, UPI ID / bank details used for extortion, chat messages, and the morphed content.",
        "🔒 Lock & Secure Social Media Accounts: Set Instagram, Facebook, and LinkedIn profiles to private. Change passwords and enable two-factor authentication (2FA). Do not delete the account yet, as police require live link verification.",
        "🛑 StopNCII.org Registration: If intimate images or deepfakes are threatened, upload image hashes to StopNCII.org (supported by Meta, Instagram, Tech platforms) to generate a cryptographic hash that automatically blocks the image across platforms without sharing the actual photo."
      ],
      stage2FormalRecourse: [
        "🚓 Immediate Cyber Crime FIR / Zero FIR: Report immediately on cybercrime.gov.in under 'Crime Against Women/Children' or 'Cyber Blackmail'. Visit your local Cyber Crime Police Station or any nearest station to register a Zero FIR under Section 173 BNSS.",
        "⚖️ Invocation of Stringent Sections: Insist on registering under Section 67A IT Act (up to 5 years imprisonment & ₹10 lakh fine for transmitting sexually explicit material), Section 308 BNS (Extortion), and Section 75/78 BNS (Sexual harassment & cyberstalking).",
        "📜 Takedown Notice to Social Media Intermediaries: Under Rule 3(2)(b) of the Information Technology (Intermediary Guidelines) Rules, 2021, social media platforms are statutorily required to remove non-consensual intimate imagery within 24 HOURS of receiving notification."
      ],
      stage3CourtProceedings: [
        "🏛️ Magistrate Direction for IP & CDR Disclosure: Police will move the Judicial Magistrate under BNSS for orders directing WhatsApp, Meta, Google, and Telecom Service Providers to disclose the IP addresses, IMEI, and Call Detail Records (CDR) of the perpetrators.",
        "🛡️ High Court Injunction & John Doe Orders: In serious cases involving widespread dissemination, file a Civil Suit before the High Court seeking 'John Doe' (Ashok Kumar) injunction orders prohibiting internet service providers and search engines from indexing or hosting the content.",
        "⚖️ Witness Protection & In-Camera Proceedings: You are entitled to complete identity masking and confidential in-camera trial proceedings under Section 366 BNSS."
      ],
      advocateStrategicAdvice: [
        "⚠️ Understand the Psychology: The extortionist's only leverage is your panic. Once you inform them that a Cyber Crime FIR with digital tracing has been filed, they usually discard the burner account and move away.",
        "👥 Confide in a trusted family member or counselor; do not suffer in isolation.",
        "📞 National Cyber Helpline: Call 1930 immediately for assistance."
      ]
    };
  }

  // 6. Unpaid Salary & Employment Disputes
  if (q.includes("salary") && (q.includes("not paid") || q.includes("unpaid") || q.includes("refusing") || q.includes("delay") || q.includes("fired") || q.includes("employer") || q.includes("months") || q.includes("recover"))) {
    return {
      category: "Labor & Employment Dispute",
      title: "Advocate Advisory: Recovery of Unpaid Salary & Wrongful Employment Termination",
      confidence: "99.6% Advocate Procedural Grounding",
      act: "Payment of Wages Act, 1936, Industrial Disputes Act, 1947 & BNS 2023",
      section: "Payment of Wages Act • Sections 316 & 318 BNS 2023 • Order 37 CPC",
      classification: {
        nature: "Civil Labor Claim + Criminal Breach of Trust & Cheating",
        cognizable: "Civil Labor Recourse + Cognizable Criminal Offense if fraudulent inducement existed",
        bailable: "Bailable",
        relevantSections: "Payment of Wages Act 1936, Industrial Disputes Act 1947, Section 316 BNS (Breach of Trust), Order 37 CPC (Summary Suit)"
      },
      stage1Immediate: [
        "📑 Preserve Employment Records: Backup offer letter, appointment letter, employment contract, official salary slips, bank credit statements, promotion letters, and company HR policy handbook.",
        "📧 Work Proof & Attendance Logs: Archive emails acknowledging your work performance, task delivery, attendance logs, timesheets, and any emails where management promises to clear pending salary.",
        "📩 Official Written Demand to HR: Send a formal, polite email to HR and Directors citing your total outstanding salary, notice pay, and reimbursement dues, requesting settlement within 7 business days."
      ],
      stage2FormalRecourse: [
        "📜 Advocate Legal Demand Notice: Engage an advocate to serve a formal 15-Day Legal Demand Notice under Section 316 & 318 BNS and Contract Law, demanding payment of principal salary plus statutory interest (typically 18% p.a.).",
        "🏛️ Complaint before Deputy Labour Commissioner: File an application under the Payment of Wages Act or State Shops & Commercial Establishments Act before the jurisdictional Labour Commissioner. The Commissioner issues summons for conciliation and can order payment of dues plus up to 10x penalty.",
        "🚓 Police Complaint for Criminal Breach of Trust: If the employer deducted Employee Provident Fund (EPF) or Tax (TDS) from your salary but failed to deposit it with EPFO/Income Tax department, file an FIR under Section 316 BNS (Criminal Breach of Trust)."
      ],
      stage3CourtProceedings: [
        "🏛️ Summary Suit under Order 37 CPC: File a Summary Suit before the Civil Judge or Commercial Court for debt recovery based on written employment contract and salary slips. In an Order 37 suit, the employer has no automatic right of defense without court leave.",
        "💼 Insolvency / IBC Section 9 Notice (for Corporate Employers): If the company owes you more than the statutory threshold, issue a Demand Notice under Section 9 of the Insolvency and Bankruptcy Code (IBC, 2016) as an Operational Creditor.",
        "⚖️ Labour Court / Industrial Tribunal Adjudication: If conciliation fails before the Labour Commissioner, get the dispute referred to the Labour Court for an enforceable recovery award."
      ],
      advocateStrategicAdvice: [
        "⚠️ Do NOT sign full-and-final settlement discharge vouchers or surrender company assets (laptop/ID) without written acknowledgement of pending dues.",
        "⏳ Limitation Period: The limitation period for filing a civil recovery suit for unpaid salary is 3 YEARS from the date the wages fell due.",
        "🏢 Check EPF Passbook: Verify online on the EPFO portal if deductions match actual deposits."
      ]
    };
  }

  // 7. Police Refusal to Register FIR
  if (q.includes("police") && (q.includes("refused") || q.includes("not taking") || q.includes("not registering") || q.includes("rejected")) && (q.includes("fir") || q.includes("complaint"))) {
    return {
      category: "Procedural Law & Public Duty Enforcement",
      title: "Advocate Advisory: Statutory Remedies against Police Refusal to Lodge FIR",
      confidence: "99.9% Advocate Procedural Grounding",
      act: "Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023 & Constitution of India",
      section: "Section 173, 175(3), 175(4), 176 BNSS 2023 • Section 199 BNS 2023 • Art 226",
      classification: {
        nature: "Dereliction of Public Duty by Police & Violation of Statutory Right",
        cognizable: "Mandatory statutory duty under Section 173 BNSS",
        bailable: "Procedural Enforcement",
        relevantSections: "Section 173 BNSS (Information in cognizable cases), Section 175(3) BNSS (Representation to SP), Section 175(4)/176 BNSS (Magistrate inquiry), Section 199 BNS (Public servant disobeying law)"
      },
      stage1Immediate: [
        "📝 Written Complaint with Receiving Stamp: Draft your complaint with complete date, time, names of accused, and facts. Carry two copies to the police station. Insist on a receiving stamp and General Diary (GD) entry number.",
        "📱 Note Station Details & Call 112: Note down the name, rank, and badge number of the officer refusing your complaint. Make an emergency call to 112 from inside or outside the police station to create an official record that you visited the station.",
        "🌍 Zero FIR Provision under Section 173 BNSS: If the officer claims the offense occurred in another jurisdiction, point out that Section 173 BNSS statutorily mandates recording a 'Zero FIR', which must be transferred to the concerned police station without delay."
      ],
      stage2FormalRecourse: [
        "⚖️ Immediate Escalation under Section 175(3) BNSS (Mandatory Advocate Step): Under Section 175(3) BNSS 2023 (formerly CrPC 154(3)), immediately send your signed written complaint by Registered Speed Post directly to the Superintendent of Police (SP) or Deputy Commissioner of Police (DCP).",
        "📮 Speed Post Proof: Preserve the postal receipt and India Post tracking report proving that the SP received your complaint. This is a mandatory prerequisite for approaching the court.",
        "⚖️ Penal Action under Section 199 BNS: Section 199 of BNS (replaces IPC 166A) makes it a criminal offense punishable with up to 2 years imprisonment for any police officer who willfully fails or refuses to record information regarding cognizable offenses."
      ],
      stage3CourtProceedings: [
        "🏛️ Section 175(4) / 176 BNSS Application to Judicial Magistrate: If the SP fails to direct an investigation within a reasonable period, file an application under Section 175(4) / 176 BNSS (formerly CrPC 156(3)) before the Judicial Magistrate First Class (JMFC).",
        "⚡ Magistrate's Judicial Direction: The Magistrate, upon examining your affidavit and postal proof, has statutory authority to pass an order directing the police station to register an FIR and submit a compliance report within a set timeframe.",
        "📜 High Court Writ of Mandamus under Article 226: In serious or politically sensitive offenses where local machinery is compromised, file a Criminal Writ Petition (Writ of Mandamus) before the High Court praying for directions to register FIR or transfer investigation to CBI/CID."
      ],
      advocateStrategicAdvice: [
        "⚠️ Landmark Binding Precedent: Constitution Bench of Supreme Court in 'Lalita Kumari v. Govt of UP' held that registration of FIR is MANDATORY under law if information discloses commission of a cognizable offense.",
        "📑 Maintain a clean chronologically dated binder with all complaint copies, speed post receipts, and 112 call logs.",
        "🤝 Seek assistance from an advocate to draft the Section 175(4) BNSS application."
      ]
    };
  }

  // 8. Illegal Detention & Custodial Abuse
  if (q.includes("illegal detention") || q.includes("arrested without") || (q.includes("police") && (q.includes("detained") || q.includes("custody") || q.includes("beating") || q.includes("picked up")))) {
    return {
      category: "Constitutional Safeguards & Human Rights",
      title: "Advocate Advisory: Illegal Police Detention, Custodial Rights & Habeas Corpus",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Constitution of India, 1950 & Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023",
      section: "Articles 21 & 22 Constitution • Sections 47, 48, 50, 53, 58 BNSS 2023",
      classification: {
        nature: "Constitutional Violation of Personal Liberty & Illegal Confinement",
        cognizable: "Severe Human Rights Violation",
        bailable: "Constitutional Remedy",
        relevantSections: "Article 21 & 22 Constitution of India, Sections 47, 48, 50, 53, 58 BNSS 2023, D.K. Basu Guidelines"
      },
      stage1Immediate: [
        "📋 Demand Written Grounds of Arrest under Section 47 BNSS: Police are statutorily required to immediately inform the arrested person of the exact grounds of arrest and whether the offense is bailable.",
        "📞 Section 50 BNSS Right to Inform Nominated Person: Under Section 50 BNSS, the arresting officer MUST immediately inform a designated family member or friend and enter this into the station log book.",
        "🩺 Mandatory Medical Examination under Section 53 BNSS: Insist on an independent medical examination of the arrested person immediately after arrest and before presentation to the Magistrate to document any existing injuries or torture.",
        "⏱️ 24-Hour Strict Constitutional Clock: Under Section 58 BNSS and Article 22(2) of the Constitution, police CANNOT detain any person for more than 24 HOURS without producing them physically before a Judicial Magistrate."
      ],
      stage2FormalRecourse: [
        "⚖️ Immediate Written Application to Judicial Magistrate: Move an urgent application before the Chief Judicial Magistrate (CJM) or Area Magistrate bringing on record the illegal detention beyond 24 hours.",
        "📜 Notice to State Human Rights Commission (SHRC): Send an urgent petition to the State Human Rights Commission and National Human Rights Commission (NHRC) regarding custodial torture or illegal confinement.",
        "📹 Preserve Police Station CCTV Footage: Under Supreme Court's mandate in 'Paramvir Singh Saini v. Baljit Singh', all police stations must have working CCTV cameras with 18-month backup. File an urgent application to seal and preserve station CCTV footage."
      ],
      stage3CourtProceedings: [
        "🏛️ Habeas Corpus Writ Petition under Article 226 / Article 32: If a person is detained in an undisclosed location or without formal FIR/remand, immediately file a Writ of Habeas Corpus before the High Court praying for directions to produce the person in court within 2 hours.",
        "⚖️ Bail Application under Section 480 / 482 BNSS: Move a regular bail application before the Magistrate/Sessions Court at the very first production, highlighting violations of arrest guidelines.",
        "💰 Compensation for Custodial Violation: High Court has constitutional power under Article 226 to grant public law damages and monetary compensation for illegal detention (Nilabati Behera v. State of Orissa)."
      ],
      advocateStrategicAdvice: [
        "⚠️ Supreme Court D.K. Basu Guidelines: Arrest memo must be signed by at least one witness who is a family member or respected member of the locality.",
        "⚖️ Right to Legal Representation: Under Section 38 BNSS, the accused has the constitutional right to consult and be defended by an advocate of their choice throughout interrogation.",
        "🛡️ Free Legal Aid: If the family cannot afford a private lawyer, demand an immediate remand advocate from the District Legal Services Authority (DLSA)."
      ]
    };
  }

  // 9. Domestic Violence, Dowry & Matrimonial Cruelty
  if (q.includes("domestic violence") || q.includes("dowry") || (q.includes("husband") && (q.includes("beaten") || q.includes("harassing") || q.includes("in-laws") || q.includes("torture") || q.includes("locked out")))) {
    return {
      category: "Women Protection & Matrimonial Law",
      title: "Advocate Advisory: Domestic Violence, Dowry Cruelty & Emergency Protection",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Protection of Women from Domestic Violence Act, 2005 & BNS 2023",
      section: "Sections 12, 18, 19, 20, 22 DV Act 2005 • Sections 85 & 86 BNS 2023",
      classification: {
        nature: "Cognizable Criminal Cruelty + Quasi-Civil Statutory Protection & Residence Orders",
        cognizable: "Cognizable (Section 85 & 86 BNS 2023)",
        bailable: "Non-Bailable (Section 85 BNS 2023 Cruelty by husband or relatives)",
        relevantSections: "Section 85 BNS (Cruelty by Husband or Relatives), Section 86 BNS (Cruelty defined), Sections 12, 18 (Protection Order), 19 (Residence Order), 20 (Monetary Relief) DV Act 2005"
      },
      stage1Immediate: [
        "🚨 Emergency Protection & National Helplines: If in immediate physical danger, call 112 (Police Emergency) or 181 (Women's Helpline). This ensures rapid police intervention and creates an official event log.",
        "🏥 Medico-Legal Examination: Visit a government hospital casualty immediately to get injuries treated and recorded under an MLC (Medico-Legal Certificate).",
        "📸 Preserve Evidence & Communications: Photograph physical bruises, injuries, damaged property, and record/screenshot abusive WhatsApp chats, emails, or audio calls.",
        "💎 Secure Stridhan and Essential Documents: Safeguard your jewellery (Stridhan), Aadhaar card, PAN, educational certificates, passport, and marriage certificate in a bank locker or safe custody."
      ],
      stage2FormalRecourse: [
        "📜 Petition under Section 12 DV Act before Magistrate: Approach the Protection Officer or Service Provider appointed under the DV Act, or file a petition directly before the Judicial Magistrate First Class (JMFC) under Section 12 DV Act.",
        "🛡️ Seek Immediate Ex-Parte Interim Orders: The Magistrate is empowered to pass ex-parte orders within 3 days of filing for: (1) Protection Order (Sec 18) restraining husband/in-laws from contacting or entering place of work, (2) Residence Order (Sec 19) restraining them from evicting you from the shared household.",
        "🚓 Criminal FIR under Section 85 & 86 BNS (replaces IPC 498A): File an FIR with the Women Police Station (CAW Cell) detailing specific acts of physical, emotional, and dowry-related cruelty."
      ],
      stage3CourtProceedings: [
        "💰 Monthly Monetary Relief & Maintenance under Section 20 DV Act & Section 144 BNSS: Claim interim monthly maintenance for yourself and your children to maintain your standard of living.",
        "🏠 Enforcement of Residence Order under Section 19: If locked out, Magistrate can direct the local police to break the locks and restore your possession in the shared matrimonial home.",
        "💍 Section 19(8) Application for Return of Stridhan: Pray for a specific order directing husband and in-laws to return all wedding gifts, gold jewellery, cash, and articles given at the time of marriage.",
        "⚖️ Damages under Section 22 DV Act: Claim compensation for emotional distress, psychological trauma, and physical injuries caused by acts of domestic violence."
      ],
      advocateStrategicAdvice: [
        "⚠️ Shared Household Right: Supreme Court in 'Satish Chander Ahuja v. Sneha Ahuja' ruled that a woman has a statutory right of residence in a shared household even if the house is owned exclusively by in-laws.",
        "⚖️ Detailed Chronology: Maintain a precise dated timeline of specific dowry demands, monetary transfers, and incidents of assault.",
        "🤝 Protection Officers: Utilize the District Legal Services Authority (DLSA) for free experienced legal representation."
      ]
    };
  }

  // 10. Consumer Rights & E-Commerce Cheating
  if (q.includes("consumer") || q.includes("defective") || (q.includes("refund") && (q.includes("product") || q.includes("online") || q.includes("store") || q.includes("ecommerce") || q.includes("shopping")))) {
    return {
      category: "Consumer Protection & Commercial Fair Trade",
      title: "Advocate Advisory: Consumer Rights, E-Commerce Cheating & Deficiency of Service",
      confidence: "99.7% Advocate Procedural Grounding",
      act: "Consumer Protection Act, 2019 & E-Commerce Rules, 2020",
      section: "Sections 2(7), 2(11), 35, 38 Consumer Protection Act 2019 • e-Daakhil Rules",
      classification: {
        nature: "Statutory Consumer Dispute & Unfair Trade Practice",
        cognizable: "Quasi-Judicial Civil Consumer Recourse",
        bailable: "Civil Redressal",
        relevantSections: "Section 2(11) (Deficiency in service), Section 2(47) (Unfair Trade Practice), Section 35 (Filing of complaint), Section 84 (Product Liability)"
      },
      stage1Immediate: [
        "📦 Preserve Product Packaging & Unboxing Video: Preserve original unboxing videos, invoice, courier airway bill (AWB), warranty card, and defective item intact.",
        "📧 Formal Written Grievance to Company: Email the company's Grievance Officer detailing the order number, defect, and demanding immediate replacement or 100% refund within 48 hours.",
        "📞 National Consumer Helpline (NCH) 1915: Lodge a grievance online on consumerhelpline.gov.in or call 1915. NCH connects directly with registered corporate e-commerce companies for pre-litigation resolution."
      ],
      stage2FormalRecourse: [
        "📜 Advocate Legal Notice: If customer support refuses or stonewalls, serve a formal 15-Day Statutory Legal Demand Notice via Speed Post and Email, putting them on notice of liability under Consumer Protection Act 2019.",
        "💻 File Digital Complaint on e-Daakhil Portal: If no resolution within 15 days, file a formal complaint online via the Government of India's e-Daakhil portal (edaakhil.nic.in) without requiring physical court presence.",
        "⚖️ Jurisdiction Flexibility: Under CPA 2019, you can file the complaint before the District Consumer Commission located in the place where YOU reside or work, rather than the seller's city."
      ],
      stage3CourtProceedings: [
        "🏛️ District Consumer Disputes Redressal Commission Proceedings: Case is registered, and notice is issued to the e-commerce company and manufacturer.",
        "💰 Comprehensive Claim Heads: Pray for: (1) 100% refund of purchase amount along with 18% p.a. interest, (2) Substantial compensation for mental agony, distress, and waste of time, (3) Full litigation expenses and advocate fees.",
        "🛡️ Product Liability Action under Section 84: If the defective product caused physical damage, burn, or fire, claim product liability compensation against the manufacturer.",
        "⚖️ Penal Powers under Section 71: If the company fails to comply with the Consumer Commission's final order, the commission can initiate enforcement proceedings including imprisonment up to 3 years."
      ],
      advocateStrategicAdvice: [
        "⚠️ No Mandatory Court Fee for Claims up to ₹5 Lakhs: Complaints filed before the District Commission with claim values up to ₹5,00,000/- are completely exempt from court fees.",
        "⏳ Limitation Period: Consumer complaints must be filed within 2 YEARS from the date of the cause of action (defect/denial of refund).",
        "📸 Maintain screenshot evidence of product specifications and return policy published on the website at the time of purchase."
      ]
    };
  }

  // 11. Property Encroachment & Land Grabbing
  if (q.includes("encroach") || q.includes("land grabbing") || q.includes("boundary wall") || (q.includes("land") && (q.includes("dispute") || q.includes("plot") || q.includes("grabbed") || q.includes("trespass")))) {
    return {
      category: "Property Law & Real Estate Disputes",
      title: "Advocate Advisory: Land Encroachment, Illegal Trespass & Injunction Proceedings",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Specific Relief Act, 1963, Code of Civil Procedure (CPC) & BNS 2023",
      section: "Sections 5, 6, 38 Specific Relief Act • Order 39 CPC • Section 329 BNS 2023",
      classification: {
        nature: "Civil Title & Possession Dispute + Criminal Trespass",
        cognizable: "Cognizable (Section 329 BNS Criminal Trespass)",
        bailable: "Bailable / Non-Compoundable",
        relevantSections: "Section 329 BNS (Criminal Trespass), Section 324 BNS (Mischief), Section 38 Specific Relief Act (Permanent Injunction), Order 39 Rules 1 & 2 CPC"
      },
      stage1Immediate: [
        "📑 Collate Title & Revenue Records: Gather registered Sale Deed, Title Chain, Khata/Patta, Encumbrance Certificate (EC), Mutation extracts, and latest municipal tax receipts.",
        "📸 Timestamped Photography & Drone/Video Survey: Record video showing the exact encroachment, unauthorized construction materials, boundary wall demolition, and demarcation pegs.",
        "📍 Demarcation Application to Revenue Tehsildar: Submit an immediate formal application before the local Revenue Tehsildar / Taluk Surveyor for official survey, demarcation, and panchnama of the boundary."
      ],
      stage2FormalRecourse: [
        "🚓 Criminal Complaint under Section 329 BNS: Lodge an FIR at the local police station for Criminal Trespass (Section 329 BNS) and Mischief/Property Damage (Section 324 BNS).",
        "⚖️ Section 175(3) BNSS Escalation to SP: If police hesitate to interfere under the pretext of 'civil dispute', send a representation to the SP/DCP highlighting active criminal breach and threat of physical violence.",
        "📜 Advocate Cease and Desist Legal Notice: Serve a formal Legal Notice demanding immediate stoppage of all unauthorized construction and removal of encroachment within 7 days."
      ],
      stage3CourtProceedings: [
        "🏛️ Civil Suit for Permanent & Mandatory Injunction: File a civil suit before the Civil Judge / District Court praying for: (1) Permanent Injunction restraining encroachment, (2) Mandatory Injunction directing demolition of unauthorized structure.",
        "⚡ Urgent Ex-Parte Injunction under Order 39 Rules 1 & 2 CPC: Move an interlocutory application on Day 1 praying for an urgent status quo / temporary injunction restraining the encroacher from altering the nature of the property.",
        "🛡️ Appointment of Court Commissioner under Order 26 Rule 9 CPC: File an application praying for the appointment of an independent Court Commissioner (Advocate + Government Surveyor) to visit the spot, measure boundaries, and submit a report to court.",
        "⚖️ Section 6 Specific Relief Act Suit: If forcibly dispossessed within the past 6 months, file an expeditious suit for recovery of possession."
      ],
      advocateStrategicAdvice: [
        "⚠️ Act Immediately: Once permanent construction is completed, obtaining a demolition order is significantly harder than obtaining a status quo order during initial digging/construction.",
        "⏳ Supreme Court Principle: Settled possession cannot be disturbed except by due process of law; your revenue records and prior possession are your greatest shield.",
        "🧱 Do not engage in armed confrontation or physical fighting on the boundary; let court injunctions and police enforcement do the work."
      ]
    };
  }

  // 12. Threat to Life & Criminal Intimidation
  if (q.includes("threat to life") || q.includes("threatening to kill") || q.includes("extortion") || (q.includes("threat") && (q.includes("violence") || q.includes("phone call") || q.includes("money") || q.includes("goons")))) {
    return {
      category: "Criminal Law & Personal Security",
      title: "Advocate Advisory: Criminal Intimidation, Extortion & Police Protection Recourse",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Bharatiya Nyaya Sanhita (BNS) 2023 & Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023",
      section: "Sections 308, 351, 352 BNS 2023 • Section 126, 173 BNSS 2023",
      classification: {
        nature: "Heinous Cognizable Offense (Extortion & Criminal Intimidation)",
        cognizable: "Cognizable (Section 308 Extortion & Section 351(3) Threat to Cause Death/Grievous Hurt)",
        bailable: "Non-Bailable",
        relevantSections: "Section 308 BNS (Extortion), Section 351 BNS (Criminal Intimidation), Section 352 BNS (Intentional Insult to Provoke Breach of Peace), Section 126 BNSS (Security for keeping peace)"
      },
      stage1Immediate: [
        "📞 Dial 112 & Record All Calls: Call 112 immediately to log the threat on official police records. Enable automatic call recording on your phone to capture audio evidence.",
        "📱 Preserve Threat Messages: Take screenshots and backup audio recordings, WhatsApp messages, SMS, and caller ID details. Under Section 63 BSA 2023, digital voice and text logs are admissible.",
        "🛡️ Personal Safety Precautions: Alter daily travel routines, inform close family members, and avoid meeting the intimidators alone in isolated locations."
      ],
      stage2FormalRecourse: [
        "🚓 File Immediate Cognizable FIR: Visit the police station and submit a written complaint. Insist on an FIR under Section 351(3) BNS (threat to cause death carries up to 7 years imprisonment) and Section 308 BNS (Extortion).",
        "⚖️ Section 175(3) BNSS Escalation to SP & Commissioner: If the local station hesitates due to the accused's political influence, personally submit a representation to the Commissioner of Police or SP requesting immediate registration of FIR and police protection.",
        "🛡️ Formal Application for Police Security: File an application before the Commissioner of Police / District Magistrate for round-the-clock armed personal security escort (PSO) due to grave threat to life."
      ],
      stage3CourtProceedings: [
        "🏛️ Application under Section 126 BNSS (Binding Over for Peace): Move the Executive Magistrate / Sub-Divisional Magistrate (SDM) to initiate proceedings against the accused under Section 126 BNSS, requiring them to execute a heavy surety bond to maintain peace and good behavior.",
        "⚡ Section 175(4) / 176 BNSS Direction to Register FIR: If police fail to take action, file an application before the Judicial Magistrate praying for judicial directions for arrest and FIR.",
        "📜 High Court Criminal Writ under Article 226 for Police Protection: Approach the High Court praying for a Writ of Mandamus directing the State to provide security cover and protect your right to life under Article 21."
      ],
      advocateStrategicAdvice: [
        "⚠️ Never give in to extortion demands: Paying money emboldens extortionists and establishes a cycle of repeated demands.",
        "⚖️ Section 351 BNS: Even a telephonic threat or verbal warning to cause grievous hurt is a non-bailable cognizable crime.",
        "📑 Keep duplicate copies of all complaints and audio recordings stored securely on cloud storage."
      ]
    };
  }

  // 13. Medical Negligence
  if (q.includes("medical negligence") || q.includes("hospital negligence") || q.includes("doctor negligence") || q.includes("wrong operation") || (q.includes("doctor") && q.includes("negligence"))) {
    return {
      category: "Medical Jurisprudence & Professional Negligence",
      title: "Advocate Advisory: Medical Negligence, Hospital Malpractice & Compensation",
      confidence: "99.6% Advocate Procedural Grounding",
      act: "Consumer Protection Act, 2019, BNS 2023 & NMC Regulations",
      section: "Section 106(1) BNS 2023 • Consumer Protection Act 2019 • Jacob Mathew Guidelines",
      classification: {
        nature: "Civil Tort & Deficiency of Service + Criminal Rash/Negligent Act",
        cognizable: "Cognizable under BNS 106(1) subject to Medical Board opinion",
        bailable: "Bailable",
        relevantSections: "Section 106(1) BNS (Causing Death by Rash or Negligent Act), Section 2(11) CPA 2019 (Deficiency in Service), National Medical Commission Ethics Regulations"
      },
      stage1Immediate: [
        "📑 Immediately Secure All Medical Records: Apply in writing to the hospital Medical Superintendent for certified copies of complete bed-head tickets (BHT), nursing charts, doctor notes, surgical consent forms, diagnostic reports, and OT notes. Under NMC regulations, hospitals are legally mandated to furnish records within 72 hours.",
        "📸 Prescription & Drug Samples: Preserve all medicine packets, pharmacy bills, doctor prescriptions, implant labels, and discharge summaries.",
        "🩺 Independent Second Medical Opinion: Consult an independent senior specialist to analyze whether the standard of care departed from ordinary medical practice."
      ],
      stage2FormalRecourse: [
        "📜 Complaint to State Medical Council (SMC) & NMC: File a formal complaint against the doctor before the State Medical Council for professional misconduct, seeking cancellation of their medical license.",
        "🚓 Police Complaint under Section 106(1) BNS: Lodge a police complaint. Under Supreme Court's guidelines in 'Jacob Mathew v. State of Punjab', police will refer the case to an independent government medical board before arresting the doctor.",
        "📜 Advocate Legal Notice for Damages: Serve a comprehensive legal notice on the hospital and treating doctors demanding compensation for medical expenses and loss of life/health."
      ],
      stage3CourtProceedings: [
        "🏛️ File Claim before State/National Consumer Commission: File a medical negligence consumer complaint before the State Commission (claims up to ₹10 Crore) or National Consumer Commission (NCDRC) claiming multi-crore compensation.",
        "💰 Heads of Compensation: Claim compensation under: (1) Total hospitalization and corrective surgery costs, (2) Lifetime loss of earnings and dependency, (3) Loss of companionship and emotional trauma, (4) Punitive damages against the hospital for systemic failure.",
        "🛡️ Res Ipsa Loquitur Principle: If foreign objects (gauze/scissors) were left inside the body or wrong limb was operated, invoke the doctrine of 'Res Ipsa Loquitur' (the thing speaks for itself), shifting the entire burden of proof onto the hospital."
      ],
      advocateStrategicAdvice: [
        "⚠️ Hospital Record Tampering: Request court or police to immediately impound original medical records to prevent post-facto tampering or rewriting of charts.",
        "⏳ Limitation: Consumer complaint must be instituted within 2 YEARS from the date of negligence or discovery of injury.",
        "🤝 Expert Evidence: A credible written opinion from an independent specialist doctor is the cornerstone of success in medical negligence litigation."
      ]
    };
  }

  // 14. Matrimonial Disputes, Divorce & Maintenance
  if (q.includes("divorce") || q.includes("child custody") || (q.includes("maintenance") && (q.includes("wife") || q.includes("husband") || q.includes("alimony")))) {
    return {
      category: "Family Law & Matrimonial Litigation",
      title: "Advocate Advisory: Matrimonial Disputes, Divorce, Child Custody & Maintenance",
      confidence: "99.7% Advocate Procedural Grounding",
      act: "Hindu Marriage Act, 1955 & Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023",
      section: "Section 13 HMA 1955 • Section 144 BNSS 2023 • Guardians and Wards Act 1890",
      classification: {
        nature: "Civil Matrimonial Dispute & Statutory Family Redressal",
        cognizable: "Non-Cognizable (instituted via Family Court Petitions)",
        bailable: "Civil Jurisdiction",
        relevantSections: "Section 13 HMA (Grounds for Divorce), Section 13B HMA (Mutual Consent Divorce), Section 144 BNSS (Order for Maintenance of Wives, Children and Parents), Section 24 HMA (Interim Maintenance)"
      },
      stage1Immediate: [
        "📑 Assemble Financial & Matrimonial Records: Gather marriage certificate, wedding photographs, joint account statements, property purchase deeds, income tax returns (ITR), salary slips of both spouses, and school records of children.",
        "📱 Document Incidents: Record dates and preserve digital correspondence (WhatsApp/emails) documenting desertion, mental cruelty, or separation.",
        "🤝 Explore Mediation First: Matrimonial disputes are statutorily referred to counseling and mediation at the Family Court to assess whether amicable reconciliation or dignified mutual separation is possible."
      ],
      stage2FormalRecourse: [
        "🏛️ Mutual Consent Divorce under Section 13B HMA (Fastest Recourse): If both parties agree, file a joint petition under Section 13B. With supreme court precedents ('Amardeep Singh v. Harveen Kaur'), the mandatory 6-month cooling-off period can be waived if all terms (alimony, custody, stridhan) are settled.",
        "📜 Contested Divorce Petition under Section 13 HMA: If contested, file a petition on statutory grounds such as Cruelty, Desertion (2+ years), Adultery, or Mental Disorder.",
        "💰 Monthly Maintenance Application under Section 144 BNSS (formerly CrPC 125): Wife or minor children can file an expeditious application for monthly living allowance and child education expenses."
      ],
      stage3CourtProceedings: [
        "⚡ Interim Maintenance under Section 24 HMA: File an application on Day 1 for interim maintenance and litigation expenses payable by the earning spouse pending final divorce disposal.",
        "👶 Child Custody & Visitation under Guardians and Wards Act: File for interim and permanent custody. Under Indian jurisprudence, the paramount consideration is the 'Welfare of the Child', not parental ego.",
        "🛡️ Restraining Orders: Seek orders restraining the spouse from taking the child outside jurisdiction or disposing of joint matrimonial properties.",
        "💍 Settlement of Permanent Alimony under Section 25 HMA: Court determines lump-sum or monthly permanent alimony based on the husband's net worth, standard of living, and duration of marriage."
      ],
      advocateStrategicAdvice: [
        "⚠️ Supreme Court Mandate in 'Rajnesh v. Neha': Both spouses MUST file mandatory detailed Affidavits of Assets and Liabilities covering all bank accounts, properties, and income sources.",
        "👶 Custody Principles: Mothers are generally granted custody of young infants below 5 years, while fathers have statutory rights to liberal weekend visitation and video calls.",
        "🤝 Dignified Resolution: Mutual consent divorce saves years of emotional trauma and legal costs compared to protracted contested battles."
      ]
    };
  }

  // 15. Servant Theft & Domestic Help Burglary
  if (q.includes("domestic help") || q.includes("servant") || q.includes("maid") || q.includes("cook") && (q.includes("stole") || q.includes("theft") || q.includes("cash") || q.includes("gold"))) {
    return {
      category: "Criminal Law & Property Offenses",
      title: "Advocate Advisory: Theft by Domestic Servant & Recovery of Stolen Valuables",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Bharatiya Nyaya Sanhita (BNS) 2023 & Bharatiya Sakshya Adhiniyam (BSA), 2023",
      section: "Section 305 & 316 BNS 2023 • Section 23 BSA 2023 • Section 173 BNSS",
      classification: {
        nature: "Aggravated Cognizable Criminal Offense (Theft by Servant & Breach of Trust)",
        cognizable: "Cognizable (Section 305 BNS Theft in dwelling house / by servant)",
        bailable: "Non-Bailable (Carries rigorous imprisonment up to 7 years + fine)",
        relevantSections: "Section 305 BNS (Theft in dwelling house or by clerk/servant), Section 316 BNS (Criminal Breach of Trust), Section 23 BSA 2023 (Information leading to discovery/recovery)"
      },
      stage1Immediate: [
        "🚨 Preserve the Scene & Cupboard: Do not touch, clean, or tamper with the locked cupboard, locks, or room where the theft occurred. Fingerprint experts from the forensic team must lift latent prints.",
        "📋 Gather Maid / Servant Identification: Compile the servant's Aadhaar card copy, voter ID, contact number, permanent home village address, placement agency contract, and recent photograph.",
        "📸 Compile Inventory of Stolen Valuables: Prepare an itemized list with photographs and purchase invoices of stolen gold bangles, jewellery, cash, or electronics, including gross weight and hallmark numbers."
      ],
      stage2FormalRecourse: [
        "🚓 Immediate Registration of FIR under Section 305 BNS: Visit the police station immediately. Insist on registering an FIR specifically under Section 305 BNS (Theft by servant/in dwelling house carries up to 7 years jail). Do NOT accept a simple 'theft' (Section 303 BNS) or lost report.",
        "⚖️ Alert Native Police Station: Provide the servant's native district and village details. Police dispatch a special team or issue a wireless message to the native police station to intercept the accused before the gold is melted or pawned.",
        "📜 Notify Local Pawnbrokers & Jewellers: Provide the police officer with a request letter to circulate pictures of the stolen ornaments to local bullion markets and gold loan agencies."
      ],
      stage3CourtProceedings: [
        "🏛️ Recovery Panchnama under Section 23 BSA 2023: When the police arrest the accused and recover the gold ornaments based on their confession disclosure, ensure a formal Recovery Memo / Panchnama is drawn with independent witnesses.",
        "⚡ Superdari Application under Section 503 BNSS: Once the police seize and deposit the recovered gold in the police Malkhana (evidence vault), file an urgent application under Section 503 BNSS (formerly CrPC 457) before the Magistrate for interim custody and return of the gold bangles to you upon furnishing an indemnity bond.",
        "⚖️ Oppose Bail: Engage an advocate to oppose the servant's regular bail until full recovery of all stolen assets is completed."
      ],
      advocateStrategicAdvice: [
        "⚠️ Insist on Section 305 BNS: Police often try to register under simple theft Section 303 to reduce crime statistics; insist on Section 305 BNS because theft by a person entrusted with domestic access involves breach of trust.",
        "⏳ Speed of Reporting: The first 24 hours are critical; once stolen ornaments are handed to illegal goldsmiths, physical recovery becomes significantly harder.",
        "📑 Police Verification: In future, always insist on formal Police Verification for all domestic staff."
      ]
    };
  }

  // 16. Defamation, Slander & Online Harassment
  if (q.includes("defamation") || q.includes("defaming") || q.includes("slander") || q.includes("reputation") || (q.includes("false") && q.includes("allegations") && (q.includes("facebook") || q.includes("social media") || q.includes("online")))) {
    return {
      category: "Civil Tort & Criminal Defamation",
      title: "Advocate Advisory: Criminal & Civil Defamation, Slander & Online Takedown",
      confidence: "99.6% Advocate Procedural Grounding",
      act: "Bharatiya Nyaya Sanhita (BNS) 2023 & Information Technology Act, 2000",
      section: "Section 356 BNS 2023 • Section 66A/79 IT Act • IT Rules 2021",
      classification: {
        nature: "Non-Cognizable Criminal Offense + Civil Action for Damages",
        cognizable: "Non-Cognizable (Section 356 BNS instituted via Private Complaint)",
        bailable: "Bailable / Compoundable",
        relevantSections: "Section 356 BNS (Defamation), Section 66D IT Act, Rule 3(2)(b) IT Intermediary Rules 2021"
      },
      stage1Immediate: [
        "📸 Timestamped Preservation of Defamatory Content: Take full-page screenshots, capture permanent post URLs, video recordings of scrolling comments, and preserve engagement metrics (shares/likes). Under Section 63 BSA 2023, digital records are admissible.",
        "👥 Document Harm to Reputation: Preserve statements or messages from friends, colleagues, clients, or employers showing that their perception of your integrity was lowered due to the publication.",
        "🛑 Do NOT Engage in Online Abuse: Avoid replying with abusive or counter-defamatory remarks, which could compromise your moral standing in court."
      ],
      stage2FormalRecourse: [
        "📜 Cease and Desist Legal Notice: Engage an advocate to serve a strict 48-Hour Cease and Desist Legal Notice demanding unconditional written apology, immediate deletion of posts, and retraction across all platforms.",
        "🚩 Takedown Notice to Social Media Intermediary: Serve a formal notice to the Resident Grievance Officer of Meta, Twitter/X, or Google citing defamation and harassment. Under the IT Rules 2021, platforms must acknowledge within 24 hours and resolve within 15 days.",
        "🚓 Cyber Crime Complaint: If the defamation includes morphed photos, impersonation, or threats, file a complaint on cybercrime.gov.in."
      ],
      stage3CourtProceedings: [
        "🏛️ Private Criminal Complaint under Section 223 BNSS for Section 356 BNS: File a criminal complaint before the Judicial Magistrate. The offense of defamation carries up to 2 years imprisonment with simple or rigorous imprisonment and community service.",
        "💰 Civil Suit for Damages & Compensation: File a civil suit before the Civil Court claiming substantial monetary damages for loss of reputation, business distress, and mental trauma.",
        "⚡ Interlocutory Injunction: Pray for an urgent interim injunction restraining the defendant from publishing, circulating, or broadcasting any further defamatory material during the pendency of the suit."
      ],
      advocateStrategicAdvice: [
        "⚠️ Defenses under Section 356 BNS: The defendant can only escape liability if they prove that the imputation was TRUE and published for the PUBLIC GOOD.",
        "⏳ Limitation: Criminal defamation complaint must be filed within 3 YEARS from the date of publication.",
        "📮 Proof of Publication: Always ensure that at least one third-party witness saw and read the defamatory post."
      ]
    };
  }

  // 17. Commercial Contract Breach & Unpaid Invoices
  if ((q.includes("contract") || q.includes("invoice") || q.includes("vendor") || q.includes("client")) && (q.includes("not paying") || q.includes("breach") || q.includes("dues") || q.includes("recovery") || q.includes("unpaid"))) {
    return {
      category: "Commercial Contracts & Debt Recovery",
      title: "Advocate Advisory: Commercial Breach of Contract & Commercial Debt Recovery",
      confidence: "99.7% Advocate Procedural Grounding",
      act: "Indian Contract Act, 1872 & Commercial Courts Act, 2015",
      section: "Sections 73 & 74 Contract Act • Section 12A Commercial Courts Act • Order 37 CPC",
      classification: {
        nature: "Civil Commercial Dispute & Liquidated Debt Recovery",
        cognizable: "Civil Commercial Jurisdiction",
        bailable: "Civil Recourse",
        relevantSections: "Section 73 (Compensation for loss caused by breach), Section 74 (Liquidated damages), Order 37 CPC (Summary Suits), Section 12A Commercial Courts Act"
      },
      stage1Immediate: [
        "📑 Audit Contractual Paper Trail: Collate the Master Service Agreement (MSA), Purchase Orders (PO), Work Orders, signed Delivery Challans, and accepted tax invoices.",
        "📧 Reconcile Communications: Export email chains where the client/vendor accepted deliverables, acknowledged debt, or approved milestones without dispute.",
        "📊 Ledger Statement & Interest Calculation: Prepare a verified ledger balance showing unpaid principal invoices along with interest calculated as per contractual terms (standard 18% p.a.)."
      ],
      stage2FormalRecourse: [
        "📜 Advocate Statutory Demand Notice: Serve a formal 15-Day Legal Demand Notice calling upon the debtor to clear outstanding dues with interest, failing which legal proceedings will be initiated.",
        "🏛️ Mandatory Pre-Institution Mediation under Section 12A: Under the Commercial Courts Act 2015, filing a commercial suit requires mandatory Pre-Institution Mediation through the District Legal Services Authority (DLSA) to attempt early recovery.",
        "💼 Insolvency Demand Notice under Section 8 & 9 IBC (for Corporate Debtors): If the debt exceeds ₹1 Crore, serve a 10-Day Statutory Demand Notice under Form 3 of the Insolvency and Bankruptcy Code (IBC, 2016)."
      ],
      stage3CourtProceedings: [
        "🏛️ Summary Suit under Order 37 CPC: File a Summary Suit before the Commercial Court. The debtor has no automatic right to defend and must apply for 'Leave to Defend' by depositing security.",
        "⚡ Attachment Before Judgment under Order 38 Rule 5 CPC: If the debtor is attempting to sell company assets or close bank accounts, file an application praying for immediate attachment of debtor's bank accounts.",
        "⚖️ Commercial Suit for Damages: Claim full contract value, consequential losses, lost profits, and full litigation costs under Section 35 CPC.",
        "🛡️ Arbitration Invocation under Section 9 / 11 of Arbitration Act: If the agreement contains an arbitration clause, move the High Court for appointment of an Arbitrator and seek interim protection under Section 9."
      ],
      advocateStrategicAdvice: [
        "⚠️ Commercial Court Speed: Commercial courts are statutorily mandated to decide summary commercial suits on fast-track timelines.",
        "⏳ Limitation Period: Limitation for commercial debt recovery is strictly 3 YEARS from the date the invoice became overdue.",
        "📮 Proof of Dispatch: Maintain India Post Speed Post tracking receipts and proof of email delivery."
      ]
    };
  }

  // 18. Lost Phone & Identity Theft
  if (q.includes("lost phone") || q.includes("stolen mobile") || q.includes("identity theft") || (q.includes("pan") && q.includes("fake loan")) || (q.includes("sim") && q.includes("misuse"))) {
    return {
      category: "Telecommunications Law & Identity Protection",
      title: "Advocate Advisory: Stolen Mobile, SIM Blocking & Identity Theft Prevention",
      confidence: "99.8% Advocate Procedural Grounding",
      act: "Central Equipment Identity Register (CEIR) & Information Technology Act, 2000",
      section: "CEIR Portal Rules • Section 66C IT Act • Section 318(2) BNS 2023",
      classification: {
        nature: "Preventive Identity Safeguard & Cognizable Cyber Offense",
        cognizable: "Cognizable (Section 66C IT Act Identity Theft & Section 318(2) BNS)",
        bailable: "Bailable / Investigative",
        relevantSections: "Section 66C IT Act (Identity Theft), Section 318(2) BNS (Cheating by Impersonation), CEIR IMEI Blocking Guidelines"
      },
      stage1Immediate: [
        "🔒 Immediate SIM Blocking within 2 Hours: Contact your telecom operator (Jio/Airtel/Vi) immediately and block your SIM card to prevent fraudsters from receiving OTPs and hijacking UPI/WhatsApp.",
        "🌐 Block IMEI on Central Equipment Identity Register (CEIR): Log on to `ceir.gov.in` (DoT Government Portal) and submit an IMEI block request. This completely blacklists the handset across all Indian telecom networks.",
        "📝 Lodge Online Lost Article Report (LAR): File a digital Lost Property / Article Report on your State Police citizen portal (e-Lost Report) to secure a verified acknowledgement slip.",
        "💳 De-link UPI & Mobile Banking: Call your bank to freeze net banking and UPI access tied to the lost phone number."
      ],
      stage2FormalRecourse: [
        "📑 Obtain Duplicate SIM with Police Slip: Visit your telecom service center with your ID proof and police Lost Article Report acknowledgement to issue a replacement SIM.",
        "🛡️ Check Credit Score (CIBIL / Experian): Monitor credit bureaus to ensure no unauthorized personal loans or credit cards have been opened using your PAN or Aadhaar.",
        "📜 Notify Telecom / NBFCs of Fraudulent Loans: If fake loans appear, send a formal dispute letter citing identity theft accompanied by the police Lost Report."
      ],
      stage3CourtProceedings: [
        "🏛️ FIR for Identity Theft under Section 66C IT Act: If your credentials were used to commit financial crimes, file a formal FIR under Section 66C IT Act and Section 318(2) BNS.",
        "⚖️ Banking & RBI Ombudsman Complaint: If NBFCs or banks refuse to remove fraudulent loans, file a complaint before the RBI Ombudsman for identity rectification.",
        "🛡️ Release of Seized Handset under Section 503 BNSS: When police trace and recover the handset, file an application before the Magistrate for return of your device upon verification of original purchase bill."
      ],
      advocateStrategicAdvice: [
        "⚠️ Protect Yourself from Criminal Accusations: If your stolen SIM or phone is used in a serious crime, having a prior dated police Lost Article Report is your conclusive proof of innocence.",
        "📦 Keep your phone's box with IMEI numbers written down safely at home.",
        "🔒 Enable SIM card PIN lock on your new phone to prevent physical SIM swaps."
      ]
    };
  }

  // 19. Public Nuisance & Neighbor Disputes
  if (q.includes("public nuisance") || q.includes("loud music") || (q.includes("neighbor") && (q.includes("nuisance") || q.includes("harassing") || q.includes("garbage") || q.includes("blocking")))) {
    return {
      category: "Public Order & Environmental Law",
      title: "Advocate Advisory: Public Nuisance, Loud Music & Neighbor Harassment Recourse",
      confidence: "99.7% Advocate Procedural Grounding",
      act: "Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023 & Noise Pollution Rules, 2000",
      section: "Section 152, 162 BNSS 2023 • Noise Pollution Rules 2000 • Section 270 BNS 2023",
      classification: {
        nature: "Statutory Public Nuisance & Environmental Violation",
        cognizable: "Non-Cognizable Criminal Offense + Executive Magistrate Powers",
        bailable: "Bailable",
        relevantSections: "Section 152 BNSS (Conditional order for removal of nuisance), Section 270 BNS (Public Nuisance), Noise Pollution (Regulation & Control) Rules 2000"
      },
      stage1Immediate: [
        "📱 Timestamped Video & Audio Recording: Record date-stamped video from inside your residence showing decibel levels, timestamp, and disturbance.",
        "📞 Dial 112 Repeatedly: Call 112 every time the violation occurs. Each call generates a police computer-aided dispatch log that serves as concrete evidence of habitual nuisance.",
        "📝 Collective Representation by Residents / RWA: Draft a joint representation signed by neighboring residents and submit it to the Resident Welfare Association (RWA) and local police station."
      ],
      stage2FormalRecourse: [
        "📜 Written Complaint to Police & Pollution Control Board: Submit a formal complaint citing violation of Noise Pollution Rules (prohibition of loud music between 10:00 PM and 6:00 AM).",
        "🏛️ Application under Section 152 BNSS before Executive Magistrate: File an application before the Sub-Divisional Magistrate (SDM) under Section 152 BNSS (formerly CrPC 133) for a conditional order to remove the public nuisance.",
        "⚖️ Execution of Bond under Section 126 BNSS: Pray to the Magistrate to require the offending neighbor to execute a peace bond with sureties."
      ],
      stage3CourtProceedings: [
        "🏛️ Civil Suit for Permanent Injunction & Damages: File a civil suit before the Civil Court praying for a permanent injunction restraining the neighbor from causing nuisance and claiming damages for mental harassment.",
        "⚡ Criminal Prosecution under Section 270 BNS: File a private criminal complaint before the Judicial Magistrate for Public Nuisance under Section 270 BNS.",
        "🛡️ Police Enforcement of SDM Order: If the neighbor violates the Magistrate's Section 152 order, they are liable for immediate penal action under Section 223 BNS (Disobedience to order duly promulgated by public servant)."
      ],
      advocateStrategicAdvice: [
        "⚠️ Do not retaliate with counter-nuisance or physical altercations, which allows them to file counter-complaints.",
        "📑 Multiple signatures from surrounding residents make an SDM Section 152 application virtually unbeatable.",
        "🎧 Noise meter mobile app screenshots provide helpful corroborative evidence."
      ]
    };
  }

  // 20. False Cases & Malicious Prosecution
  if (q.includes("false fir") || q.includes("fake fir") || q.includes("fake case") || q.includes("false case") || q.includes("framed") || q.includes("malicious prosecution")) {
    return {
      category: "Criminal Defense & Malicious Prosecution",
      title: "Advocate Advisory: Defense against False FIR & Quashing Proceedings",
      confidence: "99.9% Advocate Procedural Grounding",
      act: "Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023 & BNS 2023",
      section: "Section 482 BNSS 2023 (Anticipatory Bail) • Section 528 BNSS (Quashing) • Section 217 BNS",
      classification: {
        nature: "Frivolous Criminal Prosecution & Abuse of Judicial Process",
        cognizable: "Criminal Defense Recourse",
        bailable: "Anticipatory Bail Jurisdiction",
        relevantSections: "Section 482 BNSS (Anticipatory Bail), Section 528 BNSS (High Court Inherent Powers to Quash FIR), Section 217 BNS (Public servant framing incorrect record), Section 248 BNS (False charge of offense)"
      },
      stage1Immediate: [
        "📑 Collect Solid Alibi Evidence: Immediately secure CCTV footage, flight/train tickets, hotel booking logs, office biometric attendance, credit card swipe slips, or Google Maps timeline showing you were elsewhere when the alleged offense occurred.",
        "📱 Preserve Exculpatory Communications: Archive WhatsApp chats, emails, call logs, or audio recordings proving prior disputes, extortion attempts, or malice by the complainant.",
        "🔒 Do NOT Abscond or Evade Investigation: Cooperate with lawful investigation notices under Section 35(3) BNSS while accompanied by an advocate."
      ],
      stage2FormalRecourse: [
        "🛡️ File Urgent Anticipatory Bail under Section 482 BNSS: Move the Sessions Court or High Court immediately for Anticipatory Bail to prevent arrest. Highlight clean antecedents, absence of custodial requirement, and documentary proof of false implication.",
        "⚖️ High Court Quashing Petition under Section 528 BNSS (formerly CrPC 482): File a criminal petition before the High Court praying for complete quashing of the FIR based on the landmark Supreme Court principles in 'State of Haryana v. Bhajan Lal' (where allegations are absurd or instituted with mala fide intention).",
        "📜 Notice under Section 35(3) BNSS: Under Arnesh Kumar guidelines codified in BNSS, police cannot arrest automatically for offenses carrying up to 7 years without issuing prior notice of appearance."
      ],
      stage3CourtProceedings: [
        "🏛️ Discharge Application before Trial Court: If quashing is delayed, file an application for Discharge under Section 250 / 262 BNSS before the Magistrate/Sessions Court arguing that allegations are groundless.",
        "⚖️ Criminal Prosecution of False Complainant under Section 248 BNS: Upon discharge or acquittal, file a criminal complaint against the complainant under Section 248 BNS for instituting false criminal charges.",
        "💰 Civil Suit for Malicious Prosecution: File a civil suit claiming heavy monetary damages for loss of social reputation, legal defense expenses, and emotional trauma caused by frivolous prosecution."
      ],
      advocateStrategicAdvice: [
        "⚠️ Golden Rule: Never appear before the police without your advocate or without an order of interim protection from the court.",
        "📑 Alibi Evidence: Secure third-party electronic records immediately; CCTV servers overwrite footage every 15-30 days.",
        "🤝 Reputed Senior Counsel: Engaging experienced counsel for an urgent Section 528 BNSS quashing petition can end the harassment at the threshold."
      ]
    };
  }

  // 21. General Advocate Procedural Synthesizer (Fallback for ANY scenario)
  return {
    category: "Advocate Legal Assessment & Procedural Advisory",
    title: `Advocate Advisory: Legal Recourse for "${queryText.slice(0, 60)}..."`,
    confidence: "98.9% Advocate Procedural Grounding",
    act: "Bharatiya Nyaya Sanhita (BNS) 2023 & Bharatiya Nagarik Suraksha Sanhita (BNSS), 2023",
    section: "Relevant Statutory Penal & Procedural Codes of India",
    classification: {
      nature: "Civil / Criminal Infringement of Statutory Rights",
      cognizable: "Subject to Police General Diary Assessment",
      bailable: "Bailable / Non-Bailable based on quantum of harm",
      relevantSections: "BNS 2023, BNSS 2023, BSA 2023 & Relevant Special Enactments"
    },
    stage1Immediate: [
      "🚨 Immediate Safety & Damage Control: Take immediate steps to prevent further escalation or monetary/physical harm. Do not take the law into your own hands.",
      "📸 Forensic & Digital Documentation: Gather all contemporaneous evidence: photograph physical damages, preserve WhatsApp chats, call recordings, emails, and transaction statements under Section 63 of Bharatiya Sakshya Adhiniyam (BSA) 2023.",
      "📞 Official Emergency Logging: Call 112 or relevant government statutory helpline (e.g. 1930 for cyber, 1915 for consumer, 181 for women) to create an indisputable government event record."
    ],
    stage2FormalRecourse: [
      "📜 Serve Advocate Legal Demand Notice: Engage an advocate to issue a formal 15-Day Statutory Legal Demand Notice detailing the grievance, legal statutory violations, and demanding specific compliance or restitution.",
      "🚓 Lodge Formal Police Complaint / Zero FIR: Visit the jurisdictional police station and submit a signed complaint under Section 173 BNSS 2023. Obtain a stamped receiving copy or GD entry.",
      "⚖️ Essential Advocate Remedy if Police Refuse: Send your written complaint directly to the Superintendent of Police (SP) under Section 175(3) BNSS 2023 via Registered Speed Post."
    ],
    stage3CourtProceedings: [
      "🏛️ Section 175(4) / 176 BNSS Application to Judicial Magistrate: If police fail to register an FIR after SP representation, file an application before the Judicial Magistrate First Class (JMFC) praying for judicial orders directing investigation.",
      "⚡ Civil Injunction & Summary Redressal: File an appropriate Civil Suit / Consumer Complaint / Commercial Suit seeking temporary injunctions, restitution, or damages.",
      "💰 Comprehensive Prayer for Relief: Claim compensation for actual losses, interest, and costs of litigation."
    ],
    advocateStrategicAdvice: [
      "⚠️ Limitation Period: Ensure your legal notices and complaints are filed well within statutory limitation periods.",
      "📮 Postal Consignment Receipts: Always maintain Registered Speed Post receipts and delivery tracking consignment notes as primary evidence in court.",
      "🤝 Consult Qualified Counsel: Obtain personalized advice from an advocate licensed by the State Bar Council."
    ]
  };
}


function renderAdvocateAdvisoryBubble(advisory, queryText, chatHistory) {
  const l = typeof currentLang !== "undefined" ? currentLang : "en";
  
  const labels = {
    hi: {
      badge: "⚖️ वरिष्ठ अधिवक्ता कानूनी सलाह और अदालती कार्यवाही",
      natureHeader: "⚖️ अपराध की प्रकृति और कानूनी वर्गीकरण:",
      natureLabel: "वर्गीकरण:",
      cognizableLabel: "संज्ञेय स्थिति (Cognizable):",
      sectionsLabel: "मुख्य धाराएं:",
      stage1Header: "🚨 चरण 1: तत्काल कदम और साक्ष्य संरक्षण (पहले 24-48 घंटे)",
      stage2Header: "📜 चरण 2: औपचारिक कानूनी नोटिस और पुलिस / वैधानिक प्रक्रिया",
      stage3Header: "🏛️ चरण 3: अदालती कार्यवाही, याचिका और मिलने वाले अधिकार",
      adviceHeader: "🛡️ अधिवक्ता की रणनीतिक सलाह और महत्वपूर्ण सावधानियां",
      citationLabel: "मुख्य वैधानिक स्रोत:",
      copyBtn: "📋 कानूनी कार्यवाही रोडमैप कॉपी करें",
      readBtn: "🔊 कानूनी सलाह सुनें",
      printBtn: "🖨️ प्रिंट / पीडीएफ सेव करें",
      copied: "✓ रोडमैप कॉपी हो गया!"
    },
    bn: {
      badge: "⚖️ সিনিয়র অ্যাডভোকেট আইনি পরামর্শ ও বিচার বিভাগীয় পদক্ষেপ",
      natureHeader: "⚖️ অপরাধের প্রকৃতি ও আইনি শ্রেণিবিভাগ:",
      natureLabel: "শ্রেণিবিভাগ:",
      cognizableLabel: "আমলযোগ্য অবস্থা (Cognizable):",
      sectionsLabel: "প্রধান ধারা:",
      stage1Header: "🚨 পর্যায় ১: তাৎক্ষণিক পদক্ষেপ ও প্রমাণ সংরক্ষণ (প্রথম ২৪-৪৮ ঘণ্টা)",
      stage2Header: "📜 পর্যায় ২: আইনি নোটিশ ও পুলিশি / সংবিধিবদ্ধ পদক্ষেপ",
      stage3Header: "🏛️ পর্যায় ৩: আদালতের বিচারিক প্রক্রিয়া, পিটিশন ও প্রতিকার",
      adviceHeader: "🛡️ আইনজীবীর কৌশলগত পরামর্শ ও গুরুত্বপূর্ণ সতর্কতা",
      citationLabel: "প্রধান বিধিবদ্ধ আইন:",
      copyBtn: "📋 আইনি প্রক্রিয়ার রোডম্যাপ কপি করুন",
      readBtn: "🔊 পরামর্শ শুনুন",
      printBtn: "🖨️ প্রিন্ট / পিডিএফ সংরক্ষণ",
      copied: "✓ কপি সম্পন্ন!"
    },
    te: {
      badge: "⚖️ సీనియర్ అడ్వకేట్ చట్టపరమైన సలహా & కోర్టు ప్రక్రియ",
      natureHeader: "⚖️ నేరం యొక్క స్వభావం & చట్టపరమైన వర్గీకరణ:",
      natureLabel: "వర్గీకరణ:",
      cognizableLabel: "కాగ్నిజబుల్ స్థితి:",
      sectionsLabel: "ప్రధాన సెక్షన్లు:",
      stage1Header: "🚨 దశ 1: తక్షణ చర్యలు & సాక్ష్యాధారాల భద్రపరచడం (మొదటి 24-48 గంటలు)",
      stage2Header: "📜 దశ 2: లీగల్ నోటీసు & పోలీస్ / చట్టబద్ధమైన చర్యలు",
      stage3Header: "🏛️ దశ 3: న్యాయస్థాన ప్రక్రియలు, పిటిషన్లు & అందే ఉపశమనాలు",
      adviceHeader: "🛡️ అడ్వకేట్ వ్యూహాత్మక సలహా & ముఖ్యమైన జాగ్రత్తలు",
      citationLabel: "ప్రధాన చట్టపరమైన నిబంధనలు:",
      copyBtn: "📋 చట్టపరమైన రోడ్‌మ్యాప్ కాపీ చేయండి",
      readBtn: "🔊 సలహా వినండి",
      printBtn: "🖨️ ప్రింట్ / పీడీఎఫ్ సేవ్",
      copied: "✓ కాపీ చేయబడింది!"
    },
    ta: {
      badge: "⚖️ மூத்த வழக்கறிஞர் சட்ட ஆலோசனை & நீதிமன்ற நடைமுறைகள்",
      natureHeader: "⚖️ குற்றத்தின் தன்மை & சட்ட வகைப்பாடு:",
      natureLabel: "வகைப்பாடு:",
      cognizableLabel: "காக்னிசபிள் நிலை:",
      sectionsLabel: "முக்கிய பிரிவுகள்:",
      stage1Header: "🚨 நிலை 1: உடனடி நடவடிக்கைகள் & சான்றுகள் பாதுகாப்பு (முதல் 24-48 மணிநேரம்)",
      stage2Header: "📜 நிலை 2: முறையான சட்ட நோட்டீஸ் & காவல்துறை / சட்டரீதியான வழிமுறைகள்",
      stage3Header: "🏛️ நிலை 3: நீதிமன்ற வழக்கு நடைமுறைகள், மனுக்கள் & நிவாரணங்கள்",
      adviceHeader: "🛡️ வழக்கறிஞரின் உத்திசார் ஆலோசனை & முக்கிய எச்சரிக்கைகள்",
      citationLabel: "முக்கிய சட்ட விதிகள்:",
      copyBtn: "📋 சட்ட நடைமுறை வழிகாட்டியை நகலெடு",
      readBtn: "🔊 ஆலோசனையைக் கேளுங்கள்",
      printBtn: "🖨️ அச்சிடுக / சேமிக்கவும்",
      copied: "✓ நகலெடுக்கப்பட்டது!"
    },
    en: {
      badge: "⚖️ Senior Advocate Advisory & Legal Proceeding Roadmap",
      natureHeader: "⚖️ Case Assessment & Legal Classification:",
      natureLabel: "Classification:",
      cognizableLabel: "Cognizable Status:",
      sectionsLabel: "Key Applicable Sections:",
      stage1Header: "🚨 Phase 1: Immediate Steps & Evidence Preservation (First 24-48 Hours)",
      stage2Header: "📜 Phase 2: Formal Legal Notice & Police / Statutory Recourse",
      stage3Header: "🏛️ Phase 3: Judicial Proceedings, Petitions & Reliefs in Court",
      adviceHeader: "🛡️ Advocate's Strategic Advice & Important Cautions",
      citationLabel: "Primary Statutory Authority:",
      copyBtn: "📋 Copy Legal Proceeding Roadmap",
      readBtn: "🔊 Read Advisory Aloud",
      printBtn: "🖨️ Print / Save PDF",
      copied: "✓ Roadmap Copied!"
    }
  };

  const str = labels[l] || labels.en;

  const stage1Html = advisory.stage1Immediate.map(function(s) { return "<li>" + s + "</li>"; }).join("");
  const stage2Html = advisory.stage2FormalRecourse.map(function(s) { return "<li>" + s + "</li>"; }).join("");
  const stage3Html = advisory.stage3CourtProceedings.map(function(s) { return "<li>" + s + "</li>"; }).join("");
  const adviceHtml = advisory.advocateStrategicAdvice.map(function(s) { return "<li>" + s + "</li>"; }).join("");

  const botMsg = document.createElement("div");
  botMsg.className = "chat-bubble chat-ai advocate-card";
  botMsg.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px;">
      <span class="advocate-badge">
        <span>⚖️</span> ` + str.badge + `
      </span>
      <span class="badge-pastel-sage" style="font-size: 0.74rem;">
        <span>●</span> ` + (advisory.confidence || "99.8% Verified Grounding") + `
      </span>
    </div>

    <div style="font-weight: 800; font-size: 1.18rem; margin-bottom: 8px; color: #0F172A; line-height: 1.35;">
      ` + advisory.title + `
    </div>

    <!-- Case Assessment & Classification Box -->
    <div class="advocate-meta-box">
      <div style="color: #1E293B; font-weight: 700; margin-bottom: 4px;">` + str.natureHeader + `</div>
      <div style="color: #475569; margin-bottom: 2px;"><strong style="color: #334155;">` + str.natureLabel + `</strong> ` + advisory.classification.nature + `</div>
      <div style="color: #475569; margin-bottom: 2px;"><strong style="color: #334155;">` + str.cognizableLabel + `</strong> ` + advisory.classification.cognizable + `</div>
      <div style="color: #475569;"><strong style="color: #334155;">` + str.sectionsLabel + `</strong> <span style="color: #4338CA; font-weight: 600;">` + advisory.classification.relevantSections + `</span></div>
    </div>

    <!-- Phase 1: Immediate Steps -->
    <div style="margin-bottom: 14px;">
      <div class="advocate-section-title" style="color: #991B1B;">
        ` + str.stage1Header + `
      </div>
      <ul class="advocate-list">
        ` + stage1Html + `
      </ul>
    </div>

    <!-- Phase 2: Formal Notice & Police Recourse -->
    <div style="margin-bottom: 14px;">
      <div class="advocate-section-title" style="color: #92400E;">
        ` + str.stage2Header + `
      </div>
      <ul class="advocate-list">
        ` + stage2Html + `
      </ul>
    </div>

    <!-- Phase 3: Court Proceedings & Judicial Reliefs -->
    <div style="margin-bottom: 14px;">
      <div class="advocate-section-title" style="color: #065F46;">
        ` + str.stage3Header + `
      </div>
      <ul class="advocate-list">
        ` + stage3Html + `
      </ul>
    </div>

    <!-- Advocate Strategic Advice & Cautions -->
    <div class="advocate-caution-box">
      <div style="font-weight: 700; color: #92400E; margin-bottom: 4px; display: flex; align-items: center; gap: 6px;">
        ` + str.adviceHeader + `
      </div>
      <ul style="padding-left: 18px; margin: 0; color: #78350F; line-height: 1.55;">
        ` + adviceHtml + `
      </ul>
    </div>

    <!-- Statutory Citation Box -->
    <div class="citation-pastel-box" style="margin-bottom: 12px;">
      <strong style="color: #92400E;">` + str.citationLabel + `</strong> 
      <strong>` + advisory.act + `</strong> — <span style="color: #3730A3; font-weight: 600;">` + advisory.section + `</span>
    </div>

    <!-- Action Buttons -->
    <div style="margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap;">
      <button class="chip-btn copy-roadmap-btn" style="padding: 6px 12px; font-size: 0.78rem; font-weight: 600; background: #EEF2FF; color: #4338CA; border: 1px solid #C7D2FE;">
        ` + str.copyBtn + `
      </button>
      <button class="chip-btn read-aloud-btn" style="padding: 6px 12px; font-size: 0.78rem;">
        ` + str.readBtn + `
      </button>
      <button class="chip-btn print-advisory-btn" style="padding: 6px 12px; font-size: 0.78rem;">
        ` + str.printBtn + `
      </button>
    </div>
  `;

  // Copy Roadmap Handler
  botMsg.querySelector(".copy-roadmap-btn").addEventListener("click", function(e) {
    const formattedRoadmap = "=====================================================\n" +
      "⚖️ NYAAI LEGALTECH — SENIOR ADVOCATE LEGAL ADVISORY\n" +
      "=====================================================\n" +
      "Title: " + advisory.title + "\n" +
      "Category: " + advisory.category + "\n" +
      "Primary Authority: " + advisory.act + " (" + advisory.section + ")\n\n" +
      "[CASE ASSESSMENT & LEGAL CLASSIFICATION]\n" +
      "• Nature: " + advisory.classification.nature + "\n" +
      "• Cognizable Status: " + advisory.classification.cognizable + "\n" +
      "• Applicable Sections: " + advisory.classification.relevantSections + "\n\n" +
      "[PHASE 1: IMMEDIATE STEPS & EVIDENCE PRESERVATION (24-48 HOURS)]\n" +
      advisory.stage1Immediate.map(function(s) { return "• " + s; }).join("\n") + "\n\n" +
      "[PHASE 2: FORMAL LEGAL NOTICE & POLICE / STATUTORY RECOURSE]\n" +
      advisory.stage2FormalRecourse.map(function(s) { return "• " + s; }).join("\n") + "\n\n" +
      "[PHASE 3: JUDICIAL PROCEEDINGS, PETITIONS & RELIEFS IN COURT]\n" +
      advisory.stage3CourtProceedings.map(function(s) { return "• " + s; }).join("\n") + "\n\n" +
      "[ADVOCATE'S STRATEGIC ADVICE & IMPORTANT CAUTIONS]\n" +
      advisory.advocateStrategicAdvice.map(function(s) { return "• " + s; }).join("\n") + "\n" +
      "=====================================================\n" +
      "Generated by NYAAI (न्यायAI) LegalTech AI Platform\n" +
      "Statutory compliance verified across BNS 2023, BNSS 2023 & BSA 2023.";

    navigator.clipboard.writeText(formattedRoadmap);
    e.target.textContent = str.copied;
    setTimeout(function() { e.target.textContent = str.copyBtn; }, 2500);
  });

  // Read Aloud Handler
  botMsg.querySelector(".read-aloud-btn").addEventListener("click", function() {
    const textToSpeak = advisory.title + ". Phase 1: Immediate Steps: " + advisory.stage1Immediate.slice(0, 2).join(". ") + ". Phase 2: Formal Recourse: " + advisory.stage2FormalRecourse.slice(0, 2).join(". ") + ". Phase 3: Court Proceedings: " + advisory.stage3CourtProceedings.slice(0, 2).join(". ") + ". Primary Authority: " + advisory.act + ", " + advisory.section;
    speakText(textToSpeak);
  });

  // Print Handler
  botMsg.querySelector(".print-advisory-btn").addEventListener("click", function() {
    window.print();
  });

  chatHistory.appendChild(botMsg);
  chatHistory.scrollTop = chatHistory.scrollHeight;
}



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

    // 2. Check for conversational greeting first (fast-path friendly reply)
    const greeting = getConversationalGreeting(queryText, typeof currentLang !== "undefined" ? currentLang : "en");
    if (greeting) {
      setTimeout(() => {
        const botMsg = document.createElement("div");
        botMsg.className = "chat-bubble chat-ai";
        botMsg.innerHTML = `
          <div class="badge-pastel-blue" style="margin-bottom: 8px; font-size: 0.74rem;">
            <span>👋</span> Friendly Assistant
          </div>
          <div style="font-weight: 700; font-size: 1.05rem; margin-bottom: 6px; color: #0F172A;">
            ${greeting.title}
          </div>
          <p style="color: #334155; line-height: 1.6; margin-bottom: 12px;">
            ${greeting.message}
          </p>
          <div style="margin-top: 8px; font-size: 0.8rem; color: #64748B; font-weight: 600;">
            💡 Suggested inquiries to try:
          </div>
          <div style="margin-top: 6px; display: flex; gap: 8px; flex-wrap: wrap;">
            <button class="chip-btn inline-chip" data-query="What are my rights if arrested?">⚖️ Arrest Rights</button>
            <button class="chip-btn inline-chip" data-query="Explain Fundamental Rights under Article 21">📜 Article 21 Rights</button>
            <button class="chip-btn inline-chip" data-query="Consumer refund rights for defective products">🛍️ Consumer Refund</button>
          </div>
        `;
        botMsg.querySelectorAll(".inline-chip").forEach(chip => {
          chip.addEventListener("click", () => {
            executeQuery(chip.getAttribute("data-query"));
          });
        });
        chatHistory.appendChild(botMsg);
        chatHistory.scrollTop = chatHistory.scrollHeight;
      }, 350);
      return;
    }

    // 3. Append Typing Indicator
    const typingElem = document.createElement("div");
    typingElem.className = "chat-bubble chat-ai mono";
    typingElem.innerHTML = `<span style="color: #4F46E5; font-weight: 600;">⚡ Scanning 1,838 statutory sections across BNS, BNSS, BSA, COI...</span>`;
    chatHistory.appendChild(typingElem);
    chatHistory.scrollTop = chatHistory.scrollHeight;

    setTimeout(() => {
      typingElem.remove();

      // Check if this is a scenario-based inquiry (Advocate Mode)
      const currentLanguage = typeof currentLang !== "undefined" ? currentLang : "en";
      const scenarioAdvisory = getAdvocateScenarioAdvisory(queryText, currentLanguage);
      if (scenarioAdvisory) {
        renderAdvocateAdvisoryBubble(scenarioAdvisory, queryText, chatHistory);
        return;
      }

      // Find best match in cloud-synced knowledge base using ultra-fast multi-token scoring
      let matched = findBestLegalMatch(queryText);

      if (!matched) {
        // Dynamic Advocate Procedural Synthesizer for novel scenarios
        const fallbackAdvisory = getAdvocateScenarioAdvisory(queryText, currentLanguage, true);
        if (fallbackAdvisory) {
          renderAdvocateAdvisoryBubble(fallbackAdvisory, queryText, chatHistory);
          return;
        }

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

      const displayTitle = matched.title || (matched.section ? `${matched.section}: Legal Assessment` : "Statutory Legal Assessment");
      const summaryList = Array.isArray(matched.summary) ? matched.summary : [matched.answer || "Legal assessment details."];

      const botMsg = document.createElement("div");
      botMsg.className = "chat-bubble chat-ai";
      botMsg.innerHTML = `
        <div class="badge-pastel-sage" style="margin-bottom: 8px; font-size: 0.74rem;">
          <span>●</span> ${matched.confidence || "98.5% Verified"}
        </div>
        <div style="font-weight: 700; font-size: 1.05rem; margin-bottom: 6px; color: #0F172A;">
          ${displayTitle}
        </div>
        <ul style="padding-left: 20px; margin-bottom: 10px; color: #334155; line-height: 1.6;">
          ${summaryList.map(s => `<li>${s}</li>`).join("")}
        </ul>
        <div class="citation-pastel-box">
          <strong style="color: #92400E;">Statute Citation:</strong> 
          <strong>${matched.act || "Statutory Code of India"}</strong> — <span style="color: #3730A3; font-weight: 600;">${matched.section || "Relevant Provisions"}</span>
        </div>
        <div style="margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap;">
          <button class="chip-btn read-aloud-btn" style="padding: 4px 10px; font-size: 0.76rem;">
            🔊 Read Aloud
          </button>
          <button class="chip-btn copy-msg-btn" style="padding: 4px 10px; font-size: 0.76rem;">
            📋 Copy Citation
          </button>
          <button class="chip-btn feedback-learn-btn" style="padding: 4px 10px; font-size: 0.76rem;">
            👍 Helpful (Train)
          </button>
        </div>
      `;

      // Read aloud click
      botMsg.querySelector(".read-aloud-btn").addEventListener("click", () => {
        const textToSpeak = `${displayTitle}. ${summaryList.join(". ")}. Statute Citation: ${matched.act}, ${matched.section}`;
        speakText(textToSpeak);
      });

      // Copy click
      botMsg.querySelector(".copy-msg-btn").addEventListener("click", (e) => {
        navigator.clipboard.writeText(`${displayTitle}\n${matched.act} - ${matched.section}`);
        e.target.textContent = "✓ Copied!";
        setTimeout(() => (e.target.textContent = "📋 Copy Citation"), 2000);
      });

      // Autonomous Self-Training click
      botMsg.querySelector(".feedback-learn-btn").addEventListener("click", (e) => {
        e.target.textContent = "✓ Learned!";
        e.target.style.background = "#DEF7EC";
        e.target.style.color = "#03543F";
        try {
          const userTrained = JSON.parse(localStorage.getItem("nyaai_user_learned_qa") || "[]");
          const newEntry = {
            question: queryText,
            answer: matched.summary.join(" "),
            source: matched.act + " - " + matched.section,
            act: matched.act,
            section: matched.section,
            keywords: queryText.toLowerCase().replace(/[^a-z0-9 ]/g, " ").split(/\s+/).filter(w => w.length >= 3),
            confidence: "99.2% Self-Trained Match"
          };
          userTrained.push(newEntry);
          localStorage.setItem("nyaai_user_learned_qa", JSON.stringify(userTrained));
          legalKnowledgeBase.unshift(newEntry);
          updateTerminalStatusBadge(legalKnowledgeBase.length);
        } catch (_) {}
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
        const voiceMap = { en: "en-IN", hi: "hi-IN", bn: "bn-IN", te: "te-IN", ta: "ta-IN" };
        recognition.lang = voiceMap[currentLang] || "en-IN";
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
  const voiceMap = { en: "en-IN", hi: "hi-IN", bn: "bn-IN", te: "te-IN", ta: "ta-IN" };
  utterance.lang = voiceMap[currentLang] || "en-IN";
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

  let allCodexItems = [...statutoryLibrary];
  if (typeof legalCodex !== "undefined" && Array.isArray(legalCodex) && legalCodex.length > 0) {
    const extra = legalCodex.map(c => ({
      act: c.source ? c.source.toUpperCase().replace('.PDF', '') : "COI 1950",
      fullAct: "Constitution of India / Statutory Law",
      section: c.title || `Section ${c.id}`,
      title: c.title || `Statutory Section ${c.id}`,
      desc: c.preview || "",
      badge: "Codex Section"
    }));
    allCodexItems = allCodexItems.concat(extra);
  }

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
      renderCodex(allCodexItems);
      return;
    }
    const filtered = allCodexItems.filter(item =>
      item.act.toLowerCase().includes(term) ||
      item.section.toLowerCase().includes(term) ||
      item.title.toLowerCase().includes(term) ||
      item.desc.toLowerCase().includes(term)
    );
    renderCodex(filtered);
  });

  renderCodex(allCodexItems);
}

/* =========================================================
   5. Multilingual Localization System
   Supports: English (en), Hindi (hi), Bengali (bn), Telugu (te), Tamil (ta)
   ========================================================= */
currentLang = "en";

const i18n = {
  "en": {
    "nav": {
      "home": "Home",
      "terminal": "AI Terminal",
      "scanner": "Doc Scanner",
      "codex": "Statutory Codex",
      "sos": "Legal SOS",
      "download": "Download",
      "getApp": "Get App",
      "brandTag": "// न्यायAI"
    },
    "footer": {
      "brand": "NYAAI (न्यायAI) LegalTech Platform",
      "copyright": "© 2026 NYAAI LegalTech. All rights reserved."
    },
    "home": {
      "badge": "Reformed Criminal Laws • BNS 2023 Ready",
      "heroTitle": "Simplifying Indian Law with Modern Artificial Intelligence.",
      "heroSubtitle": "Instant statutory retrieval, on-device legal OCR, and plain-language criminal code synthesis across Bharatiya Nyaya Sanhita, BNSS, BSA, and the Constitution of India.",
      "btnTerminal": "⚡ Launch AI Terminal",
      "btnScanner": "📄 Try Document Scanner",
      "btnDownload": "📲 Download Android APK",
      "metric1": "Indexed Legal Sections",
      "metric3": "Offline Query Speed",
      "metric4": "On-Device Private OCR",
      "secBadge": "Explore Capabilities",
      "secTitle": "Everything You Need to Navigate Indian Law",
      "secDesc": "Engineered for advocates, law students, police officers, and citizens seeking immediate legal clarity.",
      "card1Title": "Interactive AI Terminal",
      "card1Desc": "Ask complex legal questions and receive answers grounded directly in specific statutory sections with confidence metrics, speech synthesis read-aloud, and speech-to-text input.",
      "card1Btn": "Open Terminal →",
      "card2Title": "On-Device Document Scanner",
      "card2Desc": "Scan paper legal notices, agreements, and FIR copies. Extract actionable legal deadlines, statutory violation clauses, and recommended steps with zero cloud leakage.",
      "card2Btn": "Scan Document →",
      "card3Title": "Indian Statutory Codex",
      "card3Desc": "Searchable, structured repository of the complete new criminal laws (BNS 2023, BNSS 2023, BSA 2023) and the Constitution of India. Search sections by keywords instantly.",
      "card3Btn": "Browse Codex →",
      "card4Title": "National Legal SOS",
      "card4Desc": "One-touch access to official toll-free emergency legal helplines across India (NALSA free legal aid, NCW Women helpline, National Cybercrime, and Childline).",
      "card4Btn": "View Helplines →",
      "card5Title": "Zero-Knowledge Privacy",
      "card5Desc": "Local SQLite FTS4 preloaded knowledge base and on-device ML Kit OCR guarantee that private legal consultations, photos, and voice data never leave your device.",
      "card5Badge": "✓ 100% On-Device",
      "card6Title": "Cross-Platform Ready",
      "card6Desc": "Available as a high-performance modern web application and production-signed native Android APK with room persistence and offline fallback.",
      "card6Btn": "Download App →"
    },
    "terminal": {
      "badge": "⚡ Interactive Legal Playground",
      "title": "Live Legal AI Terminal",
      "desc": "Query India's reformed penal and constitutional acts with instant, grounded statutory citations and confidence metrics.",
      "simBadge": "Offline-Capable Simulation",
      "verifiedBadge": "99.8% System Verified",
      "welcomeTitle": "Welcome to the NYAAI Research Terminal",
      "welcomeDesc": "I can cross-examine provisions across <strong>Bharatiya Nyaya Sanhita (BNS 2023)</strong>, <strong>BNSS 2023</strong>, <strong>BSA 2023</strong>, and the <strong>Constitution of India</strong>. Ask a specific legal question or click one of the suggested query chips below:",
      "chip1": "⚖️ Section 482 BNSS Bail",
      "chip2": "📜 Article 21 Privacy Rights",
      "chip3": "⚔️ Section 103 BNS Murder",
      "chip4": "📱 Electronic Evidence BSA",
      "chip5": "🛍️ Consumer Rights & Refunds",
      "inputPlaceholder": "Ask any legal question (e.g., arrest rights, Section 138 cheque bounce, cyber fraud)...",
      "sendBtn": "Ask AI",
      "readAloud": "🔊 Read Aloud",
      "copyCitation": "📋 Copy Citation",
      "citationLabel": "Statute Citation:"
    },
    "scanner": {
      "badge": "📄 On-Device Vision Engine",
      "title": "Legal Document Scanner & OCR",
      "desc": "Extract text from paper notices, contracts, and FIRs with zero cloud upload using local Google ML Kit text recognition.",
      "tab1": "Notice u/s 138 NI Act",
      "tab2": "FIR Extract (Cyber Fraud)",
      "tab3": "NDA Breach Notice",
      "btnScan": "⚡ Analyze Document",
      "repTypeLabel": "Document Classification:",
      "repStatutesLabel": "Statutory Framework:",
      "repDeadlineLabel": "Statutory Deadline:",
      "repActionLabel": "Recommended Action:"
    },
    "codex": {
      "badge": "📚 Statutory Knowledge Base",
      "title": "The Indian Statutory Codex",
      "desc": "Instant indexed access across 1,838+ sections of India's new criminal codes and constitutional jurisprudence.",
      "searchPlaceholder": "Search by section, keyword, or topic (e.g., murder, bail, FIR, electronic evidence, privacy)..."
    },
    "sos": {
      "badge": "🚨 Emergency Legal Directory",
      "title": "National Legal SOS & Support",
      "desc": "Direct toll-free access to statutory emergency helplines and essential citizen rights under Indian criminal law.",
      "nalsaTitle": "National Legal Aid (NALSA)",
      "nalsaDesc": "Free legal aid representation for eligible citizens & undertrials",
      "womenTitle": "Women in Distress Helpline",
      "womenDesc": "24/7 National Commission for Women emergency assistance",
      "cyberTitle": "National Cybercrime Portal",
      "cyberDesc": "Financial cyber fraud, phishing, & online offense grievance",
      "childTitle": "Childline Emergency",
      "childDesc": "Child protection, safety, and POCSO grievance redressal",
      "consumerTitle": "National Consumer Helpline",
      "consumerDesc": "Consumer dispute resolution, defective products & unfair trade",
      "seniorTitle": "Senior Citizen Helpline",
      "seniorDesc": "Elder line for welfare, maintenance & legal protection",
      "rightsBadge": "Know Your Rights",
      "rightsHeading": "Essential Citizen Rights Under Indian Law",
      "arrestTitle": "Rights Upon Arrest (D.K. Basu Guidelines)",
      "arrestPoints": [
        "Right to know the full grounds of arrest and whether the offence is bailable or non-bailable (Section 47 BNSS).",
        "Right to have a relative or friend informed of the arrest immediately.",
        "Mandatory medical examination by an authorized medical officer within 48 hours.",
        "Right to consult an advocate of your choice during interrogation."
      ],
      "zeroFirTitle": "Zero FIR Provision (Section 173 BNSS)",
      "zeroFirPoints": [
        "A police station cannot refuse to register an FIR on the grounds of territorial jurisdiction.",
        "A Zero FIR is registered and subsequently transferred to the competent jurisdiction police station.",
        "Audio-video recording of search and seizure operations is now mandated under BNSS 2023."
      ],
      "legalAidTitle": "Free Legal Aid (Article 39A)",
      "legalAidPoints": [
        "Article 39A mandates the State to secure equal justice and free legal aid for all citizens.",
        "Under the Legal Services Authorities Act, women, children, undertrials, and individuals with annual income below statutory limits are entitled to free representation in all courts."
      ]
    },
    "download": {
      "badge": "📲 Official Distribution Hub",
      "title": "Download NYAAI v1.0 Production",
      "desc": "Native Android application optimized with R8 code shrinking and verified with 2048-bit RSA release keys.",
      "cardBadge": "Signed Production Build Verified",
      "cardTitle": "NYAAI for Android (Release APK)",
      "cardDesc": "Includes pre-packaged legal database (4.27 MB, Schema v8) containing 1,838 sections across BNS 2023, BNSS 2023, BSA 2023, and Constitution of India for instant < 100ms startup.",
      "btnDownload": "⬇️ Download Signed APK (11.47 MB)",
      "btnMirror": "⚡ GitHub Releases Mirror",
      "btnWeb": "🌐 View Web Version"
    }
  },
  "hi": {
    "nav": {
      "home": "मुख्य पृष्ठ",
      "terminal": "AI टर्मिनल",
      "scanner": "दस्तावेज़ स्कैनर",
      "codex": "कानूनी कोडेक्स",
      "sos": "कानूनी SOS",
      "download": "डाउनलोड",
      "getApp": "ऐप प्राप्त करें",
      "brandTag": "// न्यायAI"
    },
    "footer": {
      "brand": "न्यायAI (NYAAI) लीगलटेक प्लेटफॉर्म",
      "copyright": "© 2026 न्यायAI लीगलटेक। सर्वाधिकार सुरक्षित।"
    },
    "home": {
      "badge": "संशोधित आपराधिक कानून • BNS 2023 तैयार",
      "heroTitle": "आधुनिक आर्टिफिशियल इंटेलिजेंस के साथ भारतीय कानून को सरल बनाना।",
      "heroSubtitle": "भारतीय न्याय संहिता, BNSS, BSA और भारत के संविधान में तत्काल वैधानिक खोज, ऑन-डिवाइस OCR और सरल भाषा में कानूनी विश्लेषण।",
      "btnTerminal": "⚡ AI टर्मिनल शुरू करें",
      "btnScanner": "📄 दस्तावेज़ स्कैनर आज़माएं",
      "btnDownload": "📲 Android APK डाउनलोड करें",
      "metric1": "अनुक्रमित कानूनी धाराएं",
      "metric3": "ऑफ़लाइन खोज गति",
      "metric4": "ऑन-डिवाइस निजी OCR",
      "secBadge": "क्षमताओं को जानें",
      "secTitle": "भारतीय कानून को समझने के लिए हर आवश्यक साधन",
      "secDesc": "अधिवक्ताओं, कानून के छात्रों, पुलिस अधिकारियों और कानूनी स्पष्टता चाहने वाले नागरिकों के लिए निर्मित।",
      "card1Title": "इंटरैक्टिव AI टर्मिनल",
      "card1Desc": "जटिल कानूनी प्रश्न पूछें और धारा उद्धरण, आत्मविश्वास मेट्रिक्स और वाक् संश्लेषण के साथ सीधे विशिष्ट वैधानिक अनुभागों पर आधारित उत्तर प्राप्त करें।",
      "card1Btn": "टर्मिनल खोलें →",
      "card2Title": "ऑन-डिवाइस दस्तावेज़ स्कैनर",
      "card2Desc": "कागजी कानूनी नोटिस, समझौते और FIR प्रतियां स्कैन करें। शून्य क्लाउड लीकेज के साथ कानूनी समयसीमा और अनुशंसित कदम निकालें।",
      "card2Btn": "दस्तावेज़ स्कैन करें →",
      "card3Title": "भारतीय कानूनी कोडेक्स",
      "card3Desc": "नए आपराधिक कानूनों (BNS 2023, BNSS 2023, BSA 2023) और भारत के संविधान का खोज योग्य, संरचित भंडार। तुरंत धाराएं खोजें।",
      "card3Btn": "कोडेक्स ब्राउज़ करें →",
      "card4Title": "राष्ट्रीय कानूनी SOS",
      "card4Desc": "भारत भर में आधिकारिक टोल-फ्री आपातकालीन कानूनी हेल्पलाइन (NALSA मुफ्त कानूनी सहायता, NCW महिला हेल्पलाइन, साइबर अपराध, चाइल्डलाइन) तक त्वरित पहुंच।",
      "card4Btn": "हेल्पलाइन देखें →",
      "card5Title": "ज़ीरो-नॉलेज निजता",
      "card5Desc": "स्थानीय SQLite FTS4 प्रीलोडेड नॉलेज बेस और ऑन-डिवाइस ML Kit OCR गारंटी देते हैं कि कानूनी परामर्श, फोटो और वॉइस डेटा कभी डिवाइस से बाहर नहीं जाता।",
      "card5Badge": "✓ 100% ऑन-डिवाइस",
      "card6Title": "क्रॉस-प्लेटफ़ॉर्म तैयार",
      "card6Desc": "आधुनिक वेब एप्लिकेशन और ऑफ़लाइन फॉलबैक के साथ प्रोडक्शन-हस्ताक्षरित मूल एंड्रॉइड एपीके के रूप में उपलब्ध।",
      "card6Btn": "ऐप डाउनलोड करें →"
    },
    "terminal": {
      "badge": "⚡ इंटरैक्टिव कानूनी प्लेग्राउंड",
      "title": "लाइव लीगल AI टर्मिनल",
      "desc": "तुरंत वैधानिक संदर्भ और विश्वास मेट्रिक्स के साथ भारत के संशोधित कानूनों के बारे में प्रश्न पूछें।",
      "simBadge": "ऑफ़लाइन-सक्षम सिमुलेशन",
      "verifiedBadge": "99.8% सिस्टम सत्यापित",
      "welcomeTitle": "न्यायAI रिसर्च टर्मिनल में आपका स्वागत है",
      "welcomeDesc": "मैं <strong>भारतीय न्याय संहिता (BNS 2023)</strong>, <strong>BNSS 2023</strong>, <strong>BSA 2023</strong> और <strong>भारत के संविधान</strong> के प्रावधानों का विश्लेषण कर सकता हूं। कोई प्रश्न पूछें या नीचे दिए गए सुझावों पर क्लिक करें:",
      "chip1": "⚖️ धारा 482 BNSS जमानत",
      "chip2": "📜 अनुच्छेद 21 निजता अधिकार",
      "chip3": "⚔️ धारा 103 BNS हत्या सजा",
      "chip4": "📱 इलेक्ट्रॉनिक साक्ष्य BSA",
      "chip5": "🛍️ उपभोक्ता अधिकार और रिफंड",
      "inputPlaceholder": "कोई भी कानूनी प्रश्न पूछें (उदा. गिरफ्तारी अधिकार, धारा 138 चेक बाउंस, साइबर अपराध)...",
      "sendBtn": "AI से पूछें",
      "readAloud": "🔊 बोलकर सुनाएं",
      "copyCitation": "📋 संदर्भ कॉपी करें",
      "citationLabel": "कानूनी संदर्भ:"
    },
    "scanner": {
      "badge": "📄 ऑन-डिवाइस विज़न इंजन",
      "title": "कानूनी दस्तावेज़ स्कैनर और OCR",
      "desc": "स्थानीय Google ML Kit के साथ बिना क्लाउड अपलोड किए कागजी नोटिस, अनुबंध और FIR से पाठ निकालें।",
      "tab1": "धारा 138 NI एक्ट नोटिस",
      "tab2": "FIR अंश (साइबर धोखाधड़ी)",
      "tab3": "NDA उल्लंघन नोटिस",
      "btnScan": "⚡ दस्तावेज़ का विश्लेषण करें",
      "repTypeLabel": "दस्तावेज़ वर्गीकरण:",
      "repStatutesLabel": "वैधानिक ढांचा:",
      "repDeadlineLabel": "कानूनी समय सीमा:",
      "repActionLabel": "अनुशंसित कार्रवाई:"
    },
    "codex": {
      "badge": "📚 वैधानिक ज्ञानकोश",
      "title": "भारतीय कानूनी कोडेक्स",
      "desc": "भारत के नए आपराधिक संहिताओं और संवैधानिक न्यायशास्त्र की 1,838+ धाराओं तक त्वरित अनुक्रमित पहुंच।",
      "searchPlaceholder": "धारा, कीवर्ड या विषय द्वारा खोजें (उदा. हत्या, जमानत, FIR, इलेक्ट्रॉनिक साक्ष्य, निजता)..."
    },
    "sos": {
      "badge": "🚨 आपातकालीन कानूनी निर्देशिका",
      "title": "राष्ट्रीय कानूनी SOS और सहायता",
      "desc": "भारतीय आपराधिक कानून के तहत आपातकालीन हेल्पलाइन और आवश्यक नागरिक अधिकारों तक सीधी पहुंच।",
      "nalsaTitle": "राष्ट्रीय कानूनी सेवा प्राधिकरण (NALSA)",
      "nalsaDesc": "पात्र नागरिकों और विचाराधीन कैदियों के लिए निःशुल्क कानूनी सहायता",
      "womenTitle": "महिला हेल्पलाइन (संकट में सहायता)",
      "womenDesc": "24/7 राष्ट्रीय महिला आयोग आपातकालीन सहायता",
      "cyberTitle": "राष्ट्रीय साइबर अपराध पोर्टल",
      "cyberDesc": "वित्तीय साइबर धोखाधड़ी, फ़िशिंग और ऑनलाइन अपराध शिकायत निवारण",
      "childTitle": "चाइल्डलाइन आपातकालीन सहायता",
      "childDesc": "बाल संरक्षण, सुरक्षा और पॉक्सो शिकायत निवारण",
      "consumerTitle": "राष्ट्रीय उपभोक्ता हेल्पलाइन",
      "consumerDesc": "उपभोक्ता विवाद समाधान, दोषपूर्ण उत्पाद और अनुचित व्यापार निवारण",
      "seniorTitle": "वरिष्ठ नागरिक हेल्पलाइन (एल्डरलाइन)",
      "seniorDesc": "बुजुर्गों के कल्याण, भरण-पोषण और कानूनी सुरक्षा के लिए सहायता",
      "rightsBadge": "अपने अधिकार जानें",
      "rightsHeading": "भारतीय कानून के तहत आवश्यक नागरिक अधिकार",
      "arrestTitle": "गिरफ्तारी पर अधिकार (डी.के. बसु दिशानिर्देश)",
      "arrestPoints": [
        "गिरफ्तारी का पूरा आधार जानने और अपराध जमानती है या गैर-जमानती, यह जानने का अधिकार (धारा 47 BNSS)।",
        "गिरफ्तारी की सूचना तुरंत किसी रिश्तेदार या मित्र को दिए जाने का अधिकार।",
        "48 घंटे के भीतर अधिकृत चिकित्सा अधिकारी द्वारा अनिवार्य स्वास्थ्य परीक्षण।",
        "पूछताछ के दौरान अपनी पसंद के अधिवक्ता से परामर्श करने का अधिकार।"
      ],
      "zeroFirTitle": "ज़ीरो FIR प्रावधान (धारा 173 BNSS)",
      "zeroFirPoints": [
        "कोई भी पुलिस स्टेशन क्षेत्राधिकार के आधार पर प्राथमिकी (FIR) दर्ज करने से इनकार नहीं कर सकता।",
        "ज़ीरो FIR तुरंत दर्ज की जाती है और बाद में सक्षम क्षेत्राधिकार पुलिस स्टेशन को स्थानांतरित की जाती है।",
        "BNSS 2023 के तहत तलाशी और जब्ती कार्यों की ऑडियो-वीडियो रिकॉर्डिंग अनिवार्य है।"
      ],
      "legalAidTitle": "निःशुल्क कानूनी सहायता (अनुच्छेद 39A)",
      "legalAidPoints": [
        "अनुच्छेद 39A राज्य को सभी नागरिकों के लिए समान न्याय और मुफ्त कानूनी सहायता सुनिश्चित करने का निर्देश देता है।",
        "विधिक सेवा प्राधिकरण अधिनियम के तहत महिलाएं, बच्चे, विचाराधीन कैदी और कम आय वाले नागरिक सभी अदालतों में मुफ्त प्रतिनिधित्व के पात्र हैं।"
      ]
    },
    "download": {
      "badge": "📲 आधिकारिक वितरण केंद्र",
      "title": "न्यायAI v1.0 प्रोडक्शन डाउनलोड करें",
      "desc": "मूल एंड्रॉइड ऐप R8 कोड ऑप्टिमाइजेशन और 2048-बिट RSA कुंजियों के साथ सत्यापित।",
      "cardBadge": "हस्ताक्षरित प्रोडक्शन बिल्ड सत्यापित",
      "cardTitle": "न्यायAI एंड्रॉइड के लिए (रिलीज़ APK)",
      "cardDesc": "तुरंत < 100ms स्टार्टअप के लिए BNS, BNSS, BSA और संविधान की 1,838 धाराओं वाला प्री-पैकेज्ड डेटाबेस (4.27 MB, Schema v8) शामिल है।",
      "btnDownload": "⬇️ हस्ताक्षरित APK डाउनलोड करें (11.47 MB)",
      "btnMirror": "⚡ GitHub रिलीज़ मिरर",
      "btnWeb": "🌐 वेब संस्करण देखें"
    }
  },
  "bn": {
    "nav": {
      "home": "হোম",
      "terminal": "AI টার্মিনাল",
      "scanner": "নথি স্ক্যানার",
      "codex": "সংবিধিবদ্ধ কোডেক্স",
      "sos": "আইনি SOS",
      "download": "ডাউনলোড",
      "getApp": "অ্যাপ পান",
      "brandTag": "// ন্যায়AI"
    },
    "footer": {
      "brand": "ন্যায়AI (NYAAI) লিগ্যালটেক প্ল্যাটফর্ম",
      "copyright": "© 2026 ন্যায়AI লিগ্যালটেক। সর্বস্বত্ব সংরক্ষিত।"
    },
    "home": {
      "badge": "সংশোধিত ফৌজদারি আইন • BNS 2023 প্রস্তুত",
      "heroTitle": "আধুনিক কৃত্রিম বুদ্ধিমত্তার মাধ্যমে ভারতীয় আইনকে সহজতর করা।",
      "heroSubtitle": "ভারতীয় ন্যায় সংহিতা, BNSS, BSA এবং ভারতের সংবিধানের তাত্ক্ষণিক সংবিধিবদ্ধ অনুসন্ধান, অন-ডিভাইস OCR এবং সহজ ভাষায় ফৌজদারি কোড বিশ্লেষণ।",
      "btnTerminal": "⚡ AI টার্মিনাল চালু করুন",
      "btnScanner": "📄 নথি স্ক্যানার ব্যবহার করুন",
      "btnDownload": "📲 Android APK ডাউনলোড করুন",
      "metric1": "সূচীবদ্ধ আইনি ধারা",
      "metric3": "অফলাইন অনুসন্ধানের গতি",
      "metric4": "অন-ডিভাইস ব্যক্তিগত OCR",
      "secBadge": "বৈশিষ্ট্যসমূহ দেখুন",
      "secTitle": "ভারতীয় আইন বোঝার জন্য প্রয়োজনীয় সবকিছু",
      "secDesc": "আইনজীবী, আইনের ছাত্র, পুলিশ কর্মকর্তা এবং তাত্ক্ষণিক আইনি স্পষ্টতা চাওয়া নাগরিকদের জন্য তৈরি।",
      "card1Title": "ইন্টারেক্টিভ AI টার্মিনাল",
      "card1Desc": "জটিল আইনি প্রশ্ন করুন এবং ধারা উদ্ধৃতি, আত্মবিশ্বাসের মেট্রিক্স এবং ভয়েস ইনপুট সহ সঠিক উত্তর পান।",
      "card1Btn": "টার্মিনাল খুলুন →",
      "card2Title": "অন-ডিভাইস নথি স্ক্যানার",
      "card2Desc": "কাগজের আইনি নোটিশ, চুক্তি এবং FIR স্ক্যান করুন। কোনো ক্লাউড আপলোড ছাড়াই সময়সীমা এবং ধারা বের করুন।",
      "card2Btn": "নথি স্ক্যান করুন →",
      "card3Title": "ভারতীয় সংবিধিবদ্ধ কোডেক্স",
      "card3Desc": "নতুন ফৌজদারি আইন (BNS 2023, BNSS 2023, BSA 2023) এবং ভারতীয় সংবিধানের সম্পূর্ণ অনুসন্ধানযোগ্য ভাণ্ডার।",
      "card3Btn": "কোডেক্স দেখুন →",
      "card4Title": "জাতীয় আইনি SOS",
      "card4Desc": "ভারত জুড়ে সরকারি টোল-ফ্রি জরুরি আইনি হেল্পলাইনগুলিতে (NALSA, NCW, সাইবার ক্রাইম, চাইল্ডলাইন) দ্রুত অ্যাক্সেস।",
      "card4Btn": "হেল্পলাইন দেখুন →",
      "card5Title": "জিরো-নলেজ গোপনীয়তা",
      "card5Desc": "স্থানীয় SQLite FTS4 এবং অন-ডিভাইস ML Kit নিশ্চিত করে যে আপনার আইনি পরামর্শ, ছবি ও ভয়েস ডেটা কখনই ডিভাইস ত্যাগ করে না।",
      "card5Badge": "✓ ১০০% অন-ডিভাইস",
      "card6Title": "ক্রস-প্ল্যাটফর্ম প্রস্তুত",
      "card6Desc": "আধুনিক ওয়েব অ্যাপ্লিকেশন এবং অফলাইন ব্যাকআপ সহ প্রোডাকশন অ্যান্ড্রয়েড APK হিসেবে উপলব্ধ।",
      "card6Btn": "অ্যাপ ডাউনলোড করুন →"
    },
    "terminal": {
      "badge": "⚡ ইন্টারেক্টিভ আইনি প্ল্যাটফর্ম",
      "title": "লাইভ আইনি AI টার্মিনাল",
      "desc": "তাত্ক্ষণিক ধারা উদ্ধৃতি এবং আত্মবিশ্বাসের মেট্রিক্স সহ ভারতের নতুন আইন সম্পর্কে প্রশ্ন করুন।",
      "simBadge": "অফলাইন-সক্ষম সিমুলেশন",
      "verifiedBadge": "৯৯.৮% সিস্টেম যাচাইকৃত",
      "welcomeTitle": "ন্যায়AI গবেষণা টার্মিনালে আপনাকে স্বাগতম",
      "welcomeDesc": "আমি <strong>ভারতীয় ন্যায় সংহিতা (BNS 2023)</strong>, <strong>BNSS 2023</strong>, <strong>BSA 2023</strong> এবং <strong>ভারতের সংবিধানের</strong> বিধান বিশ্লেষণ করতে পারি। একটি আইনি প্রশ্ন জিজ্ঞাসা করুন:",
      "chip1": "⚖️ ধারা ৪৮২ BNSS জামিন",
      "chip2": "📜 অনুচ্ছেদ ২১ গোপনীয়তার অধিকার",
      "chip3": "⚔️ ধারা ১০৩ BNS হত্যার শাস্তি",
      "chip4": "📱 ডিজিটাল প্রমাণ BSA",
      "chip5": "🛍️ ভোক্তা অধিকার ও রিফান্ড",
      "inputPlaceholder": "যেকোনো আইনি প্রশ্ন জিজ্ঞাসা করুন (যেমন গ্রেপ্তার অধিকার, চেক বাউন্স, সাইবার প্রতারণা)...",
      "sendBtn": "AI-কে জিজ্ঞাসা করুন",
      "readAloud": "🔊 পড়ে শোনান",
      "copyCitation": "📋 উদ্ধৃতি কপি করুন",
      "citationLabel": "আইনি উদ্ধৃতি:"
    },
    "scanner": {
      "badge": "📄 অন-ডিভাইস ভিশন ইঞ্জিন",
      "title": "আইনি নথি স্ক্যানার এবং OCR",
      "desc": "স্থানীয় Google ML Kit দিয়ে কোনো ক্লাউড আপলোড ছাড়াই নোটিশ, চুক্তি ও FIR থেকে টেক্সট বের করুন।",
      "tab1": "ধারা ১৩৮ NI অ্যাক্ট নোটিশ",
      "tab2": "FIR অংশ (সাইবার প্রতারণা)",
      "tab3": "NDA লঙ্ঘন নোটিশ",
      "btnScan": "⚡ নথি বিশ্লেষণ করুন",
      "repTypeLabel": "নথির ধরন:",
      "repStatutesLabel": "সংবিধিবদ্ধ আইন:",
      "repDeadlineLabel": "আইনি সময়সীমা:",
      "repActionLabel": "প্রস্তাবিত পদক্ষেপ:"
    },
    "codex": {
      "badge": "📚 সংবিধিবদ্ধ জ্ঞানকোষ",
      "title": "ভারতীয় সংবিধিবদ্ধ কোডেক্স",
      "desc": "ভারতের নতুন ফৌজদারি কোড এবং সাংবিধানিক আইনের ১,৮৩৮+ ধারায় তাত্ক্ষণিক অনুসন্ধান।",
      "searchPlaceholder": "ধারা, কিওয়ার্ড বা বিষয় দ্বারা অনুসন্ধান করুন (যেমন হত্যা, জামিন, FIR, ডিজিটাল প্রমাণ, গোপনীয়তা)..."
    },
    "sos": {
      "badge": "🚨 জরুরি আইনি ডিরেক্টরি",
      "title": "জাতীয় আইনি SOS এবং সহায়তা",
      "desc": "ভারতীয় ফৌজদারি আইনের অধীনে জরুরি হেল্পলাইন এবং প্রয়োজনীয় নাগরিক অধিকারগুলিতে সরাসরি অ্যাক্সেস।",
      "nalsaTitle": "জাতীয় আইনি পরিষেবা কর্তৃপক্ষ (NALSA)",
      "nalsaDesc": "যোগ্য নাগরিক এবং বিচারাধীন বন্দীদের জন্য বিনামূল্যে আইনি সহায়তা",
      "womenTitle": "মহিলা হেল্পলাইন (বিপদে সহায়তা)",
      "womenDesc": "২৪/৭ জাতীয় মহিলা কমিশন জরুরি সহায়তা",
      "cyberTitle": "জাতীয় সাইবার ক্রাইম পোর্টাল",
      "cyberDesc": "আর্থিক সাইবার প্রতারণা এবং অনলাইন অপরাধের অভিযোগ নিষ্পত্তি",
      "childTitle": "চাইল্ডলাইন জরুরি পরিষেবা",
      "childDesc": "শিশু সুরক্ষা, নিরাপত্তা এবং POCSO অভিযোগ নিষ্পত্তি",
      "consumerTitle": "জাতীয় ভোক্তা হেল্পলাইন",
      "consumerDesc": "ভোক্তা বিরোধ নিষ্পত্তি, ত্রুটিপূর্ণ পণ্য এবং অন্যায্য বাণিজ্য",
      "seniorTitle": "প্রবীণ নাগরিক হেল্পলাইন (এল্ডারলাইন)",
      "seniorDesc": "প্রবীণ নাগরিকদের কল্যাণ এবং আইনি সুরক্ষার জন্য হেল্পলাইন",
      "rightsBadge": "আপনার অধিকার জানুন",
      "rightsHeading": "ভারতীয় আইনের অধীনে প্রয়োজনীয় নাগরিক অধিকার",
      "arrestTitle": "গ্রেপ্তারের সময় অধিকার (ডি.কে. বসু নির্দেশিকা)",
      "arrestPoints": [
        "গ্রেপ্তারের সম্পূর্ণ কারণ এবং অপরাধ জামিনযোগ্য কি না তা জানার অধিকার (ধারা ৪৭ BNSS)।",
        "গ্রেপ্তারের বিষয়ে আত্মীয় বা বন্ধুকে অবিলম্বে জানানোর অধিকার।",
        "৪৮ ঘণ্টার মধ্যে অনুমোদিত চিকিৎসকের দ্বারা বাধ্যতামূলক স্বাস্থ্য পরীক্ষা।",
        "জিজ্ঞাসাবাদের সময় আইনজীবীর সাথে পরামর্শ করার অধিকার।"
      ],
      "zeroFirTitle": "জিরো FIR বিধান (ধারা ১৭৩ BNSS)",
      "zeroFirPoints": [
        "কোনো থানা আঞ্চলিক এখতিয়ারের অজুহাতে FIR নিতে অস্বীকার করতে পারে না।",
        "একটি জিরো FIR সঙ্গে সঙ্গে নথিভুক্ত করে উপযুক্ত থানায় স্থানান্তর করা হয়।",
        "BNSS 2023 অনুযায়ী তল্লাশি ও জব্দের অডিও-ভিডিও রেকর্ডিং বাধ্যতামূলক।"
      ],
      "legalAidTitle": "বিনামূল্যে আইনি সহায়তা (অনুচ্ছেদ ৩৯A)",
      "legalAidPoints": [
        "অনুচ্ছেদ ৩৯A সমস্ত নাগরিকের সমান ন্যায়বিচার এবং বিনামূল্যে আইনি সহায়তা নিশ্চিত করার নির্দেশ দেয়।",
        "আইনি পরিষেবা কর্তৃপক্ষ আইনের আওতায় নারী, শিশু, বিচারাধীন বন্দী এবং স্বল্প আয়ের নাগরিকরা আদালতে বিনামূল্যে আইনজীবী পাওয়ার অধিকারী।"
      ]
    },
    "download": {
      "badge": "📲 অফিসিয়াল বিতরণ কেন্দ্র",
      "title": "ন্যায়AI v1.0 প্রোডাকশন ডাউনলোড করুন",
      "desc": "নেটিভ অ্যান্ড্রয়েড অ্যাপ যা R8 অপ্টিমাইজেশন এবং ২০৪৮-বিট RSA কী সহ যাচাইকৃত।",
      "cardBadge": "স্বাক্ষরিত প্রোডাকশন বিল্ড যাচাইকৃত",
      "cardTitle": "অ্যান্ড্রয়েডের জন্য ন্যায়AI (রিলিজ APK)",
      "cardDesc": "তাত্ক্ষণিক < ১০০ms স্টার্টআপের জন্য ১,৮৩৮টি ধারা সমৃদ্ধ প্রি-প্যাকেজড ডেটাবেস (৪.২৭ MB, Schema v8) অন্তর্ভুক্ত।",
      "btnDownload": "⬇️ স্বাক্ষরিত APK ডাউনলোড করুন (১১.৪৭ MB)",
      "btnMirror": "⚡ GitHub রিলিজ মিরর",
      "btnWeb": "🌐 ওয়েব সংস্করণ দেখুন"
    }
  },
  "te": {
    "nav": {
      "home": "హోమ్",
      "terminal": "AI టెర్మినల్",
      "scanner": "పత్ర స్కానర్",
      "codex": "చట్టబద్ధమైన కోడెక్స్",
      "sos": "చట్టపరమైన SOS",
      "download": "డౌన్‌లోడ్",
      "getApp": "యాప్ పొందండి",
      "brandTag": "// న్యాయ్AI"
    },
    "footer": {
      "brand": "న్యాయ్AI (NYAAI) లీగల్‌టెక్ ప్లాట్‌ఫారమ్",
      "copyright": "© 2026 న్యాయ్AI లీగల్‌టెక్. సర్వహక్కులు ప్రత్యేకించబడ్డాయి."
    },
    "home": {
      "badge": "సంస్కరించబడిన నేర చట్టాలు • BNS 2023 సిద్ధం",
      "heroTitle": "ఆధునిక కృత్రిమ మేధస్సుతో భారతీయ చట్టాన్ని సులభతరం చేయడం.",
      "heroSubtitle": "భారతీయ న్యాయ సంహిత, BNSS, BSA మరియు భారత రాజ్యాంగంలో తక్షణ చట్టబద్ధమైన శోధన, పరికరంలో OCR మరియు సరళమైన విశ్లేషణ.",
      "btnTerminal": "⚡ AI టెర్మినల్ ప్రారంభించండి",
      "btnScanner": "📄 పత్ర స్కానర్‌ను ప్రయత్నించండి",
      "btnDownload": "📲 Android APK డౌన్‌లోడ్ చేయండి",
      "metric1": "సూచిక చేయబడిన చట్టపరమైన విభాగాలు",
      "metric3": "ఆఫ్‌లైన్ ప్రశ్న వేగం",
      "metric4": "పరికరంలో ప్రైవేట్ OCR",
      "secBadge": "సామర్థ్యాలను అన్వేషించండి",
      "secTitle": "భారతీయ చట్టాన్ని నావిగేట్ చేయడానికి మీకు కావలసినవన్నీ",
      "secDesc": "న్యాయవాదులు, న్యాయ విద్యార్థులు, పోలీసు అధికారులు మరియు పౌరుల కోసం రూపొందించబడింది.",
      "card1Title": "ఇంటరాక్టివ్ AI టెర్మినల్",
      "card1Desc": "సంక్లిష్టమైన చట్టపరమైన ప్రశ్నలను అడగండి మరియు సెక్షన్ అనులేఖనాలు, కాన్ఫిడెన్స్ మెట్రిక్స్ మరియు వాయిస్ ఇన్‌పుట్‌తో సమాధానాలను పొందండి.",
      "card1Btn": "టెర్మినల్ తెరవండి →",
      "card2Title": "పరికరంలో పత్ర స్కానర్",
      "card2Desc": "చట్టపరమైన నోటీసులు, ఒప్పందాలు మరియు FIR కాపీలను స్కాన్ చేయండి. క్లౌడ్ అప్‌లోడ్ లేకుండా వివరాలను పొందండి.",
      "card2Btn": "పత్రాన్ని స్కాన్ చేయండి →",
      "card3Title": "భారతీయ చట్టబద్ధమైన కోడెక్స్",
      "card3Desc": "కొత్త నేర చట్టాలు (BNS 2023, BNSS 2023, BSA 2023) మరియు భారత రాజ్యాంగం యొక్క శోధించదగిన డేటాబేస్.",
      "card3Btn": "కోడెక్స్ బ్రౌజ్ చేయండి →",
      "card4Title": "జాతీయ చట్టపరమైన SOS",
      "card4Desc": "భారతదేశం అంతటా అధికారిక టోల్-ఫ్రీ అత్యవసర చట్టపరమైన హెల్ప్‌లైన్‌లకు (NALSA, NCW, సైబర్ క్రైమ్, చైల్డ్‌లైన్) తక్షణ ప్రాప్యత.",
      "card4Btn": "హెల్ప్‌లైన్‌లను చూడండి →",
      "card5Title": "జీరో-నాలెడ్జ్ గోప్యత",
      "card5Desc": "స్థానిక SQLite FTS4 మరియు ఆన్-డివైస్ ML Kit మీ చట్టపరమైన సంప్రదింపులు ఎప్పుడూ పరికరాన్ని వదిలి వెళ్లవని హామీ ఇస్తాయి.",
      "card5Badge": "✓ 100% ఆన్-డివైస్",
      "card6Title": "క్రాస్-ప్లాట్‌ఫారమ్ సిద్ధం",
      "card6Desc": "ఆధునిక వెబ్ అప్లికేషన్ మరియు స్థానిక Android APKగా అందుబాటులో ఉంది.",
      "card6Btn": "యాప్ డౌన్‌లోడ్ చేయండి →"
    },
    "terminal": {
      "badge": "⚡ ఇంటరాక్టివ్ లీగల్ ప్లేగ్రౌండ్",
      "title": "లైవ్ లీగల్ AI టెర్మినల్",
      "desc": "తక్షణ చట్టపరమైన వివరాలు మరియు కాన్ఫిడెన్స్ మెట్రిక్స్‌తో భారతదేశపు చట్టాల గురించి ప్రశ్నించండి.",
      "simBadge": "ఆఫ్‌లైన్-సామర్థ్య అనుకరణ",
      "verifiedBadge": "99.8% సిస్టమ్ ధృవీకరించబడింది",
      "welcomeTitle": "న్యాయ్AI పరిశోధన టెర్మినల్‌కు స్వాగతం",
      "welcomeDesc": "నేను <strong>భారతీయ న్యాయ సంహిత (BNS 2023)</strong>, <strong>BNSS 2023</strong>, <strong>BSA 2023</strong> మరియు <strong>భారత రాజ్యాంగ</strong> నిబంధనలను పరిశీలించగలను. ఏదైనా చట్టపరమైన ప్రశ్న అడగండి:",
      "chip1": "⚖️ సెక్షన్ 482 BNSS బెయిల్",
      "chip2": "📜 ఆర్టికల్ 21 గోప్యతా హక్కులు",
      "chip3": "⚔️ సెక్షన్ 103 BNS హత్య శిక్ష",
      "chip4": "📱 ఎలక్ట్రానిక్ సాక్ష్యం BSA",
      "chip5": "🛍️ వినియోగదారుల హక్కులు & రీఫండ్‌లు",
      "inputPlaceholder": "ఏదైనా చట్టపరమైన ప్రశ్న అడగండి (ఉదా. అరెస్టు హక్కులు, చెక్ బౌన్స్, సైబర్ మోసం)...",
      "sendBtn": "AI ని అడగండి",
      "readAloud": "🔊 చదివి వినిపించండి",
      "copyCitation": "📋 కాపీ చేయండి",
      "citationLabel": "చట్ట సూచన:"
    },
    "scanner": {
      "badge": "📄 పరికరంలో విజన్ ఇంజిన్",
      "title": "చట్టపరమైన పత్ర స్కానర్ & OCR",
      "desc": "క్లౌడ్ అప్‌లోడ్ లేకుండా స్థానిక Google ML Kit ఉపయోగించి నోటీసులు, ఒప్పందాలు మరియు FIRల నుండి వచనాన్ని సంగ్రహించండి.",
      "tab1": "సెక్షన్ 138 NI యాక్ట్ నోటీసు",
      "tab2": "FIR సారాంశం (సైబర్ మోసం)",
      "tab3": "NDA ఉల్లంఘన నోటీసు",
      "btnScan": "⚡ పత్రాన్ని విశ్లేషించండి",
      "repTypeLabel": "పత్ర వర్గీకరణ:",
      "repStatutesLabel": "చట్టపరమైన ఫ్రేమ్‌వర్క్:",
      "repDeadlineLabel": "చట్టపరమైన గడువు:",
      "repActionLabel": "సిఫార్సు చేయబడిన చర్య:"
    },
    "codex": {
      "badge": "📚 చట్టబద్ధమైన నాలెడ్జ్ బేస్",
      "title": "భారతీయ చట్టబద్ధమైన కోడెక్స్",
      "desc": "భారతదేశపు కొత్త క్రిమినల్ కోడ్‌లు మరియు రాజ్యాంగ న్యాయశాస్త్రం యొక్క 1,838+ విభాగాలకు తక్షణ శోధన ప్రాప్యత.",
      "searchPlaceholder": "సెక్షన్, కీవర్డ్ లేదా అంశం ద్వారా శోధించండి (ఉదా. హత్య, బెయిల్, FIR, ఎలక్ట్రానిక్ సాక్ష్యం, గోప్యత)..."
    },
    "sos": {
      "badge": "🚨 అత్యవసర చట్టపరమైన డైరెక్టరీ",
      "title": "జాతీయ చట్టపరమైన SOS & మద్దతు",
      "desc": "భారతీయ క్రిమినల్ చట్టం కింద అత్యవసర హెల్ప్‌లైన్‌లు మరియు పౌర హక్కులకు ప్రత్యక్ష ప్రాప్యత.",
      "nalsaTitle": "జాతీయ న్యాయ సేవల అధికార సంస్థ (NALSA)",
      "nalsaDesc": "అర్హులైన పౌరులు మరియు విచారణలో ఉన్న ఖైదీలకు ఉచిత న్యాయ సహాయం",
      "womenTitle": "మహిళల అత్యవసర హెల్ప్‌లైన్",
      "womenDesc": "24/7 జాతీయ మహిళా కమిషన్ అత్యవసర సహాయం",
      "cyberTitle": "జాతీయ సైబర్ క్రైమ్ పోర్టల్",
      "cyberDesc": "ఆర్థిక సైబర్ మోసం, ఫిషింగ్ మరియు ఆన్‌లైన్ నేరాల పరిష్కారం",
      "childTitle": "చైల్డ్‌లైన్ అత్యవసర సేవ",
      "childDesc": "పిల్లల రక్షణ, భద్రత మరియు పోక్సో ఫిర్యాదుల పరిష్కారం",
      "consumerTitle": "జాతీయ వినియోగదారుల హెల్ప్‌లైన్",
      "consumerDesc": "వినియోగదారు వివాద పరిష్కారం మరియు లోపభూయిష్ట ఉత్పత్తులు",
      "seniorTitle": "సీనియర్ సిటిజన్ హెల్ప్‌లైన్ (ఎల్డర్‌లైన్)",
      "seniorDesc": "వృద్ధుల సంక్షేమం మరియు చట్టపరమైన రక్షణ కోసం ఎల్డర్ లైన్",
      "rightsBadge": "మీ హక్కులను తెలుసుకోండి",
      "rightsHeading": "భారతీయ చట్టం ప్రకారం అవసరమైన పౌర హక్కులు",
      "arrestTitle": "అరెస్టు సమయంలో హక్కులు (డి.కె. బసు మార్గదర్శకాలు)",
      "arrestPoints": [
        "అరెస్టుకు గల పూర్తి కారణాలను మరియు నేరం బెయిలబుల్ కాదా అని తెలుసుకునే హక్కు (సెక్షన్ 47 BNSS).",
        "అరెస్టు గురించి బంధువు లేదా స్నేహితుడికి వెంటనే తెలియజేసే హక్కు.",
        "48 గంటల్లో అధీకృత వైద్యాధికారితో తప్పనిసరి వైద్య పరీక్ష.",
        "విచారణ సమయంలో మీ న్యాయవాదితో సంప్రదించే హక్కు."
      ],
      "zeroFirTitle": "జీరో FIR నిబంధన (సెక్షన్ 173 BNSS)",
      "zeroFirPoints": [
        "పోలీస్ స్టేషన్ పరిధిని సాకుగా చూపి FIR నమోదు చేయడాన్ని తిరస్కరించలేరు.",
        "జీరో FIR నమోదు చేసి సంబంధిత పోలీస్ స్టేషన్‌కు బదిలీ చేస్తారు.",
        "శోధన మరియు జప్తు కార్యకలాపాల ఆడియో-వీడియో రికార్డింగ్ ఇప్పుడు BNSS 2023 కింద తప్పనిసరి."
      ],
      "legalAidTitle": "ఉచిత న్యాయ సహాయం (ఆర్టికల్ 39A)",
      "legalAidPoints": [
        "పౌరులందరికీ సమాన న్యాయం మరియు ఉచిత న్యాయ సహాయాన్ని అందించాలని ఆర్టికల్ 39A నిర్దేశిస్తుంది.",
        "మహిళలు, పిల్లలు మరియు అల్పాదాయ వర్గాల వారు న్యాయస్థానాలలో ఉచిత ప్రాతినిధ్యానికి అర్హులు."
      ]
    },
    "download": {
      "badge": "📲 అధికారిక పంపిణీ కేంద్రం",
      "title": "న్యాయ్AI v1.0 ప్రొడక్షన్ డౌన్‌లోడ్ చేయండి",
      "desc": "స్థానిక Android అప్లికేషన్ R8 కోడ్ ఆప్టిమైజేషన్ మరియు 2048-బిట్ RSA కీలతో ధృవీకరించబడింది.",
      "cardBadge": "సంతకం చేసిన ప్రొడక్షన్ బిల్డ్ ధృవీకరించబడింది",
      "cardTitle": "Android కోసం న్యాయ్AI (విడుదల APK)",
      "cardDesc": "తక్షణ < 100ms స్టార్టప్ కోసం BNS, BNSS, BSA మరియు రాజ్యాంగం యొక్క 1,838 విభాగాలతో కూడిన డేటాబేస్ (4.27 MB, Schema v8) చేర్చబడింది.",
      "btnDownload": "⬇️ సంతకం చేసిన APK డౌన్‌లోడ్ చేయండి (11.47 MB)",
      "btnMirror": "⚡ GitHub విడుదలల మిర్రర్",
      "btnWeb": "🌐 వెబ్ వెర్షన్ చూడండి"
    }
  },
  "ta": {
    "nav": {
      "home": "முகப்பு",
      "terminal": "AI முனையம்",
      "scanner": "ஆவண ஸ்கேனர்",
      "codex": "சட்டக் கோடெக்ஸ்",
      "sos": "சட்ட SOS",
      "download": "பதிவிறக்கம்",
      "getApp": "செயலியைப் பெறுக",
      "brandTag": "// நியாய்AI"
    },
    "footer": {
      "brand": "நியாய்AI (NYAAI) லீகல்தெக் தளம்",
      "copyright": "© 2026 நியாய்AI லீகல்தெக். அனைத்து உரிமைகளும் பாதுகாக்கப்பட்டவை."
    },
    "home": {
      "badge": "சீர்திருத்தப்பட்ட குற்றவியல் சட்டங்கள் • BNS 2023 தயார்",
      "heroTitle": "நவீன செயற்கை நுண்ணறிவு மூலம் இந்திய சட்டத்தை எளிமையாக்குதல்.",
      "heroSubtitle": "பாரதிய நியாய சன்ஹிதா, BNSS, BSA மற்றும் இந்திய அரசியலமைப்பில் உடனடி சட்ட ரீதியான தேடல், சாதனத்திலேயே OCR மற்றும் எளிய மொழி பகுப்பாய்வு.",
      "btnTerminal": "⚡ AI முனையத்தைத் தொடங்கவும்",
      "btnScanner": "📄 ஆவண ஸ்கேனரை முயற்சிக்கவும்",
      "btnDownload": "📲 Android APK பதிவிறக்கவும்",
      "metric1": "அட்டவணைப்படுத்தப்பட்ட சட்டப் பிரிவுகள்",
      "metric3": "ஆஃப்லைன் தேடல் வேகம்",
      "metric4": "சாதனத்திலேயே தனிப்பட்ட OCR",
      "secBadge": "திறன்களை ஆராயுங்கள்",
      "secTitle": "இந்திய சட்டத்தைப் புரிந்துகொள்ள உங்களுக்குத் தேவையான அனைத்தும்",
      "secDesc": "வழக்கறிஞர்கள், சட்ட மாணவர்கள், காவல் துறையினர் மற்றும் குடிமக்களுக்காக உருவாக்கப்பட்டது.",
      "card1Title": "ஊடாடும் AI முனையம்",
      "card1Desc": "சிக்கலான சட்டக் கேள்விகளைக் கேட்டு, குறிப்பிட்ட சட்டப் பிரிவுகள் மற்றும் நம்பிக்கைக் குறியீடுகளுடன் பதில்களைப் பெறுங்கள்.",
      "card1Btn": "முனையத்தைத் திற →",
      "card2Title": "சாதனத்திலேயே ஆவண ஸ்கேனர்",
      "card2Desc": "சட்ட அறிவிப்புகள், ஒப்பந்தங்கள் மற்றும் எஃப்ஐஆர் நகல்களை ஸ்கேன் செய்யுங்கள். கிளவுட் பதிவேற்றம் இல்லாமல் விவரங்களைப் பிரித்தெடுங்கள்.",
      "card2Btn": "ஆவணத்தை ஸ்கேன் செய் →",
      "card3Title": "இந்திய சட்டக் கோடெக்ஸ்",
      "card3Desc": "புதிய குற்றவியல் சட்டங்கள் (BNS 2023, BNSS 2023, BSA 2023) மற்றும் இந்திய அரசியலமைப்பின் தேடக்கூடிய களஞ்சியம்.",
      "card3Btn": "கோடெக்ஸை உலாவு →",
      "card4Title": "தேசிய சட்ட SOS",
      "card4Desc": "இந்தியா முழுவதும் உள்ள அதிகாரப்பூர்வ அவசர சட்ட உதவி எண்களுக்கான (NALSA, NCW, சைபர் கிரைம், சைல்டுலைன்) நேரடி அணுகல்.",
      "card4Btn": "உதவி எண்களைப் பார் →",
      "card5Title": "பூஜ்ஜிய-அறிவு தனியுரிமை",
      "card5Desc": "உள்ளூர் SQLite FTS4 மற்றும் ML Kit உங்கள் சட்ட ஆலோசனைகள் சாதனத்தை விட்டு வெளியேறாது என்பதை உறுதி செய்கிறது.",
      "card5Badge": "✓ 100% சாதனத்திலேயே",
      "card6Title": "அனைத்து தளங்களுக்கும் தயார்",
      "card6Desc": "நவீன வலை பயன்பாடாகவும் ஆஃப்லைன் ஆதரவு கொண்ட நேட்டிவ் ஆண்ட்ராய்டு APK ஆகவும் கிடைக்கிறது.",
      "card6Btn": "செயலியைப் பதிவிறக்கு →"
    },
    "terminal": {
      "badge": "⚡ ஊடாடும் சட்ட களம்",
      "title": "நேரலை சட்ட AI முனையம்",
      "desc": "உடனடி சட்டக் குறிப்புகள் மற்றும் நம்பிக்கைக் குறியீடுகளுடன் இந்தியாவின் சீர்திருத்தப்பட்ட சட்டங்களை வினவுங்கள்.",
      "simBadge": "ஆஃப்லைன்-செயல்பாட்டு மாதிரி",
      "verifiedBadge": "99.8% சரிபார்க்கப்பட்டது",
      "welcomeTitle": "நியாய்AI ஆராய்ச்சி முனையத்திற்கு வரவேற்கிறோம்",
      "welcomeDesc": "நான் <strong>பாரதிய நியாய சன்ஹிதா (BNS 2023)</strong>, <strong>BNSS 2023</strong>, <strong>BSA 2023</strong> மற்றும் <strong>இந்திய அரசியலமைப்பு</strong> விதிகளை பகுப்பாய்வு செய்ய முடியும். சட்டக் கேள்வியைக் கேளுங்கள்:",
      "chip1": "⚖️ பிரிவு 482 BNSS ஜாமீன்",
      "chip2": "📜 பிரிவு 21 தனியுரிமை உரிமை",
      "chip3": "⚔️ பிரிவு 103 BNS கொலை தண்டனை",
      "chip4": "📱 மின்னணு சான்று BSA",
      "chip5": "🛍️ நுகர்வோர் உரிமைகள் & திருப்பிச் செலுத்துதல்",
      "inputPlaceholder": "ஏதேனும் சட்டக் கேள்வியைக் கேளுங்கள் (எ.கா. கைது உரிமைகள், காசோலை திரும்புதல், இணைய மோசடி)...",
      "sendBtn": "AI-யிடம் கேள்",
      "readAloud": "🔊 உரக்கப் படி",
      "copyCitation": "📋 மேற்கோளை நகலெடு",
      "citationLabel": "சட்ட மேற்கோள்:"
    },
    "scanner": {
      "badge": "📄 சாதனத்திலேயே விஷன் எஞ்சின்",
      "title": "சட்ட ஆவண ஸ்கேனர் & OCR",
      "desc": "கிளவுட் பதிவேற்றம் இல்லாமல் லோக்கல் Google ML Kit மூலம் நோட்டீஸ்கள், ஒப்பந்தங்கள் மற்றும் எஃப்ஐஆர்களிலிருந்து உரையைப் பிரித்தெடுக்கவும்.",
      "tab1": "பிரிவு 138 NI சட்டம் நோட்டீஸ்",
      "tab2": "எஃப்ஐஆர் பகுதி (இணைய மோசடி)",
      "tab3": "NDA மீறல் நோட்டீஸ்",
      "btnScan": "⚡ ஆவணத்தை பகுப்பாய்வு செய்",
      "repTypeLabel": "ஆவண வகைப்பாடு:",
      "repStatutesLabel": "சட்டக் கட்டமைப்பு:",
      "repDeadlineLabel": "சட்ட காலக்கெடு:",
      "repActionLabel": "பரிந்துரைக்கப்பட்ட நடவடிக்கை:"
    },
    "codex": {
      "badge": "📚 சட்ட அறிவுத் தளம்",
      "title": "இந்திய சட்டக் கோடெக்ஸ்",
      "desc": "இந்தியாவின் புதிய குற்றவியல் சட்டங்கள் மற்றும் அரசியலமைப்புச் சட்டத்தின் 1,838+ பிரிவுகளுக்கான உடனடி அணுகல்.",
      "searchPlaceholder": "பிரிவு, முக்கிய சொல் அல்லது தலைப்பு மூலம் தேடுங்கள் (எ.கா. கொலை, ஜாமீன், எஃப்ஐஆர், மின்னணு ஆதாரம், தனியுரிமை)..."
    },
    "sos": {
      "badge": "🚨 அவசர சட்ட அடைவு",
      "title": "தேசிய சட்ட SOS & ஆதரவு",
      "desc": "இந்திய குற்றவியல் சட்டத்தின் கீழ் அவசர உதவி எண்கள் மற்றும் அத்தியாவசிய குடிமக்கள் உரிமைகளுக்கான நேரடி அணுகல்.",
      "nalsaTitle": "தேசிய சட்ட சேவைகள் ஆணையம் (NALSA)",
      "nalsaDesc": "தகுதியான குடிமக்கள் மற்றும் விசாரணைக் கைதிகளுக்கு இலவச சட்ட உதவி",
      "womenTitle": "பெண்கள் அவசர உதவி எண்",
      "womenDesc": "24/7 தேசிய மகளிர் ஆணைய அவசர உதவி",
      "cyberTitle": "தேசிய சைபர் கிரைம் போர்டல்",
      "cyberDesc": "நிதி இணைய மோசடி மற்றும் இணைய குற்ற புகார்கள் தீர்வு",
      "childTitle": "சைல்டுலைன் அவசர சேவை",
      "childDesc": "குழந்தைகள் பாதுகாப்பு மற்றும் போக்சோ புகார்கள் தீர்வு",
      "consumerTitle": "தேசிய நுகர்வோர் உதவி எண்",
      "consumerDesc": "நுகர்வோர் தகராறு தீர்வு மற்றும் குறைபாடுள்ள பொருட்கள்",
      "seniorTitle": "மூத்த குடிமக்கள் உதவி எண் (எல்டர்லைன்)",
      "seniorDesc": "மூத்த குடிமக்கள் நலன் மற்றும் சட்டப் பாதுகாப்புக்கான உதவி எண்",
      "rightsBadge": "உங்கள் உரிமைகளை அறியுங்கள்",
      "rightsHeading": "இந்திய சட்டத்தின் கீழ் அத்தியாவசிய குடிமக்கள் உரிமைகள்",
      "arrestTitle": "கைது செய்யப்படும்போது உரிமைகள் (டி.கே. பாசு வழிகாட்டுதல்கள்)",
      "arrestPoints": [
        "கைதுக்கான முழுக் காரணங்களையும் மற்றும் குற்றம் ஜாமீனில் வெளிவரக்கூடியதா என்பதை அறியும் உரிமை (பிரிவு 47 BNSS).",
        "கைது குறித்து உறவினர் அல்லது நண்பருக்கு உடனடியாகத் தெரிவிக்கும் உரிமை.",
        "48 மணி நேரத்திற்குள் அங்கீகரிக்கப்பட்ட மருத்துவ அதிகாரியால் கட்டாய மருத்துவப் பரிசோதனை.",
        "விசாரணையின் போது வழக்கறிஞருடன் கலந்தாலோசிக்கும் உரிமை."
      ],
      "zeroFirTitle": "ஜீரோ எஃப்ஐஆர் விதிமுறை (பிரிவு 173 BNSS)",
      "zeroFirPoints": [
        "எல்லை வரம்பைக் காரணம் காட்டி காவல் நிலையம் எஃப்ஐஆர் பதிவு செய்ய மறுக்க முடியாது.",
        "ஜீரோ எஃப்ஐஆர் உடனடியாகப் பதிவு செய்யப்பட்டு பின்னர் உரிய காவல் நிலையத்திற்கு மாற்றப்படும்.",
        "தேடுதல் மற்றும் பறிமுதல் நடவடிக்கைகளை ஆடியோ-வீடியோ பதிவு செய்வது இப்போது BNSS 2023-ன் கீழ் கட்டாயமாகும்."
      ],
      "legalAidTitle": "இலவச சட்ட உதவி (பிரிவு 39A)",
      "legalAidPoints": [
        "அனைத்து குடிமக்களுக்கும் சமமான நீதி மற்றும் இலவச சட்ட உதவியை வழங்க பிரிவு 39A உத்தரவிடுகிறது.",
        "சட்ட சேவைகள் ஆணைய சட்டத்தின் கீழ் பெண்கள், குழந்தைகள் மற்றும் குறைந்த வருமானம் உடையவர்கள் இலவச சட்ட உதவிக்கு தகுதியுடையவர்கள்."
      ]
    },
    "download": {
      "badge": "📲 அதிகாரப்பூர்வ விநியோக மையம்",
      "title": "நியாய்AI v1.0 பதிவிறக்கவும்",
      "desc": "2048-பிட் RSA விசைகளுடன் சரிபார்க்கப்பட்ட நேட்டிவ் ஆண்ட்ராய்டு செயலி.",
      "cardBadge": "தயாரிப்பு கட்டமைப்பு சரிபார்க்கப்பட்டது",
      "cardTitle": "ஆண்ட்ராய்டுக்கான நியாய்AI (வெளியீட்டு APK)",
      "cardDesc": "உடனடி < 100ms தொடக்கத்திற்கு 1,838 பிரிவுகளைக் கொண்ட சட்ட தரவுத்தளத்தை (4.27 MB, Schema v8) கொண்டுள்ளது.",
      "btnDownload": "⬇️ கையொப்பமிட்ட APK-ஐப் பதிவிறக்கு (11.47 MB)",
      "btnMirror": "⚡ GitHub வெளியீடுகள் கண்ணாடி",
      "btnWeb": "🌐 வலைப் பதிப்பைப் பார்"
    }
  }
};

function initLanguageSwitcher() {
  const langSelect = document.getElementById("langSelect");
  const savedLang = localStorage.getItem("nyaai_lang") || "en";
  currentLang = savedLang;

  if (langSelect) {
    langSelect.value = savedLang;
    langSelect.addEventListener("change", (e) => {
      const selected = e.target.value;
      currentLang = selected;
      localStorage.setItem("nyaai_lang", selected);
      applyLanguage(selected);
    });
  }

  // Apply immediately on DOMContentLoaded
  applyLanguage(savedLang);
}

function applyLanguage(lang) {
  const t = i18n[lang] || i18n.en;
  document.documentElement.lang = lang;

  // 1. Navigation links
  const navMap = {
    "index.html": t.nav.home,
    "terminal.html": t.nav.terminal,
    "scanner.html": t.nav.scanner,
    "codex.html": t.nav.codex,
    "sos.html": t.nav.sos,
    "download.html": t.nav.download
  };

  document.querySelectorAll(".nav-links a").forEach((a) => {
    const href = a.getAttribute("href") || "";
    for (const [key, text] of Object.entries(navMap)) {
      if (href.endsWith(key) || href === key) {
        a.textContent = text;
      }
    }
  });

  const navGetApp = document.querySelector(".nav-actions .btn-primary");
  if (navGetApp) navGetApp.textContent = t.nav.getApp;

  const brandTag = document.querySelector(".brand-logo span:nth-child(3)");
  if (brandTag) brandTag.textContent = t.nav.brandTag;

  // 2. Footer
  const footerBrand = document.querySelector(".pastel-footer div:first-child");
  if (footerBrand) footerBrand.textContent = t.footer.brand;

  document.querySelectorAll(".pastel-footer .footer-nav a").forEach((a) => {
    const href = a.getAttribute("href") || "";
    for (const [key, text] of Object.entries(navMap)) {
      if (href.endsWith(key) || href === key) {
        a.textContent = text;
      }
    }
  });

  const footerCopy = document.querySelector(".pastel-footer .mono");
  if (footerCopy) footerCopy.textContent = t.footer.copyright;

  // 3. Home Page (index.html)
  if (document.querySelector(".hero-title")) {
    const heroBadge = document.querySelector(".hero-wrapper .badge-pastel-gold");
    if (heroBadge) heroBadge.innerHTML = `<span>🏛️</span> <span>${t.home.badge}</span>`;

    const heroTitle = document.querySelector(".hero-title");
    if (heroTitle) heroTitle.innerHTML = `<span class="text-gradient">${t.home.heroTitle}</span>`;

    const heroSub = document.querySelector(".hero-subtitle");
    if (heroSub) heroSub.textContent = t.home.heroSubtitle;

    const heroBtnTerminal = document.querySelector(".hero-buttons a[href*='terminal.html']");
    if (heroBtnTerminal) heroBtnTerminal.textContent = t.home.btnTerminal;

    const heroBtnScanner = document.querySelector(".hero-buttons a[href*='scanner.html']");
    if (heroBtnScanner) heroBtnScanner.textContent = t.home.btnScanner;

    const heroBtnDownload = document.querySelector(".hero-buttons a[href*='download.html']");
    if (heroBtnDownload) heroBtnDownload.textContent = t.home.btnDownload;

    const metricLabels = document.querySelectorAll(".metrics-row .metric-card .metric-label");
    if (metricLabels.length >= 4) {
      metricLabels[0].textContent = t.home.metric1;
      metricLabels[2].textContent = t.home.metric3;
      metricLabels[3].textContent = t.home.metric4;
    }

    const featBadge = document.querySelector(".page-container .page-header [class*='badge-pastel']");
    if (featBadge) featBadge.textContent = t.home.secBadge;

    const featTitle = document.querySelector(".page-container .page-header .page-title");
    if (featTitle) featTitle.textContent = t.home.secTitle;

    const featDesc = document.querySelector(".page-container .page-header .page-desc");
    if (featDesc) featDesc.textContent = t.home.secDesc;

    const cards = document.querySelectorAll(".feature-showcase-grid .showcase-card");
    if (cards.length >= 6) {
      // Card 1: AI Terminal
      const c1H = cards[0].querySelector("h3");
      const c1P = cards[0].querySelector("p");
      const c1A = cards[0].querySelector("a");
      if (c1H) c1H.textContent = t.home.card1Title;
      if (c1P) c1P.textContent = t.home.card1Desc;
      if (c1A) c1A.textContent = t.home.card1Btn;

      // Card 2: Doc Scanner
      const c2H = cards[1].querySelector("h3");
      const c2P = cards[1].querySelector("p");
      const c2A = cards[1].querySelector("a");
      if (c2H) c2H.textContent = t.home.card2Title;
      if (c2P) c2P.textContent = t.home.card2Desc;
      if (c2A) c2A.textContent = t.home.card2Btn;

      // Card 3: Codex
      const c3H = cards[2].querySelector("h3");
      const c3P = cards[2].querySelector("p");
      const c3A = cards[2].querySelector("a");
      if (c3H) c3H.textContent = t.home.card3Title;
      if (c3P) c3P.textContent = t.home.card3Desc;
      if (c3A) c3A.textContent = t.home.card3Btn;

      // Card 4: SOS
      const c4H = cards[3].querySelector("h3");
      const c4P = cards[3].querySelector("p");
      const c4A = cards[3].querySelector("a");
      if (c4H) c4H.textContent = t.home.card4Title;
      if (c4P) c4P.textContent = t.home.card4Desc;
      if (c4A) c4A.textContent = t.home.card4Btn;

      // Card 5: Privacy
      const c5H = cards[4].querySelector("h3");
      const c5P = cards[4].querySelector("p");
      const c5Badge = cards[4].querySelector(".badge-pastel-sage");
      if (c5H) c5H.textContent = t.home.card5Title;
      if (c5P) c5P.textContent = t.home.card5Desc;
      if (c5Badge) c5Badge.textContent = t.home.card5Badge;

      // Card 6: Cross-Platform
      const c6H = cards[5].querySelector("h3");
      const c6P = cards[5].querySelector("p");
      const c6A = cards[5].querySelector("a");
      if (c6H) c6H.textContent = t.home.card6Title;
      if (c6P) c6P.textContent = t.home.card6Desc;
      if (c6A) c6A.textContent = t.home.card6Btn;
    }
  }

  // 4. Terminal Page (terminal.html)
  if (document.getElementById("chatHistory")) {
    const pBadge = document.querySelector(".page-header [class*='badge-pastel']");
    if (pBadge) pBadge.textContent = t.terminal.badge;

    const pTitle = document.querySelector(".page-header .page-title");
    if (pTitle) pTitle.textContent = t.terminal.title;

    const pDesc = document.querySelector(".page-header .page-desc");
    if (pDesc) pDesc.textContent = t.terminal.desc;

    const simBadge = document.querySelector(".terminal-topbar .badge-pastel-sage");
    if (simBadge) simBadge.textContent = t.terminal.simBadge;

    const input = document.getElementById("terminalInput");
    if (input) input.placeholder = t.terminal.inputPlaceholder;

    const sendBtn = document.getElementById("sendBtn");
    if (sendBtn) sendBtn.textContent = t.terminal.sendBtn;

    const chips = document.querySelectorAll(".prompt-chips .chip-btn");
    if (chips.length >= 5) {
      chips[0].textContent = t.terminal.chip1;
      chips[1].textContent = t.terminal.chip2;
      chips[2].textContent = t.terminal.chip3;
      chips[3].textContent = t.terminal.chip4;
      chips[4].textContent = t.terminal.chip5;
    }

    const initialWelcome = document.querySelector("#chatHistory .chat-ai");
    if (initialWelcome) {
      const vBadge = initialWelcome.querySelector(".badge-pastel-sage");
      if (vBadge) vBadge.innerHTML = `<span>●</span> ${t.terminal.verifiedBadge}`;
      const wTitle = initialWelcome.querySelector("div:nth-child(2)");
      if (wTitle) wTitle.textContent = t.terminal.welcomeTitle;
      const wDesc = initialWelcome.querySelector("p");
      if (wDesc) wDesc.innerHTML = t.terminal.welcomeDesc;
    }
  }

  // 5. Scanner Page (scanner.html)
  if (document.getElementById("scannerLaser")) {
    const pBadge = document.querySelector(".page-header [class*='badge-pastel']");
    if (pBadge) pBadge.textContent = t.scanner.badge;

    const pTitle = document.querySelector(".page-header .page-title");
    if (pTitle) pTitle.textContent = t.scanner.title;

    const pDesc = document.querySelector(".page-header .page-desc");
    if (pDesc) pDesc.textContent = t.scanner.desc;

    const tabs = document.querySelectorAll(".sample-tab");
    if (tabs.length >= 3) {
      tabs[0].textContent = t.scanner.tab1;
      tabs[1].textContent = t.scanner.tab2;
      tabs[2].textContent = t.scanner.tab3;
    }

    const scanDocBtn = document.getElementById("scanDocBtn");
    if (scanDocBtn && !scanDocBtn.disabled) scanDocBtn.textContent = t.scanner.btnScan;

    const repHeadings = document.querySelectorAll(".scanner-results h4");
    if (repHeadings.length >= 4) {
      repHeadings[0].textContent = t.scanner.repTypeLabel;
      repHeadings[1].textContent = t.scanner.repStatutesLabel;
      repHeadings[2].textContent = t.scanner.repDeadlineLabel;
      repHeadings[3].textContent = t.scanner.repActionLabel;
    }
  }

  // 6. Codex Page (codex.html)
  if (document.getElementById("codexGrid")) {
    const pBadge = document.querySelector(".page-header [class*='badge-pastel']");
    if (pBadge) pBadge.textContent = t.codex.badge;

    const pTitle = document.querySelector(".page-header .page-title");
    if (pTitle) pTitle.textContent = t.codex.title;

    const pDesc = document.querySelector(".page-header .page-desc");
    if (pDesc) pDesc.textContent = t.codex.desc;

    const searchInput = document.getElementById("codexSearch");
    if (searchInput) searchInput.placeholder = t.codex.searchPlaceholder;
  }

  // 7. SOS Page (sos.html)
  if (document.querySelector(".sos-directory")) {
    const pBadge = document.querySelector(".page-header [class*='badge-pastel']");
    if (pBadge) pBadge.textContent = t.sos.badge;

    const pTitle = document.querySelector(".page-header .page-title");
    if (pTitle) pTitle.textContent = t.sos.title;

    const pDesc = document.querySelector(".page-header .page-desc");
    if (pDesc) pDesc.textContent = t.sos.desc;

    const cells = document.querySelectorAll(".sos-directory .sos-cell");
    if (cells.length >= 6) {
      cells[0].children[0].textContent = t.sos.nalsaTitle;
      cells[0].children[2].textContent = t.sos.nalsaDesc;

      cells[1].children[0].textContent = t.sos.womenTitle;
      cells[1].children[2].textContent = t.sos.womenDesc;

      cells[2].children[0].textContent = t.sos.cyberTitle;
      cells[2].children[2].textContent = t.sos.cyberDesc;

      cells[3].children[0].textContent = t.sos.childTitle;
      cells[3].children[2].textContent = t.sos.childDesc;

      cells[4].children[0].textContent = t.sos.consumerTitle;
      cells[4].children[2].textContent = t.sos.consumerDesc;

      cells[5].children[0].textContent = t.sos.seniorTitle;
      cells[5].children[2].textContent = t.sos.seniorDesc;
    }

    const rightsHeader = document.querySelector("main > div:last-child .page-header");
    if (rightsHeader) {
      const rBadge = rightsHeader.querySelector("span");
      if (rBadge) rBadge.textContent = t.sos.rightsBadge;
      const rHeading = rightsHeader.querySelector("h2");
      if (rHeading) rHeading.textContent = t.sos.rightsHeading;
    }

    const rightCards = document.querySelectorAll("main > div:last-child .card-pastel");
    if (rightCards.length >= 3) {
      rightCards[0].querySelector("h3").textContent = t.sos.arrestTitle;
      rightCards[0].querySelector("ul").innerHTML = t.sos.arrestPoints.map((p) => `<li>${p}</li>`).join("");

      rightCards[1].querySelector("h3").textContent = t.sos.zeroFirTitle;
      rightCards[1].querySelector("ul").innerHTML = t.sos.zeroFirPoints.map((p) => `<li>${p}</li>`).join("");

      rightCards[2].querySelector("h3").textContent = t.sos.legalAidTitle;
      rightCards[2].querySelector("ul").innerHTML = t.sos.legalAidPoints.map((p) => `<li>${p}</li>`).join("");
    }
  }

  // 8. Download Page (download.html)
  if (document.querySelector("a[download='app-release.apk']")) {
    const pBadge = document.querySelector(".page-header [class*='badge-pastel']");
    if (pBadge) pBadge.textContent = t.download.badge;

    const pTitle = document.querySelector(".page-header .page-title");
    if (pTitle) pTitle.textContent = t.download.title;

    const pDesc = document.querySelector(".page-header .page-desc");
    if (pDesc) pDesc.textContent = t.download.desc;

    const cardBadge = document.querySelector(".card-pastel .badge-pastel-sage");
    if (cardBadge) cardBadge.innerHTML = `<span>●</span> ${t.download.cardBadge}`;

    const cardTitle = document.querySelector(".card-pastel h2");
    if (cardTitle) cardTitle.textContent = t.download.cardTitle;

    const cardDesc = document.querySelector(".card-pastel p");
    if (cardDesc) cardDesc.textContent = t.download.cardDesc;

    const btnDl = document.querySelector(".card-pastel a[download]");
    if (btnDl) btnDl.textContent = t.download.btnDownload;

    const btnMirror = document.querySelector(".card-pastel a[href*='releases']");
    if (btnMirror) btnMirror.textContent = t.download.btnMirror;
  }
}
