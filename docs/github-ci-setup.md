# GitHub CI Secret Setup

**Priority:** P0 (Required for FCM in release builds)  
**Time required:** 5 minutes  
**Status:** ⚠️ Manual action required

---

## What is this?

The Android CI workflow (`.github/workflows/android-ci.yml`) needs the Firebase configuration file to build releases with working push notifications. Currently, the workflow generates a dummy `google-services.json` file when the secret is missing, which allows builds to succeed but results in non-functional FCM at runtime.

---

## Steps

### 1. Copy the google-services.json content

The file already exists in the repository at:
```
apps/android/app/google-services.json
```

Open it and copy the **entire file contents** (already in your local clone).

### 2. Add the secret to GitHub

1. Go to your GitHub repository: **<username>/ElderCare-Guardian**
2. Click **Settings** (top menu)
3. In the left sidebar, click **Secrets and variables** → **Actions**
4. Click **New repository secret** (green button)
5. Fill in:
   - **Name:** `GOOGLE_SERVICES_JSON`
   - **Secret:** Paste the entire contents of `apps/android/app/google-services.json`
6. Click **Add secret**

### 3. Verify the secret is set

After adding:
1. Go to **Actions** tab in GitHub
2. Manually trigger the **Android CI** workflow (or push a commit)
3. Check the **Build debug APK** and **Build signed release AAB** steps
4. The log should show: "Writing google-services.json from secret"

---

## Current file contents (for reference)

```json
{
  "project_info": {
    "project_number": "126763086057",
    "project_id": "eldercare-58d1c",
    "storage_bucket": "eldercare-58d1c.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:126763086057:android:88ae71e6b7948774d58078",
        "android_client_info": {
          "package_name": "com.eldercareguardian"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "AIzaSyAtuUuh2oXWdhRh63osEXv7LSABVvTuJFU"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": []
        }
      }
    }
  ],
  "configuration_version": "1"
}
```

---

## Why is this needed?

- **Debug builds:** Will work with or without the secret (dummy file is sufficient for development)
- **Release builds (AAB):** Must have the real `google-services.json` for FCM to work in production
- **CI automation:** Allows GitHub Actions to build release AABs without storing secrets in the repository

---

## Security note

The `google-services.json` file contains:
- Firebase project ID (public)
- Android API key (restricted to `com.eldercareguardian` package via Firebase Console)

This is **not a secret key** — it's safe to commit (though we exclude it via `.gitignore`). The CI secret is only needed because the workflow runs in a clean environment without the local file.

The **server account key** (for FCM backend) is different and should **never** be committed or exposed.

---

## Related files

- `.github/workflows/android-ci.yml` — CI workflow that uses this secret
- `apps/android/app/google-services.json` — The actual file (gitignored)
- `NEXT_STEPS.md` — Task #5 (this document fulfills that task)
