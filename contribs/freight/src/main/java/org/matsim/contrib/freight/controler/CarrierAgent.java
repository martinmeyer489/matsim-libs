package org.matsim.contrib.freight.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.Pickup;
import org.matsim.contrib.freight.carrier.Tour.TourActivity;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * This keeps track of the carrier during simulation.
 * 
 * @author mzilske, sschroeder
 *
 */
class CarrierAgent implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler,
					      LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
					      PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler
{

	/**
	 * This keeps track of a scheduledTour during simulation and can thus be seen as the driver of the vehicle that runs the tour.
	 * 
	 * <p>In addition, the driver knows which planElement is associated to a shipment and service, respectively.
	 * 
	 * @author mzilske, sschroeder
	 *
	 */
	class CarrierDriverAgent {

		private Leg currentLeg;

		private Activity currentActivity;

		private List<Id<Link>> currentRoute;

		private final Id<Person> driverId;

		private final ScheduledTour scheduledTour;

		private int activityCounter = 0;

		private CarrierDriverAgent(Id<Person> driverId, ScheduledTour tour) {
			this.driverId = driverId;
			this.scheduledTour = tour;
			new HashMap<Integer, CarrierShipment>();
		}

		private void handleEvent( PersonArrivalEvent event ) {
			currentLeg.setTravelTime( event.getTime() - currentLeg.getDepartureTime().seconds());
			double travelTime = currentLeg.getDepartureTime().seconds()
					+ currentLeg.getTravelTime().seconds() - currentLeg.getDepartureTime().seconds();
			currentLeg.setTravelTime(travelTime);
			if (currentRoute.size() > 1) {
				NetworkRoute networkRoute = RouteUtils.createNetworkRoute( currentRoute );
				networkRoute.setTravelTime(travelTime);
				networkRoute.setVehicleId(getVehicle().getId() );
				currentLeg.setRoute(networkRoute);
				currentRoute = null;
			} else {
				Id<Link> startLink;
				if(currentRoute.size() != 0){
					startLink = currentRoute.get(0);
				}
				else{
					startLink = event.getLinkId();
				}
				Route genericRoute = RouteUtils.createGenericRouteImpl(startLink, event.getLinkId());    
				genericRoute.setDistance(0.0);
				currentLeg.setRoute(genericRoute);
			}
			scoringFunction.handleLeg(currentLeg);
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void handleEvent( PersonDepartureEvent event ) {
			Leg leg = PopulationUtils.createLeg(event.getLegMode());
			leg.setDepartureTime(event.getTime());
			currentLeg = leg;
			currentRoute = new ArrayList<>();
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void handleEvent( LinkEnterEvent event ) {
			scoringFunction.handleEvent(new LinkEnterEvent(event.getTime(),getVehicle().getId(),event.getLinkId()) );
			currentRoute.add(event.getLinkId());
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void handleEvent( LinkLeaveEvent event ) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void handleEvent( ActivityEndEvent event ) {
			if (currentActivity == null) {
				Activity firstActivity = PopulationUtils.createActivityFromLinkId(event.getActType(), event.getLinkId());
				firstActivity.setFacilityId(event.getFacilityId());
				currentActivity = firstActivity;
			}
			currentActivity.setEndTime(event.getTime());
			scoringFunction.handleActivity(currentActivity);
			activityFinished(event);
		}

		private TourActivity getTourActivity() {
			return (TourActivity) this.scheduledTour.getTour().getTourElements().get(activityCounter);
		}

		private void handleEvent( ActivityStartEvent event ) {
			Activity activity = PopulationUtils.createActivityFromLinkId(event.getActType(), event.getLinkId()); 
			activity.setFacilityId(event.getFacilityId());
			activity.setStartTime(event.getTime());
			if(event.getActType().equals(FreightConstants.END)){
				activity.setEndTimeUndefined();
				scoringFunction.handleActivity(activity);
			}
			else{
				TourActivity tourActivity = getTourActivity();
				if (!activity.getLinkId().toString().equals(tourActivity.getLocation().toString()))
					throw new AssertionError("linkId of activity is not equal to linkId of tourActivity. This must not be.");
				currentActivity = new FreightActivity(activity, tourActivity.getTimeWindow());
				notifyEventHappened( event, currentActivity, scheduledTour, driverId, activityCounter );
			}
		}

		private void handleEvent( VehicleLeavesTrafficEvent event ) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void handleEvent( PersonEntersVehicleEvent event ) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void handleEvent( VehicleEntersTrafficEvent event ) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void handleEvent( PersonLeavesVehicleEvent event ) {
			notifyEventHappened(event, null, scheduledTour, driverId, activityCounter);
		}

		private void activityFinished( ActivityEndEvent event ) {
			String activityType = event.getActType();
			notifyEventHappened(event, currentActivity, scheduledTour, driverId, activityCounter);
			if(FreightConstants.START.equals(activityType) ) {
				activityCounter += 1;
				return;
			}
			if( FreightConstants.END.equals(activityType)) return;
			if (FreightConstants.PICKUP.equals(activityType)) {
				activityCounter += 2;
			} else if (FreightConstants.DELIVERY.equals(activityType)) {
				activityCounter += 2;
			}
			else{
				activityCounter += 2;
			}
		}

		CarrierVehicle getVehicle() {
			return scheduledTour.getVehicle();
		}

		TourElement getPlannedTourElement(int elementIndex){
			int index = elementIndex-1;
			int elementsSize = scheduledTour.getTour().getTourElements().size();
			if(index < 0) return scheduledTour.getTour().getStart();
			else if(index == elementsSize) return scheduledTour.getTour().getEnd();
			else if(index < elementsSize){
				return scheduledTour.getTour().getTourElements().get(index);
			}
			else throw new IllegalStateException("index out of bounds");
		}
	}

	private final Id<Carrier> id;

	private LSPCarrierTracker lspTracker;
	private final Carrier carrier;

	private final Collection<Id<Person>> driverIds = new ArrayList<>();

	private int nextId = 0;

	private final Map<Id<Person>, CarrierDriverAgent> carrierDriverAgents = new HashMap<>();

	private ScoringFunction scoringFunction;

	private final Vehicle2DriverEventHandler vehicle2DriverEventHandler;

	CarrierAgent( Carrier carrier, ScoringFunction carrierScoringFunction, Vehicle2DriverEventHandler vehicle2DriverEventHandler ) {
		this.carrier = carrier;
		this.id = carrier.getId();
		Gbl.assertNotNull(carrierScoringFunction); // scoringFunctionFactory is null. this must not be.
		this.scoringFunction = carrierScoringFunction;
		this.vehicle2DriverEventHandler = vehicle2DriverEventHandler;
	}

	public CarrierAgent( LSPCarrierTracker lspCarrierTracker, Carrier carrier, Vehicle2DriverEventHandler vehicle2DriverEventHandler ){
		lspTracker = lspCarrierTracker;
		this.carrier = carrier;
		this.vehicle2DriverEventHandler = vehicle2DriverEventHandler;
		this.id = carrier.getId();
	}


	public Id<Carrier> getId() {
		return id;
	}

	/**
	 * Returns a list of plans created on the basis of the carrier's plan.
	 * 
	 * <p>A carrier plan consists usually of many tours (activity chains). Each plan in the returned list represents a carrier tour.
	 *  
	 * @return list of plans
	 * @see Plan, CarrierPlan
	 */
	List<Plan> createFreightDriverPlans() {
		clear();
		System.out.flush();
		System.err.flush() ;
		List<Plan> routes = new ArrayList<>();
		if (carrier.getSelectedPlan() == null) {
			return routes;
		}
		for (ScheduledTour scheduledTour : carrier.getSelectedPlan().getScheduledTours()) {
			Id<Person> driverId = createDriverId(scheduledTour.getVehicle());
			CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
			Person driverPerson = createDriverPerson(driverId);
			Vehicle vehicle = createVehicle(driverPerson,carrierVehicle);
			CarrierDriverAgent carrierDriverAgent = new CarrierDriverAgent(driverId, scheduledTour);
			Plan plan = PopulationUtils.createPlan();
			Activity startActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.START, scheduledTour.getVehicle().getLocation());
			startActivity.setEndTime(scheduledTour.getDeparture());
			plan.addActivity(startActivity);
			for (TourElement tourElement : scheduledTour.getTour().getTourElements()) {				
				if (tourElement instanceof org.matsim.contrib.freight.carrier.Tour.Leg) {
					org.matsim.contrib.freight.carrier.Tour.Leg tourLeg = (org.matsim.contrib.freight.carrier.Tour.Leg) tourElement;
					Route route = tourLeg.getRoute();
					if(route == null) throw new IllegalStateException("missing route for carrier " + this.getId());
					Leg leg = PopulationUtils.createLeg(TransportMode.car);
					leg.setRoute(route);
					leg.setDepartureTime(tourLeg.getExpectedDepartureTime());
					leg.setTravelTime(tourLeg.getExpectedTransportTime());
					leg.setTravelTime( tourLeg.getExpectedDepartureTime() + tourLeg.getExpectedTransportTime() - leg.getDepartureTime()
							.seconds());
					plan.addLeg(leg);
				} else if (tourElement instanceof TourActivity) {
					TourActivity act = (TourActivity) tourElement;
					Activity tourElementActivity = PopulationUtils.createActivityFromLinkId(act.getActivityType(), act.getLocation());					
					double duration = act.getDuration() ;
					tourElementActivity.setMaximumDuration(duration); // "maximum" has become a bit of a misnomer ...
					plan.addActivity(tourElementActivity);
				}
			}
			Activity endActivity = PopulationUtils.createActivityFromLinkId(FreightConstants.END, scheduledTour.getVehicle().getLocation());
			plan.addActivity(endActivity);
			driverPerson.addPlan(plan);
			plan.setPerson(driverPerson);
			FreightControlerUtils.putVehicle( plan, vehicle );
			routes.add(plan);
			carrierDriverAgents.put(driverId, carrierDriverAgent);
		}
		return routes;
	}

	private Vehicle createVehicle(Person driverPerson, CarrierVehicle carrierVehicle) {
		Gbl.assertNotNull(driverPerson);
		Gbl.assertNotNull( carrierVehicle.getType() );
		return VehicleUtils.getFactory().createVehicle(Id.create(driverPerson.getId(), Vehicle.class), carrierVehicle.getType() );
	}

	private void clear() {
		carrierDriverAgents.clear();
		driverIds.clear();
		nextId = 0;
	}

	Collection<Id<Person>> getDriverIds() {
		return Collections.unmodifiableCollection(driverIds);
	}

	private Person createDriverPerson(Id<Person> driverId) {
		return PopulationUtils.getFactory().createPerson( driverId );
	}

	private Id<Person> createDriverId(CarrierVehicle carrierVehicle) {
		Id<Person> id = Id.create("freight_" + carrier.getId() + "_veh_" + carrierVehicle.getId() + "_" + nextId, Person.class );
		driverIds.add(id);
		++nextId;
		return id;
	}

	private void notifyEventHappened( Event event, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId, int activityCounter ) {
		if ( scoringFunction==null ) {
			lspTracker.notifyEventHappened(event, carrier, activity, scheduledTour, driverId, activityCounter);}
	}

	void scoreSelectedPlan() {
		if (carrier.getSelectedPlan() == null) {
			return;
		}
		scoringFunction.finish();
		carrier.getSelectedPlan().setScore(scoringFunction.getScore());
	}
	@Override public void handleEvent(PersonArrivalEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	@Override public void handleEvent(LinkEnterEvent event) {
		getDriver(vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId())).handleEvent(event);
	}
	@Override public void handleEvent(LinkLeaveEvent event) {
		getDriver(vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId())).handleEvent(event);
	}
	@Override public void handleEvent(PersonDepartureEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	@Override public void handleEvent(ActivityEndEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	@Override public void handleEvent(ActivityStartEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	@Override public void handleEvent(VehicleLeavesTrafficEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	@Override public void handleEvent(PersonEntersVehicleEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	@Override public void handleEvent(VehicleEntersTrafficEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	@Override public void handleEvent(PersonLeavesVehicleEvent event) {
		getDriver(event.getPersonId()).handleEvent(event);
	}
	CarrierDriverAgent getDriver(Id<Person> driverId){
		return carrierDriverAgents.get(driverId);
	}
}
