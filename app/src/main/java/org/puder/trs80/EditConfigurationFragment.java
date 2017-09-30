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
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Optional;

import org.puder.trs80.browser.FileBrowserActivity;
import org.puder.trs80.configuration.ConfigurationPersistence;
import org.puder.trs80.configuration.ConfigurationPersistence.PreferenceFinder;
import org.puder.trs80.configuration.ConfigurationPersistence.PreferenceProvider;

public class EditConfigurationFragment extends PreferenceFragment implements
        OnPreferenceChangeListener {

    private Handler           handler;

    private boolean           configurationWasEdited;

    private ConfigurationPersistence  configPersitence;
    private PreferenceFinder  prefFinder;
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

    private String      defaultCassetteSummary;
    private String      defaultDisk1Summary;
    private String      defaultDisk2Summary;
    private String      defaultDisk3Summary;
    private String      defaultDisk4Summary;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configurationWasEdited = false;
        handler = new Handler();
        Intent i = getActivity().getIntent();
        int configId = i.getExtras().getInt("CONFIG_ID");
        configPersitence = ConfigurationPersistence.forIdAndManager(
                configId, getPreferenceManager());
        prefFinder = configPersitence.forPreferenceProvider(new PreferenceProvider() {
            @Override
            public Preference findPreference(String name) {
                return EditConfigurationFragment.this.findPreference(name);
            }
        });
        addPreferencesFromResource(R.xml.configuration);
        name = prefFinder.forName();
        name.setOnPreferenceChangeListener(this);
        Preference.OnPreferenceClickListener listener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                String key = pref.getKey();
                int requestCode;
                if (key.equals("conf_cassette")) {
                    requestCode = 0;
                } else {
                    requestCode = Integer.parseInt(key.substring(key.length() - 1));
                }
                Intent intent = new Intent(getActivity(), FileBrowserActivity.class);
                startActivityForResult(intent, requestCode);
                return true;
            }
        };

        cassette = prefFinder.forCasette();
        cassette.setOnPreferenceChangeListener(this);
        cassette.setOnPreferenceClickListener(listener);
        defaultCassetteSummary = cassette.getSummary().toString();

        disk1 = prefFinder.forDisk1();
        disk1.setOnPreferenceChangeListener(this);
        disk1.setOnPreferenceClickListener(listener);
        defaultDisk1Summary = disk1.getSummary().toString();

        disk2 = prefFinder.forDisk2();
        disk2.setOnPreferenceChangeListener(this);
        disk2.setOnPreferenceClickListener(listener);
        defaultDisk2Summary = disk2.getSummary().toString();

        disk3 = prefFinder.forDisk3();
        disk3.setOnPreferenceChangeListener(this);
        disk3.setOnPreferenceClickListener(listener);
        defaultDisk3Summary = disk3.getSummary().toString();

        disk4 = prefFinder.forDisk4();
        disk4.setOnPreferenceChangeListener(this);
        disk4.setOnPreferenceClickListener(listener);
        defaultDisk4Summary = disk4.getSummary().toString();

        model = prefFinder.forModel();
        model.setOnPreferenceChangeListener(this);

        characterColor = prefFinder.forCharacterColor();
        characterColor.setOnPreferenceChangeListener(this);

        keyboardPortrait = prefFinder.forKeyboardPortrait();
        keyboardPortrait.setOnPreferenceChangeListener(this);

        keyboardLandscape = prefFinder.forKeyboardLandscape();
        keyboardLandscape.setOnPreferenceChangeListener(this);

        updateSummaries();
    }

    private void updateSummaries() {
        Optional<String> valOpt = configPersitence.getName();
        if (valOpt.isPresent()) {
            name.setSummary(valOpt.get());
        }
        valOpt = configPersitence.getModel();
        if (valOpt.isPresent()) {
            String val = valOpt.get();
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
        valOpt = configPersitence.getCasettePath();
        cassette.setSummary(valOpt.or(defaultCassetteSummary));

        // Disk 1
        valOpt = configPersitence.getDiskPath(0);
        disk1.setSummary(valOpt.or(defaultDisk1Summary));

        // Disk 2
        valOpt = configPersitence.getDiskPath(1);
        disk2.setSummary(valOpt.or(defaultDisk2Summary));

        // Disk 3
        valOpt = configPersitence.getDiskPath(2);
        disk3.setSummary(valOpt.or(defaultDisk3Summary));

        // Disk 4
        valOpt = configPersitence.getDiskPath(3);
        disk4.setSummary(valOpt.or(defaultDisk4Summary));

        // Character color
        setCharacterColorSummary(configPersitence.getCharacterColor(0));

        // Keyboard portrait
        setKeyboardSummary(keyboardPortrait, configPersitence.getKeyboardLayoutPortrait());

        // Keyboard landscape
        setKeyboardSummary(keyboardLandscape, configPersitence.getKeyboardLayoutLandscape());
    }

    private void setCharacterColorSummary(int val) {
        switch (val) {
            case 0:
                characterColor.setSummary(this.getString(R.string.green));
                return;
            case 1:
                characterColor.setSummary(this.getString(R.string.white));
                return;
            default:
                return;
        }
    }

    private void setKeyboardSummary(Preference pref, int val) {
        switch (val) {
            case 0:
                pref.setSummary(this.getString(R.string.keyboard_original));
                break;
            case 1:
                pref.setSummary(this.getString(R.string.keyboard_compact));
                break;
            case 2:
                pref.setSummary(this.getString(R.string.keyboard_joystick));
                break;
            case 3:
                pref.setSummary(R.string.keyboard_game_controller);
            case 4:
                pref.setSummary(this.getString(R.string.keyboard_tilt));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            configurationWasEdited = true;
            String newValue = data.getStringExtra("PATH");
            switch (requestCode){
                case 0:
                    configPersitence.setCasettePath(newValue);
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                    configPersitence.setDiskPath(requestCode - 1, newValue);

            }
            updateSummaries();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        configurationWasEdited = true;
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

    public boolean configurationWasEdited() {
        return configurationWasEdited;
    }
}
