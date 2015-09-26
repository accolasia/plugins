package com.ideaflow.dsl

import com.ideaflow.model.Task
import com.ideaflow.model.entry.BandEnd
import com.ideaflow.model.entry.BandStart
import com.ideaflow.model.entry.Conflict
import com.ideaflow.model.entry.EditorActivity
import com.ideaflow.model.IdeaFlowModel
import com.ideaflow.model.entry.Idle
import com.ideaflow.model.entry.ModelEntry
import com.ideaflow.model.entry.Note
import com.ideaflow.model.entry.Resolution
import com.ideaflow.model.entry.StateChange
import com.ideaflow.model.StateChangeType
import org.joda.time.DateTime
import spock.lang.Specification
import test.support.FixtureSupport

@Mixin(FixtureSupport)
class IdeaFlowReaderWriterTest extends Specification {

	private StringWriter stringWriter = new StringWriter()
	private IdeaFlowWriter writer = new IdeaFlowWriter(stringWriter)



    void testReadWriteSymmetryWithData() {
        given:
		DateTime createDate = new DateTime(NOW)
		EditorActivity modifiedEditorActivity = createEditorActivity(FILE)
		modifiedEditorActivity.modified = true
		EditorActivity unmodifiedEditorActivity = createEditorActivity(FILE)
		Note note = createNote("it's a happy note!")
		StateChange event = createStateChange(StateChangeType.startIdeaFlowRecording)
		Conflict conflict = createConflict()
		Resolution resolution = createResolution()
		BandStart bandStart = createBandStart()
        bandStart.isLinkedToPreviousBand = true
		BandEnd bandEnd = createBandEnd()
	    Idle idle = createIdle()

        when:
		writer.writeInitialization(createDate)
		writer.write(modifiedEditorActivity)
		writer.write(unmodifiedEditorActivity)
		writer.write(note)
		writer.write(conflict)
		writer.write(resolution)
		writer.write(event)
		writer.write(bandStart)
		writer.write(bandEnd)
	    writer.write(idle)
	    println stringWriter.toString()
        then:
		IdeaFlowModel model = readModelAndClearIds()
		assert model.created == createDate
		assert model.entryList.remove(0) == modifiedEditorActivity
		assert model.entryList.remove(0) == unmodifiedEditorActivity
		assert model.entryList.remove(0) == note
		assert model.entryList.remove(0) == conflict
		assert model.entryList.remove(0) == resolution
		assert model.entryList.remove(0) == event
		assert model.entryList.remove(0) == bandStart
		assert model.entryList.remove(0) == bandEnd
        assert model.entryList.remove(0) == idle
		assert model.size() == 0
	}

	private IdeaFlowModel readModelAndClearIds() {
		IdeaFlowModel model = new IdeaFlowReader().readModel(new Task(taskId: 'test'), stringWriter.toString())

		model.entryList.each { ModelEntry entity ->
			assert entity.id != null
			entity.id = null
		}
		model
	}

    void testReadWriteSymmetry_EnsureNewlyAddedModelEntitySubTypesAreSerializable() {
        given:
		List<ModelEntry> subTypeInstances = getInitializedModelEntitySubClassInstances()

        when:
		writer.writeInitialization(new DateTime(NOW))
		subTypeInstances.each { ModelEntry entity ->
			try {
				writer.write(entity)
			} catch (MissingMethodException ex) {
				throw new RuntimeException("Possible reason for failure: if a subtype of ${ModelEntry.simpleName} has just been added, " +
						"ensure ${IdeaFlowWriter.simpleName} declares method write(${entity.class.simpleName})", ex)
			}
		}
		IdeaFlowModel model
		try {
			model = new IdeaFlowReader().readModel(new Task(taskId: 'test'), stringWriter.toString())
		} catch (MissingMethodException ex) {
			throw new RuntimeException("Possible reason for failure: if a subtype of ${ModelEntry.simpleName} has just been added, " +
					"ensure ${IdeaFlowReader.simpleName} declares appropriate read(<subtype>) method", ex)
		}

        then:
		model.entryList.each { ModelEntry entity ->
			assert entity.id != null
			entity.id = null
		}
		for (int i = 0; i < subTypeInstances.size(); i++) {
			assert subTypeInstances[i] == model.entryList[i]
		}
		assert subTypeInstances.size() == model.size()
	}

	private List<ModelEntry> getInitializedModelEntitySubClassInstances() {
		List<ModelEntry> subTypeInstances = getModelEntitySubClassInstances()
		DateTime createDate = new DateTime(NOW)

		subTypeInstances.each { ModelEntry entity ->
			createDate = createDate.plusSeconds(1)
			entity.created = createDate
		}
		subTypeInstances
	}

    public void testBackslashInText_ShouldNotExplode() {
        given:
		Conflict conflict = createConflict()
		conflict.question = /What's up with this regex: \s+\S+\s+/

        when:
		writer.writeInitialization(new DateTime(NOW))
		writer.write(conflict)

        then:
		IdeaFlowModel model = readModelAndClearIds()
		assert model.entryList.remove(0) == conflict
		assert model.entryList.size() == 0
	}

    public void testQuoteAtStartOrEnd_ShouldNotExplode() {
        given:
		Note singleQuoteNote = createNote("'here' is a single 'quote'")
		Note doubleQuoteNote = createNote('"here" is a double "quote"')

        when:
		writer.writeInitialization(new DateTime(NOW))
		writer.write(singleQuoteNote)
		writer.write(doubleQuoteNote)

        then:
		IdeaFlowModel model = readModelAndClearIds()
		assert model.entryList.remove(0) == singleQuoteNote
		assert model.entryList.remove(0) == doubleQuoteNote
		assert model.entryList.size() == 0
	}

    public void testTripleQuotesInString_ShouldNotExpode() {
        given:
		Note tripleSingleQuoteNote = createNote("here is a '''triple''' quote")
		Note tripleDoubleQuoteNote = createNote('here is a """double""" quote')

        when:
		writer.writeInitialization(new DateTime(NOW))
		writer.write(tripleSingleQuoteNote)
		writer.write(tripleDoubleQuoteNote)

        then:
		IdeaFlowModel model = readModelAndClearIds()
		assert model.entryList.remove(0) == tripleSingleQuoteNote
		assert model.entryList.remove(0) == tripleDoubleQuoteNote
		assert model.entryList.size() == 0
	}

}
