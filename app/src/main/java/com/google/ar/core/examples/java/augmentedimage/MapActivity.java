package com.google.ar.core.examples.java.augmentedimage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Iterator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.examples.java.augmentedimage.models.Location;
import com.google.ar.core.examples.java.augmentedimage.models.Mural;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
//import com.google.firebase.quickstart.database.databinding.ActivityNewPostBinding;
//import com.google.firebase.quickstart.database.java.models.Post;
//import com.google.firebase.quickstart.database.java.models.User;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;

/**
 * An activity that displays a Google map with a marker (pin) to indicate a particular location.
 */
// [START maps_marker_on_map_ready]
public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        OnMarkerClickListener {

    private DatabaseReference mDatabase;
    private static final String TAG = MapActivity.class.getSimpleName();

    private HashMap<Marker, Mural> markerMap = new HashMap<>();

    final long ONE_MEGABYTE = 1024 * 1024;

    // [START_EXCLUDE]
    // [START maps_marker_get_map_async]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_map);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();
    }
    // [END maps_marker_get_map_async]
    // [END_EXCLUDE]

    // [START_EXCLUDE silent]
    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    // [END_EXCLUDE]
    // [START maps_marker_on_map_ready_add_marker]
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // [START_EXCLUDE silent]
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.
        // [END_EXCLUDE]
        LatLng sydney = new LatLng(40.807537, -73.96257);
        createMarkers(googleMap);

        googleMap.setOnMarkerClickListener(this);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 13));
        // [END_EXCLUDE]
    }
    // [END maps_marker_on_map_ready_add_marker]

//    public void clickMarker(final Marker marker) {
//        final SlidingUpPanelLayout slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
//        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
//
//        TextView textView = findViewById(R.id.slide_panel);
//        textView.setText(marker.getTitle());
//    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        // This causes the marker to bounce into position when it is clicked.
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed / duration), 0);
                marker.setAnchor(0.5f, 1.0f + 2 * t);

                if (t > 0.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });

        // pulls up the info panel
        final SlidingUpPanelLayout slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setAnchorPoint((float)0.30);
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);

        TextView textView = findViewById(R.id.slide_panel_title);
        textView.setText(marker.getTitle());


        ImageView imageView = findViewById(R.id.mapImageView);
        imageView.setVisibility(View.INVISIBLE);

        Mural mural = markerMap.get(marker);

        StorageReference storageReference = FirebaseStorage.getInstance().getReference(mural.getRefImg());

        storageReference.getBytes(2 * ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap augmentedImageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView.setImageBitmap(augmentedImageBitmap);
                imageView.setVisibility(View.VISIBLE);
                Log.d(TAG, "Got Bitmap");

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, e.toString());
                e.printStackTrace();
            }
        });

        // We return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    public void goCamera(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void createMarkers(GoogleMap googleMap) {
        DatabaseReference imageDatabase = mDatabase.child("murals");

        imageDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "Reading data"+ snapshot.getValue());
                if(snapshot.hasChildren()){
                    Iterator<DataSnapshot> iter = snapshot.getChildren().iterator();
                    while (iter.hasNext()){
                        DataSnapshot snap = iter.next();
                        DataSnapshot location = snap.child("location");
                        LatLng loc = new LatLng((double)location.child("lat").getValue(), (double)location.child("lon").getValue());

                        Marker marker = googleMap.addMarker(new MarkerOptions()
                                .position(loc)
                                .title(snap.child("name").getValue().toString())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));

                        String arImg = (String) snap.child("arImg").getValue();
                        String refImg = (String) snap.child("refImg").getValue();
                        String name = (String) snap.child("name").getValue();
                        double lat = (double) snap.child("location").child("lat").getValue();
                        double lon = (double) snap.child("location").child("lon").getValue();

                        Mural mur = new Mural(name, refImg, arImg, new Location(lat, lon));

                        markerMap.put(marker, mur);
                    }

                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        final SlidingUpPanelLayout slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
    }
}
// [END maps_marker_on_map_ready]