package com.direwolf20.buildinggadgets2.common.network.handler;

import com.direwolf20.buildinggadgets2.common.network.data.PowerSettingPayload;
import com.direwolf20.buildinggadgets2.setup.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Keeps clients on a remote server in step with the server's requirePower setting, so energy bars,
 * previews and cost checks match what the server enforces.
 */
public class PacketPowerSetting {
    public static final PacketPowerSetting INSTANCE = new PacketPowerSetting();

    public static PacketPowerSetting get() {
        return INSTANCE;
    }

    public void handle(final PowerSettingPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // In singleplayer / on the LAN host the client already shares the server's config.
            if (!context.connection().isMemoryConnection())
                Config.setServerRequirePower(payload.requirePower());
        });
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player, new PowerSettingPayload(Config.REQUIRE_POWER.get()));
    }

    public static void onConfigReloaded(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(com.direwolf20.buildinggadgets2.BuildingGadgets2.MODID))
            broadcast();
    }

    /**
     * Sends the current setting to everyone on the running server, if there is one.
     */
    public static void broadcast() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
            server.execute(() -> PacketDistributor.sendToAllPlayers(new PowerSettingPayload(Config.REQUIRE_POWER.get())));
    }
}
