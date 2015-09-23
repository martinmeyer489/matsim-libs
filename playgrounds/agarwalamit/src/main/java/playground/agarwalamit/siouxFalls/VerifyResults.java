/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.siouxFalls;

import java.io.BufferedWriter;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.LoadMyScenarios;
import playground.agarwalamit.analysis.emission.EmissionCostFactors;
import playground.ikaddoura.internalizationCar.MarginalCongestionHandlerImplV3;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class VerifyResults {
	private static final Logger log = Logger.getLogger(VerifyResults.class);

	private static final double marginal_Utl_money=/*0.0789942;//*/0.062; //(for SiouxFalls =0.062 and for Munich =0.0789942);
	private static final double marginal_Utl_performing_sec=0.96/3600;
	private static final double marginal_Utl_traveling_car_sec=-0.0/3600;
	private static final double marginalUtlOfTravelTime = marginal_Utl_traveling_car_sec+marginal_Utl_performing_sec;
	private static final double vtts_car = marginalUtlOfTravelTime/marginal_Utl_money;

	private final  static String runDir = "/Users/aagarwal/Desktop/ils4/agarwal/siouxFalls/outputMCOff/";
	private  final static String [] runNr = /*{"baseCaseCtd","ei","ci","eci"};//*/{"run205"};

	private  static Scenario scenario ;

	private static final boolean considerCO2Costs=true;
	private static final double emissionCostFacotr=1.0;

	public static void main(String[] args) {

		for(int i=0;i<runNr.length;i++){
			String inputConfigFile = runDir+runNr[i]+"/output_config.xml";
			String networkFile = runDir+runNr[i]+"/output_network.xml.gz";
			
			int lastItenation = 400;//LoadMyScenarios.getLastIteration(inputConfigFile);
			String emissionsEventsFile = runDir+runNr[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".emission.events.xml.gz";
			String plansFile = runDir+runNr[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".plans.xml.gz";
			scenario = LoadMyScenarios.loadScenarioFromPlansAndNetwork(plansFile, networkFile);
			//			scenario = ScenarioUtils.loadScenario(config);
			String eventsFile=runDir+runNr[i]+"/ITERS/it."+lastItenation+"/"+lastItenation+".events.xml.gz";

//			calculateEmissionCosts(emissionsEventsFile, scenario,runNr[i]);
			calculateDelaysCosts(eventsFile,scenario,runNr[i]);
//			calculateUserBenefits(scenario, runNr[i]);
		}
		Logger.getLogger(VerifyResults.class).info("Writing files is finsished.");
	}

	private static void calculateEmissionCosts(String emissionsEventsFile, Scenario scenario, String runNr){
		EmissionsAnalyzer analyzer	= new EmissionsAnalyzer(emissionsEventsFile);
		analyzer.init((ScenarioImpl) scenario);
		analyzer.preProcessData();
		analyzer.postProcessData();
		SortedMap<String, Double> totalEmissions = analyzer.getTotalEmissions();
		double totalEmissionCost =0;
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+runNr+"/analysis/verifyTotalEmissionCost.txt");
		try {
//			for(String str:totalEmissions.keySet()){
			for(EmissionCostFactors ecf:EmissionCostFactors.values()) {
				String str = ecf.toString();
				if(str.equals("CO2_TOTAL") && !considerCO2Costs){
					// do not include CO2_TOTAL costs.
				} else {
					double emissionsCosts = ecf.getCostFactor() * totalEmissions.get(str);
					totalEmissionCost += emissionsCosts;
					writer.write(str+" emissions in gm  are = "+"\t"+totalEmissions.get(str).toString()+"\t"+". Total NOX emission cost is "+emissionsCosts);
					writer.newLine();
				}
			}
			writer.write("Emission cost factor is "+"\t"+emissionCostFacotr+"\t"+"and total cost of emissions is "+"\t"+emissionCostFacotr*totalEmissionCost);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	public static void calculateDelaysCosts(String eventsFile,Scenario scenario, String runNr){
		EventsManager em = EventsUtils.createEventsManager();
		MarginalCongestionHandlerImplV3 congestionHandler = new MarginalCongestionHandlerImplV3(em, (ScenarioImpl) scenario);
		MatsimEventsReader eventsReader = new MatsimEventsReader(em);
		em.addHandler(congestionHandler);
		eventsReader.readFile(eventsFile);
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+runNr+"/analysis/400.verifyTotalDelayCost.txt");
		try{
			writer.write("Total delays in sec are \t"+congestionHandler.getTotalDelay()+"\t"+"and total payment due to delays is \t"+vtts_car*congestionHandler.getTotalDelay());
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}

	public static void calculateUserBenefits(Scenario scenario, String runNr){
		Population population = scenario.getPopulation();
		double totalUtils=0;
		for(Person p : population.getPersons().values()){
			double personScore = p.getSelectedPlan().getScore();
			if(personScore<0) {
				log.warn("Utility for person "+p.getId()+" is negative and this ignoring this in user benefit callculation.");
				personScore=0;
			}
			totalUtils+=personScore;
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(runDir+runNr+"/analysis/verifyUserBenefits.txt");
		try{
			writer.write("Total user Benefits in utils are \t"+totalUtils+"\t"+"and total user benefits in monetary units is \t"+totalUtils/marginal_Utl_money);
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
}