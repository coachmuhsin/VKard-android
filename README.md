# VKARD PRO Android Application

## GitHub Actions Build

### How to Add Signing Secrets
Add the following secrets in your repository under **Settings > Secrets and variables > Actions**:
- `SUPABASE_URL`: Production Supabase Endpoint URL.
- `SUPABASE_KEY`: Production Supabase API Key.
- `KEYSTORE_BASE64`: Base64 encoded `.keystore` file.
- `KEYSTORE_PASSWORD`: Keystore access password.
- `KEY_ALIAS`: Private key alias name.
- `KEY_PASSWORD`: Private key password.

### How to Trigger Builds
- **Push**: Automatically builds whenever changes are pushed to the `main` branch.
- **Manual**: Go to **Actions > Android Build**, click **Run workflow**, choose a branch, and click the **Run workflow** button.

### Where APK is Downloaded
- **Latest Release**: The signed production-ready APK is permanently hosted at:
  `https://github.com/coachmuhsin/VKard-android/releases/latest/download/VKARD-PRO.apk`
- **Actions Artifacts**: Built binaries are also available directly on the workflow run summary page under `debug-apk`, `release-apk`, and `release-aab`.

### How Releases Work
- Tagging uses the format `v1.0.{github.run_number}`.
- Every successful build replaces assets on the matching release to ensure files remain updated.
- If keystore secrets are not set, signing is skipped, only the Debug APK is packaged, and the release assets are updated with the fallback Debug APK.
