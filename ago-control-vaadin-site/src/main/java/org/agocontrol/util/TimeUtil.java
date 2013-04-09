package org.agocontrol.util;

import java.util.Date;
import java.util.TimeZone;

/**
 * Time utilities.
 */
public class TimeUtil {

    /**
     * Private constructor to disable construction.
     */
    private TimeUtil() {
    }

    /**
     * Converts UTC time to local time.
     * @param utcTime the time in UTC
     * @return the time in local time zone time
     */
    public static Date toLocalTime(final Date utcTime) {
        final TimeZone tz = TimeZone.getTimeZone("Europe/Helsinki");
        Date ret = new Date(utcTime.getTime() + tz.getRawOffset());

        // if we are now in DST, back off by the delta.  Note that we are checking the GMT date, this is the KEY.
        if (tz.inDaylightTime(ret)) {
            Date dstDate = new Date(ret.getTime() + tz.getDSTSavings());

            // check to make sure we have not crossed back into standard time
            // this happens when we are on the cusp of DST (7pm the day before the change for PDT)
            if (tz.inDaylightTime(dstDate))
            {
                ret = dstDate;
            }
        }

        return ret;
    }
}
