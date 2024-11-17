public class DrivingStyleClassifier {
    private String drivingStyle;

    public DrivingStyleClassifier() {
        this.drivingStyle = "Steady"; // Default style
    }

    public String classify(DataBuffer.BufferData bufferData) {
        double speedMean = bufferData.speed.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double speedStd = calculateStd(bufferData.speed, speedMean);
        double accelMean = bufferData.acceleration.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double accelStd = calculateStd(bufferData.acceleration, accelMean);
        double rollStd = calculateStd(bufferData.roll, 0.0);
        double pitchStd = calculateStd(bufferData.pitch, 0.0);
        double yawStd = calculateStd(bufferData.yaw, 0.0);

        // Define thresholds (these values are illustrative and should be tuned)
        if (speedMean > 25 && accelStd > 3 && yawStd > 90) {
            this.drivingStyle = "Aggressive";
        } else if (speedMean < 10 && accelStd < 1 && yawStd < 30) {
            this.drivingStyle = "Calm";
        } else if (accelStd < 2 && yawStd < 60) {
            this.drivingStyle = "Steady";
        } else {
            this.drivingStyle = "Unsteady";
        }

        return this.drivingStyle;
    }

    private double calculateStd(java.util.List<Double> data, double mean) {
        double variance = data.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}
