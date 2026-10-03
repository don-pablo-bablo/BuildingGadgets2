package com.direwolf20.buildinggadgets2.client.screen;

import com.direwolf20.buildinggadgets2.common.network.handler.PacketPowerSetting;
import com.direwolf20.buildinggadgets2.setup.Config;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Mod-list config screen: the common settings up front, with NeoForge's full config screen one click away.
 */
public class BG2ConfigScreen extends Screen {
    private final ModContainer container;
    private final Screen parent;

    public BG2ConfigScreen(ModContainer container, Screen parent) {
        super(Component.translatable("buildinggadgets2.configuration.title"));
        this.container = container;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 40;

        // On a remote server the server's setting applies, so show it but don't allow changes
        boolean remoteServer = minecraft.getConnection() != null && !minecraft.isLocalServer();
        CycleButton<Boolean> requirePower = CycleButton.onOffBuilder(Config.isPowerRequired())
                .create(x, y, 200, 20, Component.translatable("buildinggadgets2.configuration.requirePower"), (button, value) -> {
                    Config.REQUIRE_POWER.set(value);
                    Config.REQUIRE_POWER.save();
                    PacketPowerSetting.broadcast();
                });
        if (remoteServer) {
            requirePower.active = false;
            requirePower.setTooltip(Tooltip.create(Component.translatable("buildinggadgets2.configuration.requirePower.server")));
        } else {
            requirePower.setTooltip(Tooltip.create(Component.translatable("buildinggadgets2.configuration.requirePower.tooltip")));
        }
        addRenderableWidget(requirePower);

        addRenderableWidget(Button.builder(Component.translatable("buildinggadgets2.configuration.allSettings"),
                        b -> minecraft.gui.setScreen(new ConfigurationScreen(container, this)))
                .bounds(x, y + 24, 200, 20)
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(x, y + 60, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.centeredText(font, title, width / 2, height / 2 - 70, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
