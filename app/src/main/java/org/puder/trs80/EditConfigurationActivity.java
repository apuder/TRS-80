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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class EditConfigurationActivity extends ActionBarActivity {

    public static final String CONF_NAME               = "conf_name";
    public static final String CONF_MODEL              = "conf_model";
    public static final String CONF_CASSETTE           = "conf_cassette";
    public static final String CONF_DISK1              = "conf_disk1";
    public static final String CONF_DISK2              = "conf_disk2";
    public static final String CONF_DISK3              = "conf_disk3";
    public static final String CONF_DISK4              = "conf_disk4";
    public static final String CONF_CHARACTER_COLOR    = "conf_character_color";
    public static final String CONF_KEYBOARD_PORTRAIT  = "conf_keyboard_portrait";
    public static final String CONF_KEYBOARD_LANDSCAPE = "conf_keyboard_landscape";
    public static final String CONF_MUTE_SOUND         = "conf_mute_sound";

    // Action Menu
    private static final int   MENU_OPTION_DONE        = 0;
    private static final int   MENU_OPTION_CANCEL      = 1;
    private static final int   MENU_OPTION_HELP        = 2;

    private AlertDialog        dialog                  = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dummy view. Will be replaced by EditConfigurationFragment.
        setContentView(new View(this));
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new EditConfigurationFragment()).commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_CANCEL, Menu.NONE,
                        this.getString(R.string.menu_cancel)).setIcon(R.drawable.cancel_icon),
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
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
            doneEditing(false);
            return true;
        case MENU_OPTION_CANCEL:
            doneEditing(true);
            return true;
        case MENU_OPTION_HELP:
            doHelp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        doneEditing(false);
    }

    private void doneEditing(boolean cancel) {
        setResult(cancel ? RESULT_CANCELED : RESULT_OK, getIntent());
        finish();
    }

    private void doHelp() {
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this,
                R.string.help_title_edit_configuration, -1, R.string.help_edit_configuration);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                dismissAlertDialog(d);
            }

        });

        dialog = builder.create();
        dialog.show();
    }

    private void dismissAlertDialog(DialogInterface d) {
        d.dismiss();
        if (d == dialog) {
            dialog = null;
        }
    }
}
