package org.traccar.model;

import org.traccar.storage.StorageName;

@StorageName("tc_converted_positions")
public class ConvertedPosition extends ExtendedModel {
    public static final String PLATFORM_AUTONAVI = "AutoNavi";
    public static final String PLATFORM_BAIDU = "Baidu";
    public static final String PLATFORM_TENCENT = "Tencent";

    public static final String CRS_GCJ_02 = "GCJ_02";
    public static final String CRS_BD_09 = "BD_09";

    public ConvertedPosition() {
    }

    public ConvertedPosition(String platform, String crs) {
        this.platform = platform;
        this.crs = crs;
    }

    private String platform;

    public String getPlatform() {
        return platform;
    }

    private String crs;

    public String getCrs() {
        return crs;
    }

    private double latitude;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude out of range");
        }
        this.latitude = latitude;
    }

    private double longitude;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude out of range");
        }
        this.longitude = longitude;
    }
}
