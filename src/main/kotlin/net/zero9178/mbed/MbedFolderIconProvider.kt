package net.zero9178.mbed

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import icons.MbedIcons
import net.zero9178.mbed.gui.MbedPackagesView

/**
 * Changes folder icon to overlay the mbed logo if the folder is a mbed package
 */
class MbedFolderIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int) =
    //We are using getServiceIfCreated here because if this project is not an mbed project we don't want
        // MbedPackageView to be instantiated.
        if (element is PsiDirectory && element.project.getServiceIfCreated(MbedPackagesView::class.java)?.packages?.any {
                it.name == element.name
            } == true
        ) {
            MbedIcons.MBED_FOLDER
        } else {
            null
        }
}
