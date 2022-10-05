package com.dqu.simplerauth.mixin;

import com.dqu.simplerauth.AuthMod;
import com.dqu.simplerauth.managers.CacheManager;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.UUID;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {
    // Not elegant, but it does work
    @Inject(method = "executeRemove", at = @At("RETURN"))
    private static void removeOnline(ServerCommandSource source, Collection<GameProfile> targets, CallbackInfoReturnable<Integer> cir) {
        Whitelist whitelist = source.getServer().getPlayerManager().getWhitelist();

        for (GameProfile target : targets) {
            if (whitelist.isAllowed(target)) {
                String username = target.getName();
                if (AuthMod.doesMinecraftAccountExist(username)) {
                    JsonObject cachedAccount = CacheManager.getMinecraftAccount(username);
                    if (cachedAccount != null) {
                        String onlineUuid = cachedAccount.get("online-uuid").getAsString();
                        // UUID.fromString doesn't work on a uuid without dashes, we need to add them back before creating the UUID
                        onlineUuid = onlineUuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");

                        GameProfile onlineProfile = new GameProfile(UUID.fromString(onlineUuid), username);
                        WhitelistEntry onlineEntry = new WhitelistEntry(onlineProfile);
                        whitelist.remove(onlineEntry);
                    }
                }
            }
        }
        source.getServer().kickNonWhitelistedPlayers(source);
    }
}
