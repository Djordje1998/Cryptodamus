# Cryptodamus

A cryptocurrency price prediction application that implements pattern recognition techniques to analyze historical price data. Built in Clojure with a Swing GUI interface.

## Overview

This application analyzes historical cryptocurrency price patterns to identify similar sequences that may indicate future price movements. The system uses statistical pattern matching to find historical periods with similar price behavior and projects their outcomes forward.

## Algorithm Implementation

### Core Pattern Recognition Process

1. **Data Normalization (Comparator Functions)**
   
   The system supports multiple mathematical approaches for normalizing price data before pattern matching. Each comparator function transforms raw price sequences into comparable formats:

   - **delta-avg**: Calculates percentage deviation from sequence average. Best for identifying patterns where prices oscillate around a mean value. Handles zero averages by using maximum absolute value as reference.
   
   - **percentage-change**: Computes period-over-period percentage changes between consecutive prices. Ideal for capturing momentum and trend patterns. Uses signed percentages to preserve directional information.
   
   - **log-returns**: Calculates logarithmic returns (ln(price[t]/price[t-1])). Preferred for volatility analysis and when dealing with exponential price movements. Mathematically stable for large price changes.
   
   - **price-differences**: Simple absolute differences between consecutive prices. Most intuitive approach, good for linear price movements and when actual price magnitudes matter.
   
   - **relative-percent-change**: Percentage changes relative to the first element in the sequence. Useful for analyzing cumulative performance from a baseline starting point.
   
   - **zero-anchoring**: Subtracts the first price from all prices in the sequence. Creates a zero-baseline view, emphasizing relative movements rather than absolute values.

2. **Pattern Matching**
   - Chunk Window (CW): Size of historical patterns to compare
   - Skip Window (SW): Step size when sliding through historical data
   - Significance threshold: Maximum allowed difference for pattern matches
   - Delta difference analysis: Compares normalized patterns using statistical differences
   - Pattern scoring: Assigns numerical scores (0-100) based on similarity

3. **Price Prediction**
   - Identifies top N most similar historical patterns
   - Projects future prices using percentage changes from historical outcomes
   - Prediction Window (PW): Number of future data points to predict
   - Returns multiple predictions ranked by pattern match quality

4. **Validation**
   - Backtesting using train/test data splits
   - Calculates mean absolute percentage error and maximum error
   - Evaluates accuracy within specified tolerance ranges
   - Parameter optimization through grid search

## Implementation Details

### Data Source
- CoinGecko API for historical price data
- Supported cryptocurrencies: Bitcoin, Ethereum, Monero
- Configurable date ranges with timestamp validation
- API key authentication through configuration file

### User Interface
- JFreeChart-based price visualization with interactive controls
- Calendar-based date selection with validation
- Real-time chart updates with multiple prediction trajectories
- Optional overlay of actual future prices for validation
- Slider-based parameter configuration for intuitive adjustment
- Finding optimal parameters (training) for prediction, using historical data

#### Parameters
- **CW (Chunk Window)**: Size of historical price sequences to compare (3-30). Larger values capture longer-term patterns but require more data
- **SW (Skip Window)**: Number of data points to advance when sliding through historical data (3-30). Smaller values provide more overlapping comparisons
- **PW (Predict Window)**: Number of future price points to forecast (5-500). Limited by available historical outcomes
- **Significance**: Maximum allowed statistical difference for pattern matches (0.1-3.0). Lower values require more similar patterns
- **Number of Predictions**: How many top-ranked predictions to display (1-10), sorted by pattern match quality
- **Tolerance**: Accuracy tolerance percentage for optimization (0.1-5.0%)
- **Comparator Function**: Which mathematical normalization method to use (see Data Normalization section above)

### Technical Features
- Automatic parameter optimization
- Multiple mathematical normalization approaches
- Backtesting framework for accuracy evaluation
- Primitive array operations for performance
- Configurable numerical precision

## Setup

### Requirements
- Java 8+
- Leiningen
- CoinGecko API key

### Configuration
1. Copy `resources/config.edn.example` to `resources/config.edn`
2. Add API key:
```clojure
{:api-keys {:coingecko "your-api-key"}}
```

### Running
```bash
# Development
lein run

# Run tests
lein midje
```

## Usage

1. Select cryptocurrency (Bitcoin, Ethereum, Monero)
2. Set date range for historical data
3. Configure parameters:
   - CW (Chunk Window): Pattern size
   - SW (Skip Window): Step size
   - PW (Predict Window): Future points
   - Significance (Sig): Similarity threshold
   - Number of Predictions (NoP): Output count
   - Comparator Function: Select normalization method
4. Generate predictions or use automatic optimization

### Chart Interpretation
- Blue: Historical prices
- Red: Predicted trajectories (with confidence scores)
- Green: Actual future prices (validation mode)

## Architecture

### Modules
- `core.clj`: Prediction algorithms and mathematical functions
- `fetch.clj`: CoinGecko API integration
- `utils.clj`: Date/time utilities and configuration
- `gui.clj`: Swing GUI interface

### Key Components
- Pattern recognition using statistical analysis
- Delta analysis for normalized sequence comparison
- Scoring system combining average and maximum differences
- Backtesting framework for validation

## Limitations
- Requires minimum historical data (chunk window size)
- Accuracy varies with market conditions
- Limited to three supported cryptocurrencies
- Subject to API rate limits

## License

Copyright © 2025 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
