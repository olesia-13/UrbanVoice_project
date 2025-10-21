package com.golap.urbanvoice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class LocationAudioService extends Service{

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Тут буде логіка запуску відстеження GPS і аудіо.

        // Повертаємо START_STICKY, щоб система намагалася перезапустити сервіс,
        // якщо його було знищено через нестачу пам'яті.
        return START_STICKY;
    }

    // Цей метод повертає комунікаційний інтерфейс, якщо Activity хоче
    // "прив'язатися" до сервісу. Нам поки що не потрібно, повертаємо null.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Цей метод викликається, коли сервіс знищується.
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Тут буде логіка зупинки GPS та звільнення ресурсів аудіо.
    }

}
