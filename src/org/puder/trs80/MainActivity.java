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

import org.puder.trs80.cast.CastMessageSender;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteButton;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity
		implements
			OnItemClickListener,
			InitialSetupDialogFragment.DownloadCompletionListener {

	private static final int REQUEST_CODE_EDIT_CONFIG = 1;
	private static final int REQUEST_CODE_RUN_EMULATOR = 2;
	private static final int REQUEST_CODE_EDIT_SETTINGS = 3;

	// Action Menu
	private static final int MENU_OPTION_DOWNLOAD = 0;
	private static final int MENU_OPTION_ADD = 1;
	private static final int MENU_OPTION_HELP = 2;
	private static final int MENU_OPTION_SETTINGS = 3;

	// Context Menu
	private static final int MENU_OPTION_START = 0;
	private static final int MENU_OPTION_RESUME = 1;
	private static final int MENU_OPTION_STOP = 2;
	private static final int MENU_OPTION_EDIT = 3;
	private static final int MENU_OPTION_DELETE = 4;
	private static final int MENU_OPTION_UP = 5;
	private static final int MENU_OPTION_DOWN = 6;

	private List<Configuration> configurations;
	private ConfigurationBackup backup;
	private ListView configurationListView;
	private SharedPreferences sharedPrefs;
	private MenuItem downloadMenuItem = null;

	private CastMessageSender castMessageSender;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sharedPrefs = this.getSharedPreferences(
				SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
		this.setContentView(R.layout.main_activity);
		castMessageSender = CastMessageSender.get();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateView();
        castMessageSender.start();
        AudioHttpServer.get().start();

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

	@Override
	protected void onPause() {
	    if (isFinishing()) {
	        castMessageSender.stop();
	        AudioHttpServer.get().stop();
	    }
		super.onPause();
	}

	public void updateView() {
		View withoutConfigurationsView = this
				.findViewById(R.id.without_configurations);
		View withConfigurationsView = this
				.findViewById(R.id.with_configurations);
		configurations = Configuration.getConfigurations();
		if (configurations.size() == 0) {
			withoutConfigurationsView.setVisibility(View.VISIBLE);
			withConfigurationsView.setVisibility(View.GONE);
			return;
		}

		withoutConfigurationsView.setVisibility(View.GONE);
		withConfigurationsView.setVisibility(View.VISIBLE);

		configurationListView = (ListView) this
				.findViewById(R.id.list_configurations);
		ConfigurationListViewAdapter confList = new ConfigurationListViewAdapter(
				this, configurations);
		configurationListView.setAdapter(confList);
		configurationListView.setOnItemClickListener(this);
		registerForContextMenu(configurationListView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.main, menu);
	    MenuItem mediaRouteItem = menu.findItem(R.id.action_cast);
	    MediaRouteButton mediaRouteButton = (MediaRouteButton) mediaRouteItem
	        .getActionView();
	    mediaRouteButton.setRouteSelector(castMessageSender.getRouteSelector());

		if (!ROMs.hasROMs()) {
			downloadMenuItem = menu.add(Menu.NONE, MENU_OPTION_DOWNLOAD,
					Menu.NONE, this.getString(R.string.menu_download));
			downloadMenuItem.setIcon(R.drawable.download_icon);
			MenuItemCompat.setShowAsAction(downloadMenuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}


		MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, MENU_OPTION_ADD, Menu.NONE,
				this.getString(R.string.menu_add)).setIcon(R.drawable.add_icon),
				MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, MENU_OPTION_SETTINGS, Menu.NONE,
				this.getString(R.string.menu_settings))
				.setIcon(R.drawable.settings_icon), MenuItemCompat.SHOW_AS_ACTION_NEVER);
		MenuItemCompat.setShowAsAction(menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE,
				this.getString(R.string.menu_help))
				.setIcon(R.drawable.help_icon), MenuItemCompat.SHOW_AS_ACTION_NEVER);
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
	    castMessageSender.sendScreenUpdate("Hello Chromecast!!!");
		runEmulator(configurations.get(position));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_OPTION_DOWNLOAD :
				downloadROMs();
				return true;
			case MENU_OPTION_ADD :
				addConfiguration();
				return true;
			case MENU_OPTION_HELP :
				showHelp();
				return true;
			case MENU_OPTION_SETTINGS :
				showSettings();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Configuration conf = configurations.get((int) info.id);
		menu.setHeaderTitle(conf.getName());
		if (EmulatorState.hasSavedState(conf.getId())) {
			menu.add(Menu.NONE, MENU_OPTION_RESUME, Menu.NONE,
					this.getString(R.string.menu_resume));
			menu.add(Menu.NONE, MENU_OPTION_STOP, Menu.NONE,
					this.getString(R.string.menu_stop));
		} else {
			menu.add(Menu.NONE, MENU_OPTION_START, Menu.NONE,
					this.getString(R.string.menu_start));
		}
		menu.add(Menu.NONE, MENU_OPTION_EDIT, Menu.NONE,
				this.getString(R.string.menu_edit));
		menu.add(Menu.NONE, MENU_OPTION_DELETE, Menu.NONE,
				this.getString(R.string.menu_delete));
		if (Configuration.getConfigurations().size() > 1) {
			if (!conf.isFirst()) {
				menu.add(Menu.NONE, MENU_OPTION_UP, Menu.NONE,
						this.getString(R.string.menu_up));
			}
			if (!conf.isLast()) {
				menu.add(Menu.NONE, MENU_OPTION_DOWN, Menu.NONE,
						this.getString(R.string.menu_down));
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
			case MENU_OPTION_START :
				EmulatorState.deleteSavedState(confID);
				runEmulator(conf);
				break;
			case MENU_OPTION_RESUME :
				runEmulator(conf);
				break;
			case MENU_OPTION_STOP :
				stopEmulator(conf);
				break;
			case MENU_OPTION_EDIT :
				editConfiguration(conf, false);
				break;
			case MENU_OPTION_DELETE :
				deleteConfiguration(conf);
				break;
			case MENU_OPTION_UP :
				conf.moveUp();
				updateView();
				break;
			case MENU_OPTION_DOWN :
				conf.moveDown();
				updateView();
				break;
			default :
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
			if (TRS80Application.hasCrashed()) {
				finish();
				return;
			}
			Configuration conf = TRS80Application.getCurrentConfiguration();
			int id = conf.getId();
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
		String msg = this.getString(R.string.alert_dialog_confirm_delete,
				conf.getName());
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.app_name);
		builder.setMessage(msg);
		builder.setIcon(R.drawable.warning_icon);
		builder.setPositiveButton(R.string.alert_dialog_ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						conf.delete();
						updateView();
					}

				});
		builder.setNegativeButton(R.string.alert_dialog_cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}

				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void stopEmulator(final Configuration conf) {
		String msg = this.getString(R.string.alert_dialog_confirm_stop_emu,
				conf.getName());
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.app_name);
		builder.setMessage(msg);
		builder.setIcon(R.drawable.warning_icon);
		builder.setPositiveButton(R.string.alert_dialog_ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						EmulatorState.deleteSavedState(conf.getId());
						updateView();
					}

				});
		builder.setNegativeButton(R.string.alert_dialog_cancel,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}

				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void runEmulator(Configuration conf) {
		Hardware hardware = null;
		int model = conf.getModel();

		String romFile = null;
		switch (model) {
			case Hardware.MODEL1 :
				romFile = SettingsActivity
						.getSetting(SettingsActivity.CONF_ROM_MODEL1);
				hardware = new Model1(conf, romFile);
				break;
			case Hardware.MODEL3 :
				romFile = SettingsActivity
						.getSetting(SettingsActivity.CONF_ROM_MODEL3);
				hardware = new Model3(conf, romFile);
				break;
			case Hardware.MODEL4 :
				romFile = SettingsActivity
						.getSetting(SettingsActivity.CONF_ROM_MODEL4);
				// TODO Change this to correct model when implemented
				hardware = null;
				break;
			case Hardware.MODEL4P :
				romFile = SettingsActivity
						.getSetting(SettingsActivity.CONF_ROM_MODEL4P);
				// TODO Change this to correct model when implemented
				hardware = null;
				break;
			default :
				hardware = null;
				break;
		}

		if (hardware == null) {
			Toast.makeText(this, R.string.error_model_not_supported,
					Toast.LENGTH_LONG).show();
			return;
		}

		if (romFile == null || !new File(romFile).exists()) {
			Toast.makeText(this, R.string.error_no_rom, Toast.LENGTH_LONG)
					.show();
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
		View view = inflater
				.inflate(R.layout.initialization_error, null, false);
		TextView t = (TextView) view.findViewById(R.id.error_text);
		t.setText(this.getString(R.string.error_init, err));
		builder.setView(view);
		builder.setPositiveButton(R.string.alert_dialog_ok,
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}

				});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void showSettings() {
		startActivityForResult(new Intent(this, SettingsActivity.class),
				REQUEST_CODE_EDIT_SETTINGS);
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
		builder.setPositiveButton(R.string.alert_dialog_cancel,
				new DialogInterface.OnClickListener() {

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
