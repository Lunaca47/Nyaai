# 🌐 NYAAI — Web Client (GitHub Pages)

This directory (`docs/`) contains the web client for **NYAAI (न्यायAI)** deployed to GitHub Pages at [https://lunaca47.github.io/Nyaai/](https://lunaca47.github.io/Nyaai/).

The client connects to the NYAAI FastAPI backend to run authentic hybrid statutory retrieval, LLM generation, and citation verification.

---

## ⚡ Key Features

1. **Grounded Legal Terminal**:
   - Live statutory research calling the NYAAI backend pipeline (`/api/v1/retrieval/search`, `/api/v1/generate`, `/api/v1/verify`).
   - Authentic citation verification badges matching the Android app (`PASSED`, `ANNOTATED_MODEL_LAW`, `ANNOTATED_UNGROUNDED`, `ANNOTATED_REPEALED`, `REJECTED_UNGROUNDED`).
   - Text-to-Speech (TTS) audio narration via Web Speech Synthesis API.
   - Speech-to-Text (STT) voice input via Web Speech Recognition API.
2. **On-Device Legal Document Scanner & OCR Simulator**:
   - Interactive document optical scan with sweeping laser grid animation.
   - Simulated text bounding boxes on legal notices.
3. **Statutory Codex Explorer**:
   - Real-time search across Bharatiya Nyaya Sanhita, BNSS, BSA, and Constitution provisions.
4. **National Legal SOS Radar**:
   - Verified Indian emergency legal aid helplines (NALSA `15100`, Women Helpline `1091`, Cybercrime `1930`, Childline `1098`) fully functional offline.
5. **Production Release Download Hub**:
   - Direct download access to `app-release.apk` (11.4 MB) with scannable QR code.
6. **Multilingual Interface**:
   - Dynamic UI localization across English, Hindi (हिन्दी), Bengali (বাংলা), Telugu (తెలుగు), and Tamil (தமிழ்).

---

## 🚀 How to Run Locally

### 1. Start the Backend
```bash
cd backend
# With venv activated:
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2. Serve the Web Client
```bash
cd docs
python -m http.server 3000
```
Open `http://localhost:3000` in your browser. The web client will automatically connect to `http://localhost:8000`.

### 3. Configuring Remote Backend
To connect the web client to a remote backend instance, specify the URL in the browser console or settings:
```javascript
localStorage.setItem("nyaai_api_url", "https://your-backend-service.run.app");
```

---

## 🌐 Free Deployment Guide

### Deploying to GitHub Pages
1. Push this repository to GitHub.
2. In GitHub, navigate to **Settings → Pages**.
3. Under **Build and deployment → Source**, choose **Deploy from a branch**.
4. Select `main` branch and folder `/docs`.
5. Click **Save**. Your site will be live at `https://lunaca47.github.io/Nyaai/`.

### Deploying to Vercel or Netlify
1. Connect your GitHub repository.
2. Set **Root Directory** to `web`.
3. Leave build command empty. Output directory is `.`.
4. Deploy.
