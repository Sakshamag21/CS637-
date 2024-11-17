import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ControlLoop {
    private int simDuration;
    private int interval;
    private int bufferSize;
    private DataSimulator dataSimulator;
    private DataBuffer dataBuffer;
    private Preprocessor preprocessor;
    private DrivingStyleClassifier drivingStyleClassifier;
    private PIDController pidSpeed;
    private PIDController pidSteering;
    private AccidentDetector accidentDetector;
    private LoggerVisualizer loggerVisualizer;
    private RLAgent rlAgent;
    private static final Logger logger = Logger.getLogger("AccidentProbabilityLogger");

    public ControlLoop(int simDuration, int interval, int bufferSize) {
        this.simDuration = simDuration;
        this.interval = interval;
        this.bufferSize = bufferSize;
        this.dataSimulator = new DataSimulator(simDuration / interval);
        this.dataBuffer = new DataBuffer(bufferSize);
        this.preprocessor = new Preprocessor();
        this.drivingStyleClassifier = new DrivingStyleClassifier();
        this.pidSpeed = new PIDController(
                0.5, 0.1, 0.05, 20.0,
                -1.0, 1.0,
                -10.0, 10.0,
                0.1
        );
        this.pidSteering = new PIDController(
                0.3, 0.05, 0.02, 0.0,
                -1.0, 1.0,
                -5.0, 5.0,
                0.1
        );
        this.accidentDetector = new AccidentDetector();
        this.loggerVisualizer = new LoggerVisualizer();

        List<String> actions = Arrays.asList("increase_Kp", "decrease_Kp", "increase_Ki", "decrease_Ki",
                "increase_Kd", "decrease_Kd", "no_change");
        this.rlAgent = new RLAgent(actions, 0.1, 0.9, 1.0, 0.995);
    }

    public void run() {
        System.out.println("Starting control loop with RL-based self-improvement...\n");
        Double previousProbability = null;

        for (int step = 0; step < simDuration / interval; step++) {
            DataSimulator.SensorData dataPoint = dataSimulator.getNextDataPoint();
            if (dataPoint == null) {
                break; // No more data
            }

            // Append data to buffer
            dataBuffer.appendData(
                    dataPoint.speed,
                    dataPoint.acceleration,
                    dataPoint.roll,
                    dataPoint.pitch,
                    dataPoint.yaw
            );

            int currentTimeSec = (step + 1) * interval;

            // Proceed only if buffer is full
            if (dataBuffer.getBuffer().speed.size() == bufferSize) {
                DataBuffer.BufferData bufferData = dataBuffer.getBuffer();

                // Preprocessing: Apply low-pass filter
                Preprocessor.FilteredData filteredData = preprocessor.filterData(bufferData);

                // Convert filteredData to BufferData-like structure
                DataBuffer.BufferData filteredBufferData = new DataBuffer.BufferData(
                        convertArrayToList(filteredData.speed),
                        convertArrayToList(filteredData.acceleration),
                        convertArrayToList(filteredData.roll),
                        convertArrayToList(filteredData.pitch),
                        convertArrayToList(filteredData.yaw)
                );

                // Driving Style Classification
                String drivingStyle = drivingStyleClassifier.classify(filteredBufferData);

                // Update PID parameters based on driving style
                updatePidParameters(drivingStyle);

                // Compute PID outputs with accurate dt
                double currentSpeed = filteredData.speed[filteredData.speed.length - 1];
                double speedControl = pidSpeed.compute(currentSpeed, interval);

                // For steering, use the latest yaw as a proxy for orientation
                double currentOrientation = filteredData.yaw[filteredData.yaw.length - 1];
                double steeringControl = pidSteering.compute(currentOrientation, interval);

                // Decide on actions
                double throttleCommand = clamp(speedControl, -1.0, 1.0);      // -1: full brake, 1: full throttle
                double steeringCommand = clamp(steeringControl, -1.0, 1.0);  // -1: full left, 1: full right

                // Calculate accident probability
                double accidentProb = accidentDetector.calculateProbability(filteredBufferData);

                // Log and visualize
                loggerVisualizer.logPrediction(currentTimeSec, accidentProb);

                // RL-based Parameter Adjustment
                String state = rlAgent.getState(accidentProb);
                String action = rlAgent.chooseAction(state);
                double reward = 0.0;
                String nextState = null;

                if (accidentProb > 0.5) {
                    double desiredSpeed = calculateDesiredSpeed(accidentProb);
                    issueSpeedCommand(desiredSpeed, currentTimeSec);
                    previousProbability = accidentProb;
                } else {
                    // No action needed, proceed as usual
                    // Optionally, evaluate if previous command was effective
                    if (previousProbability != null) {
                        // Calculate reward based on change in probability
                        double change = previousProbability - accidentProb;
                        reward = change; // Positive if probability decreased
                        nextState = rlAgent.getState(accidentProb);
                        rlAgent.updateQTable(state, action, reward, nextState);
                        rlAgent.decayExploration();
                        previousProbability = null; // Reset after evaluation
                    }
                }

                // Apply RL action to adjust PID parameters
                applyRlAction(action);

                // For demonstration, print the prediction and command
                if (accidentProb > 0.5) {
                    System.out.println("Time " + currentTimeSec + " sec: Driving Style=" + drivingStyle +
                            ", Throttle Command=" + String.format("%.2f", throttleCommand) +
                            ", Steering Command=" + String.format("%.2f", steeringCommand) +
                            ", Accident Probability=" + String.format("%.2f", accidentProb) +
                            " --> Command: Reduce speed to " + String.format("%.2f", calculateDesiredSpeed(accidentProb)) + " m/s");
                } else {
                    if (previousProbability != null && reward > 0) {
                        System.out.println("Time " + currentTimeSec + " sec: Driving Style=" + drivingStyle +
                                ", Throttle Command=" + String.format("%.2f", throttleCommand) +
                                ", Steering Command=" + String.format("%.2f", steeringCommand) +
                                ", Accident Probability=" + String.format("%.2f", accidentProb) +
                                " --> Command Outcome: Improvement");
                    } else {
                        System.out.println("Time " + currentTimeSec + " sec: Driving Style=" + drivingStyle +
                                ", Throttle Command=" + String.format("%.2f", throttleCommand) +
                                ", Steering Command=" + String.format("%.2f", steeringCommand) +
                                ", Accident Probability=" + String.format("%.2f", accidentProb));
                    }
                }

                // Simulate real-time delay (optional; can be commented out for faster execution)
                // Thread.sleep(interval * 1000);
            }
        }

        // After the loop, visualize the logged data
        loggerVisualizer.visualize(0.5);
    }

    private List<Double> convertArrayToList(double[] array) {
        List<Double> list = new ArrayList<>();
        for (double d : array) {
            list.add(d);
        }
        return list;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double calculateDesiredSpeed(double probability) {
        double maxSpeedLimit = 30.0; // Example max speed in m/s
        double desiredSpeed = maxSpeedLimit * (1 - probability);
        desiredSpeed = Math.max(desiredSpeed, 0.0); // Ensure speed is non-negative
        return Math.round(desiredSpeed * 100.0) / 100.0;
    }

    private void issueSpeedCommand(double desiredSpeed, int timeSec) {
        // Log the command in LoggerVisualizer
        loggerVisualizer.logSpeedCommand(timeSec, desiredSpeed);

        // Log the command in the general log
        logger.info("Time " + timeSec + " sec: Desired Speed Command Issued = " + desiredSpeed + " m/s");

        // Print the command
        System.out.println("*** ALERT *** At " + timeSec + " sec: Accident Probability = " +
                String.format("%.2f", accidentDetector.calculateProbability(dataBuffer.getBuffer())) +
                " > 50%. Please reduce your speed to " + desiredSpeed + " m/s for safety.");
    }

    private void applyRlAction(String action) {
        switch (action) {
            case "increase_Kp":
                pidSpeed.updateParameters(pidSpeed.Kp + 0.1, null, null);
                break;
            case "decrease_Kp":
                pidSpeed.updateParameters(Math.max(pidSpeed.Kp - 0.1, 0.0), null, null);
                break;
            case "increase_Ki":
                pidSpeed.updateParameters(null, pidSpeed.Ki + 0.05, null);
                break;
            case "decrease_Ki":
                pidSpeed.updateParameters(null, Math.max(pidSpeed.Ki - 0.05, 0.0), null);
                break;
            case "increase_Kd":
                pidSpeed.updateParameters(null, null, pidSpeed.Kd + 0.01);
                break;
            case "decrease_Kd":
                pidSpeed.updateParameters(null, null, Math.max(pidSpeed.Kd - 0.01, 0.0));
                break;
            case "no_change":
                // Do nothing
                break;
            default:
                // Unknown action
                break;
        }
        // Reset PID controllers to apply new parameters
        pidSpeed.reset();
        pidSteering.reset();
    }

    private void updatePidParameters(String drivingStyle) {
        switch (drivingStyle) {
            case "Aggressive":
                pidSpeed.updateParameters(0.7, 0.15, 0.07);
                pidSteering.updateParameters(0.5, 0.1, 0.05);
                break;
            case "Calm":
                pidSpeed.updateParameters(0.3, 0.05, 0.03);
                pidSteering.updateParameters(0.2, 0.04, 0.02);
                break;
            case "Steady":
                pidSpeed.updateParameters(0.5, 0.1, 0.05);
                pidSteering.updateParameters(0.3, 0.05, 0.02);
                break;
            case "Unsteady":
                pidSpeed.updateParameters(0.4, 0.08, 0.04);
                pidSteering.updateParameters(0.25, 0.04, 0.02);
                break;
            default:
                // Default parameters if needed
                break;
        }
        // Reset PID controllers to prevent integral windup when changing modes
        pidSpeed.reset();
        pidSteering.reset();
    }
}
