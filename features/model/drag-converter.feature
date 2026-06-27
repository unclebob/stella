Feature: Drag converter

Background:
  Given converter Converter1 at 100 250

# drag-converter-01
Scenario Outline: Move converter to new position
  When I move converter Converter1 to <x> <y>
  Then converter Converter1 should be at position 220 300
  And converter Converter1 canvas position should be 220 300

  Examples:
    | x   | y   |
    | 220 | 300 |

# drag-converter-02
Scenario: Moving converter does not create another converter
  When I move converter Converter1 to 150 200
  Then the diagram converter count should be 1
  And the diagram should contain converter Converter1

# drag-converter-03
Scenario: Moving converter preserves connector endpoints
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  And connector Connector1 runs from converter Converter1 to flow Flow1
  When I move converter Converter1 to 50 180
  Then converter Converter1 should be at position 50 180
  And connector Connector1 should run from converter Converter1 to flow Flow1

# drag-converter-04
Scenario Outline: Move one converter without moving the other
  Given converter Converter2 at 300 250
  When I move converter <name> to <x> <y>
  Then converter Converter1 should be at position 120 90
  And converter Converter2 should be at position 300 250

  Examples:
    | name       | x   | y   |
    | Converter1 | 120 | 90  |