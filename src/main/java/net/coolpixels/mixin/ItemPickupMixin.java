package net.coolpixels.mixin;

import net.coolpixels.ServerEventHandler;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemPickupMixin {
    @Inject(at = @At("HEAD"), method = "onPlayerCollision")
    private void onPlayerPickup(PlayerEntity player, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ItemStack stack = ((ItemEntity) (Object) this).getStack();
            if (!stack.isEmpty()) {
                String itemId = stack.getItem().toString();
                ServerEventHandler.reportEvent(serverPlayer, "item", itemId);
            }
        }
    }
}
