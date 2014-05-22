package org.puder.trs80;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Demonstrates combining a TabHost with a ViewPager to implement a tab UI that
 * switches between tabs and also allows the user to perform horizontal flicks
 * to move between the tabs.
 */
public class MainFragment extends SherlockFragmentActivity {

    private static final int REQUEST_CODE_EDIT_SETTINGS = 0;

    TabHost                  mTabHost;
    ViewPager                mViewPager;
    TabsAdapter              mTabsAdapter;
    MenuItem                 downloadMenuItem           = null;
    SharedPreferences        sharedPrefs;
    Handler                  handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = this.getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                Context.MODE_PRIVATE);

        handler = new Handler();

        setContentView(R.layout.main_fragment);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        mTabsAdapter.addTab(mTabHost.newTabSpec("configurations").setIndicator("Configurations"),
                ConfigurationsFragment.class, null);
        mTabsAdapter.addTab(mTabHost.newTabSpec("emulator").setIndicator("Emulator"),
                EmulatorStatusFragment.class, null);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!sharedPrefs.getBoolean(SettingsActivity.CONF_FIRST_TIME, true)) {
            return;
        }
        Editor editor = sharedPrefs.edit();
        editor.putBoolean(SettingsActivity.CONF_FIRST_TIME, false);
        editor.commit();
        if (!hasROMs()) {
            doDownload();
        }
    }

    public boolean hasROMs() {
        return hasModel1ROM() && hasModel3ROM();
    }

    public boolean hasModel1ROM() {
        return sharedPrefs.getString(SettingsActivity.CONF_ROM_MODEL1, null) != null;
    }

    public boolean hasModel3ROM() {
        return sharedPrefs.getString(SettingsActivity.CONF_ROM_MODEL3, null) != null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!hasROMs()) {
            downloadMenuItem = menu.add(Menu.NONE, 1, Menu.CATEGORY_SYSTEM, "Download");
            downloadMenuItem.setIcon(R.drawable.download_icon).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        menu.add(Menu.NONE, 1, Menu.CATEGORY_SYSTEM, "Help").setIcon(R.drawable.help_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, 1, Menu.CATEGORY_SYSTEM, "Settings").setIcon(R.drawable.settings_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ("Download".equals(item.getTitle())) {
            doDownload();
            return true;
        }
        if ("Settings".equals(item.getTitle())) {
            doSettings();
            return true;
        }
        if ("Help".equals(item.getTitle())) {
            doHelp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_EDIT_SETTINGS) {
            /*
             * Came back from SettingsActivity. Check if download icon can be
             * disabled.
             */
            if (downloadMenuItem != null && hasROMs()) {
                downloadMenuItem.setVisible(false);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void romsDownloaded() {
        downloadMenuItem.setVisible(false);
        ConfigurationsFragment fragment = (ConfigurationsFragment) mTabsAdapter.getFragment(0);
        if (fragment != null) {
            fragment.updateView();
        }
    }

    private void doSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_EDIT_SETTINGS);
    }

    private void doDownload() {
//        InitialSetupDialogFragment dialog = InitialSetupDialogFragment.newInstance(this);
//        // dialog.setTargetFragment(this, 0);
//        dialog.show(getSupportFragmentManager(), "dialog");
    }

    private void doHelp() {
        int titleId = R.string.help_title_configurations;
        int layoutId = R.layout.help_configurations;
        if (mViewPager.getCurrentItem() != 0) {
            // We are on the Emulator tab
            titleId = R.string.help_title_emulator;
            layoutId = R.layout.help_emulator;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(titleId);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(layoutId, null, false);
        TextView t = (TextView) view.findViewById(R.id.help_text);
        t.setMovementMethod(LinkMovementMethod.getInstance());
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    public void setCurrentItem(int item) {
        mViewPager.setCurrentItem(item, true);
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter implements
            TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context                mContext;
        private final TabHost                mTabHost;
        private final ViewPager              mViewPager;
        private final ArrayList<TabInfo>     mTabs             = new ArrayList<TabInfo>();
        private final Map<Integer, Fragment> mPageReferenceMap = new HashMap<Integer, Fragment>();

        static final class TabInfo {
            private final String   tag;
            private final Class<?> clss;
            private final Bundle   args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            Fragment frag = Fragment.instantiate(mContext, info.clss.getName(), info.args);
            mPageReferenceMap.put(position, frag);
            return frag;
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
            super.destroyItem(container, position, object);
            mPageReferenceMap.remove(position);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        public Fragment getFragment(int position) {
            return mPageReferenceMap.get(position);
        }
    }
}
