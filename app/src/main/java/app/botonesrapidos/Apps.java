package app.botonesrapidos;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Apps conocidas de IA y de mensajería, y cuáles están instaladas. */
final class Apps {

    /** {paquete, nombre} */
    static final String[][] AI = {
            {"com.anthropic.claude", "Claude"},
            {"com.openai.chatgpt", "ChatGPT"},
            {"com.google.android.apps.bard", "Gemini"},
            {"com.microsoft.copilot", "Copilot"},
            {"ai.perplexity.app.android", "Perplexity"},
            {"com.deepseek.chat", "DeepSeek"},
            {"ai.x.grok", "Grok"}
    };

    static final String[][] CHAT = {
            {"com.whatsapp", "WhatsApp"},
            {"com.whatsapp.w4b", "WhatsApp Business"},
            {"org.telegram.messenger", "Telegram"},
            {"org.telegram.messenger.web", "Telegram"},
            {"org.thunderdog.challegram", "Telegram X"},
            {"org.thoughtcrime.securesms", "Signal"},
            {"com.facebook.orca", "Messenger"},
            {"com.discord", "Discord"}
    };

    private Apps() {}

    /** De la lista conocida, solo las que están instaladas: {paquete, nombre real}. */
    static List<String[]> installed(Context c, String[][] known) {
        PackageManager pm = c.getPackageManager();
        List<String[]> out = new ArrayList<>();
        for (String[] k : known) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(k[0], 0);
                out.add(new String[]{k[0], pm.getApplicationLabel(ai).toString()});
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return out;
    }

    /** Todas las apps con icono en el lanzador, ordenadas por nombre. */
    static List<String[]> launchable(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        List<String[]> pairs = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (seen.contains(pkg)) continue;
            seen.add(pkg);
            pairs.add(new String[]{pkg, ri.loadLabel(pm).toString()});
        }
        Collections.sort(pairs, new Comparator<String[]>() {
            @Override
            public int compare(String[] x, String[] y) {
                return x[1].compareToIgnoreCase(y[1]);
            }
        });
        return pairs;
    }

    static boolean isInstalled(Context c, String pkg) {
        try {
            c.getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    static boolean isWhatsApp(String pkg) {
        return pkg.startsWith("com.whatsapp");
    }

    static boolean isTelegram(String pkg) {
        return pkg.startsWith("org.telegram") || pkg.equals("org.thunderdog.challegram");
    }
}
