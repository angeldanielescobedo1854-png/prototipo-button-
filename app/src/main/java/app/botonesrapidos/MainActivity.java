package app.botonesrapidos;

import android.Manifest;
import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final Class<?>[] TILES = {
            Tile1.class, Tile2.class, Tile3.class, Tile4.class, Tile5.class
    };

    private static final int REQ_PHOTO = 100;
    private static final int REQ_SMS = 200;

    private final List<String> appLabels = new ArrayList<>();
    private final List<String> appPkgs = new ArrayList<>();

    private final EditText[] nameEt = new EditText[Cfg.SLOTS];
    private final Spinner[] iconSrcSp = new Spinner[Cfg.SLOTS];
    private final Spinner[] iconSp = new Spinner[Cfg.SLOTS];
    private final LinearLayout[] photoRow = new LinearLayout[Cfg.SLOTS];
    private final TextView[] photoInfo = new TextView[Cfg.SLOTS];
    private final CheckBox[] monoCb = new CheckBox[Cfg.SLOTS];
    private final Spinner[] actionSp = new Spinner[Cfg.SLOTS];
    private final TextView[] noteTv = new TextView[Cfg.SLOTS];
    private final Spinner[] appSp = new Spinner[Cfg.SLOTS];
    private final EditText[] valueEt = new EditText[Cfg.SLOTS];
    private final EditText[] extraEt = new EditText[Cfg.SLOTS];
    private final EditText[] msgEt = new EditText[Cfg.SLOTS];
    /** Modos de icono disponibles ahora mismo en cada botón (según la acción elegida). */
    private final int[][] iconModeMap = new int[Cfg.SLOTS][];

    private LinearLayout schedBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadApps();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(40), dp(16), dp(32));
        scroll.addView(root);

        root.addView(text("Botones Rápidos", 26, true));

        TextView help = text(
                "BOTONES: configura hasta 5 y guárdalos. Luego abre el Centro de Control, "
                        + "toca el lápiz (editar) y arrastra “Botón 1…5” al panel.\n\n"
                        + "WIDGET DE CHAT: mantén presionada la pantalla de inicio, elige Widgets y "
                        + "busca “Botones Rápidos”. Ahí eliges tu IA o chat y puedes enviar o "
                        + "programar mensajes.", 14, false);
        help.setPadding(0, dp(8), 0, dp(8));
        root.addView(help);

        schedBox = new LinearLayout(this);
        schedBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(schedBox);

        for (int s = 0; s < Cfg.SLOTS; s++) {
            root.addView(buildCard(s));
        }

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshScheduled();
    }

    // ---------- Mensajes programados ----------

    private void refreshScheduled() {
        schedBox.removeAllViews();
        List<ScheduleStore.Pending> list = ScheduleStore.all(this);
        schedBox.addView(text("Mensajes programados", 17, true));
        if (list.isEmpty()) {
            schedBox.addView(small("No hay mensajes pendientes. Prográmalos desde el widget (botón del reloj)."));
            return;
        }
        SimpleDateFormat f = new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault());
        for (final ScheduleStore.Pending p : list) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(6), 0, 0);
            TextView t = text("⏰ " + f.format(new Date(p.when)) + "\n"
                    + p.dest.chatName() + " (" + p.dest.appName() + "): " + p.text, 13, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(t, lp);
            Button cancel = new Button(this);
            cancel.setText("Cancelar");
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Scheduler.cancel(MainActivity.this, p.id);
                    ScheduleStore.remove(MainActivity.this, p.id);
                    refreshScheduled();
                }
            });
            row.addView(cancel);
            schedBox.addView(row);
        }
    }

    // ---------- Tarjeta de cada botón ----------

    private View buildCard(final int s) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F3F9);
        bg.setCornerRadius(dp(16));
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        card.setLayoutParams(lp);

        card.addView(text("Botón " + (s + 1), 17, true));

        nameEt[s] = new EditText(this);
        nameEt[s].setHint("Nombre que se verá en el botón");
        nameEt[s].setSingleLine(true);
        nameEt[s].setFilters(new InputFilter[]{new InputFilter.LengthFilter(14)});
        nameEt[s].setText(Cfg.label(this, s));
        card.addView(nameEt[s]);

        // ----- Icono -----
        card.addView(small("Icono"));
        iconSrcSp[s] = new Spinner(this);
        card.addView(iconSrcSp[s]);

        iconSp[s] = new Spinner(this);
        iconSp[s].setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Cfg.ICON_NAMES));
        iconSp[s].setSelection(Cfg.iconIndex(this, s));
        card.addView(iconSp[s]);

        photoRow[s] = new LinearLayout(this);
        photoRow[s].setOrientation(LinearLayout.HORIZONTAL);
        Button pick = new Button(this);
        pick.setText("Elegir foto");
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPhoto(s);
            }
        });
        photoRow[s].addView(pick);
        photoInfo[s] = small(Cfg.photoFile(this, s).exists() ? "  Foto guardada ✔" : "  Sin foto");
        photoRow[s].addView(photoInfo[s]);
        card.addView(photoRow[s]);

        monoCb[s] = new CheckBox(this);
        monoCb[s].setText("Icono de un solo color (se ve mejor en el panel)");
        monoCb[s].setChecked(Cfg.mono(this, s));
        card.addView(monoCb[s]);

        iconSrcSp[s].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int[] map = iconModeMap[s];
                if (map != null && position >= 0 && position < map.length) {
                    updateIconUi(s, map[position]);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // ----- Acción -----
        card.addView(small("Al tocarlo…"));
        actionSp[s] = new Spinner(this);
        actionSp[s].setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Cfg.ACTION_NAMES));
        int action = Cfg.action(this, s);
        actionSp[s].setSelection(action);
        card.addView(actionSp[s]);

        noteTv[s] = small("");
        card.addView(noteTv[s]);

        appSp[s] = new Spinner(this);
        appSp[s].setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, appLabels));
        int idx = appPkgs.indexOf(Cfg.value(this, s));
        if (idx >= 0) appSp[s].setSelection(idx);
        card.addView(appSp[s]);

        valueEt[s] = new EditText(this);
        valueEt[s].setSingleLine(true);
        if (action != Cfg.A_APP) valueEt[s].setText(Cfg.value(this, s));
        card.addView(valueEt[s]);

        extraEt[s] = new EditText(this);
        extraEt[s].setSingleLine(true);
        extraEt[s].setText(Cfg.extra(this, s));
        card.addView(extraEt[s]);

        msgEt[s] = new EditText(this);
        msgEt[s].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        msgEt[s].setMinLines(2);
        msgEt[s].setText(Cfg.msg(this, s));
        card.addView(msgEt[s]);

        updateValueUi(s, action);
        actionSp[s].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateValueUi(s, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // ----- Botones -----
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);

        Button save = new Button(this);
        save.setText("Guardar");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (save(s)) toast("Guardado ✔");
            }
        });
        row.addView(save);

        Button add = new Button(this);
        add.setText("Añadir al panel");
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToPanel(s);
            }
        });
        row.addView(add);

        card.addView(row);
        return card;
    }

    private void updateIconUi(int s, int mode) {
        iconSp[s].setVisibility(mode == Cfg.I_LIST ? View.VISIBLE : View.GONE);
        photoRow[s].setVisibility(mode == Cfg.I_PHOTO ? View.VISIBLE : View.GONE);
        monoCb[s].setVisibility(mode == Cfg.I_LIST ? View.GONE : View.VISIBLE);
    }

    /** Reconstruye las opciones de icono: "icono de la app" solo existe para "Abrir una app". */
    private void refreshIconSources(int s, int action) {
        int desired;
        int[] old = iconModeMap[s];
        int pos = iconSrcSp[s].getSelectedItemPosition();
        if (old != null && pos >= 0 && pos < old.length) {
            desired = old[pos];
        } else {
            desired = Cfg.iconMode(this, s);
        }

        int[] modes = Cfg.hasAppIcon(action)
                ? new int[]{Cfg.I_LIST, Cfg.I_APP, Cfg.I_PHOTO}
                : new int[]{Cfg.I_LIST, Cfg.I_PHOTO};
        String[] names = new String[modes.length];
        int sel = 0;
        for (int i = 0; i < modes.length; i++) {
            names[i] = Cfg.ICON_SOURCES[modes[i]];
            if (modes[i] == desired) sel = i;
        }
        iconModeMap[s] = modes;
        iconSrcSp[s].setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));
        iconSrcSp[s].setSelection(sel);
        updateIconUi(s, modes[sel]);
    }

    private void updateValueUi(int s, int action) {
        refreshIconSources(s, action);
        appSp[s].setVisibility(action == Cfg.A_APP ? View.VISIBLE : View.GONE);

        boolean needsValue = action == Cfg.A_URL || action == Cfg.A_DIAL
                || action == Cfg.A_WHATSAPP || action == Cfg.A_SMS || action == Cfg.A_TELEGRAM;
        valueEt[s].setVisibility(needsValue ? View.VISIBLE : View.GONE);
        extraEt[s].setVisibility(action == Cfg.A_TELEGRAM ? View.VISIBLE : View.GONE);
        boolean needsMsg = action == Cfg.A_WHATSAPP || action == Cfg.A_SMS
                || action == Cfg.A_TELEGRAM;
        msgEt[s].setVisibility(needsMsg ? View.VISIBLE : View.GONE);

        String note = "";
        switch (action) {
            case Cfg.A_URL:
                valueEt[s].setHint("Ej: https://misitio.com");
                valueEt[s].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                break;
            case Cfg.A_DIAL:
                valueEt[s].setHint("Número, ej: 8661352460");
                valueEt[s].setInputType(InputType.TYPE_CLASS_PHONE);
                break;
            case Cfg.A_WHATSAPP:
                valueEt[s].setHint("Número con lada, ej: 528661352460");
                valueEt[s].setInputType(InputType.TYPE_CLASS_PHONE);
                msgEt[s].setHint("Mensaje ya escrito (opcional)");
                note = "WhatsApp no permite enviar mensajes sin abrir la app. Este botón abre "
                        + "el chat con el mensaje listo y solo tienes que tocar Enviar.";
                break;
            case Cfg.A_SMS:
                valueEt[s].setHint("Número, ej: 8661352460");
                valueEt[s].setInputType(InputType.TYPE_CLASS_PHONE);
                msgEt[s].setHint("Mensaje que se enviará");
                note = "Se envía al tocar el botón, sin abrir nada. Puede tener costo según tu "
                        + "plan y pedirá permiso de SMS al guardar.";
                break;
            case Cfg.A_TELEGRAM:
                valueEt[s].setHint("ID del chat, ej: 123456789");
                valueEt[s].setInputType(InputType.TYPE_CLASS_TEXT);
                extraEt[s].setHint("Token del bot (de @BotFather)");
                msgEt[s].setHint("Mensaje que se enviará");
                note = "Usa un bot tuyo de Telegram. El bot solo puede escribirle a quien le "
                        + "haya dado /start o a grupos donde esté, no a cualquier contacto.";
                break;
            default:
                break;
        }
        noteTv[s].setText(note);
        noteTv[s].setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ---------- Foto de la galería ----------

    private void pickPhoto(int s) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Elegir foto"), REQ_PHOTO + s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int s = requestCode - REQ_PHOTO;
        if (s < 0 || s >= Cfg.SLOTS) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Bitmap square = IconUtil.squareFromUri(this, data.getData());
            IconUtil.savePng(Cfg.photoFile(this, s), square);
            photoInfo[s].setText("  Foto lista ✔");
        } catch (Exception e) {
            toast("No se pudo leer esa foto");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SMS) {
            boolean ok = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            toast(ok ? "Permiso de SMS concedido ✔"
                    : "Sin permiso de SMS el botón no podrá enviar. Si no te deja activarlo: "
                    + "Ajustes > Apps > Botones Rápidos > ⋮ > Permitir ajustes restringidos.");
        }
    }

    // ---------- Guardar ----------

    /** Guarda el botón s. Devuelve false si falta algún dato. */
    private boolean save(int s) {
        int action = actionSp[s].getSelectedItemPosition();
        String name = nameEt[s].getText().toString().trim();
        if (name.isEmpty()) name = "Botón " + (s + 1);

        String value = "";
        String extra = extraEt[s].getText().toString().trim();
        String msg = msgEt[s].getText().toString().trim();

        if (action == Cfg.A_APP) {
            int i = appSp[s].getSelectedItemPosition();
            if (i >= 0 && i < appPkgs.size()) value = appPkgs.get(i);
            if (value.isEmpty()) {
                toast("Elige una app");
                return false;
            }
        } else if (action != Cfg.A_TORCH) {
            value = valueEt[s].getText().toString().trim();
            if (value.isEmpty()) {
                toast("Falta escribir el dato del botón");
                return false;
            }
            if ((action == Cfg.A_SMS || action == Cfg.A_TELEGRAM) && msg.isEmpty()) {
                toast("Escribe el mensaje que se enviará");
                return false;
            }
            if (action == Cfg.A_TELEGRAM && extra.isEmpty()) {
                toast("Falta el token del bot");
                return false;
            }
        }

        // Icono personalizado
        int[] modes = iconModeMap[s];
        int selPos = iconSrcSp[s].getSelectedItemPosition();
        int iconMode = (modes != null && selPos >= 0 && selPos < modes.length)
                ? modes[selPos] : Cfg.I_LIST;
        if (iconMode == Cfg.I_APP && !Cfg.hasAppIcon(action)) iconMode = Cfg.I_LIST;
        boolean mono = monoCb[s].isChecked();
        if (iconMode != Cfg.I_LIST) {
            Bitmap bmp = null;
            if (iconMode == Cfg.I_APP) {
                bmp = IconUtil.appIcon(this, value);
                if (bmp == null) {
                    toast("No encontré esa app instalada en el teléfono");
                    return false;
                }
            } else {
                File f = Cfg.photoFile(this, s);
                if (f.exists()) bmp = BitmapFactory.decodeFile(f.getPath());
                if (bmp == null) {
                    toast("Elige una foto primero");
                    return false;
                }
            }
            try {
                if (mono) bmp = IconUtil.toMono(bmp);
                IconUtil.savePng(Cfg.iconFile(this, s), bmp);
            } catch (Exception e) {
                toast("No se pudo guardar el icono");
                return false;
            }
        }

        Cfg.save(this, s, name, iconSp[s].getSelectedItemPosition(), iconMode, mono,
                action, value, extra, msg);

        if (action == Cfg.A_SMS && checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQ_SMS);
        }

        TileService.requestListeningState(this, new ComponentName(this, TILES[s]));
        return true;
    }

    private void addToPanel(int s) {
        if (!save(s)) return;
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Api33.add(this, new ComponentName(this, TILES[s]),
                        Cfg.label(this, s), Cfg.tileIcon(this, s));
                return;
            } catch (Exception ignored) {
                // cae al método manual
            }
        }
        toast("Guardado. Agrégalo con el lápiz del Centro de Control");
    }

    /** Pedir al sistema que agregue el botón (Android 13+). */
    private static final class Api33 {
        static void add(final Activity a, ComponentName cn, String label, Icon icon) {
            StatusBarManager sbm = a.getSystemService(StatusBarManager.class);
            sbm.requestAddTileService(cn, label, icon,
                    a.getMainExecutor(), new java.util.function.Consumer<Integer>() {
                        @Override
                        public void accept(Integer r) {
                            // 2 = agregado, 1 = ya estaba agregado
                            String msg = r == 2 ? "Botón añadido al panel ✔"
                                    : r == 1 ? "Ya está en el panel"
                                    : "No se añadió. Agrégalo con el lápiz del Centro de Control";
                            Toast.makeText(a, msg, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        final List<String[]> pairs = new ArrayList<>();
        for (ResolveInfo ri : list) {
            pairs.add(new String[]{ri.loadLabel(pm).toString(), ri.activityInfo.packageName});
        }
        Collections.sort(pairs, new Comparator<String[]>() {
            @Override
            public int compare(String[] x, String[] y) {
                return x[0].compareToIgnoreCase(y[0]);
            }
        });
        for (String[] p : pairs) {
            if (appPkgs.contains(p[1])) continue;
            appLabels.add(p[0]);
            appPkgs.add(p[1]);
        }
    }

    // ---------- helpers de UI ----------

    private TextView text(String t, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(0xFF1B1F2A);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private TextView small(String t) {
        TextView v = text(t, 12, false);
        v.setTextColor(0xFF5B6475);
        v.setPadding(0, dp(8), 0, 0);
        return v;
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
