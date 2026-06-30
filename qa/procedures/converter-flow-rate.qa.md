# QA: Converter flow rate

**Task:** `converter-flow-rate`  
**Suite:** converter-flow-rate

Verify stock-to-converter connectors supply named values for converter formulas, the computed rate appears centered on the converter icon, drives the connected flow rate, and transfers stock during simulation.

## Preconditions

- Display available (headed UI).
- `connectors`, `edit-converter`, `edit-stock`, and `run-simulation` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stocks and flow per `connect-flow` QA (stocks at `:center` and offset `(+150, +50)`).
3. Place converter: click palette `Converter`, click in region `:canvas` at offset `(-50, +80)` from `:center`.
4. Connect stock to converter: click palette `Connector`, click `:stock "Stock1"`, click `:converter "Converter1"`.
5. Wait until element `:connector "Connector2"` is visible (timeout 2 seconds).
6. Connect converter to flow: click palette `Connector`, click `:converter "Converter1"`, click `:flow "Flow1"`.
7. Wait until element `:connector "Connector1"` is visible (timeout 2 seconds).
8. Assert element `:converter "Converter1"` shows `0` centered on the icon (not the name label below).
9. Right-click element `:converter "Converter1"`.
10. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
11. Type `Stock1 * 0.1` into dialog field `Formula`.
12. Click `OK` on the `Edit Converter` dialog.
13. Double-click element `:stock "Stock1"`, set `Initial value` to `100`, confirm the edit dialog.
14. Assert element `:converter "Converter1"` shows `10` centered on the icon.
15. Assert element `:flow "Flow1"` shows rate `10`.
16. Assert element `:connector "Connector1"` shows `Stock1 * 0.1`.
17. Assert element `:stock "Stock2"` shows value `0`.
18. Click visible text `Step` once.
19. Assert element `:stock "Stock1"` shows value `99`.
20. Assert element `:stock "Stock2"` shows value `1`.
21. Assert element `:converter "Converter1"` shows `9.9` centered on the icon.
22. Assert element `:flow "Flow1"` shows rate `9.9`.
23. Right-click element `:converter "Converter1"`.
24. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
25. Type `(Stock1 + 10) / 2` into dialog field `Formula`.
26. Click `OK` on the `Edit Converter` dialog.
27. Assert element `:converter "Converter1"` shows `54.5` centered on the icon (Stock1 is 99 after step 18).
28. Right-click element `:converter "Converter1"`.
29. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
30. Type `sqrt(Stock1)` into dialog field `Formula`.
31. Click `OK` on the `Edit Converter` dialog.
32. Assert element `:converter "Converter1"` shows `9.9` centered on the icon (`sqrt(99)`).
33. Right-click element `:converter "Converter1"`.
34. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
35. Double-click element `:stock "Stock1"`, set `Initial value` to `3`, confirm the edit dialog.
36. Type `Stock1 ^ 2` into dialog field `Formula`.
37. Click `OK` on the `Edit Converter` dialog.
38. Assert element `:converter "Converter1"` shows `9` centered on the icon.
39. Right-click element `:converter "Converter1"`.
40. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
41. Type `abs(Stock1 - 10)` into dialog field `Formula`.
42. Click `OK` on the `Edit Converter` dialog.
43. Assert element `:converter "Converter1"` shows `7` centered on the icon.
44. Right-click element `:converter "Converter1"`.
45. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
46. Type `floor(3.7)` into dialog field `Formula`.
47. Click `OK` on the `Edit Converter` dialog.
48. Assert element `:converter "Converter1"` shows `3` centered on the icon.
49. Right-click element `:converter "Converter1"`.
50. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
51. Type `hypot(3, 4)` into dialog field `Formula`.
52. Click `OK` on the `Edit Converter` dialog.
53. Assert element `:converter "Converter1"` shows `5` centered on the icon.
54. Right-click element `:converter "Converter1"`.
55. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
56. Type `clamp(Stock1, 0, 10)` into dialog field `Formula`.
57. Click `OK` on the `Edit Converter` dialog.
58. Assert element `:converter "Converter1"` shows `3` centered on the icon (Stock1 is 3).
59. Right-click element `:converter "Converter1"`.
60. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
61. Type `-Stock1` into dialog field `Formula`.
62. Click `OK` on the `Edit Converter` dialog.
63. Assert element `:converter "Converter1"` shows `-3` centered on the icon.
64. Right-click element `:converter "Converter1"`.
65. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
66. Type `?` into dialog field `Formula`.
67. Click `OK` on the `Edit Converter` dialog.
68. Assert converter computed value is at least `0` and less than `1`.
69. Assert flow `Flow1` rate is at least `0` and less than `1`.
70. Right-click element `:converter "Converter1"`.
71. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
72. Type `foo(1)` into dialog field `Formula`.
73. Click `OK` on the `Edit Converter` dialog.
74. Assert dialog titled `Edit Converter` is still visible (unknown function rejected).
75. Click `Cancel` on the `Edit Converter` dialog.
76. Quit the application using `File` → `Quit`.

## Pass criteria

- Stock-to-converter connectors bind stock names to current stock values for formula evaluation.
- Converter center shows computed numeric value; name remains on the label below the circle.
- Connected flow rate label matches the converter computed value.
- Converter-to-flow connector arrow still shows the formula text.
- Formulas support `+`, `-`, `*`, `/`, `^`, `%`, unary `-`, parentheses, constants `pi` and `e`, random `?` in `[0, 1)`, and functions `sqrt`, `exp`, `ln`, `log` (base 10), `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `hypot`, `abs`, `floor`, `ceil`, `round`, `min`, `max`, `mod`, `sign`, and `clamp`.
- `?` draws a new value on each formula evaluation (including each simulation step).
- Simulation transfers stock using the computed rate, and the displayed rate updates as stock values change.
- Unbound names, unknown names, unknown functions, and malformed operators are rejected in the Edit Converter dialog.
- No project API used.