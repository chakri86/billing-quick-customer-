# Quick Customer — Android Billing App

This repository contains the runnable offline-first Quick Customer billing application.

## Included in version 0.2.0

- Adaptive Jetpack Compose interface for Android phones and tablets
- Android 8/API 26 through Android 16/API 36 support
- Landscape and portrait layouts
- Super User, Admin, and Employee access paths
- PBKDF2-hashed local passwords
- All 72 supplied products in 12 categories
- Offline Room/SQLite database
- Product add, edit, price change, enable, and disable controls
- Cart quantity controls and Cash/UPI/Card checkout
- Admin/Super User discounts with tax-inclusive or tax-exclusive calculation
- Super User shop, receipt, tax, and printer-preference settings
- Admin/Super User completed-bill cancellation with a required reason
- Persistent cancellation and settings audit records
- Transaction-safe sales and sale-item storage
- Price/name snapshots so historical bills never change
- Role-filtered sales history with Today, 7-day, 30-day, and all-time totals
- Cash/UPI/Card, discount, tax, cancellation, and top-product summaries
- Pending-sync status and future multi-shop identifiers
- Versioned Room migration from the 0.1 database
- Unit tests for permissions, billing calculations, totals, and menu completeness

Cloud API synchronization and real ESC/POS Bluetooth printing are intentionally scheduled after the local billing core is accepted. The printer switch in Settings stores the owner's preference but does not connect to hardware yet. Bills are already marked with sync state, business ID, shop ID, and device ID so cloud sync does not require a database redesign.

## Open in Android Studio

1. Extract the project ZIP.
2. In Android Studio, select **Open**.
3. Choose the `chai-duniya-billing` folder.
4. Allow Gradle synchronization to finish.
5. Confirm **SDK Platform 36** and **JDK 17** are selected.
6. Start a phone/tablet emulator or connect the Samsung SM-P615.
7. Click **Run**.

The first build downloads Android and Kotlin dependencies and can take several minutes.

## Demo accounts

| Role | Username | Password |
|---|---|---|
| Super User | `owner` | `Owner@123` |
| Admin | `admin` | `Admin@123` |
| Employee | `cashier` | `Cashier@123` |

These credentials are test data only and must be replaced before a production release.

## Recommended emulator matrix

- Compact phone: API 26
- Standard phone: API 36
- 8-inch tablet: API 33
- 10.4-inch 2000×1200 tablet: API 33 (SM-P615 profile)
- 12-inch tablet: API 36

Test both portrait and landscape orientation. Bluetooth printer validation requires a physical device and printer.

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
