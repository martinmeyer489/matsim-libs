/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.VrpQSimConfigConsistencyChecker;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


public class RunTaxiScenario
{
    public static void run(String configFile, boolean otfvis)
    {
        final TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
        Config config = ConfigUtils.loadConfig(configFile, taxiCfg, new OTFVisConfigGroup());
        config.addConfigConsistencyChecker(new VrpQSimConfigConsistencyChecker());
        config.checkConsistency();

        Scenario scenario = ScenarioUtils.loadScenario(config);
        TaxiData taxiData = new TaxiData();
        new VehicleReader(scenario.getNetwork(), taxiData).parse(taxiCfg.getTaxisFile());

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new TaxiModule(taxiData));
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule());
        controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));

        if (otfvis) {
            controler.addOverridingModule(new OTFVisLiveModule());
        }

        addDetailedTaxiStats(controler, taxiCfg, taxiData, 30);
        controler.addControlerListener(new TaxiSimulationConsistencyChecker(taxiData));

        //TODO
        //addTaxiStats:
        //        TaxiStats stats = new TaxiStatsCalculator(context.getVrpData().getVehicles().values())
        //                .getStats();

        controler.run();
    }


    private static void addDetailedTaxiStats(Controler controler, final TaxiConfigGroup taxiCfg,
            final TaxiData taxiData, final int hours)
    {
        if (taxiCfg.getDetailedTaxiStatsDir() != null) {
            controler.addControlerListener(new AfterMobsimListener() {
                @Override
                public void notifyAfterMobsim(AfterMobsimEvent event)
                {
                    int iteration = event.getIteration();
                    HourlyTaxiStatsCalculator calculator = new HourlyTaxiStatsCalculator(
                            taxiData.getVehicles().values(), hours);
                    HourlyTaxiStats.printAllStats(calculator.getStats(),
                            taxiCfg.getDetailedTaxiStatsDir() + "/hourly_stats_run_" + iteration);
                    HourlyHistograms.printAllHistograms(calculator.getHourlyHistograms(),
                            taxiCfg.getDetailedTaxiStatsDir() + "/hourly_histograms_run_"
                                    + iteration);
                    calculator.getDailyHistograms()
                            .printHistograms(taxiCfg.getDetailedTaxiStatsDir()
                                    + "/daily_histograms_run_" + iteration);
                }
            });
        }
    }


    public static void main(String[] args)
    {
        String configFile = "./src/main/resources/one_taxi/one_taxi_config.xml";
        RunTaxiScenario.run(configFile, true);
    }
}
