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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.base.Optional;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.puder.trs80.cast.CastMessageSender;
import org.puder.trs80.configuration.Configuration;
import org.puder.trs80.configuration.ConfigurationManager;
import org.puder.trs80.drag.ConfigurationItemTouchHelperCallback;
import org.puder.trs80.io.FileManager;
import org.puder.trs80.localstore.RomManager;
import org.retrostore.android.AppInstallListener;
import org.retrostore.android.RetrostoreActivity;
import org.retrostore.android.RetrostoreApi;
import org.retrostore.android.view.ImageLoader;
import org.retrostore.client.common.proto.App;
import org.retrostore.client.common.proto.SystemState;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity implements
        InitialSetupDialogFragment.DownloadCompletionListener, ConfigurationItemListener,
        NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";

    private static final String EXTRA_KEY_NEW_CONFIG_ID = "CONFIG_ID";
    private static final String EXTRA_KEY_IS_NEW = "IS_NEW";

    private static final int COLUMN_WIDTH_DP = 300;

    private static final int REQUEST_CODE_EDIT_CONFIG = 1;
    private static final int REQUEST_CODE_EDIT_SETTINGS = 2;

    // Action Menu
    private static final int MENU_OPTION_DOWNLOAD = 0;
    private static final int MENU_OPTION_ADD_CONFIGURATION = 1;
    private static final int MENU_OPTION_HELP = 2;

    private RecyclerView configurationListView;
    private ConfigurationListViewAdapter configurationListViewAdapter;
    private SharedPreferences sharedPrefs;
    private MenuItem downloadMenuItem = null;

    private ActionBarDrawerToggle toggle;
    private Configuration backupConfiguration;

    private ConfigurationManager configManager;
    private RomManager romManager;
    // Note: This is in the RetroStore package.
    private AppInstaller appInstaller;
    private Executor mInstallExecutor;
    private CastMessageSender castMessageSender;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // StrictMode.enableDefaults();
        super.onCreate(savedInstanceState);

        mInstallExecutor = Executors.newSingleThreadExecutor();
        RetrostoreApi.get().registerAppInstallListener(new AppInstallListener() {
            @Override
            public void onInstallApp(App app) {
                asyncDownloadAndInstallApp(app);
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.configuration, false);
        sharedPrefs = this.getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                Context.MODE_PRIVATE);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string
                .navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        FileManager.Creator fileManagerCreator = FileManager.Creator.get(getResources());
        try {
            configManager = ConfigurationManager.get(getApplicationContext());
            romManager = RomManager.init(fileManagerCreator);
        } catch (IOException e) {
            Log.e(TAG, "Cannot initialize RomManager / ConfigurationManager.", e);
            finish();
            // Cannot really launch the app if initialization fails.
            // TODO: Show an error message before exiting.
            return;
        }
        appInstaller = new AppInstaller(configManager, ImageLoader.get(getApplicationContext()),
                RetrostoreApi.get());
        castMessageSender = CastMessageSender.get();

        int screenWidthDp = this.getResources().getConfiguration().screenWidthDp;
        int numColumns =
                (screenWidthDp == android.content.res.Configuration.SCREEN_WIDTH_DP_UNDEFINED) ?
                        1 : screenWidthDp / COLUMN_WIDTH_DP;
        RecyclerView.LayoutManager lm = null;
        if (numColumns <= 1) {
            lm = new LinearLayoutManager(this);
        } else {
            lm = new GridLayoutManager(this, numColumns);
        }
        configurationListViewAdapter = new ConfigurationListViewAdapter(
                configManager, this, numColumns);
        configurationListView = (RecyclerView) this.findViewById(R.id.list_configurations);
        configurationListView.setLayoutManager(lm);
        configurationListView.setAdapter(configurationListViewAdapter);

        ItemTouchHelper.Callback callback = new ConfigurationItemTouchHelperCallback
                (configurationListViewAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(configurationListView);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
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

        if (!ranNewAssistant || (!romManager.hasAllRoms() && firstTime)) {
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
        EventBus.getDefault().unregister(this);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ScreenshotTakenEvent event) {
        int position = configManager.getPositionOfConfigWithId(event.configurationId);
        updateView(position, -1, -1);
    }

    private void updateView(int positionChanged, int positionInserted, int positionDeleted) {
        View withoutConfigurationsView = this.findViewById(R.id.without_configurations);
        View withConfigurationsView = this.findViewById(R.id.with_configurations);
        if (configManager.getConfigCount() == 0) {
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

        if (!romManager.hasAllRoms()) {
            downloadMenuItem = menu.add(Menu.NONE, MENU_OPTION_DOWNLOAD, Menu.NONE,
                    this.getString(R.string.menu_download));
            downloadMenuItem.setIcon(R.drawable.download_icon);
            MenuItemCompat.setShowAsAction(downloadMenuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }
        MenuItemCompat
                .setShowAsAction(
                        menu.add(Menu.NONE, MENU_OPTION_ADD_CONFIGURATION, Menu.NONE,
                                this.getString(R.string.menu_add_configuration)).setIcon(
                                R.drawable.add_icon), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItemCompat
                .setShowAsAction(
                        menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE,
                                this.getString(R.string.menu_help)).setIcon(
                                R.drawable.help_icon), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
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
            case MENU_OPTION_ADD_CONFIGURATION:
                addConfiguration();
                return true;
            case MENU_OPTION_HELP:
                showHelp();
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
        } else if (id == R.id.nav_share) {
            showShare();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onRetrostoreBtnClicked(View view) {

        Intent intent = new Intent(this, RetrostoreActivity.class);
        MainActivity.this.startActivity(intent);
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
        runEmulator(configuration, position);
    }

    @Override
    public void onConfigurationShare(Configuration configuration, int position) {
        shareEmulatorState(configuration, position);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_CONFIG) {
            if (data == null) {
                return;
            }
            boolean isNew = data.getBooleanExtra(EXTRA_KEY_IS_NEW, false);
            if (resultCode == Activity.RESULT_OK) {
                int id = data.getIntExtra(EXTRA_KEY_NEW_CONFIG_ID, -1);
                int position = configManager.getPositionOfConfigWithId(id);
                // Delete emulator state
                try {
                    configManager.getEmulatorState(id).deleteSavedState();
                } catch (IOException e) {
                    Log.e(TAG, "Could not delete emulator state.", e);
                }
                Optional<Configuration> conf = configManager.getConfigById(id);
                if (conf.isPresent()) {
                    conf.get().setCassettePosition(0f);
                }
                // Update UI
                if (isNew) {
                    updateView(-1, position, -1);
                } else {
                    updateView(position, -1, -1);
                }
                return;
            }
            if (backupConfiguration == null) {
                return;
            }
            if (isNew) {
                configManager.deleteConfigWithId(backupConfiguration.getId());
            } else {
                configManager.persistConfig(backupConfiguration);
            }
            backupConfiguration = null;
        }
    }

    private void addConfiguration() {
        Configuration currentConfiguration = configManager.newConfiguration();
        editConfiguration(currentConfiguration, true);
    }

    private void editConfiguration(Configuration conf, boolean isNew) {
        backupConfiguration = conf.createBackup();
        Intent i = new Intent(this, EditConfigurationActivity.class);
        i.putExtra(EXTRA_KEY_NEW_CONFIG_ID, conf.getId());
        i.putExtra(EXTRA_KEY_IS_NEW, isNew);
        startActivityForResult(i, REQUEST_CODE_EDIT_CONFIG);
    }

    private void deleteConfiguration(final Configuration conf, final int position) {
        String msg = this.getString(R.string.alert_dialog_confirm_delete, conf.getName().orNull());
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this, R.string.app_name,
                R.drawable.warning_icon, msg);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                AlertDialogUtil.dismissDialog(MainActivity.this);
                configManager.deleteConfigWithId(conf.getId());
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
        String msg = this.getString(R.string.alert_dialog_confirm_stop_emu, conf.getName().orNull
                ());
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this, R.string.app_name,
                R.drawable.warning_icon, msg);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                AlertDialogUtil.dismissDialog(MainActivity.this);
                try {
                    configManager.getEmulatorState(conf.getId()).deleteSavedState();
                } catch (IOException e) {
                    Log.e(TAG, "Could not delete emulator state.", e);
                }
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

    private void runEmulator(Configuration conf, int position) {
        Log.i(TAG, "RUN EMULATOR");
        View root = findViewById(R.id.main);

        int model = conf.getModel();
        if (model != Hardware.MODEL1 && model != Hardware.MODEL3) {
            Snackbar.make(root, R.string.error_model_not_supported, Snackbar.LENGTH_LONG).show();
            return;
        }


        if (!romManager.hasAllRoms()) {
            Snackbar.make(root, R.string.error_no_rom, Snackbar.LENGTH_LONG).show();
            return;
        }
        for (int i = 0; i < 4; i++) {
            if (!checkIfFileExists(conf.getDiskPath(i).orNull(), true, R.string.error_no_disk)) {
                return;
            }
        }

        Intent intent = new Intent(this, EmulatorActivity.class);
        intent.putExtra(EmulatorActivity.EXTRA_CONFIGURATION_ID, conf.getId());
        startActivity(intent);
    }

    private void shareEmulatorState(Configuration conf, int position) {
        Optional<SystemState> stateOpt = null;
        try {
            stateOpt = configManager.getEmulatorState(conf.getId()).getSystemState(conf.getModel());
            if (!stateOpt.isPresent()) {
                Log.e(TAG, "Cannot get xray system state.");
                showToast("Cannot get xray system state");
                return;
            }
        } catch (IOException e) {
            Log.e(TAG, "Cannot get emulator state.", e);
            showToast("Cannot get emulator state");
            return;
        }
        final SystemState state = stateOpt.get();

        mInstallExecutor.execute(() -> {
            Optional<Long> tokenOpt = RetrostoreApi.get().uploadSystemState(state);
            if (!tokenOpt.isPresent()) {
                Log.e(TAG, "Failed uploading state to RetroStore");
                // TODO: I18N.
                showToast("Failed uploading state to RetroStore");
                return;
            }
            showToast(StrUtil.form("System state uploaded. TOKEN: %d", tokenOpt.get()));
        });


    }

    private boolean checkIfFileExists(String path, boolean allowNull, int errMsg) {
        if (path == null && allowNull) {
            return true;
        }
        if (path == null || !new File(path).exists()) {
            View root = findViewById(R.id.main);
            Snackbar.make(root, errMsg, Snackbar.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void showSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class),
                REQUEST_CODE_EDIT_SETTINGS);
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
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string
                .community_url)));
        startActivity(browserIntent);
    }

    private void downloadROMs() {
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this,
                R.string.title_initial_setup, R.drawable.ic_info_black, R.string.initial_setup);
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

    private void showShare() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string
                .share_title)));
    }

    @Override
    public void onDownloadCompleted() {
        if (downloadMenuItem != null) {
            downloadMenuItem.setVisible(false);
        }
        updateView(-1, -1, -1);
    }

    public boolean showHint() {
        return showHint(R.string.hint_configuration_usage);
    }

    public boolean showHint(int id) {
        return AlertDialogUtil.showHint(this, id);
    }


    private void asyncDownloadAndInstallApp(final App app) {
        mInstallExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (appInstaller.downloadAndInstallApp(app)) {
                    String msg = getString(R.string.successfully_installed);
                    showToast(StrUtil.form(msg, app.getName()));
                }
            }
        });
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
