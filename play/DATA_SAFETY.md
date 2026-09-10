# Google Play Data Safety Worksheet

Use this worksheet when completing **Policy and programs → App content → Data safety**. Recheck every answer against the final release and all future SDKs before submitting.

## Current version 0.9.0

- Third-party advertising SDKs: No
- Analytics SDKs: No
- Quick Customer server account: No
- Owner-authorized Google Drive synchronization: Yes, optional
- Sale, expense, inventory, user, settings, and audit information transmitted by the app: Yes, to the connected store Google account's private Drive application-data folder
- Google account email transmitted/read: Yes, for selecting and verifying the linked store account
- Gmail password collected by Quick Customer: No
- Data sold: No
- Data shared with third parties by the app: No
- Optional Bluetooth printer data: Sent directly to the paired printer selected by the device owner
- Data deletion: Available under **Settings → Privacy and local data → Erase all local data**
- Privacy-policy access: Available inside Settings and through the public policy URL

## Data categories and purpose

The app stores account identifiers, shop contact details, financial transaction records, product data, expenses, suppliers, inventory, audit data, and optional payment QR imagery locally. When Drive sync is connected, the structured store snapshot is transmitted for backup, restore, and cross-device monitoring. The final Play Console declaration must classify each transmitted category under Google's definitions and disclose account management and app-functionality purposes.

The UPI QR selection currently uses an Android persisted content URI. It may not be usable on a restored device unless the image is separately available there; do not claim the QR image itself is synchronized until that support is implemented and verified.

## Permissions declaration

- `BLUETOOTH` and `BLUETOOTH_ADMIN` are limited to Android 11/API 30 and older.
- `BLUETOOTH_CONNECT` supports optional paired-printer use on Android 12 and newer.
- Bluetooth hardware is marked optional.
- `INTERNET` and `ACCESS_NETWORK_STATE` support Google account/Drive synchronization and public policy access.
- Google authorization requests `openid`, account email, and the narrow `drive.appdata` scope.

## Recheck before every release

Update this worksheet and the privacy policy before adding Firebase, crash reporting, analytics, advertising, a Quick Customer server, online payments, or any other SDK that transmits additional data.
