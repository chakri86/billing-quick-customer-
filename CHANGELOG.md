# Changelog

## 0.5.0 — Faster billing and catalog controls

- Added a movable Misc category for one-time bill items with a required price and optional description.
- Added safe product removal that hides obsolete products without changing historical bills or reports.
- Renamed Quick Customer Special Shakes to Special Shakes for existing and new installations.
- Added owner-controlled category ordering for keeping high-volume categories at the top.
- Replaced the permanent navigation rail and bottom bar with a temporary menu drawer to provide more billing space.
- Added a Room 4-to-5 migration that preserves existing users, products, bills, settings, and printer configuration.

## 0.4.0 — Generic Bluetooth ESC/POS printing

- Added paired Bluetooth-printer selection with Android 12+ Nearby devices permission handling.
- Added generic RFCOMM/SPP ESC/POS connection support with secure and insecure socket fallbacks.
- Added separate 58 mm and 80 mm receipt layouts.
- Added a printer test page and optional automatic printing after payment.
- Added manual printing from the successful-payment receipt.
- Added reprinting from complete historical bill details, including a cancelled-copy marker.
- Added a Room 3-to-4 migration that preserves existing local data and printer preferences.
- Added receipt-formatter unit tests and retained the approved Quick Customer launcher icon.

## 0.3.2 — Quick Customer launcher icon

- Replaced the generic Android launcher icon with the approved Quick Customer Q-and-receipt logo.
- Added standard and round launcher assets across Android screen densities.
- Added adaptive launcher icons for Android 8 and newer.
- Added secure first-run Super User creation with no built-in credentials or password-derived values in source.

## 0.3.1 — Historical bill details

- Made every sales-history bill tappable.
- Added a complete historical bill dialog with item snapshots, quantities, unit prices, totals, payment information, cash/change, cashier, cancellation, and synchronization status.
- Kept employee access restricted to bills created by that employee.

## 0.3.0 — Guided payment flow

- Replaced direct payment confirmation with a method-specific Proceed step.
- Added cash-received entry, insufficient-cash protection, and automatic change calculation.
- Added an owner-managed UPI QR image that appears during UPI checkout.
- Added a separate card-terminal confirmation step.
- Stored cash received and returned change with each completed cash bill and receipt.
- Added a Room 2-to-3 migration that preserves existing bills and shop settings.

## 0.2.2 — Startup crash fix

- Added the Kotlin standard library as an explicit runtime dependency.
- Fixed the startup crash reporting `NoClassDefFoundError: kotlin.jvm.functions.Function0` in standalone APK installs.

## 0.2.1 — Quick Customer branding

- Renamed the visible application brand from Chai Duniya to Quick Customer.
- Updated the launcher label, login screen, default shop name, and branded menu entries.
- Added a safe one-time update for existing local installations while preserving bills and settings.

## 0.2.0 — Local shop workflow

- Added editable shop and receipt settings for the Super User.
- Added configurable tax behavior and Admin/Super User discounts.
- Added authorized completed-bill cancellation with mandatory reasons and audit records.
- Added Today, 7-day, 30-day, and all-time sales views.
- Added payment, discount, tax, cancellation, and top-product summaries.
- Added a Room migration that preserves existing version 0.1 data.
- Added billing-calculation test coverage and stronger role checks.

## 0.1.0 — Initial Android MVP

- Added adaptive phone and tablet billing UI.
- Added Super User, Admin, and Employee access paths.
- Added the complete 72-product starter catalog.
- Added offline Room database, cart, checkout, receipts, and sales history.
- Added owner product and user administration screens.
- Added future-ready business, shop, device, and synchronization fields.
- Added unit tests for access policy, money calculations, and catalog completeness.
