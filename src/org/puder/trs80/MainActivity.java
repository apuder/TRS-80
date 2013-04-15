/*
 * Copyright 2012-2013, webappbooster.org
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

import org.puder.trs80.Hardware.Model;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {

    private Configuration[] configurations;
    private String[]        configurationNames;
    private Dialog          dialog;
    private int             selectedPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    // @Override
    // public void onDestroy() {
    // super.onDestroy();
    // XTRS.cleanup();
    // }

    private void updateView() {
        configurations = Configuration.getConfigurations();
        if (configurations.length == 0) {
            setContentView(R.layout.no_configurations);
            return;
        }

        setContentView(R.layout.main_activity);

        Bitmap screenshot = TRS80Application.getScreenshot();
        if (screenshot != null) {
            ImageView img = (ImageView) findViewById(R.id.screenshot);
            img.setImageBitmap(screenshot);
        }

        Configuration conf = TRS80Application.getCurrentConfiguration();
        TextView nameLabel = (TextView) findViewById(R.id.current_configuration_name);
        nameLabel.setText(conf == null ? "-" : conf.getName());

        configurationNames = new String[configurations.length];
        for (int i = 0; i < configurations.length; i++) {
            configurationNames[i] = configurations[i].getName();
        }

        ListView list = (ListView) this.findViewById(R.id.list_configurations);
        list.setAdapter(new ArrayAdapter<String>(this, R.layout.configuration_item,
                configurationNames));
        list.setLongClickable(true);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add_configuration:
            addConfiguration();
            return true;
        case R.id.menu_settings:
            showSettings();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void addConfiguration() {
        Configuration newConfig = Configuration.addConfiguration();
        editConfiguration(newConfig);
    }

    private void showSettings() {
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    private void editConfiguration(Configuration conf) {
        Intent i = new Intent(this, ConfigurationActivity.class);
        i.putExtra("CONFIG_ID", conf.getId());
        startActivity(i);
    }

    private void startConfiguration(Configuration conf) {
        Model model = conf.getModel();
        if (model != Model.MODEL3) {
            Toast.makeText(this, "Only Model 3 is supported at this time.", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        TRS80Application.setCurrentConfiguration(conf);
        Hardware hardware = new Model3();
        int sizeROM = hardware.getSizeROM();
        if (sizeROM == 0) {
            Toast.makeText(this, "No valid ROM found. Please use Settings to set ROM.", Toast.LENGTH_LONG).show();
            return;
        }
        TRS80Application.setHardware(hardware);
        byte[] memBuffer = hardware.getMemoryBuffer();
        byte[] screenBuffer = hardware.getScreenBuffer();
        int entryAddr = hardware.getEntryAddress();
        XTRS.init(conf.getModel().getModelValue(), sizeROM, entryAddr, memBuffer, screenBuffer);
        Intent i = new Intent(this, EmulatorActivity.class);
        startActivity(i);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startConfiguration(configurations[position]);
    }

    public void doEdit(View view) {
        dialog.dismiss();
        editConfiguration(configurations[selectedPosition]);
    }

    public void doStart(View view) {
        dialog.dismiss();
        startConfiguration(configurations[selectedPosition]);
    }

    public void doResumeEmulator(View view) {
        Intent i = new Intent(this, EmulatorActivity.class);
        startActivity(i);
    }

    public void doDelete(View view) {
        dialog.dismiss();
        // TODO delete
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        selectedPosition = position;
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        dialog = new Dialog(this);
        dialog.setContentView(inflater.inflate(R.layout.configuration_popup, null, false));
        dialog.setTitle("Choose action:");
        dialog.show();

        return true;
    }
}
