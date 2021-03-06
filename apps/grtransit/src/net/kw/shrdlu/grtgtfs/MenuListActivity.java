/*
 * Copyright 2011 Giles Malet.
 *
 * This file is part of GRTransit.
 * 
 * GRTransit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GRTransit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GRTransit.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.kw.shrdlu.grtgtfs;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MenuListActivity extends ListActivity {
	private static final String TAG = "MenuListActivity";

	protected ListActivity mContext;
	protected ProgressBar mProgress;
	protected TextView mTitle;
	protected View mListDetail;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mProgress = (ProgressBar) findViewById(R.id.progress, ProgressBar.class);
		mTitle = (TextView) findViewById(R.id.listtitle, TextView.class);
		mListDetail = findViewById(R.id.detail_area, View.class);


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB /* 11 */) {
			APIReflectionWrapper.API11.prepActionBar(mContext);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// We want to track a pageView every time this activity gets the focus - but if the activity was
		// previously destroyed we could have lost our global data, so this is a bit of a hack to avoid a crash!
		//if (GRTApplication.tracker == null) {
		//	Log.e(TAG, "null tracker!");
		//	startActivity(new Intent(this, FavstopsActivity.class));
		//} else {
		//	GRTApplication.tracker.trackPageView("/" + this.getLocalClassName());
		//}
	}

	// Called when a button is clicked on the title bar
	public void onTitlebarClick(View v) {
		TitlebarClick.onTitlebarClick(mContext, v);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.busstopsmenu, menu);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB /* 11 */) {
			// Remove search from the menu, as we put it on the title bar.
			menu.removeItem(R.id.menu_search);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!TitlebarClick.onOptionsItemSelected(mContext, item)) {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
}
