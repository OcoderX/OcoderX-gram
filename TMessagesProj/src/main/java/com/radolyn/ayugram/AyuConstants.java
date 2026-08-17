/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram;

import org.telegram.messenger.BuildVars;

public class AyuConstants {
    public static final long[] OFFICIAL_CHANNELS = {
            1905581924, // @ayugramchat
            1794457129, // @ayugram1338
            1434550607, // @radolyn
    };
    public static final long[] DEVS = {
            139303278, // @alexeyzavar
            778327202, // @sharapagorg
            963494570, // @Zanko_no_tachi
            238292700, // @MaxPlays
            1795176335, // @radolyn_services
    };

    public static final int DOCUMENT_TYPE_NONE = 0;
    public static final int DOCUMENT_TYPE_PHOTO = 1;
    public static final int DOCUMENT_TYPE_STICKER = 2;
    public static final int DOCUMENT_TYPE_FILE = 3;

    public static final int OPTION_HISTORY = 1338_01;
    public static final int OPTION_TTL = 1338_02;
    public static final int OPTION_READ_UNTIL = 1338_03;
    public static final int OPTION_RAW_VIEWER = 1338_04;
    public static final int OPTION_COPY_ID = 1338_05;
    public static final int OPTION_USER_HISTORY = 1338_06;
    public static final int OPTION_CONVERT_ROUND_VIDEO = 1338_07;
    public static final int OPTION_DELETE_ALL_FROM_USER = 1338_08;
    public static final int OPTION_EXPORT_CHAT = 1338_09;

    public static final int DRAWER_TOGGLE_GHOST = 1000;
    public static final int DRAWER_KILL_APP = 1001;
    public static final int DRAWER_OCODER_PREFS = 1002;
    public static final int DRAWER_DELETED_MESSAGES = 1003;
    public static final int DRAWER_CHANNEL = 1004;
    public static final int DRAWER_ADMIN = 1005;

    public static final int MESSAGE_EDITED_NOTIFICATION = 6968;
    public static final int MESSAGES_DELETED_NOTIFICATION = 6969;
    public static final int AYUSYNC_STATE_CHANGED = 6970;
    public static final int AYUSYNC_LAST_SENT_CHANGED = 6971;
    public static final int AYUSYNC_LAST_RECEIVED_CHANGED = 6972;
    public static final int AYUSYNC_REGISTER_STATUS_CODE_CHANGED = 6973;

    public static String DEFAULT_DELETED_MARK = "❌";
    public static String DEFAULT_AYUSYNC_SERVER = BuildVars.isBetaApp() ? "ayusync-dev.radolyn.com:5000" : "ayusync.cloud";

    public static String AYU_DATABASE = "ayu-data";

    public static String APP_GITHUB = "OcoderX/OcoderX-gram";
    public static String APP_NAME = "Ox-gram";

    public static String BUILD_STORE_PACKAGE = "com.android.vending";
    public static String BUILD_ORIGINAL_PACKAGE = "org.telegram.messenger";
}
