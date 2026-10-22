package util;

import java.sql.Date;
import java.time.LocalDate;

/** Input validation helpers used by all forms. */
public class Validator {

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isInteger(String s) {
        if (isBlank(s)) return false;
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidEmail(String s) {
        return s != null && s.trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    public static boolean isValidPhone(String s) {
        return s != null && s.trim().matches("^[0-9+\\-() ]{7,20}$");
    }

    /** Parses yyyy-MM-dd. Returns null when the text is not a real date. */
    public static Date parseDate(String s) {
        if (isBlank(s)) return null;
        try {
            return Date.valueOf(LocalDate.parse(s.trim()));
        } catch (Exception e) {
            return null;
        }
    }

    /** School year format: 2026-2027 (second year must be first + 1). */
    public static boolean isValidSchoolYear(String s) {
        if (s == null || !s.trim().matches("\\d{4}-\\d{4}")) return false;
        s = s.trim();
        return Integer.parseInt(s.substring(5)) == Integer.parseInt(s.substring(0, 4)) + 1;
    }

    public static String currentSchoolYear() {
        LocalDate d = LocalDate.now();
        int y = d.getYear();
        return d.getMonthValue() >= 6 ? y + "-" + (y + 1) : (y - 1) + "-" + y;
    }
}
