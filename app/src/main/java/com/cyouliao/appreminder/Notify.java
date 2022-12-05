package com.cyouliao.appreminder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Notify {

    public static String DATA_MAKEUP_CHANNEL_ID = "DataMakeup";
    public static String TODO_CHANNEL_ID = "Todo";

    private NotificationChannel channelDataMakeup;
    private NotificationChannel channelTodo;

    public Notify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelDataMakeup = new NotificationChannel(DATA_MAKEUP_CHANNEL_ID, "DataMakeupChannel", NotificationManager.IMPORTANCE_HIGH);
            channelTodo = new NotificationChannel(TODO_CHANNEL_ID, "TodoChannel", NotificationManager.IMPORTANCE_HIGH);
        }
    }

    // 上傳資料Activity
    public void pushDataMakeup(Context context) {
        Intent intent = new Intent(context, DataUploadActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DATA_MAKEUP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("尚有資料未上傳")
                .setContentText("點此進行手動作業")
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(1,builder.build());
    }

    public void pushTodo(Context context, String type) {
        Intent intent = new Intent(context, WebActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        String title = null;
        String text = null;
        switch (type) {
            case "high_risk_situation":
                title = "高風險情境評估未完成";
                text = "請點此進行填寫";
                break;
            case "set_goal":
                title = "本週減量目標尚末設定";
                text = "請點此進行設定";
                break;
            case "training_strategy":
                title = "今日尚有認知策略訓練未完成";
                text = "請點此完成訓練";
                break;
            case "self_evaluate":
                title = "您有自我評估單還未完成填寫";
                text = "請點此進行填寫";
                break;
            case "set_goal_makeup":
                title = "您尚有往期減量目標尚末設定";
                text = "請點此補設定";
                break;
            case "training_strategy_makeup":
                title = "您尚有往期認知策略訓練未完成";
                text = "請點此補訓練";
                break;
        }
        if (title != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TODO_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManagerCompat
                    = NotificationManagerCompat.from(context);
            notificationManagerCompat.notify(2,builder.build());
        }
    }

    public NotificationChannel getChannelDataMakeup() {
        return channelDataMakeup;
    }

    public NotificationChannel getChannelTodo() {
        return channelTodo;
    }

}
