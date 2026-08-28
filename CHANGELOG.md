# Changelog

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
