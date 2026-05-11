package com.studytimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

public class TimerService extends Service {

    private static final String CHANNEL_ID = "study_timer_channel";
    private static final int NOTIF_ID = 1;

    private final IBinder binder = new LocalBinder();
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private long elapsedSeconds = 0;
    private PowerManager.WakeLock wakeLock;

    public class LocalBinder extends Binder {
        TimerService getService() { return TimerService.this; }
    }

    private Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (running) {
                elapsedSeconds++;
                broadcastUpdate();
                updateNotification();
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // CPU on rakhne ke liye (screen band hone pe bhi)
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "StudyTimer::WakeLock"
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY; // System band kare to khud restart hoga
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void start() {
        if (!running) {
            running = true;
            if (!wakeLock.isHeld()) wakeLock.acquire();
            handler.post(ticker);
        }
    }

    public void pause() {
        running = false;
        if (wakeLock.isHeld()) wakeLock.release();
        handler.removeCallbacks(ticker);
        broadcastUpdate();
        updateNotification();
    }

    public void resetTimer() {
        pause();
        elapsedSeconds = 0;
        broadcastUpdate();
        updateNotification();
    }

    public boolean isRunning() { return running; }
    public long getElapsed() { return elapsedSeconds; }

    private void broadcastUpdate() {
        Intent intent = new Intent("TIMER_UPDATE");
        intent.putExtra("elapsed", elapsedSeconds);
        intent.putExtra("running", running);
        sendBroadcast(intent);
    }

    private String formatTime(long secs) {
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private Notification buildNotification() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String status = running ? "▶ Chal raha hai" : "⏸ Ruka hua";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("📚 Study Timer")
                .setContentText(status + " — " + formatTime(elapsedSeconds))
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, buildNotification());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Study Timer",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Study timer chal raha hai");
            channel.setSound(null, null);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(ticker);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }
}
