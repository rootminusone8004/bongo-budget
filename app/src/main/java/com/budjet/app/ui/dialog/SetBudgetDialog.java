package com.budjet.app.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.budjet.app.R;
import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.Category;
import com.budjet.app.databinding.DialogSetBudgetBinding;
import com.budjet.app.util.BudgetAllocationCalculator;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SetBudgetDialog extends DialogFragment {

    private static final String ARG_BUDGET = "arg_budget";
    private static final String ARG_DEFAULT_CATEGORY = "arg_default_category";

    private DialogSetBudgetBinding binding;
    private MainViewModel viewModel;
    private Budget existingBudget;
    private String defaultCategory;

    private Budget currentOverallBudget;
    private List<Budget> currentBudgetsList = new ArrayList<>();
    private List<String> allCreatedCategoriesList = new ArrayList<>();
    private boolean isOverallMode = false;

    public static SetBudgetDialog newInstance() {
        return newInstance(null, null);
    }

    public static SetBudgetDialog newInstance(@Nullable Budget budget, @Nullable String defaultCategory) {
        SetBudgetDialog fragment = new SetBudgetDialog();
        Bundle args = new Bundle();
        if (budget != null) {
            args.putSerializable(ARG_BUDGET, budget);
        }
        if (defaultCategory != null) {
            args.putString(ARG_DEFAULT_CATEGORY, defaultCategory);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogSetBudgetBinding.inflate(LayoutInflater.from(requireContext()));
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (getArguments() != null) {
            existingBudget = (Budget) getArguments().getSerializable(ARG_BUDGET);
            defaultCategory = getArguments().getString(ARG_DEFAULT_CATEGORY);
        }

        isOverallMode = (existingBudget != null && existingBudget.isOverall())
                || Budget.CATEGORY_OVERALL.equalsIgnoreCase(defaultCategory);

        if (isOverallMode) {
            // Overall Budget Mode: Completely hide category dropdown, OVERALL is not a category!
            binding.tvDialogTitle.setText(existingBudget != null ? "Edit Overall Budget" : "Set Overall Budget");
            binding.tilBudgetCategory.setVisibility(View.GONE);
            binding.tilCustomCategory.setVisibility(View.GONE);
        } else {
            // Category Budget Mode: Show presets and custom category option
            binding.tvDialogTitle.setText(existingBudget != null ? "Edit Category Budget" : "Set Category Budget");
            binding.tilBudgetCategory.setVisibility(View.VISIBLE);
        }

        if (existingBudget != null) {
            binding.etBudgetAmount.setText(String.valueOf(existingBudget.getAmount()));
            binding.btnDeleteBudget.setVisibility(View.VISIBLE);
            binding.btnDeleteBudget.setOnClickListener(v -> {
                viewModel.deleteBudget(existingBudget);
                dismiss();
            });
        }

        binding.btnCancelBudget.setOnClickListener(v -> dismiss());
        binding.btnSaveBudget.setOnClickListener(v -> saveBudget());

        binding.cardAllocationInfo.setOnClickListener(v -> {
            if (!isOverallMode && currentOverallBudget == null) {
                isOverallMode = true;
                binding.tvDialogTitle.setText("Set Overall Budget");
                binding.tilBudgetCategory.setVisibility(View.GONE);
                binding.tilCustomCategory.setVisibility(View.GONE);
                updateAllocationUI();
            }
        });

        // Amount input text watcher
        binding.etBudgetAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAllocationUI();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Custom category text watcher
        binding.etCustomCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAllocationUI();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Category dropdown listener
        binding.actBudgetCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selected = binding.actBudgetCategory.getText().toString();
            if (Category.CUSTOM_CATEGORY_OPTION.equals(selected)) {
                binding.tilCustomCategory.setVisibility(View.VISIBLE);
                binding.etCustomCategory.requestFocus();
            } else {
                binding.tilCustomCategory.setVisibility(View.GONE);
                binding.etCustomCategory.setText("");
            }
            updateAllocationUI();
        });

        // Observe ViewModel data
        viewModel.getOverallBudget().observe(this, budget -> {
            currentOverallBudget = budget;
            updateAllocationUI();
        });

        viewModel.getMonthlyBudgets().observe(this, budgets -> {
            currentBudgetsList = budgets != null ? budgets : new ArrayList<>();
            if (!isOverallMode) {
                setupCategoriesDropdown();
            }
            updateAllocationUI();
        });

        viewModel.getAllCreatedCategories().observe(this, categories -> {
            allCreatedCategoriesList = categories != null ? categories : new ArrayList<>();
            if (!isOverallMode) {
                setupCategoriesDropdown();
            }
        });

        if (!isOverallMode) {
            setupCategoriesDropdown();
        }

        updateAllocationUI();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(binding.getRoot());
        return builder.create();
    }

    private void setupCategoriesDropdown() {
        List<String> options = new ArrayList<>();

        // Set of categories that already have a budget in the current month
        Set<String> alreadyBudgetedCategories = new HashSet<>();
        if (currentBudgetsList != null) {
            for (Budget b : currentBudgetsList) {
                if (b != null && !b.isOverall() && b.getCategory() != null) {
                    // When editing an existing budget, do not exclude its own category
                    if (existingBudget != null && existingBudget.getId() == b.getId()) {
                        continue;
                    }
                    alreadyBudgetedCategories.add(b.getCategory().trim().toLowerCase(Locale.ROOT));
                }
            }
        }

        // Add preset candidate categories that are NOT already budgeted
        for (String preset : Category.EXPENSE_PRESET_CANDIDATES) {
            if (!alreadyBudgetedCategories.contains(preset.trim().toLowerCase(Locale.ROOT))
                    && !options.contains(preset)
                    && !Budget.CATEGORY_OVERALL.equalsIgnoreCase(preset)) {
                options.add(preset);
            }
        }

        // Add previously created custom categories that are NOT already budgeted
        if (allCreatedCategoriesList != null) {
            for (String cat : allCreatedCategoriesList) {
                if (!alreadyBudgetedCategories.contains(cat.trim().toLowerCase(Locale.ROOT))
                        && !options.contains(cat)
                        && !Budget.CATEGORY_OVERALL.equalsIgnoreCase(cat)) {
                    options.add(cat);
                }
            }
        }

        // If editing an existing category budget, make sure its category is included
        if (existingBudget != null && !existingBudget.isOverall()) {
            String existingCat = existingBudget.getCategory();
            if (!options.contains(existingCat)) {
                options.add(0, existingCat);
            }
        }

        // Add Custom Category option at the end
        options.add(Category.CUSTOM_CATEGORY_OPTION);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                options
        );
        binding.actBudgetCategory.setAdapter(adapter);

        if (existingBudget != null && !existingBudget.isOverall()) {
            binding.actBudgetCategory.setText(existingBudget.getCategory(), false);
            binding.tilCustomCategory.setVisibility(View.GONE);
        } else if (defaultCategory != null && !Budget.CATEGORY_OVERALL.equalsIgnoreCase(defaultCategory) && options.contains(defaultCategory)) {
            binding.actBudgetCategory.setText(defaultCategory, false);
            binding.tilCustomCategory.setVisibility(View.GONE);
        } else {
            String currentText = binding.actBudgetCategory.getText() != null ?
                    binding.actBudgetCategory.getText().toString().trim() : "";
            if (!currentText.isEmpty() && options.contains(currentText)) {
                binding.actBudgetCategory.setText(currentText, false);
                if (Category.CUSTOM_CATEGORY_OPTION.equals(currentText)) {
                    binding.tilCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    binding.tilCustomCategory.setVisibility(View.GONE);
                }
            } else if (!options.isEmpty()) {
                String first = options.get(0);
                binding.actBudgetCategory.setText(first, false);
                if (Category.CUSTOM_CATEGORY_OPTION.equals(first)) {
                    binding.tilCustomCategory.setVisibility(View.VISIBLE);
                } else {
                    binding.tilCustomCategory.setVisibility(View.GONE);
                }
            }
        }
    }

    private String getActiveCategory() {
        if (isOverallMode) {
            return Budget.CATEGORY_OVERALL;
        }

        String selected = binding.actBudgetCategory.getText() != null ?
                binding.actBudgetCategory.getText().toString().trim() : "";

        if (Category.CUSTOM_CATEGORY_OPTION.equalsIgnoreCase(selected)) {
            return binding.etCustomCategory.getText() != null ?
                    binding.etCustomCategory.getText().toString().trim() : "";
        }

        return selected;
    }

    private void updateAllocationUI() {
        if (binding == null) return;

        String category = getActiveCategory();
        String amountStr = binding.etBudgetAmount.getText() != null ?
                binding.etBudgetAmount.getText().toString().trim() : "";

        double enteredAmount = 0.0;
        boolean hasAmount = false;
        if (!TextUtils.isEmpty(amountStr)) {
            try {
                enteredAmount = Double.parseDouble(amountStr);
                hasAmount = true;
            } catch (NumberFormatException ignored) {
                hasAmount = false;
            }
        }

        if (isOverallMode) {
            // Overall Budget Mode
            binding.ivAllocationIcon.setImageResource(R.drawable.ic_wallet);
            binding.tvAllocationTitle.setText("Overall Budget Pool");

            BudgetAllocationCalculator.OverallValidationResult result =
                    BudgetAllocationCalculator.validateOverallBudget(currentBudgetsList, enteredAmount);

            if (result.minRequired > 0) {
                binding.tvAllocationOverview.setText("Allocated to Category Quotas: " + CurrencyUtils.formatAmount(result.minRequired));
                binding.tvAllocationAvailable.setText("Minimum required limit: " + CurrencyUtils.formatAmount(result.minRequired));
            } else {
                binding.tvAllocationOverview.setText("Setting this establishes your monthly spending limit.");
                binding.tvAllocationAvailable.setText("Quotas can be allocated from this pool.");
            }
            binding.tvAllocationAvailable.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));

            if (!hasAmount) {
                binding.tilBudgetAmount.setError(null);
                if (result.minRequired > 0) {
                    binding.tvPoolPreview.setText("Must be at least " + CurrencyUtils.formatAmount(result.minRequired) + " to cover category quotas");
                } else {
                    binding.tvPoolPreview.setText("Enter total monthly spending limit");
                }
                binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                binding.btnSaveBudget.setEnabled(false);
            } else if (enteredAmount <= 0) {
                binding.tilBudgetAmount.setError("Amount must be greater than 0");
                binding.tvPoolPreview.setText("Amount must be greater than 0");
                binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                binding.btnSaveBudget.setEnabled(false);
            } else if (!result.isValid) {
                binding.tilBudgetAmount.setError(result.errorMessage);
                double deficit = result.minRequired - enteredAmount;
                binding.tvPoolPreview.setText("Short by " + CurrencyUtils.formatAmount(deficit) + " to cover category quotas!");
                binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                binding.btnSaveBudget.setEnabled(false);
            } else {
                binding.tilBudgetAmount.setError(null);
                binding.tvPoolPreview.setText("Unallocated budget pool: " + CurrencyUtils.formatAmount(result.remainingUnallocatedAfter));
                binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
                binding.btnSaveBudget.setEnabled(true);
            }
        } else {
            // Category Quota Mode
            binding.ivAllocationIcon.setImageResource(R.drawable.ic_category);

            int currentId = existingBudget != null ? existingBudget.getId() : 0;
            BudgetAllocationCalculator.CategoryValidationResult result =
                    BudgetAllocationCalculator.validateCategoryBudget(
                            currentOverallBudget, currentBudgetsList, category, currentId, enteredAmount
                    );

            if (!result.hasOverallBudget) {
                binding.tvAllocationTitle.setText("Overall Budget Required");
                binding.tvAllocationOverview.setText("No Overall Budget set for this month.");
                binding.tvAllocationAvailable.setText("Set Overall Budget first before allocating quotas");
                binding.tvAllocationAvailable.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));

                binding.tilBudgetAmount.setError("Overall budget required first");
                binding.tvPoolPreview.setText("Please set the Overall Budget first.");
                binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                binding.btnSaveBudget.setEnabled(false);
            } else {
                binding.tvAllocationTitle.setText("Category Quota Allocation");
                binding.tvAllocationOverview.setText("Overall: " + CurrencyUtils.formatAmount(result.overallLimit) + " • Other Quotas: " + CurrencyUtils.formatAmount(result.otherCategoriesAllocated));
                binding.tvAllocationAvailable.setText("Max available for this quota: " + CurrencyUtils.formatAmount(result.maxAllowed));
                binding.tvAllocationAvailable.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));

                if (!hasAmount) {
                    binding.tilBudgetAmount.setError(null);
                    binding.tvPoolPreview.setText("Available unallocated pool: " + CurrencyUtils.formatAmount(result.maxAllowed));
                    binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
                    binding.btnSaveBudget.setEnabled(false);
                } else if (enteredAmount <= 0) {
                    binding.tilBudgetAmount.setError("Amount must be greater than 0");
                    binding.tvPoolPreview.setText("Amount must be greater than 0");
                    binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                    binding.btnSaveBudget.setEnabled(false);
                } else if (!result.isValid) {
                    binding.tilBudgetAmount.setError(result.errorMessage);
                    double over = enteredAmount - result.maxAllowed;
                    binding.tvPoolPreview.setText("Exceeds overall budget limit by " + CurrencyUtils.formatAmount(over) + "!");
                    binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                    binding.btnSaveBudget.setEnabled(false);
                } else {
                    binding.tilBudgetAmount.setError(null);
                    binding.tvPoolPreview.setText("Overall unallocated pool shrinks to: " + CurrencyUtils.formatAmount(result.remainingUnallocatedAfter));
                    binding.tvPoolPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
                    binding.btnSaveBudget.setEnabled(true);
                }
            }
        }
    }

    private void saveBudget() {
        String category = getActiveCategory();
        String amountStr = binding.etBudgetAmount.getText() != null ? binding.etBudgetAmount.getText().toString().trim() : "";

        if (!isOverallMode) {
            String selectedDropdown = binding.actBudgetCategory.getText() != null ?
                    binding.actBudgetCategory.getText().toString().trim() : "";

            if (Category.CUSTOM_CATEGORY_OPTION.equalsIgnoreCase(selectedDropdown)) {
                if (TextUtils.isEmpty(category)) {
                    binding.tilCustomCategory.setError("Please enter a custom category name");
                    return;
                }
                if (Budget.CATEGORY_OVERALL.equalsIgnoreCase(category)) {
                    binding.tilCustomCategory.setError("Category cannot be named OVERALL");
                    return;
                }
                if (currentBudgetsList != null) {
                    for (Budget b : currentBudgetsList) {
                        if (b != null && !b.isOverall() && category.equalsIgnoreCase(b.getCategory())) {
                            if (existingBudget == null || existingBudget.getId() != b.getId()) {
                                binding.tilCustomCategory.setError("A budget quota for \"" + category + "\" already exists");
                                return;
                            }
                        }
                    }
                }
                binding.tilCustomCategory.setError(null);
            } else {
                if (TextUtils.isEmpty(category)) {
                    binding.tilBudgetCategory.setError("Please select a category");
                    return;
                }
                if (Budget.CATEGORY_OVERALL.equalsIgnoreCase(category)) {
                    binding.tilBudgetCategory.setError("Cannot select OVERALL as category");
                    return;
                }
                binding.tilBudgetCategory.setError(null);
            }
        }

        if (TextUtils.isEmpty(amountStr)) {
            binding.tilBudgetAmount.setError("Please enter a budget amount");
            return;
        }
        binding.tilBudgetAmount.setError(null);

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                binding.tilBudgetAmount.setError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            binding.tilBudgetAmount.setError("Invalid amount");
            return;
        }

        // Strict overflow validation check
        if (isOverallMode) {
            BudgetAllocationCalculator.OverallValidationResult valResult =
                    BudgetAllocationCalculator.validateOverallBudget(currentBudgetsList, amount);
            if (!valResult.isValid) {
                binding.tilBudgetAmount.setError(valResult.errorMessage);
                return;
            }
        } else {
            int currentId = existingBudget != null ? existingBudget.getId() : 0;
            BudgetAllocationCalculator.CategoryValidationResult valResult =
                    BudgetAllocationCalculator.validateCategoryBudget(
                            currentOverallBudget, currentBudgetsList, category, currentId, amount
                    );
            if (!valResult.isValid) {
                binding.tilBudgetAmount.setError(valResult.errorMessage);
                return;
            }
        }

        String currentMonthKey = viewModel.getCurrentMonthYearKey();

        if (existingBudget != null) {
            existingBudget.setCategory(category);
            existingBudget.setAmount(amount);
            viewModel.saveBudget(existingBudget);
        } else {
            // Check if a budget for this category already exists in current month to avoid duplicates
            Budget duplicate = null;
            if (currentBudgetsList != null) {
                for (Budget b : currentBudgetsList) {
                    if (b != null && category.equalsIgnoreCase(b.getCategory())) {
                        duplicate = b;
                        break;
                    }
                }
            }

            if (duplicate != null) {
                duplicate.setAmount(amount);
                viewModel.saveBudget(duplicate);
            } else {
                Budget newBudget = new Budget(category, amount, currentMonthKey);
                viewModel.saveBudget(newBudget);
            }
        }

        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
