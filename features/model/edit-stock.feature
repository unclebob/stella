Feature: Edit stock

Background:
  Given a diagram model with stock Stock1 at 200 150

# edit-stock-01
Scenario Outline: Rename stock
  When I set stock <name> name to <new_name>
  Then the diagram should contain stock <new_name>
  And stock <new_name> canvas name should be <new_name>
  And stock <new_name> initial value should be 0

  Examples:
    | name   | new_name |
    | Stock1 | Cats     |

# edit-stock-02
Scenario Outline: Edit stock initial value
  When I set stock <name> initial value to <value>
  Then stock <name> initial value should be <value>

  Examples:
    | name   | value |
    | Stock1 | 25    |

# edit-stock-03
Scenario Outline: Set stock value bounds
  When I set stock <name> minimum to <min>
  And I set stock <name> maximum to <max>
  Then stock <name> minimum should be <min>
  And stock <name> maximum should be <max>
  And stock <name> canvas minimum should be <min>
  And stock <name> canvas maximum should be <max>

  Examples:
    | name   | min | max |
    | Stock1 | 0   | 100 |

# edit-stock-04
Scenario: Clear stock maximum bound
  When I set stock Stock1 maximum to 50
  And I clear stock Stock1 maximum
  Then stock Stock1 should have no maximum
  And stock Stock1 should display no maximum on canvas

# edit-stock-05
Scenario: Reject duplicate stock name
  Given a diagram model with stock Stock2 at 300 200
  When I set stock Stock1 name to Stock2
  Then the stock edit should be rejected
  And the diagram should contain stock Stock1
  And the diagram should contain stock Stock2

# edit-stock-06
Scenario Outline: Reject initial value below minimum
  When I set stock <name> minimum to <min>
  And I set stock <name> initial value to <value>
  Then the stock edit should be rejected
  And stock <name> initial value should be 0

  Examples:
    | name   | min | value |
    | Stock1 | 10  | 5     |

# edit-stock-07
Scenario Outline: Reject initial value above maximum
  When I set stock <name> maximum to <max>
  And I set stock <name> initial value to <value>
  Then the stock edit should be rejected
  And stock <name> initial value should be 0

  Examples:
    | name   | max | value |
    | Stock1 | 40  | 50    |