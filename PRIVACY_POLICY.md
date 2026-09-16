# Quick Customer Privacy Policy

Effective date: September 15, 2026

Quick Customer is an offline-first billing, expense, and inventory application for Android phones and tablets.

## Information stored by the app

The app can store the following information locally on the Android device and, when Google Drive synchronization is enabled, in the connected store Google account's private application-data folder:

- Owner, administrator, and employee usernames, display names, roles, password hashes, and account status
- Shop name, address, phone number, receipt preferences, tax settings, and printer settings
- Products, prices, categories, bills, payment methods, discounts, expenses, suppliers, inventory, and audit records
- A UPI QR image selected by the owner
- Bluetooth printer name and address when receipt printing is enabled

Passwords are stored as salted PBKDF2 hashes. The app does not store plain-text passwords.

## Google account and Drive synchronization

The owner may connect a dedicated Google account. Google handles sign-in; Quick Customer does not receive or store the Gmail password. The app receives a short-lived authorization token and reads the selected account email so it can verify that the correct store account is connected.

When synchronization is enabled, the app uploads a store snapshot containing users, products, categories, bills, expenses, inventory, settings, and audit records to Google Drive's private `appDataFolder`. Other devices authorized with the same account can download that snapshot. The Google account email, store identifier, device identifier, device mode, and last synchronization time are retained locally. The authorization token is held only while the app is running.

Quick Customer does not operate a separate synchronization server. It does not sell personal information, display third-party advertising, use analytics SDKs, or perform cross-app tracking. Google processes Drive data under the store owner's Google account and Google's terms and privacy policy.

Android system services may process device backup data according to the device owner's Android and Google backup settings. Selecting the public privacy-policy link opens the device browser.

## Permissions

- **Nearby devices/Bluetooth:** Used only when the owner enables and connects a paired Bluetooth receipt printer.
- **Internet and network state:** Used for owner-authorized Google account and Drive synchronization and for opening the public privacy policy.
- **Google Drive application data:** Used only after the owner authorizes the dedicated store account. The app requests the narrow `drive.appdata` scope rather than general access to Drive files.
- **Microphone:** Used only after the Super User enables voice billing and a cashier taps the microphone button. Android's selected speech-recognition service may process the audio under that provider's terms. Quick Customer uses the returned text to prepare a product-and-quantity confirmation and does not store audio recordings.

The app can be used without Bluetooth printing or voice billing. Denying Bluetooth or microphone permission does not prevent touch billing.

## Data retention and deletion

Local records remain on the device until the device owner removes them. A Super User can select **Settings → Privacy and local data → Erase all local data** to permanently remove all app users, bills, products, expenses, inventory, settings, and locally saved references. Uninstalling the app also removes its local data, subject to the Android device's backup and restore settings.

Erasing local data does not automatically delete the private store snapshot from Google Drive. To remove cloud data, the owner must remove Quick Customer's stored application data/access from the connected Google account. Other linked devices may retain their own downloaded local copy until it is erased or the app is uninstalled.

## Children

Quick Customer is a business billing application and is not designed for children.

## Changes

This policy will be updated before any release that adds a Quick Customer server, analytics, advertising, or a new category of data processing.

## Contact

For privacy or support questions, contact **chakravarthi.ananthu@gmail.com**.
