// Package declaration for the Android camera example.
package com.example.android_camera_example1;

// Importing necessary Android classes and interfaces.
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

// Camera class that extends Thread for handling camera operations.
public class Camera extends Thread {
    // Tag used for logging. Helps in identifying log messages related to this class.
    private static final String TAG = "CameraHelper";

    // Constants for camera ID and default image dimensions.
    private static final String CAMERA_ID = "0"; // Default camera ID (typically the back camera).
    private static final int DEFAULT_WIDTH = 1920; // Default image width.
    private static final int DEFAULT_HEIGHT = 1080; // Default image height.
    private static final int PRIMARY_PLANE_INDEX = 0;

    // Variables for camera settings and context.
    private Size mPreviewSize; // Holds the size of the camera preview.
    private Context mContext; // Context for accessing system services.
    private CameraDevice mCameraDevice; // Represents the camera device.
    private CaptureRequest.Builder mPreviewBuilder; // Used to build the request for camera preview.
    private CameraCaptureSession mPreviewSession; // Session for camera preview.
    private TextureView mTextureView; // View for displaying the camera preview.
    private Button mCameraCaptureButton; // Button to trigger image capture.
    private FileHelper mFileHelper; // Helper class for file operations.

    // Orientation array to convert from screen rotation to JPEG orientation.
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);

    static {
        // Mapping from screen rotation to JPEG orientation.
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    // Constructor for Camera class.
    public Camera(Context context, TextureView textureView, Button captureButton) {
        mContext = context;
        mTextureView = textureView;
        mCameraCaptureButton = captureButton;
        mFileHelper = new FileHelper(context);

        // Setting an onClick listener for the capture button.
        mCameraCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage(); // Trigger image capture.
            }
        });
    }

    // Method to open the camera. It sets up the camera and connects it.
    public void openCamera() {
        try {
            setupCamera(); // Setup camera settings.
            connectCamera(); // Connect to the camera.
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception: ", e);
        }
    }

    // Setup camera settings like preview size.
    private void setupCamera() throws CameraAccessException {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(CAMERA_ID);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0]; // Get preview sizes and set the preview size.
    }

    // Connect to the camera device.
    private void connectCamera() throws CameraAccessException {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        manager.openCamera(CAMERA_ID, mCameraStateCallback, null);
    }

    // Callbacks for camera state changes like opening, disconnecting, and errors.
    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            startPreview(); // Start the camera preview once the camera is opened.
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "onError");
        }
    };

    // Listener for changes in the texture view surface.
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureAvailable, width=" + width + ", height=" + height);
            openCamera(); // Open the camera when the surface is available.
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Do nothing when the surface texture is updated.
        }
    };

    // Start the camera preview.
    protected void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            Log.e(TAG, "startPreview fail, return");
            return;
        }

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if (null == texture) {
            Log.e(TAG, "texture is null, return");
            return;
        }

        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    mPreviewSession = session;
                    updatePreview(); // Update the preview once the session is configured.
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "Configuration failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "startPreview CameraAccessException: ", e);
        }
    }

    // Update the camera preview.
    protected void updatePreview() {
        Log.d(TAG, "updatePreview called");

        if (null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
            return;
        }

        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "updatePreview CameraAccessException: ", e);
        }
    }

    // Capture an image with the camera.
    protected void captureImage() {
        if (null == mCameraDevice) {
            Log.e(TAG, "mCameraDevice is null, return");
            return;
        }

        try {
            Size[] jpegAvailableSizes = null;
            CameraManager cameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(CAMERA_ID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map != null) {
                jpegAvailableSizes = map.getOutputSizes(ImageFormat.JPEG);
            }
            int width = jpegAvailableSizes != null && jpegAvailableSizes.length > 0 ? jpegAvailableSizes[0].getWidth() : DEFAULT_WIDTH;
            int height = jpegAvailableSizes != null && jpegAvailableSizes.length > 0 ? jpegAvailableSizes[0].getHeight() : DEFAULT_HEIGHT;

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(8);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            HandlerThread thread = new HandlerThread("CameraPicture");
            thread.start();
            final Handler backgroundHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;

                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
                            printCameraCaptureDetails(image);
                            ByteBuffer buffer = image.getPlanes()[PRIMARY_PLANE_INDEX].getBuffer();
                            byte[] data = new byte[buffer.capacity()];
                            buffer.get(data);
                            saveImage(data);
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File not found exception: ", e);
                    } catch (IOException e) {
                        Log.e(TAG, "IO exception: ", e);
                    } finally {
                        if (image != null) {
                            image.close();
                            reader.close();
                        }
                    }
                }
            }, backgroundHandler);

            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                Log.d(TAG, "onCaptureCompleted");
                                super.onCaptureCompleted(session, request, result);

                                final Handler delayPreview = new Handler();
                                delayPreview.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startPreview(); // Call the startPreview method
                                    }
                                }, 500); // Delayed by 1000 milliseconds (1 second)
                            }
                        }, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "captureImage CameraAccessException: ", e);
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(TAG, "Capture session configuration failed");
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "captureImage CameraAccessException: ", e);
        }
    }

    // Log details of the captured image.
    private void printCameraCaptureDetails(Image image) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int imageFormat = image.getFormat();
        Log.d(TAG, "Image captured - Width: " + imageWidth + ", Height: " + imageHeight + ", Format: " + imageFormat);
    }

    // Save the captured image to a file.
    private void saveImage(byte[] bytes) throws IOException {
        String filePath = mFileHelper.saveImage(bytes);
        showToast("Image saved: " + filePath);
        Log.d(TAG, "Image saved at: " + filePath);
    }

    // Show a toast message.
    private void showToast(final String text) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Set a texture listener to the TextureView for camera preview.
    public void setSurfaceTextureListener() {
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    // Resume the camera preview when the activity is resumed.
    public void onResume() {
        Log.d(TAG, "onResume");
        setSurfaceTextureListener();
    }

    // A semaphore to control camera opening and closing.
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    // Close the camera device when the activity is paused.
    public void onPause() {
        Log.d(TAG, "onPause");
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
                Log.d(TAG, "CameraDevice Close");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }
}
