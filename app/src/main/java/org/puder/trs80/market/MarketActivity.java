package org.puder.trs80.market;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import org.puder.trs80.BaseActivity;
import org.puder.trs80.R;

import java.util.List;

public class MarketActivity extends BaseActivity {

    private static final String LOG_TAG = MarketActivity.class.getSimpleName();
    private RecyclerView mFreeAppRecyclerView;
    private RecyclerView mGameAppRecyclerView;
    private RecyclerView mPaidAppRecyclerView;
    private LinearLayoutManager mFreeAppLayoutManager;
    private LinearLayoutManager mPaidAppLayoutManager;
    private LinearLayoutManager mGameAppLayoutManager;
    private AppListViewAdapter mFreeAppListAdapter;
    private AppListViewAdapter mGameAppListAdapter;
    private AppListViewAdapter mPaidAppListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.market);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_TITLE);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle(R.string.market);

        mFreeAppRecyclerView = (RecyclerView)findViewById(R.id.free_list);
        mFreeAppRecyclerView.setHasFixedSize(false);
        mFreeAppLayoutManager = new LinearLayoutManager(this);
        mFreeAppLayoutManager.setOrientation(LinearLayout.HORIZONTAL);
        mFreeAppRecyclerView.setLayoutManager(mFreeAppLayoutManager);

        mPaidAppRecyclerView = (RecyclerView)findViewById(R.id.paid_list);
        mPaidAppRecyclerView.setHasFixedSize(false);
        mPaidAppLayoutManager = new LinearLayoutManager(this);
        mPaidAppLayoutManager.setOrientation(LinearLayout.HORIZONTAL);
        mPaidAppRecyclerView.setLayoutManager(mPaidAppLayoutManager);

        mGameAppRecyclerView = (RecyclerView)findViewById(R.id.games_list);
        mGameAppRecyclerView.setHasFixedSize(false);
        mGameAppLayoutManager = new LinearLayoutManager(this);
        mGameAppLayoutManager.setOrientation(LinearLayout.HORIZONTAL);
        mGameAppRecyclerView.setLayoutManager(mGameAppLayoutManager);

        DBHelper helper = new DBHelper(this);

        helper.loadMarket();

        List<MarketApp> marketList = helper.getFreeApps();
        mFreeAppListAdapter = new AppListViewAdapter(MarketActivity.this, marketList);
        mFreeAppRecyclerView.setAdapter(MarketActivity.this.mFreeAppListAdapter);

        marketList = helper.getPaidApps();
        mPaidAppListAdapter = new AppListViewAdapter(MarketActivity.this, marketList);
        mPaidAppRecyclerView.setAdapter(mPaidAppListAdapter);

        marketList = helper.getGames();
        mGameAppListAdapter = new AppListViewAdapter(MarketActivity.this, marketList);
        mGameAppRecyclerView.setAdapter(mGameAppListAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.market, menu);
        getMenuInflater().inflate(R.menu.common, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_search:
                Log.d(this.getClass().getSimpleName(), "action search");
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
