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
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.puder.trs80.cast.CastMessageSender;
import org.puder.trs80.cast.RemoteCastScreen;
import org.puder.trs80.drag.ConfigurationItemTouchHelperCallback;

import java.io.File;

public class MainActivity extends BaseActivity implements
        InitialSetupDialogFragment.DownloadCompletionListener, ConfigurationItemListener,
        NavigationView.OnNavigationItemSelectedListener {

    private static final int     COLUMN_WIDTH_DP            = 300;

    private static final int     REQUEST_CODE_EDIT_CONFIG   = 1;
    private static final int     REQUEST_CODE_RUN_EMULATOR  = 2;
    private static final int     REQUEST_CODE_EDIT_SETTINGS = 3;

    // Action Menu
    private static final int     MENU_OPTION_DOWNLOAD       = 0;

    private RecyclerView         configurationListView;
    private ConfigurationListViewAdapter configurationListViewAdapter;
    private int                  currentConfigurationPosition;
    private SharedPreferences    sharedPrefs;
    private MenuItem             downloadMenuItem           = null;

    private ActionBarDrawerToggle toggle;

    private CastMessageSender    castMessageSender;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // StrictMode.enableDefaults();
        super.onCreate(savedInstanceState);
        sharedPrefs = this.getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                Context.MODE_PRIVATE);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        castMessageSender = CastMessageSender.get();

        int screenWidthDp = this.getResources().getConfiguration().screenWidthDp;
        int numColumns =
                (screenWidthDp == android.content.res.Configuration.SCREEN_WIDTH_DP_UNDEFINED) ?
                        1 : screenWidthDp / COLUMN_WIDTH_DP;
        RecyclerView.LayoutManager lm = null;
        boolean usesGridLayout;
        if (numColumns <= 1) {
            lm = new LinearLayoutManager(this);
            usesGridLayout = false;
        } else {
            lm = new GridLayoutManager(this, numColumns);
            usesGridLayout = true;
        }
        configurationListViewAdapter = new ConfigurationListViewAdapter(true /*usesGridLayout*/, this);
        configurationListView = (RecyclerView) this.findViewById(R.id.list_configurations);
        configurationListView.setLayoutManager(lm);
        configurationListView.setAdapter(configurationListViewAdapter);

        ItemTouchHelper.Callback callback = new ConfigurationItemTouchHelperCallback(configurationListViewAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(configurationListView);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
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
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        switch (item.getItemId()) {
        case MENU_OPTION_DOWNLOAD:
            downloadROMs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            showSettings();
        } else if (id == R.id.nav_rate) {
            showRating();
        } else if (id == R.id.nav_help) {
            showHelp();
        } else if (id == R.id.nav_community) {
            showCommunity();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onFloatingActionButtonClicked(View view) {
        addConfiguration();
    }

    @Override
    public void onConfigurationEdit(Configuration configuration, int position) {
        editConfiguration(configuration, false);
    }

    @Override
    public void onConfigurationDelete(Configuration configuration, int position) {
        deleteConfiguration(configuration, position);
    }

    @Override
    public void onConfigurationStop(Configuration configuration, int position) {
        stopEmulator(configuration, position);
    }

    @Override
    public void onConfigurationRun(Configuration configuration, int position) {
        runEmulator(configuration);
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
        Configuration currentConfiguration = Configuration.newConfiguration();
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

    private void showCommunity() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_plus_url)));
        startActivity(browserIntent);
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
}
