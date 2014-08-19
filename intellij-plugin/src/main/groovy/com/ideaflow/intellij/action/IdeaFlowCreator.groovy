package com.ideaflow.intellij.action

import com.ideaflow.controller.IFMController
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import javax.swing.Icon

@Mixin(ActionSupport)
class IdeaFlowCreator {

	void createNewIdeaFlow(AnActionEvent e) {
		IFMController controller = getIFMController(e)

		if (controller != null) {
			File ideaFlowMapFile = getSelectedOrCreateNewIdeaFlowMapFile(e)

			if (ideaFlowMapFile != null) {
				controller.newIdeaFlow(e.project, ideaFlowMapFile)
			}
		}
	}

	private File getSelectedOrCreateNewIdeaFlowMapFile(AnActionEvent e) {
		VirtualFile selectedIdeaFlowMapVirtualFile = getSelectedIdeaFlowMapFile(e)
		File ideaFlowMapFile

		if (selectedIdeaFlowMapVirtualFile) {
			ideaFlowMapFile = new File(selectedIdeaFlowMapVirtualFile.path)
		} else {
			ideaFlowMapFile = createNewFile(e)
		}
		ideaFlowMapFile
	}

	private File createNewFile(AnActionEvent e) {
		VirtualFile parentDir = getParentDirForNewFile(e)
		MessageSpec messageToShow = MessageSpec.question("Create IdeaFlow map", "Name of IdeaFlow mapping file at location ${parentDir.name}:")
		String newFileName = promptWith(messageToShow)
		newFileName ? new File(parentDir.path, newFileName) : null
	}

	private VirtualFile getParentDirForNewFile(AnActionEvent event) {
		VirtualFile[] selectedFiles = PlatformDataKeys.VIRTUAL_FILE_ARRAY.getData(event.dataContext)
		VirtualFile rootDirOfContainingModule = null

		if (selectedFiles) {
			rootDirOfContainingModule = findRootDirOfContainingModule(event.project, selectedFiles[0])
		}
		rootDirOfContainingModule ? rootDirOfContainingModule : event.project.baseDir
	}

	private VirtualFile findRootDirOfContainingModule(Project project, VirtualFile selectedFile) {
		Module containingModule = ModuleManager.getInstance(project).getModules().find { Module module ->
			module.moduleContentScope.contains(selectedFile)
		}

		VirtualFile rootDir = null
		if (containingModule) {
			GlobalSearchScope moduleContentScope = containingModule.moduleContentScope

			rootDir = selectedFile.isDirectory() ? selectedFile : selectedFile.parent
			while (moduleContentScope.contains(rootDir.parent)) {
				rootDir = rootDir.parent
			}
		}
		rootDir
	}

	private String promptWith(MessageSpec spec) {
		return Messages.showInputDialog(spec.message, spec.title, spec.icon)
	}

	private static class MessageSpec {
		String title
		String message
		Icon icon

		static MessageSpec error(String message) {
			return new MessageSpec(title: 'Error', message: message, icon: Messages.getErrorIcon())
		}

		static MessageSpec question(String title, String message) {
			return new MessageSpec(title: title, message: message, icon: Messages.getQuestionIcon())
		}
	}

}