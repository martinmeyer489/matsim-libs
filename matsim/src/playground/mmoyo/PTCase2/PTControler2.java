package playground.mmoyo.PTCase2;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;

import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.xml.sax.SAXException;

import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitScheduleReaderV1;
import playground.mmoyo.TransitSimulation.LogicFactory;
import playground.mmoyo.TransitSimulation.LogicTransitOb;
import playground.mmoyo.TransitSimulation.ToTransitScheduleConverter;
import playground.mmoyo.TransitSimulation.TransitScheduleToPTTimeTableConverter;
import playground.mmoyo.Validators.NetValidator;
import playground.mmoyo.Validators.StationValidator;
import playground.mmoyo.input.PTLineAggregator;

/** 
 * Executable class to perform data input, validations and routing test according to timetable information
 */
public class PTControler2 {
    private static final String path = "../shared-svn/studies/schweiz-ivtch/pt-experimental/"; 
    //private static final String path = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5x5/";
    
	private static final String CONFIG =  path  + "config.xml";
	private static final String ZURICHPTN = path + "network.xml";
	private static final String ZURICHPTTIMETABLE = path + "PTTimetable.xml";  // PTTimeTable format will be not longer used 
	private static final String INPTNETFILE = path + "inptnetfile.xml";
	private static final String ZURICHPTPLANS = path + "plans.xml";
	private static final String OUTPUTPLANS = path + "output_plans.xml";
	private static final String TRANSITSCHEDULEFILE = path + "transitSchedule.xml";
	private static final String INPTNEWLINES = path + "TestCase/InPTDIVA.xml";
	private static final String DIVNODES = path + "TestCase/DivNodes.xml";
	private static final String PLAINNETWORK = path + "plainNetwork.xml";
	private static final String LOGICNETWORK = path + "logicNetwork.xml";
	private static final String LOGICTRANSITSCHEDULE = path + "logicTransitSchedule.xml";
	
	public static void main(String[] args){
		PTOb pt= new PTOb(CONFIG, ZURICHPTN, ZURICHPTTIMETABLE, ZURICHPTPLANS, OUTPUTPLANS); 
		
		int option =7;
		
		if (option>0){pt.readPTNet(ZURICHPTN);}
		switch (option){
		
			//Creates the network with no repeated nodes
			case -6:
				NetworkLayer tempNet= new NetworkLayer(new NetworkFactory());
	    		MatsimNetworkReader matsimNetworkReader2 = new MatsimNetworkReader(tempNet);
	    		matsimNetworkReader2.readFile(path + "network.xml");
	    		System.out.println("writing pt network...");
	    		new NetworkWriter(tempNet, path + "network2.xml").write();
	    		System.out.println("done.");
	    		break;
	    		
			case -5: 
				/**creates net from ZVV diva2Web database, periodic departures as timetable and route agents */  
				NetworkLayer net = new NetworkLayer();
				pt.setPtNetworkLayer(net);
				PTLineAggregator ptLineAggregator = new PTLineAggregator(INPTNEWLINES, pt.getPtNetworkLayer(), pt.getPtTimeTable());
				ptLineAggregator.addLines();
	
				pt.getPtNetworkFactory().createTransferLinks(pt.getPtNetworkLayer(), pt.getPtTimeTable());
				pt.getPtNetworkFactory().createDetachedTransfers(pt.getPtNetworkLayer(), 300);
				
				pt.writeNet(ZURICHPTN);
				pt.createRouter();
				PTActWriter ptActWriter1 = new PTActWriter(pt);
	    		ptActWriter1.findRouteForActivities();
				break;
			
			case-4:
	    		/** validates the id assignation for nodes of ZVV diva2Web database*/
				Network netDiv= new NetworkLayer(new NetworkFactory());
	    		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(netDiv);
	    		matsimNetworkReader.readFile(DIVNODES);
	    		
	    		StationValidator StationValidator = new StationValidator(pt.getPtNetworkLayer());
	    		StationValidator.validateIds(netDiv);
				break;

			case -3: 
				/** adds in memory extra PTlines from a separate file creating alias nodes but 
				does not write the new PTLines back to the TimeTable file but in a matsim network format. 
				PTTimeTables will be not longer used, TransitSchedule format instead*/
				pt.createPTNetWithTLinks(INPTNETFILE);
	    		pt.readPTNet(INPTNETFILE);
				ptLineAggregator = new PTLineAggregator(INPTNEWLINES, pt.getPtNetworkLayer(), pt.getPtTimeTable());
				ptLineAggregator.addLine();
	    		pt.writeNet(ZURICHPTN);
				break;
				
			case -2:  
				/** validates that the network does not have standard links with negative values. 
				 * This is possible when a PTL has two or more profiles and a intermediate station of a profile 
				 * has a greater value than the same stretch in other profile*/
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.writeNet(ZURICHPTN);
				pt.readPTNet(ZURICHPTN);
	    		NetValidator netValidator = new NetValidator(pt.getPtNetworkLayer(), pt.getPtTimeTable());
	    		netValidator.printNegativeTransferCosts(28800);
				break;
			
			case -1:
				/** Creates and writes detached transfer links. This is necessary before routing to get better results */  
				pt.createPTNetWithTLinks(INPTNETFILE);
				pt.getPtNetworkFactory().createDetachedTransfers(pt.getPtNetworkLayer(), 300);
				pt.writeNet(ZURICHPTN);
	    		pt.readPTNet(ZURICHPTN);
	    		PTActWriter ptActWriter2 = new PTActWriter(pt);
	    		ptActWriter2.findRouteForActivities();
				break;
			
		/************the positive cases need the network already loaded in memory*/
			case 0: 
	    		pt.createPTNetWithTLinks(INPTNETFILE);
	    		pt.writeNet("plainNetwork.xml");
	    		//Map<String, List<IdImpl>> intersecionMap = pt.getPtNetworkFactory().createIntersecionMap(pt.getPtTimeTable());
	    		//new StationValidator().validateStations(pt.getPtNetworkLayer(),intersecionMap);
	    		break;
	    	
			case 1:
				/**searches and shows a PT path between two coordinates or nodes */
				Coord coord1 = new CoordImpl(747420, 262794);   
				Coord coord2 = new CoordImpl(685862, 254136);
				Node nodeA = pt.getPtNetworkLayer().getNode("_8506000");
				Node nodeB = pt.getPtNetworkLayer().getNode("_8503309");
				//Path path2 = pt.getPtRouter2().findRoute(nodeA, nodeB, 45386);
				Path path2 = pt.getPtRouter2().findPTPath (coord1, coord2, 24372, 300);
				System.out.println(path2.links.size());
				for (Link l : path2.links){
					System.out.println(l.getId()+ ": " + l.getFromNode().getId() + " " + l.getType() + l.getToNode().getId() );
				}
				break;

			case 2:
				/**invokes the ptActWriter to create a out_plan file with pt_legs and pt_acts and tries a simulation run*/
	    		PTActWriter ptActWriter = new PTActWriter(pt);
	    		ptActWriter.findRouteForActivities();
	    		//ptActWriter.SimplifyPtLegs();
	    		//ptActWriter.printPTLegs();
	    		//ptActWriter.ptTravelTime.costValidator.printNegativeVaues();
	    		//System.out.println(ptActWriter.ptTravelTime.costValidator==null);
	    		break;
	 
	    	case 3:
	    		/**counts the number of routes found for a population */
	    		PTTester pttester= new PTTester(pt);
	    		pttester.countRoutes();
	    		break;

	    	case 4:
	    		/**Converter: from mmoyo.PTCase2.PTTimeTable2 to marcel.pt.transitSchedule.transitSchedule */
	    		ToTransitScheduleConverter converter = new ToTransitScheduleConverter();	    		
	    		converter.createTransitSchedule(pt.getPtTimeTable(), pt.getPtNetworkLayer(), path + "transitSchedule.xml");
	    		break;
	    	
	    	case 5:
	    		/**reads a transitSchedule file to create a PTTimeTable and routes a population 
	    		 *  to be replaced completely by TransitSchedule
	    		 * */
	    		TransitScheduleToPTTimeTableConverter transitScheduleToPTTimeTableConverter = new TransitScheduleToPTTimeTableConverter();
	    		PTTimeTable2 ptTimeTable = transitScheduleToPTTimeTableConverter.getPTTimeTable(path + "transitSchedule.xml", pt.getPtNetworkLayer());
	    		pt.setPTTimeTable(ptTimeTable);
	    		PTActWriter ptActWriter3 = new PTActWriter(pt);
	    		ptActWriter3.findRouteForActivities();
	    		break;

	    		/**Reads a TransitSchedule File and creates plainNet, LogicTransitSchedule and logicNet from it*/
	    	case 6:
	    		//read plain Net // this is temporal, the network must proceed of original data source, temporarly is the same plain network*/ 
	    		Network plainNet= new NetworkLayer(new NetworkFactory());
	    		new MatsimNetworkReader(plainNet).readFile(PLAINNETWORK);

	    		/**read TransitSchedule */
	    		TransitSchedule transitSchedule = new TransitSchedule();
	    		try {
	    			new TransitScheduleReaderV1(transitSchedule, plainNet).readFile(TRANSITSCHEDULEFILE);
	    		} catch (SAXException e) {
	    			e.printStackTrace();
	    		} catch (ParserConfigurationException e) {
	    			e.printStackTrace();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}
	    		/**Creates logic elements*/
	    		LogicFactory logicNetLayerFactory = new LogicFactory(transitSchedule);
	    		logicNetLayerFactory.writeLogicElements(PLAINNETWORK, LOGICTRANSITSCHEDULE, LOGICNETWORK);
	    		break;

	    		/**Reads plainNetwork, logicNetwork and logicTransitSchedule files to route a population*/
	    	case 7:
	    		LogicTransitOb logicTransitOb = new LogicTransitOb(PLAINNETWORK, LOGICNETWORK, LOGICTRANSITSCHEDULE); 
	    		PTRouter2 ptRouter = logicTransitOb.getPTRouter();
	    		pt.setPtRouter2(ptRouter);
	    		pt.setPtNetworkLayer(logicTransitOb.getLogicNet());
	    		PTActWriter ptActWriter4 = new PTActWriter(pt);
	    		ptActWriter4.findRouteForActivities();
	    		break;
		}
	}
}