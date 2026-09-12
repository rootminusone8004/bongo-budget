package com.budjet.app.ui.dashboard;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.budjet.app.R;
import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.databinding.FragmentDashboardBinding;
import com.budjet.app.ui.MainActivity;
import com.budjet.app.ui.adapter.CategorySpendingAdapter;
import com.budjet.app.ui.adapter.TransactionAdapter;
import com.budjet.app.ui.dialog.AddEditTransactionBottomSheet;
import com.budjet.app.ui.dialog.CategoryTransactionsBottomSheet;
import com.budjet.app.ui.dialog.SetBudgetDialog;
import com.budjet.app.util.BudgetAllocationCalculator;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DailyBudgetCalculator;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private MainViewModel viewModel;

    private CategorySpendingAdapter categorySpendingAdapter;
    private TransactionAdapter recentTransactionsAdapter;

    private double currentIncome = 0.0;
    private double currentExpense = 0.0;
    private Budget currentOverallBudget = null;
    private List<Budget> currentMonthlyBudgets = new ArrayList<>();
    private boolean isBudgetDailyMode = false;
    private Calendar currentCalendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupRecyclerViews();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerViews() {
        // Category Spending
        categorySpendingAdapter = new CategorySpendingAdapter(category -> {
            double quota = 0.0;
            if (currentMonthlyBudgets != null) {
                for (Budget b : currentMonthlyBudgets) {
                    if (b != null && category.equalsIgnoreCase(b.getCategory())) {
                        quota = b.getAmount();
                        break;
                    }
                }
            }
            CategoryTransactionsBottomSheet.newInstance(category, quota)
                    .show(getChildFragmentManager(), "cat_transactions_dashboard");
        });
        binding.rvCategorySpending.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategorySpending.setAdapter(categorySpendingAdapter);

        // Recent Transactions
        recentTransactionsAdapter = new TransactionAdapter(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction) {
                AddEditTransactionBottomSheet.newInstance(transaction)
                        .show(getChildFragmentManager(), "edit_tx");
            }

            @Override
            public void onTransactionEdit(Transaction transaction) {
                AddEditTransactionBottomSheet.newInstance(transaction)
                        .show(getChildFragmentManager(), "edit_tx");
            }

            @Override
            public void onTransactionDelete(Transaction transaction) {
                confirmDeleteTransaction(transaction);
            }
        });
        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecentTransactions.setAdapter(recentTransactionsAdapter);
    }

    private void setupClickListeners() {
        binding.btnEditOverallBudget.setOnClickListener(v -> {
            SetBudgetDialog.newInstance(currentOverallBudget, Budget.CATEGORY_OVERALL)
                    .show(getChildFragmentManager(), "set_overall_budget");
        });

        binding.cardOverallBudget.setOnClickListener(v -> {
            if (currentOverallBudget == null) {
                SetBudgetDialog.newInstance(null, Budget.CATEGORY_OVERALL)
                        .show(getChildFragmentManager(), "set_overall_budget");
            }
        });

        binding.btnToggleDashboardBudget.setOnClickListener(v -> {
            isBudgetDailyMode = !isBudgetDailyMode;
            updateOverallBudget();
        });

        binding.btnViewAllTransactions.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTab(R.id.navigation_transactions);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getFilterCriteria().observe(getViewLifecycleOwner(), criteria -> {
            currentCalendar = viewModel.getCurrentCalendar();
            updateOverallBudget();
            updateBalance();
        });

        // Income
        viewModel.getMonthlyIncome().observe(getViewLifecycleOwner(), income -> {
            currentIncome = income != null ? income : 0.0;
            binding.tvTotalIncome.setText("+" + CurrencyUtils.formatAmount(currentIncome));
            updateBalance();
        });

        // Expense
        viewModel.getMonthlyExpense().observe(getViewLifecycleOwner(), expense -> {
            currentExpense = expense != null ? expense : 0.0;
            binding.tvTotalExpense.setText("-" + CurrencyUtils.formatAmount(currentExpense));
            updateBalance();
            updateOverallBudget();
        });

        // Category Spending breakdown
        viewModel.getCategorySpending().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                binding.rvCategorySpending.setVisibility(View.GONE);
                binding.tvNoCategorySpending.setVisibility(View.VISIBLE);
            } else {
                binding.rvCategorySpending.setVisibility(View.VISIBLE);
                binding.tvNoCategorySpending.setVisibility(View.GONE);
                categorySpendingAdapter.setData(list, currentExpense);
            }
        });

        // Overall Budget
        viewModel.getOverallBudget().observe(getViewLifecycleOwner(), budget -> {
            currentOverallBudget = budget;
            updateOverallBudget();
            updateBalance();
        });

        // Monthly Budgets (for allocation pool)
        viewModel.getMonthlyBudgets().observe(getViewLifecycleOwner(), budgets -> {
            currentMonthlyBudgets = budgets != null ? budgets : new ArrayList<>();
            updateOverallBudget();
            updateBalance();
        });

        // Recent Transactions
        viewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                binding.rvRecentTransactions.setVisibility(View.GONE);
                binding.tvNoRecentTransactions.setVisibility(View.VISIBLE);
            } else {
                binding.rvRecentTransactions.setVisibility(View.VISIBLE);
                binding.tvNoRecentTransactions.setVisibility(View.GONE);
                recentTransactionsAdapter.submitList(transactions);
            }
        });
    }

    private void updateBalance() {
        double budgetAmount = 0.0;
        if (currentOverallBudget != null && currentOverallBudget.getAmount() > 0) {
            budgetAmount = currentOverallBudget.getAmount();
        } else if (currentMonthlyBudgets != null && !currentMonthlyBudgets.isEmpty()) {
            budgetAmount = BudgetAllocationCalculator.calculateSummary(null, currentMonthlyBudgets).totalCategoryAllocated;
        }

        binding.tvTotalBudget.setText(CurrencyUtils.formatAmount(budgetAmount));

        double balance = BudgetAllocationCalculator.calculateDebitBalance(budgetAmount, currentIncome, currentExpense);
        binding.tvTotalBalance.setText(CurrencyUtils.formatAmount(balance));

        if (balance < 0) {
            binding.tvTotalBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        } else {
            binding.tvTotalBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
    }

    private void updateOverallBudget() {
        if (currentOverallBudget == null) {
            binding.layoutBudgetContent.setVisibility(View.GONE);
            binding.layoutBudgetDailyContent.setVisibility(View.GONE);
            binding.btnToggleDashboardBudget.setVisibility(View.GONE);
            binding.tvDashboardUnallocatedPool.setVisibility(View.GONE);
            binding.tvNoBudgetPrompt.setVisibility(View.VISIBLE);
            binding.btnEditOverallBudget.setText(R.string.set_budget);
        } else {
            binding.tvNoBudgetPrompt.setVisibility(View.GONE);
            binding.btnToggleDashboardBudget.setVisibility(View.VISIBLE);
            binding.btnEditOverallBudget.setText(R.string.edit_budget);

            double limit = currentOverallBudget.getAmount();
            int percentage = limit > 0 ? (int) Math.round((currentExpense / limit) * 100) : 0;
            binding.progressOverallBudget.setProgress(Math.min(percentage, 100));

            binding.tvOverallSpentLimit.setText("Spent: " + CurrencyUtils.formatAmount(currentExpense) + " of " + CurrencyUtils.formatAmount(limit));

            // Quotas allocation summary
            BudgetAllocationCalculator.AllocationSummary allocSummary =
                    BudgetAllocationCalculator.calculateSummary(currentOverallBudget, currentMonthlyBudgets);
            if (allocSummary.totalCategoryAllocated > 0) {
                binding.tvDashboardUnallocatedPool.setVisibility(View.VISIBLE);
                binding.tvDashboardUnallocatedPool.setText("Quotas: " + CurrencyUtils.formatAmount(allocSummary.totalCategoryAllocated) + " allocated • " + CurrencyUtils.formatAmount(allocSummary.unallocatedPool) + " unallocated pool");
            } else {
                binding.tvDashboardUnallocatedPool.setVisibility(View.GONE);
            }

            int progressColor;
            if (currentExpense > limit) {
                double over = currentExpense - limit;
                binding.tvOverallRemaining.setText("Over by " + CurrencyUtils.formatAmount(over) + "!");
                binding.tvOverallRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                progressColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
            } else {
                double remaining = limit - currentExpense;
                binding.tvOverallRemaining.setText(CurrencyUtils.formatAmount(remaining) + " left");
                if (percentage >= 80) {
                    binding.tvOverallRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_amber));
                    progressColor = ContextCompat.getColor(requireContext(), R.color.warning_amber);
                } else {
                    binding.tvOverallRemaining.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
                    progressColor = ContextCompat.getColor(requireContext(), R.color.income_green);
                }
            }
            binding.progressOverallBudget.setIndicatorColor(progressColor);

            // Daily calculation
            DailyBudgetCalculator.DailyBudgetInfo dailyInfo = DailyBudgetCalculator.calculate(
                    limit, currentExpense, currentCalendar
            );
            binding.tvDashboardDailySpendable.setText(dailyInfo.getDailyFormatted());
            binding.tvDashboardDailyDetails.setText(dailyInfo.getDailySubtext());

            if (dailyInfo.isOverBudget) {
                binding.tvDashboardDailySpendable.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                binding.ivDashboardDailyIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.expense_red)));
            } else {
                binding.tvDashboardDailySpendable.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                binding.ivDashboardDailyIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            }

            if (isBudgetDailyMode) {
                binding.layoutBudgetContent.setVisibility(View.GONE);
                binding.layoutBudgetDailyContent.setVisibility(View.VISIBLE);
                binding.btnToggleDashboardBudget.setText("Overall");
                binding.btnToggleDashboardBudget.setIconResource(R.drawable.ic_view_agenda);
            } else {
                binding.layoutBudgetContent.setVisibility(View.VISIBLE);
                binding.layoutBudgetDailyContent.setVisibility(View.GONE);
                binding.btnToggleDashboardBudget.setText("Daily");
                binding.btnToggleDashboardBudget.setIconResource(R.drawable.ic_swap_horiz);
            }
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
