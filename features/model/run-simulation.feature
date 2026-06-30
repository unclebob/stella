Feature: Run simulation

# run-simulation-01
Scenario Outline: Stock flow transfers value between stocks
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I set stock Stock1 initial value to 100
  And I set stock Stock2 initial value to 0
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And stock Stock2 value should be <stock2>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | stock2 | time |
    | 1     | 99     | 1      | 0.1  |
    | 5     | 95     | 5      | 0.5  |

# run-simulation-02
Scenario Outline: Source flow increases stock value
  Given a diagram model with stock Stock1 at 200 150
  And source Source1 at 50 150
  And flow Flow1 runs from source Source1 to stock Stock1
  When I set stock Stock1 initial value to 0
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | time |
    | 1     | 1      | 0.1  |
    | 5     | 5      | 0.5  |

# run-simulation-03
Scenario Outline: Sink flow decreases stock value
  Given a diagram model with stock Stock1 at 200 150
  And sink Sink1 at 400 150
  And flow Flow1 runs from stock Stock1 to sink Sink1
  When I set stock Stock1 initial value to 100
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | time |
    | 1     | 99     | 0.1  |
    | 5     | 95     | 0.5  |

# run-simulation-04
Scenario Outline: Source and sink flows net on stock value
  Given a diagram model with stock Stock1 at 200 150
  And source Source1 at 50 150
  And sink Sink1 at 400 150
  And flow Flow1 runs from source Source1 to stock Stock1
  And flow Flow2 runs from stock Stock1 to sink Sink1
  When I set stock Stock1 initial value to 0
  And I set flow Flow1 rate to 10
  And I set flow Flow2 rate to 5
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | steps | stock1 | time |
    | 1     | 0.5    | 0.1  |
    | 2     | 1      | 0.2  |

# run-simulation-05
Scenario Outline: Zero rate flow leaves stock values unchanged
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I set stock Stock1 initial value to 50
  And I set stock Stock2 initial value to 20
  And I set flow Flow1 rate to 0
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be 50
  And stock Stock2 value should be 20
  And simulation time should be <time>

  Examples:
    | steps | time |
    | 3     | 0.3  |

# run-simulation-06
Scenario: Stock value equals initial value before simulation runs
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  When I set stock Stock1 initial value to 42
  And I set stock Stock2 initial value to 7
  Then stock Stock1 value should be 42
  And stock Stock2 value should be 7
  And simulation time should be 0