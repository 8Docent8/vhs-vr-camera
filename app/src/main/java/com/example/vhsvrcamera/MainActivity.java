package com.example.vhsvrcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean isRunning = false;
    private Thread renderThread;
    private Paint paint;
    private Random random;
    private Handler handler;
    private TextView statusText;
    
    private int screenWidth = 1080;
    private int screenHeight = 1920;
    
    // VHS эффекты
    private int scanLineY = 0;
    private float waveOffset = 0;
    private long lastUpdate = 0;
    
    // Разрешения
    private static final int CAMERA_PERMISSION_CODE = 101;
    private static final int STORAGE_PERMISSION_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Полноэкранный режим
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
        setContentView(R.layout.activity_main);
        
        // Инициализация
        surfaceView = findViewById(R.id.surface_view);
        statusText = findViewById(R.id.status_text);
        
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        
        paint = new Paint();
        random = new Random();
        handler = new Handler();
        
        // Запрашиваем разрешения
        checkPermissions();
        
        // Обновляем статус
        updateStatus("Инициализация...");
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        } else {
            onPermissionsGranted();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionsGranted();
            } else {
                updateStatus("Нужно разрешение на камеру!");
                Toast.makeText(this, "Без камеры приложение не работает", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void onPermissionsGranted() {
        updateStatus("Готово! Поместите в VR очки");
        startRendering();
    }
    
    private void updateStatus(final String message) {
        handler.post(() -> {
            if (statusText != null) {
                statusText.setText(message);
            }
        });
    }
    
    private void startRendering() {
        if (isRunning) return;
        
        isRunning = true;
        renderThread = new Thread(() -> {
            while (isRunning) {
                renderFrame();
                try {
                    Thread.sleep(16); // ~60 FPS
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        renderThread.start();
    }
    
    private void renderFrame() {
        if (!surfaceHolder.getSurface().isValid()) return;
        
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) return;
        
        // Очищаем canvas
        canvas.drawColor(Color.BLACK);
        
        // Получаем размеры
        screenWidth = canvas.getWidth();
        screenHeight = canvas.getHeight();
        
        // Создаем VHS эффект
        drawVHSEffect(canvas);
        
        // Создаем VR эффект (два экрана)
        drawVREffect(canvas);
        
        surfaceHolder.unlockCanvasAndPost(canvas);
    }
    
    private void drawVHSEffect(Canvas canvas) {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdate) / 1000f;
        lastUpdate = currentTime;
        
        // Обновляем волновой эффект
        waveOffset += deltaTime * 2f;
        
        // 1. Шум и помехи
        drawNoise(canvas);
        
        // 2. Сканирующие линии
        drawScanLines(canvas);
        
        // 3. Цветовые полосы
        drawColorBands(canvas);
        
        // 4. Угловые искажения
        drawCornerDistortion(canvas);
    }
    
    private void drawNoise(Canvas canvas) {
        // Статический шум
        for (int i = 0; i < 200; i++) {
            int x = random.nextInt(screenWidth);
            int y = random.nextInt(screenHeight);
            int size = 1 + random.nextInt(3);
            
            int brightness = 50 + random.nextInt(150);
            paint.setColor(Color.rgb(brightness, brightness, brightness));
            paint.setAlpha(30 + random.nextInt(70));
            
            canvas.drawRect(x, y, x + size, y + size, paint);
        }
        
        // Зеленые пиксели (как в старых мониторах)
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(screenWidth);
            int y = random.nextInt(screenHeight);
            
            paint.setColor(Color.rgb(0, 255, 0));
            paint.setAlpha(100 + random.nextInt(100));
            
            canvas.drawPoint(x, y, paint);
        }
    }
    
    private void drawScanLines(Canvas canvas) {
        // Двигаем сканирующую линию
        scanLineY = (scanLineY + 5) % screenHeight;
        
        // Основная сканирующая линия
        paint.setColor(Color.rgb(0, 255, 0));
        paint.setAlpha(80);
        paint.setStrokeWidth(3);
        canvas.drawLine(0, scanLineY, screenWidth, scanLineY, paint);
        
        // Горизонтальные линии (каждые 2 пикселя)
        paint.setColor(Color.rgb(100, 100, 100));
        paint.setAlpha(30);
        paint.setStrokeWidth(1);
        
        for (int y = 0; y < screenHeight; y += 2) {
            canvas.drawLine(0, y, screenWidth, y, paint);
        }
        
        // Вертикальные линии для эффекта VHS
        paint.setColor(Color.rgb(50, 50, 50));
        paint.setAlpha(20);
        
        for (int x = 0; x < screenWidth; x += 4) {
            canvas.drawLine(x, 0, x, screenHeight, paint);
        }
    }
    
    private void drawColorBands(Canvas canvas) {
        // Красные полосы (хроматическая аберрация)
        paint.setColor(Color.rgb(255, 0, 0));
        paint.setAlpha(30);
        
        for (int i = 0; i < 3; i++) {
            int offset = (int) (Math.sin(waveOffset * 0.5 + i) * 10);
            int y = screenHeight / 4 * (i + 1) + offset;
            canvas.drawRect(0, y, screenWidth, y + 20, paint);
        }
        
        // Синие полосы
        paint.setColor(Color.rgb(0, 0, 255));
        paint.setAlpha(30);
        
        for (int i = 0; i < 3; i++) {
            int offset = (int) (Math.cos(waveOffset * 0.7 + i) * 8);
            int y = screenHeight / 5 * (i + 2) + offset;
            canvas.drawRect(0, y, screenWidth, y + 15, paint);
        }
    }
    
    private void drawCornerDistortion(Canvas canvas) {
        // Искажения по углам
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.rgb(0, 200, 0));
        paint.setAlpha(50);
        
        // Верхний левый угол
        canvas.drawCircle(50, 50, 100 + (float) Math.sin(waveOffset) * 20, paint);
        
        // Нижний правый угол
        canvas.drawCircle(screenWidth - 50, screenHeight - 50, 
                        80 + (float) Math.cos(waveOffset) * 15, paint);
        
        paint.setStyle(Paint.Style.FILL);
    }
    
    private void drawVREffect(Canvas canvas) {
        // Разделяем экран на два для VR
        int halfWidth = screenWidth / 2;
        
        // Левый глаз (красноватый оттенок)
        paint.setColor(Color.rgb(150, 100, 100));
        paint.setAlpha(30);
        canvas.drawRect(0, 0, halfWidth, screenHeight, paint);
        
        // Правый глаз (синеватый оттенок)
        paint.setColor(Color.rgb(100, 100, 150));
        paint.setAlpha(30);
        canvas.drawRect(halfWidth, 0, screenWidth, screenHeight, paint);
        
        // Разделительная линия
        paint.setColor(Color.WHITE);
        paint.setAlpha(100);
        paint.setStrokeWidth(3);
        canvas.drawLine(halfWidth, 0, halfWidth, screenHeight, paint);
        
        // Добавляем кресты для фокусировки
        drawFocusCross(canvas, halfWidth / 2, screenHeight / 2, Color.GREEN);
        drawFocusCross(canvas, halfWidth + halfWidth / 2, screenHeight / 2, Color.CYAN);
    }
    
    private void drawFocusCross(Canvas canvas, int x, int y, int color) {
        paint.setColor(color);
        paint.setAlpha(150);
        paint.setStrokeWidth(2);
        
        int size = 30;
        canvas.drawLine(x - size, y, x + size, y, paint);
        canvas.drawLine(x, y - size, x, y + size, paint);
        
        // Круг вокруг креста
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawCircle(x, y, size * 2, paint);
        paint.setStyle(Paint.Style.FILL);
    }
    
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        updateStatus("Поверхность создана");
    }
    
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        updateStatus("Экран: " + width + "x" + height);
    }
    
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isRunning = false;
        try {
            if (renderThread != null) {
                renderThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (surfaceHolder.getSurface().isValid()) {
            startRendering();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;
        try {
            if (renderThread != null) {
                renderThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
