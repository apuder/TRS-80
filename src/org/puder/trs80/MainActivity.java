package org.puder.trs80;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;

public class MainActivity extends SherlockFragmentActivity implements OnItemClickListener {

    private static final int    REQUEST_CODE_EDIT_CONFIG  = 1;
    private static final int    REQUEST_CODE_RUN_EMULATOR = 2;

    private static final int    MENU_OPTION_EDIT          = 0;
    private static final int    MENU_OPTION_DELETE        = 1;

    private List<Configuration> configurations;
    private ConfigurationBackup backup;
    private int                 selectedPosition;
    private ListView            configurationListView;
    private ActionMode          actionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main_activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
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
        menu.add("Add").setIcon(R.drawable.add_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        CharSequence title = item.getTitle();
        if ("Add".equals(title)) {
            addConfiguration();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Configuration");
        menu.add(0, MENU_OPTION_EDIT, 0, "Edit");
        menu.add(0, MENU_OPTION_DELETE, 0, "Delete");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        switch (item.getItemId()) {
        case MENU_OPTION_EDIT:
            editConfiguration(configurations.get(info.position), false);
            break;
        case MENU_OPTION_DELETE:
            deleteConfiguration(configurations.get(info.position));
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
}
