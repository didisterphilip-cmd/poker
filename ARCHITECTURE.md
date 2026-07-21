# Architecture

## Stack decision

**Native Android — poker engine in Java, UI in Kotlin + Jetpack Compose.**

The brief offered Unity or React Native + Node. I chose native Android for a
few concrete reasons:

- **Provable correctness beats flash.** The one thing a poker app must never get
  wrong is who wins the pot. Writing the rules engine as **pure Java** (zero
  Android dependencies) means it compiles and runs on a plain JVM, so its
  correctness is nailed down by fast unit tests and property-based fuzzing —
  no emulator, no engine runtime. That is the single highest-leverage decision
  in the whole project, and Unity/RN would have buried the logic inside a
  heavier runtime.
- **The engine is reusable server-side unchanged.** When online tables land, the
  authoritative server runs the *same* `HandEngine` and `Deck` classes to resolve
  hands. Client and server sharing one verified engine is the cleanest possible
  guarantee that offline and online play identically.
- **Compose is the fastest path to a smooth, asset-light table.** Cards are drawn
  from vectors, not 52 image files; animations, theming and layout are a few
  lines each; the APK stays small; and it runs well across devices without a game
  engine's overhead.
- **No licensing / footprint tax.** No Unity seat, no JS bridge, minimal deps.

Trade-off: Compose's 2D drawing won't match a 3D physics chip-tumble. For a
"home game with friends" that prizes feel, fairness and speed over spectacle,
that's the right trade.

## Module map

```
app/src/main/java/com/homepoker/
├── engine/                 # Pure Java. No Android imports. Server-reusable.
│   ├── Card, Rank, Suit    # Immutable value types + notation parsing
│   ├── Deck                # Secure Fisher-Yates shuffle (SecureRandom);
│   │                       #   seeded/stacked variants for tests & replay
│   ├── HandEvaluator       # Best 5-of-7; exact rules-based classifier
│   ├── HandValue           # Comparable strength (category + kicker array)
│   ├── SidePots            # Layered main/side-pot construction
│   └── HandEngine          # The state machine: blinds, action legality,
│                           #   min-raise & all-in reopening, streets, showdown
├── ai/                     # Pure Java.
│   ├── HandStrength        # Monte-Carlo equity estimate
│   ├── BotProfile          # Personalities & difficulty tiers
│   └── PokerBot            # Equity + pot-odds + personality -> Action
└── kotlin/com/homepoker/
    ├── game/               # GameViewModel: sequences turns, timers, projection
    └── ui/                 # Compose: HomeScreen, TableScreen, CardView, dialogs
```

## Correctness model

The engine is deterministic given its `Deck`. That single property is what makes
everything testable:

- **Hand evaluation** scores each of the ≤21 five-card combinations of the seven
  available cards with an exact classifier (flush / straight incl. the A-2-3-4-5
  wheel / multiplicities), returning a `Comparable` `HandValue` whose kicker array
  resolves ties and split pots exactly.
- **Betting** enforces No-Limit rules: blinds and antes, correct first-to-act and
  button rotation (incl. heads-up), min-bet = big blind, min-raise tracking, the
  *incomplete all-in does not reopen the betting* rule, and **uncalled-bet
  return**.
- **Side pots** are built by carving pot layers at each distinct all-in level;
  folded contributors' chips stay in the pot but can't be won; a layer with no
  live claimant is merged into the nearest contested pot; odd chips on a split go
  to the earliest seat left of the button.
- **Tests** (`app/src/test/java`) assert all of the above, plus a **fuzz** that
  plays thousands of random hands and hand-driven bot games checking two
  invariants that catch almost any logic bug: *total chips are conserved* and
  *no stack goes negative*.

## Turn flow (offline)

`GameViewModel` owns the loop. It creates a `HandEngine` per hand, then on a
coroutine advances actors: bot turns pause briefly to "think" and act via
`PokerBot.decide`; the human's turn hands control to the UI and starts a 20s
timer that auto-checks/folds on expiry. After each engine mutation the VM projects
an immutable `TableState` that Compose renders. All rules stay in the engine; the
VM only sequences and projects — so the UI can never disagree with the engine.

## Fairness & anti-cheat (design)

- **Shuffle**: `SecureRandom`-seeded Fisher-Yates — unbiased across all 52!
  orderings, unpredictable.
- **Server-authoritative (online, staged)**: the shuffle and every hand outcome
  resolve on the server using this same engine; clients send *actions*, never
  card data, and receive only the cards they're entitled to see. Because the
  engine is deterministic, the server can also log the seed + action list to
  replay/audit any disputed hand.

## Why the APK is built in CI

The engine and tests build and run anywhere with a JDK. The **Android** binary
needs the Android SDK + Google's Maven repos, which aren't present in every
authoring sandbox. `.github/workflows/android.yml` runs the unit tests and then
assembles/signs the APK on a GitHub runner that has them — reproducibly from this
source — and uploads it as an artifact (and a Release asset on version tags).
