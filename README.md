# Student Enrollment, Grading and Record Management System
Java Swing / AWT + JDBC + Apache Derby

## 1. Open it in NetBeans
1. **File > New Project > Java with Ant > Java Application**. Name it `StudentRecordsSystem`.
   **Uncheck** "Create Main Class".
2. Close NetBeans' project (or leave it open) and copy everything inside this zip's `src` folder
   (`Main.java`, `model`, `dao`, `database`, `ui`, `util`) into your new project's `src` folder.
   Back in NetBeans, right-click the project > **Refresh** (or reopen it).
3. Add the Derby libraries: right-click **Libraries > Add JAR/Folder** and add from your Derby `lib` folder:
   - `derby.jar`  (embedded engine)
   - `derbyshared.jar` and `derbytools.jar`  (needed by Derby 10.15 and newer)
   - `derbyclient.jar`  (only if you use the Network Server)
   
   Shortcut: **Libraries > Add Library > Java DB Driver** (bundled with NetBeans) also works.
4. Right-click `Main.java` > **Run File** (or set it as the project's main class) and press **F6**.

## 2. Database modes (database/DatabaseConnection.java)
| MODE | What happens |
|------|--------------|
| `AUTO` (default) | Tries the Derby Network Server at `localhost:1527`, then falls back to Embedded Derby |
| `NETWORK` | Network Server only (NetBeans: Services > Databases > Java DB > **Start Server**) |
| `EMBEDDED` | Embedded only; the database folder `StudentEnrollmentDB` is created in the project folder |

The database `StudentEnrollmentDB` and all tables are **created automatically** with sample data on first run.
Pick one mode and stick with it. Network and embedded keep their data in different places.
The mode in use is shown on the login screen and the dashboard.

## 3. Default accounts
| Username | Password | Role | Can do |
|----------|----------|------|--------|
| admin | admin123 | ADMIN | Everything, including user management |
| registrar | registrar123 | REGISTRAR | Students, courses, subjects, enrollment, reports |
| teacher | teacher123 | TEACHER | Grades and attendance, view the rest, reports |
| student | student123 | STUDENT | Own portal only (read-only): profile, subjects, report card, transcript, attendance |

Passwords are stored as SHA-256 hashes.

## 4. Modules
- **Login / Logout**: validation, error messages, role-based access
- **Dashboard**: live counts, menu bar, module buttons
- **Students**: register, view, search, update, delete (JOIN with courses)
- **Courses / Subjects**: CRUD, subjects per course/year/semester
- **Enrollment**: enroll a student in a subject, or **Enroll Whole Term** for his/her year and semester
- **Grades**: Prelim, Midterm, Finals; final grade = average; 75+ = PASSED; equivalent 1.00 to 5.00
- **Attendance**: PRESENT / ABSENT / LATE / EXCUSED per date, with totals
- **Reports**: student list, class list, **report card**, **transcript of records**, grade summary,
  attendance summary, students per course, daily enrollments. Each report can be printed or exported to CSV.
- **Users** (admin): create/update/delete accounts. A STUDENT account must be linked to a registered student.
- **Student Portal**: STUDENT accounts see only their own records (read-only)

To create a student account: register the student (Students), then Users > Role = STUDENT > pick the Linked Student.

## 5. Database tables
USERS (with optional student_id link), COURSES, STUDENTS, SUBJECTS, ENROLLMENTS, GRADES, ATTENDANCE. See `schema.sql`.

Relationships:
- COURSES 1:M STUDENTS, COURSES 1:M SUBJECTS
- STUDENTS 1:1 USERS (optional, for student accounts)
- STUDENTS 1:M ENROLLMENTS, SUBJECTS 1:M ENROLLMENTS (ENROLLMENTS is the associative table)
- ENROLLMENTS 1:1 GRADES, ENROLLMENTS 1:M ATTENDANCE (both ON DELETE CASCADE)

## 6. Troubleshooting
- **"No suitable driver" / cannot connect**: the Derby JARs are not added to Libraries (step 3).
- **Network mode fails**: start the server first, or set `MODE` to `EMBEDDED`.
- **"already has a table ... created by a different program"**: an old database with the same name exists. Delete that folder, or change `DB_NAME`.
- **Reset all data**: close the program and delete the `StudentEnrollmentDB` folder (embedded mode).
- Source level is Java 8 compatible; it also runs on newer JDKs.
