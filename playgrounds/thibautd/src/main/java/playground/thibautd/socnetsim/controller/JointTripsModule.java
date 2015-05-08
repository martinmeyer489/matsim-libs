/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsAndJointVehiclesModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.controller;

import org.matsim.core.controler.AbstractModule;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.router.JointPlanRouterFactory;

import com.google.inject.Scopes;

/**
 * @author thibautd
 */
public class JointTripsModule extends AbstractModule {

	@Override
	public void install() {
		bind( PlanRoutingAlgorithmFactory.class ).to( JointPlanRouterFactory.class ).in( Scopes.SINGLETON );
	}
}

