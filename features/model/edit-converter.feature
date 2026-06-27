Feature: Edit converter

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  And converter Converter1 at 100 250
  And connector Connector1 runs from converter Converter1 to flow Flow1

# edit-converter-01
Scenario Outline: Rename converter
  When I set converter <name> name to <new_name>
  Then the diagram should contain converter <new_name>
  And converter <new_name> canvas name should be <new_name>
  And connector <connector> should run from converter <new_name> to flow Flow1

  Examples:
    | name       | new_name | connector  |
    | Converter1 | Growth   | Connector1 |

# edit-converter-02
Scenario Outline: Set connector formula from converter editor
  When I set converter <name> formula to <formula>
  Then connector <connector> formula should be <formula>
  And connector <connector> canvas formula should be <formula>

  Examples:
    | name       | connector  | formula      |
    | Converter1 | Connector1 | Stock1 * 0.1 |

# edit-converter-03
Scenario: Reject duplicate converter name
  Given converter Converter2 at 300 250
  When I set converter Converter1 name to Converter2
  Then the converter edit should be rejected
  And the diagram should contain converter Converter1
  And the diagram should contain converter Converter2

# edit-converter-04
Scenario: Reject formula edit without converter to flow connector
  Given converter Converter2 at 300 250
  When I set converter Converter2 formula to Stock1 * 0.2
  Then the converter edit should be rejected
  And connector Connector1 should have no formula