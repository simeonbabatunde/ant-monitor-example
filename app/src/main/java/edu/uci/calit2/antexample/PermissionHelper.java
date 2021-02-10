package edu.uci.calit2.antexample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Helper class to get all the required permissions
 * @author Dhananjay Nikam
 */


public class PermissionHelper {
    private static final String TAG = PermissionHelper.class.getSimpleName();

    public static final int PERMISSION_REQUEST = 1;

    public static boolean requestPermissions(Activity activity) {

        boolean hasRequiredPermissions = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                    activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED ||
                    activity.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED ||
                    activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                    activity.checkSelfPermission(Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_DENIED)
            {
                String[] permissions = {
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.GET_ACCOUNTS
                };
                hasRequiredPermissions = false;
                activity.requestPermissions(permissions, PERMISSION_REQUEST);
            }

        }

        return hasRequiredPermissions;
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode,
                                                  String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted for location");
                } else {
                    // TODO: Figure out what to do if we do not get permission
                    Log.d(TAG, "Permission denied for location");
                }
                return;
            }
        }
    }

}
