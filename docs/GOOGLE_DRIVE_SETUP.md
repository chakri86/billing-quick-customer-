# Google Drive synchronization setup

Quick Customer v0.9.0 uses Google Identity Services for authorization and the Drive API's private `appDataFolder`. Google Cloud configuration is required before the Connect Gmail and Drive button can succeed.

## 1. Create and configure the Google Cloud project

1. Open Google Cloud Console and create or select the project for Quick Customer.
2. Enable **Google Drive API**.
3. Configure the OAuth consent/branding screen with the Quick Customer app name, support email, privacy-policy URL, and developer contact.
4. While the OAuth app is in testing, add each dedicated store Gmail under test users.
5. Create an **Android OAuth client** with package name `com.quickcustomer.billing`.
6. Add the SHA-1 fingerprint belonging to the key that signs the installed application.

No `google-services.json` file is required for this implementation because it uses Google Identity Services and the Drive REST API directly rather than Firebase.

## 2. Register the correct SHA-1

For an Android Studio debug build on Windows:

```powershell
.\gradlew.bat signingReport
```

Copy the SHA1 shown for the `debug` variant into the Android OAuth client. For Google Play releases, also create/register an Android OAuth client using the **Play App Signing certificate SHA-1** displayed in Play Console. The upload-key SHA-1 is not a substitute for the Play App Signing SHA-1 installed on customer devices.

An APK signed by a different key has a different SHA-1 and Google authorization will reject it. For reliable Drive testing, run the locally signed Android Studio debug build whose fingerprint you registered, or install through a Play internal-testing track whose Play App Signing fingerprint is registered.

The stable Quick Customer test/release APK produced by the project owner uses this upload-certificate SHA-1:

```text
64:3A:20:57:BF:81:60:EB:4A:E9:14:BB:7C:E1:1D:CA:95:A2:89:EA
```

Register that value with package `com.quickcustomer.billing` when testing the separately supplied signed APK. Do not use the fingerprint for a temporary CI debug APK, because its debug signing key is not stable.

## 3. First device

1. Install Quick Customer.
2. Tap **Connect Gmail and Drive** and select the dedicated store Gmail.
3. Approve access to Quick Customer's private Drive application-data area.
4. If no store data exists, create the first Super User.
5. The device becomes the primary billing device and uploads the first snapshot.

## 4. Additional device

1. Install the same app version.
2. Connect the same dedicated store Gmail.
3. The app restores the store snapshot.
4. Sign in using an existing Quick Customer username/password.
5. The device opens in read-only monitoring mode and can refresh Sales, Expenses, and Inventory.

Version 0.9.0 intentionally supports one primary writer. Additional devices cannot create or alter bills, products, expenses, inventory, users, or settings. This prevents Drive snapshot conflicts. A future server-backed release can support concurrent billing devices.

## Troubleshooting

- **Authorization failure:** Check package name, signing SHA-1, OAuth testing users, and that Drive API is enabled.
- **Wrong Gmail:** Select the exact store account shown by the app.
- **Stale monitor data:** Tap the sync chip. The access token may require renewed authorization.
- **No network:** The primary device continues local billing and uploads again after the owner reconnects Drive; monitoring devices show their most recently downloaded snapshot.
