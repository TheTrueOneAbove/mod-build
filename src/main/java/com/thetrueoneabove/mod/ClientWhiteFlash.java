package com.thetrueoneabove.mod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Matrix4f;

import java.util.Random;

@EventBusSubscriber(modid = TheTrueOneAboveMod.MODID, value = Dist.CLIENT)
public final class ClientWhiteFlash {
    private static final int FLASH_TOTAL = 70;
    private static final int IMPACT_TICKS = 12;
    private static final int TIMESTOP_INTRO = 10;

    private static int flashTicks = 0;
    private static int introTicks = 0;
    private static int freezeTicks = 0;
    private static boolean freezeCaster = false;
    private static int shakeTicks = 0;
    private static long seed = 0;

    private ClientWhiteFlash() {}

    /** "let there be light": impact frames, white hold, long fade. */
    public static void start() {
        flashTicks = FLASH_TOTAL;
        shakeTicks = 14;
        seed = System.nanoTime();
    }

    /** "the first and the last": impact frames, then a grey tint + countdown. */
    public static void startTimeStop(int ticks, boolean caster) {
        freezeTicks = ticks;
        freezeCaster = caster;
        introTicks = TIMESTOP_INTRO;
        shakeTicks = 10;
        seed = System.nanoTime();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (flashTicks > 0) flashTicks--;
        if (introTicks > 0) introTicks--;
        if (freezeTicks > 0) freezeTicks--;
        if (shakeTicks > 0) shakeTicks--;
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0) return;
        float s = shakeTicks / 10f;
        event.setRoll(event.getRoll() + (float) ((Math.random() - 0.5) * 7.0 * s));
        event.setPitch(event.getPitch() + (float) ((Math.random() - 0.5) * 3.0 * s));
        event.setYaw(event.getYaw() + (float) ((Math.random() - 0.5) * 3.0 * s));
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();

        // Time-stop: tint + countdown (under the impact frames).
        if (freezeTicks > 0) {
            if (!freezeCaster) g.fill(0, 0, w, h, 0x40B4B8D0);
            String text = "TIME STOPPED  " + (freezeTicks / 20) + "." + ((freezeTicks % 20) / 2) + "s";
            g.drawCenteredString(mc.font, text, w / 2, 20, freezeCaster ? 0xFFFFD700 : 0xFFFFFFFF);
        }

        if (introTicks > 0) {
            int elapsed = TIMESTOP_INTRO - introTicks;
            drawImpact(g, w, h, elapsed / 2, seed + 7);
        }

        if (flashTicks > 0) {
            int elapsed = FLASH_TOTAL - flashTicks;
            if (elapsed < IMPACT_TICKS) {
                drawImpact(g, w, h, elapsed / 2, seed);
            } else {
                int alpha = flashTicks > 50 ? 255 : Math.min(255, flashTicks * 5);
                g.fill(0, 0, w, h, (alpha << 24) | 0x00FFFFFF);
            }
        }
    }

    /** One anime-style impact frame: flips between white-on-black and black-on-white, with speed lines. */
    private static void drawImpact(GuiGraphics g, int w, int h, int frame, long frameSeed) {
        boolean negative = (frame & 1) == 0;
        int bg = negative ? 0xFF000000 : 0xFFFFFFFF;
        int fg = negative ? 0xFFFFFFFF : 0xFF000000;
        g.fill(0, 0, w, h, bg);
        drawSpeedLines(g, w, h, fg, new Random(frameSeed + frame * 7919L));
    }

    private static void drawSpeedLines(GuiGraphics g, int w, int h, int color, Random rnd) {
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = (float) Math.hypot(cx, cy) * 1.15f;
        int r = (color >> 16) & 255;
        int gr = (color >> 8) & 255;
        int b = color & 255;
        int a = (color >>> 24) & 255;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = g.pose().last().pose();

        int spokes = 80;
        for (int i = 0; i < spokes; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double half = 0.006 + rnd.nextDouble() * 0.05;
            float inner = radius * (0.06f + rnd.nextFloat() * 0.30f);
            float x0 = cx + (float) Math.cos(ang) * inner;
            float y0 = cy + (float) Math.sin(ang) * inner;
            float x1 = cx + (float) Math.cos(ang - half) * radius;
            float y1 = cy + (float) Math.sin(ang - half) * radius;
            float x2 = cx + (float) Math.cos(ang + half) * radius;
            float y2 = cy + (float) Math.sin(ang + half) * radius;
            buf.vertex(m, x0, y0, 0).color(r, gr, b, a).endVertex();
            buf.vertex(m, x1, y1, 0).color(r, gr, b, a).endVertex();
            buf.vertex(m, x2, y2, 0).color(r, gr, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
