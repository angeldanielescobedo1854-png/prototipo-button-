package app.botonesrapidos;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Cuadro flotante del widget: escribir el mensaje y enviarlo ahora o programarlo. */
public class ComposeActivity extends Activity {
    static final String EXTRA_ID = "id";
    static final String EXTRA_MODE = "mode";

    private static final long[] UNIT_MS = {60_000L, 3_600_000L, 86_400_000L};
    private static final String[] UNIT_NAMES = {"minutos", "horas", "días"};

    private int wid;
    private Dest dest;

    private EditText msg;
    private RadioButton rbNow;
    private RadioButton rbIn;
    private RadioButton rbAt;
    private LinearLayout rowIn;
    private LinearLayout rowAt;
    private LinearLayout exactBox;
    private EditText inNum;
    private Spinner inUnit;
    private Button dateBtn;
    private Button timeBtn;
    private Button go;
    private final Calendar at = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        wid = getIntent().getIntExtra(EXTRA_ID, -1);
        dest = WidgetCfg.get(this, wid);
        if (dest == null) {
            Toast.makeText(this, "Configura primero el widget", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        boolean schedule = "schedule".equals(getIntent().getStringExtra(EXTRA_MODE));

        at.setTimeInMillis(System.currentTimeMillis() + 3_600_000L);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(8));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Para: " + dest.chatName() + " · " + dest.appName());
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF1B1F2A);
        root.addView(title);

        msg = new EditText(this);
        msg.setHint("Escribe tu mensaje…");
        msg.setMinLines(3);
        msg.setGravity(android.view.Gravity.TOP);
        msg.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(msg);

        root.addView(small(dest.sendsByItself()
                ? "Este mensaje se envía solo."
                : "Se abrirá " + dest.appName() + " con el mensaje ya escrito y tú tocas Enviar."));

        // ¿Cuándo?
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        rbNow = radio("Ahora");
        rbIn = radio("Dentro de…");
        rbAt = radio("A una hora y día…");
        rg.addView(rbNow);
        rg.addView(rbIn);
        rg.addView(rbAt);
        root.addView(rg);

        rowIn = new LinearLayout(this);
        rowIn.setOrientation(LinearLayout.HORIZONTAL);
        inNum = new EditText(this);
        inNum.setInputType(InputType.TYPE_CLASS_NUMBER);
        inNum.setText("10");
        inNum.setMinEms(3);
        rowIn.addView(inNum);
        inUnit = new Spinner(this);
        inUnit.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, UNIT_NAMES));
        rowIn.addView(inUnit);
        root.addView(rowIn);

        rowAt = new LinearLayout(this);
        rowAt.setOrientation(LinearLayout.HORIZONTAL);
        dateBtn = new Button(this);
        timeBtn = new Button(this);
        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate();
            }
        });
        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickTime();
            }
        });
        rowAt.addView(dateBtn);
        rowAt.addView(timeBtn);
        root.addView(rowAt);
        refreshAtButtons();

        exactBox = new LinearLayout(this);
        exactBox.setOrientation(LinearLayout.VERTICAL);
        exactBox.addView(small("Para que llegue justo a la hora, activa “Alarmas y recordatorios” "
                + "para esta app. Si no, puede retrasarse unos minutos."));
        Button exactBtn = new Button(this);
        exactBtn.setText("Activar hora exacta");
        exactBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 31) {
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        Toast.makeText(ComposeActivity.this,
                                "Ajustes > Apps > Botones Rápidos > Alarmas y recordatorios",
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        exactBox.addView(exactBtn);
        root.addView(exactBox);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.END);
        Button cancel = new Button(this);
        cancel.setText("Cancelar");
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        row.addView(cancel);
        go = new Button(this);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        row.addView(go);
        root.addView(row);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateMode();
            }
        });
        rg.check(schedule ? rbIn.getId() : rbNow.getId());
        updateMode();

        setContentView(scroll);
        msg.requestFocus();
    }

    private RadioButton radio(String t) {
        RadioButton r = new RadioButton(this);
        r.setText(t);
        r.setId(View.generateViewId());
        return r;
    }

    private void updateMode() {
        rowIn.setVisibility(rbIn.isChecked() ? View.VISIBLE : View.GONE);
        rowAt.setVisibility(rbAt.isChecked() ? View.VISIBLE : View.GONE);
        boolean later = !rbNow.isChecked();
        exactBox.setVisibility(later && !Scheduler.canExact(this) ? View.VISIBLE : View.GONE);
        go.setText(later ? "Programar" : "Enviar");
    }

    private void refreshAtButtons() {
        dateBtn.setText(new SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(at.getTime()));
        timeBtn.setText(DateFormat.getTimeFormat(this).format(at.getTime()));
    }

    private void pickDate() {
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int y, int m, int d) {
                at.set(y, m, d);
                refreshAtButtons();
            }
        }, at.get(Calendar.YEAR), at.get(Calendar.MONTH), at.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int h, int min) {
                at.set(Calendar.HOUR_OF_DAY, h);
                at.set(Calendar.MINUTE, min);
                at.set(Calendar.SECOND, 0);
                refreshAtButtons();
            }
        }, at.get(Calendar.HOUR_OF_DAY), at.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this)).show();
    }

    private String pretty(long when) {
        return new SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(new Date(when));
    }

    private void submit() {
        String text = msg.getText().toString().trim();
        if (text.isEmpty()) {
            toast("Escribe el mensaje");
            return;
        }

        if (rbNow.isChecked()) {
            Sender.sendNow(this, dest, text);
            WidgetCfg.setLast(this, wid, "Tú: " + text);
            ChatWidget.update(this, wid);
            finish();
            return;
        }

        long now = System.currentTimeMillis();
        long when;
        if (rbIn.isChecked()) {
            long n;
            try {
                n = Long.parseLong(inNum.getText().toString().trim());
            } catch (NumberFormatException e) {
                n = 0;
            }
            if (n <= 0 || n > 9999) {
                toast("Escribe un número válido");
                return;
            }
            when = now + n * UNIT_MS[Math.max(0, inUnit.getSelectedItemPosition())];
        } else {
            when = at.getTimeInMillis();
            if (when <= now + 5000) {
                toast("Esa hora ya pasó. Elige una futura.");
                return;
            }
        }

        ScheduleStore.Pending p = ScheduleStore.add(this, when, text, dest);
        Scheduler.set(this, p);
        WidgetCfg.setLast(this, wid, "⏰ " + pretty(when) + ": " + text);
        ChatWidget.update(this, wid);

        String extra = Scheduler.canExact(this) ? "" : " (hora aproximada)";
        String how = dest.sendsByItself() ? "Se enviará solo" : "Te avisaré para enviarlo";
        Toast.makeText(this, how + " · " + pretty(when) + extra, Toast.LENGTH_LONG).show();
        finish();
    }

    private TextView small(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(12);
        v.setTextColor(0xFF5B6475);
        v.setPadding(0, dp(8), 0, dp(4));
        return v;
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
