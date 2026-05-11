# Restraint Protocol (RP)

> A real-time discipline enforcement protocol powered by AI, Solana staking, and Android behavioral monitoring.

Restraint Protocol (RP) is a next-generation focus and accountability system designed to help users build discipline through real-time enforcement, AI-driven behavioral analysis, and financial commitment mechanisms.

Unlike traditional productivity apps, RP actively monitors distraction behavior during focus sessions and can trigger consequences when protocol rules are violated.

---

# Core Philosophy

Restraint Protocol is built around one core idea:

> **Discipline should have accountability.**

The system combines:

* AI behavioral intelligence
* real-time Android monitoring
* Web3 financial commitment
* focus session enforcement
* distraction prevention
* child-safe content protection

to create a serious focus protocol rather than a passive timer app.

---

# Features

## Real-Time Focus Enforcement

* Active session monitoring
* Foreground app detection
* Blocked app violation detection
* Real-time distraction response
* Optional strict mode auto-fail

---

## AI-Powered Behavioral Analysis

Powered by Gemma AI.

The AI system analyzes:

* focus patterns
* streak behavior
* session success/failure
* distraction tendencies
* risk levels

and generates:

* pre-session coaching
* motivational insights
* post-session analysis
* long-term discipline summaries

---

## Solana Web3 Integration

Built with Solana Devnet support.

Features:

* SOL staking
* transaction verification
* protocol treasury lockups
* stake refund on success
* stake slash on failure
* wallet integration

Supported modes:

* Simulated Mode
* Solana Mode

---

## Android Enforcement Layer

Native Android integration enables:

* UsageStatsManager monitoring
* Accessibility Service integration
* blocked app detection
* gambling/adult-content blacklist enforcement
* child-safe monitoring
* real-time protocol violation logging

---

## Child-Safe & Anti-Addiction Protection

The blacklist system supports:

* betting sites
* adult content
* addictive social platforms
* harmful content
* unsafe domains/apps

Categories:

* adult_content
* gambling_betting
* violent_extreme
* addictive_social
* child_unsafe
* custom_user_blocked

---

# Tech Stack

## Frontend

* Ionic React
* TypeScript
* Capacitor
* Solana Wallet Adapter

## Backend

* PHP MVC Framework
* MySQL
* JWT Authentication
* RESTful API

## AI

* Gemma AI (Online API / Ollama compatible)

## Blockchain

* Solana Devnet
* @solana/web3.js verification flow

## Android Native Layer

* Java/Kotlin native plugin
* UsageStatsManager
* Accessibility Services

---

# Architecture Overview

```text
Android Device
      ↓
Ionic React Frontend
      ↓
PHP MVC Backend API
      ↓
MySQL Database
      ↓
Gemma AI + Solana Verification
```

---

# Main Backend Modules

## Authentication

* Register/Login
* JWT sessions
* Demo user mode
* Protected routes

## Focus Sessions

* start session
* complete session
* fail session
* session history
* event logging

## AI Engine

* risk analysis
* session coaching
* behavioral summaries

## Solana Logic

* stake verification
* transaction inspection
* treasury validation
* refund/slash flow

## Enforcement Layer

* blocked app detection
* violation events
* strict-mode auto-fail

---

# Project Structure

```text
/frontend
  Ionic React Application

/backend
  PHP MVC API

/android
  Native Android Enforcement Layer
```

---

# Environment Variables

## Backend

```env
APP_ENV=development

DB_HOST=localhost
DB_USER=root
DB_PASSWORD=
DB_NAME=restraint_protocol

JWT_SECRET=
JWT_EXPIRES_IN=7d

SOLANA_CLUSTER=devnet
SOLANA_RPC_URL=https://api.devnet.solana.com
PROTOCOL_TREASURY_WALLET=
TREASURY_SECRET_KEY=

GEMMA_API_URL=
GEMMA_API_KEY=
GEMMA_MODEL=
```

---

# Installation

## Frontend

```bash
npm install
ionic serve
```

---

## Android APK

```bash
ionic build
npx cap sync android
npx cap open android
```

---

## Backend

Configure:

* PHP
* Apache/Nginx
* MySQL

Import database schema.

Configure `.env`.

---

# Demo Flow

1. User opens app
2. User starts focus session
3. User stakes SOL or simulated funds
4. AI analyzes risk
5. Session becomes active
6. Android monitoring begins
7. User violates protocol OR completes session
8. Stake is returned or slashed
9. AI generates post-session insight

---

# Security Features

* JWT authentication
* password hashing
* transaction verification
* replay attack prevention
* session ownership validation
* strict session state transitions
* protected routes
* backend-side stake verification

---

# Current Status

## Implemented

* Frontend UI/UX
* Session flow
* Solana staking verification
* JWT authentication
* AI integration structure
* Android enforcement architecture
* Blacklist system
* Real-time session logic

## In Progress

* deeper Android enforcement
* advanced AI profiling
* production-grade escrow contracts
* parental dashboard
* school mode

---

# Future Roadmap

* Full Solana smart contract escrow
* AI adaptive discipline engine
* Multi-device synchronization
* School/parent dashboards
* Team accountability squads
* Reward economy
* Advanced VPN/DNS filtering
* Protocol tokenomics

---

# Philosophy

> “Freedom without discipline becomes distraction.”

Restraint Protocol exists to help users regain control over attention, focus, and digital behavior through intelligent enforcement and accountability.

---

# License

Private / Proprietary (or update as needed)

---

# Built For

* Solana Frontier Hackathon
* Web3 productivity
* AI accountability systems
* digital wellness
* focus enforcement
* child-safe technology
