# mutation-stamp: sha256=2a77606d7a9298d2c9f433bbd9dc0eaae2694bdca9f7b2f1af9efc64f54dd4eb
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-30T16:28:54.179592Z","feature_name":"Converter flow rate","feature_path":"features/model/converter-flow-rate.feature","background_hash":"32c473a256e6af6144383abd9749321b33f8b08bf9f60111f88add4cdf127f60","implementation_hash":"unknown","scenarios":[{"index":5,"name":"Exponent and square root in formula","scenario_hash":"07dd20e2774a853bf50133c62f569651caf19dd0e0fb19b80d9557f17c5657f6","mutation_count":6,"result":{"Total":6,"Killed":6,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:28:54.179592Z"},{"index":11,"name":"Fractional formula uses rational literal","scenario_hash":"66fa431754cdab5294a61cda0cba7dbe657c62b5a6e0f00ec38af7d70099c279","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:28:54.179592Z"},{"index":12,"name":"Simulation transfers at computed constant rate","scenario_hash":"ce260dc95d70183733c997d11235286790d390f773b36d841c00a8aefb15abf1","mutation_count":6,"result":{"Total":6,"Killed":6,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:28:54.179592Z"},{"index":13,"name":"Computed rate tracks changing stock during simulation","scenario_hash":"b1d346e5c45e76a72373387f5283dda825902d47ecd236b663a7ba5af725027a","mutation_count":1,"result":{"Total":1,"Killed":1,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:28:54.179592Z"},{"index":2,"name":"Connected stock named value scales in formula","scenario_hash":"5540abfe25f9cd11290adf783858230f04305a8819a7e72a8df2a404cbc8bef6","mutation_count":6,"result":{"Total":6,"Killed":6,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:28:05.648061Z"},{"index":3,"name":"Two connected stocks sum in formula","scenario_hash":"095c5a6dd5d7754bdfdd181a697466fbdc7b0721f68703c89c073fe6a6f8cd61","mutation_count":3,"result":{"Total":3,"Killed":3,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:28:05.648061Z"},{"index":4,"name":"Parentheses group operations in formula","scenario_hash":"340bbced69f1407796282d2e445b7bc422eb13f16d2eae92f879f141d1e94576","mutation_count":8,"result":{"Total":8,"Killed":8,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:28:05.648061Z"},{"index":1,"name":"Constant formula sets converter value and flow rate","scenario_hash":"e32bcaa7712d9f8a5cb866705053ef014b7f7abc8f938e7701b01cbaaa054e3e","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-30T16:24:22.485746Z"}]}
# acceptance-mutation-manifest-end

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
Scenario Outline: Exponent and square root in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | formula      | value |
    | 3      | Stock1 ^ 2   | 9     |
    | 4      | sqrt(Stock1) | 2     |

# converter-flow-rate-06a
Scenario: Sine function in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to 0
  And I set converter Converter1 formula to sin(Stock1)
  Then converter Converter1 value should be 0
  And flow Flow1 rate should be 0

# converter-flow-rate-06b
Scenario: Cosine function in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to 0
  And I set converter Converter1 formula to cos(Stock1)
  Then converter Converter1 value should be 1
  And flow Flow1 rate should be 1

# converter-flow-rate-06c
Scenario: Tangent function in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to 0
  And I set converter Converter1 formula to tan(Stock1)
  Then converter Converter1 value should be 0
  And flow Flow1 rate should be 0

# converter-flow-rate-06d
Scenario: Natural log function in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to 1
  And I set converter Converter1 formula to ln(Stock1)
  Then converter Converter1 value should be 0
  And flow Flow1 rate should be 0

# converter-flow-rate-06e
Scenario: Exponential function in formula
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to 0
  And I set converter Converter1 formula to exp(Stock1)
  Then converter Converter1 value should be 1
  And flow Flow1 rate should be 1

# converter-flow-rate-07
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

# converter-flow-rate-08
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

# converter-flow-rate-09
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

# converter-flow-rate-10
Scenario Outline: Reject invalid converter formula
  When I set converter Converter1 formula to <formula>
  Then the converter edit should be rejected
  And connector Connector1 should have no formula

  Examples:
    | formula        |
    | Stock1 & 0.1   |
    | Missing * 2    |
    | foo(1)         |

# converter-flow-rate-11
Scenario: Reject stock name without stock to converter link
  When I set converter Converter1 formula to Stock1 * 0.1
  Then the converter edit should be rejected
  And connector Connector1 should have no formula

# converter-flow-rate-12
Scenario: Converter without flow link shows zero value
  Given converter Converter2 at 300 250
  Then converter Converter2 value should be 0
  And converter Converter2 canvas value should be 0

# converter-flow-rate-13
Scenario Outline: Unary math functions on constants
  When I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | formula    | value |
    | abs(-5)    | 5     |
    | floor(3.7) | 3     |
    | ceil(3.2)  | 4     |
    | round(3.6) | 4     |
    | log(100)   | 2     |
    | pi         | 3.1   |
    | e          | 2.7   |

# converter-flow-rate-14
Scenario Outline: abs log and round with connected stock
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | formula           | value |
    | 7      | abs(Stock1 - 10)  | 3     |
    | 100    | log(Stock1)         | 2     |
    | 3.6    | round(Stock1)       | 4     |

# converter-flow-rate-15
Scenario Outline: min and max with two connected stocks
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  And connector Connector3 runs from stock Stock2 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set stock Stock2 initial value to <stock2>
  And I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | stock2 | formula            | value |
    | 30     | 20     | min(Stock1, Stock2) | 20   |
    | 30     | 20     | max(Stock1, Stock2) | 30   |

# converter-flow-rate-16
Scenario Outline: Random literal in unit interval
  When I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be at least <min>
  And converter Converter1 value should be less than <max>
  And flow Flow1 rate should be at least <min>
  And flow Flow1 rate should be less than <max>

  Examples:
    | formula | min | max |
    | ?       | 0   | 1   |
    | ? * 10  | 0   | 10  |

# converter-flow-rate-17
Scenario Outline: Inverse trig mod and percent on constants
  When I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | formula  | value |
    | asin(0)  | 0     |
    | acos(1)  | 0     |
    | atan(1)  | 0.8   |
    | mod(7, 3) | 1    |
    | 7 % 3    | 1     |

# converter-flow-rate-18
Scenario Outline: sign and clamp on constants
  When I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | formula           | value |
    | sign(-5)          | -1    |
    | sign(0)           | 0     |
    | sign(12)          | 1     |
    | clamp(15, 0, 10)  | 10    |
    | clamp(-2, 0, 10)  | 0     |
    | clamp(5, 0, 10)   | 5     |

# converter-flow-rate-19
Scenario Outline: hypot atan2 and mod with connected stocks
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  And connector Connector3 runs from stock Stock2 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set stock Stock2 initial value to <stock2>
  And I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | stock2 | formula              | value |
    | 3      | 4      | hypot(Stock1, Stock2) | 5    |
    | 3      | 4      | atan2(Stock1, Stock2) | 0.6  |
    | 7      | 3      | mod(Stock1, Stock2)   | 1    |

# converter-flow-rate-20
Scenario Outline: clamp bounds connected stock value
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set converter Converter1 formula to clamp(Stock1, 0, 10)
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | value |
    | 100    | 10    |
    | 3      | 3     |

# converter-flow-rate-21
Scenario Outline: Unary minus on constants
  When I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | formula | value |
    | -5      | -5    |
    | -3.5    | -3.5  |

# converter-flow-rate-22
Scenario Outline: Unary minus on connected stock values
  Given connector Connector2 runs from stock Stock1 to converter Converter1
  When I set stock Stock1 initial value to <stock1>
  And I set converter Converter1 formula to <formula>
  Then converter Converter1 value should be <value>
  And flow Flow1 rate should be <value>

  Examples:
    | stock1 | formula         | value |
    | 7      | -Stock1         | -7    |
    | 3      | -(Stock1 + 2)  | -5    |