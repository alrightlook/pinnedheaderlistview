package com.boredream.demo.pinnedheaderlv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
/**
 * 
 * TODO 
 * @ClassName: PinnedSectionListView  
 * @author ChenYuanming
 * @date 2015年1月5日 下午3:20:38 
 * @version V1.0
 */
public class PinnedSectionListView extends PullToRefreshListView {
	String Tag = getClass().getSimpleName();

	private Context context;
	private PinnedSectionLvAdapter adapter;
	private View currentPinnedHeader;
	private int mTranslateY;
	private boolean isPinnedHeaderShown;

	public PinnedSectionListView(Context context) {
		super(context);
		this.context = context;
		initView();
	}

	public PinnedSectionListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		initView();
	}

	public PinnedSectionListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		initView();
	}

	private void initView() {
		isPinnedHeaderShown = false;
		adapter = new PinnedSectionLvAdapter(context);
		listView = getRefreshableView();
		listView.setAdapter(adapter);
		listView.setOnScrollListener(mOnScrollListener);
	}

	/**
	 * 显示顶部悬浮框
	 */
	private synchronized void createPinnedHeader(int position) {

		View pinnedView = (TextView) adapter.getPinnedSectionView(position);

		LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) pinnedView.getLayoutParams();
		if (layoutParams == null) {
			layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		}

		int heightMode = MeasureSpec.getMode(layoutParams.height);
		int heightSize = MeasureSpec.getSize(layoutParams.height);

		if (heightMode == MeasureSpec.UNSPECIFIED)
			heightMode = MeasureSpec.EXACTLY;

		int maxHeight = getHeight() - listView.getListPaddingTop() - listView.getListPaddingBottom();
		if (heightSize > maxHeight)
			heightSize = maxHeight;

		int ws = MeasureSpec.makeMeasureSpec(getWidth() - listView.getListPaddingLeft() - listView.getListPaddingRight(), MeasureSpec.EXACTLY);
		int hs = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
		pinnedView.measure(ws, hs);
		pinnedView.layout(0, 0, pinnedView.getMeasuredWidth(), pinnedView.getMeasuredHeight());

		currentPinnedHeader = pinnedView;
	}

	private String lastGroupName = "";
	private OnScrollListener mOnScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
 				createPinnedHeader(view.getFirstVisiblePosition());
				Log.d(Tag, "stop at "+view.getFirstVisiblePosition());
				invalidate();
			}
		}

		/**
		 * 滚动时动态监测是否需要利用新顶部悬浮框顶替当前顶部悬浮框
		 */
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			// 屏幕中可以看到的顶部第一条
			MyData myData = adapter.getItem(firstVisibleItem);
			// 屏幕中可以看到的顶部第二条
			MyData nextData = adapter.getItem(firstVisibleItem + 1);

			// 对比第一二条数据
			if (!myData.firstLetter.equals(nextData.firstLetter)) {
				// 不同时即出现两个悬浮框互相顶替效果,
				// 则需要动态获取y轴偏移量,让顶部悬浮框在y轴上根据偏移量显示
				// 从而最终造成一种被顶出去顶回来(顶来顶去可欢乐了)滴效果
				View childView = view.getChildAt(0);
				if (childView != null) {
					mTranslateY = childView.getTop();
					createPinnedHeader(firstVisibleItem); // 创建当前显示的悬浮框
					postInvalidate();
					System.out.println("ding ... " + mTranslateY);
				}
			} else {
				if ((currentPinnedHeader != null && isPinnedHeaderShown)) {
					if (!myData.firstLetter.equals(lastGroupName)) {
						createPinnedHeader(firstVisibleItem);
						Log.d(Tag, "create    merge " + firstVisibleItem);
					} else {
						Log.d(Tag, "recycle  " + firstVisibleItem);
					}
					mTranslateY = 0;
				} else {
					if (!isPullRefreshing()) {
						createPinnedHeader(firstVisibleItem);
						Log.d(Tag, "create " + firstVisibleItem);
					}
				}
			}
			lastGroupName = myData.firstLetter;
		}

	};
	private ListView listView;

	@Override
	protected void preparePullDownRefresh() {
		super.preparePullDownRefresh();
		currentPinnedHeader = null;
		invalidate();
	}

	/**
	 * 核心方法 绘制顶部悬浮框
	 */
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		if (currentPinnedHeader != null) {
			View pinnedView = currentPinnedHeader;

			int pLeft = listView.getListPaddingLeft();
			int pTop = listView.getListPaddingTop();

			canvas.save();
			canvas.clipRect(pLeft, pTop, pLeft + pinnedView.getWidth(), pTop + pinnedView.getHeight());
			canvas.translate(pLeft, pTop + mTranslateY);
			drawChild(canvas, pinnedView, getDrawingTime());
			canvas.restore();

			isPinnedHeaderShown = true;
		}
	}

	/**
	 * listview适配器,设置特殊item(本例中为蓝色背景的首字母栏)
	 */
	public class PinnedSectionLvAdapter extends BaseAdapter {

		private Context context;
		private ArrayList<MyData> datas;
		public Map<String, Integer> maps;

		public PinnedSectionLvAdapter(Context context) {
			this.context = context;
			this.datas = MyData.getData();
			sortLetter(datas);
		}

		public PinnedSectionLvAdapter(Context context, ArrayList<MyData> datas) {
			this.context = context;
			this.datas = datas;
			sortLetter(datas);
		}

		/**
		 * 获取需要顶部悬浮显示的view
		 */
		public View getPinnedSectionView(int position) {
			ViewGroup view = (ViewGroup) getView(position, null, PinnedSectionListView.this);
			View vAlpha = view.getChildAt(0);
			return vAlpha;
		}

		@Override
		public int getCount() {
			return datas.size();
		}

		@Override
		public MyData getItem(int position) {
			return datas.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;
			ViewHolder holder;
			if (convertView == null) {
				view = View.inflate(context, R.layout.alpha_item, null);
				holder = new ViewHolder();
				holder.tvAlpha = (TextView) view.findViewById(R.id.alphaitem_tv_alpha);
				holder.tvContent = (TextView) view.findViewById(R.id.alphaitem_tv_content);
				view.setTag(holder);
			}else{
				view = convertView;
				holder = (ViewHolder) view.getTag();
			}
				

			MyData myData = getItem(position);

			holder.tvAlpha.setText(myData.firstLetter);
			if (maps.get(myData.firstLetter) == position) {
				holder.tvAlpha.setVisibility(View.VISIBLE);
			} else {
				holder.tvAlpha.setVisibility(View.GONE);
			}
			holder.tvAlpha.setTag(position);

			holder.tvContent.setText(datas.get(position).data);

			return view;
		}

		private void sortLetter(ArrayList<MyData> datas) {
			Collections.sort(datas, new Comparator<MyData>() {
				@Override
				public int compare(MyData lhs, MyData rhs) {
					return lhs.firstLetter.compareTo(rhs.firstLetter);
				}
			});

			maps = new HashMap<String, Integer>();
			for (int i = 0; i < datas.size(); i++) {
				if (!maps.containsKey(datas.get(i).firstLetter)) {
					maps.put(datas.get(i).firstLetter, i);
				}
			}
		}
	}

	class PinnedHeader {
		public View view;
		public int position;

		@Override
		public String toString() {
			return "PinnedHeader [view=" + view + ", position=" + position + "]";
		}
	}

	class ViewHolder{
		TextView tvAlpha,tvContent;
	}
}

/**
 * 模拟数据
 */
class MyData {
	public String firstLetter; // 数据对应首字母
	public String data; // 具体数据

	@Override
	public String toString() {
		return "MyData [firstLetter=" + firstLetter + ", data=" + data + "]";
	}

	public static ArrayList<MyData> getData() {
		ArrayList<MyData> datas = new ArrayList<MyData>();
		for (int i = 0; i < 10; i++) {
			MyData data = new MyData();
			data.firstLetter = "a";
			data.data = "a" + i;
			datas.add(data);
		}
		for (int i = 0; i < 10; i++) {
			MyData data = new MyData();
			data.firstLetter = "e";
			data.data = "e" + i;
			datas.add(data);
		}
		for (int i = 0; i < 20; i++) {
			MyData data = new MyData();
			data.firstLetter = "b";
			data.data = "b" + i;
			datas.add(data);
		}
		for (int i = 0; i < 10; i++) {
			MyData data = new MyData();
			data.firstLetter = "w";
			data.data = "w" + i;
			datas.add(data);
		}
		return datas;
	}

}
