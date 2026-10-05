package net.mesomods.finitestructures.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocateCommand.class)
public abstract class LocateCommandMixin {
    @Inject(method = "dist", at = @At("RETURN"), cancellable = true)
    private static void finiteStructures$saferDist46340(CallbackInfoReturnable<Float> cir, @Local(name = "dx") int m, @Local(name = "dz") int n) {
        cir.setReturnValue(Mth.sqrt((float) ((long) m * (long) m + (long) n * (long) n)));
    }
}
