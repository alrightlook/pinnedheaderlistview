package com.boredream.demo.pinnedheaderlv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by shenjie3 on 14-12-18.
 */
public class RefreshHeaderLayout extends LoadingLayout {

    private ProgressBar mProgressBar;
    private ImageView mImageView_State;
    private TextView mTextView_State;
    private ViewGroup mHead;

    public RefreshHeaderLayout(Context context) {
        super(context);
        init();
    }

    public RefreshHeaderLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshHeaderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);
        mImageView_State = (ImageView) findViewById(R.id.iv_state);
        mTextView_State = (TextView) findViewById(R.id.tv_state);
        mHead = (ViewGroup) findViewById(R.id.pull_header_layout_out_container);
    }

    @Override
    public float getContentSize() {
        return getContext().getResources().getDimension(R.dimen.refresh_list_view_header_effect_height);
    }

    @Override
    public void pullSize(float size) {
        ViewGroup.LayoutParams params = mHead.getLayoutParams();
        params.height = (int)size;
        mHead.requestLayout();
        if(size > getContentSize()){
            mImageView_State.setImageResource(R.drawable.ic_menu_move_up);
            mTextView_State.setText("松开刷新");
            mProgressBar.setVisibility(View.GONE);
        }else{
            mImageView_State.setImageResource(R.drawable.ic_menu_move_down);
            mTextView_State.setText("下拉刷新");
            mProgressBar.setVisibility(View.GONE);
        }

    }



    @Override
    public void resetHeaderState() {
        ViewGroup.LayoutParams params = mHead.getLayoutParams();
        params.height = 0;
        mHead.setLayoutParams(params);
    }

    @Override
    protected View createLoadingView(Context context, AttributeSet attrs) {
        return LayoutInflater.from(context).inflate(R.layout.card_received_list_view_header, null);
    }
}
