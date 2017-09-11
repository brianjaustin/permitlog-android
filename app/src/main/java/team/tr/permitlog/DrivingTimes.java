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
        switch (type) {
            case "total": return total;
            case "day": return day;
            case "night": return night;
            case "weather": return weather;
            case "adverse": return adverse;
            default: return 0;
        }
    }
}
