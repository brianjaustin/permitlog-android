package team.tr.permitlog;

import java.util.Arrays;
import java.util.List;

public class DrivingTimes {
    //Stores different types of goals:
    public static final List<String> TIME_TYPES = Arrays.asList("total", "day", "night", "weather", "adverse");

    //A class with different properties representing different types of driving times:
    public long total, day, night, weather, adverse;

    public long getTime(String type) {
        /* Returns a driving time based off the type */
        if (type.equals("total")) return total;
        else if (type.equals("day")) return day;
        else if (type.equals("night")) return night;
        else if (type.equals("weather")) return weather;
        else if (type.equals("adverse")) return adverse;
        else throw new RuntimeException("Unrecognized Driving Time type "+type);
    }

    public void setTime(String type, long value) {
        /* Sets a driving time based off the type */
        if (type.equals("total")) total = value;
        else if (type.equals("day")) day = value;
        else if (type.equals("night")) night = value;
        else if (type.equals("weather")) weather = value;
        else if (type.equals("adverse")) adverse = value;
        else throw new RuntimeException("Unrecognized Driving Time type "+type);
    }
}
