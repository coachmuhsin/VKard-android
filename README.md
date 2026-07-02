# VKARD PRO Android Application

## GitHub Actions Build

### How to Build
The build pipeline triggers automatically on any commit push to the `main` branch. You can also trigger it manually:
1. Go to the **Actions** tab on your GitHub repository.
2. Select **Android Build** from the sidebar.
3. Click the **Run workflow** dropdown, choose your branch, and click **Run workflow**.

### How to Add Signing Secrets
Configure these repository secrets under **Settings > Secrets and variables > Actions > Repository secrets**:
- `SUPABASE_URL`: Production Supabase Endpoint URL.
- `SUPABASE_KEY`: Production Supabase API Key.
- `KEYSTORE_BASE64`: Base64 encoded string of your release `.keystore` file.
- `KEYSTORE_PASSWORD`: Access password for the keystore.
- `KEY_ALIAS`: Key alias name.
- `KEY_PASSWORD`: Access password for the key.

### How Releases Work
- Releases are automatically tagged as `v{run_number}`.
- Every run builds a clean `VKARD-PRO.apk` (and `VKARD-PRO.aab` if signing secrets exist) and attaches them as release assets.
- If keystore secrets are missing or fail, the build falls back to publishing an unsigned Debug APK as the release asset instead of failing.

### How APK Download Works
- **Permanent URL**: The latest compiled application can always be fetched using the direct link:
  `https://github.com/coachmuhsin/VKard-android/releases/latest/download/VKARD-PRO.apk`
- **Build Artifacts**: APKs and AAB bundles are also available as downloadable zip files from each individual workflow run under the names `debug-apk`, `release-apk`, and `release-aab`.
