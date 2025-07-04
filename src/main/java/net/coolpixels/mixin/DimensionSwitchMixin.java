package net.coolpixels.mixin;

import net.coolpixels.ServerEventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.TeleportTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TeleportTarget.class)
public class DimensionSwitchMixin {
    @Inject(at = @At("HEAD"), method = "sendTravelThroughPortalPacket")
    private static void onTravelThroughPortal(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            String dimension = serverPlayer.getWorld().getRegistryKey().getValue().toString();
            ServerEventHandler.reportEvent(serverPlayer, "dimension", dimension);
        }
    }
}
