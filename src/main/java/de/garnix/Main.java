package de.garnix;

import de.garnix.pvprognose.ForecastHour;
import de.garnix.pvprognose.Inverter;
import de.garnix.pvprognose.OpenMeteoApi;
import de.garnix.pvprognose.PVPlane;

import java.io.FileReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class Main {

    static double longitude;
    static double latitude;

    static List<PVPlane> planes = new LinkedList<PVPlane>();
    public static void main(String[] args) throws Exception {

        loadProperties(args[0]);
        ForecastHour[] hours = OpenMeteoApi.getForecast( 6.590032, 51.066168 );

        System.out.println("TIME;WhGenFc;Temp;DirectRad;DiffRad;ShortWave;SunAzi;SunEle");
        for (ForecastHour hour : hours) {
            for (PVPlane p : planes) {
                // We calculate twice: With sun at beginning and end (now) of the past hour
                double watt = 0.0;
                for (ForecastHour.Sunpos sunpos : new ForecastHour.Sunpos[]{hour.sunposStart, hour.sunposEnd}) {

                    // eff is the efficiency for direct radiation
                    double eff = 0.0;
                    if (hour.direct_normal_irradiance > 0) {
                        eff = p.vector[0] * sunpos.vector[0] +
                                p.vector[1] * sunpos.vector[1] +
                                p.vector[2] * sunpos.vector[2];
                        eff = Math.max(0, eff);
                    }
                    if (eff > 0) {
                        //Calculate shading  in 10 degree ranges, total 36 ranges
                        int shadingIndex = (((int) Math.round((sunpos.azimuth) / 10)) % 36 + 36) % 36;
                        if (p.horizonElevation[shadingIndex] != null &&
                                p.horizonElevation[shadingIndex] > sunpos.elevation) {
                            eff *= p.horizonOpacity[shadingIndex];
                        }
                    }

                    // TODO: cos(tilt) is static
                    double shortwave_eff = (0.5 - 0.5 * Math.cos(p.tilt / 180 * Math.PI));
                    double totalRadiationOnCell = hour.direct_normal_irradiance * eff +
                            hour.diffuse_radiation * p.diffuseEfficiency +
                            hour.shortwave_radiation * shortwave_eff * p.albedo;  //flat plate equivalent of the solar irradiance
                    double cellTemperature = calcCellTemperature(hour.temperature, totalRadiationOnCell);
                    double dcPower;
                    //assume cellMaxPower is defined at 1000W/sqm
                    //dcPower = totalRadiationOnCell/1000.0 * (1+(cellTemperature - 25.0)*p.cellsTempCoeff) * p.dcCapacity;
                    dcPower = totalRadiationOnCell / 1000.0 * p.dcCapacity;
                    // System.out.println (p.name + " : dc=" + dcPower + " at " + cellTemperature + "°");
                    double dcPowerCorrected = dcPower * (1.0 + (cellTemperature - 25.0) * p.cellsTempCoeff);
                    // System.out.println (p.name + " : dct=" + dcPower + " at " + cellTemperature + "° " + p.cellsTempCoeff);
                    /* System.out.println("DEBUG: " + hour.dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) +
                            " plane=" + p.name + " sunpos.ele=" + sunpos.elevation + " " +
                            " direct=(" + hour.direct_normal_irradiance + " x " + eff + " = ) " + (hour.direct_normal_irradiance * eff ) +
                            " diffuse=("+ hour.diffuse_radiation + " x " + p.diffuseEfficiency + " = )" + (hour.diffuse_radiation*p.diffuseEfficiency) +
                            " albedo=(" + hour.shortwave_radiation + " x " + shortwave_eff + " x " + p.albedo + " = ) " + (hour.shortwave_radiation*shortwave_eff*p.albedo) +
                            " totalRadiation=" + totalRadiationOnCell +
                            " sumTotal = " + dcPower + " sumCorrected = ( x " +  (1.0 + (cellTemperature - 25.0) * p.cellsTempCoeff) + " = ) " + dcPowerCorrected) ;
                    */
                    watt += dcPowerCorrected;
                }
                p.inverter.addWatt(watt / 2.0);
            }
            double wattInverter = 0.0;
            for (Inverter i : Inverter.getInverters()) {
                wattInverter += i.getWatt();
                i.reset();
            }
            hour.power = wattInverter;

            System.out.println(hour);
        }
    }

    public static double calcCellTemperature(double ambientTemperature, double totalIrradiance){
        //models from here: https://www.scielo.br/j/babt/a/FBq5Pmm4gSFqsfh3V8MxfGN/  Photovoltaic Cell Temperature Estimation for a Grid-Connect Photovoltaic Systems in Curitiba
        //float cellTemperature =  30.006f + 0.0175f*(totalIrradiance-300f)+1.14f*(ambientTemperature-25f);  //Lasnier and Ang  Lasnier, F.; Ang, T. G. Photovoltaic engineering handbook, 1st ed.; IOP Publishing LTD: Lasnier, France, 1990; pp. 258.
        //float cellTemperature = ambientTemperature + 0.028f*totalIrradiance-1f;  //Schott Schott, T. Operation temperatures of PV modules. Photovoltaic solar energy conference 1985, pp. 392-396.
        double cellTemperature = ambientTemperature + 0.0342f*totalIrradiance;  //Ross model: https://www.researchgate.net/publication/275438802_Thermal_effects_of_the_extended_holographic_regions_for_holographic_planar_concentrator
        //assuming "not so well cooled" : 0.0342
        return cellTemperature;
    }

    static void loadProperties(String path) throws Exception {
        Properties props = new Properties();
        props.load(new FileReader(path));

        String v = props.getProperty("longitude");
        if (v==null) throw new Exception("Please set longitude");
        longitude = Double.parseDouble(v);

        v = props.getProperty("latitude");
        if (v==null) throw new Exception("Please set longitude");
        latitude = Double.parseDouble(v);

        v = props.getProperty("planes");
        if (v==null) throw new Exception("Please set at least one plane name in 'planes'");
        String[] planeNames = v.split(",\\s*");

        for (String s : planeNames) {
            planes.add (new PVPlane(props, s));
        }
    }
}