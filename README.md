# VKard PRO Android Application

This is the official native Android mobile application for VKARD PRO.

## GitHub Actions Build

The project is configured to build automatically on push to the `main` branch or manual invocation via GitHub Actions.

### Setting up GitHub Secrets

To successfully build and sign the application, the following secrets must be added to your GitHub repository (**Settings > Secrets and variables > Actions > Repository secrets**):

| Secret Name | Description | Required |
| --- | --- | --- |
| `SUPABASE_URL` | The production Supabase URL endpoint | Yes |
| `SUPABASE_KEY` | The production Supabase public key | Yes |
| `RELEASE_KEYSTORE` | Base64-encoded release `.keystore` file | Optional (for signing) |
| `RELEASE_STORE_PASSWORD` | Password for the keystore | Optional (for signing) |
| `RELEASE_KEY_ALIAS` | Key alias name | Optional (for signing) |
| `RELEASE_KEY_PASSWORD` | Password for the key | Optional (for signing) |

### How to Run Manually

1. Go to the **Actions** tab on your GitHub repository.
2. Select **Android Build** from the left sidebar.
3. Click the **Run workflow** dropdown menu.
4. Select the target branch and click **Run workflow**.

### Download Artifacts

Once the build is complete, you can download the generated assets from the summary section of the run:
- **Debug APK**: Located under the `debug-apk` artifact.
- **Release APK**: Located under the `release-apk` artifact (signed if credentials are set).
- **Release AAB**: Located under the `release-aab` bundle artifact (for Play Store publishing).
