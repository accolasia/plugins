package com.ideaflow.timeline

import com.ideaflow.model.BandEnd
import com.ideaflow.model.BandStart
import org.joda.time.DateTime

class GenericBand extends AbstractTimeBand {

	BandStart bandStart
	BandEnd bandEnd

	protected void setActivityStartCreated(DateTime created) {
		bandStart.created = created
	}

	protected void setActivityEndCreated(DateTime created) {
		bandEnd.created = created
	}

}