package de.garnix.pvprognose;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.e175.klaus.solarpositioning.AzimuthZenithAngle;
import net.e175.klaus.solarpositioning.Grena3;

public class OpenMeteoApi {
    private static final int FORECAST_DAYS = 3;
    private static final int LAST_DAYS = 2;


    public static ForecastHour[] getForecast(double longitude, double latitude) {
/*
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude + "&longitude=" +
                longitude + "&forecast_days=" + FORECAST_DAYS + "&past_days=" + LAST_DAYS +
                "&hourly=temperature_2m,diffuse_radiation," +
                "direct_normal_irradiance,shortwave_radiation,weathercode&daily=weathercode," +
                "&timeformat=unixtime&timezone=auto&cell_selection=nearest";

        */
        String url = "https://archive-api.open-meteo.com/v1/archive?latitude=" + latitude + "&longitude=" +
                longitude + "&start_date=2022-06-01&end_date=2023-05-31" +
                "&hourly=temperature_2m,diffuse_radiation," +
                "direct_normal_irradiance,shortwave_radiation,weathercode&daily=weathercode," +
                "&timeformat=unixtime&timezone=auto";
        try (InputStream in = new URL(url).openStream()) {
            JsonElement jsonRoot = JsonParser.parseReader(new InputStreamReader(in, "UTF8"));
            JsonObject hourly = jsonRoot.getAsJsonObject().getAsJsonObject("hourly");
            int utc_offset = jsonRoot.getAsJsonObject().get("utc_offset_seconds").getAsInt();
            JsonArray time = hourly.getAsJsonArray("time");
            JsonArray temperature_2m = hourly.getAsJsonArray("temperature_2m");
            JsonArray diffuse_radiation = hourly.getAsJsonArray("diffuse_radiation");
            JsonArray direct_normal_irradiance = hourly.getAsJsonArray("direct_normal_irradiance");
            JsonArray shortwave_radiation = hourly.getAsJsonArray("shortwave_radiation");

            int elements = time.size();
            ForecastHour[] hours = new ForecastHour[time.size()];
            for (int i = 0; i < elements; i++) {
                ForecastHour fh = new ForecastHour();
                fh.utc_offset = utc_offset;
                // as all timestamps refer to the previous hour, we set the endTime by timestamp..
                fh.endTime = time.get(i).getAsLong();
                // ... and startTime -3599 seconds
                fh.startTime = fh.endTime - 3599;
                fh.diffuse_radiation = diffuse_radiation.get(i).getAsDouble();
                fh.temperature = temperature_2m.get(i).getAsDouble();
                fh.direct_normal_irradiance = direct_normal_irradiance.get(i).getAsDouble();
                fh.shortwave_radiation = shortwave_radiation.get(i).getAsDouble();

                // Calculate position of sun at the end
                ZonedDateTime date = ZonedDateTime.of(
                        LocalDateTime.ofEpochSecond(fh.endTime, 0,
                                ZoneOffset.ofTotalSeconds(utc_offset)),
                                ZoneId.systemDefault());
                fh.endDateTime = date;

                AzimuthZenithAngle position = Grena3.calculateSolarPosition(
                        date,
                        latitude,
                        longitude, 0);
                fh.sunposEnd = new ForecastHour.Sunpos(position.getAzimuth(), 90 - position.getZenithAngle());

                // Calculate position of sun at the start (only for the first hour, otherwise last start)
                if (i < 1) {
                    date = ZonedDateTime.of(
                            LocalDateTime.ofEpochSecond(fh.startTime, 0,
                                    ZoneOffset.ofTotalSeconds(utc_offset)),
                            ZoneId.systemDefault());
                    position = Grena3.calculateSolarPosition(
                            date,
                            latitude,
                            longitude, 0);
                    fh.sunposStart = new ForecastHour.Sunpos(position.getAzimuth(), 90 - position.getZenithAngle());
                } else {
                    fh.sunposStart = hours[i-1].sunposEnd;
                }
                hours[i] = fh;

            }

            return hours;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
