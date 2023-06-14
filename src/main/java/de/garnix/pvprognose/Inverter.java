package de.garnix.pvprognose;

import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

public class Inverter {
    String name = "default";
    double maxWatt = Float.MAX_VALUE;
    double watt = 0.0;

    private static Inverter DEFAULT = null;

    private static HashMap<String,Inverter> list = new HashMap<>();

    public static Collection<Inverter> getInverters() {
        return list.values();
    }

    public static Inverter get(String name) {
        if (name == null)
            name = "default";
        Inverter i = list.get(name);
        return i;
    }

    public static Inverter getOrCreate(Properties p, String name) {
        if (name==null)
            name = "default";
        Inverter i = list.get(name);
        if (i == null) {
            i = new Inverter();
            i.name = name;
            String v = p.getProperty(name + "." + "maxpower");
            i.maxWatt = v!=null ? Double.parseDouble(v) : Float.MAX_VALUE;
            list.put(name, i);
        }
        return i;
    }

    public double getWatt() {
        return Math.min(watt, maxWatt);
    }

    public void addWatt(double watt) {
        this.watt += watt;
    }

    public void reset () {
        watt = 0;
    }
}
