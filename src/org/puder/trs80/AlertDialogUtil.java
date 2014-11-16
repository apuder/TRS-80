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
import android.content.Context;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AlertDialogUtil {

    public static Builder createAlertDialog(Context context, int titleId, int iconId,
            CharSequence message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(titleId);
        if (iconId != -1) {
            builder.setIcon(iconId);
        }
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.alert_dialog, null, false);
        TextView t = (TextView) view.findViewById(R.id.alert_text);
        SpannableString s = new SpannableString(message);
        t.setText(s);
        t.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setView(view);
        return builder;
    }

    public static Builder createAlertDialog(Context context, int titleId, int iconId, int messageId) {
        return createAlertDialog(context, titleId, iconId, context.getText(messageId));
    }
}
