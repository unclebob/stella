Feature: Place cloud

Background:
  Given an empty diagram model

# place-cloud-01
Scenario Outline: Place source on diagram
  When I arm the source placement tool
  And I place a source at <x> <y>
  Then the diagram should contain source <name>
  And source <name> should be at position <x> <y>

  Examples:
    | x  | y   | name    |
    | 50 | 150 | Source1 |

# place-cloud-02
Scenario Outline: Place sink on diagram
  When I arm the sink placement tool
  And I place a sink at <x> <y>
  Then the diagram should contain sink <name>
  And sink <name> should be at position <x> <y>

  Examples:
    | x   | y   | name  |
    | 400 | 150 | Sink1 |

# place-cloud-03
Scenario: Arming source without a canvas click places nothing
  When I arm the source placement tool
  Then the diagram source count should be 0

# place-cloud-04
Scenario Outline: Source placement disarms the tool
  When I arm the source placement tool
  And I place a source at <x> <y>
  Then the source placement tool should be disarmed

  Examples:
    | x  | y   |
    | 50 | 150 |

# place-cloud-05
Scenario Outline: Second source requires re-arming the placement tool
  When I arm the source placement tool
  And I place a source at <x1> <y1>
  And I arm the source placement tool
  And I place a source at <x2> <y2>
  Then the diagram should contain source <name2>

  Examples:
    | x1 | y1  | x2  | y2  | name2   |
    | 50 | 150 | 450 | 150 | Source2 |