package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }
        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.color.ic_background_ocoder_default, R.mipmap.ic_foreground_ocoder_default, R.string.AppIconDefault),
        OCODER_NEON("OcoderNeonIcon", R.color.ic_background_ocoder_neon, R.mipmap.ic_foreground_ocoder_neon, R.string.AppIconOcoderNeon),
        OCODER_V2("OcoderV2Icon", R.color.ic_background_ocoder_v2, R.mipmap.ic_foreground_ocoder_v2, R.string.AppIconOcoderV2),
        OCODER_WHITE("OcoderWhiteIcon", R.color.ic_background_ocoder_white, R.mipmap.ic_foreground_ocoder_white, R.string.AppIconOcoderWhite),
        OXGRAM_VIVID("OxgramVividIcon", R.color.ic_background_oxgram_vivid, R.mipmap.ic_foreground_oxgram_vivid, R.string.AppIconOxgramVivid),
        OXGRAM_EMERALD("OxgramEmeraldIcon", R.color.ic_background_oxgram_emerald, R.mipmap.ic_foreground_oxgram_emerald, R.string.AppIconOxgramEmerald),
        OXGRAM_BLUE("OxgramBlueIcon", R.color.ic_background_oxgram_blue, R.mipmap.ic_foreground_oxgram_blue, R.string.AppIconOxgramBlue),
        OXGRAM_RED("OxgramRedIcon", R.color.ic_background_oxgram_red, R.mipmap.ic_foreground_oxgram_red, R.string.AppIconOxgramRed);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;
        public final boolean hidden;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "com.exteragram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean hidden) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = false;
            this.hidden = hidden;
        }
    }
}
