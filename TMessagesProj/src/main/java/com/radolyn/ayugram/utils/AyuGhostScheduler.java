/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.utils;

import com.radolyn.ayugram.AyuConfig;

import java.util.Calendar;

public class AyuGhostScheduler {
    private static boolean appliedByScheduler;

    public static void checkAndApply() {
        if (!AyuConfig.ghostModeScheduleEnabled) {
            return;
        }

        var hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        var inWindow = isInWindow(hour, AyuConfig.ghostModeScheduleStartHour, AyuConfig.ghostModeScheduleEndHour);

        if (inWindow && !AyuConfig.isGhostModeActive()) {
            AyuConfig.setGhostMode(true);
            appliedByScheduler = true;
        } else if (!inWindow && appliedByScheduler && AyuConfig.isGhostModeActive()) {
            AyuConfig.setGhostMode(false);
            appliedByScheduler = false;
        }
    }

    private static boolean isInWindow(int hour, int start, int end) {
        if (start == end) {
            // 24h window
            return true;
        }

        if (start < end) {
            return hour >= start && hour < end;
        }

        // wraps around midnight, e.g. 23 -> 7
        return hour >= start || hour < end;
    }
}
