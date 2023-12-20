package com.sap.charging.server.api.v1;

import com.sap.charging.realTime.State;
import com.sap.charging.realTime.StrategyAlgorithmic;
import com.sap.charging.realTime.model.forecasting.departure.CarDepartureOracle;
import com.sap.charging.server.api.v1.store.OptimizerSettings;
import com.sap.charging.sim.Simulation;
import com.sap.charging.sim.event.Event;
import com.sap.charging.util.Loggable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(value = "emobility-smart-charging REST API")
public class OptimizeChargingProfilesController implements Loggable {

    @Override
    public int getVerbosity() {
        return Simulation.verbosity;
    }

    public StrategyAlgorithmic buildStrategy(OptimizerSettings settings) {
        StrategyAlgorithmic strategy = new StrategyAlgorithmic(new CarDepartureOracle());
        strategy.objectiveFairShare.setWeight(settings.getWeightObjectiveFairShare());
        strategy.objectiveEnergyCosts.setWeight(settings.getWeightObjectiveEnergyCosts());
        strategy.objectivePeakShaving.setWeight(settings.getWeightObjectivePeakShaving());
        strategy.objectiveLoadImbalance.setWeight(settings.getWeightObjectiveLoadImbalance());
        strategy.setReoptimizeOnStillAvailableAfterExpectedDepartureTimeslot(false);
        // strategy.setRescheduleCarsWith0A(true);
        return strategy;
    }

    @CrossOrigin(origins = "*")
    @ApiOperation(value = "Optimize Charging Profiles")
    @PostMapping(path = "/api/v1/OptimizeChargingProfiles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> optimizeChargingProfiles(@ApiParam @RequestBody OptimizeChargingProfilesRequest request) {

        Simulation.verbosity = request.verbosity;

        log(1, "Received /api/v1/OptimizeChargingProfiles with body: " + request.toString());
        log(1, "Using optimizer settings: " + request.optimizerSettings.toString());

        // Request values
        Object response = null;
        HttpStatus httpStatus = HttpStatus.ACCEPTED;
        try {
            State state = request.state.toState();
            Event event = request.event.toEvent(state);

            // React to events
            StrategyAlgorithmic strategy = buildStrategy(request.optimizerSettings);
            log(1, "Optimizer's sorting criteria (objective for cars above min SoC): " + strategy.getSortingCriteriaByObjective());

            strategy.react(state, event);

            // Create response
            response = new OptimizeChargingProfilesResponse(state.cars);
        } catch (Exception e) {
            System.err.println(e.getClass().getName());
            System.err.println(e.getMessage());
            response = new ErrorResponse(e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            e.printStackTrace();
        }

        return new ResponseEntity<>(response, httpStatus);
    }

}


