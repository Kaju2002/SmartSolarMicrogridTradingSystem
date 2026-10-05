# SolarGrid – Smart Solar Microgrid Trading System

SE4040 Enterprise Application Development – Assignment 1 (Group project)

A client-server system for solar microgrid energy reservations. A C# ASP.NET Core Web API (fat service) with MongoDB holds all business rules, a React web app serves Backoffice users and Grid Operators, and a native Android app (SQLite via Room, Google Maps, QR scanning) serves Prosumers and Grid Operators.

## Links

| Item | Link |
|---|---|
| Git repository | https://github.com/Kaju2002/SmartSolarMicrogridTradingSystem |
| Demo video (OneDrive, max 5 minutes) | [SolarGrid demo – OneDrive](https://mysliit-my.sharepoint.com/:f:/g/personal/it22224002_my_sliit_lk/IgBuggxHZWoOS6qpQ8LTiqURAd0F9UzycWFH1cr-jA6I298?e=KdP8La) |

## Individual Contributions

| Member | Module | Key Responsibilities |
|---|---|---|
| Vithusha – IT22208262 | Identity and Access Management | User registration, login, JWT-based authentication, role-based authorization, profile management, account status workflow (approve / deactivate / reactivate), forgot-password flow |
| Gabilan – IT22060426 | Station Management | Solar station CRUD operations, location and capacity management, nearby-station search using the Haversine formula, battery slot availability updates |
| Kajanthan – IT22224002 | Energy Reservation (Booking) Management | Full booking lifecycle (create, update, cancel, approve), server-side enforcement of the 7-day and 12-hour business rules, QR code generation, Reservations web page, API deployment on IIS |
| Aaron – IT22203380 | Verification and Dashboard | QR code verification and transaction finalization, operator and prosumer dashboards, booking history, search functionality |

## Project Structure

| Folder | Description | Technology |
|---|---|---|
| `SolarGrid.API` | Web API (fat service) – all business logic and data access | ASP.NET Core 8 (C#), MongoDB, JWT, hosted on IIS |
| `SolarGrid.Web` | Web app for Backoffice and Grid Operator | React 19, TypeScript, Vite, Tailwind CSS |
| `SolarGrid.Android` | Mobile app for Prosumer and Grid Operator | Kotlin (native Android), Retrofit, Room (SQLite), Google Maps SDK, ZXing |

## User Roles

- **Backoffice** (web): manages users and solar stations, assigns grid operators, monitors all reservations.
- **Grid Operator** (web and mobile): approves and cancels bookings at their stations, updates free battery slots, verifies prosumers by scanning the booking QR code and confirms the energy transfer.
- **Prosumer** (mobile): registers with NIC, finds nearby stations, books energy slots, changes or cancels bookings, and shows the booking QR code at the station.

## Running the Project

### Web API
1. Install the .NET 8 SDK.
2. Provide the settings below as environment variables or .NET user-secrets (secrets are not stored in the repository):
   - `MongoDbSettings__ConnectionString`
   - `JwtSettings__Key` (at least 32 characters)
   - `EmailSettings__Username`, `EmailSettings__Password`, `EmailSettings__FromAddress` (for password reset e-mails)
3. Run:
   ```
   cd SolarGrid.API
   dotnet run
   ```
   Swagger UI is available at `/swagger`.

### Web App
```
cd SolarGrid.Web
npm install
npm run dev
```
Set `VITE_API_URL` to the API address (default `http://localhost:5204`).

### Android App
1. Open `SolarGrid.Android` in Android Studio.
2. Add your Google Maps key to `local.properties`: `MAPS_API_KEY=...`
3. The app calls the API at `http://10.0.2.2:8081/` (the IIS-hosted API seen from the emulator). Change `BASE_URL` in `core/managers/NetworkManager.kt` if needed.
4. Run on an emulator or device.
