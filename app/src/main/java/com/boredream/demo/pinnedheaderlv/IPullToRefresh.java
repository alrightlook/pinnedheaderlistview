package com.boredream.demo.pinnedheaderlv;

import com.boredream.demo.pinnedheaderlv.PullToRefreshBase.OnRefreshListener;

import android.view.View;


public interface IPullToRefresh<T extends View> {
    
    public void setPullRefreshEnabled(boolean pullRefreshEnabled);
    
    public boolean isPullRefreshEnabled();
    
    public void setOnRefreshListener(OnRefreshListener refreshListener);
    
    public void onPullDownRefreshComplete();
    
    public T getRefreshableView();
    
    public LoadingLayout getHeaderLoadingLayout();
    
    public void setLastUpdatedLabel(CharSequence label);
}
