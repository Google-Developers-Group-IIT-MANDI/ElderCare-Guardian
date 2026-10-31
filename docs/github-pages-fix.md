# GitHub Pages Deployment Fix

**Issue:** The `.github/workflows/pages.yml` workflow fails to deploy  
**Root cause:** GitHub Pages environment not configured or permissions issue

---

## Quick Fix: Enable GitHub Pages in Repository Settings

### Step 1: Enable GitHub Pages

1. Go to your GitHub repository: **<username>/ElderCare-Guardian**
2. Click **Settings** (top menu)
3. In the left sidebar, click **Pages**
4. Under **Build and deployment**:
   - **Source:** Select **GitHub Actions** (not "Deploy from a branch")
5. Click **Save**

### Step 2: Verify Workflow Permissions

1. Still in **Settings**
2. Click **Actions** → **General** (left sidebar)
3. Scroll to **Workflow permissions**
4. Select **Read and write permissions**
5. ✅ Check **Allow GitHub Actions to create and approve pull requests**
6. Click **Save**

### Step 3: Re-run the Workflow

1. Go to **Actions** tab
2. Find the failed **Deploy ToS to GitHub Pages** run
3. Click **Re-run jobs** → **Re-run all jobs**

Expected result: ✅ Deployment succeeds, Pages available at `https://<username>.github.io/ElderCare-Guardian/`

---

## What I Fixed in the Workflow

1. **Removed `environment` block** — The `github-pages` environment is auto-created by GitHub but can cause issues if not properly initialized
2. **Changed `cancel-in-progress: false`** — Prevents race conditions if multiple pushes happen quickly
3. **Added file existence checks** — The workflow now fails early with a clear error if source files are missing

---

## Alternative: Use Cloudflare Pages Instead

If GitHub Pages continues to fail, Cloudflare Pages is a better option (already mentioned in the README as https://eldercareguardian.pages.dev):

### Cloudflare Pages Setup (5 minutes)

1. **Go to:** https://pages.cloudflare.com
2. **Sign in** with GitHub
3. Click **Create a project**
4. Select the **ElderCare-Guardian** repository
5. **Build settings:**
   - Framework preset: **None**
   - Build command: (leave empty)
   - Build output directory: `/apps`
6. **Environment variables:** (none needed)
7. Click **Save and Deploy**

**Result:** Your site is live at your configured Cloudflare Pages domain (or custom domain if you have one)

**Privacy Policy URL:** `https://<your-domain>/privacy-policy/`<br>
**ToS URL:** `https://<your-domain>/tos/`

---

## Verify Deployment

Once deployed (GitHub Pages or Cloudflare), test these URLs:

```bash
# Root (should redirect to ToS)
curl -I https://<your-domain>/

# Terms of Service
curl https://<your-domain>/tos/ | grep "Terms of Service"

# Privacy Policy
curl https://<your-domain>/privacy-policy/ | grep "Privacy Policy"
```

---

## Common Errors and Fixes

### Error: "Error: No uploaded artifact was found!"

**Cause:** The `actions/upload-pages-artifact@v3` step failed  
**Fix:** Check that `_site` directory contains the expected files:
```yaml
- name: Debug artifact
  run: |
    ls -la _site/
    cat _site/index.html
```

### Error: "Error: HttpError: Not Found"

**Cause:** GitHub Pages is not enabled in repository settings  
**Fix:** Follow Step 1 above (enable GitHub Pages with "GitHub Actions" source)

### Error: "Error: HttpError: Resource not accessible by integration"

**Cause:** Workflow doesn't have `pages: write` permission  
**Fix:** Already added in the updated workflow — verify in **Settings → Actions → General → Workflow permissions**

### Error: "Error: A path inside your .github/workflows directory seems to be ignored"

**Cause:** `.gitignore` is blocking the workflow file  
**Fix:** Verify `.github/workflows/pages.yml` is committed:
```bash
git ls-files .github/workflows/pages.yml
# Should output: .github/workflows/pages.yml
```

---

## Testing Locally

Before pushing, test the Pages content locally:

```bash
cd /path/to/ElderCare-Guardian

# Build the _site directory
mkdir -p _site/tos _site/privacy-policy
cp apps/tos/index.html _site/tos/index.html
cp apps/privacy-policy/index.html _site/privacy-policy/index.html

cat > _site/index.html <<'EOF'
<!DOCTYPE html>
<html><head>
  <meta http-equiv="refresh" content="0;url=tos/">
  <title>ElderCare Guardian</title>
</head><body>
  <p><a href="tos/">Terms of Service</a> | <a href="privacy-policy/">Privacy Policy</a></p>
</body></html>
EOF

# Serve with Python
python3 -m http.server 8000 --directory _site

# Open http://localhost:8000 in your browser
```

Expected:
- Root → redirects to `/tos/`
- `/tos/` → Shows Terms of Service
- `/privacy-policy/` → Shows Privacy Policy

---

## Update Privacy Policy URLs

After deployment succeeds, update the privacy policy URL in:

1. **`docs/play-store-submission-guide.md`** (if you use GitHub Pages):
   ```
   https://<username>.github.io/ElderCare-Guardian/privacy-policy/
   ```

2. **Android app** (if hardcoded):
   - Check `apps/android/app/src/main/res/values/strings.xml` for any privacy policy URLs

3. **README.md** (currently shows Cloudflare Pages URL — update if switching to GitHub Pages)

---

## Recommended: Use Cloudflare Pages

**Why Cloudflare Pages > GitHub Pages for this project:**
- ✅ Faster global CDN
- ✅ Zero configuration (no workflow needed)
- ✅ Automatic preview deployments
- ✅ Custom domains without extra DNS setup
- ✅ No workflow permission issues

The README already references `https://eldercareguardian.pages.dev` — if that's your actual Cloudflare deployment, you're already set and can **delete** the `.github/workflows/pages.yml` file entirely.

---

## Next Steps

1. **Choose one:**
   - **Option A:** Fix GitHub Pages (follow Step 1-3 above)
   - **Option B:** Use Cloudflare Pages (delete `pages.yml`, deploy to Cloudflare)

2. **Verify the URL works:**
   - Open the privacy policy URL in a browser
   - Check for HTTPS certificate (should be valid)

3. **Update all references:**
   - Play Store listing
   - Android app strings (if any)
   - Documentation

---

## Related Files

- `.github/workflows/pages.yml` — GitHub Pages workflow (fixed)
- `apps/tos/index.html` — Terms of Service content
- `apps/privacy-policy/index.html` — Privacy Policy content
- `docs/play-store-submission-guide.md` — Uses privacy policy URL
