package org.puder.trs80;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

public class MainActivity extends Activity implements OnItemClickListener, OnItemLongClickListener {

    private SharedPreferences globalPrefs;
    private String            configurations;
    private String[]          configurationIds;
    private String[]          configurationNames;
    private Dialog            dialog;
    private String            currentId;
    private Bitmap            screenshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        configurations = globalPrefs.getString("CONFIGURATIONS", "");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XTRS.cleanup();
    }

    private void updateView() {
        if (configurations.equals("")) {
            setContentView(R.layout.no_configurations);
            return;
        }
        setContentView(R.layout.main_activity);
        updateConfigurationNames();
        if (screenshot != null) {
            ImageView img = (ImageView) findViewById(R.id.screenshot);
            img.setImageBitmap(screenshot);
        }
        ListView list = (ListView) this.findViewById(R.id.list_configurations);
        list.setAdapter(new ArrayAdapter<String>(this, R.layout.configuration_item,
                configurationNames));
        list.setLongClickable(true);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
    }

    private void updateConfigurationNames() {
        configurationIds = configurations.split(",");
        configurationNames = new String[configurationIds.length];
        for (int i = 0; i < configurationIds.length; i++) {
            SharedPreferences config = this.getSharedPreferences("CONFIG_" + configurationIds[i],
                    MODE_PRIVATE);
            configurationNames[i] = config.getString("conf_key_name", "unknown");
        }
    }

    public void doStartEmulator(View view) {
        Intent intent = new Intent(this, EmulatorActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add_configuration:
            addConfiguration();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void addConfiguration() {
        int nextId = globalPrefs.getInt("NEXT_ID", 0);
        nextId++;
        Editor e = globalPrefs.edit();
        e.putInt("NEXT_ID", nextId);
        if (!configurations.equals("")) {
            configurations += ",";
        }
        configurations += "" + nextId;
        e.putString("CONFIGURATIONS", configurations);
        e.commit();
        editConfiguration(Integer.toString(nextId));
    }

    private void editConfiguration(String id) {
        Intent i = new Intent(this, ConfigurationActivity.class);
        i.putExtra("CONFIG_ID", id);
        startActivity(i);
    }

    private void startConfiguration(String id) {
        Configuration conf = new Configuration();
        TRS80Application.setCurrentConfiguration(conf);
        Hardware hardware = new Model3(this);
        TRS80Application.setHardware(hardware);
        byte[] memBuffer = hardware.getMemoryBuffer();
        byte[] screenBuffer = hardware.getScreenBuffer();
        int entryAddr = hardware.getEntryAddress();
        XTRS.init(conf.getModel().getModelValue(), entryAddr, memBuffer, screenBuffer);
        Intent i = new Intent(this, EmulatorActivity.class);
        i.putExtra("CONFIG_ID", id);
        startActivityForResult(i, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            screenshot = TRS80Application.getScreenshot();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startConfiguration(configurationIds[position]);
    }

    public void doEdit(View view) {
        dialog.dismiss();
        editConfiguration(currentId);
    }

    public void doStart(View view) {
        dialog.dismiss();
        startConfiguration(currentId);
    }

    public void doDelete(View view) {
        dialog.dismiss();
        // TODO delete
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        currentId = configurationIds[position];
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        dialog = new Dialog(this);
        dialog.setContentView(inflater.inflate(R.layout.configuration_popup, null, false));
        dialog.setTitle("Choose action:");
        dialog.show();

        return true;
    }
}
