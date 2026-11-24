package app.botonesrapidos;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Utilidades para crear los iconos personalizados de los botones. */
final class IconUtil {
    static final int SIZE = 128;

    private IconUtil() {}

    /** Lee una imagen de la galería, la recorta al centro (cuadrada) y la reduce. */
    static Bitmap squareFromUri(Context c, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int sample = 1;
        int minSide = Math.min(bounds.outWidth, bounds.outHeight);
        while (minSide / (sample * 2) >= SIZE) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap src;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            src = BitmapFactory.decodeStream(in, null, opts);
        }
        if (src == null) throw new IOException("No se pudo leer la imagen");

        int side = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - side) / 2;
        int y = (src.getHeight() - side) / 2;
        Bitmap square = Bitmap.createBitmap(src, x, y, side, side);
        return Bitmap.createScaledBitmap(square, SIZE, SIZE, true);
    }

    /** Dibuja el icono de una app instalada. Prueba cada paquete hasta encontrar uno. */
    static Bitmap appIcon(Context c, String... packages) {
        PackageManager pm = c.getPackageManager();
        for (String pkg : packages) {
            try {
                Drawable d = pm.getApplicationIcon(pkg);
                Bitmap b = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(b);
                d.setBounds(0, 0, SIZE, SIZE);
                d.draw(cv);
                return b;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Convierte la imagen a blanco con transparencia según la luminosidad.
     * El panel de Android pinta los iconos de un solo color, así que así se
     * reconoce la imagen en vez de verse un cuadro sólido.
     */
    static Bitmap toMono(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = (p >>> 24) & 0xFF;
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            int lum = (int) (0.299f * r + 0.587f * g + 0.114f * b);
            int v = Math.min(255, (int) (lum * 1.15f));
            int na = a * v / 255;
            px[i] = (na << 24) | 0x00FFFFFF;
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    static void savePng(File f, Bitmap b) throws IOException {
        try (FileOutputStream out = new FileOutputStream(f)) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
