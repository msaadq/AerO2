package com.aero2.android;

import android.Manifest;
import android.app.Dialog;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.aero2.android.DefaultActivities.Data.AirAzureContract;
import com.aero2.android.DefaultActivities.Data.AirAzureDbHelper;
import com.aero2.android.DefaultActivities.Data.AirAzureDownloadService;
import com.aero2.android.DefaultActivities.Data.RecordService;
import com.aero2.android.DefaultActivities.RecordingCompleteActivity;
import com.aero2.android.DefaultActivities.SimpleCursorLoader;
import com.aero2.android.DefaultClasses.AerOUtilities;
import com.aero2.android.DefaultClasses.MagicTextView;
import com.aero2.android.DefaultClasses.SystemBarTintManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;


public class MapBoxActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    public static final String LOG_TAG="AerO2 Map box Activity";

    //Google Play services
    private GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    private static final int MY_PERMISSIONS_REQUEST = 2;

    //Splash Screen
    protected Dialog splashScreen;


    //location variables
    public Double currLat;
    public Double currLong;

    //map varaibles
    private MapView mapView = null;
    public int mapID;
    public final int RESULTS_MAP_ID=1;
    public final int SAMPLES_MAP_ID=2;
    public int alpha=50;
    private FloatingActionButton goToCurretntLocation;
    private FloatingActionButton startRecordingSmog;
    private FloatingActionButton stopRecordingSmog;
    public Double mapLat;
    public Double mapLong;
    public Double mapZoom;
    public Double maxAirIndex= 1024.0;


    //UI variables
    public boolean searchBarVisible=true;
    public boolean locationButtonVisible=true;
    public boolean recordButtonVisible=true;
    public boolean fullscreenInAndOutEnabled =true;
    public boolean markerClicked=false;
    private boolean permissionJustGranted;
    public boolean inRecordMode=false;
    public TextView locationTextView;
    public MagicTextView smogTextView;
    public ImageButton openDrawer;
    public ImageView waterScreen;

    RecordService recordService;
    boolean recordServiceCompleted;
    boolean firstTimeShowingSmogValue=true;
    boolean recordingServiceIsGoingOn=false;

    long startTimeOfRecording;
    long endTimeOfRecording;


    //Start//----------------------- ACTIVITY LIFECYCLE ------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_box);
        setupStart(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause()  {
        super.onPause();
        saveMapStateInRam();
        saveMyLocationInRam();
        saveCurrentAppState();
        try {
            trimCache(this);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public void setupStart(Bundle savedInstanceState){
        bindViews();
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();
        startUpScreen();
        setupMapForStart(savedInstanceState);
        setupStatusBar();
        ploaceMarkerOnSelectedPlace();
        animateInAndOutOfFullScreen(250);
        ifMarkerHadBeenClickedDisableFullscreenAnimation();
        setOnClickListeners();


    }

    public void setupMapForStart(Bundle savedInstanceState){
        mapView = (MapView) findViewById(R.id.mapboxMapView);
        mapView.setStyleUrl("mapbox://styles/muddassir235/cijqzvhxo00568zkqbk87dftn");
        mapID=RESULTS_MAP_ID;
        mapView.setTiltEnabled(false);
        checkForLocationPermission();
        mapView.setMyLocationEnabled(true);
        mapView.onCreate(savedInstanceState);
        restorePreviousMapState();

    }
    //END//----------------------- ACTIVITY LIFECYCLE ------------------------------------------------------



    //START//------------------------- VIEWS  -----------------------------------------------------------
    public void bindViews(){
        goToCurretntLocation=(FloatingActionButton) findViewById(R.id.myLocation);
        startRecordingSmog=(FloatingActionButton) findViewById(R.id.record);
        stopRecordingSmog=(FloatingActionButton) findViewById(R.id.stopRecording);
        stopRecordingSmog.setVisibility(View.INVISIBLE);
        startRecordingSmog.setVisibility(View.VISIBLE);
        CardView recordingCard=(CardView) findViewById(R.id.recordingSmogCard);
        recordingCard.setVisibility(View.INVISIBLE);
        locationTextView=(TextView) findViewById(R.id.locationTextView);
        smogTextView=(MagicTextView) findViewById(R.id.smog_value_tv);
        openDrawer=(ImageButton) findViewById(R.id.open_drawer);
        waterScreen=(ImageView) findViewById(R.id.waterScreen);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    public void setOnClickListeners() {
        openDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawerLayout);
                drawer.openDrawer(GravityCompat.START);

            }
        });
        goToCurretntLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveCameraToMyLocation();
                goToCurretntLocation.setImageTintList(ColorStateList.valueOf(Color.parseColor("#39ADCC")));
            }
        });
        startRecordingSmog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterRecordMode();
                fadeInWaterScreen();
                checkForLocationPermission();
                moveCameraToMyLocation();
                recordService = new RecordService();
                recordService.shouldContinue = true;
                startTimeOfRecording=System.currentTimeMillis();
                recordService.startRecording(getApplicationContext());
                recordingServiceIsGoingOn = true;
                startRecordingSmog.setVisibility(View.INVISIBLE);
                stopRecordingSmog.setVisibility(View.VISIBLE);
                final Handler handler1 = new Handler();
                handler1.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        checkForLocationPermission();
                        mapView.setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
                        final Handler handler2 = new Handler();
                        handler2.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                fadeOutWaterScreen();

                            }
                        }, 2000);

                        mapView.setAllGesturesEnabled(false);
                        mapView.setStyleUrl("mapbox://styles/muddassir235/cik2moulv019bbpm3hlr90t6c");


                    }
                }, 1000);
            }
        });
        stopRecordingSmog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitRecordMode();
                recordService.shouldContinue=false;
                endTimeOfRecording=System.currentTimeMillis();
                long timeTakenForRecording=endTimeOfRecording-startTimeOfRecording;
                int mNotificationId = 235;
                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.cancel(mNotificationId);


                checkForLocationPermission();
                mapView.setMyLocationTrackingMode(MyLocationTracking.TRACKING_NONE);
                mapView.setAllGesturesEnabled(true);
                mapView.setTiltEnabled(false);

                firstTimeShowingSmogValue = true;
                recordingServiceIsGoingOn = false;
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        stopRecordingSmog.setVisibility(View.INVISIBLE);
                        startRecordingSmog.setVisibility(View.VISIBLE);
                        mapView.setStyleUrl("mapbox://styles/muddassir235/cijqzvhxo00568zkqbk87dftn");
                        smogTextView.setTextColor(Color.parseColor("#9939ADCC"));
                        smogTextView.setText(" ___ ");
                    }
                }, 1000);
                final Intent recordingCompleteActivityIntent=new Intent(getApplicationContext(),RecordingCompleteActivity.class);
                recordingCompleteActivityIntent.putExtra("TimeConsumed", timeTakenForRecording);

                PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                        .getCurrentPlace(mGoogleApiClient, null);
                result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                    @Override
                    public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                        float maxLikeliHood=0;
                        CharSequence nameOfPlace="Place Undetectable";
                        for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                            Log.i(LOG_TAG, String.format("Place '%s' has likelihood: %g",
                                    placeLikelihood.getPlace().getName(),
                                    placeLikelihood.getLikelihood()));
                            if(placeLikelihood.getLikelihood()>maxLikeliHood){
                                nameOfPlace=placeLikelihood.getPlace().getName();
                            }
                        }
                        recordingCompleteActivityIntent.putExtra("NameOfPlace",nameOfPlace);
                        startActivity(recordingCompleteActivityIntent);
                        likelyPlaces.release();
                    }
                });

            }
        });
        mapView.setOnScrollListener(new MapView.OnScrollListener() {
            @Override
            public void onScroll() {
                Location location = mapView.getMyLocation();
                if (location != null) {
                    Double latit = location.getLatitude();
                    Double longitude = location.getLongitude();

                    if (AerOUtilities.isTheMarkerOutside(getApplicationContext(), LOG_TAG, mapView.getLatLng(), latit, longitude, mapView)) {
                        goToCurretntLocation.setImageTintList(ColorStateList.valueOf(Color.parseColor("#779977")));
                    } else {
                        goToCurretntLocation.setImageTintList(ColorStateList.valueOf(Color.parseColor("#39ADCC")));
                    }
                }
            }
        });
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Location location = mapView.getMyLocation();
                if (location != null) {
                    Double latit = location.getLatitude();
                    Double longitude = location.getLongitude();

                    if (AerOUtilities.isTheMarkerOutside(getApplicationContext(), LOG_TAG, mapView.getLatLng(), latit, longitude, mapView)) {
                        goToCurretntLocation.setImageTintList(ColorStateList.valueOf(Color.parseColor("#779977")));
                    } else {
                        goToCurretntLocation.setImageTintList(ColorStateList.valueOf(Color.parseColor("#39ADCC")));
                    }
                }
            }
        }, 3000);


    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.result_map ){
            if(mapID!=RESULTS_MAP_ID){
                mapView.setStyleUrl("mapbox://styles/muddassir235/cijqzvhxo00568zkqbk87dftn");
                mapID=RESULTS_MAP_ID;
            }

        } else if (id == R.id.sample_map) {
            if(mapID!=SAMPLES_MAP_ID){
                mapView.setStyleUrl("mapbox://styles/muddassir235/cijvht0ak00gb94kq6nseust5");
                mapID=SAMPLES_MAP_ID;
            }
        }
        return true;
    }

    public void setupStatusBar(){
        CoordinatorLayout coordinatorLayout=(CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        // Set the padding to match the Status Bar height
        coordinatorLayout.setPadding(0, -AerOUtilities.getStatusBarHeight(this), 0, 0);
        CardView searchCard = (CardView) findViewById(R.id.searchCardView);
        RelativeLayout.LayoutParams paramsSearch = (RelativeLayout.LayoutParams) searchCard.getLayoutParams();
        paramsSearch.topMargin += AerOUtilities.getStatusBarHeight(this);
        searchCard.setLayoutParams(paramsSearch);
        // create our manager instance after the content view is set
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        // enable status bar tint
        tintManager.setStatusBarTintEnabled(true);
        // enable navigation bar tint
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setStatusBarAlpha(0.2f);
        tintManager.setNavigationBarAlpha(0.2f);
        tintManager.setTintAlpha(0.2f);
        tintManager.setStatusBarTintResource(R.drawable.selected);
        tintManager.setTintColor(Color.parseColor("#39ADCC"));

    }
    //END//------------------------- VIEWS  -----------------------------------------------------------


    //START//------------------------- ANIMATION -----------------------------------------------------------
    public void ploaceMarkerOnSelectedPlace() {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i(LOG_TAG, "Place: " + place.getName());
                LatLng mapboxLatLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                Log.v(LOG_TAG, " " + place.getPlaceTypes());
                Float zoom = AerOUtilities.getZoomLevelFromPlace(place);
                Log.v(LOG_TAG, " " + zoom);
                moveCameraToSearchedLocation(mapboxLatLng, zoom);
                mapView.addMarker(new MarkerOptions().position(mapboxLatLng).title((String) place.getName()).snippet((String) place.getAddress()));

            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(LOG_TAG, "An error occurred: " + status);
            }
        });
    }

    public void ifMarkerHadBeenClickedDisableFullscreenAnimation() {
        mapView.setOnMarkerClickListener(new MapView.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                fullscreenInAndOutEnabled = false;
                markerClicked = true;
                return false;
            }
        });
    }

    public void animateInAndOutOfFullScreen(final int duration) {
        mapView.setOnMapClickListener(new MapView.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng point) {
                Log.v(LOG_TAG, "Entered the on clickListener of the map");
                Log.v(LOG_TAG, " " + mapView.getMetersPerPixelAtLatitude(33));
                Log.v(LOG_TAG," lat long : "+mapView.getLatLng().getLatitude()+" "+mapView.getLatLng().getLongitude());
                if (locationButtonVisible && fullscreenInAndOutEnabled) {
                    final int amountToMoveLocationButtonDown = 0;
                    final int amountToMoveLocationButtonRight = 300;
                    final TranslateAnimation animLocationButton = new TranslateAnimation(0, amountToMoveLocationButtonRight, 0, amountToMoveLocationButtonDown);
                    animLocationButton.setDuration(duration - 100);
                    animLocationButton.setAnimationListener(new TranslateAnimation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    RelativeLayout.LayoutParams paramsLocationButton = (RelativeLayout.LayoutParams) goToCurretntLocation.getLayoutParams();
                                    paramsLocationButton.topMargin += amountToMoveLocationButtonDown;
                                    paramsLocationButton.rightMargin -= amountToMoveLocationButtonRight;
                                    goToCurretntLocation.setLayoutParams(paramsLocationButton);
                                }
                            }, 0);

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    locationButtonVisible = false;
                    goToCurretntLocation.startAnimation(animLocationButton);
                } else if (!locationButtonVisible && fullscreenInAndOutEnabled) {
                    final int amountToMoveLocationButtonDown = 0;
                    final int amountToMoveLocationButtonRight = -300;
                    final TranslateAnimation animLocationButton = new TranslateAnimation(0, amountToMoveLocationButtonRight, 0, amountToMoveLocationButtonDown);
                    animLocationButton.setDuration(duration);
                    animLocationButton.setAnimationListener(new TranslateAnimation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    RelativeLayout.LayoutParams paramsLocationButton = (RelativeLayout.LayoutParams) goToCurretntLocation.getLayoutParams();
                                    paramsLocationButton.topMargin += amountToMoveLocationButtonDown;
                                    paramsLocationButton.rightMargin -= amountToMoveLocationButtonRight;
                                    goToCurretntLocation.setLayoutParams(paramsLocationButton);
                                }
                            }, 0);

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    locationButtonVisible = true;
                    goToCurretntLocation.startAnimation(animLocationButton);
                }
                if (searchBarVisible && fullscreenInAndOutEnabled) {
                    CardView searchCard = (CardView) findViewById(R.id.searchCardView);
                    final int amountToMoveSearchDown = -350;
                    final int amountToMoveSearchRight = 0;
                    final TranslateAnimation animSearch = new TranslateAnimation(0, amountToMoveSearchRight, 0, amountToMoveSearchDown);
                    animSearch.setDuration(duration);

                    animSearch.setAnimationListener(new TranslateAnimation.AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CardView searchCard = (CardView) findViewById(R.id.searchCardView);
                                    RelativeLayout.LayoutParams paramsSearch = (RelativeLayout.LayoutParams) searchCard.getLayoutParams();
                                    paramsSearch.topMargin += amountToMoveSearchDown;
                                    paramsSearch.leftMargin += amountToMoveSearchRight;
                                    searchCard.setLayoutParams(paramsSearch);
                                }
                            }, 0);
                        }
                    });
                    searchCard.startAnimation(animSearch);
                    searchBarVisible = false;
                } else if (!searchBarVisible && fullscreenInAndOutEnabled) {
                    CardView searchCard = (CardView) findViewById(R.id.searchCardView);
                    final int amountToMoveSearchDown = 350;
                    final int amountToMoveSearchRight = 0;
                    final TranslateAnimation animSearch = new TranslateAnimation(0, amountToMoveSearchRight, 0, amountToMoveSearchDown);
                    animSearch.setDuration(duration);
                    animSearch.setAnimationListener(new TranslateAnimation.AnimationListener() {

                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CardView searchCard = (CardView) findViewById(R.id.searchCardView);
                                    RelativeLayout.LayoutParams paramsSearch = (RelativeLayout.LayoutParams) searchCard.getLayoutParams();
                                    paramsSearch.topMargin += amountToMoveSearchDown;
                                    paramsSearch.leftMargin += amountToMoveSearchRight;
                                    searchCard.setLayoutParams(paramsSearch);
                                }
                            }, 3);
                        }
                    });
                    searchCard.startAnimation(animSearch);
                    searchBarVisible = true;

                }
                if (recordButtonVisible && fullscreenInAndOutEnabled) {
                    final int amountToMoveRecordButtonDown = 0;
                    final int amountToMoveRecordButtonRight = 300;
                    final TranslateAnimation animRecordButton = new TranslateAnimation(0, amountToMoveRecordButtonRight, 0, amountToMoveRecordButtonDown);
                    animRecordButton.setDuration(duration);
                    animRecordButton.setAnimationListener(new TranslateAnimation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    RelativeLayout.LayoutParams paramsRecordButton = (RelativeLayout.LayoutParams) startRecordingSmog.getLayoutParams();
                                    paramsRecordButton.topMargin += amountToMoveRecordButtonDown;
                                    paramsRecordButton.rightMargin -= amountToMoveRecordButtonRight;
                                    startRecordingSmog.setLayoutParams(paramsRecordButton);
                                }
                            }, 0);

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    recordButtonVisible = false;
                    startRecordingSmog.startAnimation(animRecordButton);
                } else if (!recordButtonVisible && fullscreenInAndOutEnabled) {
                    final int amountToMoveRecordButtonDown = 0;
                    final int amountToMoveRecordButtonRight = -300;
                    final TranslateAnimation animRecordButton = new TranslateAnimation(0, amountToMoveRecordButtonRight, 0, amountToMoveRecordButtonDown);
                    animRecordButton.setDuration(duration - 100);
                    animRecordButton.setAnimationListener(new TranslateAnimation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    RelativeLayout.LayoutParams paramsRecordButton = (RelativeLayout.LayoutParams) startRecordingSmog.getLayoutParams();
                                    paramsRecordButton.topMargin += amountToMoveRecordButtonDown;
                                    paramsRecordButton.rightMargin -= amountToMoveRecordButtonRight;
                                    startRecordingSmog.setLayoutParams(paramsRecordButton);
                                }
                            }, 0);

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    recordButtonVisible = true;
                    startRecordingSmog.startAnimation(animRecordButton);
                }
                if (markerClicked) {
                    if (!inRecordMode) {
                        fullscreenInAndOutEnabled = true;
                    }
                }
            }

        });
    }

    //move map camera to the uers's current positon
    private void moveCameraToMyLocation() {
        Location myLocation = mapView.getMyLocation();
        if (myLocation != null) {
            double latit=myLocation.getLatitude();
            double longit=myLocation.getLongitude();
            LatLng latLng = new LatLng(latit, longit);
            CameraPosition cameraPosition = new CameraPosition(latLng, 13, 0, 0);
            CameraUpdateFactory cameraUpdateFactory = new CameraUpdateFactory();
            CameraUpdate cameraUpdate = cameraUpdateFactory.newCameraPosition(cameraPosition);
            mapView.animateCamera(cameraUpdate, 1000, null);
        }
    }

    //move map camera to the location the user just searched for
    private void moveCameraToSearchedLocation(LatLng latLng,Float zoom) {
        CameraPosition cameraPosition=new CameraPosition(latLng,zoom,0,0);
        CameraUpdateFactory cameraUpdateFactory=new CameraUpdateFactory();
        CameraUpdate cameraUpdate=cameraUpdateFactory.newCameraPosition(cameraPosition);
        mapView.animateCamera(cameraUpdate, 2000, null);
    }

    protected void removeSplashScreen() {
        if (splashScreen!= null) {
            splashScreen.dismiss();
            splashScreen = null;
        }
    }

    protected void showSplashScreen() {
        SharedPreferences afterServiceCompletion=getApplicationContext().getSharedPreferences("AFTER_SERVICE_COMPLETED", Context.MODE_WORLD_WRITEABLE);
        SharedPreferences afterPermissionGranted = getApplicationContext().getSharedPreferences("AFTER_PERMISSION_GRANTED", Context.MODE_WORLD_WRITEABLE);
        boolean serviceJustCompleted=afterServiceCompletion.getBoolean("AFTER_SERVICE_COMPLETED", false);
        splashScreen = new Dialog(this, R.style.SplashScreen);
        permissionJustGranted=afterPermissionGranted.getBoolean("AFTER_PERMISSION_GRANTED",false);

        if(!serviceJustCompleted&&!permissionJustGranted) {

//            View splashIcon=(View) findViewById(R.id.splash_icon);
//            View splashText=(View) findViewById(R.id.splash_text);
//            splashIcon.setVisibility(View.GONE);
//            splashText.setVisibility(View.GONE);
            splashScreen.setContentView(R.layout.splash_screen);
            splashScreen.setCancelable(false);
            splashScreen.show();
        }else if(serviceJustCompleted) {
            SharedPreferences.Editor afterServiceCompletionEditor=afterServiceCompletion.edit();
            afterServiceCompletionEditor.putBoolean("AFTER_SERVICE_COMPLETED", false);
            afterServiceCompletionEditor.commit();
            splashScreen.setCancelable(false);
            splashScreen.show();
        }else if(permissionJustGranted){
            removeSplashScreen();
        }




    }

    public void fadeSplashScreen() {
        // Set Runnable to remove splash screen just in case
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeSplashScreen();
            }
        }, 1000);

        if(!permissionJustGranted) {
            RelativeLayout layout = (RelativeLayout) findViewById(R.id.map_layout);
            AlphaAnimation animation = new AlphaAnimation(0f, 1.0f);
            animation.setFillAfter(true);
            animation.setDuration(1500);

            //apply the animation ( fade In ) to your LAyout
            layout.startAnimation(animation);
        }

    }

    public void enterRecordMode(){
        //fade the record card in view
        final CardView smogCard = (CardView) findViewById(R.id.recordingSmogCard);
        AlphaAnimation animation = new AlphaAnimation(0f, 1.0f);
        animation.setFillAfter(true);
        animation.setDuration(250);

        //apply the animation ( fade In ) to your LAyout
        smogCard.startAnimation(animation);
        //apply the animation ( fade In ) to your LAyout
        smogCard.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                smogCard.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        //move the current location button out of view
        final int amountToMoveLocationButtonDown = 0;
        final int amountToMoveLocationButtonRight = 300;
        final TranslateAnimation animLocationButton = new TranslateAnimation(0, amountToMoveLocationButtonRight, 0, amountToMoveLocationButtonDown);
        animLocationButton.setDuration(250);
        animLocationButton.setAnimationListener(new TranslateAnimation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RelativeLayout.LayoutParams paramsLocationButton = (RelativeLayout.LayoutParams) goToCurretntLocation.getLayoutParams();
                        paramsLocationButton.topMargin += amountToMoveLocationButtonDown;
                        paramsLocationButton.rightMargin -= amountToMoveLocationButtonRight;
                        goToCurretntLocation.setLayoutParams(paramsLocationButton);
                    }
                }, 0);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        goToCurretntLocation.startAnimation(animLocationButton);

        //take search bar out of view
        CardView searchCard = (CardView) findViewById(R.id.searchCardView);
        final int amountToMoveSearchDown = -350;
        final int amountToMoveSearchRight = 0;
        final TranslateAnimation animSearch = new TranslateAnimation(0, amountToMoveSearchRight, 0, amountToMoveSearchDown);
        animSearch.setDuration(250);

        animSearch.setAnimationListener(new TranslateAnimation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CardView searchCard = (CardView) findViewById(R.id.searchCardView);
                        RelativeLayout.LayoutParams paramsSearch = (RelativeLayout.LayoutParams) searchCard.getLayoutParams();
                        paramsSearch.topMargin += amountToMoveSearchDown;
                        paramsSearch.leftMargin += amountToMoveSearchRight;
                        searchCard.setLayoutParams(paramsSearch);
                    }
                }, 0);
            }
        });
        searchCard.startAnimation(animSearch);

        inRecordMode=true;
        fullscreenInAndOutEnabled =false;
    }

    public void exitRecordMode(){
        //fade the record card out of view
        final CardView smogCard = (CardView) findViewById(R.id.recordingSmogCard);
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0f);
        animation.setFillAfter(true);
        animation.setDuration(250);

        //apply the animation ( fade In ) to your LAyout
        smogCard.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                smogCard.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        //move the current location button back in view
        final int amountToMoveLocationButtonDown = 0;
        final int amountToMoveLocationButtonRight = -300;
        final TranslateAnimation animLocationButton = new TranslateAnimation(0, amountToMoveLocationButtonRight, 0, amountToMoveLocationButtonDown);
        animLocationButton.setDuration(250);
        animLocationButton.setAnimationListener(new TranslateAnimation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RelativeLayout.LayoutParams paramsLocationButton = (RelativeLayout.LayoutParams) goToCurretntLocation.getLayoutParams();
                        paramsLocationButton.topMargin += amountToMoveLocationButtonDown;
                        paramsLocationButton.rightMargin -= amountToMoveLocationButtonRight;
                        goToCurretntLocation.setLayoutParams(paramsLocationButton);
                    }
                }, 0);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        goToCurretntLocation.startAnimation(animLocationButton);

        //bring search bar back in view
        CardView searchCard = (CardView) findViewById(R.id.searchCardView);
        final int amountToMoveSearchDown = 350;
        final int amountToMoveSearchRight = 0;
        final TranslateAnimation animSearch = new TranslateAnimation(0, amountToMoveSearchRight, 0, amountToMoveSearchDown);
        animSearch.setDuration(250);

        animSearch.setAnimationListener(new TranslateAnimation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CardView searchCard = (CardView) findViewById(R.id.searchCardView);
                        RelativeLayout.LayoutParams paramsSearch = (RelativeLayout.LayoutParams) searchCard.getLayoutParams();
                        paramsSearch.topMargin += amountToMoveSearchDown;
                        paramsSearch.leftMargin += amountToMoveSearchRight;
                        searchCard.setLayoutParams(paramsSearch);
                    }
                }, 0);
            }
        });
        searchCard.startAnimation(animSearch);

        inRecordMode=false;
        fullscreenInAndOutEnabled =true;
    }

    public void fadeInWaterScreen(){
        AlphaAnimation animation = new AlphaAnimation(0f, 1.0f);
        animation.setFillAfter(true);
        animation.setDuration(900);
        waterScreen.startAnimation(animation);
    }

    public void startUpScreen(){
        waterScreen.setVisibility(View.VISIBLE);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeOutWaterScreen();
            }
        }, 5000);
    }

    public void fadeOutWaterScreen(){
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0f);
        animation.setFillAfter(true);
        animation.setDuration(1000);
        waterScreen.startAnimation(animation);
    }
    //END//------------------------- ANIMATION -----------------------------------------------------------



    //START//------------------------- DATA -----------------------------------------------------------
    public void saveCurrentAppState(){
        //Saving Current Map State
        SharedPreferences latMapSharedPref = getApplicationContext().getSharedPreferences("MapLatitudeAerO2", Context.MODE_PRIVATE);
        SharedPreferences longMapSharedPref = getApplicationContext().getSharedPreferences("MapLongitudeAerO2", Context.MODE_PRIVATE);
        SharedPreferences zoomPref = getApplicationContext().getSharedPreferences("MapZoomAerO2", Context.MODE_PRIVATE);
        SharedPreferences.Editor latMapEdit = latMapSharedPref.edit();
        SharedPreferences.Editor longMapEdit = longMapSharedPref.edit();
        SharedPreferences.Editor zoomPrefEdit=zoomPref.edit();
        if(mapLat!=null) {
            latMapEdit.putString("MapLatitudeAerO2", String.valueOf(mapLat));
            latMapEdit.commit();
            longMapEdit.putString("MapLongitudeAerO2", String.valueOf(mapLong));
            longMapEdit.commit();
            zoomPrefEdit.putString("MapZoomAerO2", String.valueOf(mapZoom));
            zoomPrefEdit.commit();
        }else{
            Log.v(LOG_TAG, "Latitude was null");
        }
        //Saving Current Location data
        SharedPreferences latSharedPref = getApplicationContext().getSharedPreferences("LatitudeAerO2", Context.MODE_PRIVATE);
        SharedPreferences longSharedPref = getApplicationContext().getSharedPreferences("LongitudeAerO2", Context.MODE_PRIVATE);
        SharedPreferences.Editor latEdit = latSharedPref.edit();
        if(currLat!=null) {
            latEdit.putString("LatitudeAerO2", String.valueOf(currLat));
            latEdit.commit();
            SharedPreferences.Editor longEdit = longSharedPref.edit();
            longEdit.putString("LongitudeAerO2", String.valueOf(currLong));
            longEdit.commit();
        }else{
            Log.v(LOG_TAG,"Latitude was null");
        }
        Log.v(LOG_TAG, "Saved Current map state");
//        if(permissionJustGranted) {
//            SharedPreferences afterPermissionGranted=getApplicationContext().getSharedPreferences("AFTER_PERMISSION_GRANTED", Context.MODE_WORLD_WRITEABLE);
//            SharedPreferences.Editor afterPermissionGrantedEditor = afterPermissionGranted.edit();
//            afterPermissionGrantedEditor.putBoolean("AFTER_PERMISSION_GRANTED", false);
//            afterPermissionGrantedEditor.commit();
//        }
    }

    public void saveMapStateInRam(){
        mapLat=mapView.getLatLng().getLatitude();
        mapLong=mapView.getLatLng().getLongitude();
        mapZoom=mapView.getZoom();
    }

    public void saveMyLocationInRam(){
        currLat=mapView.getMyLocation().getLatitude();
        currLong=mapView.getMyLocation().getLongitude();
    }

    //restore previous map state i.e. last time the app was closed
    public void restorePreviousMapState(){
        //Retrieving the last known location from cache
        SharedPreferences latSharedPref = getApplicationContext().getSharedPreferences("MapLatitudeAerO2", Context.MODE_PRIVATE);
        SharedPreferences longSharedPref = getApplicationContext().getSharedPreferences("MapLongitudeAerO2", Context.MODE_PRIVATE);
        SharedPreferences zoomPref = getApplicationContext().getSharedPreferences("MapZoomAerO2", Context.MODE_PRIVATE);
        mapLat = Double.valueOf(latSharedPref.getString("MapLatitudeAerO2", "33"));
        mapLong = Double.valueOf(longSharedPref.getString("MapLongitudeAerO2", "72"));
        float zoom=Float.valueOf(zoomPref.getString("MapZoomAerO2","18"));
        LatLng latLng = new LatLng(mapLat, mapLong);
        CameraPosition cameraPosition=new CameraPosition(latLng,zoom,0,0);
        CameraUpdateFactory cameraUpdateFactory=new CameraUpdateFactory();
        CameraUpdate cameraUpdate=cameraUpdateFactory.newCameraPosition(cameraPosition);
        mapView.moveCamera(cameraUpdate);

    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }


    //END//------------------------- DATA -----------------------------------------------------------



    //END//------------------------- GOOGLE PLAY SERVICES  -----------------------------------------------------------
    @Override
    public void onConnected(Bundle bundle) {
        Log.v(LOG_TAG,"Entered the on connected Method");
        requestLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        SharedPreferences latSharedPref = getApplicationContext().getSharedPreferences("CurrentSmogValue", Context.MODE_PRIVATE);
        String value=latSharedPref.getString("CurrentSmogValue",String.valueOf(-1));
        if(!firstTimeShowingSmogValue&&recordingServiceIsGoingOn) {
            smogTextView.setText(value);
            smogTextView.setTextColor(AerOUtilities.getColorFromSmogValue(1000, Double.valueOf(value)));
            Typeface myTypeface = Typeface.createFromAsset(getAssets(), "fonts/market.ttf");
            smogTextView.setTypeface(myTypeface);
        }else if(recordingServiceIsGoingOn){
            smogTextView.setText(" ___ ");
            smogTextView.setTextColor(Color.parseColor("#9939ADCC"));
            Typeface myTypeface = Typeface.createFromAsset(getAssets(), "fonts/market.ttf");
            smogTextView.setTypeface(myTypeface);
            firstTimeShowingSmogValue=false;
        }else{
            smogTextView.setText(" ___ ");
            smogTextView.setTextColor(Color.parseColor("#9939ADCC"));
            Typeface myTypeface = Typeface.createFromAsset(getAssets(), "fonts/market.ttf");
            smogTextView.setTypeface(myTypeface);
        }
        DecimalFormat decimalFormat=new DecimalFormat("#.###");
        String lati=decimalFormat.format(location.getLatitude());
        String longi=decimalFormat.format(location.getLongitude());
        String latitude = "<b>" + "lat: " + "</b> " + lati;
        String longitude = "<b>" + "lon: " + "</b> " + longi;

        locationTextView.setText(Html.fromHtml(latitude + "&nbsp &nbsp &nbsp &nbsp;" + longitude));

    }

    //request current location from the google play locations api
    public void requestLocation(){
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        checkForLocationPermission();

        Log.v(LOG_TAG, "About to request Location");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        Log.v(LOG_TAG, "Requested Location");
    }

    public void checkForLocationPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return;
        }
    }

    @Override //This function is called once the dialog for asking permission is answered
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //Permission Granted YAY!
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // this scope is unreachable as the permission has been granted if we have entered the parent scope.
                        return;
                    }
                    //once the permission to access fine location is granted restart the activity so that we can get the user's location

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }

    }

    //END//------------------------- GOOGLE PLAY SERVICES  -----------------------------------------------------------

}
