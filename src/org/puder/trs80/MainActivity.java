package org.puder.trs80;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {

    private boolean startingEmulator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Hardware hardware = new Model3(this);
        TRS80Application.setHardware(hardware);
        Log.d("TRS80", "MainActivity.onCreate()");
    }

    @Override
    public void onResume() {
        super.onResume();
        startingEmulator = false;
    }

    public void doStartEmulator(View view) {
        startingEmulator = true;
        Intent intent = new Intent(this, EmulatorActivity.class);
        startActivity(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        Z80ExecutionThread threadZ80 = TRS80Application.getZ80Thread();
        if (!startingEmulator && threadZ80 != null) {
            Log.d("TRS80", "MainActivity: killing threadZ80");
            boolean retry = true;
            threadZ80.setRunning(false);
            while (retry) {
                try {
                    threadZ80.join();
                    retry = false;
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
