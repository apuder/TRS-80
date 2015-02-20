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
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class SettingsActivity extends ActionBarActivity {

    public static final String SHARED_PREF_NAME       = "Settings";

    // Action Menu
    private static final int   MENU_OPTION_DONE       = 0;
    private static final int   MENU_OPTION_HELP       = 1;

    public static final String CONF_FIRST_TIME        = "conf_first_time";
    public static final String CONF_RAN_NEW_ASSISTANT = "conf_ran_new_assistant";
    public static final String CONF_ROM_MODEL1        = "conf_rom_model1";
    public static final String CONF_ROM_MODEL3        = "conf_rom_model3";
    public static final String CONF_ROM_MODEL4        = "conf_rom_model4";
    public static final String CONF_ROM_MODEL4P       = "conf_rom_model4p";

    private AlertDialog        dialog                 = null;

    public static String getSetting(String key) {
        SharedPreferences prefs = TRS80Application.getAppContext().getSharedPreferences(
                SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dummy view. Will be replaced by SettingsFragment.
        setContentView(new View(this));
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItemCompat
                .setShowAsAction(
                        menu.add(Menu.NONE, MENU_OPTION_DONE, Menu.NONE,
                                this.getString(R.string.menu_done)).setIcon(R.drawable.ok_icon),
                        MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        MenuItemCompat
                .setShowAsAction(
                        menu.add(Menu.NONE, MENU_OPTION_HELP, Menu.NONE,
                                this.getString(R.string.menu_help)).setIcon(R.drawable.help_icon),
                        MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_OPTION_DONE:
            doDone();
            return true;
        case MENU_OPTION_HELP:
            doHelp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        doDone();
    }

    private void doDone() {
        setResult(Activity.RESULT_OK, getIntent());
        finish();
    }

    private void doHelp() {
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this,
                R.string.help_title_settings, -1, R.string.help_settings);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                dialog.dismiss();
                dialog = null;
            }

        });

        dialog = builder.create();
        dialog.show();
    }
}
