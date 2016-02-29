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
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.puder.trs80.cast.CastMessageSender;
import org.puder.trs80.cast.RemoteCastScreen;
import org.puder.trs80.helper.OnStartDragListener;
import org.puder.trs80.helper.SimpleItemTouchHelperCallback;

import java.io.File;

public class MainActivity extends BaseActivity implements
        InitialSetupDialogFragment.DownloadCompletionListener, ConfigurationMenuListener,
        PopupMenu.OnMenuItemClickListener, PopupMenu.OnDismissListener, OnStartDragListener {

    private static final int     REQUEST_CODE_EDIT_CONFIG   = 1;
    private static final int     REQUEST_CODE_RUN_EMULATOR  = 2;
    private static final int     REQUEST_CODE_EDIT_SETTINGS = 3;

    // Action Menu
    private static final int     MENU_OPTION_DOWNLOAD       = 0;
    private static final int     MENU_OPTION_HELP           = 1;
    private static final int     MENU_OPTION_SETTINGS       = 2;
    private static final int     MENU_OPTION_RATE           = 3;

    // Context Menu
    private static final int     MENU_OPTION_START          = 0;
    private static final int     MENU_OPTION_RESUME         = 1;
    private static final int     MENU_OPTION_STOP           = 2;
    private static final int     MENU_OPTION_EDIT           = 3;
    private static final int     MENU_OPTION_DELETE         = 4;

    private RecyclerView         configurationListView;
    private ConfigurationListViewAdapter configurationListViewAdapter;
    private Configuration        currentConfiguration;
    private int                  currentConfigurationPosition;
    private SharedPreferences    sharedPrefs;
    private MenuItem             downloadMenuItem           = null;
    private PopupMenu            popup                      = null;

    private CastMessageSender    castMessageSender;


    private ItemTouchHelper mItemTouchHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // StrictMode.enableDefaults();
        super.onCreate(savedInstanceState);
        sharedPrefs = this.getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                Context.MODE_PRIVATE);
        this.setContentView(R.layout.main_activity);

        castMessageSender = CastMessageSender.get();
        configurationListViewAdapter = new ConfigurationListViewAdapter(this, this);
        configurationListView = (RecyclerView) this.findViewById(R.id.list_configurations);
        configurationListView.setLayoutManager(new LinearLayoutManager(this));
        configurationListView.setAdapter(configurationListViewAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(configurationListViewAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(configurationListView);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView(-1, -1, -1);
        castMessageSender.start();
        // TODO: Enable once fully supported.
        // AudioHttpServer.get().start();

        boolean firstTime = sharedPrefs.getBoolean(SettingsActivity.CONF_FIRST_TIME, true);
        boolean ranNewAssistant = sharedPrefs.getBoolean(SettingsActivity.CONF_RAN_NEW_ASSISTANT,
                false);

        Editor editor = sharedPrefs.edit();
        editor.putBoolean(SettingsActivity.CONF_FIRST_TIME, false);
        editor.putBoolean(SettingsActivity.CONF_RAN_NEW_ASSISTANT, true);
        editor.apply();

        if (!ranNewAssistant || (!ROMs.hasROMs() && firstTime)) {
            downloadROMs();
        }
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            castMessageSender.stop();
            // TODO: Enable once fully supported.
            // AudioHttpServer.get().stop();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (popup != null) {
            popup.dismiss();
            popup = null;
        }
    }

    private void updateView(int positionChanged, int positionInserted, int positionDeleted) {
        View withoutConfigurationsView = this.findViewById(R.id.without_configurations);
        View withConfigurationsView = this.findViewById(R.id.with_configurations);
        if (Configuration.getCount() == 0) {
            withoutConfigurationsView.setVisibility(View.VISIBLE);
            withConfigurationsView.setVisibility(View.GONE);
            return;
        }

        withoutConfigurationsView.setVisibility(View.GONE);
        withConfigurationsView.setVisibility(View.VISIBLE);

        if (positionChanged != -1) {
            configurationListViewAdapter.notifyItemChanged(positionChanged);
        } else if (positionInserted != -1) {
            configurationListViewAdapter.notifyItemInserted(positionInserted);
            configurationListView.getLayoutManager().scrollToPosition(positionInserted);
        } else if (positionDeleted != -1) {
            configurationListViewAdapter.notifyItemRemoved(positionDeleted);
        } else {
            configurationListViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteItem = menu.findItem(R.id.action_cast_trs80);
        MediaRouteButton mediaRouteButton = (MediaRouteButton) MenuItemCompat
                .getActionView(mediaRouteItem);
        mediaRouteButton.setRouteSelector(castMessageSender.getRouteSelector());

        if (!ROMs.hasROMs()) {
            downloadMenuItem = menu.add(Menu.NONE, MENU_OPTION_DOWNLOAD, Menu.NONE,
                    this.getString(R.string.menu_download));
            downloadMenuItem.setIcon(R.drawable.download_icon);
            MenuItemCompat.setShowAsAction(downloadMenuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }

        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_SETTINGS, Menu.NONE,
                        this.getString(R.string.menu_settings)).setIcon(R.drawable.settings_icon),
                MenuItemCompat.SHOW_AS_ACTION_NEVER);
        MenuItemCompat
                .setShowAsAction(
                        menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE,
                                this.getString(R.string.menu_help)).setIcon(R.drawable.help_icon),
                        MenuItemCompat.SHOW_AS_ACTION_NEVER);
        MenuItemCompat
                .setShowAsAction(
                        menu.add(Menu.NONE, MENU_OPTION_RATE, Menu.NONE,
                                this.getString(R.string.menu_rate)),
                        MenuItemCompat.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_OPTION_DOWNLOAD:
            downloadROMs();
            return true;
        case MENU_OPTION_HELP:
            showHelp();
            return true;
        case MENU_OPTION_SETTINGS:
            showSettings();
            return true;
        case MENU_OPTION_RATE:
            showRating();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onFloatingActionButtonClicked(View view) {
        addConfiguration();
    }

    @Override
    public void onConfigurationSelected(Configuration configuration, int position) {
        runEmulator(configuration);
    }

    @Override
    public void onConfigurationMenuClicked(View anchor, Configuration configuration, int position) {
        currentConfiguration = configuration;
        currentConfigurationPosition = position;
        popup = new PopupMenu(this, anchor);
        popup.setOnMenuItemClickListener(this);
        popup.setOnDismissListener(this);
        Menu menu = popup.getMenu();
        if (EmulatorState.hasSavedState(configuration.getId())) {
            menu.add(Menu.NONE, MENU_OPTION_RESUME, Menu.NONE, this.getString(R.string.menu_resume));
            menu.add(Menu.NONE, MENU_OPTION_STOP, Menu.NONE, this.getString(R.string.menu_stop));
        } else {
            menu.add(Menu.NONE, MENU_OPTION_START, Menu.NONE, this.getString(R.string.menu_start));
        }
        menu.add(Menu.NONE, MENU_OPTION_EDIT, Menu.NONE, this.getString(R.string.menu_edit));
        menu.add(Menu.NONE, MENU_OPTION_DELETE, Menu.NONE, this.getString(R.string.menu_delete));
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_OPTION_START:
            EmulatorState.deleteSavedState(currentConfiguration.getId());
            runEmulator(currentConfiguration);
            return true;
        case MENU_OPTION_RESUME:
            runEmulator(currentConfiguration);
            return true;
        case MENU_OPTION_STOP:
            stopEmulator(currentConfiguration, currentConfigurationPosition);
            return true;
        case MENU_OPTION_EDIT:
            editConfiguration(currentConfiguration, false);
            return true;
        case MENU_OPTION_DELETE:
            deleteConfiguration(currentConfiguration, currentConfigurationPosition);
            return true;
        }
        return false;
    }

    @Override
    public void onDismiss(PopupMenu popupMenu) {
        if (popup == popupMenu) {
            popup = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDIT_CONFIG) {
            if (data == null) {
                return;
            }
            boolean isNew = data.getBooleanExtra("IS_NEW", false);
            if (resultCode == Activity.RESULT_OK) {
                if (isNew) {
                    updateView(-1, currentConfigurationPosition, -1);
                } else {
                    updateView(currentConfigurationPosition, -1, -1);
                }
                return;
            }
            ConfigurationBackup backup = ConfigurationBackup.retrieveBackup();
            if (backup == null) {
                return;
            }
            if (isNew) {
                backup.delete();
            } else {
                backup.save();
            }
        }

        if (requestCode == REQUEST_CODE_RUN_EMULATOR) {
            if (TRS80Application.hasCrashed()) {
                crashAlert();
                finish();
                return;
            }
            Configuration conf = TRS80Application.getCurrentConfiguration();

            if (conf == null) {
                // If the application was killed/crashed in the meantime there
                // is not configuration.
                crashAlert();
                return;
            }
            int id = conf.getId();
            EmulatorState.saveScreenshot(id);
            EmulatorState.saveState(id);
        }
    }

    private void addConfiguration() {
        currentConfiguration = Configuration.newConfiguration();
        currentConfigurationPosition = Configuration.getCount();
        editConfiguration(currentConfiguration, true);
    }

    private void editConfiguration(Configuration conf, boolean isNew) {
        conf.backup();
        Intent i = new Intent(this, EditConfigurationActivity.class);
        i.putExtra("CONFIG_ID", conf.getId());
        i.putExtra("IS_NEW", isNew);
        startActivityForResult(i, REQUEST_CODE_EDIT_CONFIG);
    }

    private void deleteConfiguration(final Configuration conf, final int position) {
        String msg = this.getString(R.string.alert_dialog_confirm_delete, conf.getName());
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this, R.string.app_name,
                R.drawable.warning_icon, msg);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                AlertDialogUtil.dismissDialog(MainActivity.this);
                conf.delete();
                updateView(-1, -1, position);
            }

        });
        builder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface d, int which) {
                        AlertDialogUtil.dismissDialog(MainActivity.this);
                    }

                });

        AlertDialogUtil.showDialog(this, builder);
    }

    private void stopEmulator(final Configuration conf, final int position) {
        String msg = this.getString(R.string.alert_dialog_confirm_stop_emu, conf.getName());
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this, R.string.app_name,
                R.drawable.warning_icon, msg);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                AlertDialogUtil.dismissDialog(MainActivity.this);
                EmulatorState.deleteSavedState(conf.getId());
                conf.setCassettePosition(0);
                updateView(position, -1, -1);
            }

        });
        builder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface d, int which) {
                        AlertDialogUtil.dismissDialog(MainActivity.this);
                    }

                });

        AlertDialogUtil.showDialog(this, builder);
    }

    private void runEmulator(Configuration conf) {
        Hardware hardware = null;
        int model = conf.getModel();

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

        View root = findViewById(R.id.main);
        if (hardware == null) {
            Snackbar.make(root, R.string.error_model_not_supported, Snackbar.LENGTH_LONG).show();
            return;
        }

        if (romFile == null || !new File(romFile).exists()) {
            Snackbar.make(root, R.string.error_no_rom, Snackbar.LENGTH_LONG).show();
            return;
        }

        TRS80Application.setCurrentConfiguration(conf);
        TRS80Application.setHardware(hardware);
        int err = XTRS.init(hardware);
        if (err != 0) {
            showError(err);
            return;
        }
        RemoteCastScreen.get().sendConfiguration(conf);
        EmulatorState.loadState(conf.getId());
        Intent i = new Intent(this, EmulatorActivity.class);
        startActivityForResult(i, REQUEST_CODE_RUN_EMULATOR);
    }

    private void showError(int err) {
        showDialog(R.string.app_name, -1, this.getString(R.string.error_init, err));
    }

    private void showSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_EDIT_SETTINGS);
    }

    private void crashAlert() {
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this, R.string.app_name,
                R.drawable.warning_icon, R.string.alert_dialog_inform_crash);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                AlertDialogUtil.dismissDialog(MainActivity.this);
            }

        });

        AlertDialogUtil.showDialog(this, builder);
    }

    private void showHelp() {
        showDialog(R.string.help_title_configurations, -1, R.string.help_configurations);
    }

    private void showRating() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent playMarketIntent = new Intent(Intent.ACTION_VIEW, uri);
        playMarketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(playMarketIntent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void downloadROMs() {
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this,
                R.string.title_initial_setup, R.drawable.warning_icon, R.string.initial_setup);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int whichButton) {
                AlertDialogUtil.dismissDialog(MainActivity.this);
                InitialSetupDialogFragment prog = new InitialSetupDialogFragment();
                prog.show(getSupportFragmentManager(), "dialog");
            }
        }).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int whichButton) {
                AlertDialogUtil.dismissDialog(MainActivity.this);
            }
        });

        AlertDialogUtil.showDialog(this, builder);
    }

    @Override
    public void onDownloadCompleted() {
        if (downloadMenuItem != null) {
            downloadMenuItem.setVisible(false);
        }
        updateView(-1, -1, -1);
        AlertDialogUtil.showHint(MainActivity.this, R.string.hint_configuration_usage);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }
}
