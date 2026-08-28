# Quick Customer — Android Billing App

This repository contains the runnable offline-first Quick Customer billing application.

## Included in version 0.4.0

- Adaptive Jetpack Compose interface for Android phones and tablets
- Android 8/API 26 through Android 16/API 36 support
- Landscape and portrait layouts
- Super User, Admin, and Employee access paths
- PBKDF2-hashed local passwords
- All 72 supplied products in 12 categories
- Offline Room/SQLite database
- Product add, edit, price change, enable, and disable controls
- Cart quantity controls and guided Cash/UPI/Card checkout
- Cash-received entry with automatic change calculation and receipt recording
- Owner-uploaded, replaceable UPI QR image shown during UPI payment
- Separate card-terminal payment confirmation
- Admin/Super User discounts with tax-inclusive or tax-exclusive calculation
- Super User shop, receipt, tax, and Bluetooth-printer settings
- Generic ESC/POS Bluetooth printing through already paired printers
- 58 mm and 80 mm paper formatting
- Test printing, optional automatic printing, and historical-bill reprinting
- Admin/Super User completed-bill cancellation with a required reason
- Persistent cancellation and settings audit records
- Transaction-safe sales and sale-item storage
- Price/name snapshots so historical bills never change
- Role-filtered sales history with Today, 7-day, 30-day, and all-time totals
- Tappable historical bills with full receipt, payment, cancellation, and sync details
- Custom Quick Customer launcher icon for phones and tablets
- Cash/UPI/Card, discount, tax, cancellation, and top-product summaries
- Pending-sync status and future multi-shop identifiers
- Versioned Room migrations that preserve existing users, settings, products, and bills
- Unit tests for permissions, billing calculations, totals, menu completeness, and receipt formatting

Cloud API synchronization is scheduled after the local billing and printer workflows are accepted. Bills are already marked with sync state, business ID, shop ID, and device ID so cloud sync does not require a database redesign.

## Open in Android Studio

1. Extract the project ZIP.
2. In Android Studio, select **Open**.
3. Choose the folder containing `app` and `settings.gradle.kts`.
4. Allow Gradle synchronization to finish.
5. Confirm **SDK Platform 36** and **JDK 17** are selected.
6. Start a phone/tablet emulator or connect the Samsung SM-P615.
7. Click **Run**.

The first build downloads Android and Kotlin dependencies and can take several minutes.

## First-run owner setup

A fresh installation asks the shop owner to create the first Super User username and password. No default credentials, password hashes, or password salts are published in this repository. The Super User can then create Admin and Employee accounts from the Users screen. Existing installations retain their current users and local bills.

## Recommended emulator matrix

- Compact phone: API 26
- Standard phone: API 36
- 8-inch tablet: API 33
- 10.4-inch 2000×1200 tablet: API 33 (SM-P615 profile)
- 12-inch tablet: API 36

Test both portrait and landscape orientation. Bluetooth printer validation requires a physical device and printer.

## Configure a generic Bluetooth printer

1. Pair the printer in the tablet or phone's Android Bluetooth settings.
2. Sign in as the Super User.
3. Open **Settings → Bluetooth receipt printer**.
4. Enable printing and allow the Nearby devices permission.
5. Select **58 mm** or **80 mm** paper.
6. Tap **Refresh** and select the paired printer.
7. Tap **Test print**.
8. Optionally enable **Print automatically**, then save the settings.

The implementation uses the common Bluetooth Classic ESC/POS Serial Port Profile. Hardware pairing and final paper alignment must be tested on the physical device; the Android emulator cannot validate a real Bluetooth printer.

## Useful commands

On Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Debug APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Product decisions still awaiting owner confirmation

- `Black Current` versus `Blackcurrant`
- Whether `Osmania Biscuits ₹5` is per piece
- Confirmation that `Samosa (2 pcs) ₹15` means two pieces for ₹15
- Whether menu prices already include applicable taxes
- Final spelling for Sonti/Sonthi and Sukku wording
