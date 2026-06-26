# mutation-stamp: sha256=be44f71251d771aa47518eb5c79d9e6eb49278b857b427806dc5d56c097ff28b
# acceptance-mutation-manifest-begin
# {"version":1,"tested_at":"2026-06-26T20:31:39.550949Z","feature_name":"Shell diagram canvas","feature_path":"features/shell/canvas.feature","background_hash":"6a881712f550f9c1872c40dd8d8048136add8a7362a1cfd554280dbdc8f31927","implementation_hash":"unknown","scenarios":[]}
# acceptance-mutation-manifest-end

Feature: Shell diagram canvas

Background:
  Given a default shell application

# shell-canvas-01
Scenario: Diagram canvas starts empty
  Then the diagram canvas should be empty