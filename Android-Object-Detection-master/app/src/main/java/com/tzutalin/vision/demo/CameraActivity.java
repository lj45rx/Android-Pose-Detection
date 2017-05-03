/*
* verifies necessary permissions to use camera
* also initializes loading of feature matrix at the start of the program
*/
package com.tzutalin.vision.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.tzutalin.vision.visionrecognition.IAP_Helper;
import com.tzutalin.vision.visionrecognition.R;

public class CameraActivity extends Activity {

    private static final int REQUEST_CODE_PERMISSION = 1;

    private static final String TAG = "CameraActivity";

    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //start loading feature matrix
        Log.i(TAG, "MAXIAP: start loading matrix");
        long startTime = System.currentTimeMillis();
        IAP_Helper.loadMatrix(4);
        long endTime = System.currentTimeMillis();
        final double diffTime = (double) (endTime - startTime) / 1000;
        Log.i(TAG, "MAXIAP: loading matrix took: " + diffTime);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        boolean avialbe_permission = true;
        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.M ) {
            avialbe_permission = verifyPermissions(this);
        }

        if (avialbe_permission && null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
    }

    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        try {
            // Restart it after granting permission
            if (requestCode == REQUEST_CODE_PERMISSION) {
                finish();
                startActivity(getIntent());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }
}
