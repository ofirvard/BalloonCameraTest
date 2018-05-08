package com.example.ofir.ballooncameratest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
{
    // TODO: 4/18/2018 add upload 
    Context context;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    final int COUNTDOWN_MAX = 10;
    int countdown = COUNTDOWN_MAX;
    CameraView cameraView;
    TextView timer;
    GPSTracker gpsTracker;
    boolean keepTakingPictures = false;
    boolean passed2k = false;
    final Handler handler = new Handler();
    final Runnable pictureTaker = new Runnable()
    {
        @Override
        public void run()
        {
            if (keepTakingPictures)
            {
                if (countdown == 0)
                {
                    cameraView.captureImage();
                    countdown = COUNTDOWN_MAX;
                }
                countdown--;
                String s = "" + countdown;
                timer.setText(s);
                handler.postDelayed(pictureTaker, 1000);
            }
            else
            {
                countdown = COUNTDOWN_MAX;
                String s = "" + countdown;
                timer.setText(s);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,}
                , REQUEST_CAMERA_PERMISSION);
        context = this;
        gpsTracker = new GPSTracker(this);
        cameraView = findViewById(R.id.camera_view);
        timer = findViewById(R.id.timer);
        ((TextView) findViewById(R.id.timer)).setText("hello");
        cameraView.addCameraKitListener(new CameraKitEventListener()
        {
            @Override
            public void onImage(CameraKitImage cameraKitImage)
            {
                String timeStamp = new SimpleDateFormat("HH-mm-ss_dd-MM-yyyy", Locale.ENGLISH).format(new Date());
                String imageFileName = "";
                imageFileName += timeStamp;
                if (gpsTracker.canGetLocation())
                {
                    imageFileName += "_lat-" + gpsTracker.getLatitude() + "_lon-" + gpsTracker.getLongitude();
                    if (gpsTracker.getLocation().hasAltitude())
                    {
                        double alt = gpsTracker.getLocation().getAltitude();
                        imageFileName += "_alt-" + alt;

                        if (!passed2k && alt > 2000)
                            passed2k = true;

                        if (passed2k && alt < 2000)
                            keepTakingPictures = false;
                    }
                }
                imageFileName += ".jpg";
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), imageFileName);
                if (!file.exists())
                {
                    try
                    {
                        file.createNewFile();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                OutputStream outStream = null;
                try
                {
                    outStream = new FileOutputStream(file);
                    cameraKitImage.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    outStream.flush();
                    outStream.close();

                    Toast.makeText(context, "Saved " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                } catch (IOException e)
                {
                    Toast.makeText(context, "Failed to save " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(CameraKitError cameraKitError)
            {

            }

            @Override
            public void onEvent(CameraKitEvent cameraKitEvent)
            {

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo)
            {

            }
        });
    }

    public void takePictures(View view)
    {
        cameraView.captureImage();

        if (!keepTakingPictures)
        {
            countdown = Integer.parseInt(((EditText) findViewById(R.id.editText)).getText().toString());
//            timer.setText("start");
            keepTakingPictures = true;
//            Toast.makeText(context, "Started taking pictures", Toast.LENGTH_SHORT).show();
            handler.post(pictureTaker);
        }
    }

    public void stopTakingPictures(View view)
    {
//        timer.setText("stop");

        keepTakingPictures = false;
//        Toast.makeText(context, "Stopped taking pictures", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause()
    {
        cameraView.stop();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED)
            {
                // close the app
                Toast.makeText(this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
