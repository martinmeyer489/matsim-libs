package playground.anhorni.locationchoice.preprocess.plans.planmodificationsTRB09;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Counter;

public class PlanRemoveUnselected {

	private Population plans=null;
	private NetworkImpl network=null;
	private ActivityFacilitiesImpl  facilities =null;
	private String outputpath="";

	private final static Logger log = Logger.getLogger(PlanRemoveUnselected.class);

	/**
	 * @param:
	 * - path to plans file
	 */
	public static void main(final String[] args) {


		if (args.length < 1 || args.length > 1 ) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String plansfilePath=args[0];

		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";
		String worldfilePath="./input/world.xml";

		PlanRemoveUnselected plansModifier=new PlanRemoveUnselected();
		plansModifier.init(plansfilePath, networkfilePath, facilitiesfilePath, worldfilePath);
		plansModifier.runModifications();
	}

	private void runModifications() {

		Counter counter = new Counter(" person # ");
		for (Person person : this.plans.getPersons().values()) {
			counter.incCounter();
			((PersonImpl) person).removeUnselectedPlans();
		}
		log.info("runModifications done.");

		this.outputpath="./output/plans_randomizedzhlocs_cleaned.xml.gz";
		this.writePlans();
	}



	private void init(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath, final String worldfilePath) {

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		log.info("network reading done");

		//this.facilities=new Facilities();
		this.facilities = scenario.getActivityFacilities();
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		log.info("facilities reading done");

		this.plans=scenario.getPopulation();
		final PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
		log.info(this.plans.getPersons().size() + " persons");
	}

	private void writePlans() {
		new PopulationWriter(this.plans, this.network).write(this.outputpath);
		log.info("plans written to: " + this.outputpath);
	}

}
