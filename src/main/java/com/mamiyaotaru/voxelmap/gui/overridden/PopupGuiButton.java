package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PopupGuiButton extends ButtonWidget {
    final IPopupGuiScreen parentScreen;

    public PopupGuiButton(int x, int y, int width, int height, Text message, PressAction onPress, IPopupGuiScreen parentScreen) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.parentScreen = parentScreen;
    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        boolean canHover = this.parentScreen.overPopup(mouseX, mouseY);
        if (!canHover) {
            mouseX = 0;
            mouseY = 0;
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }
}
