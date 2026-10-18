package com.nyxora.client.ui;

import com.nyxora.client.animation.AnimationClock;
import com.nyxora.client.animation.Easing;
import com.nyxora.client.animation.Tween;
import com.nyxora.client.audio.UiSounds;
import com.nyxora.client.audio.UiSounds.Cue;
import com.nyxora.client.config.ConfigManager;
import com.nyxora.client.profiles.ProfileManager;
import com.nyxora.client.settings.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Main NYXORA window. One reusable Row abstraction renders every control, so styling is defined once.
 * Animations are time-based (AnimationClock + Tween). API NOTE: Click / KeyInput based signatures are
 * the 1.21.9+ forms and are UNVERIFIED against 1.21.11 sources in this environment.
 */
public final class NyxoraScreen extends Screen {
    private static final int SIDEBAR_W = 112, ROW_H = 28, TOP_BAR = 46;
    private static final String[] PAGE_NAMES = {"Sky", "Particles", "Combat Effects", "Motion", "Interface", "Profiles"};
    private static final Category[] PAGE_CATS = {Category.SKY, Category.PARTICLES, Category.COMBAT, Category.MOTION, Category.INTERFACE};
    private static final int PROFILES_PAGE = 5;
    private static int lastPage = 0; // remembered while the game runs

    private final NyxoraSettings settings;
    private final ConfigManager config;
    private final ProfileManager profiles;
    private final UiSounds sounds;

    private final AnimationClock clock = new AnimationClock();
    private final Tween open = new Tween(0, 0.18, Easing.EASE_OUT_CUBIC);
    private final Tween pageFade = new Tween(1, 0.22, Easing.EASE_OUT_CUBIC);
    private final Tween scroll = new Tween(0, 0.12, Easing.EASE_OUT_CUBIC);
    private final Map<String, Tween> hover = new HashMap<>();
    private final Map<String, Tween> toggles = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private final Deque<Toast> toasts = new ArrayDeque<>();

    private int page = lastPage;
    private double scrollTarget;
    private boolean closing;
    private Row dragging;
    private int wx, wy, ww, wh, cx, cy, cw, ch; // layout cache

    public NyxoraScreen(NyxoraSettings s, ConfigManager c, ProfileManager p, UiSounds u) {
        super(Text.literal("NYXORA"));
        this.settings = s; this.config = c; this.profiles = p; this.sounds = u;
        open.setTarget(1);
        rebuildRows();
        sounds.play(Cue.OPEN);
    }

    @Override public boolean shouldPause() { return false; }

    // ---------------------------------------------------------------- layout
    private void layout() {
        float sc = settings.uiScale.get();
        ww = Math.min(width - 16, Math.max(320, Math.round(540 * sc)));
        wh = Math.min(height - 16, Math.max(220, Math.round(330 * sc)));
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        cx = wx + SIDEBAR_W + 14; cy = wy + TOP_BAR; cw = ww - SIDEBAR_W - 28; ch = wh - TOP_BAR - 12;
    }

    private double maxScroll() { return Math.max(0, rows.size() * ROW_H - ch); }

    // ---------------------------------------------------------------- rows
    private void rebuildRows() {
        rows.clear();
        if (page == PROFILES_PAGE) {
            for (String name : profiles.names()) rows.add(new ButtonRow(name, profiles.active().equals(name) ? "ACTIVE" : "Load",
                    "Switch to this profile" + (profiles.isBuiltIn(name) ? " (built in)" : ""), () -> toast(profiles.load(name))));
            rows.add(new ButtonRow("Save current as new profile", "Save", "Stores all current settings under an auto-numbered name.", () -> {
                int n = 1; List<String> existing = profiles.names();
                while (existing.contains("Custom " + n)) n++;
                toast(profiles.saveCurrent("Custom " + n));
            }));
            rows.add(new ButtonRow("Delete active profile", "Delete", "Deletes the active custom profile (built-ins are protected).", () -> toast(profiles.delete(profiles.active()))));
            rows.add(new ButtonRow("Export settings", "Copy", "Copies all settings to the clipboard as text.", () -> {
                MinecraftClient.getInstance().keyboard.setClipboard(profiles.export());
                toast("Settings copied to clipboard");
            }));
            rows.add(new ButtonRow("Import settings", "Paste", "Reads settings text from the clipboard; invalid input is rejected.", () ->
                    toast(profiles.importText(MinecraftClient.getInstance().keyboard.getClipboard()))));
            rows.add(new ButtonRow("Reset everything", "Reset", "Restores every setting to its default.", () -> { settings.resetAll(); toast("All settings reset"); }));
        } else {
            for (Setting<?> s : settings.inCategory(PAGE_CATS[page])) {
                if (!s.isVisible()) continue;
                if (s instanceof BoolSetting b) rows.add(new ToggleRow(b));
                else if (s instanceof FloatSetting f) rows.add(new SliderRow(f));
                else if (s instanceof ChoiceSetting c) rows.add(new ChoiceRow(c));
                else if (s instanceof ColorSetting c) rows.add(new ColorRow(c));
            }
        }
        scrollTarget = Math.min(scrollTarget, maxScrollSafe());
    }

    private double maxScrollSafe() { return Math.max(0, rows.size() * ROW_H - Math.max(40, ch)); }

    private void selectPage(int p) {
        if (p == page) return;
        page = p; lastPage = p;
        scrollTarget = 0; scroll.snap(0);
        pageFade.snap(0); pageFade.setTarget(1);
        rebuildRows();
        sounds.play(Cue.CATEGORY);
    }

    // ---------------------------------------------------------------- render
    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        double dt = clock.tick(System.nanoTime());
        boolean anim = settings.animationsEnabled.get();
        if (!anim) { open.snap(open.target()); pageFade.snap(1); scroll.snap(scrollTarget); }
        open.update(dt); pageFade.update(dt);
        scroll.setTarget(scrollTarget); scroll.update(dt);
        if (closing && open.isFinished()) { MinecraftClient.getInstance().setScreen(null); return; }

        layout();
        double a = open.get();
        int yOff = (int) Math.round((1 - a) * 14);
        g.fill(0, 0, width, height, Draw.fade(0xFF000000, 0.45 * a));

        int x = wx, y = wy + yOff;
        Draw.glow(g, x, y, ww, wh, 10, Draw.ACCENT, a);
        Draw.roundOutlined(g, x, y, ww, wh, 10, Draw.fade(Draw.BG, a), Draw.fade(Draw.BORDER, a));
        g.fillGradient(x + 1, y + 1, x + ww - 1, y + 40, Draw.fade(0x338B5CF6, a), Draw.fade(0x00000000, a));

        // sidebar + wordmark
        Draw.round(g, x + 1, y + 1, SIDEBAR_W, wh - 2, 9, Draw.fade(Draw.SIDEBAR, a));
        g.drawText(textRenderer, "NYXORA", x + 16, y + 14, Draw.fade(Draw.TEXT, a), true);
        g.fill(x + 16, y + 28, x + 16 + 34, y + 29, Draw.fade(Draw.ACCENT, a));
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            int by = y + 44 + i * 26;
            boolean sel = i == page;
            boolean hov = mouseX >= x + 8 && mouseX < x + SIDEBAR_W - 6 && mouseY >= by && mouseY < by + 22;
            double h = hoverTween("tab" + i, hov || sel ? 1 : 0, dt);
            Draw.round(g, x + 8, by, SIDEBAR_W - 14, 22, 6, Draw.fade(sel ? 0x558B5CF6 : 0x228B5CF6, a * (sel ? 1 : h)));
            if (sel) g.fill(x + 8, by + 5, x + 10, by + 17, Draw.fade(Draw.ACCENT, a));
            g.drawText(textRenderer, PAGE_NAMES[i], x + 18, by + 7, Draw.fade(sel ? Draw.TEXT : Draw.DIM, a), false);
        }

        // header
        g.drawText(textRenderer, PAGE_NAMES[page], cx, y + 16, Draw.fade(Draw.TEXT, a), true);

        // content
        double pf = pageFade.get();
        int contentA = (int) Math.round(a * pf * 255);
        double alpha = a * pf;
        int slide = (int) Math.round((1 - pf) * 10);
        g.enableScissor(cx - 4, cy + yOff, cx + cw + 4, cy + yOff + ch);
        double sy = scroll.get();
        Row hovered = null;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = (int) Math.round(cy + yOff + i * ROW_H - sy);
            r.rx = cx + slide; r.ry = ry; r.rw = cw;
            if (ry + ROW_H < cy + yOff || ry > cy + yOff + ch) continue;
            boolean over = mouseX >= cx && mouseX < cx + cw && mouseY >= ry && mouseY < ry + ROW_H - 2
                    && mouseY >= cy + yOff && mouseY < cy + yOff + ch;
            if (over) hovered = r;
            double h = hoverTween(r.key(), over ? 1 : 0, dt);
            Draw.round(g, r.rx, ry, cw, ROW_H - 4, 6, Draw.fade(0x99181824, alpha));
            if (h > 0.01) Draw.round(g, r.rx, ry, cw, ROW_H - 4, 6, Draw.fade(0x228B5CF6, alpha * h));
            r.render(g, mouseX, mouseY, alpha, dt);
        }
        g.disableScissor();

        // scrollbar
        if (maxScrollSafe() > 0) {
            double frac = ch / (double) (rows.size() * ROW_H);
            int bh = Math.max(16, (int) (ch * frac));
            int by = (int) (cy + yOff + (ch - bh) * (sy / maxScrollSafe()));
            Draw.round(g, cx + cw + 1, by, 2, bh, 1, Draw.fade(Draw.ACCENT, alpha * 0.7));
        }

        // tooltip
        if (hovered != null && settings.tooltipsEnabled.get() && hovered.tooltip() != null && dragging == null) {
            drawTooltip(g, hovered.tooltip(), mouseX, mouseY, alpha);
        }
        drawToasts(g, dt, a);
    }

    private double hoverTween(String key, double target, double dt) {
        Tween t = hover.computeIfAbsent(key, k -> new Tween(0, 0.12, Easing.EASE_OUT_CUBIC));
        if (!settings.animationsEnabled.get()) t.snap(target); else t.setTarget(target);
        t.update(dt);
        return t.get();
    }

    private void drawTooltip(DrawContext g, String text, int mx, int my, double alpha) {
        int w = textRenderer.getWidth(text) + 12;
        int x = Math.min(mx + 10, width - w - 4), y = Math.min(my + 12, height - 22);
        Draw.roundOutlined(g, x, y, w, 18, 5, Draw.fade(0xF0101018, alpha), Draw.fade(Draw.ACCENT, alpha * 0.6));
        g.drawText(textRenderer, text, x + 6, y + 5, Draw.fade(Draw.TEXT, alpha), false);
    }

    private void toast(String msg) {
        toasts.addLast(new Toast(msg));
        while (toasts.size() > 3) toasts.removeFirst();
        rebuildRows();
    }

    private void drawToasts(DrawContext g, double dt, double a) {
        int i = 0;
        for (Iterator<Toast> it = toasts.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            t.age += dt;
            if (t.age > 2.4) { it.remove(); continue; }
            double f = t.age < 0.2 ? t.age / 0.2 : t.age > 2.0 ? (2.4 - t.age) / 0.4 : 1;
            int w = textRenderer.getWidth(t.text) + 20;
            int x = wx + (ww - w) / 2, y = wy + wh - 30 - i * 24;
            Draw.roundOutlined(g, x, y, w, 20, 6, Draw.fade(0xF0161622, f * a), Draw.fade(Draw.ACCENT, f * a * 0.7));
            g.drawText(textRenderer, t.text, x + 10, y + 6, Draw.fade(Draw.TEXT, f * a), false);
            i++;
        }
    }

    // ---------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        layout();
        double mx = click.x(), my = click.y();
        int button = click.button();
        for (int i = 0; i < PAGE_NAMES.length; i++) {
            int by = wy + 44 + i * 26;
            if (mx >= wx + 8 && mx < wx + SIDEBAR_W - 6 && my >= by && my < by + 22) { selectPage(i); return true; }
        }
        if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ch) {
            for (Row r : rows) {
                if (my >= r.ry && my < r.ry + ROW_H - 2) {
                    if (r.press(mx, my, button)) dragging = r;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging != null) { dragging.drag(click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) { dragging.release(); dragging = null; return true; }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollTarget = Math.max(0, Math.min(maxScrollSafe(), scrollTarget - v * 26));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) { requestClose(); return true; }
        return super.keyPressed(input);
    }

    /** Esc (and the close button) route here; actual screen removal happens after the fade-out. */
    @Override
    public void close() { requestClose(); }

    private void requestClose() {
        if (closing) return;
        closing = true;
        open.setTarget(0);
        if (!settings.animationsEnabled.get()) open.snap(0);
        sounds.play(Cue.CLOSE);
        config.saveNow();
    }

    @Override
    public void removed() {
        clock.reset();
        config.saveNow();
    }

    // ---------------------------------------------------------------- rows impl
    private static final class Toast { final String text; double age; Toast(String t) { text = t; } }

    private abstract class Row {
        int rx, ry, rw;
        abstract String key();
        abstract String tooltip();
        abstract void render(DrawContext g, int mx, int my, double alpha, double dt);
        /** @return true if this press starts a drag */
        abstract boolean press(double mx, double my, int button);
        void drag(double mx) { }
        void release() { }
        void label(DrawContext g, String text, double alpha) {
            g.drawText(textRenderer, text, rx + 10, ry + 8, Draw.fade(Draw.TEXT, alpha), false);
        }
    }

    private final class ToggleRow extends Row {
        final BoolSetting s;
        ToggleRow(BoolSetting s) { this.s = s; }
        String key() { return s.id(); }
        String tooltip() { return s.tooltip(); }
        void render(DrawContext g, int mx, int my, double alpha, double dt) {
            label(g, s.label(), alpha);
            Tween t = toggles.computeIfAbsent(s.id(), k -> { Tween n = new Tween(s.get() ? 1 : 0, 0.15, Easing.EASE_OUT_CUBIC); return n; });
            if (settings.animationsEnabled.get()) t.setTarget(s.get() ? 1 : 0); else t.snap(s.get() ? 1 : 0);
            t.update(dt);
            int w = 30, h = 14, x = rx + rw - w - 10, y = ry + 4;
            int track = com.nyxora.client.util.ColorUtil.lerp(0xFF2A2A3A, Draw.ACCENT, (float) t.get());
            Draw.round(g, x, y, w, h, 7, Draw.fade(track, alpha));
            int knob = x + 2 + (int) Math.round(t.get() * (w - h));
            Draw.round(g, knob, y + 2, h - 4, h - 4, 5, Draw.fade(0xFFF4F4FA, alpha));
            g.drawText(textRenderer, s.get() ? "ON" : "OFF", x - 26, ry + 8, Draw.fade(s.get() ? Draw.ACCENT : Draw.DIM, alpha), false);
        }
        boolean press(double mx, double my, int b) {
            s.set(!s.get());
            sounds.play(s.get() ? Cue.TOGGLE_ON : Cue.TOGGLE_OFF);
            rebuildRows();
            return false;
        }
    }

    private final class SliderRow extends Row {
        final FloatSetting s;
        SliderRow(FloatSetting s) { this.s = s; }
        String key() { return s.id(); }
        String tooltip() { return s.tooltip(); }
        int trackX() { return rx + rw / 2 - 10; }
        int trackW() { return rw / 2 - 66; }
        void render(DrawContext g, int mx, int my, double alpha, double dt) {
            label(g, s.label(), alpha);
            int tx = trackX(), tw = trackW(), ty = ry + 11;
            Draw.round(g, tx, ty, tw, 4, 2, Draw.fade(0xFF2A2A3A, alpha));
            int fill = (int) Math.round(tw * s.normalized());
            g.fillGradient(tx, ty, tx + Math.max(2, fill), ty + 4, Draw.fade(Draw.INDIGO, alpha), Draw.fade(Draw.ACCENT, alpha));
            Draw.round(g, tx + fill - 3, ty - 3, 7, 10, 3, Draw.fade(0xFFF4F4FA, alpha));
            String v = s.display();
            g.drawText(textRenderer, v, rx + rw - 10 - textRenderer.getWidth(v), ry + 8, Draw.fade(Draw.DIM, alpha), false);
        }
        boolean press(double mx, double my, int b) { drag(mx); return true; }
        void drag(double mx) {
            double n = (mx - trackX()) / Math.max(1, trackW());
            s.set((float) (s.min() + Math.max(0, Math.min(1, n)) * (s.max() - s.min())));
        }
        void release() { sounds.play(Cue.SLIDER_RELEASE); } // one sound per drag, never continuous
    }

    private final class ChoiceRow extends Row {
        final ChoiceSetting s;
        ChoiceRow(ChoiceSetting s) { this.s = s; }
        String key() { return s.id(); }
        String tooltip() { return s.tooltip() + " (click / right-click to cycle)"; }
        void render(DrawContext g, int mx, int my, double alpha, double dt) {
            label(g, s.label(), alpha);
            int w = 118, x = rx + rw - w - 8;
            Draw.roundOutlined(g, x, ry + 3, w, 16, 5, Draw.fade(0xFF14141E, alpha), Draw.fade(Draw.BORDER, alpha));
            String v = s.get();
            g.drawText(textRenderer, v, x + (w - textRenderer.getWidth(v)) / 2, ry + 8, Draw.fade(Draw.TEXT, alpha), false);
        }
        boolean press(double mx, double my, int b) {
            List<String> o = s.options();
            int i = o.indexOf(s.get());
            s.set(o.get(Math.floorMod(i + (b == 1 ? -1 : 1), o.size())));
            sounds.play(Cue.CLICK);
            rebuildRows();
            return false;
        }
    }

    private final class ColorRow extends Row {
        final ColorSetting s;
        ColorRow(ColorSetting s) { this.s = s; }
        String key() { return s.id(); }
        String tooltip() { return s.tooltip() + " (click cycles palette; edit the config file for exact values)"; }
        void render(DrawContext g, int mx, int my, double alpha, double dt) {
            label(g, s.label(), alpha);
            int x = rx + rw - 30, y = ry + 4;
            Draw.roundOutlined(g, x, y, 22, 14, 4, Draw.fade(s.get() | 0xFF000000, alpha), Draw.fade(Draw.BORDER, alpha));
            String hex = String.format("#%06X", s.get() & 0xFFFFFF);
            g.drawText(textRenderer, hex, x - 6 - textRenderer.getWidth(hex), ry + 8, Draw.fade(Draw.DIM, alpha), false);
        }
        boolean press(double mx, double my, int b) {
            int cur = -1;
            for (int i = 0; i < Draw.PALETTE.length; i++) if ((Draw.PALETTE[i] & 0xFFFFFF) == (s.get() & 0xFFFFFF)) cur = i;
            s.set(Draw.PALETTE[Math.floorMod(cur + (b == 1 ? -1 : 1), Draw.PALETTE.length)]);
            sounds.play(Cue.CLICK);
            return false;
        }
    }

    private final class ButtonRow extends Row {
        final String label, button, tip; final Runnable action;
        ButtonRow(String label, String button, String tip, Runnable action) { this.label = label; this.button = button; this.tip = tip; this.action = action; }
        String key() { return "btn:" + label; }
        String tooltip() { return tip; }
        void render(DrawContext g, int mx, int my, double alpha, double dt) {
            label(g, label, alpha);
            int w = Math.max(54, textRenderer.getWidth(button) + 16), x = rx + rw - w - 8;
            boolean active = button.equals("ACTIVE");
            Draw.roundOutlined(g, x, ry + 3, w, 16, 5, Draw.fade(active ? 0x558B5CF6 : 0xFF14141E, alpha), Draw.fade(active ? Draw.ACCENT : Draw.BORDER, alpha));
            g.drawText(textRenderer, button, x + (w - textRenderer.getWidth(button)) / 2, ry + 8, Draw.fade(Draw.TEXT, alpha), false);
        }
        boolean press(double mx, double my, int b) { sounds.play(Cue.CLICK); action.run(); return false; }
    }
}
