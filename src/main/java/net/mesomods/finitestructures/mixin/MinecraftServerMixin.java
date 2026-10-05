package net.mesomods.finitestructures.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.mesomods.finitestructures.StructureCountLimitManager;
import net.mesomods.finitestructures.notmixin.ServerLevelMixin;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements net.mesomods.finitestructures.notmixin.MinecraftServerMixin {
    @Unique
    private StructureCountLimitManager finiteStructures$structureCountLimitManager;

    @Shadow
    public abstract RegistryAccess.Frozen registryAccess();

    @Unique
    @Override
    public StructureCountLimitManager finiteStructures$getStructureCountLimitManager() {
        return this.finiteStructures$structureCountLimitManager;
    }

    @Inject(method = "createLevels", at = @At("HEAD"))
    public void finiteStructures$onCreateLevels(CallbackInfo ci) {
        this.finiteStructures$structureCountLimitManager = new StructureCountLimitManager(this.registryAccess());
    }

    @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getRandomSequences()Lnet/minecraft/world/RandomSequences;"))
    public void finiteStructures$initOverworldStructureLimitData(CallbackInfo ci, @Local ServerLevel overworld) {
        ((ServerLevelMixin)overworld).finiteStructures$initializeStructureLimitData();
    }
}
