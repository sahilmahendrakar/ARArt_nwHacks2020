package com.google.ar.core.examples.java.augmentedimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.examples.java.augmentedimage.base.BaseActivity;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class CropActivity extends BaseActivity {

    Uri mSaveImageUri;
    String loc;
    FirebaseStorage storage;
    StorageReference storageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        Intent intent = getIntent();
        mSaveImageUri = Uri.parse(intent.getStringExtra(EditImageActivity.URI));
        loc = intent.getStringExtra(EditImageActivity.LOCATION);
        storage  = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        CropImage.activity(mSaveImageUri)
                .setActivityTitle("My Crop").setMinCropResultSize(400,400)
                .setMaxCropResultSize(400,400)
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setCropMenuCropButtonTitle("Done")
                .setCropMenuCropButtonIcon(R.drawable.ic_launcher)
                .start(CropActivity.this);
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
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                StorageReference riversRef = storageRef.child(loc);
                UploadTask uploadTask = riversRef.putFile(resultUri);
                showLoading("Saving...");

// Register observers to listen for when the download is done or if it fails
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        hideLoading();
                        showSnackbar("Image Save Failed");
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Intent intent = new Intent(CropActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Cropping failed: " + result.getError(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static Bitmap gridize(Bitmap orig, Bitmap add){
        Rect rect = new Rect(0,0,400,400);
        Canvas canvas = new Canvas(orig);
        canvas.drawBitmap(add,null,rect,null);
        return orig;
    }

}
