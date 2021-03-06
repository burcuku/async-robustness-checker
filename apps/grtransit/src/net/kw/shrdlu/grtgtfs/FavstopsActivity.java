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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class FavstopsActivity extends MenuListActivity {
	private static final String TAG = "FavstopsActivity";

	private ArrayList<String[]> mDetails;
	private String mStopid;
	private FavstopsArrayAdapter mAdapter;
	private final SQLiteDatabase DB = DatabaseHelper.ReadableDB();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mContext = this;
		setContentView(R.layout.timeslayout);
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB /* 11 */) {
			// Switch title bar icon to the one without the < hint.
			final Button logo = (Button) findViewById(R.id.titlelogo, Button.class);
			final Drawable d = getResources().getDrawable(R.drawable.grticon_nohome);
			logo.setClickable(false);
			logo.setBackgroundDrawable(d);
		}

		final ListView lv = getListView();
		final TextView tv = new TextView(mContext);
		tv.setText(R.string.longpress_removes_stop);
		lv.addFooterView(tv);

		/* Make sure we can access a database */
		if (DB == null) {
			final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					mContext.finish();
					return;
				}
			};
			final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(R.string.db_is_awol)
			.setMessage(R.string.db_not_avail)
			.setNegativeButton(R.string.exit, listener)
			.create()
			.show();
		}

		// ProcessStops(); // will be done in onResume()
	}

	/* Separate the processing of stops, so we can re-do it when we need to refresh the screen on a new intent. */
	static boolean mShownalert = false;

	protected void ProcessStops() {

		mDetails = new ArrayList<String[]>();
		final ArrayList<String[]> favstops = GRTApplication.mPreferences.GetBusstopFavourites();
		// Convert from stop/description to required 4-entry layout.
		synchronized (mDetails) {
			for (final String[] stop : favstops) {
				// Just do what we can for now
				mDetails.add(new String[] { stop[0], stop[1], "", getString(R.string.loading_times), "?" });
			}
		}
		mAdapter = new FavstopsArrayAdapter(this, R.layout.favouritesrow, mDetails);
		setListAdapter(mAdapter);

		// added to go into the branch to start AsyncTask:
		mDetails.add(new String[] {"dummy", "detail", "of", "size", "5"});

		// Must do all this without doing a database read, which allows database upgrade
		// to happen in the background on a service thread, without us blocking, until
		// we really have to.
		if (!mDetails.isEmpty()) {

			// register to get long clicks on bus stop list
			getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
					// Log.i(TAG, "onItemLongClickClick position " + position);
					onListItemLongClick(parent, view, position, id);
					return true; // to say we consumed the click
				}
			});

			// Load times of next bus for each stop.
			new LoadTimes().execute();

		} else if (!mShownalert) {
			mShownalert = true;

			TextView textView;
			final View messageView = mContext.getLayoutInflater().inflate(R.layout.about, null, false);

			textView = (TextView) messageView.findViewById(R.id.about_header, TextView.class);
			textView.setVisibility(TextView.GONE);
			textView = (TextView) messageView.findViewById(R.id.about_credits, TextView.class);
			textView.setText(R.string.no_favourites);
			textView.setHorizontallyScrolling(false); // make text wrap.

			final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			// builder.setIcon(R.drawable.grticon);
			builder.setTitle(R.string.title_favourites).setView(messageView).create().show();
		}
	}

	// If we're popping back down the stack, the favourites list could have been added to
	// since we were last here, so make sure it is reloaded before display.
	@Override
	public void onResume() { // made public to be accessible from the driver class
		super.onResume();

		/* Give up if there's no database */
		if (DB == null) {
			return;
		}

		mListDetail.invalidate();
		ProcessStops();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	// Called from the listener above for a long click
	protected void onListItemLongClick(AdapterView<?> parent, View v, int position, long id) {
		// Log.v(TAG, "long clicked position " + position);

		final String[] strs = (String[]) parent.getItemAtPosition(position, String[].class);
		if (strs == null) {
			return;
		}
		mStopid = strs[0];
		final String stop_name = strs[1];
		final int aryposn = position; // so we can access it in the listener class.

		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				switch (id) {
				case DialogInterface.BUTTON_POSITIVE:
					GRTApplication.mPreferences.RemoveBusstopFavourite(mStopid);
					synchronized (mDetails) {
						mDetails.remove(aryposn);
					}
					// activities in the stack may contain out of date lists, so flush and start again.
					mContext.startActivity(new Intent(mContext, FavstopsActivity.class));
					break;
					// case DialogInterface.BUTTON_NEGATIVE:
					// // nothing
					// break;
				}
				dialog.cancel();
			}
		};

		final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setTitle("Stop " + mStopid + ", " + stop_name);
		builder.setMessage(R.string.favs_remove_from_list).setPositiveButton(R.string.yes, listener)
		.setNegativeButton(R.string.no, listener);
		builder.create();
		builder.show();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Log.v(TAG, "clicked position " + position);

		final String[] strs = (String[]) l.getItemAtPosition(position, String[].class);
		if (strs == null) {
			return;
		}
		mStopid = strs[0];
		final String stop_name = strs[1];


		final Intent routeselect = new Intent(mContext, RouteselectActivity.class);
		final String pkgstr = mContext.getApplicationContext().getPackageName();
		routeselect.putExtra(pkgstr + ".stop_id", mStopid);
		routeselect.putExtra(pkgstr + ".stop_name", stop_name);
		mContext.startActivity(routeselect);
	}

	/* Do the processing to load the ArrayAdapter for display. */
	private class LoadTimes extends AsyncTask<Void, Integer, Void> {

		@Override
		protected void onPreExecute() {
			mProgress.setVisibility(View.VISIBLE);
			mProgress.setProgress(5); // make partially visible
		}

		@Override
		protected void onProgressUpdate(Integer... parms) {
			mProgress.setProgress(parms[0]);
			mAdapter.notifyDataSetChanged();
		}

		@Override
		protected Void doInBackground(Void... foo) {
			// Find time of next bus for each stop.
			final Time t = new Time(); // TODO - this duplicates BusTimes?
			t.setToNow();
			final String datenow = String.format("%04d%02d%02d", t.year, t.month + 1, t.monthDay);

			Integer progresscount = 0;
			synchronized (mDetails) {
				for (final String[] pref : mDetails) {
					final String stopid = pref[0];
					// final String stopdescr = pref[1];

					// Log.d(TAG, "Searching for busses for stop " + stopid + " " + stopdescr);
					final String[] nextbus = ServiceCalendar.getNextDepartureTime(stopid, datenow);
					if (nextbus != null) {
						// Log.d(TAG, "Next bus for stop " + stopid + ": " + nextbus[0] + " " + nextbus[1] + " - " +
						// nextbus[2]);
						pref[2] = nextbus[0]; // time
						pref[3] = nextbus[2]; // route headsign
						pref[4] = nextbus[1]; // route number
					} else {
						// Log.d(TAG, "Next bus for stop " + stopid + ": --none--");
						pref[2] = " -- -- --"; // time
						pref[3] = getString(R.string.no_more_busses); // route details
						pref[4] = "-";
					}
					publishProgress(++progresscount * 100 / mDetails.size());
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void foo) {
			mProgress.setVisibility(View.INVISIBLE);

			final TextView v = (TextView) findViewById(R.id.listtitle, TextView.class);
			v.setText(R.string.title_favourites);
		}
	}
}
