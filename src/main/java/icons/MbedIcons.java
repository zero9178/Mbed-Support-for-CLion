package icons;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;

import javax.swing.*;

public interface MbedIcons {

    Icon MBED_ICON_16x16 = IconLoader.getIcon("/icons/mbedOs.png");

    Icon MBED_ICON_13x13 = IconUtil.toSize(MBED_ICON_16x16,13,13);

    Icon MBED_FOLDER = IconLoader.getIcon("/icons/mbedOsFolder.png");
}
