package icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.LayeredIcon;

import javax.swing.*;

public interface MbedIcons {

    Icon PLUGIN_ICON_13x13 = IconLoader.getIcon("/icons/pluginIcon_13x13.png");

    Icon PLUGIN_ICON_16x16 = IconLoader.getIcon("/icons/pluginIcon_16x16.png");

    Icon PLUGIN_FOLDER = LayeredIcon.create(AllIcons.Nodes.Folder, IconLoader.getIcon("/icons/pluginFolder_16x16.png"));
}
