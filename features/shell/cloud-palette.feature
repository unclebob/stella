Feature: Cloud palette

Background:
  Given a default shell application
  And an empty diagram model

# cloud-palette-01
Scenario: Arming highlights the source palette tool
  When I arm the source placement tool
  Then the palette tool Source should be active
  And the palette tool Sink should be inactive

# cloud-palette-02
Scenario: Arming highlights the sink palette tool
  When I arm the sink placement tool
  Then the palette tool Sink should be active
  And the palette tool Source should be inactive

# cloud-palette-03
Scenario Outline: Placing a source resets the palette
  When I arm the source placement tool
  And I place a source at <x> <y>
  Then the diagram should contain source Source1
  And the source placement tool should be disarmed
  And no palette tool should be active

  Examples:
    | x  | y   |
    | 50 | 150 |

# cloud-palette-04
Scenario Outline: Placing a sink resets the palette
  When I arm the sink placement tool
  And I place a sink at <x> <y>
  Then the diagram should contain sink Sink1
  And the sink placement tool should be disarmed
  And no palette tool should be active

  Examples:
    | x   | y   |
    | 400 | 150 |

# cloud-palette-05
Scenario: Switching palette tools moves cloud highlight
  When I arm the source placement tool
  And I arm the sink placement tool
  Then the palette tool Sink should be active
  And the palette tool Source should be inactive