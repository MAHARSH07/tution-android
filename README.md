# Tution

Tution is an offline-first Android app for managing a tuition centre. It keeps student details, monthly fees, payment dates, partial payments, fee changes, due dates, and former-student records in one place.

The app is designed for a tuition owner using one Android phone. Data is stored locally on that device, so the core workflows remain fast and usable without an internet connection.

## Main features

- Add students with a monthly fee, phone number, and monthly due day.
- View active students from the Students tab.
- Record payments by billing month, amount, payment date, and method (`Cash` or `UPI`).
- Support partial payment and clearly show the remaining balance.
- Use **Mark fully paid** to enter the exact balance after a confirmation.
- Prevent accidental payment amounts above the fee due for the selected month.
- Undo the latest payment after confirmation.
- Change a student's fee and retain a fee-change record instead of overwriting history.
- Change a monthly due date with validation for days 1–31.
- Archive a student rather than deleting them, preserving their records.
- Restore an archived student with **Rejoin student**.
- Review active and archived students in History.
- Browse monthly statements from the Dues tab using Previous/Next month controls.
- See expected fees, money received, individual payments, outstanding balances, and extra/advance payments.

## How monthly fees and history work

Tution treats payments as a ledger. Each payment records:

- the student;
- the **billing month** the payment belongs to, for example `2026-08`;
- the actual date received;
- the exact amount in paise;
- the payment method; and
- an optional note field in the data model.

Money is stored as paise in the database (`Long`), not decimal floating-point values. This avoids rounding mistakes when adding partial payments.

Fee changes have an effective month. New fee changes default to the next month so that reducing a fee after someone has already paid does not incorrectly alter that month's payment record. The Dues tab uses the applicable fee for the month being viewed.

When someone leaves, their payments remain in History. Their money is kept separate from current-student totals in the monthly statement. If they return, **Rejoin student** restores them while preserving all earlier records.

## Screens

| Screen | Purpose |
| --- | --- |
| **Students** | Current student list, quick monthly fee view, and add-student action. |
| **Student details** | Record or undo payments, mark a fee as fully paid, update fee/due date, archive or rejoin, and inspect payment and fee-change history. |
| **Dues** | Choose a month and review expected fees, received money, balances, extra payments, and every payment recorded that month. |
| **History** | Find both current and archived students. |
| **Settings** | Space for reminder, backup/export, and message-template settings. |

## Project structure

```text
Tution/
├── app/
│   ├── build.gradle.kts                  # Android module configuration
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/tution/app/
│       │   ├── MainActivity.kt           # Compose theme, screens, dialogs, and navigation
│       │   └── data/TutionDatabase.kt    # Room entities, DAO queries, and database migrations
│       └── res/                          # Android resources and launcher assets
├── docs/PRODUCT_PLAN.md                  # Product and implementation notes
├── gradle/                               # Gradle wrapper and version catalog
├── build.gradle.kts                      # Root Gradle configuration
├── settings.gradle.kts                   # Included Android modules
└── gradlew / gradlew.bat                 # Gradle wrapper scripts
```

## Data model

The local Room/SQLite database is defined in `app/src/main/java/com/tution/app/data/TutionDatabase.kt`.

| Entity | What it stores |
| --- | --- |
| `StudentEntity` | Student profile, joined/left date, active status, current configured fee, and due day. |
| `PaymentEntity` | Each individual fee payment and the billing month it applies to. |
| `FeeAdjustmentEntity` | Fee increase/decrease, old amount, new amount, and effective month. |
| `StudentStatusEvent` | Archive and rejoin events used to keep historical monthly statements accurate. |

`MainActivity.kt` contains the app's Jetpack Compose UI and business calculations for monthly balances. As the app grows, screen code can be moved into separate `ui/`, `viewmodel/`, and `repository/` packages without changing the database design.

## Requirements

- Windows, macOS, or Linux computer capable of running Android Studio.
- Android Studio with the Android SDK installed.
- Android SDK Platform 35 (Android 15) and matching Build Tools.
- Gradle JDK set to Java 17 in Android Studio.
- An Android emulator or a physical Android device.

For the Android Emulator on Windows, enable CPU virtualization and **Windows Hypervisor Platform** if Android Studio requests it.

## Run the app in Android Studio

1. Install Android Studio.
2. Select **Open** and choose the `Tution` folder containing `settings.gradle.kts`.
3. Wait for Gradle sync to finish and install any requested SDK components.
4. Open **Tools → Device Manager** and create/start an emulator, or connect a physical phone with USB debugging enabled.
5. Select the device from Android Studio's device menu.
6. Click the green **Run** button, or press `Shift + F10`.

The first build can take longer because Gradle downloads dependencies.

## Build from the command line

On Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS/Linux:

```bash
./gradlew :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If Java is not found from the command line, configure Android Studio's **Gradle JDK** to its bundled JDK or install/configure Java 17.

## Development notes

- Database operations use Room `suspend` functions and `Flow`, keeping storage work off the main UI thread.
- Lists use `LazyColumn` and stable keys for responsive scrolling.
- Do not delete student records when a learner leaves; archive them.
- Do not edit historical payment amounts to correct a mistake; use the payment undo flow or record an appropriate correction according to your accounting practice.
- Before publishing a production app, add secure backup/export, a privacy notice, tests, and a release signing key.

## Git workflow

The repository ignores build outputs, local SDK settings, and Android Studio workspace files through `.gitignore`.

```bash
git status
git add .
git commit -m "Describe the change"
git push
```
