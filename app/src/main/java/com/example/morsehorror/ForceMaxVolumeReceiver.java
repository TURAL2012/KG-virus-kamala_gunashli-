import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

public class ForceMaxVolumeReceiver extends BroadcastReceiver {
    private int lastVolume = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
            
            // Sadece Medya (Müzik/Oyun/Video) ses akışını kontrol ediyoruz
            int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            
            if (streamType == AudioManager.STREAM_MUSIC) {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager == null) return;

                int currentVolume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                // İlk çalıştırmada mevcut ses seviyesini kaydet
                if (lastVolume == -1) {
                    lastVolume = currentVolume;
                    return;
                }

                // Eğer kullanıcı sesi kıstıysa veya ses maksimumdan daha düşükse
                if (currentVolume < lastVolume || currentVolume < maxVolume) {
                    
                    // Sesi tamamen arka planda (0 bayrağı ile) maksimuma eşitle
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, 
                        maxVolume, 
                        0 // 0 kullanarak sistem arayüzünü (UI), kutuları ve bip seslerini tamamen gizleriz
                    );
                    
                    // Bir sonraki kontrol için hafızayı güncelle
                    lastVolume = maxVolume;
                } else {
                    lastVolume = currentVolume;
                }
            }
        }
    }
}
