package com.budjet.app.ui.budgets;

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
import com.budjet.app.databinding.FragmentBudgetsBinding;
import com.budjet.app.ui.adapter.BudgetAdapter;
import com.budjet.app.ui.dialog.CategoryTransactionsBottomSheet;
import com.budjet.app.ui.dialog.SetBudgetDialog;
import com.budjet.app.util.BudgetAllocationCalculator;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DailyBudgetCalculator;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetsFragment extends Fragment {

    private FragmentBudgetsBinding binding;
    private MainViewModel viewModel;
    private BudgetAdapter adapter;

    private Budget currentOverallBudget = null;
    private double currentTotalExpense = 0.0;
    private final Map<String, Double> categorySpendingMap = new HashMap<>();
    private List<Budget> currentCategoryBudgets = new ArrayList<>();
    private Calendar currentCalendar = Calendar.getInstance();
    private boolean isOverallDailyMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new BudgetAdapter(new BudgetAdapter.OnBudgetActionListener() {
            @Override
            public void onEditBudget(Budget budget) {
                SetBudgetDialog.newInstance(budget, null)
                        .show(getChildFragmentManager(), "edit_cat_budget");
            }

            @Override
            public void onDeleteBudget(Budget budget) {
                confirmDeleteBudget(budget);
            }

            @Override
            public void onBudgetClick(Budget budget) {
                CategoryTransactionsBottomSheet.newInstance(budget)
                        .show(getChildFragmentManager(), "cat_transactions");
            }
        });

        binding.rvCategoryBudgets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategoryBudgets.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnEditOverall.setOnClickListener(v -> {
            SetBudgetDialog.newInstance(currentOverallBudget, Budget.CATEGORY_OVERALL)
                    .show(getChildFragmentManager(), "edit_overall");
        });

        binding.btnToggleOverallMode.setOnClickListener(v -> {
            isOverallDailyMode = !isOverallDailyMode;
            updateOverallBudgetUI();
        });

        binding.btnToggleAllQuotas.setOnClickListener(v -> {
            adapter.toggleAllModes();
            binding.btnToggleAllQuotas.setText(adapter.isAllDailyMode() ? "Show Overall" : "Toggle All");
        });

        binding.btnAddCategoryBudget.setOnClickListener(v -> {
            SetBudgetDialog.newInstance(null, null)
                    .show(getChildFragmentManager(), "add_cat_budget");
        });
    }

    private void observeViewModel() {
        // Filter criteria (to keep date in sync)
        viewModel.getFilterCriteria().observe(getViewLifecycleOwner(), criteria -> {
            currentCalendar = viewModel.getCurrentCalendar();
            updateOverallBudgetUI();
            updateCategoryBudgetsList();
        });

        // Overall Budget
        viewModel.getOverallBudget().observe(getViewLifecycleOwner(), budget -> {
            currentOverallBudget = budget;
            updateOverallBudgetUI();
        });

        // Total Expense
        viewModel.getMonthlyExpense().observe(getViewLifecycleOwner(), expense -> {
            currentTotalExpense = expense != null ? expense : 0.0;
            updateOverallBudgetUI();
        });

        // Category Spending
        viewModel.getCategorySpending().observe(getViewLifecycleOwner(), spendings -> {
            categorySpendingMap.clear();
            if (spendings != null) {
                for (CategorySpending cs : spendings) {
                    categorySpendingMap.put(cs.getCategory(), cs.getTotalSpent());
                }
            }
            updateCategoryBudgetsList();
        });

        // Category Budgets list
        viewModel.getMonthlyBudgets().observe(getViewLifecycleOwner(), budgets -> {
            currentCategoryBudgets = new ArrayList<>();
            if (budgets != null) {
                for (Budget b : budgets) {
                    if (!b.isOverall()) {
                        currentCategoryBudgets.add(b);
                    }
                }
            }
            updateCategoryBudgetsList();
            updateOverallBudgetUI();
        });
    }

    private void updateOverallBudgetUI() {
        if (currentOverallBudget == null) {
            binding.layoutOverallDetails.setVisibility(View.GONE);
            binding.layoutOverallDaily.setVisibility(View.GONE);
            binding.layoutAllocationPool.setVisibility(View.GONE);
            binding.btnToggleOverallMode.setVisibility(View.GONE);
            binding.tvNoOverallBudget.setVisibility(View.VISIBLE);
            binding.btnEditOverall.setText(R.string.set_budget);
        } else {
            binding.tvNoOverallBudget.setVisibility(View.GONE);
            binding.btnToggleOverallMode.setVisibility(View.VISIBLE);
            binding.layoutAllocationPool.setVisibility(View.VISIBLE);
            binding.btnEditOverall.setText(R.string.edit_budget);

            double limit = currentOverallBudget.getAmount();

            // Overall Progress and Spending
            int percentage = limit > 0 ? (int) Math.round((currentTotalExpense / limit) * 100) : 0;
            binding.progressOverall.setProgress(Math.min(percentage, 100));
            binding.tvOverallStats.setText("Spent: " + CurrencyUtils.formatAmount(currentTotalExpense) + " of " + CurrencyUtils.formatAmount(limit) + " (" + percentage + "%)");

            int progressColor;
            if (currentTotalExpense > limit) {
                double over = currentTotalExpense - limit;
                binding.tvOverallLeft.setText("Over by " + CurrencyUtils.formatAmount(over) + "!");
                binding.tvOverallLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                progressColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
            } else {
                double remaining = limit - currentTotalExpense;
                binding.tvOverallLeft.setText(CurrencyUtils.formatAmount(remaining) + " left");
                if (percentage >= 80) {
                    binding.tvOverallLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_amber));
                    progressColor = ContextCompat.getColor(requireContext(), R.color.warning_amber);
                } else {
                    binding.tvOverallLeft.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
                    progressColor = ContextCompat.getColor(requireContext(), R.color.income_green);
                }
            }
            binding.progressOverall.setIndicatorColor(progressColor);

            // Daily estimated spendable calculation
            DailyBudgetCalculator.DailyBudgetInfo dailyInfo = DailyBudgetCalculator.calculate(
                    limit, currentTotalExpense, currentCalendar
            );
            binding.tvOverallDailySpendable.setText(dailyInfo.getDailyFormatted());
            binding.tvOverallDailyDetails.setText(dailyInfo.getDailySubtext());
            binding.tvOverallDailyPlanned.setText(dailyInfo.getPlannedDailyText());

            if (dailyInfo.isOverBudget) {
                binding.tvOverallDailySpendable.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
                binding.ivOverallDailyIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.expense_red)));
            } else {
                binding.tvOverallDailySpendable.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
                binding.ivOverallDailyIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary)));
            }

            // Quota allocation and shrinking pool calculation
            BudgetAllocationCalculator.AllocationSummary allocSummary =
                    BudgetAllocationCalculator.calculateSummary(currentOverallBudget, currentCategoryBudgets);

            binding.progressAllocationPool.setProgress(allocSummary.allocationPercentage);
            binding.tvAllocationAllocatedLabel.setText("Allocated: " + CurrencyUtils.formatAmount(allocSummary.totalCategoryAllocated) + " (" + allocSummary.allocationPercentage + "%)");
            binding.tvAllocationRemainingLabel.setText("Unallocated: " + CurrencyUtils.formatAmount(allocSummary.unallocatedPool));

            if (allocSummary.unallocatedPool <= 0 && allocSummary.totalCategoryAllocated > 0) {
                binding.tvAllocationPoolStatus.setText("Fully Allocated");
                binding.tvAllocationPoolStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning_amber));
            } else {
                binding.tvAllocationPoolStatus.setText(CurrencyUtils.formatAmount(allocSummary.unallocatedPool) + " unallocated pool");
                binding.tvAllocationPoolStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
            }

            // Toggle view state
            if (isOverallDailyMode) {
                binding.layoutOverallDetails.setVisibility(View.GONE);
                binding.layoutOverallDaily.setVisibility(View.VISIBLE);
                binding.btnToggleOverallMode.setText("Overall");
                binding.btnToggleOverallMode.setIconResource(R.drawable.ic_view_agenda);
                binding.tvOverallModeHint.setText("Tap to show Overall Quota");
            } else {
                binding.layoutOverallDetails.setVisibility(View.VISIBLE);
                binding.layoutOverallDaily.setVisibility(View.GONE);
                binding.btnToggleOverallMode.setText("Daily");
                binding.btnToggleOverallMode.setIconResource(R.drawable.ic_swap_horiz);
                binding.tvOverallModeHint.setText("Tap to show Daily Budget");
            }
        }
    }

    private void updateCategoryBudgetsList() {
        if (currentCategoryBudgets.isEmpty()) {
            binding.rvCategoryBudgets.setVisibility(View.GONE);
            binding.layoutEmptyBudgets.setVisibility(View.VISIBLE);
            binding.btnToggleAllQuotas.setVisibility(View.GONE);
        } else {
            binding.rvCategoryBudgets.setVisibility(View.VISIBLE);
            binding.layoutEmptyBudgets.setVisibility(View.GONE);
            binding.btnToggleAllQuotas.setVisibility(View.VISIBLE);
            adapter.setBudgets(currentCategoryBudgets, categorySpendingMap, currentCalendar);
        }
    }

    private void confirmDeleteBudget(Budget budget) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage("Delete budget limit for \"" + budget.getCategory() + "\" (" + CurrencyUtils.formatAmount(budget.getAmount()) + ")?")
                .setPositiveButton(R.string.delete, (dialog, which) -> viewModel.deleteBudget(budget))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
