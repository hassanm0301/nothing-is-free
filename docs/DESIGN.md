# Nothing OS style guide (as implemented)

Source of truth for the look. Compose tokens live in `app/src/main/kotlin/.../ui/theme/`.

## Principles
- Monochrome first. Black, white, a grey ramp. One red (`#D71921`) for live state, progress, emphasis.
- Flat. No elevation, no shadows, no gradients. Surfaces are a tone step away from the background.
- Dot-matrix is the signature. Ndot for numerals and display text; `DotMatrix` for anything rendered as pixels.
- Mono captions. Uppercase Lettera Mono with 8% tracking for every label, caption, and metadata line.
- Big radii. Cards 28dp, controls are pills or circles.

## Colour
| Token | Dark | Light | Use |
|---|---|---|---|
| background / onBackground | `#000000` / `#FFFFFF` | `#FFFFFF` / `#000000` | Screen, primary text |
| `NothingTheme.colors.card` | `#1A1A1A` | `#F2F2F2` | Cards, widget surfaces |
| `NothingTheme.colors.muted` | `#9E9E9E` | `#7A7A7A` | Secondary text, mono labels |
| `NothingTheme.colors.hairline` | `#3D3D3D` | `#DADADA` | Outlines, dividers |
| `NothingTheme.colors.dotOn` / `dotOff` | white / `#2A2A2A` | black / `#DADADA` | Dot-matrix pixels |
| `NothingTheme.colors.accent` | `#D71921` | `#D71921` | Live indicators, progress, accent buttons |

`primary` is the ink colour (white on dark, black on light) so Material components that default to
`primary` come out monochrome. `secondary` and `error` are the red.

## Type
| Role | Face | Sizes |
|---|---|---|
| display / headline | Ndot 57 | 64 / 48 / 36 / 32 / 26 / 22 sp |
| title | NType 82 Headline | 22 / 18 / 16 sp |
| body | NType 82 Regular | 16 / 14 / 12 sp |
| label | Lettera Mono (uppercase, tracked) | 14 / 12 / 10 sp |

Fonts come from <https://github.com/xeji01/nothingfont>. They are Nothing's proprietary faces,
redistributed by a third party; fine for a personal build, clear the licence before publishing.

## Components (`ui/components/`)
- `NothingButton` (Primary, Secondary, Accent, Ghost), `NothingIconButton`, `StatusDot`
- `NothingCard` (default, `inverted`, `outlined`), `PlaceholderTile`
- `NothingSwitch`, `NothingChip`, `NothingListRow`
- `NothingProgressRing`, `DotBar`
- `MonoLabel`, `SectionHeader`, `DotDivider`
- `DotMatrixClock` on top of `dotmatrix.DotMatrix` + `DotMatrixCanvas`

## Theme mode
`ThemePreferences` holds SYSTEM / LIGHT / DARK in memory. Persisting it (DataStore) is a TODO.
