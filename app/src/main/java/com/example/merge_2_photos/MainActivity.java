package com.example.merge_2_photos;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import id.zelory.compressor.Compressor;

public class MainActivity extends AppCompatActivity {
    public static final int RESULT_LOAD_IMG = 1;
    private ImageView imageView, imageOutput;
    private EditText txtHeight, txtWidth;
    private TextView txtOriginalSize, txtResultSize, txtQuality;
    private SeekBar seekBar;
    private Button btnCompress, btnPick;
    private File compressedImage, originalImage;
    private Uri originalImageUri;
    private static String filePath;
    private File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/eCompressor");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askPermission();
        try {
            setupVersionInfo();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        imageView = findViewById(R.id.imageHolder);
        imageOutput = findViewById(R.id.imageOutput);
        btnCompress = findViewById(R.id.btnCompress);
        btnPick = findViewById(R.id.btnPick);
        txtOriginalSize = findViewById(R.id.txtOriginalSize);
        txtResultSize = findViewById(R.id.txtResultSize);
        txtHeight = findViewById(R.id.txtHeight);
        txtWidth = findViewById(R.id.txtWidth);
        txtQuality = findViewById(R.id.txtQuality);
        seekBar = findViewById(R.id.seekQuality);

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String date = format.format(new Date());
        filePath = path.getAbsolutePath();

        if (!path.exists()) {
            path.mkdirs();
        }

        seekBar.setProgress(40);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txtQuality.setText("Quality: " + i + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        btnPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGallery();
            }
        });

        btnCompress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isValidParams()) {
                    try {
                        int quality = seekBar.getProgress();
                        int width = Integer.parseInt(txtWidth.getText().toString());
                        int height = Integer.parseInt(txtHeight.getText().toString());
                        compressedImage = new Compressor(MainActivity.this)
                                .setMaxHeight(height)
                                .setMaxWidth(width)
                                .setQuality(quality)
                                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                                .setDestinationDirectoryPath(filePath)
                                .compressToFile(originalImage);
                        File finalFile = new File(filePath, originalImage.getName());
                        Bitmap myBitmap = BitmapFactory.decodeFile(finalFile.getAbsolutePath());
                        imageOutput.setImageBitmap(myBitmap);
                        txtResultSize.setText("Size: " + Formatter.formatShortFileSize(MainActivity.this, finalFile.length()));
                        Toast.makeText(MainActivity.this, "Compressed & Saved to " + filePath, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("MyApp", "Error during compression: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Error during compression", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void askPermission() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        // Permission granted
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                })
                .withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(MainActivity.this, "Error occurred", Toast.LENGTH_SHORT).show();
                    }
                })
                .check();
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, RESULT_LOAD_IMG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                btnCompress.setVisibility(View.VISIBLE);
                originalImageUri = data.getData();
                Log.d("MyApp", "Original Image URI: " + originalImageUri);

                if (originalImageUri != null) {
                    final InputStream imageStream = getContentResolver().openInputStream(originalImageUri);

                    if (imageStream != null) {
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

                        if (selectedImage != null) {
                            originalImage = new File(originalImageUri.getPath());
                            imageView.setImageBitmap(selectedImage);
                            txtOriginalSize.setText("Original Size: " + Formatter.formatShortFileSize(this, originalImage.length()));
                        } else {
                            Log.e("MyApp", "Error: selectedImage is null");
                            Toast.makeText(MainActivity.this, "Error loading image", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e("MyApp", "Error: imageStream is null");
                        Toast.makeText(MainActivity.this, "Error loading image", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("MyApp", "Error: originalImageUri is null");
                    Toast.makeText(MainActivity.this, "Error loading image", Toast.LENGTH_LONG).show();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("MyApp", "Error loading image: " + e.getMessage());
                Toast.makeText(MainActivity.this, "Error loading image", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(MainActivity.this, "You haven't picked Image", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidParams() {
        boolean validity = false;
        if (!txtHeight.getText().toString().isEmpty() && !txtWidth.getText().toString().isEmpty()) {
            validity = true;
        } else {
            if (txtHeight.getText().toString().isEmpty()) {
                Toast.makeText(this, "Enter Height!", Toast.LENGTH_SHORT).show();
                validity = false;
            } else if (txtWidth.getText().toString().isEmpty()) {
                Toast.makeText(this, "Enter Width!", Toast.LENGTH_SHORT).show();
                validity = false;
            }
        }
        return validity;
    }

    private void setupVersionInfo() throws PackageManager.NameNotFoundException {
        PackageInfo mPackageInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
        if (mPackageInfo != null) {
            TextView versionInfoTextView = findViewById(R.id.versionInfoTextView);
            String vinfo = String.format("V: %s", mPackageInfo.versionName);
            versionInfoTextView.setText(vinfo);
        }
    }
}
