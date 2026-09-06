# Google Play Store Deployment Checklist for NYAAI

This guide provides a comprehensive checklist for preparing and publishing **NYAAI** on the Google Play Store using Fastlane or manual Play Console upload.

---

## 1. Google Play Console Setup & Prerequisites

### A. Google Play Developer Account
- [ ] Active Google Play Developer account ($25 USD one-time registration).
- [ ] Completed merchant / identity verification and D-U-N-S verification (for organizations).

### B. Google Cloud Service Account (for Fastlane Automation)
1. In Google Cloud Console, open the project linked to your Google Play Console.
2. Navigate to **IAM & Admin > Service Accounts** and click **Create Service Account**.
3. Name it `play-store-fastlane-deployer`.
4. Grant role: `Service Account User`.
5. Under **Actions**, select **Manage Keys > Add Key > Create new key > JSON**.
6. Save the downloaded JSON key file as `fastlane/play-store-credentials.json` (Note: this file is automatically excluded by `.gitignore`).
7. In **Google Play Console**, go to **Users and Permissions > Invite new users**, add the service account email, and grant **Releases** permissions (Create, edit, and roll out releases).

---

## 2. Graphic Asset Specifications

Prepare the following visual assets in the specified dimensions:

| Asset | Dimensions | Format | Requirement | Notes |
|---|---|---|---|---|
| **App Icon** | 512 x 512 px | PNG (32-bit), max 1 MB | Mandatory | High-resolution icon with no transparent background |
| **Feature Graphic** | 1024 x 500 px | JPEG or 24-bit PNG, max 15 MB | Mandatory | Banner promoting NYAAI AI capabilities |
| **Phone Screenshots** | Min 2, Max 8 (e.g. 1080 x 2400 px) | PNG or JPEG, 16:9 or 9:16 | Mandatory | Show Chat, OCR Document Scanner, Bookmarks, and Settings |
| **7-inch Tablet Screenshots** | Min 1 (optional) | 1200 x 1920 px | Recommended | |
| **10-inch Tablet Screenshots** | Min 1 (optional) | 1600 x 2560 px | Recommended | |

---

## 3. App Content & Policy Questionnaires

Under **Policy and programs > App content** in Play Console:

- [ ] **Privacy Policy:** Link to hosted URL (e.g., GitHub Pages hosting `PRIVACY_POLICY.md`).
- [ ] **Data Safety:** Complete using values from [`DATA_SAFETY_GUIDE.md`](DATA_SAFETY_GUIDE.md).
- [ ] **Target Audience & Content:**
  - Age groups: **18 and older**.
  - Appeal to children: **No**.
- [ ] **Government Apps:** Select **No** (NYAAI is not an official government app).
- [ ] **Financial Features:** Select **No**.
- [ ] **Content Rating (IARC):**
  - Category: **Reference, News, or Educational**.
  - Violence, profanity, drugs, sexuality: **No**.
  - Users can exchange messages? **No** (user interacts only with local database and AI).
  - Physical location shared? **No**.
  - Expected rating: **PEGI 3 / Everyone / ESRB Everyone**.

---

## 4. Production Release Execution

### Option A: Automated via Fastlane (Recommended)
Once `fastlane/play-store-credentials.json` is placed:
```bash
# 1. Run test suite
bundle exec fastlane android test

# 2. Build and upload directly to Google Play Internal Test Track
bundle exec fastlane android internal

# 3. Promote to production after testing
bundle exec fastlane android promote_to_production
```

### Option B: Manual Upload via Google Play Console
1. Generate the signed release bundle:
   ```powershell
   .\gradlew.bat bundleRelease --no-daemon
   ```
2. In Google Play Console, navigate to **Release > Internal testing** (or **Production**).
3. Click **Create new release**.
4. Upload `app/build/outputs/bundle/release/app-release.aab`.
5. Copy release notes from `fastlane/metadata/android/en-US/changelogs/1.txt`.
6. Click **Save and Review Release**.
