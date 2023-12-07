package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class GuiButtonText extends ButtonWidget {
    private boolean editing;
    private final TextFieldWidget textField;

    public GuiButtonText(TextRenderer fontRenderer, int x, int y, int width, int height, Text message, PressAction onPress) {
        super (x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.textField = new TextFieldWidget(fontRenderer, x + 1, y + 1, width - 2, height - 2, null);
    }

    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (editing) {
            textField.render(drawContext, mouseX, mouseY, delta);
            return;
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean pressed = super.mouseClicked(mouseX, mouseY, button);
        this.setEditing(pressed);
        return pressed;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
        if (editing) this.setFocused(true);

        textField.setFocused(editing);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!(editing)) return super.keyPressed(keyCode, scanCode, modifiers);
        if (keyCode != 257 && keyCode != 335 && keyCode != 258) return textField.keyPressed(keyCode, scanCode, modifiers);

        setEditing(false);
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!(editing)) return super.charTyped(chr, modifiers);
        if (chr != '\r') return textField.charTyped(chr, modifiers);

        setEditing(false);
        return false;
    }

    public boolean isEditing() { return editing; }

    public void setText(String text) { textField.setText(text); }

    public String getText() { return textField.getText(); }
}