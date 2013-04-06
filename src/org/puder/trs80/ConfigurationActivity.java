package org.puder.trs80;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class ConfigurationActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        int id = i.getExtras().getInt("CONFIG_ID");
        getPreferenceManager().setSharedPreferencesName("CONFIG_" + id);
        addPreferencesFromResource(R.xml.configuration);
    }
}