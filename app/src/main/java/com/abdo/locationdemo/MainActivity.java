package com.abdo.locationdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_ID_LOCATION = 0;
    private static final int PERMISSION_ID_SMS = 1;
    private TextView latitudeTextView, longitudeTextView, addressTextView;
    private EditText mobileNumberEditText;
    private Button getLocationButton, getAddressButton, sendSMSButton;
    private FusedLocationProviderClient fusedLocationClient;
    private String address = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitudeTextView = findViewById(R.id.latitude_text_view);
        longitudeTextView = findViewById(R.id.longitude_text_view);
        addressTextView = findViewById(R.id.address_text_view);
        mobileNumberEditText = findViewById(R.id.mobile_text_view);
        getLocationButton = findViewById(R.id.get_location_button);
        getAddressButton = findViewById(R.id.convert_location_button);
        sendSMSButton = findViewById(R.id.send_sms_button);
        sendSMSButton.setEnabled(false);
        mobileNumberEditText.setEnabled(false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID_LOCATION);
                }

            }
        });

        getAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (latitudeTextView.getText().toString().trim() != null && !latitudeTextView.getText().toString().trim().isEmpty()) {
                    double latitude = Double.parseDouble(latitudeTextView.getText().toString());
                    double longitude = Double.parseDouble(longitudeTextView.getText().toString());
                    address = getAddress(latitude, longitude);
                    addressTextView.setText(address);
                    sendSMSButton.setEnabled(true);
                    mobileNumberEditText.setEnabled(true);
                } else {
                    Toast.makeText(MainActivity.this, R.string.get_location_first, Toast.LENGTH_SHORT).show();
                }
            }
        });

        sendSMSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAddress();

            }
        });

    }

    public void sendAddress() {
        String number = mobileNumberEditText.getText().toString();
        Log.i("TAG", "sendAddress: " + validCellPhone(number));
        if (!validCellPhone(number)) {
            Toast.makeText(this, R.string.notValid_number, Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, address))
                    .putExtra("sms_body", address);
            startActivity(intent);
        }

    }

    private boolean validCellPhone(String number) {
        return android.util.Patterns.PHONE.matcher(number).matches();
    }

    private String getAddress(double latitude, double longitude) {
        StringBuilder result = new StringBuilder();
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                result.append(address.getLocality()).append("\n");
                result.append(address.getCountryName());

            }
        } catch (IOException e) {
            Log.e("TAG", e.getMessage());
        }

        return result.toString();
    }

    private void sendSMS(String message) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + "01151883341"));
        intent.putExtra("sms_body", message);
        startActivity(intent);
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_ID_SMS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSMS(address);
                }
                break;
            case PERMISSION_ID_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
                break;
        }

    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {

        if (isLocationEnabled()) {
            fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location == null) {
                        requestNewLocationData();
                    } else {
                        latitudeTextView.setText(location.getLatitude() + "");
                        longitudeTextView.setText(location.getLongitude() + "");
                    }
                }
            });
        } else {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            latitudeTextView.setText(String.valueOf(mLastLocation.getLatitude()));
            longitudeTextView.setText(String.valueOf(mLastLocation.getLongitude()));
        }
    };


    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    @Override
    protected void onResume() {
        super.onResume();

    }
}