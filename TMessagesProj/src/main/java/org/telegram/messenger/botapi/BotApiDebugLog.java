/*
 * Temporary file-based debug logger for diagnosing the bot-token feature on devices
 * (MIUI observed) that suppress third-party app logcat output entirely. Writes to
 * <filesDir>/botapi_debug.txt, readable via `adb shell run-as <pkg> cat files/botapi_debug.txt`.
 * Remove once the feature is verified working end-to-end.
 */

package org.telegram.messenger.botapi;

import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BotApiDebugLog {

    public static synchronized void log(String message) {
        try {
            File f = new File(ApplicationLoader.applicationContext.getFilesDir(), "botapi_debug.txt");
            try (FileWriter fw = new FileWriter(f, true)) {
                fw.write(new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date()) + " " + message + "\n");
            }
        } catch (Throwable ignored) {
        }
    }

    public static synchronized void log(String message, Throwable t) {
        try {
            File f = new File(ApplicationLoader.applicationContext.getFilesDir(), "botapi_debug.txt");
            try (FileWriter fw = new FileWriter(f, true)) {
                fw.write(new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date()) + " " + message + "\n");
                if (t != null) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    t.printStackTrace(new java.io.PrintWriter(sw));
                    fw.write(sw.toString());
                    fw.write("\n");
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
