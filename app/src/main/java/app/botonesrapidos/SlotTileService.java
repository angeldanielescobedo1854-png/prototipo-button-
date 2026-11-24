package app.botonesrapidos;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

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
        t.setIcon(Icon.createWithResource(this, Cfg.iconRes(this, slot())));
        t.setState(isTorch() && torchOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.updateTile();
    }

    private void doAction() {
        if (isTorch()) {
            toggleTorch();
            return;
        }
        Intent i = Cfg.buildIntent(this, slot());
        if (i == null) {
            // Aún sin configurar: abre la app para configurarlo.
            i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
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
            Toast.makeText(this, "Este teléfono no tiene flash", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            cam().setTorchMode(id, !torchOn);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "No se pudo usar la linterna", Toast.LENGTH_SHORT).show();
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
