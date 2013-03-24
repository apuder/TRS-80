package org.puder.trs80;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Hardware hardware = new Model3(this);
        TRS80Application.setHardware(hardware);
        byte[] memBuffer = hardware.getMemoryBuffer();
        byte[] screenBuffer = hardware.getScreenBuffer();
        int entryAddr = hardware.getEntryAddress();
        XTRS.init(entryAddr, memBuffer, screenBuffer);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XTRS.cleanup();
    }

    public void doStartEmulator(View view) {
        Intent intent = new Intent(this, EmulatorActivity.class);
        startActivity(intent);
    }
}
