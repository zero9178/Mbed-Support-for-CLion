package net.zero9178.mbed

import com.intellij.ide.IconProvider
import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import icons.MbedIcons
import net.zero9178.mbed.gui.MbedPackagesView

class MbedFolderIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int) =
        if (element is PsiDirectory && ServiceManager.getServiceIfCreated(
                element.project,
                MbedPackagesView::class.java
            )?.packages?.any {
                it.name == element.name
            } == true
        ) {
            MbedIcons.MBED_FOLDER
        } else {
            null
        }
}
