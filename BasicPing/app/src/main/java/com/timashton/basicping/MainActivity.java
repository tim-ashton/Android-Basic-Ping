package com.timashton.basicping;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public class MainActivity extends Activity implements View.OnClickListener {

    public static String TAG = MainActivity.class.getName();

    PingTask mPingTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button pingButton = (Button) findViewById(R.id.button_start_ping);
        pingButton.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mPingTask.stop();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_start_ping) {
            Log.d(TAG, "onClick");

            EditText host = (EditText) findViewById(R.id.et_ip_hostname);

            mPingTask = new PingTask();
            mPingTask.execute(host.getText().toString());
        }
    }


    class PingTask extends AsyncTask<String, Void, Void> {
        PipedOutputStream mPOut;
        PipedInputStream mPIn;
        LineNumberReader mReader;
        Process mProcess;
        TextView mText = (TextView) findViewById(R.id.tv_ping_out);

        @Override
        protected void onPreExecute() {
            mPOut = new PipedOutputStream();
            try {
                mPIn = new PipedInputStream(mPOut);
                mReader = new LineNumberReader(new InputStreamReader(mPIn));
            } catch (IOException e) {
                Log.e(TAG, e.toString());

                cancel(true);
            }

        }

        public void stop() {
            if (mProcess != null) {
                mProcess.destroy();
            }
            cancel(true);
        }

        @Override
        protected Void doInBackground(String... params) {
            try {

                // get a handle to ping
                mProcess = new ProcessBuilder()
                        .command("/system/bin/ping", params[0])
                        .redirectErrorStream(true)
                        .start();

                try {
                    InputStream in = mProcess.getInputStream();
                    OutputStream out = mProcess.getOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;

                    while ((count = in.read(buffer)) != -1) {
                        mPOut.write(buffer, 0, count);

                        // update the textview in main activity
                        publishProgress();
                    }
                    out.close();
                    in.close();
                    mPOut.close();
                    mPIn.close();
                } finally {
                    mProcess.destroy();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {

            // on each read of the ping response update the textview
            try {
                while (mReader.ready()) {
                    // update the output to the textview
                    mText.setText(mReader.readLine());
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }


}
