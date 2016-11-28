package com.ivan_pc.downloadpicture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    protected static final String STATE = "state";
    protected static final String BROADCAST_ACTION = "com.ivan_pc.downloadpicture.servicebroadcast";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String IS_SHOWING = "isShowing";

    protected class MyReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(STATE, -1);
            if (state == -1) return;
            if (state == -2) {
                turnToError();
                return;
            }
            ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
            bar.setMax(100);
            if (state < 100) {
                turnToProcess();
                bar.setVisibility(View.VISIBLE);
                bar.setProgress(state);
            }
            else {
                turnToDownloaded();
            }
        }
    }
    MyReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("mainActivity", "start");
        receiver = new MyReceiver();

        IntentFilter iFilter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(receiver, iFilter);

        if (savedInstanceState != null && savedInstanceState.getBoolean(IS_SHOWING) && fileFound()) {
            turnToDisplay();
            display();
            return;
        }

        if (inProcess()) {
            turnToProcess();
        } else if (fileFound()) {
            turnToDownloaded();
        } else {
            turnToUnDownloaded();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        View iv = findViewById(R.id.imageView);
        if (iv.getVisibility() == View.VISIBLE) {
            outState.putBoolean(IS_SHOWING, true);
        }
        else {
            outState.putBoolean(IS_SHOWING, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    public void startDownloading(View view) {
        turnToProcess();
        MyIntentService.startDownloading(this);
    }

    public void deleteFile(View view) {
        new File(this.getFilesDir().toString() + getString(R.string.fileName)).delete();
        turnToUnDownloaded();
    }

    public void display(View view) {
        turnToDisplay();
        display();
    }

    private boolean fileFound() {
        String filePath = this.getFilesDir().toString() + getString(R.string.fileName);
        File image = new File(filePath);
        return (image.length() == (long) MyIntentService.PHOTO_SIZE);
    }

    private boolean inProcess() {
        return MyIntentService.onWork;
    }

    private void turnToProcess() {
        findViewById(R.id.imageView).setVisibility(View.INVISIBLE);
        findViewById(R.id.deleteBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.displayBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        TextView tv = (TextView) findViewById(R.id.textView);
        tv.setText(getString(R.string.downloadingInProgress));
        tv.setVisibility(View.VISIBLE);
        Button dBtn = (Button) findViewById(R.id.startDownloading);
        dBtn.setVisibility(View.VISIBLE);
        dBtn.setEnabled(false);
    }

    private void turnToDownloaded() {
        findViewById(R.id.imageView).setVisibility(View.INVISIBLE);
        findViewById(R.id.startDownloading).setVisibility((View.INVISIBLE));
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.deleteBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.displayBtn).setVisibility(View.VISIBLE);
        TextView tv = (TextView) findViewById(R.id.textView);
        tv.setText(getString(R.string.downloaded));
        tv.setVisibility(View.VISIBLE);
    }

    private void turnToUnDownloaded() {
        findViewById(R.id.imageView).setVisibility(View.INVISIBLE);
        findViewById(R.id.startDownloading).setVisibility((View.VISIBLE));
        findViewById(R.id.startDownloading).setEnabled(true);

        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.deleteBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.displayBtn).setVisibility(View.INVISIBLE);
        TextView tv = (TextView) findViewById(R.id.textView);
        tv.setText(getString(R.string.notDownloaded));
        tv.setVisibility(View.VISIBLE);
    }

    private void turnToDisplay() {
        findViewById(R.id.imageView).setVisibility(View.VISIBLE);
        findViewById(R.id.startDownloading).setVisibility((View.INVISIBLE));
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.deleteBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.displayBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.textView).setVisibility(View.INVISIBLE);
    }

    private void turnToError() {
        findViewById(R.id.imageView).setVisibility(View.INVISIBLE);
        findViewById(R.id.startDownloading).setVisibility((View.VISIBLE));
        findViewById(R.id.startDownloading).setEnabled(true);
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        findViewById(R.id.deleteBtn).setVisibility(View.INVISIBLE);
        findViewById(R.id.displayBtn).setVisibility(View.INVISIBLE);
        TextView tv = (TextView) findViewById(R.id.textView);
        tv.setText(getString(R.string.error));
        tv.setVisibility(View.VISIBLE);
    }

    private void display() {
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        String filePath = this.getFilesDir().toString() + getString(R.string.fileName);

        int targetW = 2048;
        int targetH = 2048;
        BitmapFactory.Options bmo = new BitmapFactory.Options();
        bmo.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, bmo);
        int photoH = bmo.outHeight;
        int photoW = bmo.outWidth;
        int scale = 1;
        while (photoW / scale > targetW || photoH / scale > targetH) {
            scale *= 2;
        }
        Log.d(LOG_TAG, Integer.toString(scale));
        bmo.inSampleSize = scale;

        bmo.inJustDecodeBounds = false;

        Bitmap bm = BitmapFactory.decodeFile(filePath, bmo);
        iv.setImageBitmap(bm);

        //iv.setVisibility(View.VISIBLE);
    }
}
