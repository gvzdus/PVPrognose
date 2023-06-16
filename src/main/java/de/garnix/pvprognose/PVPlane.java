package de.garnix.pvprognose;

import java.util.Properties;

public class PVPlane {
    public String name;
    public double azimuth;
    public double tilt;
    public double diffuseEfficiency;
    public double albedo;
    public double cellsTempCoeff;
    public Inverter inverter;
    public int dcCapacity;
    public double tiltCosDiffuse;

    public double [] vector = new double[3];

    private ElevationTransparency[][] elevationTransparencies = null;
    private float azimuthMultiple;

    public PVPlane(Properties p, String name) throws Exception {
        String v;
        this.name = name;
        String prefix = name + ".";

        v = p.getProperty(prefix + "azimuth");
        if (v == null) throw new Exception(prefix + "azimuth not set");
        azimuth = Double.parseDouble(v);
        double azimuthSin = Math.sin((azimuth / 180.0 * Math.PI));
        double azimuthCos = Math.cos((azimuth / 180.0 * Math.PI));

        v = p.getProperty(prefix + "tilt");
        if (v == null) throw new Exception(prefix + "tilt not set");
        tilt = Double.parseDouble(v);
        double tiltSin = Math.sin(((90-tilt) / 180.0 * Math.PI));
        double tiltCos = Math.cos(((90-tilt) / 180.0 * Math.PI));
        tiltCosDiffuse = Math.cos((tilt / 180.0 * Math.PI));

        vector[0] = azimuthSin * tiltCos;
        vector[1] = azimuthCos * tiltCos;
        vector[2] = tiltSin;

        v = p.getProperty(prefix + "capacity");
        if (v == null) throw new Exception(prefix + "capacity not set");
        dcCapacity = Integer.parseInt(v);

        v = p.getProperty(prefix + "cellsTempCoeff");
        cellsTempCoeff = v==null ? -0.004 : Double.parseDouble(v)/100.0;

        v = p.getProperty(prefix + "albedo");
        albedo = v==null ? 0.1 : Double.parseDouble(v);

        v = p.getProperty(prefix + "diffuseEfficiency");
        diffuseEfficiency = v==null ? 0.6 : Double.parseDouble(v);

        v = p.getProperty(prefix + "inverter");
        inverter = Inverter.getOrCreate(p, v);

        v = p.getProperty(prefix + "horizon");
        if (v!=null) {
            String[] svals = v.split("\\s*,\\s*");
            int noSvals = svals.length;
            elevationTransparencies = new ElevationTransparency[noSvals][];
            azimuthMultiple = (float) (noSvals / 360.0);
            for (int i = 0; i < noSvals; i++) {
                String sval = svals[i];
                if (sval.length()>0) {
                    String[] tvals = sval.split("t");
                    int noTvals = tvals.length;
                    elevationTransparencies[i] = new ElevationTransparency[(noTvals+1)/2];
                    try {
                        for (int j = 0; j < noTvals; j+=2) {
                            ElevationTransparency et = new ElevationTransparency();
                            et.elevation = Float.parseFloat(tvals[j]);
                            if (j+1 < noTvals) {
                                et.transparency = Float.parseFloat(tvals[j+1]);
                            }
                            elevationTransparencies[i][j] = et;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new Exception ("Cannot parse horizon value " + v + ": " + sval + " broken:" + e.toString());
                    }
                }
            }
        }


        /*

            Old code for old format
        for (int i = 0; i < 36; i++) {
            v = p.getProperty(prefix + "horizon" + String.format("%02d", i));
            if (v == null) continue;
            String[] vs = v.split(",\\s*");
            if (vs.length > 0) {
                horizonElevation[i] = Integer.parseInt(vs[0]);
                if (vs.length > 1) {
                    horizonOpacity[i] = Double.parseDouble(vs[1]);
                } else {
                    horizonOpacity[i] = 0.0;
                }
            }
        }
         */
    }

    public double getElevationCorrectedValue (double azimuth, double elevation, double value) {
        if (value<=0.0)
            return 0.0;
        int idx = (int) Math.floor((azimuth+360) * azimuthMultiple) % elevationTransparencies.length;
        if (elevationTransparencies[idx]==null)
            return value;
        for (ElevationTransparency et : elevationTransparencies[idx]) {
            if (elevation <= et.elevation)
                return et.transparency*value;
        }
        return value;
    }


    private static class ElevationTransparency {
        float elevation;
        float transparency = 0.0f;
    }
}