Feature: Stock thermometer

Background:
  Given a diagram model with stock Stock1 at 200 150

# stock-thermometer-01
Scenario Outline: Thermometer fill reflects initial value on bounded scale
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  And I set stock Stock1 initial value to <value>
  Then stock Stock1 canvas thermometer fill width should be <fill_width>
  And stock Stock1 canvas thermometer fill color should be light blue

  Examples:
    | value | fill_width |
    | 0     | 0          |
    | 50    | 36         |
    | 100   | 72         |

# stock-thermometer-02
Scenario Outline: Thermometer fill reflects initial value on unbounded scale
  When I clear stock Stock1 maximum
  And I set stock Stock1 initial value to <value>
  Then stock Stock1 canvas thermometer fill width should be <fill_width>
  And stock Stock1 canvas thermometer fill color should be light blue

  Examples:
    | value | fill_width |
    | 0     | 0          |
    | 25    | 18         |

# stock-thermometer-03
Scenario: Stock icon layout places name above thermometer
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  Then stock Stock1 canvas name should be at top
  And stock Stock1 canvas thermometer should be below name
  And stock Stock1 canvas minimum should be 0
  And stock Stock1 canvas maximum should be 100

# stock-thermometer-04
Scenario: Thermometer track is a horizontal rectangle
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  Then stock Stock1 canvas thermometer track width should be 72
  And stock Stock1 canvas thermometer track height should be 8

# stock-thermometer-05
Scenario Outline: Thermometer fill updates after simulation
  Given a diagram model with stock Stock1 at 100 100
  And source Source1 at 50 100
  And flow Flow1 runs from source Source1 to stock Stock1
  When I set stock Stock1 minimum to 0
  And I set stock Stock1 maximum to 100
  And I set stock Stock1 initial value to 0
  And I set flow Flow1 rate to 10
  And I run the simulation for <steps> steps
  Then stock Stock1 canvas thermometer fill width should be <fill_width>
  And stock Stock1 canvas thermometer fill color should be light blue

  Examples:
    | steps | fill_width |
    | 1     | 1          |
    | 5     | 4          |