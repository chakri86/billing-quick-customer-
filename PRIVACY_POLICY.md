# Quick Customer Privacy Policy

Effective date: August 31, 2026

Quick Customer is an offline-first billing, expense, and inventory application for Android phones and tablets.

## Information stored by the app

The app can store the following information locally on the Android device:

- Owner, administrator, and employee usernames, display names, roles, password hashes, and account status
- Shop name, address, phone number, receipt preferences, tax settings, and printer settings
- Products, prices, categories, bills, payment methods, discounts, expenses, suppliers, inventory, and audit records
- A UPI QR image selected by the owner
- Bluetooth printer name and address when receipt printing is enabled

Passwords are stored as salted PBKDF2 hashes. The app does not store plain-text passwords.

## Collection, sharing, advertising, and tracking

The current release does not send shop or user data to a Quick Customer server. It does not sell or share personal information, display third-party advertising, use analytics SDKs, or perform cross-app tracking.

Android system services may process device backup data according to the device owner's Android and Google backup settings. Selecting the public privacy-policy link opens the device browser.

## Permissions

- **Nearby devices/Bluetooth:** Used only when the owner enables and connects a paired Bluetooth receipt printer.
- **Internet and network state:** Reserved for public policy access and future owner-controlled synchronization. The current release has no Quick Customer cloud synchronization.

The app can be used without Bluetooth printing. Denying Bluetooth permission does not prevent billing.

## Data retention and deletion

Local records remain on the device until the device owner removes them. A Super User can select **Settings → Privacy and local data → Erase all local data** to permanently remove all app users, bills, products, expenses, inventory, settings, and locally saved references. Uninstalling the app also removes its local data, subject to the Android device's backup and restore settings.

Because the current release has no Quick Customer server account, there is no separate server-side account or cloud record to delete.

## Children

Quick Customer is a business billing application and is not designed for children.

## Changes

This policy will be updated before any release that adds cloud synchronization, analytics, advertising, or a new category of data processing.

## Contact

For privacy or support questions, contact **chakravarthi.ananthu@gmail.com**.
