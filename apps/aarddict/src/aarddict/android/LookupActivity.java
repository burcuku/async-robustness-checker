/* This file is part of Aard Dictionary for Android <http://aarddict.org>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License <http://www.gnu.org/licenses/gpl-3.0.txt>
 * for more details.
 *
 * Copyright (C) 2010 Igor Tkach
*/

package aarddict.android;

import java.util.*;

import aarddict.Entry;
import aarddict.EntryComparator;
import aarddict.EntryComparators;
import aarddict.MatchIterator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;
import checker.Checker;
import checker.ProcMode;
import checker.SkipException;

public class LookupActivity extends BaseDictionaryActivity {

    private final static String TAG     = LookupActivity.class.getName();

    //private Timer               timer;
    private ListView            listView;
    private Iterator<Entry> empty = new ArrayList<Entry>().iterator();

    void updateTitle() {
        int dictCount = dictionaryService.getVolumes().size();
        Resources r = getResources();
        String dictionaries = r.getQuantityString(R.plurals.dictionaries, dictCount);
        String appName = r.getString(R.string.appName);
        String mainTitle = r.getString(R.string.titleLookupActivity, appName, String.format(dictionaries, dictCount));
        setTitle(mainTitle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //timer.cancel();
    }

    private void updateWordListUI(final Iterator<Entry> results) {
        /*runOnUiThread(new Runnable() {
            public void run() {
                TextView messageView = (TextView)findViewById(R.id.messageView, TextView.class);
                if (!results.hasNext()) {
                    Editable text = editText.getText();
                    if (text != null && !text.toString().equals("")) {
                        messageView.setText(Html.fromHtml(getString(R.string.nothingFound)));
                        messageView.setVisibility(View.VISIBLE);
                    }
                    else {
                        messageView.setVisibility(View.GONE);
                    }
                }
                else {
                        messageView.setVisibility(View.GONE);
                }
                WordAdapter wordAdapter = new WordAdapter(results);
                listView.setAdapter(wordAdapter);
                listView.setOnItemClickListener(wordAdapter);
                setProgressBarIndeterminateVisibility(false);
            }
        });*/

        Runnable runnable = new Runnable() {
            public void run() {
                TextView messageView = (TextView)findViewById(R.id.messageView, TextView.class);
                if (!results.hasNext()) {
                    Editable text = editText.getText();
                    if (text != null && !text.toString().equals("")) {
                        messageView.setText(Html.fromHtml(getString(R.string.nothingFound)));
                        messageView.setVisibility(View.VISIBLE);
                    }
                    else {
                        messageView.setVisibility(View.GONE);
                    }
                }
                else {
                    messageView.setVisibility(View.GONE);
                }
                WordAdapter wordAdapter = new WordAdapter(results);
                listView.setAdapter(wordAdapter);
                listView.setOnItemClickListener(wordAdapter);
                setProgressBarIndeterminateVisibility(false);
            }
        };
        // simulate runnable's async execution

        try {
            // runs async on the main thread
            Checker.beforeAsyncProc(ProcMode.ASYNCMain);
            runnable.run();

        } catch (SkipException e) {
        } finally {
            Checker.afterAsyncProc();
            //Checker.setProcMode(ProcMode.SYNCMain);
        }

    }

    final Runnable updateProgress = new Runnable() {
        public void run() {
            setProgressBarIndeterminateVisibility(true);
        }
    };

    private void doLookup(CharSequence word) {
        //System.out.println("Inside doLookup, reading dictionaryService in the backg");
        if (dictionaryService == null)
            return;
        word = trimLeft(word.toString());
        if (word.equals("")) {
                Log.d(TAG, "Nothing to look up");
                updateWordListUI(empty);
                return;
        }
        runOnUiThread(updateProgress); // UI only
        long t0 = System.currentTimeMillis();
        try {
            Iterator<Entry> results = dictionaryService.lookup(word);
            Log.d(TAG, "Looked up " + word + " in "
                    + (System.currentTimeMillis() - t0));
            updateWordListUI(results);
        } catch (SkipException e) {
            throw e;  // not to block SkipException
        } catch (Exception e) {
            StringBuilder msgBuilder = new StringBuilder(
                    "There was an error while looking up ").append("\"")
                    .append(word).append("\"");
            if (e.getMessage() != null) {
                msgBuilder.append(": ").append(e.getMessage());
            }
            final String msg = msgBuilder.toString();
            Log.e(TAG, msg, e);
        }
    }

    private void launchWord(Entry theWord) {
        Intent next = new Intent();
        next.setClass(this, ArticleViewActivity.class);
        next.putExtra("word", theWord.title);
        next.putExtra("section", theWord.section);
        next.putExtra("volumeId", theWord.volumeId);
        next.putExtra("articlePointer", theWord.articlePointer);
        startActivity(next);
    }


    final class WordAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        private final List<Entry>    words;
        private final LayoutInflater mInflater;
        private int                  itemCount;
        private Iterator<Entry>      results;
        private boolean              displayMore;

        public WordAdapter(Iterator<Entry> results) {
            this.results = results;
            this.words = new ArrayList<Entry>();
            loadBatch();
            mInflater = (LayoutInflater) LookupActivity.this.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        private void loadBatch() {
            int count = 0;
            while (results.hasNext() && count < 20) {
                count++;
                words.add(results.next());
            }
            displayMore = results.hasNext();
            itemCount = words.size();
        }

        public int getCount() {
            return itemCount;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (displayMore && position == itemCount - 1) {
                loadMore(position);
            }
            TwoLineListItem view = (convertView != null) ? (TwoLineListItem) convertView :
                    createView(parent);
            bindView(view, words.get(position));
            return view;
        }

        private int loadingMoreForPos;

        private void loadMore(int forPos) {
                if (loadingMoreForPos == forPos) {
                        return;
                }
                loadingMoreForPos = forPos;
                /*new Thread(new Runnable() {
                        public void run() {
                                loadBatch();
                                runOnUiThread(new Runnable() {
                                        public void run() {
                                                notifyDataSetChanged();
                                        }
                                });
                        }
                }).start();*/
            // make synchronous
            Runnable runnable = new Runnable() {
                public void run() {
                    loadBatch();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            };

            // simulate its async execution
            try {
                // runs async on a background thread
                Checker.beforeAsyncProc(ProcMode.ASYNCBack);
                runnable.run();

            } catch (SkipException e) {
            } finally {
                Checker.afterAsyncProc();
            }
        }

        private TwoLineListItem createView(ViewGroup parent) {
                TwoLineListItem item;
                if (DeviceInfo.EINK_SCREEN)
                        item = (TwoLineListItem) mInflater.inflate(
                        R.layout.eink_simple_list_item_2, parent, false);
                else
                item = (TwoLineListItem) mInflater.inflate(
                        android.R.layout.simple_list_item_2, parent, false);
            item.getText2().setSingleLine();
            return item;
        }

        private void bindView(TwoLineListItem view, Entry word) {
            view.getText1().setText(word.title);
            view.getText2().setText(dictionaryService.getDisplayTitle(word.volumeId));
        }

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchWord(words.get(position));
        }
    }


    final static int MENU_DICT_INFO = 1;
    final static int MENU_ABOUT = 2;
    final static int MENU_DICT_REFRESH = 3;
    public EditText editText; //made public to be accessible from the driver class

    public TextWatcher textWatcher; //made public to be accessible from the driver class

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_DICT_INFO, 0, R.string.mnInfo).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, MENU_ABOUT, 0, R.string.mnAbout).setIcon(R.drawable.ic_menu_aarddict);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DICT_INFO:
            startActivity(new Intent(this, DictionariesActivity.class));
            break;
        case MENU_ABOUT:
            showAbout();
            break;
        }
        return true;
    }

        private void showAbout() {
        PackageManager manager = getPackageManager();
        String versionName = "";
        try {
                        PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
                        versionName = info.versionName;
                } catch (NameNotFoundException e) {
                        Log.e(TAG, "Failed to load package info for " + getPackageName(), e) ;
                }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.FILL_PARENT, 1));
        layout.setPadding(10, 10, 10, 10);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.aarddict);
        logo.setPadding(0, 0, 20, 0);
        logo.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT));
        TextView textView = new TextView(this);
        textView.setGravity(0);
        textView.setLineSpacing(2f, 1);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT));
        textView.setText(Html.fromHtml(getString(R.string.about, getString(R.string.appName), versionName)));

        LinearLayout textViewLayout = new LinearLayout(this);
        textViewLayout.setOrientation(LinearLayout.VERTICAL);
        textViewLayout.setPadding(0, 0, 0, 10);
        textViewLayout.addView(textView);

        layout.addView(logo);
        layout.addView(textViewLayout);

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.titleAbout).setView(layout).setNeutralButton(R.string.btnDismiss, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.show();
        }

    @Override
    void onDictionaryServiceReady() {
        updateTitle();
        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEARCH)) {
            final String word = intent.getStringExtra("query");
            editText.setText(word);

            try {
                /*timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Running in: " + Thread.currentThread().getName());
                        Log.d(TAG, "running lookup task for " + word + " in " + Thread.currentThread());
                        doLookup(word);
                    }
                }, 0);*/
                // make synchronous
                //new TimerTask() {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        //Log.d(TAG, "running lookup task for " + word + " in " + Thread.currentThread());
                        doLookup(word);
                    }
                };

                // simulate its async behavior
                try {
                    Checker.beforeAsyncProc(ProcMode.ASYNCBack);
                    runnable.run();

                } catch (SkipException e) {
                } finally {
                    Checker.afterAsyncProc();
                }

            }
            catch(IllegalStateException e) {
                Log.e(TAG, "Failed to schedule lookup task", e);
            }
        }
        else {
            textWatcher.afterTextChanged(editText.getText());
        }
    }

    @Override
    void onDictionaryOpenFinished() {
        onDictionaryServiceReady();
    }

    @Override
    void initUI() {
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        if (DeviceInfo.EINK_SCREEN)
        {
                setContentView(R.layout.eink_lookup);
            listView = (ListView)findViewById(R.id.einkLookupResult, ListView.class);
        }
        else
        {
                setContentView(R.layout.lookup);
            listView = (ListView)findViewById(R.id.lookupResult, ListView.class);
        }

        //timer = new Timer();

        editText = (EditText)findViewById(R.id.wordInput, EditText.class);

        textWatcher = new TextWatcher() {

            TimerTask currentLookupTask;

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            public void afterTextChanged(Editable s) {
                //System.out.println("Inside after text changed: " + s);

                if (currentLookupTask != null) {
                    currentLookupTask.cancel();
                }

                final Editable textToLookup = s;

                /*try {
                        timer.schedule(currentLookupTask, 600);
                }
                catch(IllegalStateException e) {
                        //this may happen if orientation changes while loading
                        Log.d(TAG, "Failed to schedule lookup task", e);
                }*/

                Runnable runnable = new Runnable() {
                //currentLookupTask = new TimerTask() {
                    @Override
                    public void run() {
                        //Log.d(TAG, "running lookup task for " + textToLookup + " in " + Thread.currentThread());
                        if (textToLookup.toString().equals(editText.getText().toString())) {
                            //System.out.println("Inside if of doLookUp " + textToLookup);
                            doLookup(textToLookup);
                        }
                    }
                };

                // simulate its async behavior
                try {
                    // load clinit parameters so that they do not throw SkipExceptions inside async block
                    Comparator<Entry> dummy[] = EntryComparators.ALL;
                    Checker.beforeAsyncProc(ProcMode.ASYNCBack);
                    runnable.run();

                } catch (SkipException e) {
                } finally {
                    Checker.afterAsyncProc();
                }



            }
        };
        editText.addTextChangedListener(textWatcher);

        editText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                 if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                         // removed InputMethodManager code
                         return true;
                 }
                 return false;
            }
        });

        editText.setInputType(0);

        Button btnClear = (Button)findViewById(R.id.clearButton, Button.class);
        btnClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                editText.setText("");
                editText.requestFocus();
                // removed InputMethodManager code

            }
        });
    }

    static String trimLeft(String s) {
        return s.replaceAll("^\\s+", "");
    }
}
