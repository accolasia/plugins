package com.ideaflow.dsl

import com.ideaflow.model.Conflict
import com.ideaflow.model.ModelEntity
import com.ideaflow.model.StateChange
import com.ideaflow.model.EditorActivity
import com.ideaflow.model.Note
import com.ideaflow.model.Resolution

import java.text.SimpleDateFormat

class IdeaFlowWriter {

	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

	private BufferedWriter writer
	private SimpleDateFormat dateFormat

	IdeaFlowWriter(Writer writer) {
		this.writer = createBufferedWriter(writer)
		this.dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT)
	}

	private BufferedWriter createBufferedWriter(Writer writer) {
		writer instanceof BufferedWriter ? writer as BufferedWriter : new BufferedWriter(writer)
	}

	void close() {
		writer.close()
	}

	void writeInitialization(Date created) {
		writer.print "initialize ("
		writer.print "dateFormat: \"${DEFAULT_DATE_FORMAT}\", "
		writer.print "created: '${dateFormat.format(created)}', "
		writer.println ")"
		writer.flush()
	}

	void write(StateChange event) {
		writeItem('stateChange', event, ['created', 'type'])
	}

	void write(Note note) {
		writeItem('note', note, ['created', 'comment'])
	}

	void write(Conflict conflict) {
		writeItem('conflict', conflict, ['created', 'mistakeType', 'question', 'cause', 'notes'])
	}

	void write(Resolution resolution) {
		writeItem('resolution', resolution, ['created', 'answer'])
	}

	void write(EditorActivity interval) {
		writeItem('interval', interval, ['created', 'name', 'duration'])
	}

	private void writeItem(String name, ModelEntity entity, List orderedKeyList) {
		Map properties = getPropertiesToWrite(entity)
		assertActualPropertyKeysMatchDeclaredKeyList(entity, properties, orderedKeyList)

		writer.print "${name} ("
		orderedKeyList.each { String key ->
			writeItemEntry(key, properties[key])
		}
		writer.println ")"
		writer.flush()

	}

	private void writeItemEntry(String key, value) {
		if (value != null) {
			if (value instanceof String) {
				writer.print "${key}: '''${value}''', "
			} else if (value instanceof Number) {
				writer.print "${key}: ${value}, "
			} else {
				if (value instanceof Date) {
					value = dateFormat.format(value)
				}
				writer.print "${key}: '${value}', "
			}
		}
	}

	private void assertActualPropertyKeysMatchDeclaredKeyList(ModelEntity entity, Map map, List declaredKeylist) {
		String simpleName = entity.class.simpleName
		List actualKeyList = map.keySet().asList()

		List additionalProperties = actualKeyList - declaredKeylist
		if (additionalProperties) {
			throw new RuntimeException("Object ${simpleName} declares unknown properties=${additionalProperties}.  Add properties to key list of method IdeaFlowWriter:write(${simpleName})")
		}

		List missingProperties = declaredKeylist - actualKeyList
		if (missingProperties) {
			throw new RuntimeException("IdeaFlowWriter:write(${simpleName}) is configured to write out properties=${missingProperties} which are not declared in corresponding class.")
		}
	}

	private Map getPropertiesToWrite(ModelEntity entity) {
		Map properties = entity.getProperties()
		properties.remove('id')
		properties.remove('class')
		properties
	}

}
