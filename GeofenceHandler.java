package com.example.acer.geofencingmainactivity;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by Acer on 05-Sep-18.
 */

public class GeofenceHandler {
    private Marker mmarker;
    private boolean isFenceSet;

    public GeofenceHandler(){
        isFenceSet = false;
    }
    public void setMmarker(Marker mmarker) {
        this.mmarker = mmarker;
    }

    public void setFence(boolean fenceSet) {
        isFenceSet = fenceSet;
    }

    public Marker getMmarker() {
        return mmarker;
    }

    public boolean isFenceSet() {
        return isFenceSet;
    }
}
