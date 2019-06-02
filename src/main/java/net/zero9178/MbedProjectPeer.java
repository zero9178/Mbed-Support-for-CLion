/*
 * Created by JFormDesigner on Mon May 27 15:18:55 CEST 2019
 */

package net.zero9178;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.platform.ProjectGeneratorPeer;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBLabel;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;

import javax.swing.*;

/**
 * @author Schülerlizenz 2019/20
 */
public abstract class MbedProjectPeer implements ProjectGeneratorPeer<String> {
    public MbedProjectPeer() {
        initComponents();
    }

    protected void setLoading(boolean loading) {
        m_loading.setIcon(loading ? new AnimatedIcon.Default() : null);
    }

    protected JBLabel m_errorLabel;

    public JPanel getPanel() {
        return m_panel;
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel m_panel;
    protected ComboBox<String> m_versionSelection;
    private JBLabel m_loading;

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        m_panel = new JPanel();
        JBLabel m_versionLabel = new JBLabel();
        m_versionSelection = new ComboBox<>();
        m_loading = new JBLabel();
        m_errorLabel = new JBLabel();
        CellConstraints cc = new CellConstraints();

        //======== m_panel ========
        {
            m_panel.setLayout(new FormLayout(
                new ColumnSpec[] {
                    new ColumnSpec(Sizes.dluX(21)),
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(Sizes.dluX(44)),
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(Sizes.dluX(168)),
                    FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                    new ColumnSpec(Sizes.dluX(13))
                },
                    new RowSpec[]{
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC
                    }));

            //---- m_versionLabel ----
            m_versionLabel.setText("mbed-os version:");
            m_panel.add(m_versionLabel, cc.xywh(1, 1, 3, 1));
            m_panel.add(m_versionSelection, cc.xy(5, 1));

            //---- m_loading ----
            m_loading.setToolTipText("Querying versions...");
            m_panel.add(m_loading, cc.xywh(6, 1, 2, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));
            m_panel.add(m_errorLabel, cc.xywh(1, 3, 7, 1));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
