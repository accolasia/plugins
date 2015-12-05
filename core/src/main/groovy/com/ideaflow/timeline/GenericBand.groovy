package com.ideaflow.timeline

import com.ideaflow.model.entry.BandEnd
import com.ideaflow.model.entry.BandStart
import com.ideaflow.model.BandType
import org.joda.time.DateTime

class GenericBand extends AbstractTimeBand implements Entity {

	BandStart bandStart
	BandEnd bandEnd

	protected void setActivityStartCreated(DateTime created) {
		bandStart.created = created
	}

	protected void setActivityEndCreated(DateTime created) {
		bandEnd.created = created
	}

	boolean isLinkedToPreviousBand() {
		bandStart.isLinkedToPreviousBand
	}

	BandType getBandType() {
		bandStart.type
	}

	String getId() {
		bandStart.getId()
	}

	String getComment() {
		bandStart.comment
	}

	String toString() {
		"GenericBand: $id, $bandType, $comment"
	}
}