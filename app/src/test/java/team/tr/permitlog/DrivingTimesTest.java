package team.tr.permitlog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * DrivingTimes tests.
 */
public class DrivingTimesTest {
    // Instances of DrivingTimes to be used for testing.
    private DrivingTimes dt1 = new DrivingTimes();
    private DrivingTimes dt2 = new DrivingTimes(10, 5, 3, 2, 8);

    /**
     * Test the DrivingTimes getTotalTime method with valid types.
     *
     * @see DrivingTimes#getTime(String)
     * @see DrivingTimes#TIME_TYPES
     */
    @Test
    public void getTime() {
        assertEquals(0, this.dt1.getTime("total"));
        assertEquals(0, this.dt1.getTime("day"));
        assertEquals(0, this.dt1.getTime("night"));
        assertEquals(0, this.dt1.getTime("weather"));
        assertEquals(0, this.dt1.getTime("adverse"));
        assertEquals(10, this.dt2.getTime("total"));
        assertEquals(5, this.dt2.getTime("day"));
        assertEquals(3, this.dt2.getTime("night"));
        assertEquals(2, this.dt2.getTime("weather"));
        assertEquals(8, this.dt2.getTime("adverse"));
    }

    /**
     * Test the DrivingTimes getTotalTime method with invalid types.
     *
     * @see DrivingTimes#getTime(String)
     * @see DrivingTimes#TIME_TYPES
     */
    @Test(expected = IllegalArgumentException.class)
    public void getBadTypeTime() {
        this.dt1.getTime("bad");
        this.dt2.getTime("weathers");
    }

    /**
     * Test the DrivingTimes setTime method with valid types and values.
     *
     * @see DrivingTimes#setTime(String, long)
     * @see DrivingTimes#TIME_TYPES
     */
    @Test
    public void setTime() {
        this.dt1.setTime("total", 15);
        assertEquals(15, this.dt1.getTime("total"));
        this.dt2.setTime("adverse", 3);
        assertEquals(3, this.dt2.getTime("adverse"));
    }

    /**
     * Test the DrivingTimes setTime method with invalid times (less than 0).
     *
     * @see DrivingTimes#setTime(String, long)
     * @see DrivingTimes#TIME_TYPES
     */
    @Test(expected = IllegalArgumentException.class)
    public void setBadTime() {
        this.dt1.setTime("total", -3);
        this.dt1.setTime("day", -2);
        this.dt1.setTime("night", -3);
        this.dt1.setTime("weather", -1);
        this.dt1.setTime("adverse", -10);
        this.dt1.setTime("bad", -3);
    }

    /**
     * Test the DrivingTimes setTotalTime method with invalid types but valid times
     * (greater than 0).
     *
     * @see DrivingTimes#setTime(String, long)
     * @see DrivingTimes#TIME_TYPES
     */
    @Test(expected = IllegalArgumentException.class)
    public void setBadTypeTime() {
        this.dt1.setTime("bad", 3);
    }

}