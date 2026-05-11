package com.studytimer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvHours, tvMinutes, tvSeconds, tvStatus;
    private TextView tvTodayTotal, tvCurrentDate;
    private Button btnStartPause, btnSave, btnReset, btnShowRecords, btnBack;
    private LinearLayout layoutTimer, layoutRecords;
    private ListView lvRecords;

    private TimerService timerService;
    private boolean isBound = false;
    private DatabaseHelper dbHelper;

    private BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long elapsed = intent.getLongExtra("elapsed", 0);
            boolean running = intent.getBooleanExtra("running", false);
            updateTimerDisplay(elapsed, running);
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            TimerService.LocalBinder b = (TimerService.LocalBinder) binder;
            timerService = b.getService();
            isBound = true;
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Screen ON rakhne ke liye
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        // Views
        tvHours      = findViewById(R.id.tvHours);
        tvMinutes    = findViewById(R.id.tvMinutes);
        tvSeconds    = findViewById(R.id.tvSeconds);
        tvStatus     = findViewById(R.id.tvStatus);
        tvTodayTotal = findViewById(R.id.tvTodayTotal);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnSave       = findViewById(R.id.btnSave);
        btnReset      = findViewById(R.id.btnReset);
        btnShowRecords = findViewById(R.id.btnShowRecords);
        btnBack       = findViewById(R.id.btnBack);
        layoutTimer   = findViewById(R.id.layoutTimer);
        layoutRecords = findViewById(R.id.layoutRecords);
        lvRecords     = findViewById(R.id.lvRecords);

        // Aaj ki date dikhao
        String today = new SimpleDateFormat("dd MMMM yyyy, EEEE", new Locale("hi")).format(new Date());
        tvCurrentDate.setText(today);

        // Buttons
        btnStartPause.setOnClickListener(v -> {
            if (isBound) {
                if (timerService.isRunning()) {
                    timerService.pause();
                    btnStartPause.setText("▶  SHURU KARO");
                    btnStartPause.setBackgroundColor(0xFF1B5E20);
                } else {
                    timerService.start();
                    btnStartPause.setText("⏸  BAND KARO");
                    btnStartPause.setBackgroundColor(0xFFB71C1C);
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            if (isBound) {
                long elapsed = timerService.getElapsed();
                if (elapsed == 0) {
                    Toast.makeText(this, "Pehle timer chalao!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Aaj ki date ke record mein add karo
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                dbHelper.addSession(date, elapsed);
                timerService.resetTimer();
                updateTodayTotal();
                Toast.makeText(this, "✓ Padhai save ho gayi!", Toast.LENGTH_SHORT).show();
                btnStartPause.setText("▶  SHURU KARO");
                btnStartPause.setBackgroundColor(0xFF1B5E20);
            }
        });

        btnReset.setOnClickListener(v -> {
            if (isBound) {
                timerService.resetTimer();
                updateTimerDisplay(0, false);
                Toast.makeText(this, "Timer reset hua (save nahi hua)", Toast.LENGTH_SHORT).show();
            }
        });

        btnShowRecords.setOnClickListener(v -> {
            showRecordsScreen();
        });

        btnBack.setOnClickListener(v -> {
            layoutRecords.setVisibility(View.GONE);
            layoutTimer.setVisibility(View.VISIBLE);
        });

        // Service start karo
        Intent serviceIntent = new Intent(this, TimerService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        updateTodayTotal();
    }

    private void updateTimerDisplay(long elapsedSeconds, boolean running) {
        long h = elapsedSeconds / 3600;
        long m = (elapsedSeconds % 3600) / 60;
        long s = elapsedSeconds % 60;
        tvHours.setText(String.format(Locale.getDefault(), "%02d", h));
        tvMinutes.setText(String.format(Locale.getDefault(), "%02d", m));
        tvSeconds.setText(String.format(Locale.getDefault(), "%02d", s));
        tvStatus.setText(running ? "🟢 CHAL RAHA HAI" : "⏹ RUKA HUA");
        tvStatus.setTextColor(running ? 0xFF4CAF50 : 0xFFFF9800);
        btnStartPause.setText(running ? "⏸  BAND KARO" : "▶  SHURU KARO");
        btnStartPause.setBackgroundColor(running ? 0xFFB71C1C : 0xFF1B5E20);
    }

    private void updateUI() {
        if (isBound) {
            updateTimerDisplay(timerService.getElapsed(), timerService.isRunning());
        }
    }

    private void updateTodayTotal() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long total = dbHelper.getTotalForDate(date);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        tvTodayTotal.setText(String.format(Locale.getDefault(), "Aaj: %dh %dm padha", h, m));
    }

    private void showRecordsScreen() {
        List<String> records = dbHelper.getAllRecords();
        ArrayList<String> display = new ArrayList<>();
        if (records.isEmpty()) {
            display.add("Abhi koi record nahi hai.");
        } else {
            display.addAll(records);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, display);
        lvRecords.setAdapter(adapter);
        layoutTimer.setVisibility(View.GONE);
        layoutRecords.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("TIMER_UPDATE");
        registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        updateUI();
        updateTodayTotal();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try { unregisterReceiver(timerReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
