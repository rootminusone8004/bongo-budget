# App Usage Techniques

## How to Start the App
  1. Locate the **Bongo Budget** icon on your Android home screen or app drawer.
  2. Tap the icon to open the app.

---

## Navigating the App

The app consists of four main navigation destinations accessible via the bottom navigation bar: **Dashboard**, **Transactions**, **Budgets**, and **Backup**.

### Dashboard Page
This is your personal financial overview. Here you can perform the following tasks:
  - **Net Balance**: View your current monthly balance (`Total Income - Total Expenses`) formatted in Bangladeshi Taka (`৳`).
  - **Overall Monthly Budget Progress**: Track your spending against your master monthly budget limit. A colored progress bar visually represents budget usage and turns red if spending exceeds limits.
  - **Spending by Category**: Inspect a breakdown of expenses across allocated categories with distinctive icons and colors.
  - **Recent Transactions**: Review your latest income and expense entries.
  - **Quick Add**: Tap the floating action button (`+`) in the bottom right corner to quickly log a new transaction.

### Transactions Page
This page contains the complete chronological ledger of your financial activities:
  - **Filter Transactions**: Switch between **All**, **Expense** (red indicator), and **Income** (green indicator) using the top chip filters.
  - **Search Transactions**: Use the search bar to filter transactions in real-time by title, note, or category name.
  - **Edit a Transaction**: Tap on any transaction card to open the edit dialog and update the title, amount, category, date, or note.
  - **Delete a Transaction**: Tap the delete icon on a transaction card and confirm to remove it.
  - **Add a Transaction**: Tap the floating action button (`+`). Note that for expenses, you must select an established category quota for the active month.

### Budgets Page
Here you manage your monthly spending discipline and category quotas:
  - **Set Master Overall Budget**:
    - Tap the edit icon on the **Total Monthly Budget** card to set your maximum monthly budget ceiling (e.g., `৳25,000`).
    - The **Available Pool** indicator automatically shows how much budget remains unallocated.
  - **Shrinking Pool Allocation**:
    - Adding or adjusting category quotas (e.g., Groceries `৳8,000`, Food `৳5,000`) automatically deducts from your available master pool in real-time.
    - The app prevents setting category quotas that exceed your available pool to avoid budget overruns.
  - **Daily Spendable Mode**:
    - Tap on any budget card or use the **Toggle All** button in the header bar to switch between **Monthly Total** and **Daily Spendable** mode.
    - In Daily Spendable mode, Bongo Budget calculates your exact daily spending allowance based on the remaining days of the month and your spending pace.
  - **Category Drill-down (Transaction Inspection)**:
    - Tap on any category card to open the Category Transactions bottom sheet.
    - This sheet displays all transactions logged under that specific category for the current month and includes a `+ Add [Category]` button for fast expense recording.
  - **Add a Category Quota**:
    - Tap the `+ Add Category Budget` button at the bottom.
    - Select an existing category or choose `+ Custom Category...` to create your own category.
    - Custom categories automatically receive smart icons and color palettes based on keyword matching (e.g., *Gym*, *Gaming*, *Health*, *Books*).
  - **Edit or Delete a Budget Quota**:
    - Tap the edit or delete icon on any category card to modify its monthly quota or remove the quota.

### Backup & Settings Page
Manage your offline data with complete privacy:
  - **Export to JSON File**: Tap **Export JSON (File)** to save a backup file to your device storage using Android's Storage Access Framework (SAF).
  - **Share JSON Backup**: Tap **Share JSON** to send your backup file via the Android ShareSheet to Google Drive, WhatsApp, Telegram, or email.
  - **Copy to Clipboard**: Tap **Copy to Clipboard** to copy the raw JSON backup data directly to your clipboard.
  - **Import JSON Backup**:
    - Tap **Import JSON (File)** and select a valid Bongo Budget `.json` file.
    - Choose **Merge** to append new records while keeping existing data.
    - Choose **Replace** to completely overwrite current data and restore the exact state from the backup file.
  - **Load Sample Data**: Tap **Load Sample Data** to populate the app with realistic Bangladeshi income, expense, and budget data for exploration.
  - **Reset All Data**: Tap **Reset All Data** and confirm in the alert dialog to safely wipe all transactions and budgets.

---

## Getting Help or Any Issue
  - If you encounter any issues, have feature requests, or need clarification, feel free to report them in the [issues](https://github.com/rootminusone8004/bongo-budget/issues) section.
