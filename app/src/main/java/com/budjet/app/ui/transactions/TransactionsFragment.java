package com.budjet.app.ui.transactions;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.budjet.app.R;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.databinding.FragmentTransactionsBinding;
import com.budjet.app.ui.adapter.TransactionAdapter;
import com.budjet.app.ui.dialog.AddEditTransactionBottomSheet;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private MainViewModel viewModel;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupRecyclerView();
        setupSearchAndFilters();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(new TransactionAdapter.OnTransactionClickListener() {
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

        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTransactions.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                binding.btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                viewModel.setSearchQuery(query);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearch.setText(""));

        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chip_all) {
                viewModel.setFilterType("ALL");
            } else if (checkedId == R.id.chip_expense) {
                viewModel.setFilterType(Transaction.TYPE_EXPENSE);
            } else if (checkedId == R.id.chip_income) {
                viewModel.setFilterType(Transaction.TYPE_INCOME);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getFilteredTransactions().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions == null || transactions.isEmpty()) {
                binding.rvTransactions.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
            } else {
                binding.rvTransactions.setVisibility(View.VISIBLE);
                binding.layoutEmptyState.setVisibility(View.GONE);
                adapter.submitList(transactions);
            }
        });
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
