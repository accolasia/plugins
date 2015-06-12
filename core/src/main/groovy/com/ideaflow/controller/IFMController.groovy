package com.ideaflow.controller

import com.ideaflow.dsl.IdeaFlowReader
import com.ideaflow.dsl.client.IIdeaFlowClient
import com.ideaflow.event.EventToEditorActivityHandler
import com.ideaflow.model.BandEnd
import com.ideaflow.model.BandStart
import com.ideaflow.model.BandType
import com.ideaflow.model.Conflict
import com.ideaflow.model.IdeaFlowModel
import com.ideaflow.model.ModelEntry
import com.ideaflow.model.Note
import com.ideaflow.model.Resolution
import com.ideaflow.model.StateChange
import com.ideaflow.model.StateChangeType
import com.ideaflow.model.Task
import org.joda.time.DateTime

class IFMController<T> {

	private IdeaFlowModel ideaFlowModel
	private EventToEditorActivityHandler eventToIntervalHandler
	private IDEService<T> ideService
	private IFMWorkingSet workingSet

	private IIdeaFlowClient client


	IFMController(IDEService<T> ideService) {
		this.ideService = ideService
		this.workingSet = new IFMWorkingSet()
	}

	void addWorkingSetListener(IFMWorkingSetListener workingSetListener) {
		workingSet.addWorkingSetListener(workingSetListener)
	}

	List<File> getWorkingSetFiles() {
		workingSet.getTasks()
	}

	void setWorkingSetFiles(List<File> files) {
		workingSet.setTasks(files)
	}

	IdeaFlowModel getActiveIdeaFlowModel() {
		ideaFlowModel?.file?.exists() ? ideaFlowModel : null
	}

	String promptForInput(T context, String title, String message) {
		ideService.promptForInput(context, title, message)
	}

	String getActiveIdeaFlowName() {
		activeIdeaFlowModel?.file?.name
	}

	boolean isIdeaFlowOpen() {
		activeIdeaFlowModel != null
	}

	Conflict getActiveConflict() {
		return (isIdeaFlowOpen() ? activeIdeaFlowModel.getActiveConflict() : null)
	}

	BandStart getActiveBandStart() {
		return (isIdeaFlowOpen() ? activeIdeaFlowModel.getActiveBandStart() : null)
	}

	boolean isOpenConflict() {
		getActiveConflict() != null
	}

	boolean isOpenBand() {
		getActiveBandStart() != null
	}

	void startConflict(T context, String question) {
		if (question) {
            boolean nested = isOpenBand()
			addModelEntry(context, new Conflict(question, nested))
		}
	}

	void endConflict(T context, String answer) {
		if (answer) {
			addModelEntry(context, new Resolution(answer))
		}
	}

	void startBand(T context, String comment, BandType bandType, boolean isLinkedToPreviousBand) {
		if (comment) {
			BandStart activeBandStart = getActiveBandStart()
			if (activeBandStart) {
				endBand(context, activeBandStart.type)
				isLinkedToPreviousBand = true
			}

			addModelEntry(context, new BandStart(bandType, comment, isLinkedToPreviousBand))
		}
	}

	void endBand(T context, BandType bandType) {
		if (bandType) {
			addModelEntry(context, new BandEnd(bandType))
		}
	}

	void addNote(T context, String comment) {
		if (comment) {
			addModelEntry(context, new Note(comment))
		}
	}

	/**
	 * @deprecated
	 * @param context
	 * @param file
	 */
	void newIdeaFlow(T context, File file) {
		suspendActiveIdeaFlow(context)

		file = addExtension(file)
		if (ideService.fileExists(context, file)) {
			println("Resuming existing IdeaFlow: ${file.absolutePath}")
			String xml = ideService.readFile(context, file)
			ideaFlowModel = new IdeaFlowReader().readModel(file, xml)
			ideaFlowModel.file = file
		} else {
			println("Creating new IdeaFlow: ${file.absolutePath}")
			ideService.createNewFile(context, file, "")
			ideaFlowModel = new IdeaFlowModel(file, new DateTime())
		}

		//workingSet.setActiveIfmFile(ideaFlowModel.file)
		eventToIntervalHandler = new EventToEditorActivityHandler(ideaFlowModel)
		addStateChange(context, StateChangeType.startIdeaFlowRecording)
		startFileEventForCurrentFile(context)
	}

	void newIdeaFlow(T context, Task task) {

		suspendActiveIdeaFlow(context)

		workingSet.setActiveTask(task)
		ideaFlowModel = new IdeaFlowModel(task, new DateTime())
		eventToIntervalHandler = new EventToEditorActivityHandler(ideaFlowModel)
		addStateChange(context, StateChangeType.startIdeaFlowRecording)
		startFileEventForCurrentFile(context)
	}

	void closeIdeaFlow(T context) {
		if (activeIdeaFlowModel) {
			suspendActiveIdeaFlow(context)

			workingSet.removeTask(ideaFlowModel.file)

			if (workingSet.isEmpty()) {
				ideaFlowModel = null
				eventToIntervalHandler = null
			} else {
				newIdeaFlow(context, workingSet.getTasks().first())
			}
		}
	}

	private void suspendActiveIdeaFlow(T context) {
		if (activeIdeaFlowModel) {
			endFileEvent(null)
			flush(context)
		}
	}

    void flushActiveEvent() {
        eventToIntervalHandler?.flushActiveEvent()
    }


    void startFileEvent(T context, String eventName) {
		eventToIntervalHandler?.startEvent(eventName)
		flush(context)
	}

	void fileModified(String eventName) {
		eventToIntervalHandler?.activeEventModified(eventName)
	}

	void startFileEventForCurrentFile(T context) {
		String fileName = ideService.getActiveFileSelection(context)
		startFileEvent(context, fileName)
	}

	void endFileEvent(String eventName) {
		eventToIntervalHandler?.endEvent(eventName)
	}

	void markActiveFileEventAsIdle(String comment) {
		eventToIntervalHandler?.endActiveEventAsIdle(comment)
	}

	void pause(T context) {
		println("Paused")
		endFileEvent(null)
		flush(context)
		activeIdeaFlowModel?.isPaused = true
	}

	void resume(T context) {
		println("Resumed")
		activeIdeaFlowModel?.isPaused = false
		startFileEventForCurrentFile(context)
	}

	boolean isPaused() {
		activeIdeaFlowModel?.isPaused
	}

	private File addExtension(File file) {
		File fileWithExtension = file
		if (file.name.endsWith(".ifm") == false) {
			fileWithExtension = new File(file.absolutePath + ".ifm")
		}
		return fileWithExtension
	}

	private void flush(T context) {
		if (activeIdeaFlowModel) {
			client.updateEntries(activeIdeaFlowModel.entryList)
			//String xml = new DSLTimelineSerializer().serialize(activeIdeaFlowModel)
			//ideService.writeFile(context, activeIdeaFlowModel.file, xml)
		}
	}

	private void addStateChange(T context, StateChangeType type) {
		addModelEntry(context, new StateChange(type))
	}

	private void addModelEntry(T context, ModelEntry event) {
		flushActiveEvent()
		activeIdeaFlowModel?.addModelEntry(event)
		flush(context)
		startFileEventForCurrentFile(context)
	}

}
