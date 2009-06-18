package playground.mmoyo.TransitSimulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.api.network.Network;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.transitSchedule.*;
import playground.mmoyo.PTCase2.PTTimeTable2;
import playground.mmoyo.PTRouter.PTLine;

/**
 * From PTtimeTable to transitShcedule converter 
 */
public class ToTransitScheduleConverter {
	final TransitSchedule transitSchedule = new TransitSchedule();
	
	public ToTransitScheduleConverter (){
				
	}
	
	/**reads a PTTimetable and writes a transitFile*/
	public void createTransitSchedule(final PTTimeTable2 ptTimeTable, final Network ptNet, String outTransitFile) {
		TransitLine transitLine;
		/*
		for (Node node:  ptNet.getNodes().values()){
			this.transitSchedule.addStopFacility(new TransitStopFacility(node.getId(), node.getCoord()));
		}
		*/
		for (PTLine ptLine: ptTimeTable.getPtLineList()){
			Id ptLineId = ptLine.getId();
			boolean transitLineExists = this.transitSchedule.getTransitLines().containsKey(ptLineId); 
			if (!transitLineExists){
				transitLine = new TransitLine(ptLineId);
			}else{
				transitLine = transitSchedule.getTransitLines().get(ptLineId);
			}
			
			int x=0;
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			for (Id idNode : ptLine.getNodeRoute()){				
				if (!transitSchedule.getFacilities().containsKey(idNode)){
					TransitStopFacility transitStopFacility =  new 	TransitStopFacility(idNode, ptNet.getNode(idNode).getCoord());
					transitSchedule.addStopFacility(transitStopFacility);	
				}
				double min = ptLine.getMinutes().get(x++).doubleValue();  
				double arrivalDelay = min*60;
				double departureDelay = min*60;
				transitRouteStops.add(new TransitRouteStop(transitSchedule.getFacilities().get(idNode), arrivalDelay, departureDelay));
			}
			
			Id idTransitRoute = new IdImpl(ptLineId + "-" +  ptLine.getDirection());
			NetworkRoute networkRoute= null; //new NodeNetworkRoute(null, null);
			TransitRoute transitRoute = new  TransitRoute(idTransitRoute, networkRoute, transitRouteStops, TransportMode.pt);
			transitRoute.setTransportMode(ptLine.getTransportMode());
			x=0;
			for (String strDeparture : ptLine.getDepartures()){
				Id idDeparture = new IdImpl(x);
				//double dblDeparture =x;			// fictional departures
				double dblDeparture=  Time.parseTime(strDeparture);
				Departure departure = new Departure (idDeparture, dblDeparture);
				transitRoute.addDeparture(departure);
				x++;
			}
			transitRoute.setDescription(ptLine.getId() + " " + ptLine.getDirection());
			transitLine.addRoute(transitRoute);
			if (!transitLineExists){this.transitSchedule.addTransitLine(transitLine);}
		}

		TransitScheduleWriterV1 transitScheduleWriterV1 = new TransitScheduleWriterV1 (this.transitSchedule);
		try{
			transitScheduleWriterV1.write(outTransitFile);
		} catch (IOException ex) {
			System.out.println(this + ex.getMessage());
		}
		System.out.println("done.");
	}
	
	public void createFacilities(Network net){
	
	}
	
	
	
	
}