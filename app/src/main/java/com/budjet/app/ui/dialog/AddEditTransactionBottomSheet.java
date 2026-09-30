package com.budjet.app.ui.dialog;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.budjet.app.R;
import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.Category;
import com.budjet.app.data.model.CategorySpending;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.databinding.BottomSheetAddTransactionBinding;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DateUtils;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddEditTransactionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TRANSACTION = "arg_transaction";
    private static final String ARG_DEFAULT_CATEGORY = "arg_default_category";

    private BottomSheetAddTransactionBinding binding;
    private MainViewModel viewModel;
    private Transaction existingTransaction;
    private String defaultCategory;

    private final Calendar selectedDate = Calendar.getInstance();
    private String selectedType = Transaction.TYPE_EXPENSE;
    private List<String> createdExpenseCategories = new ArrayList<>();
    private List<Budget> currentBudgets = new ArrayList<>();
    private List<CategorySpending> currentCategorySpending = new ArrayList<>();

    public static final String ARG_DEFAULT_DATE = "arg_default_date";

    public static AddEditTransactionBottomSheet newInstance(@Nullable Transaction transaction) {
        return newInstance(transaction, null, 0);
    }

    public static AddEditTransactionBottomSheet newInstance(@Nullable Transaction transaction, @Nullable String defaultCategory) {
        return newInstance(transaction, defaultCategory, 0);
    }

    public static AddEditTransactionBottomSheet newInstance(@Nullable Transaction transaction, @Nullable String defaultCategory, long defaultDate) {
        AddEditTransactionBottomSheet fragment = new AddEditTransactionBottomSheet();
        Bundle args = new Bundle();
        if (transaction != null) {
            args.putSerializable(ARG_TRANSACTION, transaction);
        }
        if (defaultCategory != null) {
            args.putString(ARG_DEFAULT_CATEGORY, defaultCategory);
        }
        if (defaultDate > 0) {
            args.putLong(ARG_DEFAULT_DATE, defaultDate);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (getArguments() != null) {
            existingTransaction = (Transaction) getArguments().getSerializable(ARG_TRANSACTION);
            defaultCategory = getArguments().getString(ARG_DEFAULT_CATEGORY);
            long defDate = getArguments().getLong(ARG_DEFAULT_DATE, 0);
            if (defDate > 0 && existingTransaction == null) {
                selectedDate.setTimeInMillis(defDate);
            }
        }

        setupTypeToggle();
        setupDatePicker();

        viewModel.getCreatedExpenseCategoriesLive().observe(getViewLifecycleOwner(), categories -> {
            createdExpenseCategories = categories != null ? categories : new ArrayList<>();
            if (Transaction.TYPE_EXPENSE.equals(selectedType)) {
                setupCategories(selectedType);
            }
        });

        viewModel.getMonthlyBudgets().observe(getViewLifecycleOwner(), budgets -> {
            currentBudgets = budgets != null ? budgets : new ArrayList<>();
        });

        viewModel.getCategorySpending().observe(getViewLifecycleOwner(), spending -> {
            currentCategorySpending = spending != null ? spending : new ArrayList<>();
        });

        setupCategories(selectedType);

        if (existingTransaction != null) {
            binding.tvSheetTitle.setText(R.string.edit_transaction);
            populateExistingData();
        } else {
            binding.tvSheetTitle.setText(R.string.add_transaction);
            selectedDate.setTimeInMillis(System.currentTimeMillis());
        }
        binding.tilDate.setVisibility(View.GONE);

        binding.btnCancelTransaction.setOnClickListener(v -> dismiss());
        binding.btnSaveTransaction.setOnClickListener(v -> saveTransaction());
        binding.btnCreateCategoryFirst.setOnClickListener(v -> {
            dismiss();
            SetBudgetDialog.newInstance(null, null)
                    .show(getParentFragmentManager(), "set_budget");
        });
    }

    private void setupTypeToggle() {
        binding.toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_type_expense) {
                    selectedType = Transaction.TYPE_EXPENSE;
                } else {
                    selectedType = Transaction.TYPE_INCOME;
                }
                setupCategories(selectedType);
            }
        });
    }

    private void setupCategories(String type) {
        if (binding == null || getContext() == null) return;

        List<String> categories = new ArrayList<>();

        if (Transaction.TYPE_INCOME.equals(type)) {
            categories.addAll(Category.INCOME_CATEGORIES);
            binding.layoutNoCategoriesWarning.setVisibility(View.GONE);
            binding.tilCategory.setError(null);
            binding.btnSaveTransaction.setEnabled(true);
            binding.tilCategory.setEnabled(true);
        } else {
            // Expense type: strictly created categories only
            categories.addAll(createdExpenseCategories);

            // If editing an existing transaction, include its category if not present
            if (existingTransaction != null && !TextUtils.isEmpty(existingTransaction.getCategory())) {
                String existCat = existingTransaction.getCategory();
                if (!categories.contains(existCat) && !Budget.CATEGORY_OVERALL.equalsIgnoreCase(existCat)) {
                    categories.add(0, existCat);
                }
            }

            if (categories.isEmpty()) {
                binding.layoutNoCategoriesWarning.setVisibility(View.VISIBLE);
                binding.tilCategory.setError("No category quotas set for this month");
                binding.btnSaveTransaction.setEnabled(false);
                binding.tilCategory.setEnabled(false);
                binding.actCategory.setText("", false);
            } else {
                binding.layoutNoCategoriesWarning.setVisibility(View.GONE);
                binding.tilCategory.setError(null);
                binding.btnSaveTransaction.setEnabled(true);
                binding.tilCategory.setEnabled(true);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        binding.actCategory.setAdapter(adapter);

        String currentText = binding.actCategory.getText() != null ?
                binding.actCategory.getText().toString().trim() : "";

        if (!currentText.isEmpty() && categories.contains(currentText)) {
            binding.actCategory.setText(currentText, false);
        } else if (existingTransaction != null && categories.contains(existingTransaction.getCategory())) {
            binding.actCategory.setText(existingTransaction.getCategory(), false);
        } else if (defaultCategory != null && categories.contains(defaultCategory)) {
            binding.actCategory.setText(defaultCategory, false);
        } else if (!categories.isEmpty()) {
            binding.actCategory.setText(categories.get(0), false);
        } else {
            binding.actCategory.setText("", false);
        }
    }

    private void setupDatePicker() {
        binding.etDate.setOnClickListener(v -> showDatePicker());
        binding.tilDate.setEndIconOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    // Preserve current time of day (don't reset to midnight)
                    binding.tilDate.setError(null);
                    updateDateDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        // Prevent selecting future dates
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void updateDateDisplay() {
        binding.etDate.setText(DateUtils.formatDateTime(selectedDate.getTimeInMillis()));
    }

    private void populateExistingData() {
        double amt = existingTransaction.getAmount();
        String amtStr = (amt == Math.floor(amt) && !Double.isInfinite(amt)) ?
                String.valueOf((long) amt) : String.valueOf(amt);
        binding.etAmount.setText(amtStr);
        binding.etTitle.setText(existingTransaction.getTitle());
        selectedDate.setTimeInMillis(existingTransaction.getDate());
        updateDateDisplay();

        if (existingTransaction.getNote() != null) {
            binding.etNote.setText(existingTransaction.getNote());
        }

        if (existingTransaction.isIncome()) {
            binding.toggleType.check(R.id.btn_type_income);
            selectedType = Transaction.TYPE_INCOME;
        } else {
            binding.toggleType.check(R.id.btn_type_expense);
            selectedType = Transaction.TYPE_EXPENSE;
        }

        setupCategories(selectedType);
        binding.actCategory.setText(existingTransaction.getCategory(), false);
    }

    private void saveTransaction() {
        String amountStr = binding.etAmount.getText() != null ? binding.etAmount.getText().toString().trim() : "";
        String title = binding.etTitle.getText() != null ? binding.etTitle.getText().toString().trim() : "";
        String category = binding.actCategory.getText() != null ? binding.actCategory.getText().toString().trim() : "";
        String note = binding.etNote.getText() != null ? binding.etNote.getText().toString().trim() : "";

        if (TextUtils.isEmpty(amountStr)) {
            binding.tilAmount.setError("Please enter an amount");
            return;
        }
        binding.tilAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.tilAmount.setError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            binding.tilAmount.setError("Invalid amount");
            return;
        }

        if (TextUtils.isEmpty(title)) {
            title = category; // fallback title to category name
        }

        if (TextUtils.isEmpty(category)) {
            binding.tilCategory.setError("Please select a category");
            return;
        }

        if (Budget.CATEGORY_OVERALL.equalsIgnoreCase(category)) {
            binding.tilCategory.setError("OVERALL cannot be used as a category");
            return;
        }

        if (Transaction.TYPE_EXPENSE.equals(selectedType)) {
            boolean isAllowed = createdExpenseCategories.contains(category)
                    || (existingTransaction != null && category.equals(existingTransaction.getCategory()));
            if (createdExpenseCategories.isEmpty() || !isAllowed) {
                binding.tilCategory.setError("Expenses can only be recorded under created category quotas");
                Toast.makeText(requireContext(), "Please set a category budget quota first", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        binding.tilCategory.setError(null);

        long transactionTimestamp;
        if (existingTransaction != null) {
            // Editing: preserve original transaction date/time
            transactionTimestamp = existingTransaction.getDate();
        } else {
            // New transaction: current wall-clock time
            transactionTimestamp = System.currentTimeMillis();
        }

        // Safety check: reject future timestamps
        if (transactionTimestamp > System.currentTimeMillis()) {
            binding.tilDate.setError("Cannot create transactions in the future");
            return;
        }
        binding.tilDate.setError(null);

        if (Transaction.TYPE_EXPENSE.equals(selectedType)) {
            Budget targetBudget = null;
            if (currentBudgets != null) {
                for (Budget b : currentBudgets) {
                    if (b != null && !b.isOverall() && category.equalsIgnoreCase(b.getCategory())) {
                        targetBudget = b;
                        break;
                    }
                }
            }

            double currentCategorySpent = 0.0;
            if (currentCategorySpending != null) {
                for (CategorySpending cs : currentCategorySpending) {
                    if (cs != null && category.equalsIgnoreCase(cs.getCategory())) {
                        currentCategorySpent = cs.getTotalSpent();
                        break;
                    }
                }
            }

            if (existingTransaction != null && existingTransaction.isExpense()
                    && category.equalsIgnoreCase(existingTransaction.getCategory())) {
                currentCategorySpent = Math.max(0.0, currentCategorySpent - existingTransaction.getAmount());
            }

            double projectedSpent = currentCategorySpent + amount;

            if (targetBudget != null && targetBudget.getAmount() > 0 && projectedSpent > targetBudget.getAmount()) {
                double deficit = projectedSpent - targetBudget.getAmount();
                promptOverspendingDeduction(title, amount, category, note, transactionTimestamp, targetBudget, deficit);
                return;
            }
        }

        executeSaveTransaction(title, amount, category, note, transactionTimestamp);
    }

    private void promptOverspendingDeduction(
            String title,
            double amount,
            String category,
            String note,
            long transactionTimestamp,
            Budget targetBudget,
            double deficit
    ) {
        List<Budget> candidateBudgets = new ArrayList<>();
        List<String> candidateLabels = new ArrayList<>();

        if (currentBudgets != null) {
            for (Budget b : currentBudgets) {
                if (b != null && !b.isOverall() && !category.equalsIgnoreCase(b.getCategory()) && b.getAmount() >= deficit) {
                    candidateBudgets.add(b);
                }
            }
        }

        // Sort candidates: categories with available balance >= deficit first, sorted descending
        candidateBudgets.sort((b1, b2) -> {
            double spent1 = getSpentForCategory(b1.getCategory());
            double avail1 = b1.getAmount() - spent1;
            double spent2 = getSpentForCategory(b2.getCategory());
            double avail2 = b2.getAmount() - spent2;

            boolean b1HasEnough = avail1 >= deficit;
            boolean b2HasEnough = avail2 >= deficit;

            if (b1HasEnough && !b2HasEnough) return -1;
            if (!b1HasEnough && b2HasEnough) return 1;
            return Double.compare(avail2, avail1);
        });

        for (Budget b : candidateBudgets) {
            double spent = getSpentForCategory(b.getCategory());
            double avail = Math.max(0.0, b.getAmount() - spent);
            candidateLabels.add(b.getCategory() + " (Available: " + CurrencyUtils.formatAmount(avail) + " / Quota: " + CurrencyUtils.formatAmount(b.getAmount()) + ")");
        }

        if (candidateBudgets.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Overspending Detected")
                    .setMessage("This expense exceeds the budget quota for \"" + category + "\" by " + CurrencyUtils.formatAmount(deficit) + ".\n\nNo other category has sufficient quota (minimum " + CurrencyUtils.formatAmount(deficit) + ") to cover the overspent amount.")
                    .setPositiveButton("Save Anyway", (dialog, which) -> {
                        executeSaveTransaction(title, amount, category, note, transactionTimestamp);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        final int[] selectedIndex = {0};
        String[] labelsArray = candidateLabels.toArray(new String[0]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Overspending Detected")
                .setMessage("This expense exceeds the budget quota for \"" + category + "\" by " + CurrencyUtils.formatAmount(deficit) + ".\n\nSelect a category to deduct the overspent amount from:")
                .setSingleChoiceItems(labelsArray, 0, (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setPositiveButton("Deduct & Save", (dialog, which) -> {
                    Budget donor = candidateBudgets.get(selectedIndex[0]);
                    donor.setAmount(donor.getAmount() - deficit);
                    donor.setLastModified(System.currentTimeMillis());
                    targetBudget.setAmount(targetBudget.getAmount() + deficit);
                    targetBudget.setLastModified(System.currentTimeMillis());
                    viewModel.saveBudget(donor);
                    viewModel.saveBudget(targetBudget);
                    Toast.makeText(requireContext(),
                            "Deducted " + CurrencyUtils.formatAmount(deficit) + " from " + donor.getCategory(),
                            Toast.LENGTH_SHORT).show();
                    executeSaveTransaction(title, amount, category, note, transactionTimestamp);
                })
                .setNeutralButton("Save Anyway", (dialog, which) -> {
                    executeSaveTransaction(title, amount, category, note, transactionTimestamp);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private double getSpentForCategory(String category) {
        if (currentCategorySpending != null && category != null) {
            for (CategorySpending cs : currentCategorySpending) {
                if (cs != null && category.equalsIgnoreCase(cs.getCategory())) {
                    return cs.getTotalSpent();
                }
            }
        }
        return 0.0;
    }

    private void executeSaveTransaction(String title, double amount, String category, String note, long transactionTimestamp) {
        if (existingTransaction != null) {
            Transaction updatedTx = new Transaction(
                    existingTransaction.getId(),
                    title,
                    amount,
                    selectedType,
                    category,
                    transactionTimestamp,
                    note
            );
            viewModel.updateTransaction(updatedTx);
        } else {
            Transaction newTx = new Transaction(title, amount, selectedType, category, transactionTimestamp, note);
            viewModel.addTransaction(newTx);
        }

        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
