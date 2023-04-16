package com.example.downloadmanger;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private long id ;
    private File apkFile;
    private  DownloadManager downloadmanager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadData("Hello");
       // downloadData("Hey");
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        TextView show =  findViewById(R.id.showDownloads);
        TextView release = findViewById(R.id.ReleaseDownloads);

        show.setOnClickListener(view -> {
            downloadData("yy");
        });

        release.setOnClickListener(view -> {
            downloadmanager.remove(id);
        });
    }

    private void downloadData(String title){
        String filename = "_"+System.currentTimeMillis() + ".apk";
        apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        Uri uri = Uri.parse("http://xt-15p-update.s3.us-east-1.amazonaws.com/APKs/CNN_v7.10.0.apk");
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(title);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
       // request.setDestinationUri(Uri.parse(apkFile.toString()));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,filename);
        id = downloadmanager.enqueue(request);
        getProgress();
    }



    @SuppressLint("Range")
    public void queryStatus() {
        Cursor c= downloadmanager.query(new DownloadManager.Query().setFilterById(id));

        if (c==null) {
            Toast.makeText(this, "Download not found!", Toast.LENGTH_LONG).show();
        }
        else {
            c.moveToFirst();

            Log.d("TATZ", "COLUMN_Title: "+
                    c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE)));
            Log.d("TATZ", "COLUMN_ID: "+
                    c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
            Log.d("TATZ", "COLUMN_BYTES_DOWNLOADED_SO_FAR: "+
                    c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)));
            Log.d("TATZ", "COLUMN_LAST_MODIFIED_TIMESTAMP: "+
                    c.getLong(c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)));
            Log.d("TATZ", "COLUMN_LOCAL_URI: "+
                    c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            Log.d("TATZ", "COLUMN_STATUS: "+
                    c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
            Log.d("TATZ", "COLUMN_REASON: "+
                    c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));

            Log.i("TATZ", "queryStatus: "+statusMessage(c));
        }
    }


    private void getProgress(){
        new Thread(new Runnable() {

            @SuppressLint("Range")
            @Override
            public void run() {

                boolean downloading = true;

                while (downloading) {
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(id);

                    Cursor cursor = downloadmanager.query(q);
                    cursor.moveToFirst();
                    if (cursor.getCount() > 0){
                        String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                        int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
                                || cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_FAILED) {
                            downloading = false;
                        }

                        final int dl_progress = (int) ((bytes_downloaded * 100L) / bytes_total);
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Log.i("TATZ", "title: "+title);
                                Log.i("TATZ", "progress: "+dl_progress);
                            }
                        });
                    }else {
                        downloading = false;
                    }
                    cursor.close();
                }

            }
        }).start();
    }

    BroadcastReceiver onComplete=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            Log.i("TATZ", "onComplete: ");
           // queryStatus();
        }
    };

    BroadcastReceiver onNotificationClick=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            Log.i("TATZ", "onNotificationClick: ");
        }
    };

    public void viewLog() {
        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
    }

    @SuppressLint("Range")
    private String statusMessage(Cursor c) {
        String msg="???";

        switch(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg="Download failed!";
                break;

            case DownloadManager.STATUS_PAUSED:
                msg="Download paused!";
                break;

            case DownloadManager.STATUS_PENDING:
                msg="Download pending!";
                break;

            case DownloadManager.STATUS_RUNNING:
                msg="Download in progress!";
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                msg="Download complete!";
                break;

            default:
                msg="Download is nowhere in sight";
                break;
        }

        return(msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(onComplete);
        unregisterReceiver(onNotificationClick);
    }
}