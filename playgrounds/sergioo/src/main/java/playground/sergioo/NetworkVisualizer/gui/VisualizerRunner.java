package playground.sergioo.NetworkVisualizer.gui;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class VisualizerRunner {

	/**
	 * @param args: 0 - Title, 1 - MATSim XML network file, 2 - MATSim XML public transport schedule file
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[1]);
		Window window = null;
		if(args.length<3)
			window = new Window(args[0],scenario.getNetwork());
		else {
			((ScenarioImpl)scenario).getConfig().scenario().setUseTransit(true);
			new TransitScheduleReader(scenario).readFile(args[2]);
			window = new Window(args[0],scenario.getNetwork(),((ScenarioImpl)scenario).getTransitSchedule());
		}
		window.setVisible(true);
	}

}
