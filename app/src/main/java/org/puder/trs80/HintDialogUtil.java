/*
 * Copyright 2012-2015, Arno Puder
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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class HintDialogUtil {

    static private Dialog   dialog   = null;
    static private Runnable runnable = null;


    static public void dismissHint() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
        runnable = null;
    }

    static public void showHint(Context context, int hintId) {
        showHint(context, hintId, null);
    }

    static public void showHint(Context context, int hintId, Runnable action) {
        dismissHint();
        String key = "conf_hint_id" + hintId;
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        if (!sharedPrefs.getBoolean(key, true)) {
            if (action != null) {
                action.run();
            }
            return;
        }
        runnable = action;
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, false);
        editor.commit();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.hint_title);
        builder.setMessage(hintId);
        builder.setPositiveButton(R.string.hint_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                dismissAlertDialog(d);
                if (runnable != null) {
                    runnable.run();
                    runnable = null;
                }
            }

        });

        dialog = builder.create();
        dialog.show();
    }

    static private void dismissAlertDialog(DialogInterface d) {
        d.dismiss();
        if (d == dialog) {
            dialog = null;
        }
    }
}
