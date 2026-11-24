package app.botonesrapidos;

import android.app.Activity;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private static final Class<?>[] TILES = {
            Tile1.class, Tile2.class, Tile3.class, Tile4.class, Tile5.class
    };

    private final List<String> appLabels = new ArrayList<>();
    private final List<String> appPkgs = new ArrayList<>();

    private final EditText[] nameEt = new EditText[Cfg.SLOTS];
    private final Spinner[] iconSp = new Spinner[Cfg.SLOTS];
    private final Spinner[] actionSp = new Spinner[Cfg.SLOTS];
    private final Spinner[] appSp = new Spinner[Cfg.SLOTS];
    private final EditText[] valueEt = new EditText[Cfg.SLOTS];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadApps();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(40), dp(16), dp(32));
        scroll.addView(root);

        TextView title = text("Botones Rápidos", 26, true);
        root.addView(title);

        TextView help = text(
                "Configura hasta 5 botones y guárdalos. Luego abre el Centro de Control, "
                        + "toca el lápiz (editar) y arrastra “Botón 1…5” al panel. "
                        + "En Android 13+ también puedes usar “Añadir al panel”.", 14, false);
        help.setPadding(0, dp(8), 0, dp(8));
        root.addView(help);

        for (int s = 0; s < Cfg.SLOTS; s++) {
            root.addView(buildCard(s));
        }

        setContentView(scroll);
    }

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

        card.addView(small("Icono"));
        iconSp[s] = new Spinner(this);
        iconSp[s].setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Cfg.ICON_NAMES));
        iconSp[s].setSelection(Cfg.iconIndex(this, s));
        card.addView(iconSp[s]);

        card.addView(small("Al tocarlo…"));
        actionSp[s] = new Spinner(this);
        actionSp[s].setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Cfg.ACTION_NAMES));
        int action = Cfg.action(this, s);
        actionSp[s].setSelection(action);
        card.addView(actionSp[s]);

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

    private void updateValueUi(int s, int action) {
        appSp[s].setVisibility(action == Cfg.A_APP ? View.VISIBLE : View.GONE);
        boolean needsText = action == Cfg.A_URL || action == Cfg.A_DIAL || action == Cfg.A_WHATSAPP;
        valueEt[s].setVisibility(needsText ? View.VISIBLE : View.GONE);
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
                break;
            default:
                break;
        }
    }

    /** Guarda el botón s. Devuelve false si falta algún dato. */
    private boolean save(int s) {
        int action = actionSp[s].getSelectedItemPosition();
        String name = nameEt[s].getText().toString().trim();
        if (name.isEmpty()) name = "Botón " + (s + 1);

        String value = "";
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
        }

        Cfg.save(this, s, name, iconSp[s].getSelectedItemPosition(), action, value);
        TileService.requestListeningState(this, new ComponentName(this, TILES[s]));
        return true;
    }

    private void addToPanel(int s) {
        if (!save(s)) return;
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Api33.add(this, new ComponentName(this, TILES[s]),
                        Cfg.label(this, s), Cfg.iconRes(this, s));
                return;
            } catch (Exception ignored) {
                // cae al método manual
            }
        }
        toast("Guardado. Agrégalo con el lápiz del Centro de Control");
    }

    /** Pedir al sistema que agregue el botón (Android 13+). */
    private static final class Api33 {
        static void add(final Activity a, ComponentName cn, String label, int iconRes) {
            StatusBarManager sbm = a.getSystemService(StatusBarManager.class);
            sbm.requestAddTileService(cn, label, Icon.createWithResource(a, iconRes),
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
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
