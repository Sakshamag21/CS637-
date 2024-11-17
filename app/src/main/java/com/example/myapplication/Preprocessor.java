import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.filter.ButterworthFilter;
import org.apache.commons.math3.filter.Filter;
import org.apache.commons.math3.filter.Filter.Type;
import org.apache.commons.math3.filter.ButterworthFilter;
import org.apache.commons.math3.filter.FrequencyResponse;
import org.apache.commons.math3.util.FastMath;

public class Preprocessor {
    public Preprocessor() {
        // Constructor can be expanded if needed
    }

    public double[] applyLowPassFilter(double[] data, double cutoff, double fs, int order) {
        ButterworthFilter filter = new ButterworthFilter();
        filter.lowPass(order, fs, cutoff);
        return filter.filter(data);
    }

    public FilteredData filterData(DataBuffer.BufferData dataDict) {
        FilteredData filteredData = new FilteredData();
        for (String key : new String[]{"speed", "acceleration", "roll", "pitch", "yaw"}) {
            double[] dataArray = null;
            switch (key) {
                case "speed":
                    dataArray = dataDict.speed.stream().mapToDouble(Double::doubleValue).toArray();
                    break;
                case "acceleration":
                    dataArray = dataDict.acceleration.stream().mapToDouble(Double::doubleValue).toArray();
                    break;
                case "roll":
                    dataArray = dataDict.roll.stream().mapToDouble(Double::doubleValue).toArray();
                    break;
                case "pitch":
                    dataArray = dataDict.pitch.stream().mapToDouble(Double::doubleValue).toArray();
                    break;
                case "yaw":
                    dataArray = dataDict.yaw.stream().mapToDouble(Double::doubleValue).toArray();
                    break;
            }

            if (dataArray != null) {
                double cutoffFreq = (key.equals("roll") || key.equals("pitch") || key.equals("yaw")) ? 5.0 : 3.0;
                int order = (key.equals("roll") || key.equals("pitch") || key.equals("yaw")) ? 3 : 2;
                double[] filtered = applyLowPassFilter(dataArray, cutoffFreq, 30.0, order);
                switch (key) {
                    case "speed":
                        filteredData.speed = filtered;
                        break;
                    case "acceleration":
                        filteredData.acceleration = filtered;
                        break;
                    case "roll":
                        filteredData.roll = filtered;
                        break;
                    case "pitch":
                        filteredData.pitch = filtered;
                        break;
                    case "yaw":
                        filteredData.yaw = filtered;
                        break;
                }
            }
        }
        return filteredData;
    }

    // Inner class to hold filtered data
    public static class FilteredData {
        public double[] speed;
        public double[] acceleration;
        public double[] roll;
        public double[] pitch;
        public double[] yaw;
    }
}
