/*
 * Created by JFormDesigner on Tue May 28 21:25:26 CEST 2019
 */

package net.zero9178;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Schülerlizenz 2019/20
 */
public abstract class MbedSettings implements Configurable {

    protected TextFieldWithBrowseButton myCliPath;
    private JPanel myPanel;

    @Override
    public @Nullable
    JComponent createComponent() {
        return myPanel;
    }
}
