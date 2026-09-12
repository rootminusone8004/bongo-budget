package com.budjet.app.ui.dialog;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.budjet.app.R;
import com.budjet.app.data.model.Category;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.databinding.BottomSheetCategoryTransactionsBinding;
import com.budjet.app.ui.adapter.TransactionAdapter;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DateUtils;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class CategoryTransactionsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CATEGORY = "arg_category";
    private static final String ARG_QUOTA_AMOUNT = "arg_quota_amount";

    private BottomSheetCategoryTransactionsBinding binding;
    private MainViewModel viewModel;
    private TransactionAdapter adapter;

    private String category = "";
    private double quotaAmount = 0.0;

    public static CategoryTransactionsBottomSheet newInstance(String category, double quotaAmount) {
        CategoryTransactionsBottomSheet fragment = new CategoryTransactionsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        args.putDouble(ARG_QUOTA_AMOUNT, quotaAmount);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCategoryTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY, "");
            quotaAmount = getArguments().getDouble(ARG_QUOTA_AMOUNT, 0.0);
        }

        setupUI();
        setupRecyclerView();
        observeTransactions();
    }

    private void setupUI() {
        binding.tvCategoryName.setText(category);

        int iconRes = Category.getIconResource(category);
        int colorRes = Category.getColorResource(category);
        int resolvedColor = ContextCompat.getColor(requireContext(), colorRes);

        binding.ivCategoryIcon.setImageResource(iconRes);
        binding.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(resolvedColor));
        binding.iconContainer.getBackground().mutate().setTint(
                (resolvedColor & 0x00FFFFFF) | 0x22000000
        );

        applyExpenseText();

        binding.btnCloseSheet.setOnClickListener(v -> dismiss());

        binding.btnAddExpenseForCategory.setOnClickListener(v -> {
            AddEditTransactionBottomSheet.newInstance(null, category)
                    .show(getParentFragmentManager(), "add_tx_for_cat");
        });
    }

    private void applyExpenseText() {
        binding.btnAddExpenseForCategory.setText("+ Add " + category);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction) {
                AddEditTransactionBottomSheet.newInstance(transaction)
                        .show(getParentFragmentManager(), "edit_tx");
            }

            @Override
            public void onTransactionEdit(Transaction transaction) {
                AddEditTransactionBottomSheet.newInstance(transaction)
                        .show(getParentFragmentManager(), "edit_tx");
            }

            @Override
            public void onTransactionDelete(Transaction transaction) {
                confirmDeleteTransaction(transaction);
            }
        });

        binding.rvCategoryTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategoryTransactions.setAdapter(adapter);
    }

    private void observeTransactions() {
        viewModel.getFilterCriteria().observe(getViewLifecycleOwner(), criteria -> {
            updateHeaderSubtitle(criteria != null ? criteria.monthYearDisplay : "", adapter.getItemCount());
        });

        viewModel.getTransactionsForCategory(category).observe(getViewLifecycleOwner(), transactions -> {
            adapter.submitList(transactions);

            int count = transactions != null ? transactions.size() : 0;
            String monthDisplay = viewModel.getFilterCriteria().getValue() != null ?
                    viewModel.getFilterCriteria().getValue().monthYearDisplay : "";
            updateHeaderSubtitle(monthDisplay, count);

            if (count == 0) {
                binding.rvCategoryTransactions.setVisibility(View.GONE);
                binding.layoutEmptyCategoryTransactions.setVisibility(View.VISIBLE);
                binding.tvEmptyMessage.setText("No expenses recorded in \"" + category + "\" for this month.");
            } else {
                binding.rvCategoryTransactions.setVisibility(View.VISIBLE);
                binding.layoutEmptyCategoryTransactions.setVisibility(View.GONE);
            }

            // Calculate spent in this category
            double totalSpent = 0.0;
            if (transactions != null) {
                for (Transaction t : transactions) {
                    if (t.isExpense()) {
                        totalSpent += t.getAmount();
                    }
                }
            }

            updateQuotaCard(totalSpent);
        });
    }

    private void updateHeaderSubtitle(String monthDisplay, int count) {
        String countText = count + " transaction" + (count == 1 ? "" : "s");
        if (monthDisplay != null && !monthDisplay.isEmpty()) {
            binding.tvCategorySubtitle.setText(monthDisplay + " • " + countText);
        } else {
            binding.tvCategorySubtitle.setText(countText);
        }
    }

    private void updateQuotaCard(double totalSpent) {
        if (quotaAmount > 0) {
            binding.cardQuotaSummary.setVisibility(View.VISIBLE);

            int percentage = (int) Math.round((totalSpent / quotaAmount) * 100);
            binding.progressQuota.setProgress(Math.min(percentage, 100));
            binding.tvQuotaSpentLimit.setText("Spent: " + CurrencyUtils.formatAmount(totalSpent) + " of " + CurrencyUtils.formatAmount(quotaAmount) + " (" + percentage + "%)");

            int progressColor;
            if (totalSpent > quotaAmount) {
                double over = totalSpent - quotaAmount;
                binding.tvQuotaRemaining.setText("Over by " + CurrencyUtils.formatAmount(over) + "!");
                binding.tvQuotaRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                progressColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
            } else {
                double remaining = quotaAmount - totalSpent;
                binding.tvQuotaRemaining.setText(CurrencyUtils.formatAmount(remaining) + " left");
                if (percentage >= 80) {
                    binding.tvQuotaRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_amber));
                    progressColor = ContextCompat.getColor(requireContext(), R.color.warning_amber);
                } else {
                    binding.tvQuotaRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
                    progressColor = ContextCompat.getColor(requireContext(), R.color.income_green);
                }
            }
            binding.progressQuota.setIndicatorColor(progressColor);
        } else {
            binding.cardQuotaSummary.setVisibility(View.GONE);
        }
    }

    private void confirmDeleteTransaction(Transaction transaction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage("Delete \"" + transaction.getTitle() + "\" (" + CurrencyUtils.formatAmount(transaction.getAmount()) + ")?")
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteTransaction(transaction))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
