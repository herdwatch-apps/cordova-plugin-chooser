package com.cyph.cordova;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import android.content.ClipData;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class Chooser extends CordovaPlugin {
    private static final String ACTION_OPEN = "getFile";
    private static final int PICK_FILE_REQUEST = 1;
    private static final String TAG = "Chooser";

    private CallbackContext callback;

    private JSONObject pluginArgs;
    private int maxFileSize = 0;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if (action.equals(Chooser.ACTION_OPEN)) {
                this.chooseFile(callbackContext, args);
                return true;
            }
        } catch (JSONException err) {
            this.callback.error("Execute failed: " + err.toString());
        }

        return false;
    }

    public void chooseFile(CallbackContext callbackContext, JSONArray args) throws JSONException {
        JSONObject options = args.optJSONObject(0);
        this.pluginArgs = options;
        String mimeTypes = options.optString("mimeTypes");
        int maxFileSize = options.optInt("maxFileSize");

        boolean allowMultiple = options.optBoolean("allowMultiple");

        if (maxFileSize != 0) {
            this.maxFileSize = maxFileSize;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        if (!mimeTypes.equals("*/*")) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.split(","));
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, Chooser.PICK_FILE_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        this.callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        try {
//            if (requestCode == Chooser.PICK_FILE_REQUEST && this.callback != null) {
//                if (resultCode == Activity.RESULT_OK) {
//                    Uri uri = data.getData();
//                    if (uri != null) {
//                        Activity activity = cordova.getActivity();
//                        ContentResolver contentResolver = activity.getContentResolver();
//                        InputStream inputStream = contentResolver.openInputStream(uri);
//                        String displayName = "File";
//                        String uriString = uri.toString();
//                        String mimeType = null;
//                        String extension = null;
//                        String filePath = null;
//
//                        int size = inputStream.available();
//                        if (this.maxFileSize != 0) {
//                            if (size > this.maxFileSize) {
//                                this.callback.error("Invalid size");
//                                return;
//                            }
//                        }
//
//                        if (uriString.startsWith("content://")) {
//                            Cursor cursor = null;
//                            try {
//                                cursor = contentResolver.query(uri, null, null, null, null);
//                                if (cursor != null && cursor.moveToFirst()) {
//                                    displayName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
//                                }
//                                mimeType = contentResolver.getType(uri);
//                            } finally {
//                                assert cursor != null;
//                                cursor.close();
//                            }
//                        } else if (uriString.startsWith("file://")) {
//                            displayName = new File(uriString).getName();
//                            String[] parts = uriString.split("\\.");
//                            String ext = parts[parts.length - 1];
//                            if (ext != null) {
//                                MimeTypeMap mime = MimeTypeMap.getSingleton();
//                                mimeType = mime.getMimeTypeFromExtension(ext);
//                            }
//                        }
//
//                        if (mimeType != null) {
//                            MimeTypeMap mime = MimeTypeMap.getSingleton();
//                            extension = mime.getExtensionFromMimeType(mimeType);
//                        }
//
//                        if (mimeType == null || mimeType.isEmpty()) {
//                            mimeType = "application/octet-stream";
//                        }
//
//                         filePath = activity.getCacheDir().getAbsolutePath() + '/' + displayName;
//                         // copyInputStreamToFile(inputStream, filePath);
//
//                        try {
//                            copyInputStreamToFileAsync(uri, filePath,  this.getFileName(displayName), mimeType, extension, size,  this.callback);
//                            JSONObject result = new JSONObject();
//                            //result.put("path", uri.toString());
//                            result.put("path", new File(filePath).exists() ? "file://" + filePath : "");
//                            result.put("name",  this.getFileName(displayName)); // without extension
//                            result.put("displayName",  displayName); // with extension
//                            result.put("mimeType", mimeType);
//                            result.put("extension", extension);
//                            result.put("size", size);
//
//                           // this.callback.success(result);
//                        } catch (JSONException e) {
//                            this.callback.error("JSON Object not supported");
//                        }
//
//                    } else {
//                        this.callback.error("File URI was null.");
//                    }
//                } else if (resultCode == Activity.RESULT_CANCELED) {
//                    this.callback.success("RESULT_CANCELED");
//                } else {
//                    this.callback.error(resultCode);
//                }
//            }
//        } catch (Exception err) {
//            this.callback.error("Failed to read file: " + err.toString());
//        }
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (requestCode == Chooser.PICK_FILE_REQUEST && this.callback != null) {
                if (resultCode == Activity.RESULT_OK) {
                    Activity activity = cordova.getActivity();
                    ContentResolver contentResolver = activity.getContentResolver();

                    JSONArray resultsArray = new JSONArray();

                    // --- Multiple files case ---
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            JSONObject fileObj = processUri(activity, contentResolver, uri);
                            if (fileObj != null) {
                                resultsArray.put(fileObj);
                            }
                        }
                    }
                    // --- Single file case ---
                    else if (data.getData() != null) {
                        Uri uri = data.getData();
                        JSONObject fileObj = processUri(activity, contentResolver, uri);
                        if (fileObj != null) {
                            resultsArray.put(fileObj);
                        }
                    }

                    // Return array of results
                    this.callback.success(resultsArray);

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    this.callback.success("RESULT_CANCELED");
                } else {
                    this.callback.error(resultCode);
                }
            }
        } catch (Exception err) {
            this.callback.error("Failed to read file: " + err.toString());
        }
    }

    /**
     * Helper method to process a single Uri into a JSONObject
     */
    private JSONObject processUri(Activity activity, ContentResolver contentResolver, Uri uri) {
        try {
            if (uri == null) return null;

            InputStream inputStream = contentResolver.openInputStream(uri);
            String displayName = "File";
            String uriString = uri.toString();
            String mimeType = null;
            String extension = null;
            String filePath = null;

            long size = -1;

            if (this.maxFileSize != 0 && size > this.maxFileSize) {
                this.callback.error("Invalid size");
                return null;
            }

            if (uriString.startsWith("content://")) {
                Cursor cursor = null;
                try {
                    cursor = contentResolver.query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex);
                        }

                    }
                    mimeType = contentResolver.getType(uri);
                } finally {
                    if (cursor != null) cursor.close();
                }
            } else if (uriString.startsWith("file://")) {
                File file = new File(Objects.requireNonNull(uri.getPath()));
                size = file.length();

                displayName = new File(uriString).getName();
                String[] parts = uriString.split("\\.");
                String ext = parts[parts.length - 1];
                if (ext != null) {
                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                    mimeType = mime.getMimeTypeFromExtension(ext);
                }
            }

            if (mimeType != null) {
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                extension = mime.getExtensionFromMimeType(mimeType);
            }

            if (mimeType == null || mimeType.isEmpty()) {
                mimeType = "application/octet-stream";
            }

            filePath = activity.getCacheDir().getAbsolutePath() + '/' + displayName;
            String finalFilePath;
            if(!this.pluginArgs.getBoolean("rawPath")) {
                // Copy file asynchronously
                copyInputStreamToFileAsync(uri, filePath, this.getFileName(displayName), mimeType, extension, size, this.callback);
                finalFilePath = new File(filePath).exists() ? "file://" + filePath : "";
            }else{
                finalFilePath = uriString;
            }
            JSONObject result = new JSONObject();
            result.put("path", new File(filePath).exists() ? "file://" + filePath : "");

            // Add raw path if requested.
            if(this.pluginArgs.getBoolean("rawPath")){
                result.put("rawPath", finalFilePath);
            }

            result.put("name", this.getFileName(displayName)); // without extension
            result.put("displayName", displayName);            // with extension
            result.put("mimeType", mimeType);
            result.put("extension", extension);
            result.put("size", size);

            return result;
        } catch (Exception e) {
            this.callback.error("Error processing file: " + e.toString());
            return null;
        }
    }

    private void copyInputStreamToFileAsync(
            final Uri uri,
            final String filePath,
            final String displayName,
            final String mimeType,
            final String extension,
            final long size,
            final CallbackContext callback
    ) {
        new Thread(() -> {
            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            try {
                File outFile = new File(cordova.getActivity().getCacheDir(), displayName);
                ParcelFileDescriptor pfd = cordova.getActivity().getContentResolver().openFileDescriptor(uri, "r");
                if (pfd == null) {
                    runOnUi(() -> callback.error("Unable to open file descriptor"));
                    return;
                }

                inputChannel = new FileInputStream(pfd.getFileDescriptor()).getChannel();
                outputChannel = new FileOutputStream(outFile).getChannel();

                long transferred = inputChannel.transferTo(0, inputChannel.size(), outputChannel);



                JSONObject result = new JSONObject();
                result.put("path", outFile.exists() ? "file://" + filePath : "");
                result.put("pathRaw", uri.toString());
                result.put("name", getFileName(displayName)); // without extension
                result.put("displayName", displayName);       // with extension
                result.put("mimeType", mimeType != null ? mimeType : "application/octet-stream");
                result.put("extension", extension != null ? extension : "");
                result.put("size", size > 0 ? size : outFile.length());

                runOnUi(() -> callback.success(result));

            } catch (OutOfMemoryError oom) {
                runOnUi(() -> callback.error("Memory error during file copy. Try smaller file or free up memory."));
            } catch (IOException io) {
                runOnUi(() -> callback.error("I/O error: " + io.getMessage()));
            } catch (Exception e) {
                runOnUi(() -> callback.error("Unexpected error: " + e.toString()));
            } finally {
                try {
                    if (inputChannel != null) inputChannel.close();
                    if (outputChannel != null) outputChannel.close();
                } catch (IOException e) {
                    runOnUi(() -> callback.error("Error closing channels: " + e.getMessage()));
                }
            }
        }).start();


    }

    // Utility to run on UI thread
    private void runOnUi(Runnable runnable) {
        cordova.getActivity().runOnUiThread(runnable);
    }

    public String getFileName(String fileName) {
        if(!fileName.contains(".")){
            return fileName;
        }
        return fileName.substring(0,fileName.lastIndexOf("."));
    }
    
}
