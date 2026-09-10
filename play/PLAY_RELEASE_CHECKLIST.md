# Quick Customer Play Release Checklist

## One-time setup

1. Create the application in Google Play Console with package ID `com.quickcustomer.billing`.
2. Confirm that Google Play accepts the package ID before distributing this build publicly.
3. Enroll the app in Play App Signing.
4. Create a dedicated upload keystore and store at least two secure backups outside GitHub.
5. Add the upload-key secrets to the GitHub repository if the signed workflow will be used.

## Required GitHub Actions secrets

- `QC_UPLOAD_KEYSTORE_BASE64`
- `QC_STORE_PASSWORD`
- `QC_KEY_ALIAS`
- `QC_KEY_PASSWORD`

Generate the base64 value in Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\secure\quick-customer-upload.jks")) | Set-Clipboard
```

## Store configuration

- Upload the signed `app-release.aab`, never the APK.
- Use the descriptions in `play/STORE_LISTING.md`.
- Upload the 512 × 512 icon, 1024 × 500 feature graphic, and clean phone/tablet screenshots.
- Set the privacy-policy URL.
- Complete Data Safety using `play/DATA_SAFETY.md`.
- Declare that the app contains no ads.
- Complete the content-rating questionnaire for a business billing application.
- Select an adult/business target audience; the app is not designed for children.
- Paste the instructions from `play/REVIEWER_ACCESS.md` into App access.

## Testing and production

1. Upload to Internal testing.
2. Install from the tester Play Store link on a phone and tablet.
3. Verify owner setup, login, billing, reports, expenses, inventory, local-data deletion, orientation changes, and app restart.
4. Test Bluetooth printing on physical hardware.
5. Complete any closed-testing requirement shown by the Play Console account.
6. Fix every pre-launch report error before production submission.

## Update rules

- Keep package ID `com.quickcustomer.billing` unchanged after the first Play upload.
- Increase `versionCode` for every upload.
- Keep the upload keystore backed up and never commit it.
- Update the privacy policy and Data Safety form whenever data handling changes.
