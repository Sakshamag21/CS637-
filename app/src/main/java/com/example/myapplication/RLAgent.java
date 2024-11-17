import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RLAgent {
    private List<String> actions;
    private double learningRate;
    private double discountFactor;
    private double explorationRate;
    private double explorationDecay;
    private Map<String, Map<String, Double>> qTable;
    private Random rand;

    public RLAgent(List<String> actions, double learningRate, double discountFactor, double explorationRate, double explorationDecay) {
        this.actions = actions;
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.explorationRate = explorationRate;
        this.explorationDecay = explorationDecay;
        this.qTable = new HashMap<>();
        this.rand = new Random();
    }

    public String getState(double accidentProb) {
        if (accidentProb < 0.3) {
            return "Low";
        } else if (accidentProb < 0.6) {
            return "Medium";
        } else {
            return "High";
        }
    }

    public String chooseAction(String state) {
        if (!qTable.containsKey(state)) {
            qTable.put(state, new HashMap<>());
            for (String action : actions) {
                qTable.get(state).put(action, 0.0);
            }
        }

        if (rand.nextDouble() < explorationRate) {
            return actions.get(rand.nextInt(actions.size()));
        } else {
            double maxVal = Double.NEGATIVE_INFINITY;
            for (double val : qTable.get(state).values()) {
                if (val > maxVal) {
                    maxVal = val;
                }
            }
            // In case all Q-values are equal
            List<String> bestActions = new ArrayList<>();
            for (Map.Entry<String, Double> entry : qTable.get(state).entrySet()) {
                if (entry.getValue() == maxVal) {
                    bestActions.add(entry.getKey());
                }
            }
            return bestActions.get(rand.nextInt(bestActions.size()));
        }
    }

    public void updateQTable(String state, String action, double reward, String nextState) {
        if (!qTable.containsKey(state)) {
            qTable.put(state, new HashMap<>());
            for (String act : actions) {
                qTable.get(state).put(act, 0.0);
            }
        }
        if (!qTable.containsKey(nextState)) {
            qTable.put(nextState, new HashMap<>());
            for (String act : actions) {
                qTable.get(nextState).put(act, 0.0);
            }
        }

        double oldValue = qTable.get(state).get(action);
        double nextMax = qTable.get(nextState).values().stream().mapToDouble(v -> v).max().orElse(0.0);
        double newValue = oldValue + learningRate * (reward + discountFactor * nextMax - oldValue);
        qTable.get(state).put(action, newValue);
    }

    public void decayExploration() {
        explorationRate *= explorationDecay;
        explorationRate = Math.max(explorationRate, 0.01); // Minimum exploration rate
    }
}
