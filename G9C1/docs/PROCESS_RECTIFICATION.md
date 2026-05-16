# Process flow rectification proposal

This document audits the current node graph in [factory.js](../src/main/resources/static/js/factory.js)
against real-world iron / steel sand-casting practice and proposes a set of
targeted corrections. Nothing has been changed in code — please review and
approve / amend each item before I implement.

## Current graph (for reference)

```
SCRAP ─┐
       ├──► F1 ┐                                                     pours
       └──► F2 ┘ ──► A1 / A2 / KW (lines, marry mould+metal → casting)
CORE ─► CM ──► PR-A1 / PR-A2 / PR-KW ──► A1 / A2 / KW
SAND ─► PR-A1 / PR-A2 / PR-KW                       (recipe sand+core → mould)

A1 ──► SK-A1 ──► FT-A1 ─┐
A2 ──► SK-A2 ──► FT-A2 ─┼──► SB ──► HT ─(MC | QC)─► MC ─(PT | QC)─► PT ──► QC ──► DISPATCH
KW ──► SK-KW ──► FT-KW ─┘

Sand recycle: SK-* ──► SAND   (one bag of sand per casting, instantly)
```

---

## Flaws identified

### F1 — Sand reclamation is missing
Used moulding sand exiting shakeout is hot, lumpy and contaminated with
binder residue and fines. Real plants pass it through **cool → magnetic
separation → screen → mix with fresh make-up sand** before it can be reused.
At the moment we wire `SK-*` straight back to `SAND` instantaneously, which
makes the sand loop free.

### F2 — In-house revert loop is missing
A real foundry remelts everything that doesn't ship: gating system / runners /
risers cut off at fettling, shakeout fines, and any **QC-reject castings**.
Today nothing returns to `SCRAP`, so all our scrap is "new" scrap and rejected
parts vanish.

### F3 — Core making consumes only "core stock"
Sand cores are produced from **silica sand + chemical binder** (cold-box,
shell, hot-box, etc.). Modelling cores as a totally separate raw material
divorced from sand is inaccurate. Cores should draw from the sand store too
(they share the silica supply), with the *coreraw* intake repurposed as
**binder / chemical additives** stock.

### F4 — Fettling order is debatable
Industry order is usually:

```
Shakeout → Degating (cut off sprue/runners/risers) → Shot-blast →
Grinding / fettling (final dressing) → Heat-treat → Machining → Paint → QC
```

We currently do `SK → FT → SB`. That is defensible if `FT` is interpreted as
*degating only*, but it conflates two operations (degating vs. final dressing)
into one box. A cleaner two-step model is recommended.

### F5 — No holding ladle between furnace and pouring line
Molten metal is **tapped from the furnace into a transfer / pouring ladle**
and *that* ladle pours into moulds. The ladle is also where alloying /
inoculation happens. We send `F1/F2` output straight to the line, which
ignores ladle as both a buffer and an alloying step.

### F6 — Furnaces don't consume any flux / additions
Real melt charges include scrap **plus** pig iron / returns / alloying
elements / flux. Our furnaces only consume scrap. This is a minor abstraction
loss but worth flagging — easy to model later as the revert loop (F2)
naturally feeds part of the charge.

### F7 — `A1 / A2 / KW` are visually identical lines
The three lines are structurally identical and produce indistinguishable
castings. In reality they would typically run different alloys / part
families (e.g. A1 = grey iron, A2 = ductile iron, KW = steel) and would be
fed by *different* furnaces. Cosmetic only — flagging it because it is the
implicit reason there are three lines.

---

## Proposed rectifications

I recommend doing F1–F4 now (they materially change the graph) and treating
F5–F7 as optional polish.

### R1 — Insert a **Sand Reclamation** node (covers F1)

```
SK-A1 ┐
SK-A2 ┼──► SR (Sand Reclamation) ──► SAND
SK-KW ┘
```

* `SR` is a `process` node, `processTime ≈ 1.5 s`, `replicas = 1`.
* Receives `sand` units from the recycle output of every shakeout.
* Outputs `sand` to the `SAND` store (so it merges with fresh intake).
* Implementation note: today `recycle: { to: 'SAND', kind: 'sand' }` posts
  directly to the destination's `output`. Change it to post to `SR.buffer`,
  and add an edge `SR → SAND`.

### R2 — Add an **In-house revert** loop (covers F2)

* Make `QC` a branching node with weighted outputs:
  `QC → DISPATCH` (e.g. 0.95) and `QC → SCRAP` (0.05) representing rejects.
* Make `FT-*` (or a dedicated post-fettle "GATING WASTE" output) also feed a
  small fraction back to `SCRAP` representing cut-off runners / sprues.
* Either implement as a real recycle (slot completion enqueues a `scrap`
  unit on the `SCRAP` output) so the loop is visible, or as a synthetic
  branch edge `QC → SCRAP` — the former is more honest.

### R3 — Tie cores to the sand supply (covers F3)

Two equally valid options — pick one:

* **Option A (cleaner):** delete the `CORE` intake. Make `CM`'s recipe
  `['sand', 'sand']` (a core uses ~2 units of sand) and the third intake
  becomes **BINDER** that `CM` also consumes (`recipe: ['sand', 'binder']`).
* **Option B (simpler):** keep `CORE` intake but rename it to **BINDER**, and
  make `CM`'s recipe `['sand', 'binder']`. The label change alone removes the
  absurdity that cores are mined as a raw material.

I recommend **Option B** — minimal code churn, clearer semantics.

### R4 — Split fettling into **Degating** and **Grinding** (covers F4)

```
SK-A* ──► DG-A* (Degating, per line) ──► SB ──► GR (Grinding/Fettling) ──► HT
```

* `DG-A1/A2/KW` replace today's `FT-A1/A2/KW`. They cut off sprues + runners.
  This is the natural place for the gating-waste recycle in R2.
* `GR` (single shared station) handles the final fettling / dressing **after**
  shot-blast and **before** heat-treat. `processTime ≈ 1.5 s`, `replicas = 2`.
* Net column count goes from 12 → 13. The whole layout already adapts to
  `NCOLS`, so this is a one-line constant change plus the new nodes.

### R5 — Holding ladle (optional, covers F5)

Insert two ladle nodes between the furnaces and the lines:

```
F1 ──► LD1 ┐
F2 ──► LD2 ┼──► A1 / A2 / KW
```

* `LD1/LD2` are simple `process` nodes (`processTime ≈ 0.5 s`) that act as a
  buffer + alloying step. Optional — recommend skipping for now to keep the
  layout uncluttered.

### R6 — Differentiate the three lines (optional, covers F7)

* Tag each line with an `alloy` (e.g. A1 = grey iron, A2 = ductile,
  KW = steel) and recolour the line body / cast unit accordingly.
* Optionally pin each line to a preferred furnace
  (`F1 → A1, A2`; `F2 → KW`) instead of full any-to-any.

Pure cosmetics — no behaviour change unless we add per-alloy heat-treat
recipes.

---

## Resulting graph if R1–R4 are accepted

```
SCRAP ─┐                                                  (revert: QC reject + DG waste)
       ├──► F1 ──┐                                                          ▲
       └──► F2 ──┼──► A1 / A2 / KW ──► SK-* ──► DG-* ──► SB ──► GR ──► HT  │
SAND  ─► PR-* ───┘            │                  │                          │
       ▲                       └─► (recycle sand)│                          │
       │                                         ▼                          │
       └─────────────── SR (reclamation) ◄───────┘                          │
BINDER ─► CM (sand + binder → core) ──► PR-*                                │
                                                                            │
                          HT ──► MC ──► PT ──► QC ──► DISPATCH              │
                          │      │                │                         │
                          └──────┴──► (bypass to QC for as-cast / net-shape)│
                                                  │                         │
                                                  └─────► SCRAP (rejects) ──┘
```

Buffer-count semantics, dial scaling, save/load and the gauge device are all
unaffected — these changes only reshape the node graph and add three new
station kinds (`SR`, `DG-*`, `GR`).

---

## Decision matrix

| ID | Change                                       | Recommend | Effort |
|----|----------------------------------------------|-----------|--------|
| R1 | Sand reclamation node `SR`                   | **Yes**   | small  |
| R2 | In-house revert loop (QC reject + DG waste)  | **Yes**   | small  |
| R3 | Cores draw from sand + binder (Option B)     | **Yes**   | trivial|
| R4 | Split fettling into `DG-*` + shared `GR`     | **Yes**   | small  |
| R5 | Holding ladles `LD1 / LD2`                   | optional  | medium |
| R6 | Per-line alloy tagging                       | optional  | medium |

Please mark which items to implement (e.g. "do R1 R2 R3 R4, skip R5 R6") and
I'll apply the changes.
