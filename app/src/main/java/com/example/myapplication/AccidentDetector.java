public class AccidentDetector {
    public AccidentDetector() {
        // Constructor can be expanded if needed
    }

    public double calculateProbability(DataBuffer.BufferData dataDict) {
        double speed = dataDict.speed.get(dataDict.speed.size() - 1);
        double acceleration = dataDict.acceleration.get(dataDict.acceleration.size() - 1);
        double roll = dataDict.roll.get(dataDict.roll.size() - 1);
        double pitch = dataDict.pitch.get(dataDict.pitch.size() - 1);
        double yaw = dataDict.yaw.get(dataDict.yaw.size() - 1);

        double probability = 0.0;

        // Define rules to calculate probability
        if (speed > 25) {
            probability += 0.3;
        }
        if (Math.abs(acceleration) > 3) {
            probability += 0.3;
        }
        if (Math.abs(yaw) > 90) {
            probability += 0.2;
        }
        if (Math.abs(roll) > 60 || Math.abs(pitch) > 60) {
            probability += 0.1;
        }

        // Normalize probability to [0,1]
        probability = Math.min(probability, 1.0);

        return probability;
    }
}
