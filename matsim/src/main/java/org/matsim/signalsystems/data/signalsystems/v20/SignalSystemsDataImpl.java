/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemsImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.signalsystems.data.signalsystems.v20;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;



/**
 * @author dgrether
 *
 */
public class SignalSystemsDataImpl implements SignalSystemsData {

	private SignalSystemsDataFactory factory = new SignalSystemsDataFactoryImpl();
	
	private Map<Id, SignalSystemData> signalSystemData = new HashMap<Id, SignalSystemData>();
	
	@Override
	public SignalSystemsDataFactory getFactory() {
		return factory;
	}

	@Override
	public Map<Id, SignalSystemData> getSignalSystemData() {
		return this.signalSystemData;
	}

	@Override
	public void addSignalSystemData(SignalSystemData signalSystemData) {
		this.signalSystemData.put(signalSystemData.getId(), signalSystemData);
	}
	
	@Override
	public void setFactory(SignalSystemsDataFactory factory) {
		this.factory = factory;
	}
}