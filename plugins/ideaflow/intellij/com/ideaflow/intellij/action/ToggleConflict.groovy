package com.ideaflow.intellij.action

import com.ideaflow.controller.IFMController
import com.ideaflow.intellij.IdeaFlowComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.UIBundle

@Mixin(ActionSupport)
class ToggleConflict extends ToggleAction {

    private static final String START_CONFLICT_TITLE = "Start Conflict"
    private static final String START_CONFLICT_MSG = "What conflict question is in your head?"

    private static final String END_CONFLICT_TITLE = "End Conflict"
    private static final String END_CONFLICT_MSG = "What answer resolved the conflict?"



    @Override
    boolean isSelected(AnActionEvent e) {
        return isOpenConflict(e)
    }

    @Override
    void setSelected(AnActionEvent e, boolean state) {
        IFMController controller = IdeaFlowComponent.getIFMController(e.project)

        if (controller.isOpenConflict()) {
            String note = controller.promptForInput(END_CONFLICT_TITLE, END_CONFLICT_MSG)
            controller.endConflict(note)
        } else {
            String note = controller.promptForInput(START_CONFLICT_TITLE, START_CONFLICT_MSG)
            controller.startConflict(note)
        }
    }



    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Presentation presentation = e.getPresentation()
        disableWhenNoIdeaFlow(e)

        if (isOpenConflict()) {
            presentation.setText(END_CONFLICT_TITLE)
        } else {
            presentation.setText(START_CONFLICT_TITLE)
        }

    }

}
