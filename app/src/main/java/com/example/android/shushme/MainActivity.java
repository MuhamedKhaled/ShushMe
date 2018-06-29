package com.example.android.shushme;


import android.Manifest;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private static final int PLACE_PICKER_REQUEST = 1;


    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private  GoogleApiClient client;
    private boolean mIsEnabled;
    private Geofencing mGeofencing;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this,null);
        mRecyclerView.setAdapter(mAdapter);

        Switch onOffSwitch = (Switch) findViewById(R.id.enable_switch);
        mIsEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled), false);
        onOffSwitch.setChecked(mIsEnabled);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled), isChecked);
                mIsEnabled = isChecked;
                editor.apply();
                if (isChecked)
                    mGeofencing.registerAllGeofences();
                else
                    mGeofencing.unRegisterAllGeofences();
            }

        });



        // Build up the LocationServices API client
        // Uses the addApi method to request the LocationServices API
        // Also uses enableAutoManage to automatically when to connect/suspend the client
         client = new GoogleApiClient.Builder(this)
                                .addConnectionCallbacks(this)
                                .addOnConnectionFailedListener(this)
                                .addApi(LocationServices.API)
                                .addApi(Places.GEO_DATA_API)
                                .enableAutoManage(this,this)
                                .build();

         mGeofencing = new Geofencing(client, this);
    }
    /**
     * Called when the Google API Client is successfully connected
     *
     * @param bundle Bundle of data provided to clients by Google Play services
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG,"API Client connection Successful!");
        // Get live data information
        refreshPlacesData();
    }


    /***
     * Called when the Google API Client is suspended
     *
     * @param i cause The reason for the disconnection. Defined by constants CAUSE_*.
     */

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG,"API Client connection Suspended!");

    }

    /***
     * Called when the Google API Client failed to connect to Google Play Services
     *
     * @param connectionResult A ConnectionResult that can be used for resolving the error
     */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG,"API Client connection Failed!");
    }



    /**
     * Button Click event handler to handle clicking the "Add new location" Button
     *
     * @param view
     */
    public void onAddPlaceButtonClicked(View view){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.need_location_permission_message), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, getString(R.string.location_permissions_granted_message), Toast.LENGTH_LONG).show();
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        Intent intent = null;
        try {
            intent = builder.build(this);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
        startActivityForResult(intent,PLACE_PICKER_REQUEST);
    }

    public void refreshPlacesData(){
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        Cursor cursor = getContentResolver().query(uri,
                null,null,null,null,null);

        if (cursor==null || cursor.getCount()==0)return;

        ArrayList<String>guides = new ArrayList<>();

        while (cursor.moveToNext()){
            guides.add(cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID)));
        }

        PendingResult<PlaceBuffer> results =
                Places.GeoDataApi.getPlaceById(client,guides.toArray(new String[guides.size()]));

        results.setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(@NonNull PlaceBuffer places) {
                mAdapter.updateData(places);
                mGeofencing.updateGepfencesList(places);
                if (mIsEnabled) mGeofencing.registerAllGeofences();
            }
        });
    }

    /***
     * Called when the Place Picker Activity returns back with a selected place (or after canceling)
     *
     * @param requestCode The request code passed when calling startActivityForResult
     * @param resultCode  The result code specified by the second activity
     * @param data        The Intent that carries the result data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==PLACE_PICKER_REQUEST && resultCode ==  RESULT_OK){
            Place place = PlacePicker.getPlace(this,data);

            if (place==null){
                Log.i(TAG,"No place selected");
                return;
            }

            String placeName = place.getName().toString();
            String placeAddress = place.getAddress().toString();
            String placID = place.getId();

            ContentValues  values  = new ContentValues();
            values.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID,placID);
            getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, values);


            // Get live data information
            refreshPlacesData();
        }
    }

    public void onLocationPermissionClicked(View view){
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    public void onRingerPermissionsClicked(View view) {
        Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        startActivityForResult(intent,0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckBox locationPermissions = (CheckBox) findViewById(R.id.location_permission_checkbox);
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.setChecked(false);
        } else {
            locationPermissions.setChecked(true);
            locationPermissions.setEnabled(false);
        }

        CheckBox ringerPermissions = findViewById(R.id.ringer_permissions_checkbox);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 24 && !notificationManager.isNotificationPolicyAccessGranted())
            ringerPermissions.setChecked(false);
        else
        {
            ringerPermissions.setChecked(true);
            ringerPermissions.setEnabled(false);
        }

    }

}
