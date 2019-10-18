package net.zero9178.mbed

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import icons.MbedIcons

class MbedFolderIconProvider : IconProvider() {
    override fun getIcon(element: PsiElement, flags: Int) = if (element is PsiDirectory && element.name == "mbed-os") {
        MbedIcons.MBED_FOLDER
    } else {
        null
    }
}
