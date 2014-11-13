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

import org.puder.trs80.browser.FileBrowserActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class EditConfigurationFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private SharedPreferences sharedPrefs;
    private Handler           handler;

    private Preference        model;
    private Preference        name;
    private Preference        cassette;
    private Preference        disk1;
    private Preference        disk2;
    private Preference        disk3;
    private Preference        disk4;
    private Preference        characterColor;
    private Preference        keyboardPortrait;
    private Preference        keyboardLandscape;

    private CharSequence      defaultCassetteSummary;
    private CharSequence      defaultDisk1Summary;
    private CharSequence      defaultDisk2Summary;
    private CharSequence      defaultDisk3Summary;
    private CharSequence      defaultDisk4Summary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        Intent i = getActivity().getIntent();
        int configId = i.getExtras().getInt("CONFIG_ID");
        getPreferenceManager().setSharedPreferencesName("CONFIG_" + configId);
        addPreferencesFromResource(R.xml.configuration);
        sharedPrefs = this.getPreferenceManager().getSharedPreferences();// this.getPreferences(MODE_PRIVATE);
        name = (Preference) findPreference(EditConfigurationActivity.CONF_NAME);
        name.setOnPreferenceChangeListener(this);
        Preference.OnPreferenceClickListener listener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                String key = pref.getKey();
                int requestCode;
                if (key.equals(EditConfigurationActivity.CONF_CASSETTE)) {
                    requestCode = 0;
                } else {
                    requestCode = Integer.parseInt(key.substring(key.length() - 1));
                }
                Intent intent = new Intent(getActivity(), FileBrowserActivity.class);
                startActivityForResult(intent, requestCode);
                return true;
            }
        };

        cassette = (Preference) findPreference(EditConfigurationActivity.CONF_CASSETTE);
        cassette.setOnPreferenceChangeListener(this);
        cassette.setOnPreferenceClickListener(listener);
        defaultCassetteSummary = cassette.getSummary();

        disk1 = (Preference) findPreference(EditConfigurationActivity.CONF_DISK1);
        disk1.setOnPreferenceChangeListener(this);
        disk1.setOnPreferenceClickListener(listener);
        defaultDisk1Summary = disk1.getSummary();

        disk2 = (Preference) findPreference(EditConfigurationActivity.CONF_DISK2);
        disk2.setOnPreferenceChangeListener(this);
        disk2.setOnPreferenceClickListener(listener);
        defaultDisk2Summary = disk2.getSummary();

        disk3 = (Preference) findPreference(EditConfigurationActivity.CONF_DISK3);
        disk3.setOnPreferenceChangeListener(this);
        disk3.setOnPreferenceClickListener(listener);
        defaultDisk3Summary = disk3.getSummary();

        disk4 = (Preference) findPreference(EditConfigurationActivity.CONF_DISK4);
        disk4.setOnPreferenceChangeListener(this);
        disk4.setOnPreferenceClickListener(listener);
        defaultDisk4Summary = disk4.getSummary();

        model = (Preference) findPreference(EditConfigurationActivity.CONF_MODEL);
        model.setOnPreferenceChangeListener(this);

        characterColor = (Preference) findPreference(EditConfigurationActivity.CONF_CHARACTER_COLOR);
        characterColor.setOnPreferenceChangeListener(this);

        keyboardPortrait = (Preference) findPreference(EditConfigurationActivity.CONF_KEYBOARD_PORTRAIT);
        keyboardPortrait.setOnPreferenceChangeListener(this);

        keyboardLandscape = (Preference) findPreference(EditConfigurationActivity.CONF_KEYBOARD_LANDSCAPE);
        keyboardLandscape.setOnPreferenceChangeListener(this);

        updateSummaries();
    }

    private void updateSummaries() {
        String val = sharedPrefs.getString(EditConfigurationActivity.CONF_NAME, null);
        if (val != null) {
            name.setSummary(val);
        }
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_MODEL, null);
        if (val != null) {
            if (val.equals("1")) {
                val = "I";
            } else if (val.equals("3")) {
                val = "III";
            } else if (val.equals("5")) {
                val = "4P";
            }
            model.setSummary("Model " + val);
        }

        // Cassette
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_CASSETTE, null);
        cassette.setSummary(val != null ? val : defaultCassetteSummary);

        // Disk 1
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_DISK1, null);
        disk1.setSummary(val != null ? val : defaultDisk1Summary);

        // Disk 2
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_DISK2, null);
        disk2.setSummary(val != null ? val : defaultDisk2Summary);

        // Disk 3
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_DISK3, null);
        disk3.setSummary(val != null ? val : defaultDisk3Summary);

        // Disk 4
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_DISK4, null);
        disk4.setSummary(val != null ? val : defaultDisk4Summary);

        // Character color
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_CHARACTER_COLOR, null);
        setCharacterColorSummary(val);

        // Keyboard portrait
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_KEYBOARD_PORTRAIT, null);
        setKeyboardSummary(keyboardPortrait, val);

        // Keyboard landscape
        val = sharedPrefs.getString(EditConfigurationActivity.CONF_KEYBOARD_LANDSCAPE, null);
        setKeyboardSummary(keyboardLandscape, val);
    }

    private void setCharacterColorSummary(String val) {
        if (val == null) {
            return;
        }
        if ("0".equals(val)) {
            characterColor.setSummary(this.getString(R.string.green));
        }
        if ("1".equals(val)) {
            characterColor.setSummary(this.getString(R.string.white));
        }
    }

    private void setKeyboardSummary(Preference pref, String val) {
        if (val == null) {
            return;
        }
        if ("0".equals(val)) {
            pref.setSummary(this.getString(R.string.keyboard_original));
        }
        if ("1".equals(val)) {
            pref.setSummary(this.getString(R.string.keyboard_compact));
        }
        if ("2".equals(val)) {
            pref.setSummary(this.getString(R.string.keyboard_joystick));
        }
        if ("3".equals(val)) {
            pref.setSummary("unused");
        }
        if ("4".equals(val)) {
            pref.setSummary(this.getString(R.string.keyboard_tilt));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String newValue = data.getStringExtra("PATH");
            SharedPreferences.Editor editor = sharedPrefs.edit();
            String key;
            if (requestCode == 0) {
                key = EditConfigurationActivity.CONF_CASSETTE;
            } else {
                key = "conf_disk" + requestCode;
            }
            editor.putString(key, newValue);
            editor.commit();
            updateSummaries();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
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
