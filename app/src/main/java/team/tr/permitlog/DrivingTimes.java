package team.tr.permitlog;

import java.util.Arrays;
import java.util.List;

/**
 * DrivingTimes Model object.
 *
 * The possible types of driving time goals and values for each type.
 */
public class DrivingTimes {
    // Stores different types of driving time goals.
    static final List<String> TIME_TYPES =
            Arrays.asList("total", "day", "night", "weather", "adverse");

    // The values for each type of driving time goal.
    public long total, day, night, weather, adverse;

    /**
     * Default constructor.
     *
     * The default values for this object:
     * <ul>
     *     <li>total is 0</li>
     *     <li>day is 0</li>
     *     <li>night is 0</li>
     *     <li>weather is 0</li>
     *     <li>adverse is 0</li>
     * </ul>
     */
    DrivingTimes() {
        this.setTime("total",   0);
        this.setTime("day",     0);
        this.setTime("night",   0);
        this.setTime("weather", 0);
        this.setTime("adverse", 0);
    }

    /**
     * Constructor.
     *
     * @param total   the total amount of driving done. Must be greater than or equal to 0.
     * @param day     the amount of driving during the day. Must be greater than or equal to 0.
     * @param night   the amount of driving done at night. Must be greater than or equal to 0.
     * @param weather the amount of driving done during some weather conditions.
     *                Must be greater than or equal to 0.
     * @param adverse the amount of driving done in adverse conditions.
     *                Must be greater than or equal to 0.
     */
    DrivingTimes(long total, long day, long night, long weather, long adverse) {
        if (total < 0 || day < 0 || night < 0 || weather < 0 || adverse < 0) {
            throw new IllegalArgumentException("Driving times must be " +
                    "greater than or equal to zero.");
        }
        this.setTime("total",   total);
        this.setTime("day",     day);
        this.setTime("night",   night);
        this.setTime("weather", weather);
        this.setTime("adverse", adverse);
    }

    /**
     * Returns a driving time based off the type.
     *
     * @param type the type of driving time to set (one of TIME_TYPES)
     * @return     the value corresponding to the given type of time
     */
    long getTime(String type) {
        switch(type) {
            case "total":
                return this.total;
            case "day":
                return this.day;
            case "night":
                return this.night;
            case "weather":
                return this.weather;
            case "adverse":
                return this.adverse;
            default:
                throw new IllegalArgumentException("Invalid driving time type: " + type);
        }
    }

    /**
     * Sets a driving time based off the type.
     *
     * @param type  the type of driving time to set (one of TIME_TYPES). Must be greater than
     *              or equal to 0.
     * @param value the amount of time to set.
     */
    void setTime(String type, long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Driving times must be " +
                    "greater than or equal to zero.");
        }
        switch(type) {
            case "total":
                this.total = value;
                break;
            case "day":
                this.day = value;
                break;
            case "night":
                this.night = value;
                break;
            case "weather":
                this.weather = value;
                break;
            case "adverse":
                this.adverse = value;
                break;
            default:
                throw new IllegalArgumentException("Invalid driving time type " + type);
        }
    }
}
