package team.tr.permitlog;

/**
 * An interface for objects that consume a DrivingTimes object and return nothing.
 *
 * @see DrivingTimes
 */
public interface DrivingTimesConsumer {
    /**
     * A function that takes in a DrivingTimes object and returns nothing.
     *
     * @param timesObj the DrivingTimes object to be consumed.
     */
    void accept(DrivingTimes timesObj);
}
