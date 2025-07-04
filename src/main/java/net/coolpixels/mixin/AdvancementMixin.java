package net.coolpixels.mixin;

import net.coolpixels.ServerEventHandler;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.advancement.PlayerAdvancementTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public class AdvancementMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Inject(at = @At("HEAD"), method = "grantCriterion")
    private void onGrantCriterion(AdvancementEntry advancement, String criterionName,
            CallbackInfoReturnable<Boolean> cir) {
        if (owner != null) {
            AdvancementProgress progress = ((PlayerAdvancementTracker) (Object) this).getProgress(advancement);
            if (progress != null && progress.isDone()) {
                String advancementId = advancement.id().toString();
                ServerEventHandler.reportEvent(owner, "advancement", advancementId);
            }
        }
    }
}
