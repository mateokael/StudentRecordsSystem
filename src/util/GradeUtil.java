package util;

import java.util.List;

/** Grade computation: final grade = average of Prelim, Midterm and Finals. */
public class GradeUtil {

    public static final double PASSING = 75.0;

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Returns null if any component is missing. */
    public static Double computeFinal(Double prelim, Double midterm, Double finals) {
        if (prelim == null || midterm == null || finals == null) return null;
        return round2((prelim + midterm + finals) / 3.0);
    }

    public static String remarks(Double finalGrade) {
        if (finalGrade == null) return "INCOMPLETE";
        return finalGrade >= PASSING ? "PASSED" : "FAILED";
    }

    /** Philippine-style equivalent (1.00 highest, 5.00 failed). Returns null if no grade. */
    public static Double equivalentValue(Double g) {
        if (g == null) return null;
        if (g >= 97) return 1.00;
        if (g >= 94) return 1.25;
        if (g >= 91) return 1.50;
        if (g >= 88) return 1.75;
        if (g >= 85) return 2.00;
        if (g >= 82) return 2.25;
        if (g >= 79) return 2.50;
        if (g >= 76) return 2.75;
        if (g >= 75) return 3.00;
        return 5.00;
    }

    public static String equivalent(Double g) {
        Double e = equivalentValue(g);
        return e == null ? "-" : String.format("%.2f", e);
    }

    /** Parses a score text field. Blank = null. Must be 0-100. */
    public static Double parseScore(String text, String label) {
        if (Validator.isBlank(text)) return null;
        double v;
        try {
            v = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a valid number.");
        }
        if (v < 0 || v > 100) {
            throw new IllegalArgumentException(label + " must be between 0 and 100.");
        }
        return round2(v);
    }

    public static Double toDouble(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }

    /** Units-weighted average of the grades (rows that have a grade only). */
    public static Double weightedAverage(List<Object[]> rows, int unitsIdx, int gradeIdx, boolean useEquivalent) {
        double sum = 0, units = 0;
        for (Object[] r : rows) {
            Double g = toDouble(r[gradeIdx]);
            if (g == null) continue;
            double u = ((Number) r[unitsIdx]).doubleValue();
            double val = useEquivalent ? equivalentValue(g) : g;
            sum += val * u;
            units += u;
        }
        return units == 0 ? null : round2(sum / units);
    }
}
