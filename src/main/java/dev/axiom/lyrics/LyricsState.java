package dev.axiom.lyrics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LyricsState {
    public record LrcLine(long ms, String text) {}

    /** front = aktive/aktuelle Zeile, past = inaktiv, glow = aura, active = karaoke-Wort. */
    public record Theme(String name, int front, int past, int glow, int active) {
        public int depth(int i) { return scale(glow, 0.85f - i * 0.15f); }
    }

    public static final Theme[] THEMES = {
        new Theme("Lila",   0xF3ECFF, 0xB79CFF, 0xA06CFF, 0xFFFFFF),
        new Theme("Weiß",   0xFFFFFF, 0xC9CCE0, 0x8C96D8, 0xFFE566),
        new Theme("Grün",   0xE3FFEE, 0x6BE39A, 0x1DB954, 0xFFFFFF),
        new Theme("Rosa",   0xFFEAF5, 0xFF9FCF, 0xFF5CA8, 0xFFFFFF),
        new Theme("Blau",   0xE8F4FF, 0x8FC6FF, 0x4C9CFF, 0xFFFFFF),
        new Theme("Orange", 0xFFF0E0, 0xFFB070, 0xFF7A2F, 0xFFFFFF),
        new Theme("Cyan",   0xE6FFFF, 0x6EE7E7, 0x22D3EE, 0xFFFFFF),
    };

    // ---- Einstellungen ----
    public static volatile boolean enabled = true;
    public static volatile int mode = 1;            // 0 = 3D, 1 = Karaoke unten (TikTok-Style default)
    public static volatile float size = 1.15f;
    public static volatile int offsetMs = 0;
    public static volatile int themeIdx = 0;
    public static volatile int source = defaultSource();
    public static volatile boolean customColor = false;
    public static volatile int customFront = 0xF3ECFF;
    public static volatile int customPast = 0xB79CFF;
    public static volatile int customGlow = 0xA06CFF;
    public static volatile int customActive = 0xFFFFFF;
    public static volatile float glowStrength = 1.0f; // 0.3 .. 2.0
    public static volatile boolean showBg = true;

    // ---- Laufzeit ----
    public static volatile String trackId = "";
    public static volatile String title = "", artist = "";
    public static volatile String status = "Nicht verbunden. Taste K -> Client ID eintragen.";
    public static volatile boolean playing;
    public static volatile long progressMs, fetchedAt, durationMs;
    public static volatile List<LrcLine> lines = List.of();

    private static int defaultSource() {
        return System.getProperty("os.name", "").toLowerCase().contains("win") ? 0 : 1;
    }

    public static Theme theme() {
        if (customColor) {
            return new Theme("Custom", customFront, customPast, customGlow, customActive);
        }
        return THEMES[Math.floorMod(themeIdx, THEMES.length)];
    }

    public static long now() {
        return playing ? progressMs + (System.currentTimeMillis() - fetchedAt) : progressMs;
    }

    public static int indexAt(List<LrcLine> l, long t) {
        int i = -1;
        for (int k = 0; k < l.size(); k++) {
            if (l.get(k).ms() <= t) i = k; else break;
        }
        return i;
    }

    public static int scale(int rgb, float f) {
        int r = Math.min(255, Math.max(0, (int) (((rgb >> 16) & 255) * f)));
        int g = Math.min(255, Math.max(0, (int) (((rgb >> 8) & 255) * f)));
        int b = Math.min(255, Math.max(0, (int) ((rgb & 255) * f)));
        return (r << 16) | (g << 8) | b;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("spotifylyrics-settings.json");
    }

    public static void loadSettings() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            if (o.has("enabled")) enabled = o.get("enabled").getAsBoolean();
            if (o.has("mode")) mode = o.get("mode").getAsInt();
            if (o.has("size")) size = Math.max(0.4f, Math.min(3f, o.get("size").getAsFloat()));
            if (o.has("offsetMs")) offsetMs = Math.max(-1500, Math.min(1500, o.get("offsetMs").getAsInt()));
            if (o.has("themeIdx")) themeIdx = o.get("themeIdx").getAsInt();
            if (o.has("source")) source = o.get("source").getAsInt() == 1 ? 1 : 0;
            if (o.has("customColor")) customColor = o.get("customColor").getAsBoolean();
            if (o.has("customFront")) customFront = o.get("customFront").getAsInt() & 0xFFFFFF;
            if (o.has("customPast")) customPast = o.get("customPast").getAsInt() & 0xFFFFFF;
            if (o.has("customGlow")) customGlow = o.get("customGlow").getAsInt() & 0xFFFFFF;
            if (o.has("customActive")) customActive = o.get("customActive").getAsInt() & 0xFFFFFF;
            if (o.has("glowStrength")) glowStrength = Math.max(0.3f, Math.min(2f, o.get("glowStrength").getAsFloat()));
            if (o.has("showBg")) showBg = o.get("showBg").getAsBoolean();
        } catch (Exception ignored) {}
    }

    public static void saveSettings() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", enabled);
            o.addProperty("mode", mode);
            o.addProperty("size", size);
            o.addProperty("offsetMs", offsetMs);
            o.addProperty("themeIdx", themeIdx);
            o.addProperty("source", source);
            o.addProperty("customColor", customColor);
            o.addProperty("customFront", customFront);
            o.addProperty("customPast", customPast);
            o.addProperty("customGlow", customGlow);
            o.addProperty("customActive", customActive);
            o.addProperty("glowStrength", glowStrength);
            o.addProperty("showBg", showBg);
            Files.writeString(file(), o.toString());
        } catch (Exception ignored) {}
    }

    private LyricsState() {}
}
