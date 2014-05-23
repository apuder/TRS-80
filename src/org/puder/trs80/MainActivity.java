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

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MainActivity extends SherlockFragmentActivity implements OnItemClickListener,
        InitialSetupDialogFragment.DownloadCompletionListener {

    private static final int    REQUEST_CODE_EDIT_CONFIG   = 1;
    private static final int    REQUEST_CODE_RUN_EMULATOR  = 2;
    private static final int    REQUEST_CODE_EDIT_SETTINGS = 3;

    private static final int    MENU_OPTION_START          = 0;
    private static final int    MENU_OPTION_RESUME         = 1;
    private static final int    MENU_OPTION_STOP           = 2;
    private static final int    MENU_OPTION_EDIT           = 3;
    private static final int    MENU_OPTION_DELETE         = 4;
    private static final int    MENU_OPTION_UP             = 5;
    private static final int    MENU_OPTION_DOWN           = 6;

    private List<Configuration> configurations;
    private ConfigurationBackup backup;
    private ListView            configurationListView;
    private SharedPreferences   sharedPrefs;
    private MenuItem            downloadMenuItem           = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = this.getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                Context.MODE_PRIVATE);
        this.setContentView(R.layout.main_activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        if (!sharedPrefs.getBoolean(SettingsActivity.CONF_FIRST_TIME, true)) {
            return;
        }
        Editor editor = sharedPrefs.edit();
        editor.putBoolean(SettingsActivity.CONF_FIRST_TIME, false);
        editor.commit();
        if (!ROMs.hasROMs()) {
            downloadROMs();
        }
    }

    public void updateView() {
        View withoutConfigurationsView = this.findViewById(R.id.without_configurations);
        View withConfigurationsView = this.findViewById(R.id.with_configurations);
        configurations = Configuration.getConfigurations();
        if (configurations.size() == 0) {
            withoutConfigurationsView.setVisibility(View.VISIBLE);
            withConfigurationsView.setVisibility(View.GONE);
            return;
        }

        withoutConfigurationsView.setVisibility(View.GONE);
        withConfigurationsView.setVisibility(View.VISIBLE);

        configurationListView = (ListView) this.findViewById(R.id.list_configurations);
        ConfigurationListViewAdapter confList = new ConfigurationListViewAdapter(this,
                configurations);
        configurationListView.setAdapter(confList);
        configurationListView.setOnItemClickListener(this);
        registerForContextMenu(configurationListView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!ROMs.hasROMs()) {
            downloadMenuItem = menu.add("Download");
            downloadMenuItem.setIcon(R.drawable.download_icon).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        menu.add("Add").setIcon(R.drawable.add_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, 1, Menu.CATEGORY_SYSTEM, "Help").setIcon(R.drawable.help_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, 1, Menu.CATEGORY_SYSTEM, "Settings").setIcon(R.drawable.settings_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        runEmulator(configurations.get(position));
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        CharSequence title = item.getTitle();
        if ("Download".equals(title)) {
            downloadROMs();
            return true;
        }
        if ("Add".equals(title)) {
            addConfiguration();
            return true;
        }
        if ("Help".equals(title)) {
            showHelp();
            return true;
        }
        if ("Settings".equals(title)) {
            showSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Configuration conf = configurations.get((int) info.id);
        menu.setHeaderTitle(conf.getName());
        if (EmulatorState.hasSavedState(conf.getId())) {
            menu.add(0, MENU_OPTION_RESUME, 0, this.getString(R.string.context_menu_resume));
            menu.add(0, MENU_OPTION_STOP, 0, this.getString(R.string.context_menu_stop));
        } else {
            menu.add(0, MENU_OPTION_START, 0, this.getString(R.string.context_menu_start));
        }
        menu.add(0, MENU_OPTION_EDIT, 0, this.getString(R.string.context_menu_edit));
        menu.add(0, MENU_OPTION_DELETE, 0, this.getString(R.string.context_menu_delete));
        if (Configuration.getConfigurations().size() > 1) {
            if (!conf.isFirst()) {
                menu.add(0, MENU_OPTION_UP, 0, this.getString(R.string.context_menu_up));
            }
            if (!conf.isLast()) {
                menu.add(0, MENU_OPTION_DOWN, 0, this.getString(R.string.context_menu_down));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        Configuration conf = configurations.get(info.position);
        int confID = conf.getId();
        switch (item.getItemId()) {
        case MENU_OPTION_START:
            EmulatorState.deleteSavedState(confID);
            runEmulator(conf);
            break;
        case MENU_OPTION_RESUME:
            runEmulator(conf);
            break;
        case MENU_OPTION_STOP:
            stopEmulator(conf);
            break;
        case MENU_OPTION_EDIT:
            editConfiguration(conf, false);
            break;
        case MENU_OPTION_DELETE:
            deleteConfiguration(conf);
            break;
        case MENU_OPTION_UP:
            conf.moveUp();
            updateView();
            break;
        case MENU_OPTION_DOWN:
            conf.moveDown();
            updateView();
            break;
        default:
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDIT_CONFIG) {
            if (resultCode == Activity.RESULT_OK || data == null) {
                return;
            }
            boolean isNew = data.getBooleanExtra("IS_NEW", false);
            if (isNew) {
                backup.delete();
            } else {
                backup.save();
            }
            updateView();
        }

        if (requestCode == REQUEST_CODE_RUN_EMULATOR) {
            int id = TRS80Application.getCurrentConfiguration().getId();
            EmulatorState.saveScreenshot(id);
            EmulatorState.saveState(id);
        }
    }

    private void addConfiguration() {
        Configuration newConfig = Configuration.newConfiguration();
        editConfiguration(newConfig, true);
    }

    private void editConfiguration(Configuration conf, boolean isNew) {
        backup = conf.backup();
        Intent i = new Intent(this, EditConfigurationActivity.class);
        i.putExtra("CONFIG_ID", conf.getId());
        i.putExtra("IS_NEW", isNew);
        startActivityForResult(i, REQUEST_CODE_EDIT_CONFIG);
    }

    private void deleteConfiguration(final Configuration conf) {
        String msg = this.getString(R.string.alert_dialog_confirm_delete, conf.getName());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(msg);
        builder.setIcon(R.drawable.warning_icon);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                conf.delete();
                updateView();
            }

        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void stopEmulator(final Configuration conf) {
        String msg = this.getString(R.string.alert_dialog_confirm_stop_emu, conf.getName());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setMessage(msg);
        builder.setIcon(R.drawable.warning_icon);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                EmulatorState.deleteSavedState(conf.getId());
                updateView();
            }

        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void runEmulator(Configuration conf) {
        Hardware hardware;
        int model = conf.getModel();
        if (model != Hardware.MODEL1 && model != Hardware.MODEL3) {
            Toast.makeText(this, "Only Model I and Model III are supported at this time.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String romFile = null;
        switch (model) {
        case Hardware.MODEL1:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL1);
            hardware = new Model1(conf, romFile);
            break;
        case Hardware.MODEL3:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL3);
            hardware = new Model3(conf, romFile);
            break;
        case Hardware.MODEL4:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4);
            // TODO Change this to correct model when implemented
            hardware = null;
            break;
        case Hardware.MODEL4P:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4P);
            // TODO Change this to correct model when implemented
            hardware = null;
            break;
        default:
            hardware = null;
            break;
        }

        if (hardware == null) {
            Toast.makeText(this, "Model not supported.", Toast.LENGTH_LONG).show();
            return;
        }

        if (romFile == null || !new File(romFile).exists()) {
            Toast.makeText(this, "No valid ROM found. Please use Settings to set ROM.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        TRS80Application.setCurrentConfiguration(conf);
        TRS80Application.setHardware(hardware);
        int err = XTRS.init(hardware);
        if (err != 0) {
            showError(err);
            return;
        }
        EmulatorState.loadState(conf.getId());
        Intent i = new Intent(this, EmulatorActivity.class);
        startActivityForResult(i, REQUEST_CODE_RUN_EMULATOR);
    }

    private void showError(int err) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.initialization_error, null, false);
        TextView t = (TextView) view.findViewById(R.id.error_text);
        t.setText(this.getString(R.string.error_init, err));
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_EDIT_SETTINGS);
    }

    private void showHelp() {
        int titleId = R.string.help_title_configurations;
        int layoutId = R.layout.help_configurations;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(layoutId, null, false);
        TextView t = (TextView) view.findViewById(R.id.help_text);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void downloadROMs() {
        InitialSetupDialogFragment dialog = new InitialSetupDialogFragment();
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onDownloadCompleted() {
        downloadMenuItem.setVisible(false);
        updateView();
    }
}
