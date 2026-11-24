package app.botonesrapidos;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
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
import java.util.List;

/** Elegir a qué IA o chat apunta un widget (según las apps instaladas). */
public class WidgetConfigActivity extends Activity {

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Spinner kindSp;
    private Spinner appSp;
    private TextView appNote;
    private EditText labelEt;
    private EditText toEt;
    private EditText tokenEt;
    private TextView help;

    private final List<String[]> apps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setResult(RESULT_CANCELED);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        Dest cur = WidgetCfg.get(this, widgetId);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(24));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Configurar widget de chat");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF1B1F2A);
        root.addView(title);

        root.addView(small("¿A dónde va este widget?"));
        kindSp = new Spinner(this);
        kindSp.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Dest.KIND_NAMES));
        root.addView(kindSp);

        appSp = new Spinner(this);
        root.addView(appSp);
        appNote = small("");
        root.addView(appNote);

        labelEt = new EditText(this);
        labelEt.setSingleLine(true);
        root.addView(labelEt);

        toEt = new EditText(this);
        toEt.setSingleLine(true);
        root.addView(toEt);

        tokenEt = new EditText(this);
        tokenEt.setSingleLine(true);
        root.addView(tokenEt);

        help = small("");
        root.addView(help);

        Button ok = new Button(this);
        ok.setText("Listo");
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        root.addView(ok, lp);

        int startKind = Dest.K_AI;
        if (cur != null) {
            startKind = cur.kind;
            labelEt.setText(cur.label);
            toEt.setText(cur.to);
            tokenEt.setText(cur.token);
        }
        kindSp.setSelection(startKind);
        loadApps(startKind, cur != null ? cur.pkg : "");
        updateUi(startKind);

        kindSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadApps(position, "");
                updateUi(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setContentView(scroll);
    }

    private void loadApps(int kind, String selectPkg) {
        apps.clear();
        if (kind == Dest.K_AI) {
            apps.addAll(Apps.installed(this, Apps.AI));
        } else if (kind == Dest.K_CHAT) {
            apps.addAll(Apps.installed(this, Apps.CHAT));
        } else if (kind == Dest.K_OTHER) {
            apps.addAll(Apps.launchable(this));
        }
        List<String> names = new ArrayList<>();
        int sel = 0;
        for (int i = 0; i < apps.size(); i++) {
            names.add(apps.get(i)[1]);
            if (apps.get(i)[0].equals(selectPkg)) sel = i;
        }
        appSp.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));
        if (!names.isEmpty()) appSp.setSelection(sel);
    }

    private void updateUi(int kind) {
        boolean usesApp = kind == Dest.K_AI || kind == Dest.K_CHAT || kind == Dest.K_OTHER;
        appSp.setVisibility(usesApp ? View.VISIBLE : View.GONE);

        String note = "";
        if (usesApp && apps.isEmpty()) {
            note = kind == Dest.K_AI
                    ? "No encontré apps de IA conocidas instaladas. Prueba con “Otra app instalada”."
                    : "No encontré apps de mensajería conocidas. Prueba con “Otra app instalada”.";
        } else if (kind == Dest.K_AI) {
            note = "Se muestran solo las IAs que tienes instaladas.";
        } else if (kind == Dest.K_CHAT) {
            note = "Se muestran solo las apps de mensajería que tienes instaladas.";
        }
        appNote.setText(note);
        appNote.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);

        boolean showTo = kind == Dest.K_CHAT || kind == Dest.K_SMS || kind == Dest.K_BOT;
        toEt.setVisibility(showTo ? View.VISIBLE : View.GONE);
        tokenEt.setVisibility(kind == Dest.K_BOT ? View.VISIBLE : View.GONE);

        String h = "";
        switch (kind) {
            case Dest.K_AI:
                labelEt.setHint("Nombre del chat o tema (ej: Ideas de la página)");
                h = "El widget no puede leer tus chats dentro de la app: muestra el nombre que "
                        + "pongas aquí y lo último que enviaste desde el widget.";
                break;
            case Dest.K_CHAT:
                labelEt.setHint("Nombre del contacto o grupo");
                toEt.setHint("Número con lada (WhatsApp) o @usuario (Telegram)");
                toEt.setInputType(InputType.TYPE_CLASS_TEXT);
                h = "Si dejas vacío el número o usuario, la app te pedirá elegir a quién enviar.";
                break;
            case Dest.K_SMS:
                labelEt.setHint("Nombre del contacto");
                toEt.setHint("Número de teléfono");
                toEt.setInputType(InputType.TYPE_CLASS_PHONE);
                h = "Los SMS se envían solos. Puede tener costo según tu plan.";
                break;
            case Dest.K_BOT:
                labelEt.setHint("Nombre del chat");
                toEt.setHint("ID del chat de Telegram");
                toEt.setInputType(InputType.TYPE_CLASS_TEXT);
                tokenEt.setHint("Token del bot (de @BotFather)");
                h = "El bot solo puede escribirle a quien le haya dado /start o a grupos donde esté.";
                break;
            default:
                labelEt.setHint("Nombre del chat o tema");
                h = "Se le enviará el texto con “Compartir”. Si esa app no lo acepta, "
                        + "el texto se copia para que lo pegues.";
                break;
        }
        help.setText(h);
        help.setVisibility(h.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void save() {
        int kind = kindSp.getSelectedItemPosition();
        Dest d = new Dest();
        d.kind = kind;
        d.label = labelEt.getText().toString().trim();
        d.to = toEt.getText().toString().trim();
        d.token = tokenEt.getText().toString().trim();

        boolean usesApp = kind == Dest.K_AI || kind == Dest.K_CHAT || kind == Dest.K_OTHER;
        if (usesApp) {
            int i = appSp.getSelectedItemPosition();
            if (i < 0 || i >= apps.size()) {
                toast("No hay una app para elegir");
                return;
            }
            d.pkg = apps.get(i)[0];
            d.app = apps.get(i)[1];
        }
        if (kind == Dest.K_SMS && d.to.isEmpty()) {
            toast("Escribe el número de teléfono");
            return;
        }
        if (kind == Dest.K_BOT && (d.to.isEmpty() || d.token.isEmpty())) {
            toast("Faltan el token del bot y el ID del chat");
            return;
        }

        WidgetCfg.put(this, widgetId, d);
        ChatWidget.update(this, widgetId);

        // Permisos que se usarán al enviar o programar
        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (kind == Dest.K_SMS
                && checkSelfPermission(Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            need.add(Manifest.permission.SEND_SMS);
        }
        if (need.isEmpty()) {
            finishOk();
        } else {
            requestPermissions(need.toArray(new String[0]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        finishOk();
    }

    private void finishOk() {
        Intent r = new Intent();
        r.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, r);
        finish();
    }

    private TextView small(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(12);
        v.setTextColor(0xFF5B6475);
        v.setPadding(0, dp(10), 0, 0);
        return v;
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
