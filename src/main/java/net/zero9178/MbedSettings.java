/*
 * Created by JFormDesigner on Tue May 28 21:25:26 CEST 2019
 */

package net.zero9178;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;

import javax.swing.*;

/**
 * @author Schülerlizenz 2019/20
 */
public abstract class MbedSettings implements Configurable {
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    protected JPanel m_panel;
    protected JBCheckBox m_enableChaching;
    protected JButton m_clearCache;
    protected TextFieldWithBrowseButton m_cliPath;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public MbedSettings() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        m_panel = new JPanel();
        m_enableChaching = new JBCheckBox();
        m_clearCache = new JButton();
        JSeparator separator1 = new JSeparator();
        JLabel label1 = new JLabel();
        m_cliPath = new TextFieldWithBrowseButton();
        CellConstraints cc = new CellConstraints();

        //======== m_panel ========
        {
            m_panel.setLayout(new FormLayout(
                    new ColumnSpec[]{
                            new ColumnSpec(Sizes.dluX(70)),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(Sizes.dluX(162))
                    },
                    new RowSpec[]{
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC
                    }));

            //---- m_enableChaching ----
            m_enableChaching.setText("Cache releases");
            m_panel.add(m_enableChaching, cc.xy(1, 1));

            //---- m_clearCache ----
            m_clearCache.setText("Clear cache");
            m_panel.add(m_clearCache, cc.xy(3, 1));
            m_panel.add(separator1, cc.xywh(1, 3, 3, 1));

            //---- label1 ----
            label1.setText("mbed cli path:");
            m_panel.add(label1, cc.xy(1, 5));
            m_panel.add(m_cliPath, cc.xy(3, 5));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
}
