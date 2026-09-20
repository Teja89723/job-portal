# What was wrong & what I changed

## Your symptoms, explained

1. **Worked on first run** → you had a working MySQL connection (local or a
   fresh Render database).
2. **Worked on Render for a few days, then failed opening the server link**
   → this is almost always a **database connectivity problem**, not an app
   bug:
   - Free MySQL/Postgres instances on Render (and similar free hosts like
     PlanetScale, Clever Cloud, freemysqlhosting.net, etc.) are commonly
     **deleted or suspended after ~30 days**, go to sleep after inactivity,
     or silently drop idle connections after a timeout.
   - This project's connection pool (Hikari, Spring Boot's default) had
     **no timeout/keep-alive settings**, so once the DB became unreachable
     or handed back a stale connection, every DB-backed request — including
     login, which has to query the `users` table — would fail or hang. The
     home page can still look "fine" because it doesn't touch the database.
   - **Action needed from you:** check your Render dashboard → your MySQL
     database's status/expiry, and rotate/renew the `SPRING_DATASOURCE_URL`,
     `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` env vars on
     the Render **web service** if the database was recreated or its
     credentials changed. I can't fix an expired external database from
     inside the code — only Render's dashboard (or a new DB) can.
3. **Runs fine in VS Code now, but login fails for both roles** → the
   project's `application.properties` (below) had **no local fallback**
   for the datasource — it only reads `SPRING_DATASOURCE_URL` /
   `_USERNAME` / `_PASSWORD` from environment variables with no default.
   If those aren't set in your local machine/VS Code, Spring either fails
   to start, or — if you still have old Render env vars lying around in
   your OS/VS Code environment — it silently tries to reach a database
   that's no longer reachable, and every login attempt (a DB query) fails
   with no visible reason on the login page itself.

## Root cause summary

- No local database fallback configuration → local runs depend on an
  external, possibly-expired database.
- No Hikari connection-pool resilience settings → dead/stale connections to
  a free-tier DB aren't detected or retried, so requests just fail.
- The login page gave **zero feedback** when a login attempt failed
  (no error message shown at all), so a bad password and a dead database
  connection looked identical to you — "it's just failing."
- Minor: `login.html` had a broken `th:object="${user}"` binding with
  `field="..."` attributes that don't do anything (should've been
  `th:field`), and referenced a non-existent `/css/style.css` file (the
  real file is `styles.css`).
- Minor: `RecruiterProfileController` could throw a `NullPointerException`
  if a recruiter submitted their profile without choosing a photo.

## Files changed

| File | Change |
|---|---|
| `src/main/resources/application.properties` | Added **local defaults** for the datasource URL/user/password (matches `DB_Scripts/`), so it runs locally with zero env vars. Render/production env vars still override these automatically — no deployment change needed. Added Hikari `connection-timeout`, `validation-timeout`, `max-lifetime`, `idle-timeout`, `keepalive-time` so dead/stale connections to a sleeping or expired free DB are detected and replaced instead of silently breaking requests. Added logging so security/DB issues show up in the console. |
| `src/main/resources/templates/login.html` | Removed the dead/broken `th:object`/`field=` attributes. Added a visible error banner ("Invalid email or password") and a logout banner, so a failed login is no longer silent. Fixed a broken CSS reference (`style.css` → `styles.css`) and removed a duplicate stylesheet `<link>`. |
| `src/main/java/com/jobportal/config/CustomAuthenticationFailureHandler.java` | **New file.** Logs the exact reason a login attempt failed (bad credentials vs. a database exception) to the console/Render logs, so the next time login fails you can see *why* instead of guessing. |
| `src/main/java/com/jobportal/config/WebSecurityConfig.java` | Wired in the new failure handler. |
| `src/main/java/com/jobportal/controllers/RecruiterProfileController.java` | Fixed a potential `NullPointerException` when saving a recruiter profile without a photo. |
| `DB_Scripts/00-create-user.sql` | The script only granted the `jobportal` DB user access from `'localhost'`. Added a matching `'jobportal'@'%'` user/grant so it also works over TCP (e.g. from Docker, or a JDBC URL like `jdbc:mysql://localhost:3306/...`, which some MySQL setups treat differently from a Unix-socket `localhost` connection). |
| `docker-compose.yml` | **New file.** One command (`docker compose up -d`) starts a local MySQL pre-loaded with the schema from `DB_Scripts/01-jobportal.sql`, matching the new local defaults exactly — so VS Code works out of the box. |
| `.env.example` | **New file.** Documents how to point local runs at a remote (e.g. Render) database instead of the local one, if you ever need to. |
| `.gitignore` | Now also ignores `.env` (so secrets never get committed) and the `photos/` upload folder (user-uploaded files shouldn't be in version control). |

## How to run it locally now

```bash
docker compose up -d          # starts MySQL with the schema already loaded
./mvnw spring-boot:run        # or just click Run in VS Code / IntelliJ
```

Then open `http://localhost:8080`, register a new Job Seeker and a new
Recruiter account, and log in with each — both should now work.

## If login still fails after this

Watch the console output right after you submit the login form:

- If you see a log line from `CustomAuthenticationFailureHandler` saying
  **"Bad credentials"** → the email/password combo is wrong (try
  registering a fresh test account).
- If you see a **`AuthenticationServiceException`**, a MySQL/JDBC
  exception, or a connection timeout → it's a database connectivity issue,
  not an app bug. Check that MySQL is actually running
  (`docker compose ps`) and that your datasource env vars (if any are set)
  point to a live database.

## Note on the Render deployment

Once your Render database is confirmed active again, double-check on
Render's dashboard that the **web service's** environment variables
(`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`) still match that database's **current**
connection string and credentials — these can change if the database was
ever recreated.

---

# Round 2: Saved Jobs, Apply, and a real Job Seeker profile

After login was confirmed working, testing surfaced a second issue: the
"View Saved Jobs" link 404'd (`No static resource saved-jobs`). Digging in
showed this wasn't a login bug at all — several features were **referenced
in the templates and even had database tables already created for them,
but had no Java code behind them at all.**

## What was actually missing

- `job_seeker_save` and `job_seeker_apply` tables already existed in
  `DB_Scripts/01-jobportal.sql`, and `job-details.html` already had
  "Apply Now" / "Save Job" buttons and an applicant list — but there was
  **no JPA entity, repository, service, or controller endpoint** for
  either one anywhere in the code. The buttons rendered as invisible
  because the model attributes they depended on (`alreadyApplied`,
  `alreadySaved`, `applyList`) were never set.
- `/saved-jobs/` and `/job-seeker-profile/` were linked from the nav menu
  but had **no controller and no template** — clicking either produced a
  404.
- The job seeker's own "Search Results" list on the dashboard was **never
  populated** — the controller only filled in job listings for recruiters,
  so job seekers saw an empty dashboard with nothing to apply to or save.
- `dashboard.html` referenced `jobPost.jobLocation`, which exists on the
  recruiter's DTO but not on the raw job entity used for job seekers —
  this would have thrown an error the moment job seekers *did* get a
  job list.
- There was no dedicated job seeker "Edit Profile" page at all, despite
  being linked from three different templates.

## What I built

**Saved Jobs (now fully working):**
- `JobSeekerSave` entity + repository + `JobSeekerSaveService`, mapped to
  the existing `job_seeker_save` table.
- `/job-details/save/{id}` (POST) — toggles save/unsave from the job
  details page.
- `/saved-jobs/` (GET) — a new page listing everything you've saved, with
  a "Remove" button (`/saved-jobs/remove/{id}`).
- The main job dashboard now shows a live "(Saved)" tag per job.

**Apply to jobs (was also broken, fixed since it shares the same page):**
- `JobSeekerApply` entity + repository + `JobSeekerApplyService`, mapped
  to the existing `job_seeker_apply` table.
- `/job-details/apply/{id}` (POST) — applies to a job (once only; the
  button correctly switches to "Already Applied").
- Recruiters now see their actual list of applicants on each job's detail
  page (`applyList`), instead of an empty section.

**Job Seeker Profile — expanded to collect Naukri-style details:**
A dedicated `/job-seeker-profile/` page now exists (`JobSeekerProfileController`
+ `job_seeker_profile.html`), matching the pattern already used for the
recruiter profile page. Beyond the original name/city/state fields, it now
also collects:
- Phone number, date of birth, gender
- Current designation, current company, total experience
- Current CTC, expected CTC, notice period
- Highest qualification, university, graduation year
- Key skills (comma-separated tags → stored as individual `Skills` rows)
- A free-text profile summary and LinkedIn URL
- Resume upload (PDF/DOC/DOCX) and profile photo, alongside the existing fields

These new fields were added to the `JobSeekerProfile` entity and to
`DB_Scripts/01-jobportal.sql`; Hibernate's `ddl-auto=update` will also add
them automatically to an existing database on next startup even if you
don't re-run the SQL script.

**Fixes needed to make the above actually render:**
- Job seekers now get a real, populated job list on `/dashboard/`
  (previously always empty), each job flagged with live "(Saved)" and
  "(Applied)" status.
- Added a `getJobLocation()`/`getJobCompany()` alias on `JobPostActivity`
  so the dashboard template (shared between the recruiter and job seeker
  views) doesn't throw a property-not-found error for the job seeker case.
- Profile photo `<img>` tags on the dashboard and job details page were
  pointing at just the bare filename instead of the actual `/photos/...`
  path — fixed to use the existing (and now also job-seeker-side)
  `getPhotosImagePath()` helper.

## New/changed files (round 2)

| File | What |
|---|---|
| `entity/JobSeekerProfile.java` | Expanded with Naukri-style fields + photo/resume path helpers |
| `entity/JobPostActivity.java` | Added `getJobLocation()`/`getJobCompany()` aliases |
| `entity/JobSeekerApply.java` | **New** |
| `entity/JobSeekerSave.java` | **New** |
| `repository/JobSeekerApplyRepository.java` | **New** |
| `repository/JobSeekerSaveRepository.java` | **New** |
| `services/JobSeekerApplyService.java` | **New** |
| `services/JobSeekerSaveService.java` | **New** |
| `services/JobSeekerProfileService.java` | **New** |
| `services/JobPostActivityService.java` | Added `getAllActiveJobs()` |
| `controllers/JobSeekerApplyController.java` | Rewritten — added apply/save endpoints and the model attributes `job-details.html` already expected |
| `controllers/SavedJobsController.java` | **New** |
| `controllers/JobSeekerProfileController.java` | **New** |
| `controllers/JobPostActivityController.java` | Now populates job seeker's dashboard job list with saved/applied status |
| `templates/saved-jobs.html` | **New** |
| `templates/job_seeker_profile.html` | **New** — the Naukri-style profile form |
| `templates/dashboard.html`, `templates/job-details.html` | Fixed profile photo path |
| `DB_Scripts/01-jobportal.sql` | Added new `job_seeker_profile` columns |

## Known remaining gap (out of scope for this pass)

The recruiter's "Edit Job" / "Delete Job" buttons on `job-details.html`
post to `/dashboard/edit/{id}` and `/dashboard/deleteJob/{id}`, but only a
`GET /dashboard/edit/{id}` mapping exists (wrong HTTP method) and
`deleteJob` doesn't exist at all. This is unrelated to job seeker login,
saved jobs, or the profile — flagging it here so it isn't a surprise if
you click those buttons as a recruiter. Happy to fix this in a follow-up
if you'd like.

