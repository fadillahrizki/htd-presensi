package com.htd.presensi.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;

import com.google.android.gms.location.LocationResult;

public class GpsService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null)
        {
            String action = intent.getAction();
            if(action.equals("1"))
            {
                LocationResult result = LocationResult.extractResult(intent);
                if(result != null)
                {
                    Location location = result.getLastLocation();
                    try {
                        // MainActivity.getInstance().showUpdateLocation("Lokasi Anda \nLat : " + location.getLatitude() + "\nLng : " + location.getLongitude());
                    }catch (Exception e){

                    }
                }
            }
        }
    }
}