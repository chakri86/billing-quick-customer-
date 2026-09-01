# Google Play Data Safety Worksheet

Use this worksheet when completing **Policy and programs → App content → Data safety**. Recheck every answer against the final release and all future SDKs before submitting.

## Current version 0.7.0

- Third-party advertising SDKs: No
- Analytics SDKs: No
- Quick Customer cloud account: No
- Quick Customer server synchronization: No
- Sale, expense, inventory, user, settings, and QR information transmitted by the app: No
- Data sold: No
- Data shared with third parties by the app: No
- Optional Bluetooth printer data: Sent directly to the paired printer selected by the device owner
- Data deletion: Available under **Settings → Privacy and local data → Erase all local data**
- Privacy-policy access: Available inside Settings and through the public policy URL

## Local data categories

The app stores account identifiers, shop contact details, financial transaction records, product data, expenses, suppliers, inventory, and optional payment QR imagery locally. Local-only storage is not declared as collected when it is never transmitted off the device by the app, but the final Play Console answers must follow Google's definitions at submission time.

## Permissions declaration

- `BLUETOOTH` and `BLUETOOTH_ADMIN` are limited to Android 11/API 30 and older.
- `BLUETOOTH_CONNECT` supports optional paired-printer use on Android 12 and newer.
- Bluetooth hardware is marked optional.
- `INTERNET` and `ACCESS_NETWORK_STATE` are present for policy access and the planned synchronization foundation; version 0.7.0 has no server synchronization.

## Recheck before every release

Update this worksheet and the privacy policy before adding Firebase, crash reporting, analytics, advertising, cloud backup, remote authentication, online payments, or any other SDK that transmits data.
