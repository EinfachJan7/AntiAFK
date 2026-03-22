package de.antiafk.util;

/**
 * Einheitliche AFK-Zeitdarstellung für Placeholder, Befehle und Leaderboard.
 */
public final class TimeFormatUtil {

    private TimeFormatUtil() {
    }

    /**
     * @param showSeconds false: kompakt ohne Sekunden (außer unter 1 Minute);
     *                    true: überall Sekunden (z. B. 5m 30s, 2h 1m 5s)
     */
    public static String format(long totalSeconds, boolean showSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }

        if (!showSeconds) {
            if (totalSeconds < 60) {
                return totalSeconds + " Sekunden";
            }
            if (totalSeconds < 3600) {
                return (totalSeconds / 60) + " Minuten";
            }
            if (totalSeconds < 86400) {
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                return hours + "h " + minutes + "m";
            }
            long days = totalSeconds / 86400;
            long hours = (totalSeconds % 86400) / 3600;
            return days + "d " + hours + "h";
        }

        if (totalSeconds < 60) {
            return totalSeconds + " Sekunden";
        }
        if (totalSeconds < 3600) {
            long minutes = totalSeconds / 60;
            long secs = totalSeconds % 60;
            return minutes + "m " + secs + "s";
        }
        if (totalSeconds < 86400) {
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long secs = totalSeconds % 60;
            return hours + "h " + minutes + "m " + secs + "s";
        }
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        return days + "d " + hours + "h " + minutes + "m " + secs + "s";
    }
}
