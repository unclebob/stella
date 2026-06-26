Feature: Place stock

Background:
  Given an empty diagram model

# place-stock-01
Scenario Outline: Place first stock on empty diagram
  When I arm the stock placement tool
  And I place a stock at <x> <y>
  Then the diagram should contain stock <name>
  And stock <name> should be at position <x> <y>
  And stock <name> initial value should be <value>

  Examples:
    | x   | y   | name   | value |
    | 200 | 150 | Stock1 | 0     |

# place-stock-02
Scenario Outline: Second stock requires re-arming the placement tool
  When I arm the stock placement tool
  And I place a stock at <x1> <y1>
  And I arm the stock placement tool
  And I place a stock at <x2> <y2>
  Then the diagram should contain stock <name1>
  And the diagram should contain stock <name2>
  And stock <name2> should be at position <x2> <y2>
  And stock <name2> initial value should be 0

  Examples:
    | x1  | y1  | x2  | y2  | name1  | name2  |
    | 100 | 100 | 300 | 200 | Stock1 | Stock2 |

# place-stock-03
Scenario: Arming without a canvas click places no stock
  When I arm the stock placement tool
  Then the diagram stock count should be 0

# place-stock-04
Scenario Outline: Placement disarms the stock tool
  When I arm the stock placement tool
  And I place a stock at <x> <y>
  Then the stock placement tool should be disarmed

  Examples:
    | x   | y   |
    | 200 | 150 |