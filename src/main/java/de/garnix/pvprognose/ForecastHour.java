package de.garnix.pvprognose;

import java.time.*;
import java.time.format.DateTimeFormatter;



public class ForecastHour {
    /** Covered timerange */
    public long startTime, endTime;
    public ZonedDateTime endDateTime;
    /** Sun position azimuth & elevation at exact time of endTime, calculated by Grena3 */
    public Sunpos sunposEnd;
    /** Sun position at startTime */
    public Sunpos sunposStart;
    public int utc_offset;

    /** Values returned by Open-Meteo service */
    public double temperature;
    /** The radiation values are - according to Open-Meteo - aggregated values of the last hour.
     * Therefore we take Open-Meteos timestamp as endtime, and calculate with these values to
     * average Watt at starttime and endtime.
     */
    public double direct_normal_irradiance;
    public double diffuse_radiation;
    public double shortwave_radiation;

    /** Calculated Power */
    public double power;

    @Override
    public String toString() {
        return endDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + ";" +
                String.format("%.0f", power+0.49) + ";" +
                temperature + ";" + direct_normal_irradiance + ";" + diffuse_radiation + ";" +
                shortwave_radiation + ";" +
                String.format("%.1f;%.1f", sunposEnd.azimuth+0.05, sunposEnd.elevation+0.05);
    }

    public static class Sunpos {
        public final double azimuth;
        public final double elevation;
        double azimuthSin;
        double azimuthCos;
        double elevationSin;
        double elevationCos;
        public final double [] vector = new double[3];
        public Sunpos(double azimuth, double elevation) {
            this.azimuth = azimuth;
            this.azimuthCos = Math.cos(azimuth / 180 * Math.PI);
            this.azimuthSin = Math.sin(azimuth / 180 * Math.PI);
            this.elevation = elevation;
            this.elevationCos = Math.cos(elevation / 180 * Math.PI);
            this.elevationSin = Math.sin(elevation / 180 * Math.PI);

            vector[0] = azimuthSin * elevationCos;
            vector[1] = azimuthCos * elevationCos;
            vector[2] = elevationSin;
        }
    }
}
