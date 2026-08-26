# Kodo — Brand assets

- `logo-mark.svg` — app icon / square mark (512×512, rounded).
- `logo-wordmark.svg` — horizontal lockup for light backgrounds.
- `feature-graphic.svg` — Google Play feature graphic (1024×500).

## Palette
| Token   | Hex       | Use |
|---------|-----------|-----|
| Indigo  | `#6C4CF1` | Primary brand |
| Mint    | `#22D3A6` | Accent / success (code captured) |
| Coral   | `#FF7A59` | Tertiary highlight |
| Ink     | `#0E0F1A` | Dark background |
| Cloud   | `#F6F6FB` | Light background |

## Logo idea
A rounded tray with a code "dropping in" (a downward chevron). It says:
*every code falls into one place.* Indigo→mint gradient signals security + success.

Export PNGs (icon 512, feature 1024×500) from these SVGs with any tool, e.g.:
`rsvg-convert -w 512 -h 512 logo-mark.svg > icon-512.png`
