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
import com.budjet.app.data.model.Transaction;
import com.budjet.app.databinding.BottomSheetAddTransactionBinding;
import com.budjet.app.util.DateUtils;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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

    public static AddEditTransactionBottomSheet newInstance(@Nullable Transaction transaction) {
        return newInstance(transaction, null);
    }

    public static AddEditTransactionBottomSheet newInstance(@Nullable Transaction transaction, @Nullable String defaultCategory) {
        AddEditTransactionBottomSheet fragment = new AddEditTransactionBottomSheet();
        Bundle args = new Bundle();
        if (transaction != null) {
            args.putSerializable(ARG_TRANSACTION, transaction);
        }
        if (defaultCategory != null) {
            args.putString(ARG_DEFAULT_CATEGORY, defaultCategory);
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
        }

        setupTypeToggle();
        setupDatePicker();

        viewModel.getCreatedExpenseCategoriesLive().observe(getViewLifecycleOwner(), categories -> {
            createdExpenseCategories = categories != null ? categories : new ArrayList<>();
            if (Transaction.TYPE_EXPENSE.equals(selectedType)) {
                setupCategories(selectedType);
            }
        });

        setupCategories(selectedType);

        if (existingTransaction != null) {
            binding.tvSheetTitle.setText(R.string.edit_transaction);
            populateExistingData();
        } else {
            binding.tvSheetTitle.setText(R.string.add_transaction);
            updateDateDisplay();
        }

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

        if (existingTransaction != null && categories.contains(existingTransaction.getCategory())) {
            binding.actCategory.setText(existingTransaction.getCategory(), false);
        } else if (defaultCategory != null && categories.contains(defaultCategory)) {
            binding.actCategory.setText(defaultCategory, false);
        } else if (!currentText.isEmpty() && categories.contains(currentText)) {
            binding.actCategory.setText(currentText, false);
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
                    updateDateDisplay();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateDisplay() {
        binding.etDate.setText(DateUtils.formatDate(selectedDate.getTimeInMillis()));
    }

    private void populateExistingData() {
        binding.etAmount.setText(String.valueOf(existingTransaction.getAmount()));
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

        if (existingTransaction != null) {
            existingTransaction.setTitle(title);
            existingTransaction.setAmount(amount);
            existingTransaction.setType(selectedType);
            existingTransaction.setCategory(category);
            existingTransaction.setDate(selectedDate.getTimeInMillis());
            existingTransaction.setNote(note);
            viewModel.updateTransaction(existingTransaction);
        } else {
            Transaction newTx = new Transaction(title, amount, selectedType, category, selectedDate.getTimeInMillis(), note);
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
