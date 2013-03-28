package org.puder.trs80;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends Activity {

    private ImageView screenshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        screenshot = (ImageView) findViewById(R.id.screenshot);
        Configuration conf = new Configuration();
        TRS80Application.setCurrentConfiguration(conf);
        Hardware hardware = new Model3(this);
        TRS80Application.setHardware(hardware);
        byte[] memBuffer = hardware.getMemoryBuffer();
        byte[] screenBuffer = hardware.getScreenBuffer();
        int entryAddr = hardware.getEntryAddress();
        XTRS.init(conf.getModel().getModelValue(), entryAddr, memBuffer, screenBuffer);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XTRS.cleanup();
    }

    public void doStartEmulator(View view) {
        Intent intent = new Intent(this, EmulatorActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap b = TRS80Application.getScreenshot();
            if (b != null) {
                screenshot.setImageBitmap(b);
            }
        }
    }

}
