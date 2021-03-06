package com.totsp.bookworm;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class About extends Activity {

   public TextView about;
   public Button aboutDetails;

   @Override
   public void onCreate(final Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about);

      about = (TextView) findViewById(R.id.aboutcontent, TextView.class);
      about.setText(Html.fromHtml(getString(R.string.aboutcontent)), TextView.BufferType.SPANNABLE);

      aboutDetails = (Button) findViewById(R.id.aboutdetails, Button.class);
      aboutDetails.setOnClickListener(new OnClickListener() {
         public void onClick(View v) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("file:///android_asset/release_notes.html"),
                     About.this, HtmlScreen.class));
         }
      });
   }

   @Override
   public void onStart() {
      super.onStart();
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   @Override
   protected void onStop() {
      super.onStop();
   }

   @Override
   protected void onRestoreInstanceState(final Bundle savedInstanceState) {
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(final Bundle saveState) {
      super.onSaveInstanceState(saveState);
   }
}