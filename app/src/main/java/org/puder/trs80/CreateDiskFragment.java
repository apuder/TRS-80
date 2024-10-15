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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import static org.puder.trs80.CreateDiskActivity.MKDISK_DENSITY;
import static org.puder.trs80.CreateDiskActivity.MKDISK_FORMAT;
import static org.puder.trs80.CreateDiskActivity.MKDISK_IGNORE_DENSITY;
import static org.puder.trs80.CreateDiskActivity.MKDISK_NAME;
import static org.puder.trs80.CreateDiskActivity.MKDISK_SIDED;
import static org.puder.trs80.CreateDiskActivity.MKDISK_SIZE;

public class CreateDiskFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

    private Handler handler;

    private SharedPreferences sharedPrefs;

    private Preference name;
    private Preference format;
    private Preference sided;
    private Preference density;
    private Preference size;
    private Preference ignoreDensity;

    private String defaultNameSummary;
    private String defaultFormatSummary;
    private String defaultSidedSummary;
    private String defaultDensitySummary;
    private String defaultSizeSummary;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        addPreferencesFromResource(R.xml.mkdisk);

        name = findPreference(MKDISK_NAME);
        name.setOnPreferenceChangeListener(this);
        defaultNameSummary = name.getSummary().toString();

        format = findPreference(MKDISK_FORMAT);
        format.setOnPreferenceChangeListener(this);
        defaultFormatSummary = format.getSummary().toString();

        sided = findPreference(MKDISK_SIDED);
        sided.setOnPreferenceChangeListener(this);
        defaultSidedSummary = sided.getSummary().toString();

        density = findPreference(MKDISK_DENSITY);
        density.setOnPreferenceChangeListener(this);
        defaultDensitySummary = density.getSummary().toString();

        size = findPreference(MKDISK_SIZE);
        size.setOnPreferenceChangeListener(this);
        defaultSizeSummary = size.getSummary().toString();

        ignoreDensity = findPreference(MKDISK_IGNORE_DENSITY);
        ignoreDensity.setOnPreferenceChangeListener(this);

        updateSummaries();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setFitsSystemWindows(true);
    }

    private void updateSummaries() {
        String val;

        val = sharedPrefs.getString(MKDISK_NAME, defaultNameSummary);
        if (val.isEmpty()) val = defaultNameSummary;
        name.setSummary(val);

        val = sharedPrefs.getString(MKDISK_FORMAT, defaultFormatSummary);
        format.setSummary(val);

        boolean dmkSelected = val.equalsIgnoreCase(getString(R.string.mkdisk_dmk));
        getPreferenceScreen().findPreference(MKDISK_SIDED).setEnabled(dmkSelected);
        getPreferenceScreen().findPreference(MKDISK_DENSITY).setEnabled(dmkSelected);
        getPreferenceScreen().findPreference(MKDISK_SIZE).setEnabled(dmkSelected);
        getPreferenceScreen().findPreference(MKDISK_IGNORE_DENSITY).setEnabled(dmkSelected);

        val = sharedPrefs.getString(MKDISK_SIDED, defaultSidedSummary);
        sided.setSummary(val);

        val = sharedPrefs.getString(MKDISK_DENSITY, defaultDensitySummary);
        density.setSummary(val);

        val = sharedPrefs.getString(MKDISK_SIZE, defaultSizeSummary);
        size.setSummary(val);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equalsIgnoreCase(MKDISK_NAME)) {
            CreateDiskActivity activity = (CreateDiskActivity)getActivity();

            if (!activity.validateDiskImageName(newValue.toString())) {
                View root = activity.findViewById(android.R.id.content);
                Snackbar.make(root, getString(R.string.mkdisk_bad_path) + newValue.toString(),
                        Snackbar.LENGTH_SHORT).show();
                return false;
            }
        }

        /*
         * When we get to this point, the preferences have not yet been updated
         * yet. For this reason updateSummaries() is called via a handler to
         * ensure the preferences have been updated.
         */
        handler.post(new Runnable() {

            @Override
            public void run() {
                updateSummaries();
            }
        });
        return true;
    }
}
