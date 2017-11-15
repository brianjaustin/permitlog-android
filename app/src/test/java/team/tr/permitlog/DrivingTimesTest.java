package team.tr.permitlog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DrivingTimesTest {
    private DrivingTimes tester = new DrivingTimes();

    @Test
    public void getTotalTime() throws Exception {
        // setup the times
        tester.total = 20;
        tester.day = 12;
        tester.night = 5;
        tester.weather = 3;
        tester.adverse = 0;

        // test getting the valid types of times
        assertEquals(20, tester.getTime("total"));
        assertEquals(12, tester.getTime("day"));
        assertEquals(5, tester.getTime("night"));
        assertEquals(3, tester.getTime("weather"));
        assertEquals(0, tester.getTime("adverse"));
    }

    @Test(expected = RuntimeException.class)
    public void getBadTotalTime() throws Exception {
        // test an invalid type
        tester.getTime("bad");
    }

    @Test
    public void setTotalTime() throws Exception {
        // test setting the valid types of times
        tester.setTime("total", 15);
        assertEquals(15, tester.total);

        tester.setTime("day", 10);
        assertEquals(10, tester.day);

        tester.setTime("night", 3);
        assertEquals(3, tester.night);

        tester.setTime("weather", 1);
        assertEquals(1, tester.weather);

        tester.setTime("adverse", 1);
        assertEquals(1, tester.adverse);
    }

    @Test(expected = RuntimeException.class)
    public void setBadTotalTime() throws Exception {
        // test an invalid type
        tester.setTime("bad", 3);
    }

}