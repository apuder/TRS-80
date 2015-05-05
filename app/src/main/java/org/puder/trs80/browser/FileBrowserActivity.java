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

package org.puder.trs80.browser;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import org.puder.trs80.AlertDialogUtil;
import org.puder.trs80.BaseActivity;
import org.puder.trs80.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileBrowserActivity extends BaseActivity implements OnItemClickListener {

    // Action Menu
    private static final int       MENU_OPTION_CANCEL = 0;
    private static final int       MENU_OPTION_EJECT  = 1;

    private List<String>           items              = new ArrayList<String>();
    private String                 pathPrefix;
    private TextView               pathLabel;
    private String                 currentPath;
    private BrowserListViewAdapter fileListAdapter;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.file_browser);
        pathPrefix = this.getString(R.string.path) + ": ";
        pathLabel = (TextView) this.findViewById(R.id.path);
        fileListAdapter = new BrowserListViewAdapter(this, items);
        ListView listView = (ListView) this.findViewById(R.id.file_list);
        listView.setAdapter(fileListAdapter);
        listView.setOnItemClickListener(this);
        getFiles(Environment.getExternalStorageDirectory().getPath());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_EJECT, Menu.NONE,
                        this.getString(R.string.menu_eject)).setIcon(R.drawable.eject_icon),
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_CANCEL, Menu.NONE,
                        this.getString(R.string.menu_cancel)).setIcon(R.drawable.cancel_icon),
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_OPTION_CANCEL:
            setResult(RESULT_CANCELED, getIntent());
            finish();
            return true;
        case MENU_OPTION_EJECT:
            eject();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
        String file = items.get((int) id);
        if (file.equals("..")) {
            int idx = currentPath.lastIndexOf("/");
            if (idx == 0) {
                idx++;
            }
            getFiles(currentPath.substring(0, idx));
            return;
        }
        if (new File(file).isDirectory()) {
            getFiles(file);
            return;
        }
        Intent i = getIntent();
        i.putExtra("PATH", file);
        setResult(RESULT_OK, i);
        finish();
    }

    private void eject() {
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this, R.string.app_name,
                R.drawable.warning_icon, R.string.alert_dialog_confirm_eject);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                AlertDialogUtil.dismissDialog(FileBrowserActivity.this);
                Intent i = getIntent();
                i.putExtra("PATH", (String) null);
                setResult(RESULT_OK, i);
                finish();
            }

        });
        builder.setNegativeButton(R.string.alert_dialog_cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface d, int which) {
                        AlertDialogUtil.dismissDialog(FileBrowserActivity.this);
                    }

                });
        AlertDialogUtil.showDialog(this, builder);
    }

    private void getFiles(String path) {
        currentPath = path;
        pathLabel.setText(pathPrefix + currentPath);
        File[] files = new File(path).listFiles();
        items.clear();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    items.add(file.getPath());
                }
            }
            for (File file : files) {
                if (!file.isDirectory()) {
                    items.add(file.getPath());
                }
            }
        }
        Collections.sort(items);
        if (!path.equals("/")) {
            items.add(0, "..");
        }

        fileListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, getIntent());
        finish();
    }
}