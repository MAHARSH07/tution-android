# Tution implementation plan

## Data rules

Room (on-device SQLite) is the source of truth. Payments and fee changes are append-only records. When a student leaves, archive them (`isActive = false`); never delete their payment or fee history. Store money in paise (`Long`), never floating-point values. A month is `yyyy-MM`; remaining fee is expected fee minus payments against that month, so partial fees work automatically.

## Screens

1. **Students** — active list, search, add/edit/archive (not delete).
2. **Student detail** — profile, payment ledger, remaining balance, fee-change timeline, record payment.
3. **Dues** — monthly expected/paid/balance; red for overdue, amber for partial, green for paid.
4. **History** — active and archived students plus payments and fee changes searchable by date.
5. **Settings** — reminders, templates, backup/restore, export CSV, permissions.

## Coding map

| Area | Location |
| --- | --- |
| Database entities and DAO | `app/src/main/java/com/tution/app/data/TutionDatabase.kt` |
| Current UI/navigation | `app/src/main/java/com/tution/app/MainActivity.kt` |
| Future reminder worker | `app/src/main/java/com/tution/app/reminders/` |
| Future share/SMS/WhatsApp actions | `app/src/main/java/com/tution/app/messaging/` |
| Unit and device tests | `app/src/test/` and `app/src/androidTest/` |

## Build order

1. Open this folder in Android Studio and allow Gradle sync.
2. Implement Add/edit/archive student with validation.
3. Add payment recording (billing month, paid date, amount, method, note).
4. Calculate due summaries in a repository/ViewModel, then build Dues and History.
5. Implement fee-change records—never revise old transactions.
6. Use WorkManager for on-device daily due reminders.
7. Use a user-triggered Android share sheet or prefilled WhatsApp intent for sending reminders. Direct background SMS/WhatsApp messages require consent, platform permissions, and potentially a business API; do not silently send messages.
8. Add backup/export and test on a mid-range phone with 1,000 students and 20,000 payments.

## Performance rules

Keep database work off the main thread; use `LazyColumn`, stable IDs, and precomputed monthly summaries. Do not calculate all fee balances during every UI recomposition.
