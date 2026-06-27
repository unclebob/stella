Feature: Select objects

Background:
  Given a diagram model with stock Stock1 at 100 100
  And stock Stock2 at 300 200
  And converter Converter1 at 100 250

# select-objects-01
Scenario Outline: Click selects an object
  When I click select <kind> <name>
  Then <kind> <name> should be selected

  Examples:
    | kind       | name       |
    | stock      | Stock1     |
    | converter  | Converter1 |

# select-objects-02
Scenario: Clicking a selected object deselects it
  When I click select stock Stock1
  And I click select stock Stock1
  Then stock Stock1 should not be selected
  And the selection count should be 0

# select-objects-03
Scenario: Plain click replaces the current selection
  When I click select stock Stock1
  And I click select stock Stock2
  Then stock Stock1 should not be selected
  And stock Stock2 should be selected

# select-objects-04
Scenario: Shift click adds to the selection
  When I click select stock Stock1
  And I shift click select stock Stock2
  Then stock Stock1 should be selected
  And stock Stock2 should be selected
  And the selection count should be 2

# select-objects-05
Scenario: Shift clicking a selected object removes it from the selection
  When I click select stock Stock1
  And I shift click select stock Stock2
  And I shift click select stock Stock1
  Then stock Stock1 should not be selected
  And stock Stock2 should be selected
  And the selection count should be 1

# select-objects-06
Scenario Outline: Marquee select chooses intersecting objects
  When I marquee select from <x1> <y1> to <x2> <y2>
  Then stock Stock1 should be selected
  And stock Stock2 should not be selected

  Examples:
    | x1 | y1 | x2  | y2  |
    | 50 | 50 | 200 | 200 |

# select-objects-07
Scenario: Escape clears the selection
  When I click select stock Stock1
  And I clear the selection
  Then nothing should be selected

# select-objects-08
Scenario: Selection is disabled while a placement tool is armed
  When I arm the flow placement tool
  And I click select stock Stock1
  Then nothing should be selected