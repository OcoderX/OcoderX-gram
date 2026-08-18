/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 */
package org.telegram.messenger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Estimates the approximate registration ("account creation") date of a Telegram
 * account from its numeric user id.
 *
 * Telegram's API (including the TL layer this app targets) does not expose the
 * real account-creation date anywhere. There is no such field on TL_user /
 * TL_userFull in this schema (or in any officially documented layer). Telegram
 * user ids, however, are handed out roughly sequentially over time, so the
 * community has crowdsourced a set of "id -> known/observed registration date"
 * reference points (from early-account screenshots, self-reported dates, bots
 * that logged the first time an id was seen, etc). This class linearly
 * interpolates between those reference points, sorted by id, to produce a
 * rough estimate.
 *
 * IMPORTANT: this is a heuristic, not an authoritative value. Telegram never
 * guaranteed strictly monotonic id allocation and the reference data itself is
 * self-reported/crowdsourced, so results can realistically be off by weeks or
 * months (more so for very old or very new accounts, where reference points
 * are sparser). Always present the result to the user as an approximation
 * (e.g. prefixed with "~") - never as a verified fact.
 *
 * Reference points below were collected from publicly available crowdsourced
 * "Telegram id age" datasets (the kind of table used by various Telegram
 * client mods / "creation date" bots), spanning account #0 (August 2013)
 * through late 2025.
 */
public class RegistrationDateEstimator {

    // {telegram user id, "yyyy-MM-dd"} - MUST stay sorted by id ascending.
    private static final Object[][] RAW_REFERENCE_POINTS = {
            {0L, "2013-08-14"},
            {2768409L, "2013-11-01"},
            {23646077L, "2014-02-26"},
            {46145305L, "2014-05-15"},
            {63263518L, "2014-10-27"},
            {101260938L, "2015-03-06"},
            {124872445L, "2015-08-17"},
            {148670295L, "2016-01-08"},
            {181783990L, "2016-04-10"},
            {225034354L, "2016-06-18"},
            {285253072L, "2016-10-18"},
            {328594461L, "2017-01-28"},
            {369669043L, "2017-03-31"},
            {400169472L, "2017-07-31"},
            {805158066L, "2019-07-15"},
            {1974255900L, "2021-10-12"},
            {5031711230L, "2021-12-06"},
            {5177789190L, "2022-01-24"},
            {5179102906L, "2022-03-12"},
            {5210565134L, "2022-02-09"},
            {5271530336L, "2022-04-30"},
            {5394432429L, "2022-05-23"},
            {5468950164L, "2022-07-14"},
            {5505809357L, "2022-05-27"},
            {5519218712L, "2022-08-14"},
            {5598262640L, "2022-06-11"},
            {5721138769L, "2022-09-23"},
            {5744374534L, "2022-10-10"},
            {5795660441L, "2022-11-06"},
            {5862080962L, "2022-12-13"},
            {5869978651L, "2023-03-24"},
            {5931294587L, "2022-11-19"},
            {5964221956L, "2023-01-09"},
            {5983753471L, "2022-12-23"},
            {6074830852L, "2023-05-01"},
            {6180394472L, "2023-05-29"},
            {6271031786L, "2023-02-12"},
            {6277658932L, "2023-03-17"},
            {6326011828L, "2023-07-07"},
            {6523424924L, "2023-08-02"},
            {6684986493L, "2023-09-25"},
            {6732829831L, "2024-02-11"},
            {6827058708L, "2023-11-06"},
            {6903333095L, "2024-01-31"},
            {6926984452L, "2024-01-09"},
            {6947316117L, "2023-12-15"},
            {7002435197L, "2024-04-06"},
            {7085776398L, "2024-05-10"},
            {7104310277L, "2024-04-19"},
            {7242296450L, "2024-05-29"},
            {7357703634L, "2024-09-10"},
            {7363299295L, "2024-07-25"},
            {7409259451L, "2024-06-20"},
            {7450316621L, "2024-12-02"},
            {7458668365L, "2024-08-02"},
            {7747102337L, "2024-11-06"},
            {7825518194L, "2025-01-16"},
            {7829910989L, "2025-05-11"},
            {7832006200L, "2024-09-19"},
            {7870888707L, "2025-06-08"},
            {7899152800L, "2025-04-08"},
            {7915901421L, "2025-07-07"},
            {8017192943L, "2025-10-05"},
            {8044853035L, "2025-03-20"},
            {8096742229L, "2025-08-31"},
            {8173852075L, "2025-02-21"},
            {8238766847L, "2025-07-31"},
            {8393200797L, "2025-10-25"},
            {8461579295L, "2025-09-11"},
            {8559682245L, "2025-11-11"},
    };

    private static long[] referenceIds;
    private static long[] referenceDatesMs;

    private static synchronized void ensureParsed() {
        if (referenceIds != null) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        int n = RAW_REFERENCE_POINTS.length;
        long[] ids = new long[n];
        long[] dates = new long[n];
        for (int i = 0; i < n; i++) {
            ids[i] = (Long) RAW_REFERENCE_POINTS[i][0];
            long t;
            try {
                t = sdf.parse((String) RAW_REFERENCE_POINTS[i][1]).getTime();
            } catch (ParseException e) {
                t = 0;
            }
            dates[i] = t;
        }
        referenceIds = ids;
        referenceDatesMs = dates;
    }

    /**
     * @param userId numeric Telegram user id
     * @return estimated registration timestamp in epoch milliseconds (UTC), or -1
     * if it cannot be estimated (invalid id).
     */
    public static long estimateRegistrationDateMs(long userId) {
        if (userId <= 0) {
            return -1;
        }
        ensureParsed();
        int n = referenceIds.length;
        if (n == 0) {
            return -1;
        }
        if (userId <= referenceIds[0]) {
            return referenceDatesMs[0];
        }
        if (userId >= referenceIds[n - 1]) {
            if (n < 2) {
                return referenceDatesMs[n - 1];
            }
            long idSpan = referenceIds[n - 1] - referenceIds[n - 2];
            long timeSpan = referenceDatesMs[n - 1] - referenceDatesMs[n - 2];
            long extrapolated;
            if (idSpan <= 0) {
                extrapolated = referenceDatesMs[n - 1];
            } else {
                double rate = (double) timeSpan / (double) idSpan;
                extrapolated = referenceDatesMs[n - 1] + (long) ((userId - referenceIds[n - 1]) * rate);
            }
            long now = System.currentTimeMillis();
            return Math.min(extrapolated, now);
        }
        int lo = 0, hi = n - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (referenceIds[mid] <= userId) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        long id0 = referenceIds[lo], id1 = referenceIds[hi];
        long t0 = referenceDatesMs[lo], t1 = referenceDatesMs[hi];
        if (id1 == id0) {
            return t0;
        }
        double frac = (double) (userId - id0) / (double) (id1 - id0);
        return t0 + (long) (frac * (t1 - t0));
    }
}
