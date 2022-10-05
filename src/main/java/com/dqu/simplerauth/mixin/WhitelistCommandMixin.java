package com.dqu.simplerauth.mixin;

import com.dqu.simplerauth.AuthMod;
import com.dqu.simplerauth.managers.CacheManager;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.dynamic.DynamicSerializableUuid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.UUID;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {
    @Shadow @Final private static SimpleCommandExceptionType REMOVE_FAILED_EXCEPTION;
    @Shadow @Final private static SimpleCommandExceptionType ADD_FAILED_EXCEPTION;

    @Overwrite
    private static int executeAdd(ServerCommandSource source, Collection<GameProfile> targets) throws CommandSyntaxException {
        // Force use offline UUID to avoid confusion. This should not affect the player's archive
        Whitelist whitelist = source.getServer().getPlayerManager().getWhitelist();
        int i = 0;

        for (GameProfile profile : targets) {
            if (!whitelist.isAllowed(profile)) {
                String username = profile.getName();

                UUID offlineUuid = DynamicSerializableUuid.getOfflinePlayerUuid(username);
                GameProfile offlineProfile = new GameProfile(offlineUuid, username);
                WhitelistEntry offlineEntry = new WhitelistEntry(offlineProfile);
                whitelist.add(offlineEntry);
                source.sendFeedback(Text.translatable("commands.whitelist.add.success", Texts.toText(profile)), true);
                ++i;
            }
        }

        if (i == 0) {
            throw ADD_FAILED_EXCEPTION.create();
        } else {
            return i;
        }
    }

    @Overwrite
    private static int executeRemove(ServerCommandSource source, Collection<GameProfile> targets) throws CommandSyntaxException {
        Whitelist whitelist = source.getServer().getPlayerManager().getWhitelist();
        int i = 0;

        for (GameProfile profile : targets) {
            if (whitelist.isAllowed(profile)) {
                String username = profile.getName();
                // handle online account
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
                // handle offline account
                UUID offlineUuid = DynamicSerializableUuid.getOfflinePlayerUuid(username);
                GameProfile offlineProfile = new GameProfile(offlineUuid, username);
                WhitelistEntry offlineEntry = new WhitelistEntry(offlineProfile);
                whitelist.remove(offlineEntry);

                source.sendFeedback(Text.translatable("commands.whitelist.remove.success", Texts.toText(profile)), true);
                ++i;
            }
        }
        if (i == 0) {
            throw REMOVE_FAILED_EXCEPTION.create();
        } else {
            source.getServer().kickNonWhitelistedPlayers(source);
            return i;
        }
    }
}
