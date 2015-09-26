package com.ideaflow.dsl.client.local

import com.ideaflow.controller.IDEService
import com.ideaflow.dsl.DSLTimelineSerializer
import com.ideaflow.dsl.client.IIdeaFlowClient
import com.ideaflow.model.IdeaFlowModel
import com.ideaflow.model.Task
import org.joda.time.DateTime

class IdeaFlowFileClient<T> implements IIdeaFlowClient<T> {

    private IDEService<T> ideService
	private DSLTimelineSerializer serializer = new DSLTimelineSerializer()

	IdeaFlowFileClient(IDEService<T> ideService) {
		this.ideService = ideService
	}

	@Override
    IdeaFlowModel readModel(T context, Task task) {
        IdeaFlowModel ideaFlowModel

        File file = getFile(task)

        if (ideService.fileExists(context, file)) {
            println("Resuming existing IdeaFlow: ${file.absolutePath}")

            String xml = ideService.readFile(context, file)

            ideaFlowModel = serializer.deserialize(task, xml)
        } else {
            println("Creating new IdeaFlow: ${file.absolutePath}")

            ideService.createNewFile(context, file, "")

            ideaFlowModel = new IdeaFlowModel(task, new DateTime())
        }

        ideaFlowModel.task = task

        return ideaFlowModel
    }

    private File getFile(Task task) {
        def file = new File(System.getProperty("user.home") + "/.ifm/" + task.taskId)

        return file.name.endsWith(".ifm") == false ?
                new File(file.absolutePath + ".ifm") :
                file
    }

    @Override
    void saveModel(T context, IdeaFlowModel ideaFlowModel) {
        String xml = serializer.serialize(ideaFlowModel)
        ideService.writeFile(context, getFile(ideaFlowModel.task), xml)
    }
}
