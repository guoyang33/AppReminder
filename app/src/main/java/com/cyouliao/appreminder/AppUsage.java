package com.cyouliao.appreminder;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AppUsage {

    private UsageStatsManager usageStatsManager;
//    private AppCategory appCategory;

    public AppUsage(Context context, UsageStatsManager usageStatsManager) {

        this.usageStatsManager = usageStatsManager;
//        appCategory = new AppCategory(context);

    }

    public UsageStatsManager getUsageStatsManager() {
        return usageStatsManager;
    }

    public List<UsageStats> queryUsageStatsByDate(String dateString) {
        List<UsageStats> usageStatsList = null;
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateString);
            endDate = new Date(startDate.getTime() + (24 * 60 * 60 * 1000));
            usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startDate.getTime(), endDate.getTime());
//            Map<String, String> appCategoryMap = appCategory.getAppCategoryMap();
//            ArrayList<String> newPackageNameArray = new ArrayList<>();
            for (UsageStats usageStats :usageStatsList) {
                // 使用時間 api 29以前不能用getTotalTimeVisible()
                Long usageTime = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    usageTime = (usageStats.getTotalTimeVisible()-usageStats.getTotalTimeVisible()%1000)/1000;
//                    System.out.println("total time visiblel: " + (usageStats.getTotalTimeVisible()-usageStats.getTotalTimeVisible()%1000)/1000);
                } else {
                    usageTime = (usageStats.getTotalTimeInForeground()-usageStats.getTotalTimeInForeground()%1000)/1000;
//                    System.out.println("total time visiblel: " + (usageStats.getTotalTimeInForeground()-usageStats.getTotalTimeInForeground()%1000)/1000);
                }
//                if (usageTime != null) {
//                    String packageName = usageStats.getPackageName();
//                    if (!appCategoryMap.containsKey(packageName)) {
//                        newPackageNameArray.add(packageName);
//                    } else {
//                        String category = appCategoryMap.get(packageName);
//                        System.out.println("package name: " + usageStats.getPackageName());
//                        System.out.println("app category: " + category);
//                        System.out.println("usage time: " + usageTime);
//                    }
//                }
            }
//            System.out.println("new package name array size: " + newPackageNameArray.size());
//            if (newPackageNameArray.size() > 0) {
//                appCategory.fetchRemote(newPackageNameArray);
//            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return usageStatsList;
    }

}
