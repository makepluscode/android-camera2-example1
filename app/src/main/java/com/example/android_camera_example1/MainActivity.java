package com.example.android_camera_example1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// MainActivity class for handling the camera application's main screen.
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AndroidCameraExample";
    private static final int REQUEST_CAMERA_PERMISSION = 1; // Request code for camera permission.

    private Camera mCamera; // Instance of the Camera class to handle camera operations.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Set the main layout as the content view.

        // Check and request necessary permissions.
        if (!hasRequiredPermissions()) {
            requestNecessaryPermissions();
        }

        // Initialize UI components.
        initializeComponents();
    }

    // Method to initialize UI components and set up the camera.
    private void initializeComponents() {
        TextureView cameraTextureView = findViewById(R.id.cameraTextureView);
        Button captureButton = findViewById(R.id.capture_button);

        // Initialize the Camera with the TextureView and button.
        mCamera = new Camera(this, cameraTextureView, captureButton);
    }

    // Check if the app has required permissions.
    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Request necessary permissions for the app.
    private void requestNecessaryPermissions() {
        ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    // Handle the result of the permission request.
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            // Check if the permissions were granted.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mCamera.openCamera(); // Open the camera if permission is granted.
                Log.d(TAG, "Camera permission granted");
            } else {
                Toast.makeText(this, "Camera permission is required to use this app.", Toast.LENGTH_LONG).show();
                finish(); // Close the app if permission is denied.
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera.onResume(); // Resume camera preview when the activity resumes.
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.onPause(); // Pause camera preview when the activity pauses.
    }
}
