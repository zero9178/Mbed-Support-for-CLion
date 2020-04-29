package net.zero9178.mbed.project

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.VcsCloneComponent

/**
 * Class implementing the "Get from Version Control" functionality for mbed import
 */
class MbedImportCheckout : CheckoutProvider {

    override fun doCheckout(project: Project, listener: CheckoutProvider.Listener?) {
        assert(false)
    }

    override fun getVcsName(): String = "mbed import"

    override fun buildVcsCloneComponent(project: Project, modalityState: ModalityState): VcsCloneComponent {
        return MbedImportCheckoutDialogImpl(project)
    }
}