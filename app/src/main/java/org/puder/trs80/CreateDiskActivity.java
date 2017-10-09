/*
 * Copyright 2012-2013, Arno Puder
 * Copyright 2017, Robert Corrigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class CreateDiskActivity extends BaseActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String MKDISK_NAME = "mkdisk_name";
    public static final String MKDISK_FORMAT = "mkdisk_format";
    public static final String MKDISK_SIDED = "mkdisk_sided";
    public static final String MKDISK_DENSITY = "mkdisk_density";
    public static final String MKDISK_SIZE = "mkdisk_size";
    public static final String MKDISK_IGNORE_DENSITY = "mkdisk_ignore_density";

    private CreateDiskFragment fragment;
    private SharedPreferences sharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dummy view. Will be replaced by CreateDiskFragment.
        setContentView(new View(this));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        clearDiskImageName();

        fragment = new CreateDiskFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment).commit();
    }

    @Override
    protected void onDestroy() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_create_media, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String diskImageName = getDiskImageName();
        boolean enable = validateDiskImageName(diskImageName);

        MenuItem create_media = menu.findItem(R.id.create_media);
        create_media.setEnabled(enable);
        create_media.getIcon().setAlpha(enable ? 255 : 96);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.create_media) {
            if (createDiskImage()) {
                doneEditing(false);
                return true;
            }
        } else if (id == R.id.cancel_media) {
            doneEditing(true);
            return true;
        } else if (id == android.R.id.home) {
            doneEditing(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean createDiskImage() {
        try {
            String diskImg = getDiskImageName();

            Intent i = getIntent();
            String currentPath = i.getStringExtra("DIR");

            String diskImgPath = new File(currentPath, diskImg).getCanonicalPath();
            if (!diskImgPath.toLowerCase().endsWith(".dsk")) {
                diskImgPath = diskImgPath.concat(".dsk");
            }

            if (new File(diskImgPath).exists()) {
                throw new Exception(getString(R.string.mkdisk_file_exists_error));
            }

            String diskImgFormat = getDiskImageFormat();
            boolean success = false;

            if (diskImgFormat.equalsIgnoreCase(getString(R.string.mkdisk_jv1))) {
                success = XTRS.createBlankJV1(diskImgPath);
            } else if (diskImgFormat.equalsIgnoreCase(getString(R.string.mkdisk_jv3))) {
                success = XTRS.createBlankJV3(diskImgPath);
            } else if (diskImgFormat.equalsIgnoreCase(getString(R.string.mkdisk_dmk))) {
                int diskImgSides = getDiskImageSided();
                int diskImgDensity = getDiskImageDensity();
                int diskImgEight = getDiskImageEight();
                int diskImgIgnoreDensity = getDiskImageIgnoreDensity();

                success = XTRS.createBlankDMK(diskImgPath, diskImgSides, diskImgDensity,
                        diskImgEight, diskImgIgnoreDensity);
            }

            if (success) {
                i.putExtra("PATH", diskImgPath);
                return success;
            } else {
                throw new Exception(getString(R.string.mkdisk_create_error));
            }
        } catch (Exception e) {
            View root = findViewById(android.R.id.content);
            Snackbar.make(root, e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        doneEditing(true);
    }

    private void doneEditing(boolean cancel) {
        setResult(cancel ? RESULT_CANCELED : RESULT_OK, getIntent());
        clearDiskImageName();
        finish();
    }

    private String getDiskImageName() {
        return sharedPrefs.getString(MKDISK_NAME, "");
    }
    private String getDiskImageFormat() {
        return sharedPrefs.getString(MKDISK_FORMAT, getString(R.string.mkdisk_jv1));
    }
    private int getDiskImageSided() {
        String sided = sharedPrefs.getString(MKDISK_SIDED, "1");
        return Integer.parseInt(sided);
    }
    private int getDiskImageDensity() {
        String density = sharedPrefs.getString(MKDISK_DENSITY, getString(R.string.mkdisk_single));
        return density.equalsIgnoreCase(getString(R.string.mkdisk_double)) ? 2 : 1;
    }
    private int getDiskImageEight() {
        String density = sharedPrefs.getString(MKDISK_SIZE, getString(R.string.mkdisk_5_inch));
        return density.equalsIgnoreCase(getString(R.string.mkdisk_8_inch)) ? 1 : 0;
    }
    private int getDiskImageIgnoreDensity() {
        return sharedPrefs.getBoolean(MKDISK_IGNORE_DENSITY, false) ? 1 : 0;
    }


    public boolean validateDiskImageName(String name) {
        return Pattern.matches("[-_.A-Za-z0-9]+", name);
    }

    private void clearDiskImageName() {
        SharedPreferences.Editor edit = sharedPrefs.edit();
        edit.putString(MKDISK_NAME, "");
        edit.commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(MKDISK_NAME)) {
            invalidateOptionsMenu();
        }
    }
}
