/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.drt.routingModule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.jbischoff.drt.config.DRTConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class StopBasedDRTRoutingModule implements RoutingModule {

	private final StageActivityTypes drtStageActivityType = new DRTStageActivityType();
	private final RoutingModule walkRouter;
	private final Map<Id<TransitStopFacility>,TransitStopFacility> stops;
	private final DRTConfigGroup drtconfig;
	private final double walkBeelineFactor;
	private final Network network;
	private final Scenario scenario;
	/**
	 * 
	 */
	@Inject
	public StopBasedDRTRoutingModule(@Named(TransportMode.walk) RoutingModule walkRouter, @Named(DRTConfigGroup.DRTMODE) TransitSchedule transitSchedule, Config config, Scenario scenario) {
			transitSchedule.getFacilities();
			this.walkRouter = walkRouter;
			this.stops = transitSchedule.getFacilities();
			this.drtconfig = (DRTConfigGroup) config.getModules().get(DRTConfigGroup.GROUPNAME);
			this.walkBeelineFactor = config.plansCalcRoute().getModeRoutingParams().get(TransportMode.walk).getBeelineDistanceFactor();
			this.network = scenario.getNetwork();
			this.scenario = scenario;
	}
	
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		List<PlanElement> legList = new ArrayList<>();
		TransitStopFacility accessFacility = findAccessFacility(fromFacility, toFacility);
		TransitStopFacility egressFacility = findEgressFacility(accessFacility, toFacility);
		legList.addAll(walkRouter.calcRoute(fromFacility, accessFacility, departureTime, person));
		Leg walkLeg = (Leg) legList.get(0);
		Activity drtInt1 = scenario.getPopulation().getFactory().createActivityFromCoord(DRTStageActivityType.DRTSTAGEACTIVITY, accessFacility.getCoord());
		drtInt1.setMaximumDuration(1);
		drtInt1.setLinkId(accessFacility.getLinkId());
		legList.add(drtInt1);
		
		Route drtRoute = new GenericRouteImpl(accessFacility.getLinkId(), egressFacility.getLinkId());
	    drtRoute.setDistance(Double.NaN);
	    drtRoute.setTravelTime(Double.NaN);

	    Leg drtLeg = PopulationUtils.createLeg(DRTConfigGroup.DRTMODE);
        drtLeg.setDepartureTime(departureTime+walkLeg.getTravelTime()+1);
        drtLeg.setTravelTime(Double.NaN);
        drtLeg.setRoute(drtRoute);
		
        legList.add(drtLeg);
        
		Activity drtInt2 = scenario.getPopulation().getFactory().createActivityFromCoord(DRTStageActivityType.DRTSTAGEACTIVITY, egressFacility.getCoord());
		drtInt2.setMaximumDuration(1);
		drtInt2.setLinkId(egressFacility.getLinkId());
		legList.add(drtInt2);
		legList.addAll(walkRouter.calcRoute(egressFacility, toFacility, departureTime, person));
		return legList;
	}


	/**
	 * @param fromFacility
	 * @param toFacility
	 * @return
	 */
	private TransitStopFacility findAccessFacility(Facility<?> fromFacility, Facility<?> toFacility) {
		Coord fromCoord = getFacilityCoord(fromFacility);
		Coord toCoord = getFacilityCoord(toFacility);
		Set<TransitStopFacility> stopCandidates = findStopCoordinates(fromCoord);
		TransitStopFacility accessFacility = null;
		double bestHeading = Double.MAX_VALUE;
		for (TransitStopFacility stop : stopCandidates)
		{
			Link stopLink = network.getLinks().get(stop.getLinkId());
			double [] stopLinkVector = getVector(stopLink.getFromNode().getCoord(), stopLink.getToNode().getCoord());
			double [] destinationVector = getVector(stopLink.getFromNode().getCoord(),toCoord);
			double heading = calcHeading(stopLinkVector,destinationVector);
			if (heading<bestHeading) {
				accessFacility = stop;
				bestHeading = heading;
			}
		}
		return accessFacility;
	}
	
	private TransitStopFacility findEgressFacility(TransitStopFacility fromStopFacility, Facility<?> toFacility) {
		Coord fromCoord = fromStopFacility.getCoord();
		Coord toCoord = getFacilityCoord(toFacility);
		Set<TransitStopFacility> stopCandidates = findStopCoordinates(toCoord);
		
		TransitStopFacility egressFacility = null;
		double bestHeading = Double.MAX_VALUE;
		for (TransitStopFacility stop : stopCandidates)
		{
			Link stopLink = network.getLinks().get(stop.getLinkId());
			double [] stopLinkVector = getVector(stopLink.getFromNode().getCoord(), stopLink.getToNode().getCoord());
			double [] originVector = getVector(fromCoord,stopLink.getToNode().getCoord());
			double heading = calcHeading(stopLinkVector,originVector);
			if (heading<bestHeading) {
				egressFacility = stop;
				bestHeading = heading;
			}
		}
		return egressFacility;
	}


	/**
	 * @param stopLinkVector
	 * @param destinationVector
	 * @return
	 */
	private double calcHeading(double[] stopLinkVector, double[] destinationVector) {
		
		return Math.acos((stopLinkVector[0]*destinationVector[0]+stopLinkVector[1]*destinationVector[1])/(Math.sqrt(stopLinkVector[0]*stopLinkVector[0]+stopLinkVector[1]*stopLinkVector[1])*Math.sqrt(destinationVector[0]*destinationVector[0]+destinationVector[1]*destinationVector[1])));
	}


	private Set<TransitStopFacility> findStopCoordinates(Coord coord) {
		Set<TransitStopFacility> stopCandidates = new HashSet<>();
		for (TransitStopFacility stop : this.stops.values()){
			double distance = walkBeelineFactor*CoordUtils.calcEuclideanDistance(coord, stop.getCoord());
			if (distance<=drtconfig.getMaximumWalkDistance()){
				stopCandidates.add(stop);
			}
			
		}
		return stopCandidates;
	}


	@Override
	public StageActivityTypes getStageActivityTypes() {
		return drtStageActivityType;
	}

	Coord getFacilityCoord(Facility<?> facility){
		Coord coord = facility.getCoord();
		if (coord == null)
		{
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			if (coord == null)
				throw new RuntimeException("From facility has neither coordinates nor link Id. Should not happen.");
		}
		return coord;
	}
	
	double[] getVector(Coord from, Coord to){
		double[] vector = new double[2];
		vector[0] = to.getX()-from.getX();
		vector[1] = to.getY()-from.getY();
		return vector;
	}
	
}

