package com.boredream.demo.pinnedheaderlv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Adapter;
import android.widget.ListView;


/**
 * Created by shenjie3 on 14-12-18.
 */
public class PullToRefreshListView extends PullToRefreshBase<ListView> {
    public PullToRefreshListView(Context context) {
        this(context, null);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ListView createRefreshableView(Context context, AttributeSet attrs) {
  
        ListView listView = (ListView) LayoutInflater
                .from(context).inflate(R.layout.card_received_list_view, null);
        return listView;
    }

    @Override
    protected boolean isReadyForPullDown() {
        return isFirstItemVisible();
    }

    @Override
    protected LoadingLayout createHeaderLoadingLayout(Context context, AttributeSet attrs) {
        return new RefreshHeaderLayout(context,attrs);
    }

    private boolean isFirstItemVisible() {
        ListView listView = getRefreshableView();
        final Adapter adapter = listView.getAdapter();

        if (null == adapter || adapter.isEmpty()) {
            return true;
        }

        int mostTop = (listView.getChildCount() > 0) ?
                listView.getChildAt(0).getTop() : 0;
        if (mostTop >= 0) {
            return true;
        }

        return false;
    }
}
