package playground.jhackney.algorithms;

import java.io.File;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.Knowledge;
import org.matsim.socialnetworks.io.ActivityActReader;

public class InitializeKnowledge {
	public InitializeKnowledge(final Population plans, final Facilities facilities){

		ActivityActReader aar = null;


		// Knowledge is already initialized in some plans files
		// Map agents' knowledge (Activities) to their experience in the plans (Acts)


//		Attempt to open file of mental maps and read it in
		System.out.println("  Opening the file to read in the map of Acts to Facilities");
		aar = new ActivityActReader(Integer.valueOf(Gbl.getConfig().socnetmodule().getInitIter()).intValue());

		String fileName = Gbl.getConfig().socnetmodule().getInDirName()+ "ActivityActMap"+Integer.valueOf(Gbl.getConfig().socnetmodule().getInitIter()).intValue()+".txt";

		if (new File(fileName).exists()) {
			// File or directory exists
			aar.openFile(fileName);
		} else {
			// File or directory does not exist
			aar=null;
		}


		System.out.println(" ... done");

		Iterator<Person> p_it = plans.getPersons().values().iterator();
		while (p_it.hasNext()) {
			Person person=p_it.next();

			Knowledge k = person.getKnowledge();
			if(k ==null){
				k = person.createKnowledge("created by " + this.getClass().getName());
			}
			for (int ii = 0; ii < person.getPlans().size(); ii++) {
				Plan plan = person.getPlans().get(ii);

				// JH Hack to make sure act types are compatible with social nets
				k.getMentalMap().prepareActs(plan);
				// JH If the Acts are not initialized with a Facility they get a random Facility on the Link
				k.getMentalMap().initializeActActivityMapRandom(plan);
				// JH If there is a user-supplied file of Facilities for the Act, read it in
				k.getMentalMap().initializeActActivityMapFromFile(plan,facilities, aar);
			}
		}
		if(aar!=null){
			aar.close();//close the file with the input act-activity map
		}
	}
}
