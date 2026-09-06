# 🌐 NYAAI — Futuristic Web Application

This directory (`web/`) contains the standalone, futuristic web application for **NYAAI (न्यायAI)**. It is completely isolated from the Android mobile application codebase (`app/`), allowing it to be hosted on GitHub Pages, Vercel, Netlify, or any static web hosting service.

---

## ⚡ Key Features

1. **Dynamic Neural Constellation & Scales Canvas**: Interactive HTML5 canvas rendering neural network constellations and mathematical legal justice wireframes reacting in real-time to mouse gestures.
2. **Live Legal AI Terminal (Playground)**:
   - Interactive chat simulator providing grounded statutory citations across BNS 2023, BNSS 2023, BSA 2023, and Constitution of India.
   - Text-to-Speech (TTS) audio narration via Web Speech Synthesis API.
   - Speech-to-Text (STT) voice input via Web Speech Recognition API.
   - Real-time confidence metrics (e.g. `98.7% High Confidence`).
3. **On-Device Legal Document Scanner & OCR Simulator**:
   - Interactive document optical scan with sweeping laser grid animation.
   - Simulated text bounding boxes on legal notices (Section 138 NI Act, Cyber Crime FIR under Section 173 BNSS, NDA Breach Notice).
   - Structured extraction output detailing Document Type, Governing Provisions, Deadlines, and Recommended Action Steps.
4. **3D Statutory Codex**:
   - Real-time search filter across key provisions of Bharatiya Nyaya Sanhita, BNSS, BSA, and the Constitution of India.
5. **National Legal SOS Radar**:
   - One-click access to Indian emergency legal aid helplines (NALSA `15100`, Women Helpline `1091`, Cybercrime `1930`, Childline `1098`).
6. **Production Release Download Hub**:
   - Direct download access to `app-release.apk` (11.47 MB) with scannable QR code.
7. **Multilingual Interface**:
   - Dynamic UI localization across English, Hindi (हिन्दी), Bengali (বাংলা), Telugu (తెలుగు), and Tamil (தமிழ்).

---

## 🚀 How to Run Locally

### Option 1: Python Built-in Server (Recommended)
```bash
# Navigate to the web folder and start HTTP server
cd web
python -m http.server 8000
```
Open your browser at `http://localhost:8000`.

### Option 2: Node.js / NPX Serve
```bash
cd web
npx serve .
```

### Option 3: Open Directly
Double-click `web/index.html` to open it in Chrome, Edge, Brave, or Firefox.

---

## 🌐 Free Deployment Guide

### Deploying to GitHub Pages
1. Push this repository to GitHub.
2. In GitHub, navigate to **Settings → Pages**.
3. Under **Build and deployment → Source**, choose **Deploy from a branch**.
4. Select `main` branch and folder `/web`.
5. Click **Save**. Your site will be live at `https://Lunaca47.github.io/Nyaai/`.

### Deploying to Vercel or Netlify
1. Connect your GitHub repository.
2. Set **Root Directory** to `web`.
3. Leave build command empty. Output directory is `.`.
4. Deploy.
