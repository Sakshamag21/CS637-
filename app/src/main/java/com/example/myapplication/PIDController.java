public class PIDController {
    private double Kp;
    private double Ki;
    private double Kd;
    private double setpoint;
    private Double outputMin;
    private Double outputMax;
    private Double integralMin;
    private Double integralMax;
    private double derivativeFilterCoeff;

    private double previousError;
    private double integral;
    private double previousDerivative;

    public PIDController(double Kp, double Ki, double Kd, double setpoint,
                         Double outputMin, Double outputMax,
                         Double integralMin, Double integralMax,
                         double derivativeFilterCoeff) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
        this.setpoint = setpoint;
        this.outputMin = outputMin;
        this.outputMax = outputMax;
        this.integralMin = integralMin;
        this.integralMax = integralMax;
        this.derivativeFilterCoeff = derivativeFilterCoeff;
        this.previousError = 0.0;
        this.integral = 0.0;
        this.previousDerivative = 0.0;
    }

    public double compute(double measurement, double dt) {
        if (dt <= 0.0) {
            throw new IllegalArgumentException("dt must be positive and non-zero");
        }

        double error = setpoint - measurement;

        // Proportional term
        double P = Kp * error;

        // Integral term with anti-windup
        integral += error * dt;
        if (integralMin != null) {
            integral = Math.max(integralMin, integral);
        }
        if (integralMax != null) {
            integral = Math.min(integralMax, integral);
        }
        double I = Ki * integral;

        // Derivative term with filtering
        double derivative = (error - previousError) / dt;
        double derivativeFiltered = (derivativeFilterCoeff * derivative) +
                                    ((1 - derivativeFilterCoeff) * previousDerivative);
        double D = Kd * derivativeFiltered;
        previousDerivative = derivativeFiltered;

        // PID Output before limits
        double output = P + I + D;

        // Apply output limits
        if (outputMin != null) {
            output = Math.max(outputMin, output);
        }
        if (outputMax != null) {
            output = Math.min(outputMax, output);
        }

        previousError = error;

        return output;
    }

    public void updateParameters(Double Kp, Double Ki, Double Kd) {
        if (Kp != null) {
            this.Kp = Kp;
        }
        if (Ki != null) {
            this.Ki = Ki;
        }
        if (Kd != null) {
            this.Kd = Kd;
        }
    }

    public void reset() {
        this.previousError = 0.0;
        this.integral = 0.0;
        this.previousDerivative = 0.0;
    }

    // Getters and Setters can be added as needed
}
