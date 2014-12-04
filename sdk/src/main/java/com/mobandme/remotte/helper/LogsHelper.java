package com.mobandme.remotte.helper;

/**
 * Copyright Mob&Me 2014 (@MobAndMe)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Website: http://mobandme.com
 * Contact: Txus Ballesteros <txus.ballesteros@mobandme.com>
 */

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

public final class LogsHelper {

    private static final String LOG_TAG = "Remotte";

    public static final int VERBOSE = 0;
    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int WARN = 3;
    public static final int ERROR = 4;
    public static final int WTF = 5;

    private static Context mContext;
    public static void    setContext(Context context) { mContext = context.getApplicationContext(); }
    public static Context getContext() { return mContext; }

    public static void log(int level, String msg) {
        if (isLoggable(level)) {
            String tag = LOG_TAG;
            switch (level) {
                case VERBOSE: Log.v(tag, msg); break;
                case DEBUG: Log.d(tag, msg); break;
                case INFO: Log.i(tag, msg); break;
                case WARN: Log.w(tag, msg); break;
                case ERROR: Log.e(tag, msg); break;
                case WTF: Log.w(tag, msg); break;
            }
        }
    }

    public static void log(int level, String msg, Throwable tr) {
        if (isLoggable(level)) {
            String tag = LOG_TAG;
            switch (level) {
                case VERBOSE: Log.v(tag, msg, tr); break;
                case DEBUG: Log.d(tag, msg, tr); break;
                case INFO: Log.i(tag, msg, tr); break;
                case WARN: Log.w(tag, msg, tr); break;
                case ERROR: Log.e(tag, msg, tr); break;
                case WTF: Log.w(tag, msg, tr); break;
            }
        }
    }

    private static boolean isLoggable(int level) {
        if (getContext() != null)
            return (isDebuggable() || level > DEBUG);
        else
            return false;
    }

    private static boolean isDebuggable() {
        boolean isDebuggable = (0 != (getContext().getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
        return isDebuggable;
    }
}
