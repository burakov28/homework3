package com.ivan_pc.downloadpicture;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;




public class MyIntentService extends IntentService {

    private static final int BLOCK_SIZE = 16 * 1024;
    public static final int PHOTO_SIZE = 9566288; // because method getContentLength() always
                                                  // returns -1
    protected static boolean onWork = false;
    private static final String ACTION_DOWNLOADING = "com.ivan_pc.downloadpicture.action.ACTION_DOWNLOADING";
    private static final String LOG_TAG = MyIntentService.class.getSimpleName();

    public MyIntentService() {
        super("MyIntentService");
    }


    public static void startDownloading(Context context) {
        onWork = true;
        Intent intent = new Intent(context, MyIntentService.class);
        intent.setAction(ACTION_DOWNLOADING);
        context.startService(intent);
    }


    private void sendProgress(String title, String message, int iconId, int flags) {
        NotificationManager nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        Notification notif = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(iconId)
                .setWhen(System.currentTimeMillis())
                .build();
        notif.flags |= flags;
        nm.notify(0, notif);
    }

    private void sendDone() {
        NotificationManager nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        Notification notif = new Notification.Builder(this)
                .setContentIntent(pIntent)
                .setContentTitle("Done")
                .setContentText("Downloading complete")
                .setSmallIcon(R.drawable.done)
                .setWhen(System.currentTimeMillis())
                .build();
        notif.flags |= Notification.FLAG_AUTO_CANCEL;
        nm.notify(0, notif);
    }

    private void handleDownloading(Context context) {
        HttpURLConnection connection = null;
        InputStream dwn = null;
        FileOutputStream toFile = null;
        String filePath = context.getFilesDir().toString() + getString(R.string.fileName);
        File image = new File(filePath);
        Log.d(LOG_TAG, filePath);
        if (image.length() != (long) PHOTO_SIZE) {
            try {
                connection = (HttpURLConnection) new URL(getString(R.string.imageURL)).openConnection();
                connection.connect();
                //long size = connection.getContentLength() it doesn't work! always return -1;
                int size = PHOTO_SIZE;

                dwn = new BufferedInputStream(connection.getInputStream(), BLOCK_SIZE);

                if (image.exists()) {
                    if (!image.delete()) {
                        throw new IOException("can't delete file");
                    }
                    Log.d(LOG_TAG, "has_been_deleted");
                }
                Log.d(LOG_TAG, Long.toString(size));
                toFile = new FileOutputStream(image, true);
                byte[] bytes = new byte[BLOCK_SIZE];
                int count;
                int read = 0;
                String title = "Downloading...";
                String message = "% has been downloaded";
                int flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR |
                        Notification.FLAG_AUTO_CANCEL;
                //Log.d(LOG_TAG, "started");
                int last = 0;
                while ((count = dwn.read(bytes, 0, BLOCK_SIZE)) != -1) {
                    //Log.d(LOG_TAG, "read");
                    read += count;
                    toFile.write(bytes, 0, count);
                    //Log.d(LOG_TAG, "written");
                    int percent = (read * 100) / size;
                    if (last != percent) {
                        last = percent;
                        Intent msg = new Intent(MainActivity.BROADCAST_ACTION).putExtra(MainActivity.STATE, percent);
                        sendBroadcast(msg);
                        sendProgress(title, Integer.toString(percent) + message, R.drawable.downloading, flags);
                    }
                }
                toFile.flush();
                Log.d(LOG_TAG, Long.toString(read));
                Intent msg = new Intent(MainActivity.BROADCAST_ACTION).putExtra(MainActivity.STATE, 100);
                sendBroadcast(msg);
                sendDone();

            }catch(MalformedURLException e){
                Intent msg = new Intent(MainActivity.BROADCAST_ACTION).putExtra(MainActivity.STATE, -2);
                sendBroadcast(msg);
                e.printStackTrace();
                sendProgress("Error!", "Downloading has been interrupted!", R.drawable.error,
                        Notification.FLAG_AUTO_CANCEL);
            }catch(IOException e){
                Intent msg = new Intent(MainActivity.BROADCAST_ACTION).putExtra(MainActivity.STATE, -2);
                sendBroadcast(msg);
                sendProgress("Error!", "Downloading error", R.drawable.error,
                        Notification.FLAG_AUTO_CANCEL);
            }finally{

                try {
                    onWork = false;
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (toFile != null) {
                        toFile.close();
                    }
                    if (dwn != null) {
                        dwn.close();
                    }
                } catch (IOException e) {
                    sendProgress("Error!", "File has been unexpectedly deleted", R.drawable.error,
                            Notification.FLAG_AUTO_CANCEL);
                    Intent msg = new Intent(MainActivity.BROADCAST_ACTION).putExtra(MainActivity.STATE, -2);
                    sendBroadcast(msg);
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_DOWNLOADING.equals(action)) {
                Log.d("handleIntent", "start");
                handleDownloading(this);
            }
        }
    }

}
