package cn.fanfan.homepage;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnLastItemVisibleListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestParams;

import cn.fanfan.common.Config;
import cn.fanfan.common.NetworkState;
import cn.fanfan.main.MainActivity;
import cn.fanfan.main.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class HomePageFragment extends Fragment {
	public static final String TAG = "HomePageFragment";
	private TextView tvHomePageLoading;
	private List<HomePageItemModel> itemDataList = new ArrayList<HomePageItemModel>();
	private HomePageAdapter adapter;
	private Bundle bundle;
	private int mPage = 0;
	private int totalRow;
	private PullToRefreshListView mPullRefreshListView;
	private Boolean isFirstEnter = true;
	boolean mIsUp;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View fragmentView;
		fragmentView = inflater.inflate(R.layout.fragment_homepage, container,
				false);
		tvHomePageLoading = (TextView) fragmentView
				.findViewById(R.id.tvHomePageLoading);
		getHomePageInfo(mPage);// ��ȡ����
		final MainActivity activity = (MainActivity) getActivity();
		adapter = new HomePageAdapter(activity, R.layout.listitem_homepage,
				itemDataList);
		mPullRefreshListView = (PullToRefreshListView) fragmentView
				.findViewById(R.id.lvHomeListView);
		mPullRefreshListView.setMode(Mode.BOTH);// ���¶���������
		if (isFirstEnter) {
			tvHomePageLoading.setVisibility(View.VISIBLE);
			mPullRefreshListView.setVisibility(View.GONE);
		}
		mPullRefreshListView.setAdapter(adapter);
		mPullRefreshListView
				.setOnLastItemVisibleListener(new OnLastItemVisibleListener() {

					@Override
					public void onLastItemVisible() {
						// TODO Auto-generated method stub
						mPullRefreshListView.getLoadingLayoutProxy()
								.setRefreshingLabel("���ڼ���");
						mPullRefreshListView.getLoadingLayoutProxy()
								.setPullLabel("�������ظ���");
						mPullRefreshListView.getLoadingLayoutProxy()
								.setReleaseLabel("�ͷſ�ʼ����");
						Log.i(TAG, "�����ײ�");
					}
				});
		mPullRefreshListView
				.setOnRefreshListener(new OnRefreshListener2<ListView>() {

					@Override
					public void onPullDownToRefresh(
							PullToRefreshBase<ListView> refreshView) {
						// TODO Auto-generated method stub
						Log.i(TAG, "����");
						itemDataList.clear();
						adapter.notifyDataSetChanged();
						getHomePageInfo(0);
					}

					@Override
					public void onPullUpToRefresh(
							PullToRefreshBase<ListView> refreshView) {
						// TODO Auto-generated method stub
						Log.i(TAG, "����");
						mPage = mPage + 1;
						getHomePageInfo(mPage);
					}
				});
		return fragmentView;
	}

	private void getHomePageInfo(int page) {
		NetworkState networkState = new NetworkState();
		final MainActivity activity = (MainActivity) getActivity();
		if (networkState.isNetworkConnected(activity)) {
			Log.i(TAG, "NetworkIsConnected");
			RequestParams params = new RequestParams();
			params.put("page", page);
			String url = Config.getValue("HomePageUrl");
			AsyncHttpClient client = new AsyncHttpClient();
			PersistentCookieStore mCookieStore = new PersistentCookieStore(
					activity);
			client.setCookieStore(mCookieStore);
			client.get(url, params, new AsyncHttpResponseHandler() {

				@Override
				public void onFailure(int arg0, Header[] arg1, byte[] arg2,
						Throwable arg3) {
					// TODO Auto-generated method stub
					Toast.makeText((MainActivity) getActivity(),
							" �����е㲻��Ŷ�������ԣ���", Toast.LENGTH_LONG).show();
					mPullRefreshListView.onRefreshComplete();
				}

				@Override
				public void onSuccess(int arg0, Header[] arg1,
						byte[] responseContent) {
					// TODO Auto-generated method stub
					int layoutType = HomePageItemModel.LAYOUT_TYPE_SIMPLE;
					String string = new String(responseContent);
					Log.i(TAG, string);
					if (isFirstEnter == true) {
						isFirstEnter = false;
						tvHomePageLoading.setVisibility(View.GONE);
						mPullRefreshListView.setVisibility(View.VISIBLE);
					}
					try {
						JSONObject all = new JSONObject(string);
						JSONObject rsm = all.getJSONObject("rsm");
						totalRow = (rsm.getInt("total_rows"));
						Log.i(TAG, Integer.toString(totalRow));
						if (totalRow == 0) {
							// �Ѿ�����ȫ��������
							mPage = mPage - 1;
							Toast.makeText(activity, "�ף��������ô���ˣ�",
									Toast.LENGTH_LONG).show();
							mPullRefreshListView.onRefreshComplete();
						}
						JSONArray rows = rsm.getJSONArray("rows");
						for (int i = 0; i < rows.length(); i++) {
							JSONObject rowsObject = rows.getJSONObject(i);
							// int history_id = rowsObject.getInt("history_id");
							int userUid = rowsObject.getInt("uid");
							int allAction = rowsObject
									.getInt("associate_action");
							Log.i(TAG, Integer.toString(allAction));
							// int addTime = rowsObject.getInt("add_time");
							// ��ȡuserInfo����
							JSONObject userInfoObject = rowsObject
									.getJSONObject("user_info");
							String userName = userInfoObject
									.getString("user_name");
							Log.i(TAG, userName);
							String avatarUrl = Config
									.getValue("AvatarPrefixUrl")
									+ userInfoObject.getString("avatar_file");
							Log.i(TAG, userInfoObject.getString("avatar_file"));
							// ת��
							if (allAction == 101 || allAction == 105) {
								String action, itemTitle, bestAnswer = " ";
								int itemTitleUid, bestAnswerUid = 1, agreeCount = 1;
								JSONObject questionInfoObject = rowsObject
										.getJSONObject("question_info");
								itemTitle = questionInfoObject.getString(
										"question_content").trim();
								Log.i(TAG, itemTitle);
								itemTitleUid = questionInfoObject
										.getInt("question_id");
								Log.i(TAG, Integer.toString(itemTitleUid));
								if (allAction == 101) {
									action = "����������";
									layoutType = HomePageItemModel.LAYOUT_TYPE_SIMPLE;
								} else {
									action = "��ע������";
									layoutType = HomePageItemModel.LAYOUT_TYPE_SIMPLE;
								}
								layoutType = HomePageItemModel.LAYOUT_TYPE_SIMPLE;
								// ���ص�ListItemModel
								HomePageItemModel item = new HomePageItemModel(
										layoutType, avatarUrl, userName,
										userUid, action, itemTitle,
										itemTitleUid, bestAnswer,
										bestAnswerUid, agreeCount);
								itemDataList.add(item);
							}

							if (allAction == 201 || allAction == 204) {
								String action, itemTitle, bestAnswer = " ";
								int itemTitleUid, bestAnswerUid = 1, agreeCount = 1;
								JSONObject answerInfoObject = rowsObject
										.getJSONObject("answer_info");
								bestAnswerUid = answerInfoObject
										.getInt("answer_id");
								bestAnswer = answerInfoObject.getString(
										"answer_content").trim();
								Log.i(TAG, bestAnswer);
								agreeCount = answerInfoObject
										.getInt("agree_count");
								// ��ȡquestion_info����
								JSONObject questionInfoObject = rowsObject
										.getJSONObject("question_info");
								itemTitle = questionInfoObject.getString(
										"question_content").trim();
								Log.i(TAG, itemTitle);
								itemTitleUid = questionInfoObject
										.getInt("question_id");

								layoutType = HomePageItemModel.LAYOUT_TYPE_COMPLEX;

								if (allAction == 201) {
									action = "�ش������";
									layoutType = HomePageItemModel.LAYOUT_TYPE_COMPLEX;
								} else {
									action = "��ͬ�ûش�";
									layoutType = HomePageItemModel.LAYOUT_TYPE_COMPLEX;
								}
								// ���ص�ListItemModel
								HomePageItemModel item = new HomePageItemModel(
										layoutType, avatarUrl, userName,
										userUid, action, itemTitle,
										itemTitleUid, bestAnswer,
										bestAnswerUid, agreeCount);
								itemDataList.add(item);
							}

							if (allAction == 501 || allAction == 502) {
								String action, itemTitle, bestAnswer = " ";
								int itemTitleUid, bestAnswerUid = 1, agreeCount = 1;
								JSONObject articleInfoObject = rowsObject
										.getJSONObject("article_info");
								itemTitleUid = articleInfoObject.getInt("id");
								itemTitle = articleInfoObject
										.getString("title").trim();

								if (allAction == 501) {
									action = "����������";
									layoutType = HomePageItemModel.LAYOUT_TYPE_SIMPLE;
								} else {
									action = "��ͬ������";
									layoutType = HomePageItemModel.LAYOUT_TYPE_SIMPLE;
								}
								layoutType = HomePageItemModel.LAYOUT_TYPE_SIMPLE;
								//
								// ���ص�ListItemModel
								HomePageItemModel item = new HomePageItemModel(
										layoutType, avatarUrl, userName,
										userUid, action, itemTitle,
										itemTitleUid, bestAnswer,
										bestAnswerUid, agreeCount);
								itemDataList.add(item);
							}
							mPullRefreshListView.onRefreshComplete();
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.i(TAG, "Json�����쳣");
						mPullRefreshListView.onRefreshComplete();
					}

				}
			});
		} else {
			Toast.makeText((MainActivity) getActivity(), "δ�������磡",
					Toast.LENGTH_LONG).show();
			mPullRefreshListView.onRefreshComplete();
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (bundle != null) {
			((MainActivity) activity).onSectionAttached(getArguments().getInt(
					"position"));
		}
	}

}