package com.example.vsiefix;

import net.minecraft.world.level.Level;

/**
 * Create renders schematic previews in fake levels (SchematicLevel / SchematicWorld /
 * VirtualRenderWorld). Block entities inside them get re-created and re-loaded from NBT over and
 * over while a blueprint is held, so any heavy per-block logic there is a frame killer.
 */
public final class PreviewGuard {

    private static int reported;

    private PreviewGuard() {
    }

    public static boolean isPreview(Level level) {
        if (level == null) {
            return false;
        }
        String name = level.getClass().getName();
        boolean preview = name.contains("Schematic") || name.contains("VirtualRenderWorld");
        if (preview && reported < 8) {
            reported++;
            System.out.println("[vsiefix] skipped VSIE link logic in preview level " + name);
        }
        return preview;
    }
}
