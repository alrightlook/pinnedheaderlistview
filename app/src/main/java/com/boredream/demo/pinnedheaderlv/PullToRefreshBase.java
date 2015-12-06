package com.boredream.demo.pinnedheaderlv;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.boredream.demo.pinnedheaderlv.ILoadingLayout.State;

public abstract class PullToRefreshBase<T extends View> extends LinearLayout
        implements IPullToRefresh<T> {
    private static final String TAG = PullToRefreshBase.class.getSimpleName();
    
    private static final int SCROLL_DURATION = 150;
    private static final float OFFSET_RADIO = 2.5f;   
    
    T mRefreshableView;
    private LoadingLayout mHeaderLayout;
    private ViewGroup mHeaderOutContainer;
    private int mHeaderHeight;
    private boolean mPullRefreshEnabled = true;
    private boolean mInterceptEventEnable = true;
    private boolean mIsHandledTouchEvent = false;
    private int mTouchSlop;
    private State mPullDownState = State.NONE;
    private SmoothScrollRunnable mSmoothScrollRunnable;
    private FrameLayout mRefreshableViewWrapper;
    private float mLastMotionY = -1;
    private OnRefreshListener mRefreshListener;
    
    public interface OnRefreshListener {
        void onPullDownToRefresh();
    }
    
    public PullToRefreshBase(Context context) {
        super(context);
        init(context, null);
    }

    public PullToRefreshBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }


	public PullToRefreshBase(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(LinearLayout.VERTICAL);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mHeaderLayout = createHeaderLoadingLayout(context, attrs);
        mHeaderOutContainer = (ViewGroup) mHeaderLayout.findViewById(R.id.pull_header_layout_out_container);
        mRefreshableView = createRefreshableView(context, attrs);
        if (null == mRefreshableView) {
            throw new NullPointerException("Refreshable view can not be null.");
        }

        addRefreshableView(context, mRefreshableView);
        addHeaderAndFooter(context);

        getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        refreshLoadingViewsSize();
                        getViewTreeObserver()
                                .removeGlobalOnLayoutListener(this);
                    }
                });
    }

    private void refreshLoadingViewsSize() {
        int headerHeight = (null != mHeaderLayout) ?
                (int)mHeaderLayout.getContentSize() : 0;
        if (headerHeight < 0) {
            headerHeight = 0;
        }

        mHeaderHeight = headerHeight;
    }

    @Override
    protected final void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        refreshLoadingViewsSize();
        refreshRefreshableViewSize(w, h);

        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    @Override
    public void setOrientation(int orientation) {
        if (LinearLayout.VERTICAL != orientation) {
            throw new IllegalArgumentException(
                    "This class only supports VERTICAL orientation.");
        }

        super.setOrientation(orientation);
    }

    @Override
    public final boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isInterceptTouchEventEnabled()) {
            return false;
        }

        if (!isPullRefreshEnabled()) {
            return false;
        }

        final int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            mIsHandledTouchEvent = false;
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsHandledTouchEvent) {
            return true;
        }

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mLastMotionY = event.getY();
            mIsHandledTouchEvent = false;
            break;

        case MotionEvent.ACTION_MOVE:
            final float deltaY = event.getY() - mLastMotionY;
            final float absDiff = Math.abs(deltaY);

            if (absDiff > mTouchSlop || isPullRefreshing() /*|| isPullLoading()*/) {
                mLastMotionY = event.getY();
                if (isPullRefreshEnabled() && isReadyForPullDown()) {
                    mIsHandledTouchEvent = (Math.abs(getScrollYValue()) > 0 || deltaY > 0.5f);
                    if (mIsHandledTouchEvent) {
                        mRefreshableView.onTouchEvent(event);
                    }
                }
            }
            break;

        default:
            break;
        }

        return mIsHandledTouchEvent;
    }

    @Override
    public final boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        switch (ev.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mLastMotionY = ev.getY();
            mIsHandledTouchEvent = false;
            break;

        case MotionEvent.ACTION_MOVE:
            final float deltaY = ev.getY() - mLastMotionY;
            if (isPullRefreshEnabled() && isReadyForPullDown()) {
            	preparePullDownRefresh();
                pullHeaderLayout(deltaY / OFFSET_RADIO);
                handled = true;
            } else {
                mIsHandledTouchEvent = false;
            }
            break;

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
            if (mIsHandledTouchEvent) {
                mIsHandledTouchEvent = false;
                if (isReadyForPullDown()) {
                    if (mPullRefreshEnabled
                            && (mPullDownState == State.RELEASE_TO_REFRESH)) {
                        startRefreshing();
                        handled = true;
                    }
                    Log.i(TAG, "onTouchEvent ACTION_UP: " + mPullDownState);
                    resetHeaderLayout();
                }
            }
            break;

        default:
            break;
        }

        return handled;
    }

    protected void preparePullDownRefresh() {
		
	}

	@Override
    public void setPullRefreshEnabled(boolean pullRefreshEnabled) {
        mPullRefreshEnabled = pullRefreshEnabled;
    }

    @Override
    public boolean isPullRefreshEnabled() {
        return mPullRefreshEnabled && (null != mHeaderLayout);
    }

    @Override
    public void setOnRefreshListener(OnRefreshListener refreshListener) {
        mRefreshListener = refreshListener;
    }

    @Override
    public void onPullDownRefreshComplete() {
        if (isPullRefreshing()) {
            mPullDownState = State.RESET;
            onStateChanged(State.RESET, true);

            postDelayed(new Runnable() {
                @Override
                public void run() {
                    setInterceptTouchEventEnabled(true);
                    mHeaderLayout.setState(State.RESET);
                }
            }, getSmoothScrollDuration());

            resetHeaderLayout();
            setInterceptTouchEventEnabled(false);
        }
    }

    @Override
    public T getRefreshableView() {
        return mRefreshableView;
    }

    @Override
    public LoadingLayout getHeaderLoadingLayout() {
        return mHeaderLayout;
    }

    @Override
    public void setLastUpdatedLabel(CharSequence label) {
        if (null != mHeaderLayout) {
            mHeaderLayout.setLastUpdatedLabel(label);
        }
    }

    public void doPullRefreshing(final boolean smoothScroll,
            final long delayMillis) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                int newScrollValue = -mHeaderHeight;
                int duration = smoothScroll ? SCROLL_DURATION : 0;

                startRefreshing();
                smoothScrollTo(newScrollValue, duration, 0);
            }
        }, delayMillis);
    }

    protected abstract T createRefreshableView(Context context,
            AttributeSet attrs);

    protected abstract boolean isReadyForPullDown();

    protected abstract LoadingLayout createHeaderLoadingLayout(
            Context context, AttributeSet attrs);

    protected long getSmoothScrollDuration() {
        return SCROLL_DURATION;
    }

    protected void refreshRefreshableViewSize(int width, int height) {
        if (null != mRefreshableViewWrapper) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mRefreshableViewWrapper
                    .getLayoutParams();
            if (lp.height != height) {
                lp.height = height;
                mRefreshableViewWrapper.requestLayout();
            }
        }
    }

    protected void addRefreshableView(Context context, T refreshableView) {
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.MATCH_PARENT;

        mRefreshableViewWrapper = new FrameLayout(context);
        mRefreshableViewWrapper.addView(refreshableView, width, height);

        height = 10;
        addView(mRefreshableViewWrapper, new LinearLayout.LayoutParams(width, height));
    }

    protected void addHeaderAndFooter(Context context) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        final LoadingLayout headerLayout = mHeaderLayout;
        if (null != headerLayout) {
            if (this == headerLayout.getParent()) {
                removeView(headerLayout);
            }

            addView(headerLayout, 0, params);
        }
    }

    protected void pullHeaderLayout(float delta) {
        if (delta < 0 ) {
            mHeaderLayout.pullSize(0);
            return;
        }
        mHeaderLayout.pullSize(Math.abs(delta));
        int headerSize = (int) Math.abs(delta);
        if (isPullRefreshEnabled() && !isPullRefreshing()) {
            if (headerSize > mHeaderHeight) {
                mPullDownState = State.RELEASE_TO_REFRESH;
            } else {
                mPullDownState = State.PULL_TO_REFRESH;
            }
            Log.i(TAG, "pullHeaderLayout State: " + mPullDownState);
            mHeaderLayout.setState(mPullDownState);
            onStateChanged(mPullDownState, true);
        }  
    }

    protected void resetHeaderLayout() {
        
//        mHeaderLayout.resetHeaderState();
        
        smoothScrollTo(0, SCROLL_DURATION, 0);
    }

    protected boolean isPullRefreshing() {
        return (mPullDownState == State.REFRESHING);
    }
    protected void startRefreshing() {
      if (null != mRefreshListener) {
          mRefreshListener.onPullDownToRefresh();
//      postDelayed(new Runnable() {
//          @Override
//          public void run() {
//              mRefreshListener.onPullDownToRefresh();
//          }
//          }, getSmoothScrollDuration());
      }
//        if (isPullRefreshing()) {
//            return;
//        }
//
//        mPullDownState = State.REFRESHING;
//        onStateChanged(State.REFRESHING, true);
//
//        if (null != mHeaderLayout) {
//            mHeaderLayout.setState(State.REFRESHING);
//        }
//
//        if (null != mRefreshListener) {
//            postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mRefreshListener.onPullDownToRefresh(PullToRefreshBase.this);
//                }
//            }, getSmoothScrollDuration());
//        }
    }

    protected void onStateChanged(State state, boolean isPullDown) {

    }

    private void setScrollTo(int x, int y) {
        scrollTo(x, y);
    }

    private int getScrollYValue() {
        return getScrollY();
    }

    private void smoothScrollTo(int newScrollValue, long duration,
            long delayMillis) {
        if (null != mSmoothScrollRunnable) {
            mSmoothScrollRunnable.stop();
        }

//        int oldScrollValue = this.getScrollYValue();
        int oldScrollValue = mHeaderOutContainer.getHeight();
        boolean post = (oldScrollValue != newScrollValue);
        if (post) {
            mSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue,
                    newScrollValue, duration);
        }

        if (post) {
            if (delayMillis > 0) {
                postDelayed(mSmoothScrollRunnable, delayMillis);
            } else {
                post(mSmoothScrollRunnable);
            }
        }
    }

    private void setInterceptTouchEventEnabled(boolean enabled) {
        mInterceptEventEnable = enabled;
    }

    private boolean isInterceptTouchEventEnabled() {
        return mInterceptEventEnable;
    }

    final class SmoothScrollRunnable implements Runnable {
        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;
        private final long mDuration;
        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;

        public SmoothScrollRunnable(int fromY, int toY, long duration) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mDuration = duration;
            mInterpolator = new DecelerateInterpolator();
        }

        @Override
        public void run() {
            if (mDuration <= 0) {
                setScrollTo(0, mScrollToY);
                return;
            }

            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {

                /**
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                final long oneSecond = 1000; // SUPPRESS CHECKSTYLE
                long normalizedTime = (oneSecond * (System.currentTimeMillis() - mStartTime))
                        / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, oneSecond),
                        0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY)
                        * mInterpolator.getInterpolation(normalizedTime
                                / (float) oneSecond));
                mCurrentY = mScrollFromY - deltaY;
                mHeaderLayout.pullSize(Math.abs(mCurrentY));
            }

            // If we're not at the target Y, keep going...
            if (mContinueRunning && mScrollToY != mCurrentY) {
                PullToRefreshBase.this.postDelayed(this, 16);// SUPPRESS
            } else if (mContinueRunning) {
                mHeaderLayout.pullSize(Math.abs(mScrollToY));
            }
        }

        public void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }
}
