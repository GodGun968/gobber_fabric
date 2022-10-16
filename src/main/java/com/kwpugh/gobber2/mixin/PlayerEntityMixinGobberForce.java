package com.kwpugh.gobber2.mixin;

import com.kwpugh.gobber2.Gobber2;
import com.kwpugh.gobber2.util.GobberForceManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
    Mixins for various methods in PlayerEntity
    class to support Gobber Force
 */

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixinGobberForce extends LivingEntity
{
    @Shadow public abstract void readCustomDataFromNbt(NbtCompound nbt);
    @Shadow public abstract void writeCustomDataToNbt(NbtCompound nbt);

    protected PlayerEntityMixinGobberForce(EntityType<? extends LivingEntity> type, World world)
    {
        super(type, world);
    }

    // Run various tasks each player tick
    @Inject(method = "tick", at = @At("TAIL"))
    public void gobberForceTick(CallbackInfo ci)
    {
        if(world instanceof ServerWorld && Gobber2.CONFIG.GENERAL.enableGobberForce)
        {
            PlayerEntity player = (PlayerEntity) (Object) this;
            GobberForceManager.update(player);
        }
    }

    // Gobber Force damage shield
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void gobberForceDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
    {
        if(world instanceof ServerWorld && Gobber2.CONFIG.GENERAL.enableGobberForce)
        {
            PlayerEntity player = (PlayerEntity) (Object) this;
            int damageAmount = (int) amount;

            //damage reduction only available above a certain GobberForce level
            if(GobberForceManager.getGobberForce(player) > 30)
            {
                if(damageAmount <= GobberForceManager.getGobberForce(player))
                {
                    GobberForceManager.subtractGobberForce(player, damageAmount);
                    player.sendMessage((Text.translatable("Player damage absorbed by GobberForce: " + damageAmount).formatted(Formatting.GOLD).formatted(Formatting.BOLD)), true);
                    cir.setReturnValue(false);
                }
                else if(damageAmount > GobberForceManager.getGobberForce(player))
                {
                    int diff = damageAmount - GobberForceManager.getGobberForce(player);
                    amount = damageAmount - diff;
                    player.sendMessage((Text.translatable("Your GobberForce amount reduced by: " + diff).formatted(Formatting.DARK_PURPLE).formatted(Formatting.BOLD)), true);
                    GobberForceManager.subtractGobberForce(player, diff);
                }
            }
        }
    }

    // Cancel exhaustion
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    public void gobberForceAddExhaustion(float exhaustion, CallbackInfo ci)
    {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if(player.world.isClient)
        {
            if((exhaustion > 0.0F) && (GobberForceManager.getGobberForce(player) > 30))
            {
                player.sendMessage((Text.translatable("GobberForce vigor!").formatted(Formatting.BLUE).formatted(Formatting.BOLD)), true);
                GobberForceManager.subtractGobberForce(player, 1);
                ci.cancel();
            }
        }
    }

    // Add writeNbt to player class
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    public void writeNbtGobberForce(NbtCompound nbt, CallbackInfo ci)
    {
        if(world instanceof ServerWorld && Gobber2.CONFIG.GENERAL.enableGobberForce)
        {
            GobberForceManager.writeNbt(nbt);
        }
    }

    // Add readNbt to player class
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    public void readNbtGobberForce(NbtCompound nbt, CallbackInfo ci)
    {
        if(world instanceof ServerWorld && Gobber2.CONFIG.GENERAL.enableGobberForce)
        {
            if(nbt.contains("gobberForce"))
            {
                GobberForceManager.readNbt(nbt);
            }
        }
    }
}
