package com.example.android_camera_example1;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

// FileHelper class for handling file operations using MediaStore, specifically for saving images.
public class FileHelper {
    private Context mContext;

    // Constructor for the FileHelper class.
    public FileHelper(Context context) {
        mContext = context;
    }

    // Method to save an image byte array using MediaStore.
    public String saveImage(byte[] bytes) throws IOException {
        // Generate a unique file name using the current timestamp.
        String fileName = createFileName();

        // Create a ContentValues object to hold metadata about the file.
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        // Get the ContentResolver and insert the file into MediaStore.
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Failed to create new MediaStore record.");
        }

        // Write the byte array to the file using the obtained OutputStream.
        writeToFile(resolver, uri, bytes);

        // Return the absolute path of the saved file.
        return uri.toString();
    }

    // Create a unique file name using a timestamp.
    private String createFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return "IMG_" + dateFormat.format(new Date()) + ".jpg";
    }

    // Write the byte array to the specified Uri using the ContentResolver.
    private void writeToFile(ContentResolver resolver, Uri uri, byte[] bytes) throws IOException {
        try (OutputStream output = resolver.openOutputStream(uri)) {
            if (output != null) {
                output.write(bytes);
            } else {
                throw new IOException("Failed to obtain OutputStream from MediaStore.");
            }
        }
    }
}
