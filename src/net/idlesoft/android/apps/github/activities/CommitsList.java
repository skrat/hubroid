/**
 * Hubroid - A GitHub app for Android
 *
 * Copyright (c) 2011 Eddie Ringle.
 *
 * Licensed under the New BSD License.
 */

package net.idlesoft.android.apps.github.activities;

import net.idlesoft.android.apps.github.R;
import net.idlesoft.android.apps.github.adapters.CommitListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class CommitsList extends BaseActivity {
    private static class GatherCommitsTask extends AsyncTask<Void, Void, Void> {
        public CommitsList activity;

        @Override
        protected Void doInBackground(final Void... params) {
            try {
                activity.mCommitsJSON = new JSONObject(activity.mGApi.commits.list(
                        activity.mRepoOwner, activity.mRepoName, activity.mBranchName).resp)
                        .getJSONArray("commits");
                ArrayList<JSONObject> list = new ArrayList<JSONObject>();
                int size = activity.mCommitsJSON.length();
                for (int i = 0; i < size; i++) {
                    list.add(activity.mCommitsJSON.getJSONObject(i));
                }
                activity.mCommitListAdapter.loadData(list);
            } catch (final JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            /*
             * activity.mCommitListView.setAdapter(activity.mCommitListAdapter);
             * activity.mProgressDialog.dismiss();
             */
            activity.mCommitListAdapter.pushData();
            activity.mCommitListAdapter.setIsLoadingData(false);
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            /*
             * activity.mProgressDialog = ProgressDialog.show(activity,
             * "Please wait...", "Loading repository's commits...", true);
             */
            activity.mCommitListAdapter.setIsLoadingData(true);
        }
    }

    public String mBranchName;

    public CommitListAdapter mCommitListAdapter;

    public ListView mCommitListView;

    public JSONArray mCommitsJSON;

    private GatherCommitsTask mGatherCommitsTask;

    public ProgressDialog mProgressDialog;

    public String mRepoName;

    public String mRepoOwner;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle, R.layout.commits_list);

        setupActionBar();

        getActionBar().setTitle("Recent Commits");

        mCommitListView = (ListView) findViewById(R.id.lv_commits_list_list);
        mCommitListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                try {
                    final Intent i = new Intent(CommitsList.this, Commit.class);
                    i.putExtra("committer",
                            mCommitsJSON.getJSONObject(position).getJSONObject("committer")
                                    .getString("login"));
                    i.putExtra(
                            "author",
                            mCommitsJSON.getJSONObject(position).getJSONObject("author")
                                    .getString("login"));
                    i.putExtra("commit_sha", mCommitsJSON.getJSONObject(position).getString("id"));
                    i.putExtra("repo_name", mRepoName);
                    i.putExtra("repo_owner", mRepoOwner);
                    CommitsList.this.startActivity(i);
                } catch (final JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mCommitListAdapter = new CommitListAdapter(CommitsList.this, mCommitListView);
        mCommitListView.setAdapter(mCommitListAdapter);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mRepoName = extras.getString("repo_name");
            mRepoOwner = extras.getString("repo_owner");
            mBranchName = extras.getString("branch_name");
        }
    }

    @Override
    public void onPause() {
        if ((mProgressDialog != null) && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        try {
            if (savedInstanceState.containsKey("commitsJson")) {
                mCommitsJSON = new JSONArray(savedInstanceState.getString("commitsJson"));
                if (mCommitsJSON != null) {
                    ArrayList<JSONObject> list = new ArrayList<JSONObject>();
                    int size = mCommitsJSON.length();
                    for (int i = 0; i < size; i++) {
                        list.add(mCommitsJSON.getJSONObject(i));
                    }
                    mCommitListAdapter.loadData(list);
                    mCommitListAdapter.pushData();
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    protected void onResume() {
        mGatherCommitsTask = (GatherCommitsTask) getLastNonConfigurationInstance();
        if (mGatherCommitsTask == null) {
            mGatherCommitsTask = new GatherCommitsTask();
        }
        mGatherCommitsTask.activity = this;
        if (mGatherCommitsTask.getStatus() == AsyncTask.Status.PENDING) {
            mGatherCommitsTask.execute();
        }
        super.onResume();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mGatherCommitsTask;
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        if (mCommitsJSON != null) {
            savedInstanceState.putString("commitsJson", mCommitsJSON.toString());
        }
        super.onSaveInstanceState(savedInstanceState);
    }
}
