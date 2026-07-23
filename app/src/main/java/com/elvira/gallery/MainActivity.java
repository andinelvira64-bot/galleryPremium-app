package com.elvira.gallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSIONS = 100;
    private static final int GRID_SPAN = 3;
    private static final int FILTER_ALL = 0;
    private static final int FILTER_PHOTOS_ONLY = 1;
    private static final int FILTER_VIDEOS_ONLY = 2;
    /** How long the filter button lingers after scrolling stops, so there's
     *  time to tap it right after you finish scrolling to where you wanted. */
    private static final long FILTER_BUTTON_HIDE_DELAY_MS = 1500L;

    private RecyclerView recyclerView;
    private TextView emptyView;
    private ImageButton btnFilterType;
    private MediaAdapter adapter;
    /** What's currently shown in the grid (filtered view of allMediaItems). */
    private final List<MediaItem> mediaList = new ArrayList<>();
    /** Everything scanned, regardless of the active filter. */
    private final List<MediaItem> allMediaItems = new ArrayList<>();
    private final MediaScanner scanner = new MediaScanner();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideFilterButtonRunnable = this::hideFilterButton;

    private int filterMode = FILTER_ALL;
    private boolean filterButtonVisible = false;

    @Nullable
    private ActionMode activeActionMode;

    private final ActivityResultLauncher<Intent> viewerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                boolean deletedOccurred = data != null
                        && data.getBooleanExtra(ViewerActivity.EXTRA_DELETED_OCCURRED, false);
                if (deletedOccurred) {
                    checkPermissionsAndScan();
                }
            });

    private final ActivityResultLauncher<Intent> deletePermissionTreeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    this::handleDeleteTreePicked);

    private final DeleteFlow deleteFlow = new DeleteFlow(this, deletePermissionTreeLauncher);

    private void handleDeleteTreePicked(androidx.activity.result.ActivityResult result) {
        deleteFlow.onTreePicked(result.getData());
    }

    private final ActivityResultLauncher<Uri> openTreeLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), treeUri -> {
                if (treeUri == null) return;
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                performMove(treeUri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);
        btnFilterType = findViewById(R.id.btnFilterType);

        recyclerView.setLayoutManager(new GridLayoutManager(this, GRID_SPAN));
        adapter = new MediaAdapter(mediaList, this::openViewer, this::onItemLongClick);
        recyclerView.setAdapter(adapter);

        setupFilterButton();
        setupMainMenuButton();
        checkPermissionsAndScan();
    }

    // ---------------------------------------------------------------
    // Quick photo/video filter (appears while scrolling, near the top
    // corner where the selection "..." overflow also shows up)
    // ---------------------------------------------------------------

    private void setupFilterButton() {
        btnFilterType.setOnClickListener(v -> {
            filterMode = (filterMode + 1) % 3; // ALL -> PHOTOS_ONLY -> VIDEOS_ONLY -> ALL
            applyFilter();
            updateFilterButtonIcon();
            int toastRes;
            if (filterMode == FILTER_PHOTOS_ONLY) toastRes = R.string.filter_toast_photos_only;
            else if (filterMode == FILTER_VIDEOS_ONLY) toastRes = R.string.filter_toast_videos_only;
            else toastRes = R.string.filter_toast_all;
            Toast.makeText(this, toastRes, Toast.LENGTH_SHORT).show();
            scheduleHideFilterButton();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dx != 0 || dy != 0) showFilterButton();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scheduleHideFilterButton();
                } else {
                    cancelHideFilterButton();
                }
            }
        });
    }

    private void updateFilterButtonIcon() {
        // Icon always shows what the NEXT tap will switch to.
        int iconRes;
        int descRes;
        if (filterMode == FILTER_ALL) {
            iconRes = R.drawable.ic_photo_filter;
            descRes = R.string.filter_button_show_photos;
        } else if (filterMode == FILTER_PHOTOS_ONLY) {
            iconRes = R.drawable.ic_video_filter;
            descRes = R.string.filter_button_show_videos;
        } else {
            iconRes = R.drawable.ic_all_media;
            descRes = R.string.filter_button_show_all;
        }
        btnFilterType.setImageResource(iconRes);
        btnFilterType.setContentDescription(getString(descRes));
    }

    private void showFilterButton() {
        cancelHideFilterButton();
        if (!filterButtonVisible) {
            filterButtonVisible = true;
            btnFilterType.setVisibility(View.VISIBLE);
            btnFilterType.animate().alpha(1f).setDuration(150).start();
        }
    }

    private void hideFilterButton() {
        if (filterButtonVisible) {
            filterButtonVisible = false;
            btnFilterType.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> btnFilterType.setVisibility(View.GONE)).start();
        }
    }

    private void scheduleHideFilterButton() {
        uiHandler.removeCallbacks(hideFilterButtonRunnable);
        uiHandler.postDelayed(hideFilterButtonRunnable, FILTER_BUTTON_HIDE_DELAY_MS);
    }

    private void cancelHideFilterButton() {
        uiHandler.removeCallbacks(hideFilterButtonRunnable);
    }

    private void applyFilter() {
        mediaList.clear();
        if (filterMode == FILTER_PHOTOS_ONLY) {
            for (MediaItem item : allMediaItems) {
                if (!item.isVideo()) mediaList.add(item);
            }
        } else if (filterMode == FILTER_VIDEOS_ONLY) {
            for (MediaItem item : allMediaItems) {
                if (item.isVideo()) mediaList.add(item);
            }
        } else {
            mediaList.addAll(allMediaItems);
        }
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (!mediaList.isEmpty()) {
            emptyView.setVisibility(View.GONE);
            return;
        }
        emptyView.setVisibility(View.VISIBLE);
        if (allMediaItems.isEmpty()) {
            emptyView.setText(R.string.no_media_found);
        } else if (filterMode == FILTER_VIDEOS_ONLY) {
            // There is media, just none matching the current filter.
            emptyView.setText(R.string.no_videos_found);
        } else {
            emptyView.setText(R.string.no_photos_found);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (handleMainMenuItem(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    private boolean handleMainMenuItem(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            checkPermissionsAndScan();
            return true;
        } else if (item.getItemId() == R.id.action_youtube) {
            startActivity(new Intent(this, YoutubeBrowserActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_equalizer) {
            // Audio Equalizer now opens the bundled ASsound audio engine
            // (formerly the standalone Tunex app) instead of a local
            // in-gallery equalizer screen.
            startActivity(new Intent(this, com.assound.MainActivity.class));
            return true;
        }
        return false;
    }

    private void setupMainMenuButton() {
        View btnMainMenu = findViewById(R.id.btnMainMenu);
        btnMainMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(this::handleMainMenuItem);
            popup.show();
        });
    }

    private void openViewer(int position) {
        ArrayList<String> paths = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        for (MediaItem m : mediaList) {
            paths.add(m.getPath());
            types.add(m.type);
        }
        Intent intent = new Intent(this, ViewerActivity.class);
        intent.putStringArrayListExtra(ViewerActivity.EXTRA_PATHS, paths);
        intent.putIntegerArrayListExtra(ViewerActivity.EXTRA_TYPES, types);
        intent.putExtra(ViewerActivity.EXTRA_START_POSITION, position);
        viewerLauncher.launch(intent);
    }

    // ---------------------------------------------------------------
    // Selection mode (long-press to start, contextual toolbar with "..." menu)
    // ---------------------------------------------------------------

    private void onItemLongClick(int position) {
        adapter.toggleSelection(position);
        if (activeActionMode == null) {
            activeActionMode = startSupportActionMode(actionModeCallback);
        }
        updateActionModeTitle();
    }

    private void updateActionModeTitle() {
        if (activeActionMode != null) {
            int count = adapter.getSelectedCount();
            if (count == 0) {
                activeActionMode.finish();
                return;
            }
            activeActionMode.setTitle(getString(R.string.selected_count, count));

            int photoCount = 0;
            int videoCount = 0;
            for (MediaItem item : getSelectedItems()) {
                if (item.isVideo()) videoCount++; else photoCount++;
            }
            activeActionMode.setSubtitle(getString(R.string.selected_breakdown, photoCount, videoCount));
            activeActionMode.invalidate();
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.selection_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<MediaItem> selected = getSelectedItems();
            boolean singlePhoto = selected.size() == 1 && !selected.get(0).isVideo();
            boolean singleItem = selected.size() == 1;

            menu.findItem(R.id.action_wallpaper).setVisible(singlePhoto);
            menu.findItem(R.id.action_info).setVisible(singleItem);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_select_all) {
                adapter.selectAll();
                updateActionModeTitle();
                return true;
            } else if (id == R.id.action_move) {
                openTreeLauncher.launch(null);
                return true;
            } else if (id == R.id.action_info) {
                showFileInfo();
                return true;
            } else if (id == R.id.action_delete) {
                confirmAndDelete();
                return true;
            } else if (id == R.id.action_wallpaper) {
                showWallpaperDialog();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelection();
            activeActionMode = null;
        }
    };

    private List<MediaItem> getSelectedItems() {
        List<MediaItem> result = new ArrayList<>();
        for (int pos : adapter.getSelectedPositions()) {
            if (pos >= 0 && pos < mediaList.size()) {
                result.add(mediaList.get(pos));
            }
        }
        return result;
    }

    // ---------------------------------------------------------------
    // Move to another folder (Storage Access Framework - works for SD card too)
    // ---------------------------------------------------------------

    private void performMove(Uri targetTreeUri) {
        List<MediaItem> selected = getSelectedItems();
        if (selected.isEmpty()) return;

        Toast.makeText(this, R.string.pick_target_folder_hint, Toast.LENGTH_SHORT).show();

        FileOps.moveFiles(this, selected, targetTreeUri, (movedCount, failedCount) -> {
            if (failedCount == 0) {
                Toast.makeText(this, getString(R.string.move_success, movedCount), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.move_partial_fail, movedCount, failedCount), Toast.LENGTH_LONG).show();
            }
            if (activeActionMode != null) activeActionMode.finish();
            checkPermissionsAndScan();
        });
    }

    // ---------------------------------------------------------------
    // File info: name + folder location
    // ---------------------------------------------------------------

    private void showFileInfo() {
        List<MediaItem> selected = getSelectedItems();
        if (selected.size() != 1) return;
        MediaItem item = selected.get(0);
        String folder = item.file.getParentFile() != null
                ? item.file.getParentFile().getAbsolutePath()
                : "-";

        new AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setMessage(getString(R.string.file_info_message, item.getName(), folder))
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    // ---------------------------------------------------------------
    // Permanent delete (removes from actual storage, not just the app's list)
    // ---------------------------------------------------------------

    private void confirmAndDelete() {
        List<MediaItem> selected = getSelectedItems();
        if (selected.isEmpty()) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(getString(R.string.delete_confirm_message, selected.size()))
                .setNegativeButton(R.string.delete_cancel, null)
                .setPositiveButton(R.string.delete_yes, (dialog, which) -> performDelete(selected))
                .show();
    }

    private void performDelete(List<MediaItem> selected) {
        deleteFlow.start(selected, (deletedCount, failedCount) -> {
            if (failedCount == 0) {
                Toast.makeText(this, getString(R.string.delete_success, deletedCount), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.delete_partial_fail, deletedCount, failedCount), Toast.LENGTH_LONG).show();
            }
            if (activeActionMode != null) activeActionMode.finish();
            checkPermissionsAndScan();
        });
    }

    // ---------------------------------------------------------------
    // Set as wallpaper (photo only, single selection)
    // ---------------------------------------------------------------

    private void showWallpaperDialog() {
        List<MediaItem> selected = getSelectedItems();
        if (selected.size() != 1 || selected.get(0).isVideo()) {
            Toast.makeText(this, R.string.wallpaper_only_photo, Toast.LENGTH_SHORT).show();
            return;
        }
        MediaItem photo = selected.get(0);

        String[] options = {
                getString(R.string.wallpaper_home),
                getString(R.string.wallpaper_lock),
                getString(R.string.wallpaper_both)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_dialog_title)
                .setItems(options, (dialog, which) -> {
                    int target;
                    if (which == 0) target = WallpaperHelper.TARGET_HOME;
                    else if (which == 1) target = WallpaperHelper.TARGET_LOCK;
                    else target = WallpaperHelper.TARGET_BOTH;

                    WallpaperHelper.apply(this, photo, target, success -> {
                        Toast.makeText(this,
                                success ? R.string.wallpaper_success : R.string.wallpaper_failed,
                                Toast.LENGTH_LONG).show();
                        if (activeActionMode != null) activeActionMode.finish();
                    });
                })
                .show();
    }

    // ---------------------------------------------------------------
    // Permissions
    // ---------------------------------------------------------------

    private void checkPermissionsAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: need "All files access" to reliably walk ANY
            // nested subfolder (Download/fkdos/djdiejd/...), not just
            // what MediaStore happens to have indexed.
            if (Environment.isExternalStorageManager()) {
                startScan();
            } else {
                Toast.makeText(this,
                        "Izinkan akses semua file agar galeri bisa membaca semua folder",
                        Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                }
            }
        } else {
            boolean hasRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            boolean hasWrite = Build.VERSION.SDK_INT > Build.VERSION_CODES.P
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
            if (hasRead && hasWrite) {
                startScan();
            } else {
                // WRITE_EXTERNAL_STORAGE is what actually lets file.delete()
                // and moves succeed on Android 10 and below - without it
                // being granted, delete silently fails every time.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQ_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allGranted = requestCode == REQ_PERMISSIONS && grantResults.length > 0;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) allGranted = false;
        }
        if (allGranted) {
            startScan();
        } else {
            Toast.makeText(this, "Izin penyimpanan (baca & tulis) dibutuhkan agar galeri dan fitur hapus/pindah bisa jalan", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // In case user just came back from the "All files access" settings screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Environment.isExternalStorageManager()
                && allMediaItems.isEmpty()) {
            startScan();
        }
    }

    // ---------------------------------------------------------------
    // Scanning
    // ---------------------------------------------------------------

    private void startScan() {
        emptyView.setText(R.string.scanning);
        emptyView.setVisibility(View.VISIBLE);

        scanner.scanAsync(resolveScanRoots(), items -> {
            allMediaItems.clear();
            allMediaItems.addAll(items);
            applyFilter();
        });
    }

    /**
     * Builds the list of root folders to scan: the ENTIRE internal storage
     * volume, plus the entire root of every additional storage volume
     * Android reports (e.g. an inserted SD card). MediaScanner then walks
     * every nested subfolder beneath each of these roots (any depth), so
     * photos/videos show up no matter which folder they're in - Download,
     * DCIM, Pictures, WhatsApp, a custom folder, etc - not just Download.
     */
    private List<File> resolveScanRoots() {
        List<File> roots = new ArrayList<>();
        String primaryPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        roots.add(new File(primaryPath));

        File[] externalDirs = ContextCompat.getExternalFilesDirs(this, null);
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                if (dir == null) continue;
                String path = dir.getAbsolutePath();
                int idx = path.indexOf("/Android/data/");
                if (idx <= 0) continue;
                String volumeRoot = path.substring(0, idx);
                if (!volumeRoot.equals(primaryPath)) {
                    roots.add(new File(volumeRoot));
                }
            }
        }
        return roots;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacksAndMessages(null);
    }
}
