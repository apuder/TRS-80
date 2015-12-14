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

import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.runtime.transaction.SelectListTransaction;
import com.raizlabs.android.dbflow.runtime.transaction.TransactionListener;
import com.raizlabs.android.dbflow.runtime.transaction.TransactionListenerAdapter;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

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
    private AppListViewAdapter mFreeAppListAdapter;
    private AppListViewAdapter mGameAppListAdapter;
    private AppListViewAdapter mPaidAppListAdapter;

    private List<MarketApp> mMarketApps;

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

        MarketLoadUtil.loadMarket();

        Condition freeCondition = Condition.column(MarketApp$Table.PRICE).eq(0.0);
        fetchApps(freeCondition, new TransactionListenerAdapter<List<MarketApp>>() {
            @Override
            public void onResultReceived(List<MarketApp> marketList) {
                // retrieved here
                Log.d(LOG_TAG, "free result size:" + marketList.size());
                for (MarketApp app : marketList) {
                    Log.d(LOG_TAG, "app name:" + app.getName());
                }
                mFreeAppListAdapter = new AppListViewAdapter(MarketActivity.this, marketList);
                mFreeAppRecyclerView.setAdapter(MarketActivity.this.mFreeAppListAdapter);
            }
        });

        Condition paidCondition = Condition.column(MarketApp$Table.PRICE).greaterThan(0.0);
        fetchApps(paidCondition, new TransactionListenerAdapter<List<MarketApp>>() {
            @Override
            public void onResultReceived(List<MarketApp> marketList) {
                // retrieved here
                Log.d(LOG_TAG, "paid result size:" + marketList.size());
                for (MarketApp app : marketList) { Log.d(LOG_TAG, "app name:" + app.getName()); }
                mPaidAppListAdapter = new AppListViewAdapter(MarketActivity.this, marketList);
                mPaidAppRecyclerView.setAdapter(mPaidAppListAdapter);
            }
        });

/*        Condition gameCondition = Condition.column(MarketApp$Table.ATYPE).eq(MarketApp.TYPE_GAME);
        fetchApps(gameCondition, new TransactionListenerAdapter<List<MarketApp>>() {
            @Override
            public void onResultReceived(List<MarketApp> marketList) {
                // retrieved here
                Log.d(LOG_TAG, "game result size:" + marketList.size());
                for (MarketApp app : marketList) { Log.d(LOG_TAG, "app name:" + app.getName()); }
                mGameAppListAdapter = new AppListViewAdapter(MarketActivity.this, marketList);
                mGameAppRecyclerView.setAdapter(mGameAppListAdapter);
            }
        });*/

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

    private void fetchApps(Condition condition, TransactionListenerAdapter<List<MarketApp>> adapter) {
        // Async Transaction Queue Retrieval (Recommended)
        TransactionManager.getInstance().addTransaction(new SelectListTransaction<>(
                new Select()
                        .from(MarketApp.class)
                        .where(condition), adapter));
    }
}
