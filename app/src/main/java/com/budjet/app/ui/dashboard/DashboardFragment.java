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
import com.budjet.app.data.model.CategorySpending;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.databinding.FragmentDashboardBinding;
import com.budjet.app.ui.adapter.CategorySpendingAdapter;
import com.budjet.app.ui.dialog.CategoryTransactionsBottomSheet;
import com.budjet.app.ui.dialog.SetBudgetDialog;
import com.budjet.app.util.BudgetAllocationCalculator;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DailyBudgetCalculator;
import com.budjet.app.viewmodel.MainViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private MainViewModel viewModel;

    private CategorySpendingAdapter categorySpendingAdapter;

    private double currentIncome = 0.0;
    private double currentExpense = 0.0;
    private Budget currentOverallBudget = null;
    private List<Budget> currentMonthlyBudgets = new ArrayList<>();
    private boolean isBudgetDailyMode = false;
    private boolean isCategoryDailyMode = true; // Daily remaining by default
    private Map<String, Double> todaySpentMap = new HashMap<>();
    private List<CategorySpending> currentCategorySpendingList = new ArrayList<>();
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

        binding.btnToggleCategorySpending.setOnClickListener(v -> {
            isCategoryDailyMode = !isCategoryDailyMode;
            binding.btnToggleCategorySpending.setText(isCategoryDailyMode ? "Daily" : "Total");
            updateCategorySpending();
        });
    }

    private void observeViewModel() {
        viewModel.getFilterCriteria().observe(getViewLifecycleOwner(), criteria -> {
            currentCalendar = viewModel.getCurrentCalendar();
            updateOverallBudget();
            updateBalance();
            updateCategorySpending();
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
            updateCategorySpending();
        });

        // Category Spending breakdown
        viewModel.getCategorySpending().observe(getViewLifecycleOwner(), list -> {
            currentCategorySpendingList = list != null ? list : new ArrayList<>();
            updateCategorySpending();
        });

        // Monthly transactions (to calculate today's spending per category reliably)
        viewModel.getMonthlyTransactions().observe(getViewLifecycleOwner(), transactions -> {
            calculateTodaySpentMap(transactions);
            updateCategorySpending();
        });

        // Overall Budget
        viewModel.getOverallBudget().observe(getViewLifecycleOwner(), budget -> {
            currentOverallBudget = budget;
            updateOverallBudget();
            updateBalance();
        });

        // Monthly Budgets (for allocation pool and category quotas)
        viewModel.getMonthlyBudgets().observe(getViewLifecycleOwner(), budgets -> {
            currentMonthlyBudgets = budgets != null ? budgets : new ArrayList<>();
            updateOverallBudget();
            updateBalance();
            updateCategorySpending();
        });
    }

    private void calculateTodaySpentMap(List<Transaction> transactions) {
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        long startOfToday = todayCal.getTimeInMillis();

        todayCal.set(Calendar.HOUR_OF_DAY, 23);
        todayCal.set(Calendar.MINUTE, 59);
        todayCal.set(Calendar.SECOND, 59);
        todayCal.set(Calendar.MILLISECOND, 999);
        long endOfToday = todayCal.getTimeInMillis();

        Map<String, Double> map = new HashMap<>();
        if (transactions != null) {
            for (Transaction t : transactions) {
                if (t != null && "EXPENSE".equalsIgnoreCase(t.getType())) {
                    long d = t.getDate();
                    if (d >= startOfToday && d <= endOfToday) {
                        String cat = t.getCategory();
                        map.put(cat, map.getOrDefault(cat, 0.0) + t.getAmount());
                    }
                }
            }
        }
        this.todaySpentMap = map;
    }

    private void updateCategorySpending() {
        if (currentCategorySpendingList == null || currentCategorySpendingList.isEmpty()) {
            binding.rvCategorySpending.setVisibility(View.GONE);
            binding.tvNoCategorySpending.setVisibility(View.VISIBLE);
            binding.btnToggleCategorySpending.setVisibility(View.GONE);
        } else {
            binding.rvCategorySpending.setVisibility(View.VISIBLE);
            binding.tvNoCategorySpending.setVisibility(View.GONE);
            binding.btnToggleCategorySpending.setVisibility(View.VISIBLE);
            binding.btnToggleCategorySpending.setText(isCategoryDailyMode ? "Daily" : "Total");
            categorySpendingAdapter.setData(
                    currentCategorySpendingList,
                    currentMonthlyBudgets,
                    todaySpentMap,
                    currentExpense,
                    currentCalendar,
                    isCategoryDailyMode
            );
        }
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
            binding.tvDashboardDailySpendable.setText(dailyInfo.getDailyFormattedAmount());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
