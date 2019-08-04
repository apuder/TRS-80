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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.view.MenuItemCompat;

import com.google.android.material.snackbar.Snackbar;

import org.puder.trs80.AlertDialogUtil;
import org.puder.trs80.BaseActivity;
import org.puder.trs80.CreateDiskActivity;
import org.puder.trs80.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileBrowserActivity extends BaseActivity implements OnItemClickListener {

    // Action Menu
    private static final int       MENU_OPTION_EJECT  = 0;
    private static final int       MENU_OPTION_ADD    = 1;

    private static final int       MKDISK_REQUEST     = 0;

    private List<String>           items              = new ArrayList<String>();
    private String                 pathPrefix;
    private TextView               pathLabel;
    private String                 currentPath;
    private BrowserListViewAdapter fileListAdapter;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.file_browser);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pathPrefix = this.getString(R.string.path) + ": ";
        pathLabel = (TextView) this.findViewById(R.id.path);
        fileListAdapter = new BrowserListViewAdapter(this, items);
        ListView listView = (ListView) this.findViewById(R.id.file_list);
        listView.setAdapter(fileListAdapter);
        listView.setOnItemClickListener(this);

        Intent intent = getIntent();
        String startingDir = intent.getStringExtra("DIR");
        if (startingDir == null) {
            getFiles(Environment.getExternalStorageDirectory().getPath());
        } else {
            getFiles(startingDir);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_EJECT, Menu.NONE,
                        this.getString(R.string.menu_eject)).setIcon(R.drawable.eject_icon),
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        MenuItemCompat.setShowAsAction(
                menu.add(Menu.NONE, MENU_OPTION_ADD, Menu.NONE,
                        this.getString(R.string.menu_add)).setIcon(R.drawable.add_icon),
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(RESULT_CANCELED, getIntent());
            finish();
            return true;
        case MENU_OPTION_EJECT:
            eject();
            return true;
        case MENU_OPTION_ADD:
            createDisk();
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

    private void createDisk() {
        Intent intent = new Intent(this, CreateDiskActivity.class);
        intent.putExtra("DIR", currentPath);
        startActivityForResult(intent, MKDISK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MKDISK_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                View root = findViewById(android.R.id.content);
                Snackbar.make(root, getString(R.string.disk_image_created) + data.getStringExtra("PATH"),
                        Snackbar.LENGTH_SHORT).show();
                getFiles(currentPath);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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