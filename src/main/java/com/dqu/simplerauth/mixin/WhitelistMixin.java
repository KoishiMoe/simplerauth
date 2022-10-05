package com.dqu.simplerauth.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.ServerConfigList;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(Whitelist.class)
public abstract class WhitelistMixin extends ServerConfigList<GameProfile, WhitelistEntry> {
    public WhitelistMixin(File file) {
        super(file);
    }

    @Inject(method = "isAllowed", at = @At("HEAD"), cancellable = true)
    public void isAllowed(GameProfile profile, CallbackInfoReturnable<Boolean> cir) {
        String username = profile.getName();
        if (containsCaseInsensitive(((Whitelist) (Object)this).getNames(), username)) cir.setReturnValue(true);
    }

    private boolean containsCaseInsensitive(String[] list, String username) {
        for (String name : list) {
            if (name.equalsIgnoreCase(username)) return true;
        }
        return false;
    }
}
