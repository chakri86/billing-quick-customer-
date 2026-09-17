# Quick Customer — Android Billing App

This repository contains the runnable offline-first Quick Customer billing application.

## Included in version 0.9.5

Voice review now shows tappable choices for duplicate product names, including category and current price. Matches in the selected category appear first, but none is selected automatically. Every unclear item must be selected before Add to cart becomes available. Clear items and quantities are retained. Product-only orders still default to one; prefix/suffix quantities remain supported.

When no catalog name matches, up to five close spelling suggestions may appear and always require an explicit tap. This is catalog matching, not a new speech engine, and cannot fix every transcription. Quantity ambiguity still asks for a clearer order. No database migration or Google authorization changes are required.

Device checks for 0.9.5: say “Rose milk”, “two rose milk”, and “rose milk two”; verify all active duplicates show their current category/price, selected-category matches appear first, Add to cart stays disabled until selection, and the chosen product/quantity enters the cart only after confirmation. Test “coffee and rose milk”, repeated Rose Milk, cancel/retry, and a near spelling such as “rose mil”. Check long lists scroll on phone and tablet. Automated tests do not replace device microphone/UI testing.

- Adaptive Jetpack Compose interface for Android phones and tablets
- Android 8/API 26 through Android 16/API 36 support
- Landscape and portrait layouts
- Super User, Admin, and Employee access paths
- PBKDF2-hashed local passwords
- All 72 supplied products in 12 categories
- Offline Room/SQLite database
- Product add, edit, price change, temporary disable, and safe removal controls
- One-time Misc bill items with a required amount and optional description
- Super User category ordering for keeping high-volume categories at the top
- Temporary navigation drawer that leaves the full screen available for billing
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
- Cash/UPI/Card, discount, tax, cancellation, and period-aware top-product summaries
- Top 10, Top 20, Top 30, Top 50, and All product-ranking controls
- All-products default with category filtering and historical category snapshots
- Pending-sync status and future multi-shop identifiers
- Employee expense entry with Admin/Super User approval
- Approved-expense and sales-minus-expenses reports
- Inclusive custom From/To date ranges for Sales and Expenses reports
- Backdated expense entry with a separate, immutable audit-entry timestamp
- Hybrid inventory for packaged stock and recipe ingredients
- Low-stock warnings that never interrupt billing
- Purchase, adjustment, wastage, supplier-return, and stock-history workflows
- Automatic approved expenses for inventory purchases
- Recipe-based sale deductions and stock restoration when a bill is cancelled
- Weighted-average inventory cost and product-profit snapshots when costs are configured
- Versioned Room migrations that preserve existing users, settings, products, and bills
- Unit tests for permissions, billing calculations, totals, menu completeness, and receipt formatting
- Complete Quick Customer application identity using package ID `com.quickcustomer.billing`
- In-app privacy-policy access and permanent local-data deletion
- Play Store listing, Data Safety worksheet, reviewer instructions, release checklist, and signed-AAB workflow
- First-install connection to a dedicated store Gmail account
- Private Google Drive `appDataFolder` backup and whole-store synchronization
- Automatic new-store detection and first Super User setup after Drive connection
- Existing-store restore when the same store Gmail is connected on another device
- One primary billing device with additional read-only monitoring devices
- Manual refresh plus two-minute foreground refresh on monitoring devices
- Owner-controlled, tap-to-speak voice billing for product names and quantities
- Review-and-confirm dialog before recognized items are added to the cart
- No voice control for payments, discounts, cancellations, or administration

Version 0.9.3 adds optional voice-assisted item entry while retaining the version 0.9.0 Google Drive synchronization foundation. The primary device continues to save every operation locally first and uploads a store snapshot after changes. Monitoring devices download that snapshot and cannot create or modify business records.

## Configure voice billing

1. Sign in as the Super User.
2. Open **Settings → Voice billing**.
3. Enable the switch and save settings.
4. Return to Billing and tap **Voice billing**.
5. Allow microphone access when Android asks.
6. Say an order such as **“two tea two coffee.”**
7. Review the exact products and quantities, then tap **Add to cart**.

The microphone is hidden while the setting is disabled. Quick Customer does not store recordings; Android's selected speech-recognition service processes the audio. Touch billing remains available on devices without speech recognition.

## Open in Android Studio

1. Extract the project ZIP.
2. In Android Studio, select **Open**.
3. Choose the folder containing `app` and `settings.gradle.kts`.
4. Allow Gradle synchronization to finish.
5. Confirm **SDK Platform 36** and **JDK 17** are selected.
6. Start a phone/tablet emulator or connect the Samsung SM-P615.
7. Click **Run**.

The first build downloads Android and Kotlin dependencies and can take several minutes.

## First-run store and owner setup

A fresh installation first asks the owner to connect the dedicated store Gmail and authorize Quick Customer's private Drive application-data folder. If no existing store snapshot is found, the app asks the owner to create the first Super User. If a snapshot exists, the app restores it and asks for an existing Quick Customer user login. No default credentials, password hashes, or password salts are published in this repository.

Google authorization requires one manual Cloud Console setup before Drive testing. Follow [docs/GOOGLE_DRIVE_SETUP.md](docs/GOOGLE_DRIVE_SETUP.md). Device-only mode remains available when Drive is not required.

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
3. Open the top-left menu, then select **Settings → Bluetooth receipt printer**.
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

## Google Play release

Google Play uses the Android App Bundle:

```powershell
.\gradlew.bat bundleRelease
```

Unsigned bundle output when no upload key is configured:

```text
app\build\outputs\bundle\release\app-release.aab
```

For a signed bundle, create `keystore.properties` from `keystore.properties.example` and keep the keystore and passwords outside GitHub. The full process is documented in `play/PLAY_RELEASE_CHECKLIST.md`.

## Product decisions still awaiting owner confirmation

- `Black Current` versus `Blackcurrant`
- Whether `Osmania Biscuits ₹5` is per piece
- Confirmation that `Samosa (2 pcs) ₹15` means two pieces for ₹15
- Whether menu prices already include applicable taxes
- Final spelling for Sonti/Sonthi and Sukku wording

## Multilingual voice billing (0.9.3)

Enable Voice billing in Settings. Select English, Telugu or Hindi on that device; this does not change the shop or phone language. Say a product alone for one, or place its quantity before or after the name; review the exact matched items. Examples: `two tea two coffee`, `rendu tea oka coffee`, `రెండు టీ ఒక కాఫీ`, `do chai ek coffee`, `दो चाय एक कॉफी`, `two tea రెండు కాఫీ`.

The parser supports common quantity words 1–20 in these languages and numeric digits 1–99, including Telugu and Hindi digits. Common cafe aliases are built in; arbitrary translations of every custom product are not provided. Use the catalog name for other products. Generic tea/coffee still shows the first active category product for confirmation.

Automatic language switching is an optional Android 14+ request, off by default; it depends on the installed recognition service and downloaded English/Telugu/Hindi models. Older devices and unsupported services should use a selected primary language. Mixed speech accuracy needs physical-device testing. Quick Customer does not record audio.

Device validation: test each example with the corresponding language; test mixed mode if offered; verify the heard text and exact quantities; cancel and confirm separately; restart and verify the language preference; disable voice and verify the microphone disappears. Test in actual shop noise.

Android API reference: https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_ENABLE_LANGUAGE_SWITCH

## Voice entry for products (0.9.3)

Enable Settings → Voice input (previously Voice billing). In Products → Add or Edit product, use the microphone beside Product name, Category, or Price. Dictate one field at a time; choose Use value after reviewing the recognized text, then explicitly Save the completed form. Existing product-management permissions still apply. No product is saved by speech alone. Names retain the recognized language; exact existing category names are reused case-insensitively.

Prices use the existing whole-rupee form. Native digits and common English/Telugu/Hindi number words are accepted; unclear prices, decimals, negative amounts and overflow are rejected. Type unsupported number phrases manually. The same per-device language and optional automatic-switching preferences apply.

Device tests: create a product by dictating all three fields; cancel a proposed value and check the old field is retained; test price “twenty”, “ఇరవై”, “बीस” and “20”; check Save creates exactly one product; edit a product and confirm no changes are stored until Save; deny microphone permission; disable voice and verify microphones disappear; confirm employees without product-management permission cannot access product editing.

## Flexible voice orders (0.9.4)

`coffee` means one coffee; `two tea` and `tea two` both mean two teas. The same rule applies to supported Telugu/Hindi quantities, native digits and product names. Multiple products can be spoken together. Separate ambiguous phrases with “and”: `tea two and coffee one` is explicit, whereas `tea two coffee` could attach “two” to either product and asks you to retry. All words must be accounted for; unknown words prevent adding a partial order.

Targeted recognition corrections map `brew` to `BRU` and `dumpty` to `Dum Tea` when those active products match. Existing exact catalog names take precedence. Ambiguous partial names are rejected; generic category defaults such as tea/coffee retain their existing first-active-product behavior and always show the actual product for review. No arbitrary fuzzy substitution, product renaming, or changes to product-form dictation are performed.

Device validation: say coffee; two tea; tea two; tea two coffee three; two tea and coffee; brew tea; two brew tea; dumpty; dumpty two. Confirm the actual product and quantity shown. Also try an unknown product and an ambiguous partial name; neither should silently add items.
