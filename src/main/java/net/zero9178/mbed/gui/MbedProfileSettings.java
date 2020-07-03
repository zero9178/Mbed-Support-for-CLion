package net.zero9178.mbed.gui;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class MbedProfileSettings implements Configurable {

    protected JComponent myDecorator;
    private JPanel myPanel;

    @Override
    public @Nullable JComponent createComponent() {
        return myPanel;
    }

    protected abstract void createUIComponents();
}
