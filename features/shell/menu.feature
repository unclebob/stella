Feature: Shell menu bar

Background:
  Given a default shell application

# shell-menu-01
Scenario Outline: Default shell exposes standard menus
  Then the shell menu bar should include <menu>

  Examples:
    | menu |
    | File |
    | Edit |
    | View |
    | Help |

# shell-menu-02
Scenario Outline: Stub menu items are disabled
  Then the shell menu item <item> should be disabled

  Examples:
    | item        |
    | New         |
    | Open...     |
    | Save        |
    | Save As...  |
    | Undo        |
    | Redo        |
    | Cut         |
    | Copy        |
    | Paste       |
    | Zoom In     |
    | Zoom Out    |
    | Reset Zoom  |

# shell-menu-03
Scenario Outline: Essential menu items are enabled
  Then the shell menu item <item> should be enabled

  Examples:
    | item         |
    | Quit         |
    | About Stella |