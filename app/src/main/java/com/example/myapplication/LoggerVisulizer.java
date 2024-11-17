import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class LoggerVisualizer {
    private static final Logger logger = Logger.getLogger("AccidentProbabilityLogger");
    private List<Integer> timestamps;
    private List<Double> probabilities;
    private List<SpeedCommand> speedCommands;
    private List<CommandOutcome> commandOutcomes;

    public LoggerVisualizer() {
        configureLogging();
        this.timestamps = new ArrayList<>();
        this.probabilities = new ArrayList<>();
        this.speedCommands = new ArrayList<>();
        this.commandOutcomes = new ArrayList<>();
    }

    private void configureLogging() {
        try {
            Handler fileHandler = new FileHandler("accident_probability.log");
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false); // Prevent logging to console
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logPrediction(int timeSec, double probability) {
        logger.info("Time " + timeSec + " sec: Accident Probability = " + String.format("%.4f", probability));
        timestamps.add(timeSec);
        probabilities.add(probability);
    }

    public void logSpeedCommand(int timeSec, double desiredSpeed) {
        logger.info("Time " + timeSec + " sec: Desired Speed Command Issued = " + desiredSpeed + " m/s");
        speedCommands.add(new SpeedCommand(timeSec, desiredSpeed));
    }

    public void logCommandOutcome(int timeSec, String outcome) {
        logger.info("Time " + timeSec + " sec: Command Outcome = " + outcome);
        commandOutcomes.add(new CommandOutcome(timeSec, outcome));
    }

    public void visualize(double threshold) {
        XYSeries series = new XYSeries("Accident Probability");
        for (int i = 0; i < timestamps.size(); i++) {
            series.add(timestamps.get(i), probabilities.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Accident Probability Over Time with Commands and Outcomes",
                "Time (seconds)",
                "Probability",
                dataset
        );

        // Add threshold line
        XYPlot plot = chart.getXYPlot();
        Marker thresholdMarker = new ValueMarker(threshold);
        thresholdMarker.setPaint(java.awt.Color.RED);
        thresholdMarker.setStroke(new java.awt.BasicStroke(2.0f));
        plot.addRangeMarker(thresholdMarker, org.jfree.ui.Layer.FOREGROUND);

        // Highlight points where speed commands were issued
        for (SpeedCommand cmd : speedCommands) {
            plot.addDomainMarker(new ValueMarker(cmd.timeSec), org.jfree.ui.Layer.ABOVE);
            // Additional markers can be added for better visualization
        }

        // Highlight command outcomes
        for (CommandOutcome outcome : commandOutcomes) {
            if (outcome.outcome.equals("Success")) {
                plot.addRangeMarker(new ValueMarker(outcome.timeSec), org.jfree.ui.Layer.FOREGROUND);
            } else {
                // Handle failure markers if needed
            }
        }

        ChartFrame frame = new ChartFrame("Accident Probability", chart);
        frame.pack();
        frame.setVisible(true);
    }

    // Inner classes to represent commands and outcomes
    public static class SpeedCommand {
        public int timeSec;
        public double desiredSpeed;

        public SpeedCommand(int timeSec, double desiredSpeed) {
            this.timeSec = timeSec;
            this.desiredSpeed = desiredSpeed;
        }
    }

    public static class CommandOutcome {
        public int timeSec;
        public String outcome;

        public CommandOutcome(int timeSec, String outcome) {
            this.timeSec = timeSec;
            this.outcome = outcome;
        }
    }
}
