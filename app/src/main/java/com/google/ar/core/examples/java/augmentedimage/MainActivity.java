package com.google.ar.core.examples.java.augmentedimage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.augmentedimage.models.Location;
import com.google.ar.core.examples.java.augmentedimage.models.Mural;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

//TODO: Implement DatabaseReference and StorageReference, load all items in database reference, database reference should hold uri of corresponding image in firebase storage
public class MainActivity extends AppCompatActivity {
    final long ONE_MEGABYTE = 1024 * 1024;
    private static final String TAG = MainActivity.class.getSimpleName();

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private ArFragment arFragment;
    private ArSceneView arSceneView;

    private boolean installRequested;

    private Session session;
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();

    private boolean shouldConfigureSession = false;

    private Button drawButton;
    private boolean drawButtonVisible = false;

    private DatabaseReference mDatabase;

    private FirebaseStorage mImages;

    private ArrayList<Mural> murals = new ArrayList<>();

    private String currRefImg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.surfaceview);

        arFragment.getPlaneDiscoveryController().hide();
        arFragment.getPlaneDiscoveryController().setInstructionView(null);

        arSceneView = arFragment.getArSceneView();

//        arSceneView = findViewById(R.id.surfaceview);

        installRequested = false;

        initializeSceneView();

        setupDrawButton();

        setupStorage();

        setupDatabase();

    }

    private void setupDrawButton() {
        drawButton = (Button) findViewById(R.id.drawButton);
        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //callback when button is clicked
                Intent intent = new Intent(MainActivity.this, EditImageActivity.class).putExtra("refImg", currRefImg);
                startActivity(intent); //opens drawactivity
            }
        });
    }

    private void setupDatabase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference imageDatabase = mDatabase.child("murals");

        imageDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Database value: " + snapshot.getValue());
                if(snapshot.hasChildren()) {
                    for(DataSnapshot mural : snapshot.getChildren()) {
                        String arImg = (String) mural.child("arImg").getValue();
                        String refImg = (String) mural.child("refImg").getValue();
                        String name = (String) mural.child("name").getValue();
                        double lat = (double) mural.child("location").child("lat").getValue();
                        double lon = (double) mural.child("location").child("lon").getValue();

                        Mural mur = new Mural(name, refImg, arImg, new Location(lat, lon));
                        murals.add(mur);
                    }
                }
                for(Mural mural : murals) {
                    Log.d(TAG, "Mural Name: " + mural.getName());
                }

                Log.d(TAG, "Running configure session from database");
                configureSession();
                shouldConfigureSession = false;
                arSceneView.setupSession(session);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //Pushes to database
//        Mural test = new Mural("Super Awesome Mural", "hi", "bye", new Location(5.0, 10.0));
//        DatabaseReference testRef = imageDatabase.push();
//        testRef.setValue(test);
    }

    private void setupStorage() {
        mImages = FirebaseStorage.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                session = new Session(/* context = */ this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            shouldConfigureSession = true;
        }

        if (shouldConfigureSession) {
            configureSession();
            shouldConfigureSession = false;
            arSceneView.setupSession(session);
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
            arSceneView.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            messageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            session = null;
            return;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            arSceneView.pause();
            session.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(
                    this, "Camera permissions are needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    private void initializeSceneView() {
        arSceneView.getScene().setOnUpdateListener((this::onUpdateFrame));
    }

    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = arSceneView.getArFrame();
        Collection<AugmentedImage> updatedAugmentedImages =
                frame.getUpdatedTrackables(AugmentedImage.class);

        boolean foundMatch = false;

        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            if (augmentedImage.getTrackingState() == TrackingState.TRACKING) {
                //for mural if augmented.equals mural.getname
                // Check camera image matches our reference image


                for(Mural mural : murals) {
                    if (augmentedImage.getName().equals(mural.getRefImg())) {
                        foundMatch = true;
//                    AugmentedImageNode node = new AugmentedImageNode(this, "model.sfb");
//                    node.setImage(augmentedImage);
//                    arSceneView.getScene().addChild(node);

                        //LinearLayOut Setup
                        LinearLayout linearLayout = new LinearLayout(this);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);

                        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT));

                        ImageView imageView = new ImageView(this);

                        //imageView.setImageResource(R.drawable.delorean);
                        imageView.setLayoutParams(new LinearLayout.LayoutParams(500,
                                500));

                        linearLayout.addView(imageView);

                        StorageReference storageReference = mImages.getReference(mural.getArImg());
                        //Glide.with(this).load(storageReference).into(imageView);

                        storageReference.getBytes(2 * ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap augmentedImageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                imageView.setImageBitmap(augmentedImageBitmap);
                                Log.d(TAG, "Got Bitmap");

                                renderObject(arFragment, augmentedImage.createAnchor(augmentedImage.getCenterPose()), linearLayout);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG, e.toString());
                                e.printStackTrace();
                            }
                        });

                    }
                }
            }
        }

        //Note draw button doesn't go away after initially appearing
            if (foundMatch) {
                if (!drawButtonVisible) {
                    drawButton.setVisibility(View.VISIBLE);
                    drawButtonVisible = true;
                }
            }
    }

//    private void renderObject(ArFragment fragment, Anchor anchor, int model) {
//        ViewRenderable.builder().setView(this, model).build().thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable));
//    }

    private void renderObject(ArFragment fragment, Anchor anchor, View view) {
        ViewRenderable.builder().setView(this, view).build().thenAccept(renderable -> addNodeToScene(fragment, anchor, renderable));
    }


    private void addNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable){
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);

        node.setLocalRotation(Quaternion.axisAngle(new Vector3(-1f, 0, 0), 90f));
        arSceneView.getScene().addChild(anchorNode);
        node.select();
    }

    private void configureSession() {
        Log.d(TAG, "In configure session");
        Config config = new Config(session);
        if (!setupAugmentedImageDb(config)) {
            messageSnackbarHelper.showError(this, "Could not setup augmented image database");
        }

    }

    private boolean setupAugmentedImageDb(Config config) {
        AugmentedImageDatabase augmentedImageDatabase;
        augmentedImageDatabase = new AugmentedImageDatabase(session);



        Iterator<Mural> iter = murals.iterator();
        while(iter.hasNext()) {
            Log.d(TAG, "In iter");
            Mural mural = iter.next();
            StorageReference imageRef = mImages.getReference(mural.getRefImg());
            imageRef.getBytes(2*ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap augmentedImageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                    if (augmentedImageBitmap == null) {
//                        return false;
//                    }
                    Log.d(TAG, "Adding image to augmented database: " + mural.getRefImg());
                    augmentedImageDatabase.addImage(mural.getRefImg(), augmentedImageBitmap);

//                    DisplayMetrics dm = new DisplayMetrics();
//                    getWindowManager().getDefaultDisplay().getMetrics(dm);
//                    ImageView imageView = findViewById(R.id.imageView);
//                    imageView.setMinimumHeight(dm.heightPixels);
//                    imageView.setMinimumWidth(dm.widthPixels);
//                    imageView.setImageBitmap(augmentedImageBitmap);

                    if(!iter.hasNext()) {
                        Log.d(TAG, "Setting image database");
                        config.setAugmentedImageDatabase(augmentedImageDatabase);
                        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                        session.configure(config);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                }
            });
        }

        return true;
    }

//    private boolean setupAugmentedImageDb(Config config) {
//        AugmentedImageDatabase augmentedImageDatabase;
//
//        Bitmap augmentedImageBitmap = loadAugmentedImage();
//        if (augmentedImageBitmap == null) {
//            return false;
//        }
//
//        augmentedImageDatabase = new AugmentedImageDatabase(session);
//        augmentedImageDatabase.addImage("delorean", augmentedImageBitmap);
//
//        config.setAugmentedImageDatabase(augmentedImageDatabase);
//        return true;
//    }
//
//    private Bitmap loadAugmentedImage() {
//        try (InputStream is = getAssets().open("delorean.jpg")) {
//            return BitmapFactory.decodeStream(is);
//        } catch (IOException e) {
//            Log.e(TAG, "IO exception loading augmented image bitmap.", e);
//        }
//        return null;
//    }
}
