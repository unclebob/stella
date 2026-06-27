Feature: Drag stock

Background:
  Given a diagram model with stock Stock1 at 100 100

# drag-stock-01
Scenario Outline: Move stock to new position
  When I move stock Stock1 to <x> <y>
  Then stock Stock1 should be at position 250 180
  And stock Stock1 canvas position should be 250 180

  Examples:
    | x   | y   |
    | 250 | 180 |

# drag-stock-02
Scenario: Moving stock does not create another stock
  When I move stock Stock1 to 200 150
  Then the diagram stock count should be 1
  And the diagram should contain stock Stock1

# drag-stock-03
Scenario: Moving stock preserves flow endpoints
  Given a diagram model with stock Stock2 at 300 200
  And flow Flow1 runs from stock Stock1 to stock Stock2
  When I move stock Stock1 to 50 80
  Then stock Stock1 should be at position 50 80
  And flow Flow1 should run from stock Stock1 to stock Stock2

# drag-stock-04
Scenario Outline: Move one stock without moving the other
  Given a diagram model with stock Stock2 at 300 200
  When I move stock <name> to <x> <y>
  Then stock Stock1 should be at position 120 90
  And stock Stock2 should be at position 300 200

  Examples:
    | name   | x   | y   |
    | Stock1 | 120 | 90  |