# VKARD PRO Android Application

This is the official native Android mobile application for VKARD PRO.

## GitHub Actions Build

The project is configured with a fully automated, production-ready CI/CD pipeline powered by GitHub Actions.

### 1. Setting up GitHub Secrets

To build, sign, and release the application, configure the following secrets in your GitHub repository (**Settings > Secrets and variables > Actions > Repository secrets**):

| Secret Name | Description | Required |
| --- | --- | --- |
| `SUPABASE_URL` | The production Supabase URL endpoint | Yes |
| `SUPABASE_KEY` | The production Supabase public API key | Yes |
| `KEYSTORE_BASE64` | Base64-encoded release `.keystore` file | Optional (Safe Fallback to Debug Build) |
| `KEYSTORE_PASSWORD` | Password for the keystore | Optional (Safe Fallback to Debug Build) |
| `KEY_ALIAS` | Key alias name | Optional (Safe Fallback to Debug Build) |
| `KEY_PASSWORD` | Password for the key | Optional (Safe Fallback to Debug Build) |

> [!NOTE]
> If the `KEYSTORE_BASE64` secret is missing or signing fails, the workflow completes without failure, falling back to packaging the Debug APK as the final release artifact.

### 2. How to Trigger Builds

The workflow runs automatically in two scenarios:
1. **Push to Main**: Automatically triggers on every commit pushed to the `main` or `master` branch.
2. **Manual Dispatch**: 
   - Go to the **Actions** tab on your GitHub repository.
   - Select **Android Production Build** from the left sidebar.
   - Click the **Run workflow** dropdown, choose the branch, and click **Run workflow**.

### 3. How Releases Work

Every build run automatically generates a GitHub Release matching the app's version configuration:
* **Tag Format**: `v{versionName}` (e.g. `v1.0.3`) read dynamically from Gradle configuration.
* **Title**: `VKARD PRO v{versionName}`.
* **Release Assets**:
  - `VKARD-PRO.apk` (Signed Release APK, or Debug APK fallback).
  - `VKARD-PRO.aab` (Signed Release AAB bundle, if signing secrets are provided).
  - `version.json` (Dynamic version tracking metadata containing versionCode, versionName, download URL, and release logs).
* **Release Notes**: Automatically generated from the last 5 commit logs.
* **Asset Replacement**: Rebuilds will overwrite existing releases and assets for the same version without creating duplicates.

### 4. How APK Download Links Work

* **Permanent Latest Link**: The build always publishes assets to a fixed address. The mobile application can permanently fetch the latest compiled APK at:
  ```
  https://github.com/coachmuhsin/VKard-android/releases/latest/download/VKARD-PRO.apk
  ```
* **Auto-Updates Check**: The compiled `version.json` file is attached to the latest release and contains:
  ```json
  {
    "versionCode": 1,
    "versionName": "1.0.0",
    "apk": "https://github.com/coachmuhsin/VKard-android/releases/latest/download/VKARD-PRO.apk",
    "changes": [
      "Commit message 1",
      "Commit message 2"
    ]
  }
  ```
  The mobile application reads this JSON file remotely to implement checks and alert users of updates.
