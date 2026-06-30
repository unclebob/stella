Feature: Fractional flow rate

Background:
  Given a diagram model with stock Stock1 at 200 150
  And source Source1 at 50 150
  And flow Flow1 runs from source Source1 to stock Stock1
  When I set stock Stock1 initial value to 0

# fractional-flow-rate-01
Scenario Outline: Fractional source flow accumulates stock value
  When I set flow Flow1 rate to <rate>
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And simulation time should be <time>

  Examples:
    | rate | steps | stock1 | time |
    | 0.1  | 10    | 0.1    | 1    |
    | 0.1  | 20    | 0.2    | 2    |

# fractional-flow-rate-02
Scenario Outline: Fractional stock flow transfers between stocks
  Given stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I set stock Stock1 initial value to 1
  And I set stock Stock2 initial value to 0
  And I set flow Flow1 rate to <rate>
  And I run the simulation for <steps> steps
  Then stock Stock1 value should be <stock1>
  And stock Stock2 value should be <stock2>
  And simulation time should be <time>

  Examples:
    | rate | steps | stock1 | stock2 | time |
    | 0.2  | 5     | 0.9    | 0.1    | 0.5  |