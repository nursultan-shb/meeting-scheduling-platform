# meeting-scheduling-platform
A simulation of a meeting scheduling platform that supports time slot management and meeting scheduling

## Quick start
### Run with Docker (recommended)
Only Docker is required. The compose file builds the service from source (multi-stage build)
and starts it together with PostgreSQL:

```
docker compose up --build
```

* API: http://localhost:16200
* Swagger UI: http://localhost:16200/swagger-ui/index.html
* PostgreSQL: localhost:5432, database/user/password `scheduler`

Data is stored in the `postgres-data` volume and survives restarts. To wipe it, run `docker compose down -v`.
The database schema is created by Flyway migrations (`src/main/resources/db/migration`) on startup.

### Run from an IDE
Prerequisites: Java 17+ and Maven 3.9+ (or the bundled `./mvnw`).

The service needs a running PostgreSQL. The simplest option is to start only the database from the compose file:

```
docker compose up -d postgres
./mvnw spring-boot:run
```

The connection can be overridden with the `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` environment variables
(default: `jdbc:postgresql://localhost:5432/scheduler`, `scheduler`/`scheduler`).

### Tests
`./mvnw test` runs the unit tests. They mock the repositories and do not need a database.

## Using the API
Times are ISO-8601 instants in UTC, for example `2026-11-02T09:00:00Z`. A full walkthrough:

```bash
# 1. Create two users. The id is returned in the Location header
curl -i -X POST localhost:16200/users -H 'Content-Type: application/json' \
     -d '{"email": "alice@example.com", "name": "Alice"}'
curl -i -X POST localhost:16200/users -H 'Content-Type: application/json' \
     -d '{"email": "bob@example.com", "name": "Bob"}'

# 2. Alice opens 09:00-12:00 as six 30-minute slots
curl -X POST localhost:16200/users/$ALICE/slots -H 'Content-Type: application/json' \
     -d '{"startTime": "2026-11-02T09:00:00Z", "endTime": "2026-11-02T12:00:00Z", "slotDurationMinutes": 30}'

# 3. Book the first slot as a meeting with Bob
curl -i -X POST localhost:16200/meetings -H 'Content-Type: application/json' \
     -d '{"title": "1:1", "description": "Weekly sync", "slotId": "'$SLOT'", "participants": ["'$BOB'"]}'
```

### Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/users` | Creates a user (`{"email", "name"}`) together with their calendar. Returns 201 with the user's URL in `Location` |
| GET | `/users/{userId}` | Returns the user |
| POST | `/users/{userId}/slots` | Creates free time slots (`{"startTime", "endTime", "slotDurationMinutes"?}`). Without `slotDurationMinutes` one slot covers the range; with it, the range is split into consecutive slots of that length (1-1440 minutes, the range must be a multiple of it, at most 500 slots per request). Returns 201 with the created slots |
| GET | `/users/{userId}/slots?from&to[&status][&page&size]` | Lists the user's slots that overlap the range, optionally only `FREE` or `BUSY` ones. Paginated, see below |
| PUT | `/users/{userId}/slots/{slotId}` | Changes a slot's start and end time (`{"startTime", "endTime"}`). Not allowed for a slot booked for a meeting |
| PUT | `/users/{userId}/slots/{slotId}/status` | Marks a slot `BUSY` or `FREE` (`{"status"}`). A slot booked for a meeting cannot be freed |
| DELETE | `/users/{userId}/slots/{slotId}` | Deletes a slot that is not booked for a meeting |
| POST | `/meetings` | Books a free slot as a meeting (`{"title", "description"?, "slotId", "participants"?}`). The slot owner and all participants must be available. Returns 201 with the meeting's URL in `Location` |
| GET | `/meetings/{meetingId}` | Returns the meeting with its time, organizer and participants |
| PUT | `/meetings/{meetingId}` | Replaces the title, description and participants (`{"title", "description"?, "participants"}`). Newly added participants must be available. The time cannot be changed: cancel and book another slot instead |
| DELETE | `/meetings/{meetingId}` | Cancels the meeting and frees its slot |

The slot list is paginated with `page` (0-based, default 0) and `size` (default 50, at most 200), always ordered by
start time:

```json
{"content": [{"id": "...", "startTime": "...", "endTime": "...", "status": "FREE"}], "page": 0, "size": 50, "totalElements": 120, "totalPages": 3}
```

Query ranges (`from`/`to`) may span at most 366 days. Interactive documentation of all request and response
bodies is available in the Swagger UI: http://localhost:16200/swagger-ui/index.html

### Errors
Errors are returned as [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem details:

```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"Time slot ... is not free","instance":"/meetings"}
```

| Status | When |
|---|---|
| 400 | Invalid request body or parameters (validation errors are listed per field in `errors`), start time not before end time, range not a multiple of the slot duration, too many slots or too large a query range |
| 404 | Unknown user, time slot, meeting or meeting participant; a slot that does not belong to the user in the path |
| 409 | Overlapping slot; booking a slot that is not free; an attendee who is busy at that time (their ids are listed); changing, freeing or deleting a slot booked for a meeting; duplicate email; concurrent modification |

## Design decisions

### Domain model

```
User 1 ── 1 Calendar 1 ── * TimeSlot 1 ── 0..1 Meeting * ── * User (participants)
```

* **Calendar** is the user's personal calendar and owns their time slots. As the task requires, the term exists
  only in the domain: the API addresses everything through the user (`/users/{userId}/slots`), and a calendar
  is created together with its user. The calendar also turns a time range into slots of a given length
  (`Calendar.openSlots`) and is the unit of locking (see below).
* **TimeSlot** is a period in a calendar, `FREE` or `BUSY`. A slot becomes busy when it is booked or when its
  owner marks it busy, for example to block time without a meeting. Slots of one calendar never overlap.
* **Meeting** is created from a free slot of its organizer (the slot owner) and has a title, description and
  participants. It occupies exactly that slot, so a slot booked for a meeting cannot be moved, freed or deleted;
  cancelling the meeting frees the slot again.

### Consistency under concurrent requests
Overlap and availability rules are "check, then write" operations, which two concurrent requests could both pass.

* Every write to a calendar first locks the calendar's row (`SELECT ... FOR UPDATE`). This serializes writes to the
  same calendar while writes to different calendars run in parallel.
* Booking a meeting, or adding participants to one, locks the calendars of the organizer and all participants, in a fixed order (by id) so that
  two bookings with overlapping attendees cannot deadlock. As a result, a user can never be double-booked, even
  by two different organizers at the same moment.
* Optimistic locking (`@Version`) on all entities and a unique constraint on `meetings.time_slot_id` are a
  second line of defence; a lost race is reported as 409.

An alternative is a PostgreSQL exclusion constraint on `tstzrange(start_time, end_time)` per calendar. It guards
overlapping slots well, but cannot express "no attendee is in another meeting", so row locks are used for both.


### Technology
* **Spring Boot 4 / Java 17**, **Spring Data JPA** for persistence.
* **PostgreSQL**, as required by the "all data should be persisted" requirement, with the schema versioned by
  **Flyway** migrations.
* **springdoc-openapi** for the Swagger UI.
* **Docker Compose** runs the service and its database. The image is built from source in a multi-stage build,
  so only Docker is needed.

### Known limitations and next steps
* No authentication: the user id in the path is trusted. A real service would take the user from a token and
  only let owners change their calendar.
* Meetings can only be changed by cancelling and re-booking when their time should change. There is no endpoint
  listing a user's meetings; the free/busy view shows when they take place.
* There is no endpoint listing a user’s meetings
* Recurring availability (e.g. "every weekday 9-17") is not supported. All times are UTC instants; time zones
  are left to the client.
* Tests are unit tests with mocked repositories. Integration tests against PostgreSQL (Testcontainers) would be
  the next step, in particular for the locking behaviour.
