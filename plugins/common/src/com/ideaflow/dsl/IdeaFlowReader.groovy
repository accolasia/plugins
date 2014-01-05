package com.ideaflow.dsl

import com.ideaflow.model.Conflict
import com.ideaflow.model.Event
import com.ideaflow.model.IdeaFlowModel
import com.ideaflow.model.EditorActivity
import com.ideaflow.model.Note
import com.ideaflow.model.Resolution

import java.text.SimpleDateFormat

class IdeaFlowReader {

	IdeaFlowModel readModel(String dslContent) {
		IdeaFlowModelLoader loader = new IdeaFlowModelLoader()
		String wrappedDslContent = "ideaFlowModel {${dslContent}}"
		Script dslScript = new GroovyShell().parse(wrappedDslContent)

		dslScript.metaClass = createEMC(dslScript.class, { ExpandoMetaClass emc ->
			emc."ideaFlowModel" { Closure cl ->
				cl.delegate = loader
				cl.resolveStrategy = Closure.DELEGATE_FIRST

				cl()
			}
		})
		dslScript.run()
		loader.model
	}

	private ExpandoMetaClass createEMC(Class clazz, Closure cl) {
		ExpandoMetaClass emc = new ExpandoMetaClass(clazz, false)

		cl(emc)

		emc.initialize()
		return emc
	}

	private static class IdeaFlowModelLoader {

		IdeaFlowModel model
		private SimpleDateFormat dateFormat

		IdeaFlowModelLoader() {
			model = new IdeaFlowModel('', null)
		}

		def initialize(Map initializeMap) {
			String dateFormatString = initializeMap['dateFormat']
			dateFormat = new SimpleDateFormat(dateFormatString)

			String createdDateString = initializeMap['created'] as String
			model.created = dateFormat.parse(createdDateString)
		}

		def interval(Map intervalMap) {
			replaceCreatedStringWithDate(intervalMap)
			model.addModelEntity(new EditorActivity(intervalMap))
		}

		def event(Map eventMap) {
			replaceCreatedStringWithDate(eventMap)
			model.addModelEntity(new Event(eventMap))
		}

		def note(Map noteMap) {
			replaceCreatedStringWithDate(noteMap)
			model.addModelEntity(new Note(noteMap))
		}

		def conflict(Map eventMap) {
			replaceCreatedStringWithDate(eventMap)
			model.addModelEntity(new Conflict(eventMap))
		}

		def resolution(Map eventMap) {
			replaceCreatedStringWithDate(eventMap)
			model.addModelEntity(new Resolution(eventMap))
		}

		private void replaceCreatedStringWithDate(Map map) {
			String createdString = map['created']
			Date createdDate = dateFormat.parse(createdString)
			map['created'] = createdDate
		}

	}
}
