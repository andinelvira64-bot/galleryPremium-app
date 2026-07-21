package com.elvira.gallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    private RecyclerView recyclerView;
    private TextView emptyView;
    private MediaAdapter adapter;
    private final List<MediaItem> mediaList = new ArrayList<>();
    private final MediaScanner scanner = new MediaScanner();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new GridLayoutManager(this, GRID_SPAN));
        adapter = new MediaAdapter(mediaList, this::openViewer);
        recyclerView.setAdapter(adapter);

        checkPermissionsAndScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            checkPermissionsAndScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        startActivity(intent);
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQ_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            Toast.makeText(this, "Izin penyimpanan dibutuhkan untuk menampilkan galeri", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // In case user just came back from the "All files access" settings screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Environment.isExternalStorageManager()
                && mediaList.isEmpty()) {
            startScan();
        }
    }

    // ---------------------------------------------------------------
    // Scanning
    // ---------------------------------------------------------------

    private void startScan() {
        emptyView.setText(R.string.scanning);
        emptyView.setVisibility(android.view.View.VISIBLE);

        // Root folder to scan. Change this if photos/videos live somewhere else,
        // e.g. new File(Environment.getExternalStorageDirectory(), "DCIM")
        File downloadRoot = new File(Environment.getExternalStorageDirectory(), "Download");

        scanner.scanAsync(downloadRoot, items -> {
            mediaList.clear();
            mediaList.addAll(items);
            adapter.notifyDataSetChanged();

            if (mediaList.isEmpty()) {
                emptyView.setText(R.string.no_media_found);
                emptyView.setVisibility(android.view.View.VISIBLE);
            } else {
                emptyView.setVisibility(android.view.View.GONE);
            }
        });
    }
}
