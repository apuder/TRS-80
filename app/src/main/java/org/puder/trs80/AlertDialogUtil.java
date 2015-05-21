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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class AlertDialogUtil {

    static private Map<Context, Dialog> dialogs = new HashMap<>();


    public static Builder createAlertDialog(final Context context, int titleId, int iconId,
            CharSequence message) {
        dismissDialog(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        if (iconId != -1) {
            builder.setIcon(iconId);
        }
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.alert_dialog, null, false);
        TextView t = (TextView) view.findViewById(R.id.alert_text);
        final int size = (int) (t.getTextSize() * 1.2f);
        t.setText(Html.fromHtml(message.toString(), new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String source) {
                source = source.replace("/", "");
                int id = context.getResources()
                        .getIdentifier(source, "drawable", "org.puder.trs80");
                Drawable d = context.getResources().getDrawable(id);
                d.setBounds(0, 0, size, size);
                return d;
            }
        }, null));
        t.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setView(view);
        return builder;
    }

    public static void showDialog(Context context, Builder builder) {
        dismissDialog(context);
        AlertDialog dialog = builder.create();
        dialog.show();
        dialogs.put(context, dialog);
    }

    public static Builder createAlertDialog(Context context, int titleId, int iconId, int messageId) {
        return createAlertDialog(context, titleId, iconId, context.getText(messageId));
    }

    static public void dismissDialog(Context context) {
        if (dialogs.containsKey(context)) {
            dialogs.get(context).dismiss();
            dialogs.remove(context);
        }
    }

    static public boolean showHint(final Context context, int hintId, int buttonTitle,
            final Runnable buttonCallback) {
        String key = "conf_new_hint_id" + hintId;
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        if (!sharedPrefs.getBoolean(key, true)) {
            return false;
        }
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, false);
        editor.apply();

        AlertDialog.Builder builder = createAlertDialog(context, R.string.hint_title, -1, hintId);
        builder.setPositiveButton(R.string.hint_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface d, int which) {
                dismissDialog(context);
            }

        });

        if (buttonCallback != null) {
            builder.setNegativeButton(context.getText(buttonTitle),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismissDialog(context);
                            buttonCallback.run();
                        }
                    });
        }
        showDialog(context, builder);
        return true;
    }

    static public boolean showHint(Context context, int hintId) {
        return showHint(context, hintId, 0, null);
    }
}
