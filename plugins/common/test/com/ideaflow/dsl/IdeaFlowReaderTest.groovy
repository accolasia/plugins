package com.ideaflow.dsl

import com.ideaflow.model.*
import test.support.FixtureSupport

@Mixin(FixtureSupport)
class IdeaFlowReaderTest extends GroovyTestCase {

	private StringWriter stringWriter
	private IdeaFlowWriter writer

	void setUp() {
		stringWriter = new StringWriter()
		writer = new IdeaFlowWriter(stringWriter)
	}

	void testReadModel_ShouldReadContentWrittenByWriter() {
		Date createDate = new Date(NOW)
		Interval interval = createInterval(FILE, NOW)
		Note note = createNote("it's a happy note!", NOW)
		Event event = createEvent(EventType.open, NOW)
		Conflict conflict = createConflict(NOW)
		Resolution resolution = createResolution(NOW)

		writer.writeInitialization(createDate)
		writer.write(interval)
		writer.write(note)
		writer.write(conflict)
		writer.write(resolution)
		writer.write(event)
		IdeaFlowModel model = new IdeaFlowReader().readModel(stringWriter.toString())

		assert model.created == createDate
		assert model.entityList[0] == interval
		assert model.entityList[1] == note
		assert model.entityList[2] == conflict
		assert model.entityList[3] == resolution
		assert model.entityList[4] == event
	}

}
