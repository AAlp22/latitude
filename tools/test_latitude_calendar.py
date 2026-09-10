from pathlib import Path
import os
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/java/com/example/globe/world/LatitudeCalendarMath.java"
JAVAC = Path(os.environ.get("JAVAC", r"C:/Program Files/Java/jdk-17/bin/javac.exe"))
JAVA = Path(os.environ.get("JAVA", r"C:/Program Files/Java/jdk-17/bin/java.exe"))

HARNESS = r'''
import com.example.globe.world.LatitudeCalendarMath;
import com.example.globe.world.LatitudeCalendarMath.CalendarDate;
import com.example.globe.world.LatitudeCalendarMath.LunarPhase;
import com.example.globe.world.LatitudeCalendarMath.Season;
import com.example.globe.world.LatitudeCalendarMath.SolarPosition;

public final class LatitudeCalendarMathTest {
    private static final long TICKS_PER_DAY = LatitudeCalendarMath.TICKS_PER_DAY;

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        CalendarDate start = LatitudeCalendarMath.calendarAt(0L);
        check(start.year() == 1 && start.dayOfYear() == 1, "calendar must start at year 1 day 1");
        check(start.month() == 1 && start.dayOfMonth() == 1, "month must start at 1/1");
        check(start.weekOfYear() == 1 && start.weekOfMonth() == 1 && start.dayOfWeek() == 1, "week counters must start at 1");
        check(start.season() == Season.SPRING, "day 1 must be northern Spring");
        check(start.lunarDay() == 1 && start.lunarPhase() == LunarPhase.NEW, "lunar cycle must start new");

        CalendarDate summer = LatitudeCalendarMath.calendarAt(64L * TICKS_PER_DAY);
        check(summer.season() == Season.SUMMER && summer.month() == 3, "day 65 must start Summer/month 3");
        check(summer.dayOfYear() == 65 && summer.dayOfMonth() == 1, "season boundary date is wrong");

        CalendarDate monthTwo = LatitudeCalendarMath.calendarAt(32L * TICKS_PER_DAY);
        check(monthTwo.month() == 2 && monthTwo.dayOfMonth() == 1 && monthTwo.weekOfMonth() == 1,
                "32-day month boundary is wrong");

        CalendarDate fourWeeks = LatitudeCalendarMath.calendarAt(24L * TICKS_PER_DAY);
        check(fourWeeks.month() == 1 && fourWeeks.weekOfMonth() == 4,
                "8-day weeks must produce four weeks per month");

        CalendarDate nextYear = LatitudeCalendarMath.calendarAt(256L * TICKS_PER_DAY);
        check(nextYear.year() == 2 && nextYear.dayOfYear() == 1 && nextYear.season() == Season.SPRING, "256-day year boundary is wrong");

        CalendarDate lunar = LatitudeCalendarMath.calendarAt(4L * TICKS_PER_DAY);
        check(lunar.lunarDay() == 5 && lunar.lunarPhase() == LunarPhase.WAXING_CRESCENT, "32-day lunar phase mapping is wrong");

        long northernSummerNoon = 64L * TICKS_PER_DAY + TICKS_PER_DAY / 4L;
        SolarPosition north = LatitudeCalendarMath.solarAt(northernSummerNoon, 60.0);
        SolarPosition south = LatitudeCalendarMath.solarAt(northernSummerNoon, -60.0);
        check(north.localSeason() == Season.SUMMER && south.localSeason() == Season.WINTER, "hemisphere season reversal is wrong");
        check(north.daylightFraction() > 0.5 && south.daylightFraction() < 0.5, "hemisphere daylight reversal is wrong");
        check(north.solarDeclinationDegrees() > 0.0, "northern summer declination must be positive");

        SolarPosition primeMeridianNoon = LatitudeCalendarMath.solarAt(TICKS_PER_DAY / 4L, 0.0, 0.0);
        SolarPosition eastQuarterTurn = LatitudeCalendarMath.solarAt(TICKS_PER_DAY / 4L, 0.0, 90.0);
        SolarPosition westQuarterTurn = LatitudeCalendarMath.solarAt(TICKS_PER_DAY / 4L, 0.0, -90.0);
        check(Math.abs(primeMeridianNoon.localSolarTimeHours() - 12.0) < 0.01, "6,000 ticks must be local solar noon");
        check(Math.abs(eastQuarterTurn.localSolarTimeHours() - 18.0) < 0.01, "+90 longitude must be six hours later");
        check(Math.abs(westQuarterTurn.localSolarTimeHours() - 6.0) < 0.01, "-90 longitude must be six hours earlier");

        CalendarDate localNextDay = LatitudeCalendarMath.calendarAt(0L, 180.0);
        check(localNextDay.dayOfYear() == 1, "longitude date offset must not advance at dawn");
        CalendarDate localDateAfterMidnight = LatitudeCalendarMath.calendarAt(18L * TICKS_PER_DAY / 24L, 180.0);
        check(localDateAfterMidnight.dayOfYear() == 2, "longitude date offset must cross local midnight");

        SolarPosition splitClockDawn = LatitudeCalendarMath.solarAt(64L * TICKS_PER_DAY, 0L, 60.0, 0.0);
        SolarPosition splitClockNoon = LatitudeCalendarMath.solarAt(64L * TICKS_PER_DAY, TICKS_PER_DAY / 4L, 60.0, 0.0);
        check(Math.abs(splitClockDawn.solarDeclinationDegrees() - splitClockNoon.solarDeclinationDegrees()) < 0.0001,
                "setting time of day must not change seasonal declination");
        check(splitClockNoon.solarElevationRadians() > splitClockDawn.solarElevationRadians(),
                "setting time of day must move the Sun above the horizon");
        check(LatitudeCalendarMath.calendarAt(64L * TICKS_PER_DAY, 1000L, 0.0).dayOfYear() == 65,
                "mutable time-of-day must not reset elapsed calendar day");

        SolarPosition equatorEquinox = LatitudeCalendarMath.solarAt(TICKS_PER_DAY / 4L, 0.0);
        SolarPosition equatorSolstice = LatitudeCalendarMath.solarAt(northernSummerNoon, 0.0);
        check(Math.abs(equatorEquinox.daylightFraction() - 0.5) < 0.01, "equatorial daylight must stay near 12 hours");
        check(equatorEquinox.equatorialWetness() > 0.9, "equinox wet-season signal must be high at the equator");
        check(equatorSolstice.equatorialWetness() < 0.1, "solstice wet-season signal must be low at the equator");

        SolarPosition northPoleSummer = LatitudeCalendarMath.solarAt(northernSummerNoon, 90.0);
        SolarPosition northPoleWinter = LatitudeCalendarMath.solarAt(192L * TICKS_PER_DAY + TICKS_PER_DAY / 2L, 90.0);
        check(northPoleSummer.polarDay() && northPoleSummer.daylightFraction() == 1.0, "north polar summer must be polar day");
        check(northPoleWinter.polarNight() && northPoleWinter.daylightFraction() == 0.0, "north polar winter must be polar night");

        System.out.println("calendar and solar math checks passed");
    }
}
'''

if not SOURCE.is_file():
    print(f"RED: missing production source {SOURCE}")
    raise SystemExit(1)

with tempfile.TemporaryDirectory(prefix="latitude-calendar-test-") as temp:
    temp_path = Path(temp)
    harness = temp_path / "LatitudeCalendarMathTest.java"
    harness.write_text(HARNESS, encoding="utf-8")
    subprocess.run([str(JAVAC), "-d", str(temp_path), str(SOURCE), str(harness)], check=True)
    subprocess.run([str(JAVA), "-cp", str(temp_path), "LatitudeCalendarMathTest"], check=True)
