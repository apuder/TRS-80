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

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class EditConfigurationActivity extends BaseActivity {

    // Action Menu
    private static final int   MENU_OPTION_CANCEL      = 0;
    private static final int   MENU_OPTION_HELP        = 1;

    private EditConfigurationFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dummy view. Will be replaced by EditConfigurationFragment.
        setContentView(new View(this));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragment = new EditConfigurationFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_CANCEL, Menu.NONE,
                        this.getString(R.string.menu_cancel)).setIcon(R.drawable.cancel_icon),
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
        case android.R.id.home:
            doneEditing(!fragment.configurationWasEdited());
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
        doneEditing(!fragment.configurationWasEdited());
    }

    private void doneEditing(boolean cancel) {
        setResult(cancel ? RESULT_CANCELED : RESULT_OK, getIntent());
        finish();
    }

    private void doHelp() {
        showDialog(R.string.help_title_edit_configuration, -1, R.string.help_edit_configuration);
    }
}
