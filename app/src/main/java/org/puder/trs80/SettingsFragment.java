/*
 * Copyright 2012-2013, Arno Puder
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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import org.puder.trs80.browser.FileBrowserActivity;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener {

    private SharedPreferences sharedPrefs;
    private Preference        romModel1;
    private Preference        romModel3;
    private Preference        romModel4;
    private Preference        romModel4p;

    private CharSequence      defaultRomModel1Summary;
    private CharSequence      defaultRomModel3Summary;
    private CharSequence      defaultRomModel4Summary;
    private CharSequence      defaultRomModel4PSummary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        PreferenceManager mgr = this.getPreferenceManager();
        mgr.setSharedPreferencesName(SettingsActivity.SHARED_PREF_NAME);
        sharedPrefs = mgr.getSharedPreferences();
        Preference.OnPreferenceClickListener listener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(getActivity(), FileBrowserActivity.class);
                String key = pref.getKey();
                startActivityForResult(intent, key.hashCode());
                return true;
            }
        };
        romModel1 = findPreference(SettingsActivity.CONF_ROM_MODEL1);
        romModel1.setOnPreferenceChangeListener(this);
        romModel1.setOnPreferenceClickListener(listener);
        defaultRomModel1Summary = romModel1.getSummary();

        romModel3 = findPreference(SettingsActivity.CONF_ROM_MODEL3);
        romModel3.setOnPreferenceChangeListener(this);
        romModel3.setOnPreferenceClickListener(listener);
        defaultRomModel3Summary = romModel3.getSummary();

        /*
        romModel4 = findPreference(SettingsActivity.CONF_ROM_MODEL4);
        romModel4.setOnPreferenceChangeListener(this);
        romModel4.setOnPreferenceClickListener(listener);
        defaultRomModel4Summary = romModel4.getSummary();

        romModel4p = findPreference(SettingsActivity.CONF_ROM_MODEL4P);
        romModel4p.setOnPreferenceChangeListener(this);
        romModel4p.setOnPreferenceClickListener(listener);
        defaultRomModel4PSummary = romModel4p.getSummary();
        */
        updateSummaries();
    }

    private void updateSummaries() {
        String val = sharedPrefs.getString(SettingsActivity.CONF_ROM_MODEL1, null);
        romModel1.setSummary(val != null ? val : defaultRomModel1Summary);
        val = sharedPrefs.getString(SettingsActivity.CONF_ROM_MODEL3, null);
        romModel3.setSummary(val != null ? val : defaultRomModel3Summary);
        /*
        val = sharedPrefs.getString(SettingsActivity.CONF_ROM_MODEL4, null);
        romModel4.setSummary(val != null ? val : defaultRomModel4Summary);
        val = sharedPrefs.getString(SettingsActivity.CONF_ROM_MODEL4P, null);
        romModel4p.setSummary(val != null ? val : defaultRomModel4PSummary);
        */
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String path = data.getStringExtra("PATH");
            SharedPreferences.Editor editor = sharedPrefs.edit();
            if (SettingsActivity.CONF_ROM_MODEL1.hashCode() == requestCode) {
                editor.putString(SettingsActivity.CONF_ROM_MODEL1, path);
            }
            if (SettingsActivity.CONF_ROM_MODEL3.hashCode() == requestCode) {
                editor.putString(SettingsActivity.CONF_ROM_MODEL3, path);
            }
            if (SettingsActivity.CONF_ROM_MODEL4.hashCode() == requestCode) {
                editor.putString(SettingsActivity.CONF_ROM_MODEL4, path);
            }
            if (SettingsActivity.CONF_ROM_MODEL4P.hashCode() == requestCode) {
                editor.putString(SettingsActivity.CONF_ROM_MODEL4P, path);
            }
            editor.apply();
            updateSummaries();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        preference.setSummary(newValue.toString());
        return true;
    }
}