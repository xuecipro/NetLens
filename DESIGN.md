# NetLens DESIGN.md

## Identity
Product UI Designer — network utility; convention mode (clarity over spectacle).

## Objective
Help Chinese students/home users diagnose Wi‑Fi, measure speed (incl. domestic mirrors), and act on advice — without ads or tracking.

## Palette
| Token | Light | Dark | Role |
|-------|-------|------|------|
| primary | #1B6EF3 | #4DA3FF | actions, selection |
| secondary | #0D9488 | #2DD4BF | success / good signal |
| tertiary | #7C3AED | #A78BFA | latency-only / LAN |
| background | #F6F8FB | #0B1220 | page |
| surface | #FFFFFF | #121A2A | cards |

**Monet:** On API 31+, `dynamicLight/DarkColorScheme(activity)` with window bar sync; toggle in Settings + live swatch strip. Fallback = brand palette (MIUI-safe Activity context).

## Typography
Material 3 default. ExtraBold for screen titles, SemiBold for section headers, labelSmall for metadata (ISP/host/ping). Monospace only for IP/MAC.

## Layout
- Bottom NavigationBar: 6 tabs, switch in-place (no back stack).
- Cards: 14–18dp radius, 12–16dp padding, one action per card.
- Speed page order: node picker (domestic → international) → start → dual-unit results (Mbps + MB/s).
- Empty/error states name the next action, never apologize.

## Domestic speed strategy
1. Latency-only nodes: 223.5.5.5 / 119.29.29.29 / 114.114.114.114 (almost always reachable).
2. Download nodes: multi-URL fallback + HTTP Range (20MB sample) on public mirrors.
3. International Cloudflare remains the only default upload path.

## Anti-patterns
- No purple-blue gradient heroes.
- No card grids that hide the primary action.
- No “自动(Cloudflare)” dead-end UI; every node shows mode (latency vs download).
- Do not claim campus gateway admin access.

## Voice
Chinese default, English parallel. Verbs first. Errors state cause + next step.
