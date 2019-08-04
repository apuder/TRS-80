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

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    public void onStop() {
        super.onStop();
        AlertDialogUtil.dismissDialog(this);
    }

    protected void showDialog(int titleId, int iconId, String message, String buttonText,
            final Runnable buttonCallback) {
        AlertDialog.Builder builder = AlertDialogUtil.createAlertDialog(this, titleId, iconId,
                message);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                AlertDialogUtil.dismissDialog(BaseActivity.this);
            }

        });

        if (buttonCallback != null) {
            builder.setNegativeButton(buttonText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlertDialogUtil.dismissDialog(BaseActivity.this);
                    buttonCallback.run();
                }
            });
        }
        AlertDialogUtil.showDialog(this, builder);
    }

    protected void showDialog(int titleId, int iconId, String message) {
        showDialog(titleId, iconId, message, null, null);
    }

    protected void showDialog(int titleId, int iconId, int messageId) {
        showDialog(titleId, iconId, getString(messageId));
    }
}
