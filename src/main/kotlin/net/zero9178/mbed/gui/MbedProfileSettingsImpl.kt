package net.zero9178.mbed.gui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import net.zero9178.mbed.actions.MbedReloadChangesAction
import net.zero9178.mbed.state.MbedProjectState

class MbedProfileSettingsImpl(private val myProject: Project) : MbedProfileSettings() {

    private lateinit var myList: JBList<String>
    private lateinit var myModel: CollectionListModel<String>
    private lateinit var myToolbarDecorator: ToolbarDecorator

    init {
        myModel.add(MbedProjectState.getInstance(myProject).additionalProfiles)
    }

    override fun isModified() = myModel.toList() != MbedProjectState.getInstance(myProject).additionalProfiles

    override fun getDisplayName(): String {
        return "Mbed profiles"
    }

    override fun apply() {
        MbedProjectState.getInstance(myProject).additionalProfiles = myModel.toList()
        MbedReloadChangesAction.update(myProject)
    }

    override fun createUIComponents() {
        myModel = CollectionListModel()
        myList = JBList(myModel)
        myToolbarDecorator = ToolbarDecorator.createDecorator(myList, myModel)
        myToolbarDecorator.setAddAction { _ ->
            val vf = myProject.basePath?.let { it -> LocalFileSystem.getInstance().findFileByPath(it) }
            FileChooser.chooseFile(FileChooserDescriptor(true, false, false, false, false, false), myProject, vf) {
                myModel.add(it.path)
            }
        }
        myToolbarDecorator.setEditAction { _ ->
            val vf = LocalFileSystem.getInstance().findFileByPath(myList.selectedValue)
            FileChooser.chooseFile(FileChooserDescriptor(true, false, false, false, false, false), myProject, vf) {
                myModel.setElementAt(it.path, myList.selectedIndex)
            }
        }
        myDecorator = myToolbarDecorator.createPanel()
    }
}