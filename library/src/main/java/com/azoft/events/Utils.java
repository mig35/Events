package com.azoft.events;

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

final class Utils {

    private static final Map<String, Integer> NAME_ANDROID_IDS = new HashMap<String, Integer>();

    static String getName(final int resourceId) {
        if (null != Events.appContext) {
            try {
                return Events.appContext.getResources().getResourceEntryName(resourceId);
            }
            catch (final Resources.NotFoundException ignored) {
                // Returning id itself (below)
            }
        }

        return String.valueOf(resourceId);
    }

    static String getClassName(final Object obj) {
        return null == obj ? "null" : obj.getClass().getSimpleName();
    }

    static int convertKeyToId(final String str) {
        if (null == str) {
            throw new RuntimeException("null string keys are not acceptable");
        }

        Integer androidId = NAME_ANDROID_IDS.get(str);
        if (null == androidId) {
            androidId = IdsGenerationUtil.generateViewId();
            NAME_ANDROID_IDS.put(str, androidId);
        }

        return androidId;
    }

    private Utils() {
    }
}