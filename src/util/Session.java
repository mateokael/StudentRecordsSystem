package util;

import model.User;

/** Keeps the currently logged-in user and role-based permissions. */
public class Session {

    private static User currentUser;

    public static void login(User u) { currentUser = u; }
    public static void logout() { currentUser = null; }
    public static User getUser() { return currentUser; }

    private static String role() {
        return currentUser == null ? "" : currentUser.getRole();
    }

    public static boolean isStudent() { return "STUDENT".equals(role()); }

    public static boolean isAdmin() { return "ADMIN".equals(role()); }

    /** Admin and Registrar can add/update/delete students, courses, subjects and enrollments. */
    public static boolean canManageRecords() {
        return "ADMIN".equals(role()) || "REGISTRAR".equals(role());
    }

    /** Admin and Teacher can encode grades and attendance. */
    public static boolean canEncodeGrades() {
        return "ADMIN".equals(role()) || "TEACHER".equals(role());
    }
}
