# Cryptodamus

A cryptocurrency price prediction tool that uses pattern recognition in historical price data to forecast future price movements.

## Overview

Cryptodamus analyzes historical price patterns to identify similar trends that might predict future price movements. Named after Nostradamus, it attempts to see the future through patterns of the past.

## Core Concepts

The prediction engine works through several key steps:

1. **Pattern Analysis**
   - Calculates percentage deviations from moving averages
   - Identifies similar patterns using delta analysis
   - Scores pattern matches based on similarity

2. **Price Prediction**
   - Ranks historical patterns by match quality
   - Projects future prices using percentage changes
   - Returns multiple possible price trajectories

3. **Accuracy Evaluation**
   - Measures prediction accuracy against actual prices
   - Calculates mean and maximum errors
   - Reports accuracy within specified tolerance

## Usage

FIXME: explanation

    $ java -jar cryptodamus-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
