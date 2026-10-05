package net.mesomods.finitestructures.mixin;

import net.mesomods.finitestructures.StructureLimitData;
import net.mesomods.finitestructures.notmixin.MinecraftServerMixin;
import net.mesomods.finitestructures.notmixin.StructureLimitDataUser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends LevelMixin implements net.mesomods.finitestructures.notmixin.ServerLevelMixin {
    @Shadow
    @Final
    private net.minecraft.world.level.levelgen.structure.StructureCheck structureCheck;
    @Shadow
    @Final
    private ServerChunkCache chunkSource;

    @Shadow
    public abstract DimensionDataStorage getDataStorage();

    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (this.dimension() == Level.OVERWORLD) return;
        this.initializeStructureLimitData();
    }

    @Unique
    @Override
    public void initializeStructureLimitData() {
        StructureLimitData data = this.getDataStorage().computeIfAbsent(StructureLimitData.TYPE);
        if (!data.isInitialized()) {
            data = StructureLimitData.create((ServerLevel) (Object) this, ((MinecraftServerMixin) this.getServer()).getStructureCountLimitManager());
            this.getDataStorage().set(StructureLimitData.TYPE, data);
        }
        ((StructureLimitDataUser) this.structureCheck).setStructureLimitData(data);
        ((StructureLimitDataUser) this.chunkSource.getGenerator()).setStructureLimitData(data);
    }
}
