package com.google.ar.core.examples.java.augmentedimage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.examples.java.augmentedimage.base.BaseActivity;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import ja.burhanrashid52.photoeditor.OnPhotoEditorListener;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;
import ja.burhanrashid52.photoeditor.SaveSettings;
import ja.burhanrashid52.photoeditor.ViewType;

public class CropActivity extends BaseActivity{

    Uri mSaveImageUri;
    String loc;
    FirebaseStorage storage;
    StorageReference storageRef;
    PhotoEditor mPhotoEditor;
    private PhotoEditorView mPhotoEditorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        mPhotoEditorView = findViewById(R.id.photoEditorView);

        mPhotoEditorView.getSource().setImageResource(R.drawable.got);
        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
                .build();
        Intent intent = getIntent();
        mSaveImageUri = Uri.parse(intent.getStringExtra(EditImageActivity.URI));
        ImageView imgV = findViewById(R.id.quick_start_cropped_image);
        imgV.setImageURI(mSaveImageUri);
        loc = intent.getStringExtra(EditImageActivity.LOCATION);
        storage  = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    /** Start pick image activity with chooser. */
    public void onSelectImageClick(View view) {
        CropImage.activity(mSaveImageUri)
                .setActivityTitle("My Crop").setMinCropResultSize(400,400)
                .setMaxCropResultSize(400,400)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setCropMenuCropButtonTitle("Done")
                .setCropMenuCropButtonIcon(R.drawable.ic_launcher)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // handle result of CropImageActivity
        super.onActivityResult(requestCode, resultCode, data);
        showLoading("Loading...");
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap restbitmap;// = ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), resultUri));

                try {
                    restbitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                    File file = new File(Environment.getExternalStorageDirectory()
                            + File.separator + ""
                            + System.currentTimeMillis() + ".png");
                    file.createNewFile();
                    GlideApp.with(this)
                            .asBitmap()
                            .load(storageRef.child(loc))
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                    try {
                                        Bitmap bitmap = gridize(resource,restbitmap);
                                        ImageView imgV = findViewById(R.id.quick_start_cropped_image);
                                        imgV.setImageBitmap(bitmap);
                                                mPhotoEditorView.getSource().setImageBitmap(bitmap);
                                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
                                        byte[] byteArray = stream.toByteArray();

                                        FileOutputStream fileOuputStream =
                                                new FileOutputStream(new File(file.getAbsolutePath()),false);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 0, fileOuputStream);
                                        fileOuputStream.flush();
                                        fileOuputStream.close();
                                        Uri uri =Uri.fromFile(new File(file.getAbsolutePath()));;
                                        UploadTask uploadTask = storageRef.child(loc).putFile(uri);

// Register observers to listen for when the download is done or if it fails
                                        uploadTask.addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception exception) {
                                                // Handle unsuccessful uploads
                                                Log.d("FAILURE","bad");
                                            }
                                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                hideLoading();
                                                Intent intent = new Intent(CropActivity.this, MainActivity.class);
                                                startActivity(intent);
                                            }
                                        });

                                    }catch(Exception e){
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                }
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                



            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static Bitmap gridize(Bitmap orig, Bitmap add){
        int d1 = new Random().nextInt(3)*400;
        int d2 =new Random().nextInt(3)*400;
        Rect rect = new Rect(0,0,400,400);
        Canvas canvas = new Canvas(orig);
        canvas.drawBitmap(add,null,rect,null);
        return orig;
    }

}
