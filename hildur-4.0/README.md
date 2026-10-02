# Hildur 4.0 – digital guest experience

A small Nordic hotel's guest app. **Hildur** is the friendly AI receptionist; **Kjell** is the hotel cat
(role: mascot – no admin rights). Guests check in, book the sauna, order breakfast, report problems,
watch for northern lights, get the Wi-Fi password, manage their data and check out. Staff get one
screen with sauna capacity, problem reports and an activity log.

| Part     | Tech                                             | Folder      |
|----------|--------------------------------------------------|-------------|
| Frontend | React 19 + TypeScript, built with Vite           | `frontend/` |
| Backend  | Java 25, Spring Boot 4 (REST API)                | `backend/`  |
| Database | H2 **in-memory** database via Spring Data JPA    | (in RAM)    |

The original design (HTML prototype + chat with the designer) is in `project/` and `chats/`.

## Run it

You need **Java 25**, **Maven 3.9+** and **Node 20+**.

```bash
# 1) Backend – http://localhost:8080
cd backend
mvn spring-boot:run

# 2) Frontend – http://localhost:5173  (in a second terminal)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The dev server forwards every `/api/...` call to the backend.

**One-jar option:** `cd frontend && npm run build:backend` puts the built React app inside
`backend/src/main/resources/static`; then `cd backend && mvn package && java -jar target/hildur-backend-4.0.0.jar`
serves app + API together on http://localhost:8080.

### Demo logins

Tap **“Demo: fyll i testuppgifter”** on the login screen, or type:

| Who   | Booking number / username | Last name / PIN |
|-------|---------------------------|-----------------|
| Guest | `HX-4821`                 | `Lind`  (Anna, room 12) |
| Guest | `HX-5530`                 | `Berg`  (Erik, room 7)  |
| Guest | `HX-6102`                 | `Smith` (Sam, room 4)   |
| Staff | `lisa.b`                  | `2026`                  |

Because the database lives in memory, **restarting the backend resets everything** to this demo data.
Browse the tables while it runs at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:hildur`, user `sa`, no password).

### Tests

```bash
cd backend && mvn test        # 11 API tests: logins, roles, sauna capacity, Wi-Fi rule, reports, checkout
cd frontend && npm run build  # type-check + production build
```

## How it fits together (the 10-year-old version)

Think of the hotel itself:

- The **frontend** (React) is the *lobby*: what guests see and tap. It holds no secrets.
- The **backend** (Spring Boot) is the *front desk*: it checks who you are and decides what you may do.
- The **database** (H2) is the *big guest book* behind the desk. "In-memory" means it's written in
  pencil on a whiteboard – fast, but wiped when the lights go out (the app stops).
- The **API** is the *desk counter*: the lobby can only ask for things through it, using fixed questions
  like `POST /api/guest/sauna/booking`.

A request travels like this:

```
React screen ──fetch──▶ AuthInterceptor ──▶ Controller ──▶ Service ──▶ Repository ──▶ H2 database
 (the lobby)            (the bouncer)       (the waiter)   (the chef)   (the librarian)  (the guest book)
```

| Layer | Example file | Its one job |
|-------|--------------|-------------|
| **Entity** | `domain/Booking.java` | A Java class that *is* a database table row (`@Entity`). |
| **Repository** | `domain/BookingRepository.java` | An interface; Spring writes the SQL for you from method names like `findBySaunaSlot`. |
| **Service** | `guest/GuestService.java` | The business rules: "a full slot can't be booked", "Wi-Fi only after check-in". `@Transactional` = all-or-nothing. |
| **Controller** | `guest/GuestController.java` | Turns HTTP into Java calls and Java objects into JSON. No rules here. |
| **DTO** | `guest/GuestDtos.java` | Java `record`s describing the exact JSON shapes sent over the wire. |
| **Interceptor** | `auth/AuthInterceptor.java` | Runs before every controller; checks the token and the role. |

### Key ideas worth remembering

- **Two separate logins.** Guests: booking number + last name. Staff: username + PIN (stored only as a
  salted SHA-256 *hash*, never the PIN itself). Each login returns a random **token**; the browser sends it
  as `Authorization: Bearer <token>`. A guest token can't open `/api/staff/**` (HTTP 403) and vice versa.
- **"Guests only see their own booking"** is enforced on the server: guest endpoints never take a booking
  number from the request – they use the one inside the logged-in session.
- **The Wi-Fi password never reaches a guest who hasn't checked in** – the server refuses (403), so
  hiding it isn't just a UI trick.
- **Every change is logged** (`activity/ActivityLogService.java`) in Swedish *and* English.
- **Optimistic locking** (`@Version` on `SaunaSlot`): if two guests grab the last seat at the same moment,
  one wins and the other gets a friendly "try again" instead of an overbooked sauna.
- **Encapsulation (OOP):** `SaunaSlot.takeSeat()` refuses when full – the object protects its own rules
  instead of letting anyone write `booked++` from outside.
- **Scheduled work:** a new report is auto-picked up by the on-duty staff member after 6 seconds
  (`hildur.auto-assign-delay`), so the guest sees "Lisa är på väg". Staff tap the status chip to move it on.
- **Live updates:** the staff screen refreshes every 4 s and the guest's report screen every 3 s (polling).

## API overview

| Method & path | Who | What |
|---------------|-----|------|
| `POST /api/auth/guest` · `/api/auth/staff` · `/api/auth/logout` | anyone | log in / out |
| `GET /api/guest/me` | guest | everything about my stay |
| `POST /api/guest/check-in` | guest | check in |
| `GET /api/guest/sauna/slots` | guest | today's slots and seats taken |
| `POST` / `DELETE /api/guest/sauna/booking` | guest | book (`{"slotId":1}`) / cancel |
| `PUT /api/guest/breakfast` | guest | order (`items`, `mode` ROOM/DINING, `time`) |
| `POST /api/guest/reports` | guest | report a problem |
| `GET` / `PUT /api/guest/aurora` | guest | tonight's forecast / alert on-off |
| `GET /api/guest/wifi` | guest | network + password (only after check-in) |
| `POST /api/guest/data/export` · `/data/deletion` | guest | request export / deletion |
| `POST /api/guest/check-out` | guest | check out with optional 1–5 rating |
| `POST /api/guest/restart` | guest | demo: start the stay over |
| `GET /api/staff/dashboard` | staff | slots, reports, activity log |
| `POST /api/staff/reports/{id}/advance` | staff | Received → In progress → Fixed |

Errors come back as `{"error":"slotFull"}`; the frontend translates the code into a friendly sentence.

## Design notes

- Mobile first: the guest app is a 480 px column (designed at 390 px) centred on the night-blue page;
  the staff view is a responsive grid that stacks on phones.
- Swedish by default, English via the SV/EN switch (remembered per browser). Suomi / Norsk / Deutsch
  show "coming soon", as in the design.
- Easter egg: tap Hildur's avatar three times quickly – she rhymes once, then politely refuses.
- The photo on a problem report is picked and previewed on the device; only "has photo" is sent
  (there's no file storage in this in-memory setup).
- Fonts and icons load from Google Fonts (Young Serif, Nunito Sans, JetBrains Mono, Material Symbols Rounded).
