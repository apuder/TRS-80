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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ConfigurationsFragment extends SherlockFragment implements OnItemClickListener {

    private static final int    REQUEST_CODE_EDIT_CONFIG  = 1;
    private static final int    REQUEST_CODE_RUN_EMULATOR = 2;

    private Configuration[]     configurations;
    private String[]            configurationNames;
    private ConfigurationBackup backup;
    private int                 selectedPosition;
    private ListView            configurationListView;
    private ActionMode          actionMode;

    private final class ConfigurationActionMode implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add("Play").setIcon(R.drawable.play_icon)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add("Edit").setIcon(R.drawable.edit_icon)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            menu.add("Delete").setIcon(R.drawable.delete_icon)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if ("Play".equals(item.getTitle())) {
                doStart();
                return true;
            }
            if ("Edit".equals(item.getTitle())) {
                doEdit();
                return true;
            }
            if ("Delete".equals(item.getTitle())) {
                doDelete();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            unselect();
            actionMode = null;
        }

        private void unselect() {
            configurationListView.clearChoices();
            // Needed because of a bug in Android
            configurationListView.requestLayout();
        }
    }

    public static class AlertDialogFragment extends SherlockDialogFragment {

        private ConfigurationsFragment configFrag;

        public static AlertDialogFragment newInstance(ConfigurationsFragment configFrag,
                String title) {
            AlertDialogFragment frag = new AlertDialogFragment();
            frag.configFrag = configFrag;
            Bundle args = new Bundle();
            args.putString("title", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            return new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.warning_icon)
                    .setTitle(title)
                    .setPositiveButton(R.string.alert_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    configFrag.userConfirmedDeleteConfiguration();
                                }
                            })
                    .setNegativeButton(R.string.alert_dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            }).create();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.configurations, container, false);
    }

    public void updateView() {
        View withoutConfigurationsView = getView().findViewById(R.id.without_configurations);
        View withConfigurationsView = getView().findViewById(R.id.with_configurations);
        configurations = Configuration.getConfigurations();
        if (configurations.length == 0) {
            withoutConfigurationsView.setVisibility(View.VISIBLE);
            withConfigurationsView.setVisibility(View.GONE);
            return;
        }

        withoutConfigurationsView.setVisibility(View.GONE);
        withConfigurationsView.setVisibility(View.VISIBLE);

        configurationNames = new String[configurations.length];
        for (int i = 0; i < configurations.length; i++) {
            configurationNames[i] = configurations[i].getName();
        }

        configurationListView = (ListView) getView().findViewById(R.id.list_configurations);
        configurationListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        int textViewResId = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? android.R.layout.simple_list_item_activated_1
                : android.R.layout.simple_list_item_checked;
        configurationListView.setAdapter(new ArrayAdapter<String>(getActivity(), textViewResId,
                configurationNames));
        configurationListView.setOnItemClickListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add("Add").setIcon(R.drawable.add_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        CharSequence title = item.getTitle();
        if ("Add".equals(title)) {
            addConfiguration();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            ((MainFragment) getActivity()).setCurrentItem(1);
        }
    }

    private void addConfiguration() {
        Configuration newConfig = Configuration.newConfiguration();
        editConfiguration(newConfig, true);
    }

    private void userConfirmedDeleteConfiguration() {
        configurations[selectedPosition].delete();
        actionMode.finish();
        updateView();
    }

    private void editConfiguration(Configuration conf, boolean isNew) {
        backup = conf.backup();
        Intent i = new Intent(getActivity(), EditConfigurationActivity.class);
        i.putExtra("CONFIG_ID", conf.getId());
        i.putExtra("IS_NEW", isNew);
        startActivityForResult(i, REQUEST_CODE_EDIT_CONFIG);
    }

    private void startConfiguration(final Configuration conf) {
        Configuration currentConf = TRS80Application.getCurrentConfiguration();
        if (currentConf == null) {
            userConfirmedStartConfiguration(conf);
            return;
        }
        String msg = getActivity().getString(R.string.alert_dialog_confirm_launch_new_config,
                currentConf.getName());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.app_name);
        builder.setMessage(msg);
        builder.setIcon(R.drawable.warning_icon);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                userConfirmedStartConfiguration(conf);
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

    private void userConfirmedStartConfiguration(Configuration conf) {
        int model = conf.getModel();
        if (model != Hardware.MODEL1 && model != Hardware.MODEL3) {
            Toast.makeText(getActivity(), "Only Model 1 and Model 3 are supported at this time.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String romFile = null;
        switch (model) {
        case Hardware.MODEL1:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL1);
            break;
        case Hardware.MODEL3:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL3);
            break;
        case Hardware.MODEL4:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4);
            break;
        case Hardware.MODEL4P:
            romFile = SettingsActivity.getSetting(SettingsActivity.CONF_ROM_MODEL4P);
            break;
        default:
            break;
        }
        if (romFile == null || !new File(romFile).exists()) {
            Toast.makeText(getActivity(), "No valid ROM found. Please use Settings to set ROM.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Hardware hardware = new Model3(conf, romFile);
        TRS80Application.setCurrentConfiguration(conf);
        TRS80Application.setHardware(hardware);
        int err = XTRS.init(hardware);
        if (err != 0) {
            showError(err);
            return;
        }
        Intent i = new Intent(getActivity(), EmulatorActivity.class);
        startActivityForResult(i, REQUEST_CODE_RUN_EMULATOR);
    }

    private void showError(int err) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.app_name);
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.initialization_error, null, false);
        TextView t = (TextView) view.findViewById(R.id.error_text);
        t.setText(this.getString(R.string.error_init, err));
        builder.setView(view);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectedPosition = position;
        if (actionMode == null) {
            actionMode = getSherlockActivity().startActionMode(new ConfigurationActionMode());
        }
    }

    private void doEdit() {
        actionMode.finish();
        editConfiguration(configurations[selectedPosition], false);
    }

    private void doDelete() {
        String title = getActivity().getString(R.string.alert_dialog_confirm_delete,
                configurationNames[selectedPosition]);
        AlertDialogFragment dialog = AlertDialogFragment.newInstance(this, title);
        dialog.setTargetFragment(this, 0);
        dialog.show(getActivity().getSupportFragmentManager(), "dialog");
    }

    private void doStart() {
        actionMode.finish();
        startConfiguration(configurations[selectedPosition]);
    }
}
