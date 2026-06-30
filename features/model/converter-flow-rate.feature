Feature: Converter flow rate

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  And converter Converter1 at 100 250
  And connector Connector1 runs from converter Converter1 to flow Flow1

# converter-flow-rate-01
Scenario: Connected converter without formula shows zero
  Then converter Converter1 value should be 0
  And flow Flow1 rate should be 0
  And converter Converter1 canvas value should be 0

# converter-flow-rate-02
Scenario Outline: Constant formula sets converter value and flow rate
  When I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>
  And converter Converter1 canvas value should be <value>

  Examples:
    | formula | value |
    | 5       | 5     |

# converter-flow-rate-03
Scenario Outline: Connected stock named value scales in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | formula      | value |
    | 100    | Stock1 * 0.1 | 10    |
    | 50     | Stock1 * 0.1 | 5     |

# converter-flow-rate-04
Scenario Outline: Two connected stocks sum in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  And connector Connector3 runs from stock Stock2 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set stock Stock2 initial value to <stock2>
  And I set converter Converter1 formula to Stock1 + Stock2
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | stock2 | value |
    | 30     | 20     | 50    |

# converter-flow-rate-05
Scenario Outline: Parentheses group operations in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  And connector Connector3 runs from stock Stock2 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set stock Stock2 initial value to <stock2>
  And I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | stock2 | formula              | value |
    | 100    | 50     | (Stock1 + Stock2) * 0.1 | 15 |
    | 80     | 20     | (Stock1 - Stock2) / 2    | 30 |

# converter-flow-rate-06
Scenario Outline: Fractional formula uses rational literal
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to 1
  And I set converter Converter1 formula to Stock1 * 1/2
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>
  And converter Converter1 canvas value should be <value>

  Examples:
    | value |
    | 0.5   |

# converter-flow-rate-07
Scenario Outline: Simulation transfers at computed constant rate
  When I set stock Stock1 initial value to 100
  And I set stock Stock2 initial value to 0
  And I set converter Converter1 formula to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And stock Stock2 value should be <stock2>

  Examples:
    | steps | stock1 | stock2 |
    | 1     | 99     | 1      |
    | 5     | 95     | 5      |

# converter-flow-rate-08
Scenario Outline: Computed rate tracks changing stock during simulation
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to 100
  And I set stock Stock2 initial value to 0
  And I set converter Converter1 formula to Stock1 * 0.1
  And I run the simulation for 1 steps
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | value |
    | 9.9   |

# converter-flow-rate-09
Scenario Outline: Reject invalid converter formula
  When I set converter Converter1 formula to <formula>
  Then the converter edit should be rejected
  And connector Connector1 should have no formula

  Examples:
    | formula      |
    | Stock1 & 0.1 |
    | Missing * 2  |

# converter-flow-rate-10
Scenario: Reject stock name without stock to converter link
  When I set converter Converter1 formula to Stock1 * 0.1
  Then the converter edit should be rejected
  And connector Connector1 should have no formula

# converter-flow-rate-11
Scenario: Converter without flow link shows zero value
  Given converter Converter2 at 300 250
  Then converter Converter2 value should be 0
  And converter Converter2 canvas value should be 0