# mutation-stamp: sha256=c57890d76db0284e39196fc5ab5e43e605b24fabf43eeca898823e181b4196da
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T20:31:40.615683Z","feature_name":"Shell menu bar","feature_path":"features/shell/menu.feature","background_hash":"6a881712f550f9c1872c40dd8d8048136add8a7362a1cfd554280dbdc8f31927","implementation_hash":"unknown","scenarios":[{"index":2,"name":"Essential menu items are enabled","scenario_hash":"311cb0d7720b1bf9904830821eda8fc4627f0bcc01477a4263e5a6882dea2df1","mutation_count":2,"result":{"Total":2,"Killed":2,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:19:43.266129Z"},{"index":0,"name":"Default shell exposes standard menus","scenario_hash":"36f8df5dbc783cb55d823455825d38d834ed4a77b554d4b89dfda65b9c80f9ce","mutation_count":4,"result":{"Total":4,"Killed":4,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:18:03.550099Z"},{"index":1,"name":"Stub menu items are disabled","scenario_hash":"e2b263ca32ddb7f268221e6f348abac7886f8057932587efca1b665475b5860c","mutation_count":12,"result":{"Total":12,"Killed":12,"Survived":0,"Errors":0},"tested_at":"2026-06-26T20:18:03.550099Z"}]}
# acceptance-mutation-manifest-end

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