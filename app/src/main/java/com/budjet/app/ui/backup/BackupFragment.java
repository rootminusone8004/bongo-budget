package com.budjet.app.ui.backup;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.budjet.app.R;
import com.budjet.app.data.repository.BudgetRepository;
import com.budjet.app.databinding.FragmentBackupBinding;
import com.budjet.app.util.DateUtils;
import com.budjet.app.viewmodel.MainViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupFragment extends Fragment {

    private FragmentBackupBinding binding;
    private MainViewModel viewModel;

    private ActivityResultLauncher<String> createDocumentLauncher;
    private ActivityResultLauncher<String[]> openDocumentLauncher;

    private Uri pendingImportUri = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SAF Export Launcher
        createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri != null) {
                        performExportToUri(uri);
                    }
                }
        );

        // SAF Import Launcher
        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        pendingImportUri = uri;
                        showImportModeDialog();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBackupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        setupClickListeners();
        observeViewModel();
    }

    private void setupClickListeners() {
        // Export to File
        binding.btnExportFile.setOnClickListener(v -> {
            String defaultFileName = "bongo_budget_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".json";
            createDocumentLauncher.launch(defaultFileName);
        });

        // Share JSON via Android ShareSheet
        binding.btnShareJson.setOnClickListener(v -> shareBackupJson());

        // Copy JSON to clipboard
        binding.btnCopyJson.setOnClickListener(v -> copyBackupJsonToClipboard());

        // Import from File
        binding.btnImportFile.setOnClickListener(v -> {
            openDocumentLauncher.launch(new String[]{"application/json", "text/plain", "*/*"});
        });

        // Sample Data
        binding.btnLoadSampleData.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.sample_data)
                    .setMessage("This will add realistic transactions and monthly budgets for this month. Continue?")
                    .setPositiveButton("Load", (dialog, which) -> viewModel.populateSampleData())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        // Reset Data
        binding.btnResetData.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_clear_all_title)
                    .setMessage(R.string.confirm_clear_all_msg)
                    .setPositiveButton("Reset Everything", (dialog, which) -> viewModel.clearAllData())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void observeViewModel() {
        viewModel.getImportSuccessEvent().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Import Complete")
                        .setMessage(getString(R.string.import_success, result.transactionCount, result.budgetCount))
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void performExportToUri(Uri uri) {
        try {
            OutputStream os = requireContext().getContentResolver().openOutputStream(uri);
            if (os != null) {
                viewModel.exportDataToStream(os);
            } else {
                Toast.makeText(requireContext(), "Failed to open output file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareBackupJson() {
        viewModel.exportDataToString(new BudgetRepository.RepositoryCallback<String>() {
            @Override
            public void onSuccess(String json) {
                try {
                    File exportDir = new File(requireContext().getCacheDir(), "exports");
                    if (!exportDir.exists()) exportDir.mkdirs();

                    File file = new File(exportDir, "budjet_backup.json");
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(json.getBytes(StandardCharsets.UTF_8));
                    }

                    Uri contentUri = FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            file
                    );

                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/json");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Bongo Budget Backup");
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Budget Backup"));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error sharing backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Failed to prepare backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void copyBackupJsonToClipboard() {
        viewModel.exportDataToString(new BudgetRepository.RepositoryCallback<String>() {
            @Override
            public void onSuccess(String json) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Bongo Budget Backup", json);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "Backup JSON copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Failed to generate JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImportModeDialog() {
        if (pendingImportUri == null) return;

        String[] options = new String[]{
                getString(R.string.import_merge),
                getString(R.string.import_overwrite)
        };

        final int[] selectedMode = {0}; // 0 = merge, 1 = overwrite

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_strategy_title)
                .setSingleChoiceItems(options, 0, (dialog, which) -> selectedMode[0] = which)
                .setPositiveButton("Restore", (dialog, which) -> {
                    boolean overwrite = (selectedMode[0] == 1);
                    performImport(pendingImportUri, overwrite);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performImport(Uri uri, boolean overwrite) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            if (is != null) {
                viewModel.importDataFromStream(is, overwrite);
            } else {
                Toast.makeText(requireContext(), "Could not open selected file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Import error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
