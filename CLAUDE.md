# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a hedge fund-style custom index strategy application that combines macro-level signals with micro-level stock selection and options-based hedging to build a robust long/short trading framework. The core component is a rule-based signal index that determines market regime (risk-on vs. risk-off) and adjusts exposure accordingly.

## Architecture and Structure

The application is built using Spring Boot and follows a layered architecture:

1. **Data Layer**: Handles data ingestion from various sources including Yahoo Finance, Coinbase, and other financial data providers
2. **Strategy Layer**: Contains multiple trading strategies including:
   - Market regime detection strategies (VIX, VVIX, SKEW, etc.)
   - Equity selection strategies (Beta, PEG ratio, revenue growth, etc.)
   - Hedging strategies using options and volatility products
3. **Service Layer**: Provides business logic for trading, data management, and analysis
4. **Repository Layer**: Manages data persistence using JPA/Hibernate with H2 database
5. **Web Layer**: Exposes REST endpoints and JSP views for the web interface

## Key Components

- **HighLanderStrategy**: The main composite strategy that combines hedge index and beta strategies
- **HedgeIndexStrategy**: Implements the market regime detection logic based on macro indicators
- **DataManager classes**: Handle data ingestion from different sources (Yahoo, Coinbase, etc.)
- **BarSeriesService**: Manages time series data for technical analysis
- **TradingAccountService**: Handles trading account operations and transactions

## How to Build and Run

### Prerequisites
- Java 17
- Maven 3.x
- Git

### Build Process
1. Run the build script: `./build-frosk.sh`
   - This will build the gdax-java dependencies and then build frosk-analyzer
   - The script also starts the application automatically

### Manual Build
1. `mvn clean install -DskipTests`
2. `mvn spring-boot:run`

### Configuration
The main configuration is in `src/main/resources/application.properties`. Key settings include:
- Database connection (H2 in-memory database)
- Data source URLs and credentials
- Strategy parameters and thresholds
- Coinbase API settings

## Testing

Tests are located in `src/test/java/nu/itark/frosk/` and cover:
- Individual strategy components
- Data managers
- Service layer functionality
- Repository operations

To run tests:
- `mvn test` (or `mvn surefire:test`)

## Development Workflow

1. Make changes to source code in `src/main/java/`
2. Run tests to verify changes
3. Build and run the application to test functionality
4. Use the H2 console at `/h2-console` for database inspection

## Important Notes

- The application uses the ta4j library for technical analysis
- It integrates with Coinbase API for market data and trading
- The system is designed to run with Yahoo Finance data for equity metrics
- Database is H2 by default, but can be configured for other databases
- The application includes both web interface (JSP) and REST API components