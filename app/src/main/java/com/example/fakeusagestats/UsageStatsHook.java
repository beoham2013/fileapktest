package com.example.fakeusagestats;

import android.app.usage.UsageStats;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.Arrays;
import java.util.List;

public class UsageStatsHook implements IXposedHookLoadPackage {

    private static final String TARGET_APP = "com.ldy.funpass";

    private static final List<String> FAKE_GAME_PACKAGES = Arrays.asList(
            "com.nexon.ma"
    );

    // 15 phút = 900.000 ms
    private static final long FAKE_TIME_MS = 15 * 60 * 1000L;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_APP)) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                "android.app.usage.UsageStatsManager",
                lpparam.classLoader,
                "queryUsageStats",
                int.class,
                long.class,
                long.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        List<UsageStats> statsList = (List<UsageStats>) param.getResult();

                        if (statsList == null || statsList.isEmpty()) {
                            return;
                        }

                        for (UsageStats stats : statsList) {
                            String pkgName = stats.getPackageName();

                            if (FAKE_GAME_PACKAGES.contains(pkgName)) {
                                try {
                                    XposedHelpers.setLongField(stats, "mTotalTimeInForeground", FAKE_TIME_MS);
                                    XposedHelpers.setLongField(stats, "mLastTimeUsed", System.currentTimeMillis());
                                } catch (Exception e) {
                                    XposedBridge.log("FakeUsageStats Error: " + e.getMessage());
                                }
                            }
                        }

                        param.setResult(statsList);
                    }
                }
        );
    }
}
