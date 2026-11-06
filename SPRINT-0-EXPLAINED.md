# 📖 Sprint 0 — The Complete Architectural Guide

Welcome to the foundational blueprint of **SkribblePro**!

When you open a multi-module microservice project for the first time, seeing 20+ files across different folders can feel overwhelming. This guide breaks down **every single file, folder, and architectural decision** made in Sprint 0 using clear, relatable Spring Boot concepts.

---

## 🗺️ Table of Contents
1. [The 10,000-Foot View: What did we build?](#1-the-10000-foot-view-what-did-we-build)
2. [Why a Monorepo? (Folder Structure Explained)](#2-why-a-monorepo-folder-structure-explained)
3. [Deep Dive: The Root Parent POM](#3-deep-dive-the-root-parent-pom)
4. [Deep Dive: `shared-dto` (The Common Language)](#4-deep-dive-shared-dto-the-common-language)
5. [Deep Dive: `api-gateway` (The Front Door & Guest Session System)](#5-deep-dive-api-gateway-the-front-door--guest-session-system)
6. [Deep Dive: The Microservice Placeholders](#6-deep-dive-the-microservice-placeholders)
7. [Deep Dive: The Frontend Architecture](#7-deep-dive-the-frontend-architecture)
8. [Deep Dive: Docker & Infrastructure](#8-deep-dive-docker--infrastructure)
9. [Sprint 0 Verification Cheatsheet](#9-sprint-0-verification-cheatsheet)

---

## 1. The 10,000-Foot View: What did we build?

In Sprint 0, we did **not** build the game logic yet. Instead, we established the **skeleton and the front door**:

```
                       ┌──────────────────────────────────────────────┐
                       │               Browser (React)                │
                       │   Player enters name & picks avatar color    │
                       └──────────────────────┬───────────────────────┘
                                              │ HTTP :8080
                                   ┌──────────▼──────────┐
                                   │     api-gateway     │
                                   │  - Issues Session   │
                                   │  - Validates Token  │
                                   └──────────┬──────────┘
                                              │ Checks / Stores Token
                                   ┌──────────▼──────────┐
                                   │        Redis        │
                                   │  session:<token>    │
                                   └─────────────────────┘
```

### What works right now:
1. **Maven Multi-Module Build**: All services build together with a single `mvn clean compile`.
2. **Guest Session System**: Players don't need Google/passwords. They enter a name, get a `sessionToken` stored in Redis (24-hour sliding TTL).
3. **Gateway Security Interceptor**: Every API call without a valid session token is blocked with `401 Unauthorized`.
4. **Service Routing**: The gateway is pre-wired to route traffic to the upcoming microservices.
5. **Frontend Onboarding**: A complete React interface with avatar color swatches and auto-token management.

---

## 2. Why a Monorepo? (Folder Structure Explained)

In enterprise companies, you often see two styles of microservices:
1. **Multi-Repo**: 5 separate Git repositories. You have to open 5 VS Code / IntelliJ windows, push 5 separate PRs, and manually publish shared JAR libraries to a private Artifactory repository.
2. **Monorepo**: All 5 services live in **one Git repository** under a single root folder.

We chose a **Monorepo** because you can build, run, and refactor everything in one IDE window!

Here is how the directory is mapped:

```
SkribblePro/
├── pom.xml                     <-- 1. PARENT POM: Coordinates all versions & builds
├── docker-compose.yml          <-- 2. DOCKER: Spins up Postgres, Redis, and all services
├── .env.example                <-- 3. ENVIRONMENT: Template for secrets & DB URLs
│
├── backend/
│   ├── shared-dto/             <-- 4. SHARED DTOs: Pure Java records (No Spring)
│   ├── api-gateway/            <-- 5. GATEWAY (:8080): Front door + Redis Guest Sessions
│   ├── room-service/           <-- 6. ROOMS (:8081): Manages lobby, codes & players (Sprint 1)
│   ├── game-engine/            <-- 7. ENGINE (:8082): WebSocket drawing & turns (Sprint 3-5)
│   └── word-service/           <-- 8. WORDS (:8083): Gemini AI word generator (Sprint 6)
│
└── frontend/                   <-- 9. REACT + VITE: Web UI with Tailwind CSS & Zustand
```

---

## 3. Deep Dive: The Root Parent POM

Look at `SkribblePro/pom.xml`. Notice three crucial elements:

### A. `<packaging>pom</packaging>`
Normal Spring Boot apps produce a `.jar` file (`<packaging>jar</packaging>`). 
The root POM produces **no JAR**. Its only job is to act as a parent coordinator.

### B. `<modules>`
```xml
<modules>
    <module>backend/shared-dto</module>
    <module>backend/api-gateway</module>
    <module>backend/room-service</module>
    <module>backend/game-engine</module>
    <module>backend/word-service</module>
</modules>
```
When you run `mvn clean compile` in the root folder, Maven automatically analyzes the dependencies and compiles them in the exact right order (`shared-dto` first, then the services).

### C. `<dependencyManagement>` vs `<dependencies>`
* `<dependencies>` tells Maven: *"Download and attach this library to this project right now."*
* `<dependencyManagement>` tells Maven: *"Here is a dictionary of library versions (BOM). Don't download them yet, but if any child module asks for Spring Cloud, make sure they all use version `2023.0.4`."*

This prevents version mismatch bugs across your microservices.

---

## 4. Deep Dive: `shared-dto` (The Common Language)

Located at: `backend/shared-dto/`

### Why does this module have ZERO dependencies?
If you put Spring Boot, Hibernate, or Jackson dependencies inside `shared-dto`, any version conflict in the future would break every microservice. 
`shared-dto` is **pure Java 21**.

### Why Java 21 `record` instead of classes?
Before Java 14/16, creating a simple data carrier meant writing 50 lines of boilerplate:
```java
// The Old Way:
public class PlayerInfo {
    private final String guestId;
    private final String name;
    // + Constructor, Getters, equals(), hashCode(), toString()...
}
```

In Java 21, a `record` does all of that in **one line**:
```java
// Java 21 Record:
package com.skribblepro.dto;

public record PlayerInfo(String guestId, String name, String color) {}
```
Records are immutable, thread-safe, and automatically serialize to JSON cleanly.

### The DTOs Created:
* **`PlayerInfo`**: Represents a guest player (ID, name, hex color).
* **`StrokeEvent`**: Coordinates for a line drawn on canvas (`x1, y1, x2, y2, color, brushSize, type`).
* **`GameStateEvent`**: Current phase, round number, drawer name, word blanks, and timer.
* **`PowerUpEvent`**: Details of an attack (e.g., `FREEZE`, `ERASE`, `FAKE_LINES`).
* **`CreateSessionRequest` & `SessionResponse`**: Payloads for logging in as a guest.

---

## 5. Deep Dive: `api-gateway` (The Front Door & Guest Session System)

Located at: `backend/api-gateway/` (Runs on **Port 8080**)

The API Gateway is the **most important service** in Sprint 0. Clients never communicate with `room-service` or `game-engine` directly; everything passes through port 8080.

> [!IMPORTANT]
> **Spring Cloud Gateway is REACTIVE (WebFlux).**
> Unlike standard Spring MVC (`spring-boot-starter-web`) which uses 1 thread per request (Tomcat), Spring Cloud Gateway uses Netty and Reactive Streams (`Mono` and `Flux`). This allows a single gateway node to handle tens of thousands of concurrent WebSocket and HTTP connections with minimal memory!

### A. How Guest Sessions Work (`SessionController.java`)

When the user enters their name on the frontend:
1. Frontend makes a `POST /sessions` with `{ "name": "Sangi", "color": "#FF6B6B" }`.
2. `SessionController` validates:
   * Name length is between 3 and 20 characters.
   * Color matches hex pattern `^#[0-9A-Fa-f]{6}$`.
3. It generates two random UUIDs:
   * `guestId`: A permanent player identifier for the session.
   * `sessionToken`: The secret key sent back to the browser.
4. It stores this in Redis:
   ```
   Redis Key: session:<sessionToken>
   Hash Fields:
     guestId -> "550e8400-e29b-41d4-a716-446655440000"
     name    -> "Sangi"
     color   -> "#FF6B6B"
   TTL: 86400 seconds (24 Hours)
   ```
5. Returns `SessionResponse` containing the token to the frontend.

### B. How Authentication is Enforced (`SessionValidationFilter.java`)

Every single incoming HTTP request passes through this `GlobalFilter`:

```
Incoming Request
      │
      ▼
Is path /sessions (POST) or /actuator/** or /ws/**?
 ├── YES ──► Allow through immediately
 └── NO
      │
      ▼
Does header have "Authorization: Bearer <token>"?
 ├── NO  ──► Return HTTP 401 Unauthorized
 └── YES
      │
      ▼
Does key "session:<token>" exist in Redis?
 ├── NO  ──► Return HTTP 401 Unauthorized (Expired / Fake Token)
 └── YES
      │
      ├── 1. Slide TTL: Refresh Redis expiration for another 24 Hours
      ├── 2. Extract guestId, name, color from Redis
      ├── 3. Inject new headers into the request:
      │         X-Guest-Id:    550e8400...
      │         X-Guest-Name:  Sangi
      │         X-Guest-Color: #FF6B6B
      ▼
Forward request to downstream microservice (room-service, game-engine, etc.)
```

> [!TIP]
> **Why forward `X-Guest-Id` headers?**
> Because downstream services (`room-service`, `game-engine`) don't need to touch Redis just to know who is calling them! The Gateway validates the player once, and hands the player's identity to the microservices on a silver platter.

### C. Gateway Routing (`application.yml`)
The gateway routes requests based on URL paths:
* `/rooms/**` $\rightarrow$ Forwarded to `http://localhost:8081` (Room Service)
* `/games/**` $\rightarrow$ Forwarded to `http://localhost:8082` (Game Engine)
* `/ws/**`    $\rightarrow$ WebSocket connection upgraded to `ws://localhost:8082`

---

## 6. Deep Dive: The Microservice Placeholders

We created 3 other services:
* **`room-service`** on port **8081**
* **`game-engine`** on port **8082**
* **`word-service`** on port **8083**

In Sprint 0, each service contains:
1. A standard `pom.xml` importing `spring-boot-starter-web`, `spring-boot-starter-actuator`, and `shared-dto`.
2. A standard `@SpringBootApplication` main class.
3. An `application.yml` setting its port and enabling `/actuator/health`.

### Why create them now if they have no business logic yet?
In distributed systems, testing that **networking, ports, and Docker builds work** *before* writing 2,000 lines of complex business code saves days of troubleshooting later. We verified that all 4 services can compile, boot, and communicate.

---

## 7. Deep Dive: The Frontend Architecture

Located at: `frontend/` (Vite + React 18 + Tailwind CSS + Zustand)

### A. The Tech Stack
* **Vite**: Modern lightning-fast frontend build tool (replaces Create-React-App).
* **Tailwind CSS**: Utility-first CSS classes (e.g. `bg-slate-900 text-white rounded-xl`).
* **Zustand (`src/store/gameStore.js`)**: A lightweight, boilerplate-free state manager (much simpler than Redux). It stores the player's active session and syncs it with `localStorage`.
* **Axios Interceptor (`src/services/api.js`)**: Automatically looks for `skribblepro_session_token` in `localStorage` and adds `Authorization: Bearer <token>` to every HTTP request.

### B. The Screen Flow
1. **`/` (`EntryPage.jsx`)**: The onboarding screen. User types their name, clicks an avatar color swatch, and clicks "Play Now". Calls `POST /sessions`.
2. **`/lobby` (`LobbyPage.jsx`)**: Displays player's avatar and two options: "Create Room" or "Join Room by 6-char code".
3. **`/room/:code` (`RoomPage.jsx`)**: Waiting room before game starts (Sprint 1 & 2).
4. **`/game/:id` (`GamePage.jsx`)**: The drawing canvas arena (Sprint 3 & 4).

---

## 8. Deep Dive: Docker & Infrastructure

Look at `docker-compose.yml`:

```yaml
services:
  postgres:     # Port 5432: Relational database for room and game history
  redis:        # Port 6379: In-memory store for guest sessions & live game state
  api-gateway:  # Port 8080: Gateway + Auth filter
  room-service: # Port 8081: Room management
  game-engine:  # Port 8082: Real-time WebSockets
  word-service: # Port 8083: Gemini AI word generator
```

* **Healthchecks**: Microservices are configured with `depends_on: { service_healthy }` so Spring Boot doesn't attempt to start before PostgreSQL or Redis is ready to accept connections.
* **Network Isolation**: All services communicate across a private Docker bridge network (`skribble-net`).

---

## 9. Sprint 0 Verification Cheatsheet

Want to verify Sprint 0 on your machine? Here are the exact commands:

### 1. Build the Backend
From the root folder (`SkribblePro/`):
```bash
mvn clean compile
```
*(Should output `BUILD SUCCESS` across all 6 modules)*

### 2. Build the Frontend
From the `frontend/` directory:
```bash
npm run build
```
*(Should compile into `frontend/dist/` in ~1.3 seconds)*

### 3. Running the Gateway Locally (Quick Test)
1. Ensure your local Redis is running on port 6379 (or start it via Docker: `docker run -p 6379:6379 redis:alpine`).
2. Run `ApiGatewayApplication.java` from your IDE.
3. Test guest session creation using curl:
```bash
curl -X POST http://localhost:8080/sessions \
     -H "Content-Type: application/json" \
     -d '{"name":"Sangi","color":"#FF6B6B"}'
```
**Response:**
```json
{
  "sessionToken": "d748f220-4a88-4f91-888d-71289196b052",
  "guestId": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
  "name": "Sangi",
  "color": "#FF6B6B"
}
```

4. Test security rejection (without token):
```bash
curl -i http://localhost:8080/rooms/123
```
**Response:** `HTTP/1.1 401 Unauthorized` 🚫

---

## 🎯 Summary
Sprint 0 gave you a **production-grade foundation**:
* Clean multi-module separation.
* Instant guest onboarding with Redis tokens.
* Secure API Gateway routing and filtering.
* A modern React UI ready for multiplayer.

You are now 100% prepared to build **Sprint 1: The Room Service**! 🚀

