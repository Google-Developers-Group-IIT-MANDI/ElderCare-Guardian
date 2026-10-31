# Play Store Submission Guide — ElderCare Guardian

**Priority:** P1 (Required for pilot distribution)  
**Time required:** 2–3 hours (first-time setup)  
**Prerequisites:** 
- Google Play Developer account ($25 one-time fee)
- Signed release AAB (`./gradlew bundleRelease`)
- Screenshots prepared (✅ already in `docs/play-store-screenshots/`)

---

## Overview

This guide walks through the complete Play Store submission process, from developer account creation to internal testing release. ElderCare Guardian will be released to the **Internal Testing** track first for team validation, then promoted to production after successful FCM and Samsung Health tests.

---

## Part 1: Pre-submission checklist

### ✅ Required assets (all ready)

| Asset | Location | Status |
|-------|----------|--------|
| App icon (512×512) | `docs/play-store-screenshots/icon-512.png` | ✅ |
| Feature graphic (1024×500) | `docs/play-store-screenshots/feature-graphic-1024x500.png` | ✅ |
| Screenshot 1: Vitals | `docs/play-store-screenshots/screenshot_01_vitals.png` | ✅ |
| Screenshot 2: Safety | `docs/play-store-screenshots/screenshot_02_safety.png` | ✅ |
| Screenshot 3: Caregiver | `docs/play-store-screenshots/screenshot_03_caregiver.png` | ✅ |
| Screenshot 4: Readiness | `docs/play-store-screenshots/screenshot_04_readiness.png` | ✅ |
| Screenshot 5: Settings | `docs/play-store-screenshots/screenshot_05_settings.png` | ✅ |
| Screenshot 6: Consent | `docs/play-store-screenshots/screenshot_06_consent.png` | ✅ |

### ✅ Privacy policy (hosted)

**URL:** https://eldercareguardian.pages.dev  
**Status:** ✅ Live on Cloudflare Pages (see `apps/privacy-policy/`)

### ✅ Build signed release AAB

```bash
cd apps/android
./gradlew bundleRelease

# Output: apps/android/app/build/outputs/bundle/release/app-release.aab
```

**Verify the signature:**
```bash
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

Expected output: `jar verified.`

---

## Part 2: Google Play Developer account setup

### Step 1: Create a developer account

1. **Go to:** https://play.google.com/console
2. **Sign in** with a Google account (use IIT Mandi team account or personal)
3. Click **Create account** → **Developer account**
4. Choose **Organization** (recommended for team projects)
   - Organization name: `IIT Mandi — ElderCare Guardian`
   - Country: `India`
5. **Read and accept** the Developer Distribution Agreement
6. **Pay the $25 registration fee** (one-time, credit card required)
7. **Verify your email** (check inbox for Google Play verification link)

**Timeline:** Account approval takes 24–48 hours. Check email for approval notification.

---

## Part 3: Create the app listing

### Step 2: Create a new app

1. **Go to:** https://play.google.com/console → **All apps**
2. Click **Create app** (blue button, top-right)
3. Fill in:
   - **App name:** ElderCare Guardian
   - **Default language:** English (United States)
   - **App or game:** App
   - **Free or paid:** Free
4. **Declarations:**
   - ✅ I confirm this app complies with Google Play policies
   - ✅ I confirm this app complies with US export laws
5. Click **Create app**

### Step 3: Set up store listing

Navigate to **Store presence → Main store listing** (left sidebar):

#### App details

| Field | Value |
|-------|-------|
| **App name** | ElderCare Guardian |
| **Short description** (80 chars max) | Real-time fall detection and vitals monitoring for elderly care. |
| **Full description** | ElderCare Guardian pairs with a BLE wearable (ESP32-C3) to monitor heart rate, SpO2, motion, and temperature. On-device ML detects falls, dehydration, and cardiac anomalies. Caregiver alerts are sent via SMS or push notification. All health data is encrypted locally — never uploaded to the cloud.<br><br>**Features:**<br>• Fall detection with IMU-based posture analysis<br>• SpO2 + heart rate monitoring (MAX30102)<br>• ECG anomaly detection (AFib, tachycardia, bradycardia)<br>• Caregiver alert system (SMS + FCM push)<br>• Samsung Health integration<br>• DPDPA-compliant data export and deletion<br><br>**Hardware Required:**<br>ESP32-C3 wearable with MAX30102 sensor (prototype available for IIT Mandi pilot program). |

#### Graphics

1. **App icon:** Upload `docs/play-store-screenshots/icon-512.png`
2. **Feature graphic:** Upload `docs/play-store-screenshots/feature-graphic-1024x500.png`
3. **Phone screenshots:** Upload all 6 screenshots in order:
   - `screenshot_01_vitals.png`
   - `screenshot_02_safety.png`
   - `screenshot_03_caregiver.png`
   - `screenshot_04_readiness.png`
   - `screenshot_05_settings.png`
   - `screenshot_06_consent.png`

#### Categorization

| Field | Value |
|-------|-------|
| **App category** | Medical |
| **Tags** | eldercare, fall-detection, health-monitor, wearable, caregiver |

#### Contact details

| Field | Value |
|-------|-------|
| **Email** | eldercare@iitmandi.ac.in (or team member's email) |
| **Phone** | +91 XXXX XXXXXX (optional) |
| **Website** | https://eldercareguardian.pages.dev |

#### External marketing (optional)

Leave blank for now — add YouTube demo video post-pilot.

**Save draft.**

---

## Part 4: App content

Navigate to **Policy → App content** (left sidebar):

### Step 4: Privacy policy

1. Click **Privacy policy** → **Start**
2. **Privacy policy URL:** https://eldercareguardian.pages.dev
3. Click **Save**

### Step 5: App access

1. Click **App access** → **Start**
2. **All or some functionality is restricted:** No
3. **Explanation:** All features are available without special access.
4. Click **Save**

### Step 6: Ads

1. Click **Ads** → **Start**
2. **Does your app contain ads?** No
3. Click **Save**

### Step 7: Content rating

1. Click **Content rating** → **Start questionnaire**
2. **Email address:** eldercare@iitmandi.ac.in
3. **Category:** Utility, Productivity, Communication, or Other
4. **Questionnaire:**
   - Violence: None
   - Sexual content: None
   - Language: None
   - Controlled substances: None
   - Gambling: None
   - User-generated content: No
   - Sharing location with other users: No
5. Click **Submit** → **Apply rating**

Expected rating: **Everyone** (suitable for all ages)

### Step 8: Target audience

1. Click **Target audience** → **Start**
2. **Target age:** 18 and over (elderly users + caregivers)
3. **Appeals to children:** No
4. Click **Save**

### Step 9: News app

1. Click **News app** → **Start**
2. **Is this a news app?** No
3. Click **Save**

### Step 10: Data safety

This is the most important section. Click **Data safety** → **Start**.

#### Data collection and security

**Question 1: Does your app collect or share user data?**  
Answer: **Yes**

#### Data types collected

| Data type | Collected? | Shared? | Optional? | Purpose |
|-----------|-----------|---------|-----------|---------|
| **Health and fitness** | ✅ Yes | ❌ No | ❌ Required | App functionality, Analytics |
| • Heart rate | ✅ | ❌ | ❌ | |
| • SpO2 | ✅ | ❌ | ❌ | |
| • Body temperature | ✅ | ❌ | ❌ | |
| • Physical activity | ✅ | ❌ | ❌ | |
| **Personal info** | ✅ Yes | ❌ No | ❌ Required | App functionality |
| • Name | ✅ | ❌ | ❌ | Patient profile |
| • Phone number | ✅ | ❌ | ✅ | Caregiver SMS alerts |
| **Device or other IDs** | ✅ Yes | ❌ No | ❌ Required | App functionality |
| • BLE device MAC address | ✅ | ❌ | ❌ | |

#### Data security practices

**Question 2: Is data encrypted in transit?**  
Answer: **Yes** (BLE bonding with MITM protection)

**Question 3: Is data encrypted at rest?**  
Answer: **Yes** (AES-256-GCM via SQLCipher)

**Question 4: Can users request data deletion?**  
Answer: **Yes** (Settings → Delete all data, DPDPA-compliant)

**Question 5: Is data shared with third parties?**  
Answer: **No** (all processing is on-device, no cloud upload)

**Question 6: Is data processed on device?**  
Answer: **Yes** (ML models run locally, no external API calls)

**Data handling summary:**
> ElderCare Guardian collects health data (heart rate, SpO2, temperature, motion) from a paired BLE wearable device. All data is encrypted and stored locally using SQLCipher. No data is uploaded to remote servers. Users can export and delete their data at any time via Settings. Caregiver phone numbers are optionally collected for SMS alerts and are never shared with third parties.

Click **Save**.

---

## Part 5: Set up Android app

Navigate to **Release → Setup → App integrity** (left sidebar):

### Step 11: App signing

1. Click **App signing** → **Use Google Play App Signing** (recommended)
2. **Upload key:** Google generates a production signing key and stores it securely
3. **What you upload:** Upload a signed AAB with your **upload key** (the `eldercare-release.jks` keystore)
4. Click **Continue**

**Why this is better:**
- Google manages the production signing key
- If your upload key is lost, you can request a reset
- Easier key rotation for security updates

---

## Part 6: Create a release

### Step 12: Internal testing release

Navigate to **Release → Testing → Internal testing** (left sidebar):

1. Click **Create new release**
2. **Upload the AAB:**
   - Drag `apps/android/app/build/outputs/bundle/release/app-release.aab` to the upload area
   - Wait for upload to complete (Google will analyze the AAB and show app details)
3. **Release name:** `1.0.0-beta` (auto-populated from `versionName` in `build.gradle.kts`)
4. **Release notes:**
   ```
   Initial internal testing release for IIT Mandi pilot program.
   
   Features:
   - Real-time vitals monitoring (HR, SpO2, temp)
   - Fall detection with IMU posture analysis
   - Caregiver alert system (SMS + FCM push)
   - Samsung Health integration
   - DPDPA-compliant data privacy
   
   Known limitations:
   - Requires ESP32-C3 wearable (prototype only)
   - Samsung Health requires Developer Mode (awaiting Partner approval)
   ```
5. Click **Save**

### Step 13: Add internal testers

1. **Create an email list:**
   - List name: `IIT Mandi Team`
   - Emails: Add team members (Pranay, Ariyan, Reman Dey, advisors)
2. **Opt-in URL:** Copy the generated URL (looks like `https://play.google.com/apps/internaltest/...`)
3. **Share the URL** with testers via email or Slack

### Step 14: Review and rollout

1. Click **Review release**
2. **Check for errors:** Google will flag any policy violations or missing metadata
3. If all checks pass, click **Start rollout to Internal testing**
4. **Confirm:** Release will be live in 1–2 hours

---

## Part 7: Post-submission

### Step 15: Monitor the release

1. **Go to:** Release dashboard → Internal testing
2. **Check status:** Processing → Available (takes 1–2 hours)
3. **Install on test devices:**
   - Open the opt-in URL on an Android device
   - Tap **Become a tester**
   - Tap **Download from Play Store**
4. **Run FCM test:** Follow `docs/fcm-test-guide.md` with the Play Store build

### Step 16: Promote to production (after pilot validation)

Once internal testing is successful:
1. Navigate to **Release → Production**
2. Click **Promote release** → Select the internal testing build
3. **Staged rollout:** Start with 10% of users (safety net)
4. **Monitor crash rate:** If crash-free rate > 99%, increase to 50% after 24h
5. **Full rollout:** After 48h with no critical issues, increase to 100%

---

## Troubleshooting

### "Account verification pending"
- Timeline: 24–48 hours after payment
- Check email for updates
- Check Play Console dashboard for status

### "App name already taken"
- Someone else registered "ElderCare Guardian"
- Options:
  1. Try "ElderCare Guardian — IIT Mandi"
  2. Try "ElderGuardian"
  3. Try "SafeGuard Elderly Monitor"

### "APK/AAB signature verification failed"
- The AAB is not signed with the upload keystore
- Re-run: `./gradlew clean bundleRelease`
- Verify keystore path in `apps/android/app/build.gradle.kts`

### "Privacy policy URL unreachable"
- Google's crawler couldn't access https://eldercareguardian.pages.dev
- Check: Cloudflare Pages deployment status
- Wait 10 minutes and retry

### "Data safety section incomplete"
- All questions must be answered, even if "No"
- Common miss: "Can users request data deletion?" (must be Yes for DPDPA compliance)

### "SEND_SMS permission requires declaration"
- Play Console → App content → Permissions
- Add declaration:
  - Permission: `android.permission.SEND_SMS`
  - Purpose: Emergency caregiver SMS alerts
  - Justification: See `docs/play-store-listing.md` → SEND_SMS section

---

## Timeline estimate

| Day | Milestone |
|-----|-----------|
| 0 | Create developer account, pay $25 fee |
| 1–2 | Account verification (wait for Google email) |
| 2 | Complete store listing, app content, data safety |
| 2 | Upload AAB to Internal testing, add testers |
| 2–3 | Internal testing release goes live (1–2 hours after submission) |
| 3–7 | Team testing + FCM validation (see `docs/fcm-test-guide.md`) |
| 8+ | Promote to production (staged rollout) |

---

## Security checklist before production

✅ Revoke debug keystore access (if any test builds were signed with debug)  
✅ Rotate Firebase service account key (backend only)  
✅ Enable ProGuard obfuscation (already enabled in `build.gradle.kts`)  
✅ Verify SQLCipher encryption (run Room Inspector on debug build)  
✅ Test data deletion flow (DPDPA requirement)  
✅ Confirm FCM push notifications work on Play Store build  
✅ Test Samsung Health integration with production AAB  

---

## Related files

- `docs/play-store-listing.md` — Store listing content reference
- `docs/play-store-screenshots/` — All required graphics (✅ ready)
- `apps/android/app/build.gradle.kts` — Version code/name, signing config
- `DEPLOYMENT_PLAN.md` — CI/CD pipeline and keystore management
- `docs/fcm-test-guide.md` — FCM validation after Play Store install
- `NEXT_STEPS.md` — Task #9 (this document fulfills that task)

---

## Next steps after Play Store approval

1. Submit Samsung Health Partner Program application (see `docs/samsung-health-partnership.md`)
2. Run 50+ labeled fall events for SisFall recalibration (see `NEXT_STEPS.md` task #11)
3. Train TFLite ECG model on MIT-BIH + PTB-XL (see `docs/ml-roadmap.md`)
4. Apply for CE marking (Europe) or FDA 510(k) (USA) if expanding beyond India

---

## Support

- **Play Console Help:** https://support.google.com/googleplay/android-developer
- **Policy violations:** https://play.google.com/console → Policy status
- **Developer Support:** https://support.google.com/googleplay/android-developer/contact/dev_tools

**Good luck with the submission!** 🚀
