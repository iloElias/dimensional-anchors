package com.iloelias.dimensionalanchors;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

final class RoomBoundaryProtection {
    private RoomBoundaryProtection() {
    }

    static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(RoomGeneration.POCKET)
                && RoomAllocationSavedData.get(level).protects(event.getPos())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
        }
    }
}
