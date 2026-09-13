package com.budjet.app.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.budjet.app.R;
import com.budjet.app.data.model.Budget;
import com.budjet.app.databinding.ActivityMainBinding;
import com.budjet.app.ui.backup.BackupFragment;
import com.budjet.app.ui.budgets.BudgetsFragment;
import com.budjet.app.ui.dashboard.DashboardFragment;
import com.budjet.app.ui.dialog.AddEditTransactionBottomSheet;
import com.budjet.app.ui.dialog.SetBudgetDialog;
import com.budjet.app.ui.transactions.TransactionsFragment;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_DASHBOARD = "dashboard";
    private static final String TAG_TRANSACTIONS = "transactions";
    private static final String TAG_BUDGETS = "budgets";
    private static final String TAG_BACKUP = "backup";
    private static final String[] ALL_TAGS = {TAG_DASHBOARD, TAG_TRANSACTIONS, TAG_BUDGETS, TAG_BACKUP};
    private static final String STATE_CURRENT_TAG = "state_current_tag";

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private String currentTag = TAG_DASHBOARD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupToolbar();
        setupBottomNavigation();
        setupFab();
        observeViewModel();

        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString(STATE_CURRENT_TAG, TAG_DASHBOARD);
        } else {
            currentTag = TAG_DASHBOARD;
        }

        int navId = getNavIdForTag(currentTag);
        if (binding.bottomNavigation.getSelectedItemId() != navId) {
            binding.bottomNavigation.setSelectedItemId(navId);
        } else {
            switchTab(currentTag);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_TAG, currentTag);
    }

    private void setupToolbar() {
        binding.topAppBar.setOnMenuItemClickListener(this::onToolbarMenuItemClick);

        binding.btnPrevMonth.setOnClickListener(v -> viewModel.previousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.nextMonth());
        binding.tvMonthYear.setOnClickListener(v -> {
            viewModel.setCurrentMonth();
            Toast.makeText(this, "Jumped to current month", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_dashboard) {
                switchTab(TAG_DASHBOARD);
                return true;
            } else if (itemId == R.id.navigation_transactions) {
                switchTab(TAG_TRANSACTIONS);
                return true;
            } else if (itemId == R.id.navigation_budgets) {
                switchTab(TAG_BUDGETS);
                return true;
            } else if (itemId == R.id.navigation_backup) {
                switchTab(TAG_BACKUP);
                return true;
            }
            return false;
        });
    }

    private void switchTab(String targetTag) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) {
            return;
        }

        FragmentTransaction ft = fm.beginTransaction();
        Fragment targetFragment = fm.findFragmentByTag(targetTag);

        // Hide all other added fragments to prevent overlapping views
        for (String tag : ALL_TAGS) {
            if (!tag.equals(targetTag)) {
                Fragment f = fm.findFragmentByTag(tag);
                if (f != null && f.isAdded() && !f.isHidden()) {
                    ft.hide(f);
                }
            }
        }

        // Show or add the target fragment
        if (targetFragment == null) {
            targetFragment = createFragmentForTag(targetTag);
            ft.add(R.id.nav_host_fragment, targetFragment, targetTag);
        } else if (!targetFragment.isAdded()) {
            ft.add(R.id.nav_host_fragment, targetFragment, targetTag);
        } else {
            ft.show(targetFragment);
        }

        ft.commitNowAllowingStateLoss();
        currentTag = targetTag;

        updateFabVisibility(targetTag);
    }

    private Fragment createFragmentForTag(String tag) {
        switch (tag) {
            case TAG_TRANSACTIONS:
                return new TransactionsFragment();
            case TAG_BUDGETS:
                return new BudgetsFragment();
            case TAG_BACKUP:
                return new BackupFragment();
            case TAG_DASHBOARD:
            default:
                return new DashboardFragment();
        }
    }

    private int getNavIdForTag(String tag) {
        switch (tag) {
            case TAG_TRANSACTIONS:
                return R.id.navigation_transactions;
            case TAG_BUDGETS:
                return R.id.navigation_budgets;
            case TAG_BACKUP:
                return R.id.navigation_backup;
            case TAG_DASHBOARD:
            default:
                return R.id.navigation_dashboard;
        }
    }

    private void updateFabVisibility(String tag) {
        if (TAG_DASHBOARD.equals(tag) || TAG_TRANSACTIONS.equals(tag)) {
            binding.fabAddTransaction.show();
        } else {
            binding.fabAddTransaction.hide();
        }
    }

    public void navigateToTab(int itemId) {
        binding.bottomNavigation.setSelectedItemId(itemId);
    }

    private void setupFab() {
        binding.fabAddTransaction.setOnClickListener(v -> {
            if (!viewModel.hasCreatedCategoriesInCurrentMonth()) {
                boolean hasOverall = viewModel.getOverallBudget().getValue() != null;
                String title = "No Category Quotas Set";
                String message = hasOverall
                        ? "No transactions can take place until a category is created. Please create a category budget quota first."
                        : "No transactions can take place until a category quota is created. First, set an Overall Budget pool, then allocate category quotas.";
                String positiveText = hasOverall ? "Create Category" : "Set Overall Budget";

                new MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(positiveText, (d, w) -> {
                            if (hasOverall) {
                                SetBudgetDialog.newInstance(null, null)
                                        .show(getSupportFragmentManager(), "set_budget");
                            } else {
                                SetBudgetDialog.newInstance(null, Budget.CATEGORY_OVERALL)
                                        .show(getSupportFragmentManager(), "set_budget");
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return;
            }
            AddEditTransactionBottomSheet.newInstance(null, null, viewModel.getSelectedDayTimestamp())
                    .show(getSupportFragmentManager(), "add_tx_dialog");
        });
    }

    private boolean onToolbarMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_export || id == R.id.action_import) {
            navigateToTab(R.id.navigation_backup);
            return true;
        } else if (id == R.id.action_sample_data) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.sample_data)
                    .setMessage("This will add realistic sample transactions and budgets for this month. Proceed?")
                    .setPositiveButton("Load", (d, w) -> viewModel.populateSampleData())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        } else if (id == R.id.action_clear_all) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirm_clear_all_title)
                    .setMessage(R.string.confirm_clear_all_msg)
                    .setPositiveButton("Reset Everything", (d, w) -> viewModel.clearAllData())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return false;
    }

    private void observeViewModel() {
        viewModel.getFilterCriteria().observe(this, criteria -> {
            if (criteria != null) {
                binding.tvMonthYear.setText(criteria.monthYearDisplay);
            }
        });

        viewModel.getMonthlyBudgets().observe(this, budgets -> {
            // Keep monthlyBudgets continuously active in memory for category quota validation
        });

        viewModel.getOverallBudget().observe(this, overall -> {
            // Keep overallBudget continuously active in memory
        });

        viewModel.getToastMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
