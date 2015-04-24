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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

public class HintUtil {

    static private Dialog   dialog   = null;
    static private Runnable runnable = null;


    static public void dismissHint() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = null;
        runnable = null;
    }

    static public void showHint(Activity activity, int hintId) {
        showHint(activity, hintId, null);
    }

    static public void showHint(Activity activity, int hintId, Runnable action) {
        dismissHint();
        String key = "conf_hint_id" + hintId;
        SharedPreferences sharedPrefs = activity.getSharedPreferences(
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
        editor.apply();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
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


    final static public int SHOWCASE_CONFIG_MENU    = 0;
    final static public int SHOWCASE_CONFIG_REORDER = 1;
    final static public int SHOWCASE_CONFIG_START   = 2;
    final static public int SHOWCASE_EMU_PAUSE      = 3;
    final static public int SHOWCASE_EMU_MUTE       = 4;
    final static public int SHOWCASE_EMU_REWIND     = 5;


    interface ShowcaseListener {
        void showcaseShown(int scv);

        void showcaseWillBeShown();
    }


    static class Showcase {
        public int   resId;
        public int   type;
        public int   title;
        public int   description;
        public float scaleMultiplier;
    }


    final static private Showcase[] showcases;

    static {
        showcases = new Showcase[6];

        // SHOWCASE_CONFIG_MENU
        Showcase sc = new Showcase();
        sc.resId = R.id.configuration_menu;
        sc.title = R.string.scv_config_menu_title;
        sc.description = R.string.scv_config_menu_descr;
        sc.scaleMultiplier = 0.3f;
        showcases[SHOWCASE_CONFIG_MENU] = sc;

        // SHOWCASE_CONFIG_REARRANGE
        sc = new Showcase();
        sc.resId = R.id.configuration_reorder;
        sc.title = R.string.scv_config_reorder_title;
        sc.description = R.string.scv_config_reorder_descr;
        sc.scaleMultiplier = 0.3f;
        showcases[SHOWCASE_CONFIG_REORDER] = sc;

        // SHOWCASE_CONFIG_START
        sc = new Showcase();
        sc.resId = R.id.card_view;
        sc.title = R.string.scv_config_card_view_title;
        sc.description = R.string.scv_config_card_view_descr;
        sc.scaleMultiplier = 1;
        showcases[SHOWCASE_CONFIG_START] = sc;
    }


    static public void showCase(Activity activity, final int scv, final ShowcaseListener listener) {
        SharedPreferences sharedPrefs = activity.getSharedPreferences(
                SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String key = "conf_scv_id" + scv;
        /*
         * if (!sharedPrefs.getBoolean(key, true)) { // SCV has already been
         * shown earlier return true; }
         */

        Showcase sc = showcases[scv];
        View view = activity.findViewById(sc.resId);
        if (view == null) {
            // Target view does not exist yet, so can't show SCV
            return;
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, false);
        editor.apply();

        listener.showcaseWillBeShown();

        ViewTarget target = new ViewTarget(sc.resId, activity);
        ShowcaseView s = new ShowcaseView.Builder(activity, true).setTarget(target)
                .setStyle(R.style.ShowcaseTheme).setScaleMultiplier(sc.scaleMultiplier)
                .setContentTitle(sc.title).setContentText(sc.description).hideOnTouchOutside()
                .setShowcaseEventListener(new OnShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewHide(ShowcaseView showcaseView) {

                    }

                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        listener.showcaseShown(scv);
                    }

                    @Override
                    public void onShowcaseViewShow(ShowcaseView showcaseView) {

                    }
                }).build();

        if (activity.getResources().getBoolean(R.bool.align_showcase_button_left)) {
            // On small screens, align the OK button in bottom left corner
            RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lps.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lps.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            int margin = (int) activity.getResources().getDimension(
                    com.github.amlcurran.showcaseview.R.dimen.button_margin);
            lps.setMargins(margin, margin, margin, margin);
            s.setButtonPosition(lps);
        }
    }
}
