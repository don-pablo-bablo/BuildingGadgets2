package com.direwolf20.buildinggadgets2.client.screen.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class GuiTextFieldBase extends EditBox {
    private boolean suspended;
    private String valueDefault, valueOld;
    private BiConsumer<GuiTextFieldBase, String> postModification;
    private Predicate<String> filter = s -> true;

    public GuiTextFieldBase(Font fontRenderer, int x, int y, int width) {
        super(fontRenderer, x, y, width, 15, Component.empty());

        setMaxLength(50);
    }

    // EditBox no longer supports filters as of 26.3, so validate each edit and roll it back if rejected.
    private void setFilter(Predicate<String> filter) {
        this.filter = filter;
    }

    private void applyFiltered(Runnable edit) {
        String before = getValue();
        int cursor = getCursorPosition();
        edit.run();
        if (!filter.test(getValue())) {
            super.setValue(before);
            setCursorPosition(cursor);
            setHighlightPos(cursor);
        }
    }

    @Override
    public void insertText(String input) {
        applyFiltered(() -> super.insertText(input));
    }

    @Override
    public void deleteCharsToPos(int pos) {
        applyFiltered(() -> super.deleteCharsToPos(pos));
    }

    @Override
    public void setValue(String textIn) {
        valueOld = getValue();
        super.setValue(textIn);
        postModification(textIn);
    }

    public void postModification(String text) {
        if (!suspended && postModification != null) {
            suspended = true;
            postModification.accept(this, valueOld);
            suspended = false;
        }
    }

    public GuiTextFieldBase restrictToNumeric() {
        setFilter(s -> {
            if (s == null || s.isEmpty() || "-".equals(s))
                return true;

            try {
                Integer.parseInt(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        });

        return this;
    }

    public int getInt() {
        try {
            return Integer.parseInt(getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public GuiTextFieldBase setDefaultInt(int defaultInt) {
        return setDefaultValue(Integer.toString(defaultInt));
    }

    public GuiTextFieldBase setDefaultValue(String defaultValue) {
        this.valueDefault = defaultValue;
        return this;
    }
}
