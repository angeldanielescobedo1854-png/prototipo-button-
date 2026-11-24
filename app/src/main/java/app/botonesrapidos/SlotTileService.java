package app.botonesrapidos;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/** Lógica común de los 5 botones del Centro de Control. */
public abstract class SlotTileService extends TileService {

    protected abstract int slot();

    private CameraManager camera;
    private CameraManager.TorchCallback torchCallback;
    private boolean torchOn = false;

    @Override
    public void onTileAdded() {
        refresh();
    }

    @Override
    public void onStartListening() {
        refresh();
        if (isTorch()) registerTorch();
    }

    @Override
    public void onStopListening() {
        unregisterTorch();
    }

    @Override
    public void onClick() {
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                doAction();
            }
        };
        if (isLocked()) {
            unlockAndRun(action);
        } else {
            action.run();
        }
    }

    private boolean isTorch() {
        return Cfg.action(this, slot()) == Cfg.A_TORCH;
    }

    private void refresh() {
        Tile t = getQsTile();
        if (t == null) return;
        t.setLabel(Cfg.label(this, slot()));
        t.setIcon(Cfg.tileIcon(this, slot()));
        t.setState(isTorch() && torchOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.updateTile();
    }

    private void doAction() {
        switch (Cfg.action(this, slot())) {
            case Cfg.A_TORCH:
                toggleTorch();
                return;
            case Cfg.A_SMS:
                sendSms();
                return;
            case Cfg.A_TELEGRAM:
                sendTelegram();
                return;
            default:
                break;
        }
        Intent i = Cfg.buildIntent(this, slot());
        if (i == null) {
            openConfig();
            return;
        }
        launch(i);
    }

    /** Aún sin configurar: abre la app para configurarlo. */
    private void openConfig() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launch(i);
    }

    @SuppressWarnings("deprecation")
    private void launch(Intent i) {
        if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent pi = PendingIntent.getActivity(
                    this, slot(), i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            startActivityAndCollapse(pi);
        } else {
            startActivityAndCollapse(i);
        }
    }

    private void toast(String m) {
        Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show();
    }

    // ---------- Enviar SMS sin abrir nada ----------

    @SuppressWarnings("deprecation")
    private void sendSms() {
        String number = Cfg.value(this, slot()).trim();
        String text = Cfg.msg(this, slot()).trim();
        if (number.isEmpty() || text.isEmpty()) {
            openConfig();
            return;
        }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            toast("Falta el permiso de SMS. Abre la app y toca Guardar en este botón.");
            openConfig();
            return;
        }
        try {
            SmsManager sm = Build.VERSION.SDK_INT >= 31
                    ? getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            ArrayList<String> parts = sm.divideMessage(text);
            sm.sendMultipartTextMessage(number, null, parts, null, null);
            toast("SMS enviado a " + number);
        } catch (Exception e) {
            toast("No se pudo enviar el SMS");
        }
    }

    // ---------- Enviar mensaje por Telegram (bot) ----------

    private void sendTelegram() {
        final String chat = Cfg.value(this, slot()).trim();
        final String token = Cfg.extra(this, slot()).trim();
        final String text = Cfg.msg(this, slot()).trim();
        if (chat.isEmpty() || token.isEmpty() || text.isEmpty()) {
            openConfig();
            return;
        }
        final Context app = getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = false;
                HttpURLConnection c = null;
                try {
                    URL u = new URL("https://api.telegram.org/bot" + token + "/sendMessage");
                    c = (HttpURLConnection) u.openConnection();
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    String body = "chat_id=" + URLEncoder.encode(chat, "UTF-8")
                            + "&text=" + URLEncoder.encode(text, "UTF-8");
                    OutputStream os = c.getOutputStream();
                    os.write(body.getBytes("UTF-8"));
                    os.close();
                    ok = c.getResponseCode() == 200;
                } catch (Exception ignored) {
                } finally {
                    if (c != null) c.disconnect();
                }
                final boolean result = ok;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(app, result
                                        ? "Mensaje enviado por Telegram"
                                        : "No se pudo enviar. Revisa el token, el chat y tu internet.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    // ---------- Linterna ----------

    private CameraManager cam() {
        if (camera == null) camera = (CameraManager) getSystemService(CAMERA_SERVICE);
        return camera;
    }

    private String torchId() {
        try {
            CameraManager cm = cam();
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics ch = cm.getCameraCharacteristics(id);
                Boolean flash = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = ch.get(CameraCharacteristics.LENS_FACING);
                if (flash != null && flash && facing != null
                        && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (CameraAccessException ignored) {
        }
        return null;
    }

    private void toggleTorch() {
        String id = torchId();
        if (id == null) {
            toast("Este teléfono no tiene flash");
            return;
        }
        try {
            cam().setTorchMode(id, !torchOn);
        } catch (CameraAccessException e) {
            toast("No se pudo usar la linterna");
        }
    }

    private void registerTorch() {
        if (torchCallback != null) return;
        torchCallback = new CameraManager.TorchCallback() {
            @Override
            public void onTorchModeChanged(String cameraId, boolean enabled) {
                torchOn = enabled;
                refresh();
            }
        };
        cam().registerTorchCallback(torchCallback, null);
    }

    private void unregisterTorch() {
        if (torchCallback != null) {
            cam().unregisterTorchCallback(torchCallback);
            torchCallback = null;
        }
    }
}
