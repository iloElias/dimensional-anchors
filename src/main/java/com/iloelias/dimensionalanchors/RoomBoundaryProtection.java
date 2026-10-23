package com.iloelias.dimensionalanchors;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.BlockEvent;
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

    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(RoomGeneration.POCKET)) {
            return;
        }
        var allocation = RoomAllocationSavedData.get(level);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            if (multi.getReplacedBlockSnapshots().stream()
                    .anyMatch(snapshot -> !allocation.allowsPlacement(snapshot.getPos()))) {
                event.setCanceled(true);
            }
        } else if (!allocation.allowsPlacement(event.getPos())) {
            event.setCanceled(true);
        }
    }
}
