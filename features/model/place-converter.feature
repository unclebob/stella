Feature: Place converter

Background:
  Given an empty diagram model

# place-converter-01
Scenario Outline: Place converter on diagram
  When I arm the converter placement tool
  And I place a converter at <x> <y>
  Then the diagram should contain converter <name>
  And converter <name> should be at position <x> <y>
  And converter <name> value should be <value>

  Examples:
    | x   | y   | name       | value |
    | 100 | 250 | Converter1 | 0     |

# place-converter-02
Scenario: Arming converter without a canvas click places nothing
  When I arm the converter placement tool
  Then the diagram converter count should be 0

# place-converter-03
Scenario Outline: Converter placement disarms the tool
  When I arm the converter placement tool
  And I place a converter at <x> <y>
  Then the converter placement tool should be disarmed

  Examples:
    | x   | y   |
    | 100 | 250 |

# place-converter-04
Scenario Outline: Second converter requires re-arming the placement tool
  When I arm the converter placement tool
  And I place a converter at <x1> <y1>
  And I arm the converter placement tool
  And I place a converter at <x2> <y2>
  Then the diagram should contain converter <name2>

  Examples:
    | x1  | y1  | x2  | y2  | name2      |
    | 100 | 250 | 300 | 250 | Converter2 |