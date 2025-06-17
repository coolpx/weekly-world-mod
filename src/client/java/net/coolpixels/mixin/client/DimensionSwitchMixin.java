package net.coolpixels.mixin.client;

import net.coolpixels.WeeklyWorldClient;
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
    private static void init(Entity entity, CallbackInfo info) {
        if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
            String dimension = serverPlayerEntity.getWorld().getRegistryKey().getValue().toString();
            WeeklyWorldClient.reportEvent("dimension", dimension);
        }
    }
}