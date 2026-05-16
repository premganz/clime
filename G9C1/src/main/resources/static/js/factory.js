(function () {
  // ============================================================
  //  G9C1 - Foundry Floor (A1 / A2 / KW pouring lines)
  // ============================================================
  const canvas = document.getElementById('floor');
  const ctx = canvas.getContext('2d');

  const DEFAULT_EDGE_TIME = 2.0;    // seconds per move on a typical conveyor
  const FAST_EDGE_TIME    = 0.5;    // mould-flow / pour edges
  const BOTTLENECK_THRESHOLD = 70;  // station body turns RED/YELLOW when buffer crosses this
  const MAX_PROCESS_TIME = 60;      // seconds (slider upper bound, lifted for furnace)
  const MAX_BOOST = 6;              // parallel slot upper bound
  const INIT_BOOST = 3;             // every boostable node starts at this level

  // ---- Material types tagged on units ----
  // 'scrap'   : raw to be melted
  // 'sand'    : raw moulding sand
  // 'coreraw' : raw stock for cores
  // 'core'    : finished core (for setting into a box)
  // 'box'     : empty mould box (sand mould body, no core yet)
  // 'mould'   : empty box WITH a core set into it (ready to receive metal)
  // 'metal'   : molten metal pour
  // 'cast'    : poured casting (mould + metal). Heavier, yellow-filled.
  // 'final'   : ready for dispatch

  // ---- Node graph (snake layout) ----
  // The grid is 7 columns x 4 rows. The flow snakes left->right then
  // right->left then left->right. Furnaces sit ABOVE the pouring lines
  // and drop molten metal in laterally - they do not feed mould prep.
  //
  //  row 0:  CORE  CM        F1   F2
  //  row 1:  SAND  MB   CS   A1   A2   KW   SK   FT
  //  row 2:  PT    BP   MC   BM   HT   BH   SB   BS  <- right-to-left return
  //  row 3:  QC -> DISPATCH
  //
  // BS / BH / BM / BP are tiny diamond "decision" nodes that route either
  // to the next finishing step or directly to QC (customer asks for as-cast,
  // unmachined, unpainted, etc).
  const nodes = {};
  function N(id, col, row, type, processTime, opts) {
    opts = opts || {};
    nodes[id] = {
      id: id, col: col, row: row, type: type,
      processTime: processTime,                // seconds per unit per slot (or batch)
      boost: opts.boost || 1,                  // number of parallel slots ("boost")
      batchSize: opts.batchSize || 1,          // for 'batch' type: units per run
      transmute: !!opts.transmute,             // batch: build new units (true) or preserve charge (false)
      label: opts.label || id,
      kindOut: opts.kindOut || null,
      recipe: opts.recipe || null,             // e.g. ['box','core'] or ['mould','metal']
      appendCode: opts.appendCode || '',       // letter appended to lineage code
      routeFn: opts.routeFn || null,           // bypass: u -> targetId | [targetIds]
      validateFn: opts.validateFn || null,     // process: returns true/false on output
      buffer: [], output: [],
      slots: [],                               // [{ remaining, jobTime, out }]
      edgeCooldowns: {}, totalIn: 0, totalOut: 0,
      shape: opts.shape || 'box'
    };
  }

  // ============================================================
  //  Topology - cleaner snake, no crossings.
  //
  //   row 0 :  SCRAP   F1   F2                          (top, drops metal down)
  //   row 1 :  BOX  SAND  MB  CB  CS  A1  A2  KW  CL    (left->right spine)
  //   row 2 :  PT   BP   MC  BM  HT  BH  SB  BS  FT  SK (right->left spine)
  //   row 3 :  QC   DISPATCH                            (bottom)
  //
  //  CORE feeder hangs off the top-left corner (col -0.3, row 1) into CM.
  //
  //  Bypass diamonds (BS/BH/BM/BP) sit IN-LINE between adjacent finishing
  //  stages. Each one routes either FORWARD to the next process or SKIP-
  //  AHEAD to the next bypass. Only BP can drop straight down to QC. This
  //  removes the long diagonal jumps to QC that crossed the spine before.
  // ============================================================

  // Intakes (left wall)
  N('CORE',  0,    0.3, 'intake', 0, { label: 'CORE STOCK',  kindOut: 'coreraw', shape: 'hold' });
  N('BOX',   0,    1,   'intake', 0, { label: 'BOX STORE',   kindOut: 'box',     shape: 'hold' });
  N('SAND',  0.7,  1,   'intake', 0, { label: 'SAND STORE',  kindOut: 'sand',    shape: 'hold' });
  N('SCRAP', 3,    0,   'intake', 0, { label: 'SCRAP YARD',  kindOut: 'scrap',   shape: 'hold' });

  // Furnaces - BATCH processing. Accumulate 40 scrap units, then 40s melt,
  // then emit 40 metal units to the output (the molten ladle).
  N('F1', 4,   0,   'batch', 40, { label: 'Furnace F1', kindOut: 'metal', shape: 'furnace',
                                    batchSize: 40, transmute: true, boost: INIT_BOOST });
  N('F2', 5,   0,   'batch', 40, { label: 'Furnace F2', kindOut: 'metal', shape: 'furnace',
                                    batchSize: 40, transmute: true, boost: INIT_BOOST });

  // Core maker (between CORE store and CS). Sits one notch above row 1.
  N('CM', 1.5, 0.4, 'process', 1.0, { label: 'Core Maker', kindOut: 'core', shape: 'box',
                                       boost: INIT_BOOST });

  // SAND-PRESSING / MOULD-MAKING. Issues an empty box from the BOX STORE,
  // pulls sand, presses it into the box -> empty mould (no core yet).
  N('MB', 1.5, 1, 'process', 1.0, { label: 'Sand Press', kindOut: 'mould', shape: 'box',
                                     boost: INIT_BOOST, recipe: ['box', 'sand'],
                                     appendCode: 'S' });

  // Decision: does this mould need a core?  Branches to CS (with-core) or
  // skips straight to the combiner row (green moulds).
  N('CB', 2.5, 1, 'bypass', 0, { label: 'Core?', shape: 'diamond' });

  // Core-setting (only for non-green moulds). Drops a core into the mould.
  N('CS', 3,   1, 'process', 3.0, { label: 'Core Setting', kindOut: 'mould', shape: 'box',
                                     boost: INIT_BOOST, recipe: ['mould', 'core'],
                                     appendCode: 'C' });

  // Pouring combiners on row 1. Pouring itself is quick (1.5s).
  N('A1', 4,   1, 'line', 1.5, { label: 'Combiner A1', kindOut: 'cast', shape: 'line',
                                  recipe: ['mould', 'metal'],
                                  appendCode: 'P' });
  N('A2', 4.7, 1, 'line', 1.5, { label: 'Combiner A2', kindOut: 'cast', shape: 'line',
                                  recipe: ['mould', 'metal'],
                                  appendCode: 'P' });
  N('KW', 5.4, 1, 'line', 1.5, { label: 'Combiner KW', kindOut: 'cast', shape: 'line',
                                  recipe: ['mould', 'metal'],
                                  appendCode: 'P' });

  // COOLER - batch of 40 castings, 10s hold, then forward as a wave to SK.
  // transmute=false so unit identity (color, tags) is preserved.
  N('CL', 6,   1, 'batch', 10, { label: 'Cooler', kindOut: 'cast', shape: 'box', boost: INIT_BOOST,
                                  batchSize: 40, transmute: false,
                                  appendCode: 'Cl' });

  // Row 2 - right-to-left finishing spine. SK + FT first (still hot work).
  N('SK', 6,   2, 'process', 1.5, { label: 'Shakeout', kindOut: 'cast', shape: 'box', boost: INIT_BOOST,
                                     appendCode: 'Sh' });
  N('FT', 5.4, 2, 'process', 1.5, { label: 'Fettling', kindOut: 'cast', shape: 'box', boost: INIT_BOOST,
                                     appendCode: 'Ft' });

  // ---- Bypass ladder: each diamond sits between two adjacent stages. ----
  // BS skips SB, BH skips HT, BM skips MC, BP skips PT.
  N('BS', 4.85, 2, 'bypass', 0, { label: 'Skip SB?', shape: 'diamond' });
  N('SB', 4.4,  2, 'process', 3.0, { label: 'Shot Blast', kindOut: 'cast', shape: 'box', boost: INIT_BOOST, appendCode: 'T' });
  N('BH', 3.95, 2, 'bypass', 0, { label: 'Skip HT?', shape: 'diamond' });
  N('HT', 3.4,  2, 'process', 5.0, { label: 'Heat Treat', kindOut: 'cast', shape: 'box', boost: INIT_BOOST, appendCode: 'H' });
  N('BM', 2.95, 2, 'bypass', 0, { label: 'Skip MC?', shape: 'diamond' });
  N('MC', 2.4,  2, 'process', 4.0, { label: 'Machining',  kindOut: 'cast', shape: 'box', boost: INIT_BOOST, appendCode: 'M' });
  N('BP', 1.95, 2, 'bypass', 0, { label: 'Skip PT?', shape: 'diamond' });
  N('PT', 1.4,  2, 'process', 3.0, { label: 'Paint',      kindOut: 'cast',  shape: 'box', boost: INIT_BOOST, appendCode: 'F' });

  // QC + DISPATCH below the leftmost finishing stations.
  N('QC',       1.4, 3, 'process', 1.5, { label: 'QC',       kindOut: 'final', shape: 'box', boost: INIT_BOOST });
  N('DISPATCH', 6,   3, 'dispatch', 0,   { label: 'DISPATCH',                    shape: 'hold' });

  // ============================================================
  // Edges - per-edge transfer time enforces conveyor pace.
  // Default 2 sec/move; box-flow + pour edges are 0.5s.
  // ============================================================
  function E(to, t) { return { to: to, t: (typeof t === 'number') ? t : DEFAULT_EDGE_TIME }; }
  const F = FAST_EDGE_TIME;

  const edges = {
    // intake feeds
    'SCRAP': [E('F1'), E('F2')],
    'CORE':  [E('CM')],
    'BOX':   [E('MB', F)],
    'SAND':  [E('MB', F)],

    // mould-flow pipeline (fast)
    'CM':    [E('CS', F)],
    'MB':    [E('CB', F)],
    // CB decides: with-core (forward to CS) or skip-core (jump to a combiner).
    'CB':    [E('CS', F), E('A1', F), E('A2', F), E('KW', F)],
    'CS':    [E('A1', F), E('A2', F), E('KW', F)],

    // furnaces drop metal DOWN into the combiners (the actual pour is fast)
    'F1':    [E('A1', F), E('A2', F), E('KW', F)],
    'F2':    [E('A1', F), E('A2', F), E('KW', F)],

    // combiners -> COOLER (right end of row 1) -> down to SK on row 2
    'A1':    [E('CL')],
    'A2':    [E('CL')],
    'KW':    [E('CL')],
    'CL':    [E('SK')],

    // Row 2 finishing spine, right -> left, with the bypass ladder.
    // Each bypass either continues to the NEXT process or skips to the
    // bypass AFTER it. Only BP can drop down to QC (skip paint).
    'SK':    [E('FT')],
    'FT':    [E('BS')],
    'BS':    [E('SB'), E('BH')],   // skip SB -> straight to BH
    'SB':    [E('BH')],
    'BH':    [E('HT'), E('BM')],   // skip HT -> straight to BM
    'HT':    [E('BM')],
    'BM':    [E('MC'), E('BP')],   // skip MC -> straight to BP
    'MC':    [E('BP')],
    'BP':    [E('PT'), E('QC')],   // skip PT -> drop to QC
    'PT':    [E('QC')],
    'QC':    [E('DISPATCH')]
  };

  function edgeTargets(id) {
    return (edges[id] || []).map(e => e.to);
  }
  function edgeTransferTime(fromId, toId) {
    const list = edges[fromId] || [];
    for (const e of list) if (e.to === toId) return e.t;
    return DEFAULT_EDGE_TIME;
  }

  // Per-edge weights for branching dispatchers. Each bypass diamond defaults
  // to 70% forward, 30% direct-to-QC. CB defaults to 60% with-core (CS),
  // 40% bare mould split across A1/A2/KW.
  const BYPASS_DEFAULT = 0.30;
  const CORE_NEEDED_DEFAULT = 0.60;
  const edgeWeights = {};
  for (const id in edges) {
    const ts = edgeTargets(id);
    if (ts.length < 2) continue;
    edgeWeights[id] = {};
    if (id === 'BS' || id === 'BH' || id === 'BM' || id === 'BP') {
      // Two outputs: forward (to the process) and skip (to next bypass or QC).
      // Default 70% process, 30% skip.
      const PROC = { BS: 'SB', BH: 'HT', BM: 'MC', BP: 'PT' };
      const proc = PROC[id];
      const skip = ts.find(t => t !== proc);
      edgeWeights[id][proc] = 1 - BYPASS_DEFAULT;
      edgeWeights[id][skip] = BYPASS_DEFAULT;
    } else if (id === 'CB') {
      const skip = ts.filter(t => t !== 'CS');
      edgeWeights[id]['CS'] = CORE_NEEDED_DEFAULT;
      const each = (1 - CORE_NEEDED_DEFAULT) / Math.max(1, skip.length);
      for (const t of skip) edgeWeights[id][t] = each;
    } else {
      const w = 1 / ts.length;
      for (const t of ts) edgeWeights[id][t] = w;
    }
  }

  // Initialise edge cooldowns
  for (const id in edges) for (const t of edgeTargets(id)) nodes[id].edgeCooldowns[t] = 0;

  // ============================================================
  //  Box-color routing rules + QC validation (code-based)
  // ============================================================
  // Every well-formed unit carries a lineage CODE built up as it moves:
  //
  //   B-<C>     issued from BOX STORE, where <C> is the colour letter
  //   B-<C>S    after sand-press at MB
  //   B-<C>SC   after core-setting at CS  (skipped for green)
  //   B-<C>SCP  after pour at a combiner  (or B-<C>SP for green)
  //   ...T      after shot-blast (SB)
  //   ...H      after heat-treat (HT)
  //   ...M      after machining (MC)
  //   ...F      after paint (PT)
  //
  //  Colour letters and per-colour expected codes:
  //    Y yellow : full pipeline               B-YSCPTHMF
  //    B blue   : skip MC + PT                B-BSCPTH
  //    G green  : skip CS (no core), full     B-GSPTHMF
  //    X gray   : skip ALL finishing          B-XSCP
  //
  // QC compares the unit's code to the exact expected code for its colour.
  // Anything else -> rejects bucket.
  const COLORS    = ['yellow', 'blue', 'green', 'gray'];
  const COLOR_LET = { yellow: 'Y', blue: 'B', green: 'G', gray: 'X' };
  const COLOR_HEX = { yellow: '#f5d76e', blue: '#7aa9e5', green: '#6ada6a', gray: '#aab' };
  // Tokens appended along the journey:
  //   S  Sand-press      C  Core-set       P  Pour
  //   Cl Cool             Sh Shakeout       Ft Fettle
  //   T  shot-blasT       H  Heat-treat     M  Machine    F  Finish-paint
  //
  //  Per-colour expected lineage code (tokens joined with '-'):
  //    yellow : full pipeline               B-Y-S-C-P-Cl-Sh-Ft-T-H-M-F
  //    blue   : skip MC + PT                B-B-S-C-P-Cl-Sh-Ft-T-H
  //    green  : skip CS (no core), full     B-G-S-P-Cl-Sh-Ft-T-H-M-F
  //    gray   : skip ALL finishing          B-X-S-C-P-Cl-Sh-Ft
  const EXPECT_CODE = {
    yellow: 'B-Y-S-C-P-Cl-Sh-Ft-T-H-M-F',
    blue:   'B-B-S-C-P-Cl-Sh-Ft-T-H',
    green:  'B-G-S-P-Cl-Sh-Ft-T-H-M-F',
    gray:   'B-X-S-C-P-Cl-Sh-Ft'
  };

  function qcValidate(u) {
    if (!u || !u.color) return false;
    const want = EXPECT_CODE[u.color];
    return !!want && u.code === want;
  }

  // Routing fns - return single id or array of candidates.
  // Bypass ladder is forward-only: each diamond chooses 'next process' or
  // 'skip ahead to the next diamond'. Only BP can drop down to QC.
  nodes['CB'].routeFn = u => (u.color === 'green') ? ['A1', 'A2', 'KW'] : 'CS';
  nodes['BS'].routeFn = u => (u.color === 'gray') ? 'BH' : 'SB';   // gray skips T,H,M,F
  nodes['BH'].routeFn = u => (u.color === 'gray') ? 'BM' : 'HT';   // gray keeps skipping
  nodes['BM'].routeFn = u => (u.color === 'blue' || u.color === 'gray') ? 'BP' : 'MC';
  nodes['BP'].routeFn = u => {
    // Yellow + green need paint; everyone else drops to QC here.
    if (u.color === 'yellow' || u.color === 'green') return 'PT';
    return 'QC';
  };

  nodes['QC'].validateFn = qcValidate;

  // Rejects bucket - lives outside resetSim so the constant is visible early.
  const rejects = { total: 0, yellow: 0, blue: 0, green: 0, gray: 0, untagged: 0 };


  // ---- Layout ----
  const NCOLS = 7;
  const NROWS = 4;     // 0..3 (snake)
  let W = 0, H = 0;
  let STATION_W, STATION_H, PALLET_W, PALLET_H;
  let WALL_PAD = 70;   // leave whitespace + room for trees outside the wall

  function resize() {
    const stage = document.getElementById('stage');
    const dpr = window.devicePixelRatio || 1;
    const cssW = stage.clientWidth - 8;
    const cssH = stage.clientHeight - 8;
    canvas.style.width = cssW + 'px';
    canvas.style.height = cssH + 'px';
    canvas.width = cssW * dpr;
    canvas.height = cssH * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    W = cssW; H = cssH;

    WALL_PAD = Math.max(50, Math.min(90, H * 0.08));

    // Generous interior padding so pallets, dispatcher diamonds and badges
    // sit well inside the factory walls.
    const interiorPadX = 70;
    const interiorPadY = 70;
    const innerX = WALL_PAD + interiorPadX;
    const innerY = WALL_PAD + interiorPadY;
    const innerW = Math.max(200, W - innerX * 2);
    const innerH = Math.max(200, H - innerY * 2);
    const colW = innerW / (NCOLS - 1);
    const rowH = innerH / (NROWS - 1);

    STATION_W = Math.min(colW * 0.50, 78);
    STATION_H = Math.min(rowH * 0.32, 46);
    PALLET_W  = Math.min(STATION_W * 0.30, 20);
    PALLET_H  = STATION_H;

    for (const id in nodes) {
      const n = nodes[id];
      n.x = innerX + n.col * colW;
      n.y = innerY + n.row * rowH;
    }
  }
  window.addEventListener('resize', resize);

  // ============================================================
  //  Simulation
  // ============================================================
  let running = false, pause = false, speed = 1.0;
  let lastT = 0;
  let unitIdSeq = 0, totalDispatched = 0, batchSize = 0;
  const travelers = [];

  // ---- Run-time stopwatch (sim seconds, not wall seconds) ----
  // Cumulative simulated time elapsed while the run is RUNNING (not paused).
  // STOP_PCT  : fraction of batch that must have reached DISPATCH or rejects
  //             before the idle-stop watchdog can fire.
  // STOP_IDLE : seconds of zero dispatch activity that triggers the stop.
  // The reported total subtracts STOP_IDLE so the wait window is excluded.
  const STOP_PCT  = 0.80;
  const STOP_IDLE = 30.0;
  let runElapsedSec = 0;          // sim seconds since startBatch (running, !pause)
  let lastDispatchSec = 0;        // runElapsedSec at the moment of the last dispatch
  let lastDispatchedSeen = 0;     // last observed totalDispatched (for edge detection)
  let runFinalSec = null;         // frozen total once stopped
  let runStopReason = '';         // 'BATCH_DONE' or 'IDLE_STOP'

  function newUnit(kind, line) {
    return { id: ++unitIdSeq, kind: kind, line: line || null,
             color: null, code: '', parts: [] };
  }

  // Initialise per-node metrics
  for (const id in nodes) {
    nodes[id].metrics = {
      inRate: 0, outRate: 0,
      inSlope: 0, outSlope: 0,           // d(buffer)/dt for input and output pallet
      lastIn: 0, lastOut: 0,
      lastBuf: 0, lastOutBuf: 0,
      lastT: 0
    };
  }

  function resetSim() {
    for (const id in nodes) {
      const n = nodes[id];
      n.buffer = []; n.output = []; n.slots = [];
      n.totalIn = 0; n.totalOut = 0;
      n.metrics.inRate = 0; n.metrics.outRate = 0;
      n.metrics.inSlope = 0; n.metrics.outSlope = 0;
      n.metrics.lastIn = 0; n.metrics.lastOut = 0;
      n.metrics.lastBuf = 0; n.metrics.lastOutBuf = 0;
      n.metrics.lastT = performance.now();
      for (const t in n.edgeCooldowns) n.edgeCooldowns[t] = 0;
    }
    travelers.length = 0;
    totalDispatched = 0; batchSize = 0; running = false;
    runElapsedSec = 0; lastDispatchSec = 0; lastDispatchedSeen = 0;
    runFinalSec = null; runStopReason = '';
    plantSamples.length = 0; lastPlantSampleT = 0; plantRate = 0; plantTrend = 0;
    rejects.total = 0; rejects.yellow = 0; rejects.blue = 0;
    rejects.green = 0; rejects.gray = 0; rejects.untagged = 0;
  }

  function startBatch(n) {
    resetSim();
    batchSize = n;
    // SCRAP - 1:1, no code (becomes metal at the furnace and joins a box-line).
    for (let i = 0; i < n; i++) nodes['SCRAP'].output.push(newUnit('scrap'));
    // SAND - 1:1, no code (consumed by MB into the box-line).
    for (let i = 0; i < n; i++) nodes['SAND'].output.push(newUnit('sand'));
    // BOX - 1:1, round-robin colours so the batch is exactly 25% of each.
    // Each box starts the lineage code as 'B-<colourLetter>'.
    for (let i = 0; i < n; i++) {
      const u = newUnit('box');
      u.color = COLORS[i % COLORS.length];
      u.code  = 'B-' + COLOR_LET[u.color];
      nodes['BOX'].output.push(u);
    }
    // CORE - only the non-green fraction needs cores (75%).
    const cores = Math.ceil(n * 0.75);
    for (let i = 0; i < cores; i++) nodes['CORE'].output.push(newUnit('coreraw'));
    running = true;
  }

  function pickReadyEdge(n) {
    const targets = edgeTargets(n.id);
    if (!targets.length) return null;
    // Edge is 'ready' when its cooldown has elapsed.
    const ready = targets.filter(t => n.edgeCooldowns[t] <= 0);
    if (!ready.length) return null;
    const w = edgeWeights[n.id];
    if (w) {
      let total = 0;
      for (const t of ready) total += (w[t] || 0);
      if (total <= 0) return ready[Math.floor(Math.random() * ready.length)];
      let r = Math.random() * total;
      for (const t of ready) { r -= (w[t] || 0); if (r <= 0) return t; }
      return ready[ready.length - 1];
    }
    return ready[Math.floor(Math.random() * ready.length)];
  }

  // Pick a target from a fixed candidate set (used by routeHint). Prefers the
  // candidate whose destination buffer is shallowest, for natural load balance.
  function pickReadyFromCandidates(n, cands) {
    const ready = [];
    for (const t of cands) {
      if (n.edgeCooldowns[t] === undefined) continue;   // not a configured edge
      if (n.edgeCooldowns[t] > 0) continue;
      const d = nodes[t]; if (!d) continue;
      ready.push(t);
    }
    if (!ready.length) return null;
    ready.sort((a, b) => nodes[a].buffer.length - nodes[b].buffer.length);
    return ready[0];
  }

  function dispatchOutputs(n) {
    while (n.output.length > 0) {
      const head = n.output[0];
      let to;
      if (head && head.routeHint) {
        // Per-unit forced route (set by a bypass node). Wait if the chosen
        // target(s) are full or cooling - do NOT silently re-route.
        const cands = (typeof head.routeHint === 'string') ? [head.routeHint] : head.routeHint;
        to = pickReadyFromCandidates(n, cands);
      } else {
        to = pickReadyEdge(n);
      }
      if (!to) break;                     // all edges cooling
      const t = edgeTransferTime(n.id, to);
      const unit = n.output.shift();
      delete unit.routeHint;              // consumed; downstream gets a fresh one
      travelers.push({
        fromId: n.id, toId: to, t: 0,
        duration: Math.max(0.05, t),      // seconds to traverse the conveyor
        unit: unit
      });
      n.edgeCooldowns[to] = t;            // pace = transfer time (1 unit per t sec)
      n.totalOut++;
    }
  }

  function tickStations(dt) {
    for (const id in nodes) {
      const n = nodes[id];
      for (const t in n.edgeCooldowns) {
        if (n.edgeCooldowns[t] > 0) n.edgeCooldowns[t] -= dt;
      }

      if (n.type === 'intake') {
        dispatchOutputs(n);
        continue;
      }

      if (n.type === 'bypass') {
        // Pass-through. If the node has a per-unit routeFn, stamp the unit
        // with a routeHint so dispatchOutputs forces it onto the chosen edge.
        while (n.buffer.length > 0) {
          const u = n.buffer.shift();
          if (n.routeFn) {
            try { u.routeHint = n.routeFn(u); }
            catch (e) { /* leave unrouted -> falls back to weights */ }
          }
          n.output.push(u);
        }
        dispatchOutputs(n);
        continue;
      }

      if (n.type === 'dispatch') {
        while (n.buffer.length > 0) {
          n.output.push(n.buffer.shift());
          n.totalIn++;
          totalDispatched++;
        }
        continue;
      }

      if (n.type === 'batch') {
        // Batch processor.
        //   transmute=true  (furnaces): consume the charge, produce N fresh
        //                                units of kindOut (e.g. scrap -> metal).
        //                                Code/colour are NOT inherited - metal is
        //                                anonymous until it joins a box-line.
        //   transmute=false (cooler):   PRESERVE the actual charge units, just
        //                                hold them for processTime, then release.
        //                                Code/colour stay intact.
        while (n.slots.length < n.boost && n.buffer.length >= n.batchSize) {
          const charge = n.buffer.splice(0, n.batchSize);
          let outs;
          if (n.transmute) {
            outs = [];
            for (let i = 0; i < n.batchSize; i++) {
              outs.push(newUnit(n.kindOut || 'cast'));
            }
          } else {
            outs = charge;
            if (n.appendCode) {
              for (const u of outs) {
                if (u.code && u.code.charAt(0) === 'B') u.code += '-' + n.appendCode;
              }
            }
          }
          const jt = Math.max(0.001, n.processTime);
          n.slots.push({ remaining: jt, jobTime: jt, outs: outs });
        }
        for (let i = n.slots.length - 1; i >= 0; i--) {
          const s = n.slots[i];
          s.remaining -= dt;
          if (s.remaining <= 0) {
            for (const u of s.outs) n.output.push(u);
            n.slots.splice(i, 1);
          }
        }
        dispatchOutputs(n);
        continue;
      }

      // 'process' or 'line' - parallel slots up to n.boost
      // 1) advance running slots and emit completed ones
      for (let i = n.slots.length - 1; i >= 0; i--) {
        const s = n.slots[i];
        s.remaining -= dt;
        if (s.remaining <= 0) {
          if (n.validateFn) {
            // Validator (QC). Good units -> output (so they reach DISPATCH).
            // Bad units -> rejects bucket (they are NOT counted as production).
            if (n.validateFn(s.out)) {
              n.output.push(s.out);
            } else {
              rejects.total++;
              const c = s.out && s.out.color;
              if (c && rejects[c] !== undefined) rejects[c]++;
              else rejects.untagged++;
            }
          } else {
            n.output.push(s.out);
          }
          n.slots.splice(i, 1);
        }
      }
      // 2) fill empty slots from the input buffer
      while (n.slots.length < n.boost && canStartJob(n)) {
        startJob(n);
      }
      dispatchOutputs(n);
    }
  }

  function canStartJob(n) {
    if (n.recipe && n.recipe.length) {
      const need = n.recipe.slice();
      for (const u of n.buffer) {
        const idx = need.indexOf(u.kind);
        if (idx >= 0) need.splice(idx, 1);
        if (!need.length) return true;
      }
      return false;
    }
    return n.buffer.length >= 1;
  }

  function startJob(n) {
    let consumed = [];
    if (n.recipe && n.recipe.length) {
      const need = n.recipe.slice();
      const indices = [];
      for (let i = 0; i < n.buffer.length && need.length; i++) {
        const idx = need.indexOf(n.buffer[i].kind);
        if (idx >= 0) {
          need.splice(idx, 1);
          indices.push(i);
        }
      }
      indices.sort((a, b) => b - a);
      for (const i of indices) consumed.push(n.buffer.splice(i, 1)[0]);
    } else {
      consumed = n.buffer.splice(0, 1);
    }

    const lineHint = (n.id === 'A1' || n.id === 'A2' || n.id === 'KW')
      ? n.id : (consumed[0] && consumed[0].line);
    const out = newUnit(n.kindOut || 'cast', lineHint);
    // Conservative move semantics: the produced unit inherits its lineage code
    // and colour from the box-line parent (the consumed unit whose code starts
    // with 'B-'). Then this node appends its own letter (if any).
    let lineage = null;
    for (const u of consumed) {
      if (u.code && u.code.charAt(0) === 'B') { lineage = u; break; }
    }
    if (lineage) {
      out.color = lineage.color;
      out.code  = lineage.code + (n.appendCode ? '-' + n.appendCode : '');
    } else {
      // No box-lineage parent (e.g. CM consuming raw core). Output stays
      // codeless / colourless and will only contribute when later joined to
      // a box-line at CS or a combiner.
      out.code = '';
    }
    for (const u of consumed) for (const p of (u.parts || [])) out.parts.push(p);
    out.parts.push({ stage: n.id });
    const jt = Math.max(0.001, n.processTime);
    n.slots.push({ remaining: jt, jobTime: jt, out: out });
  }

  function tickTravelers(dt) {
    for (let i = travelers.length - 1; i >= 0; i--) {
      const tr = travelers[i];
      tr.t += dt / tr.duration;
      if (tr.t >= 1) {
        const dst = nodes[tr.toId];
        // Furnaces tag scrap -> still scrap on input; intake furnace process turns it into metal via kindOut
        // Lines need 'mould' or 'metal' kinds in buffer. Ensure tagging.
        // Travelers carry the unit produced by the source node.
        dst.buffer.push(tr.unit);
        dst.totalIn++;
        travelers.splice(i, 1);
      }
    }
  }

  function inFlightCount() {
    let c = travelers.length;
    for (const id in nodes) {
      const n = nodes[id];
      if (n.type === 'dispatch' || n.type === 'intake') continue;
      c += n.buffer.length + n.output.length + n.slots.length;
    }
    // Also include any units still on the intake holds waiting to leave
    c += nodes['SCRAP'].output.length + nodes['SAND'].output.length
       + nodes['CORE'].output.length  + nodes['BOX'].output.length;
    return c;
  }

  // ============================================================
  //  Drawing - background, walls, grass, trees
  // ============================================================
  // Pre-generated trees so they don't move every frame
  const trees = [];
  function regenerateTrees() {
    trees.length = 0;
    const wp = WALL_PAD;
    // top strip
    for (let x = 12; x < W - 12; x += 36) {
      trees.push({ x: x + (Math.random() - 0.5) * 10, y: wp / 2 + (Math.random() - 0.5) * 10,
                   r: 10 + Math.random() * 6, ph: Math.random() * Math.PI * 2 });
    }
    // bottom strip
    for (let x = 12; x < W - 12; x += 36) {
      trees.push({ x: x + (Math.random() - 0.5) * 10, y: H - wp / 2 + (Math.random() - 0.5) * 10,
                   r: 10 + Math.random() * 6, ph: Math.random() * Math.PI * 2 });
    }
    // left strip
    for (let y = wp + 12; y < H - wp - 12; y += 42) {
      trees.push({ x: wp / 2, y: y + (Math.random() - 0.5) * 10,
                   r: 9 + Math.random() * 5, ph: Math.random() * Math.PI * 2 });
    }
    // right strip
    for (let y = wp + 12; y < H - wp - 12; y += 42) {
      trees.push({ x: W - wp / 2, y: y + (Math.random() - 0.5) * 10,
                   r: 9 + Math.random() * 5, ph: Math.random() * Math.PI * 2 });
    }
  }

  function drawBackground(now) {
    // outer surround — dark steel gray
    ctx.fillStyle = '#3a3e42';
    ctx.fillRect(0, 0, W, H);
    // factory floor — lighter steel
    ctx.fillStyle = '#c8c8c6';
    ctx.fillRect(WALL_PAD, WALL_PAD, W - WALL_PAD * 2, H - WALL_PAD * 2);
    // factory boundary — single steel-gray frame
    ctx.strokeStyle = '#6a6e72';
    ctx.lineWidth = 3;
    ctx.strokeRect(WALL_PAD, WALL_PAD, W - WALL_PAD * 2, H - WALL_PAD * 2);
    ctx.lineWidth = 1;
    ctx.strokeRect(WALL_PAD + 5, WALL_PAD + 5, W - WALL_PAD * 2 - 10, H - WALL_PAD * 2 - 10);

    // gates (cuts in the wall) at left and right
    ctx.fillStyle = '#3a3e42';
    ctx.fillRect(WALL_PAD - 2, nodes['SCRAP'].y - 14, 8, 28);
    ctx.fillRect(WALL_PAD - 2, nodes['CORE'].y  - 14, 8, 28);
    ctx.fillRect(WALL_PAD - 2, nodes['SAND'].y  - 14, 8, 28);
    ctx.fillRect(W - WALL_PAD - 6, nodes['DISPATCH'].y - 14, 8, 28);

    // labels for the gates
    ctx.fillStyle = '#ccc';
    ctx.font = 'bold 9px Segoe UI';
    ctx.textAlign = 'center';
    ctx.fillText('SCRAP IN', WALL_PAD / 2, nodes['SCRAP'].y - 4);
    ctx.fillText('CORE IN',  WALL_PAD / 2, nodes['CORE'].y  - 4);
    ctx.fillText('SAND IN',  WALL_PAD / 2, nodes['SAND'].y  - 4);
    ctx.fillText('DISPATCH', W - WALL_PAD / 2, nodes['DISPATCH'].y - 4);
  }

  function drawTrees(now) {
    const t = now / 800;
    for (const tr of trees) {
      const sway = Math.sin(t + tr.ph) * 1.5;
      // trunk
      ctx.fillStyle = '#5a3e1f';
      ctx.fillRect(tr.x - 1.5, tr.y, 3, tr.r * 0.6);
      // foliage (3 stacked circles for a pine-ish look)
      ctx.fillStyle = '#2e6b30';
      ctx.beginPath();
      ctx.arc(tr.x + sway,        tr.y - tr.r * 0.2,  tr.r * 0.85, 0, Math.PI * 2);
      ctx.arc(tr.x + sway * 0.7,  tr.y - tr.r * 0.7,  tr.r * 0.65, 0, Math.PI * 2);
      ctx.arc(tr.x + sway * 0.4,  tr.y - tr.r * 1.1,  tr.r * 0.45, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#3f8a44';
      ctx.beginPath();
      ctx.arc(tr.x + sway - tr.r * 0.2, tr.y - tr.r * 0.5, tr.r * 0.35, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // ============================================================
  //  Drawing - conveyors and stations
  // ============================================================
  // Decide where to leave 'a' and where to enter 'b', so the conveyor exits
  // the correct face (right/left/top/bottom). Then route as an L (or two-Ls).
  function nodeRadius(n) {
    if (n.shape === 'diamond') return 12;
    if (n.shape === 'hold')    return Math.max(STATION_W * 0.7, STATION_H * 1.2);
    return STATION_W / 2 + PALLET_W + 2;
  }
  function nodeRadiusV(n) {
    if (n.shape === 'diamond') return 12;
    if (n.shape === 'hold')    return STATION_H * 1.2;
    return STATION_H / 2 + 2;
  }

  function exitPoint(n, towardX, towardY) {
    // Pick which face to exit on, based on direction toward target.
    const dx = towardX - n.x, dy = towardY - n.y;
    if (Math.abs(dx) >= Math.abs(dy) * 0.8) {
      // horizontal
      const r = nodeRadius(n);
      return { x: n.x + Math.sign(dx || 1) * r, y: n.y, axis: 'h' };
    }
    const rv = nodeRadiusV(n);
    return { x: n.x, y: n.y + Math.sign(dy || 1) * rv, axis: 'v' };
  }

  // Lane offset for one of several conveyors leaving the same source. We push
  // each branch's horizontal channel into a distinct lane so a furnace fanning
  // out to A1/A2/KW does not visually overlap A1/A2 when the destination is KW.
  function laneOffset(fromId, toId) {
    const sibs = edgeTargets(fromId);
    if (sibs.length < 2) return 0;
    const idx = sibs.indexOf(toId);
    if (idx < 0) return 0;
    // Centred lanes: -N/2 .. +N/2, 14 px apart.
    const mid = (sibs.length - 1) / 2;
    return (idx - mid) * 14;
  }

  function conveyorPoints(a, b) {
    const ep = exitPoint(a, b.x, b.y);
    const sp = exitPoint(b, a.x, a.y);
    // Lane offset moves the horizontal segment vertically (and the vertical
    // segment horizontally) so sibling conveyors stay visually disjoint.
    const lane = laneOffset(a.id, b.id);
    let p1, p2;
    if (ep.axis === 'h' && sp.axis === 'h') {
      const mx = (ep.x + sp.x) / 2 + lane;     // jitter the bend column
      p1 = { x: mx, y: ep.y };
      p2 = { x: mx, y: sp.y };
    } else if (ep.axis === 'v' && sp.axis === 'v') {
      const my = (ep.y + sp.y) / 2 + lane;     // jitter the bend row
      p1 = { x: ep.x, y: my };
      p2 = { x: sp.x, y: my };
    } else if (ep.axis === 'h' && sp.axis === 'v') {
      // exit horizontally, enter from top/bottom: route via a clearly-separated
      // horizontal channel above (or below) the destination row.
      const channelY = ep.y + lane;
      p1 = { x: sp.x, y: channelY };
      p2 = p1;
    } else {
      // exit vertically, enter horizontally: clear vertical channel.
      const channelX = ep.x + lane;
      p1 = { x: channelX, y: sp.y };
      p2 = p1;
    }
    return [ep, p1, p2, sp];
  }

  function drawConveyor(a, b) {
    const p = conveyorPoints(a, b);
    ctx.strokeStyle = '#555a5e';
    ctx.lineWidth = 7;
    ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(p[0].x, p[0].y);
    for (let i = 1; i < p.length; i++) ctx.lineTo(p[i].x, p[i].y);
    ctx.stroke();
    ctx.strokeStyle = '#9a9ea2';
    ctx.lineWidth = 1.2;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.moveTo(p[0].x, p[0].y);
    for (let i = 1; i < p.length; i++) ctx.lineTo(p[i].x, p[i].y);
    ctx.stroke();
    ctx.setLineDash([]);
  }

  function pointAlong(a, b, t) {
    const p = conveyorPoints(a, b);
    const segs = [[p[0], p[1]], [p[1], p[2]], [p[2], p[3]]];
    const lens = segs.map(s => Math.hypot(s[1].x - s[0].x, s[1].y - s[0].y));
    const total = lens[0] + lens[1] + lens[2] || 1;
    let d = t * total;
    for (let i = 0; i < segs.length; i++) {
      if (d <= lens[i] || i === segs.length - 1) {
        const r = lens[i] === 0 ? 0 : Math.max(0, Math.min(1, d / lens[i]));
        return { x: segs[i][0].x + (segs[i][1].x - segs[i][0].x) * r,
                 y: segs[i][0].y + (segs[i][1].y - segs[i][0].y) * r };
      }
      d -= lens[i];
    }
    return p[3];
  }

  function drawDispatcher(a, targets) {
    if (!targets || targets.length < 2) return;
    const dx = a.x + STATION_W / 2 + PALLET_W + 12;
    const dy = a.y;
    ctx.save();
    ctx.translate(dx, dy);
    ctx.rotate(Math.PI / 4);
    ctx.fillStyle = '#e8c060';
    ctx.strokeStyle = '#5a3e1f';
    ctx.lineWidth = 1.2;
    ctx.fillRect(-6, -6, 12, 12);
    ctx.strokeRect(-6, -6, 12, 12);
    ctx.restore();
  }

  function drawHold(n) {
    const w = STATION_W * 1.3, h = STATION_H * 2.4;
    const x = n.x - w / 2, y = n.y - h / 2;
    const isOut = n.type === 'dispatch';
    ctx.fillStyle = isOut ? '#7a5a5a' : '#5a6a5a';
    ctx.strokeStyle = '#4a4e52';
    ctx.lineWidth = 2;
    ctx.fillRect(x, y, w, h);
    ctx.strokeRect(x, y, w, h);
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 10px Segoe UI';
    ctx.textAlign = 'center';
    ctx.fillText(n.label, n.x, y - 4);
    if (isOut) {
      ctx.font = 'bold 18px Segoe UI';
      ctx.fillText('\u03A3 ' + n.output.length, n.x, n.y + 6);
    } else {
      ctx.font = 'bold 18px Segoe UI';
      ctx.fillText(String(n.output.length), n.x, n.y + 6);
      ctx.font = '9px Segoe UI';
      ctx.fillText('queued', n.x, n.y + 18);
    }
  }

  function drawFurnace(n, hotIn, hotOut) {
    const w = STATION_W, h = STATION_H * 1.3;
    const x = n.x - w / 2, y = n.y - h / 2;
    // chimney
    ctx.fillStyle = '#555a5e';
    ctx.fillRect(n.x - 6, y - 14, 12, 14);
    // body
    let bodyFill = '#6e3a2a';
    if (hotIn) bodyFill = '#a04040';
    else if (hotOut) bodyFill = '#b0935a';
    ctx.fillStyle = bodyFill;
    ctx.strokeStyle = '#3a3e42';
    ctx.lineWidth = 2;
    ctx.fillRect(x, y, w, h);
    ctx.strokeRect(x, y, w, h);
    // glowing mouth
    const glow = (Math.sin(performance.now() / 200) + 1) / 2;
    ctx.fillStyle = `rgba(200,${110 + glow * 50},${30 + glow * 25},0.85)`;
    ctx.fillRect(x + 6, y + h - 14, w - 12, 8);
    // flames
    ctx.fillStyle = `rgba(200,${140 + glow * 40},60,0.6)`;
    ctx.beginPath();
    ctx.moveTo(x + 8,     y + h - 14);
    ctx.lineTo(x + w / 2, y + h - 22 - glow * 4);
    ctx.lineTo(x + w - 8, y + h - 14);
    ctx.closePath();
    ctx.fill();

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 11px Segoe UI';
    ctx.textAlign = 'center';
    ctx.fillText(n.label, n.x, n.y - 2);
    ctx.font = '9px Segoe UI';
    ctx.fillText(formatRate(n), n.x, n.y + 9);
  }

  function drawLine(n, hotIn, hotOut) {
    // long horizontal line + a small "pour" indicator above it
    const w = STATION_W * 1.05, h = STATION_H * 0.8;
    const x = n.x - w / 2, y = n.y - h / 2;
    const busy = n.slots.length > 0;
    let body = '#5a5e62';
    if (hotIn) body = '#a04040';
    else if (hotOut) body = '#b0935a';
    else if (busy) body = '#6e7276';
    ctx.fillStyle = body;
    ctx.strokeStyle = '#3a3e42';
    ctx.lineWidth = 2;
    ctx.fillRect(x, y, w, h);
    ctx.strokeRect(x, y, w, h);

    // pour spout above the line
    ctx.fillStyle = '#555a5e';
    ctx.fillRect(n.x - 4, y - 14, 8, 12);
    if (busy) {
      // pour stream
      ctx.fillStyle = `rgba(200,110,40,0.75)`;
      ctx.fillRect(n.x - 2, y - 2, 4, 8);
    }

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 11px Segoe UI';
    ctx.textAlign = 'center';
    ctx.fillText(n.label, n.x, n.y + 3);
    ctx.font = '9px Segoe UI';
    ctx.fillText(formatRate(n), n.x, n.y + 14);
  }

  function drawStation(n, showLabels, showRates) {
    const inOver  = n.buffer.length > BOTTLENECK_THRESHOLD;
    const outOver = n.output.length > BOTTLENECK_THRESHOLD;

    // pallets
    ctx.fillStyle = '#8a8e92';
    ctx.strokeStyle = '#5a5e62';
    ctx.lineWidth = 1.3;
    ctx.fillRect(n.x - STATION_W / 2 - PALLET_W, n.y - PALLET_H / 2, PALLET_W, PALLET_H);
    ctx.strokeRect(n.x - STATION_W / 2 - PALLET_W, n.y - PALLET_H / 2, PALLET_W, PALLET_H);
    ctx.fillRect(n.x + STATION_W / 2, n.y - PALLET_H / 2, PALLET_W, PALLET_H);
    ctx.strokeRect(n.x + STATION_W / 2, n.y - PALLET_H / 2, PALLET_W, PALLET_H);

    if (n.shape === 'furnace') {
      drawFurnace(n, inOver, outOver);
    } else if (n.shape === 'line') {
      drawLine(n, inOver, outOver);
    } else {
      const busy = n.slots.length > 0;
      let body = '#4e5660';
      if (inOver) body = '#a04040';
      else if (outOver) body = '#b0935a';
      else if (busy) body = '#5a6470';
      ctx.fillStyle = body;
      ctx.strokeStyle = '#3a3e42';
      ctx.lineWidth = 2;
      ctx.fillRect(n.x - STATION_W / 2, n.y - STATION_H / 2, STATION_W, STATION_H);
      ctx.strokeRect(n.x - STATION_W / 2, n.y - STATION_H / 2, STATION_W, STATION_H);
      const txtCol = (inOver || outOver) ? '#1a1a1a' : '#fff';
      ctx.fillStyle = txtCol;
      ctx.textAlign = 'center';
      if (showLabels) {
        ctx.font = 'bold 11px Segoe UI';
        ctx.fillText(n.label, n.x, n.y + 1);
      }
      if (showRates) {
        ctx.font = '9px Segoe UI';
        ctx.fillText(formatRate(n), n.x, n.y + 13);
      }
    }

    drawPalletStack(n.buffer, n.x - STATION_W / 2 - PALLET_W, n.y);
    drawPalletStack(n.output, n.x + STATION_W / 2,            n.y);
    drawBadge(n.x - STATION_W / 2 - PALLET_W / 2, n.y - PALLET_H / 2 - 6, n.buffer.length, inOver);
    drawBadge(n.x + STATION_W / 2 + PALLET_W / 2, n.y - PALLET_H / 2 - 6, n.output.length, outOver);

    // progress bar - shows the slot closest to completion (most-advanced)
    if (n.slots.length > 0) {
      let best = 0;
      for (const s of n.slots) {
        const pct = s.jobTime > 0 ? (1 - s.remaining / s.jobTime) : 1;
        if (pct > best) best = pct;
      }
      ctx.fillStyle = '#222';
      ctx.fillRect(n.x - STATION_W / 2, n.y + STATION_H / 2 + 2, STATION_W, 3);
      ctx.fillStyle = '#f5d76e';
      ctx.fillRect(n.x - STATION_W / 2, n.y + STATION_H / 2 + 2, STATION_W * best, 3);
    }

    // BATCH overlay (furnace, cooler...) - charging fill while accumulating,
    // then a label of either 'charge X/N' or 'running Ns'.
    if (n.type === 'batch') {
      const barW = STATION_W, barX = n.x - STATION_W / 2, barY = n.y + STATION_H / 2 + 7;
      ctx.fillStyle = '#222';
      ctx.fillRect(barX, barY, barW, 4);
      let frac = 0, color = '#a04040';
      if (n.slots.length > 0) {
        // Show progress of the slot closest to completion
        let best = 0;
        for (const s of n.slots) {
          const p = s.jobTime > 0 ? (1 - s.remaining / s.jobTime) : 0;
          if (p > best) best = p;
        }
        frac = best;
        color = '#7a9a7a';
      } else {
        frac = Math.min(1, n.buffer.length / Math.max(1, n.batchSize));
      }
      ctx.fillStyle = color;
      ctx.fillRect(barX, barY, barW * Math.max(0, Math.min(1, frac)), 4);
      ctx.fillStyle = '#bbb';
      ctx.font = '9px Segoe UI';
      ctx.textAlign = 'center';
      const txt = n.slots.length > 0
        ? n.slots.length + 'x ' + Math.ceil(Math.min(...n.slots.map(s => s.remaining))) + 's'
        : n.buffer.length + '/' + n.batchSize;
      ctx.fillText(txt, n.x, barY + 14);
    }
    // replica chip in the top-right corner of the station body
    if (n.boost > 1 && n.shape !== 'hold') {
      ctx.fillStyle = '#2a2e32';
      ctx.beginPath(); ctx.arc(n.x + STATION_W / 2 - 6, n.y - STATION_H / 2 + 6, 7, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = '#c8c0b0';
      ctx.font = 'bold 9px Segoe UI';
      ctx.textAlign = 'center';
      ctx.fillText('x' + n.boost, n.x + STATION_W / 2 - 6, n.y - STATION_H / 2 + 9);
    }
  }

  function drawPalletStack(arr, x0, cy) {
    const max = Math.min(arr.length, 4);
    for (let i = 0; i < max; i++) {
      const px = x0 + PALLET_W * 0.25 + (i % 2) * (PALLET_W * 0.4);
      const py = cy - PALLET_H * 0.25 + Math.floor(i / 2) * (PALLET_H * 0.4);
      drawUnit(px, py, arr[i], 5);
    }
  }

  function drawBadge(x, y, c, hot) {
    if (c === 0) return;
    ctx.fillStyle = hot ? '#a04040' : 'rgba(40,44,48,0.8)';
    ctx.beginPath(); ctx.arc(x, y, 8, 0, Math.PI * 2); ctx.fill();
    if (hot) { ctx.strokeStyle = '#fff'; ctx.lineWidth = 1.3; ctx.stroke(); }
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 10px Segoe UI';
    ctx.textAlign = 'center';
    ctx.fillText(String(c), x, y + 3);
  }

  function drawTravelers() {
    for (const tr of travelers) {
      const a = nodes[tr.fromId], b = nodes[tr.toId];
      const t = Math.max(0, Math.min(1, tr.t));
      const p = isBypassArc(tr.fromId, tr.toId)
        ? pointAlongArc(a, b, t)
        : pointAlong(a, b, t);
      drawUnit(p.x, p.y, tr.unit, 8);
    }
  }

  // Letter-based unit renderer. The unit's lineage code (e.g. "B-YSCP") is
  // shown as a coloured pill: background = box colour, text = the code.
  // Auxiliary materials with no code (sand, scrap, raw core, fresh metal) get
  // a small placeholder letter.
  function unitGlyph(unit) {
    if (unit.code) return unit.code;
    switch (unit.kind) {
      case 'sand':    return 's';
      case 'scrap':   return 'x';
      case 'coreraw': return 'c';
      case 'core':    return 'C';
      case 'metal':   return 'P';
      case 'final':   return 'OK';
      default:        return '?';
    }
  }
  function unitFill(unit) {
    if (unit.color && COLOR_HEX[unit.color]) return COLOR_HEX[unit.color];
    switch (unit.kind) {
      case 'sand':    return '#d4b06a';
      case 'scrap':   return '#666';
      case 'coreraw': return '#9c8a70';
      case 'core':    return '#e6d6a8';
      case 'metal':   return '#ff7a20';
      case 'final':   return '#5a8a5a';
      default:        return '#888';
    }
  }
  function unitTextColor(unit) {
    // Dark text on light fill, light text on dark fill.
    if (unit.kind === 'metal' || unit.kind === 'scrap' || unit.color === 'blue' ||
        unit.color === 'gray' || unit.kind === 'final') return '#fff';
    return '#1a1a1a';
  }

  function drawUnit(x, y, unit, scale) {
    const text = unitGlyph(unit);
    const fontSize = Math.max(8, scale * 1.05);
    ctx.save();
    ctx.font = 'bold ' + fontSize + 'px Consolas, monospace';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const padX = scale * 0.4, padY = scale * 0.25;
    const tw = ctx.measureText(text).width + padX * 2;
    const th = fontSize + padY * 2;
    // pill background
    ctx.fillStyle = unitFill(unit);
    ctx.strokeStyle = '#1a1a1a';
    ctx.lineWidth = 0.8;
    const r = Math.min(th, tw) / 2;
    if (ctx.roundRect) {
      ctx.beginPath();
      ctx.roundRect(x - tw / 2, y - th / 2, tw, th, r * 0.45);
      ctx.fill(); ctx.stroke();
    } else {
      ctx.fillRect(x - tw / 2, y - th / 2, tw, th);
      ctx.strokeRect(x - tw / 2, y - th / 2, tw, th);
    }
    ctx.fillStyle = unitTextColor(unit);
    ctx.fillText(text, x, y + 0.5);
    ctx.restore();
  }

  function drawStations() {
    const showLabels = $('#ctl-labels').is(':checked');
    const showRates  = $('#ctl-rates').is(':checked');
    for (const id in nodes) {
      const n = nodes[id];
      if (n.type === 'intake' || n.type === 'dispatch') drawHold(n);
      else if (n.type === 'bypass')                     drawDiamond(n);
      else                                              drawStation(n, showLabels, showRates);
    }
  }

  function drawDiamond(n) {
    const r = 12;
    ctx.save();
    ctx.translate(n.x, n.y);
    ctx.rotate(Math.PI / 4);
    ctx.fillStyle = '#b0935a';
    ctx.strokeStyle = '#5a5044';
    ctx.lineWidth = 1.5;
    ctx.fillRect(-r, -r, r * 2, r * 2);
    ctx.strokeRect(-r, -r, r * 2, r * 2);
    ctx.restore();
    ctx.fillStyle = '#1a1a1a';
    ctx.font = 'bold 9px Segoe UI';
    ctx.textAlign = 'center';
    ctx.fillText('?', n.x, n.y + 3);
    // small id below
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 9px Segoe UI';
    ctx.fillText(n.id, n.x, n.y + r + 12);
  }

  // Set of bypass "skip" edges that should render as arcs instead of conveyors.
  // Each entry: from→to where the edge jumps over a process station.
  const BYPASS_ARC_EDGES = new Set(['BS->BH', 'BH->BM', 'BM->BP', 'BP->QC']);
  function isBypassArc(fromId, toId) { return BYPASS_ARC_EDGES.has(fromId + '->' + toId); }

  function drawBypassArc(a, b) {
    // Curved arc below the two nodes (they share row 2, arc dips downward).
    const x1 = a.x, y1 = a.y;
    const x2 = b.x, y2 = b.y;
    const mx = (x1 + x2) / 2;
    const dip = 38;   // how far below the node centres the arc peaks
    const cy = Math.max(y1, y2) + dip;
    ctx.strokeStyle = '#8a7a5a';
    ctx.lineWidth = 2;
    ctx.setLineDash([6, 4]);
    ctx.beginPath();
    ctx.moveTo(x1, y1 + 12);            // start below diamond
    ctx.quadraticCurveTo(mx, cy, x2, y2 + 12);   // arc below
    ctx.stroke();
    ctx.setLineDash([]);
    // Small arrowhead at the destination end
    const t = 0.95;   // near the end
    const px = (1 - t) * (1 - t) * x1 + 2 * (1 - t) * t * mx + t * t * x2;
    const py = (1 - t) * (1 - t) * (y1 + 12) + 2 * (1 - t) * t * cy + t * t * (y2 + 12);
    const dx = (x2) - px, dy = (y2 + 12) - py;
    const len = Math.hypot(dx, dy) || 1;
    const ux = dx / len, uy = dy / len;
    ctx.fillStyle = '#8a7a5a';
    ctx.beginPath();
    ctx.moveTo(x2, y2 + 12);
    ctx.lineTo(x2 - ux * 7 - uy * 4, y2 + 12 - uy * 7 + ux * 4);
    ctx.lineTo(x2 - ux * 7 + uy * 4, y2 + 12 - uy * 7 - ux * 4);
    ctx.closePath();
    ctx.fill();
  }

  // Point along a bypass arc (for travelers).
  function pointAlongArc(a, b, t) {
    const x1 = a.x, y1 = a.y + 12;
    const x2 = b.x, y2 = b.y + 12;
    const mx = (x1 + x2) / 2;
    const cy = Math.max(a.y, b.y) + 38;
    const u = Math.max(0, Math.min(1, t));
    return {
      x: (1 - u) * (1 - u) * x1 + 2 * (1 - u) * u * mx + u * u * x2,
      y: (1 - u) * (1 - u) * y1 + 2 * (1 - u) * u * cy + u * u * y2
    };
  }

  function drawEdges() {
    for (const from in edges) {
      const a = nodes[from];
      for (const to of edgeTargets(from)) {
        if (isBypassArc(from, to)) drawBypassArc(a, nodes[to]);
        else drawConveyor(a, nodes[to]);
      }
    }
  }

  // ============================================================
  //  Main loop
  // ============================================================
  let frameCount = 0, fpsT = performance.now();
  function frame(now) {
    requestAnimationFrame(frame);
    const dt = Math.min(0.1, ((now - lastT) || 16) / 1000) * speed;
    lastT = now;

    if (running && !pause) {
      tickStations(dt);
      tickTravelers(dt);
      // sim-time stopwatch + idle-stop watchdog
      runElapsedSec += dt;
      if (totalDispatched > lastDispatchedSeen) {
        lastDispatchedSeen = totalDispatched;
        lastDispatchSec = runElapsedSec;
      }
      if (batchSize > 0) {
        const done = totalDispatched + rejects.total;
        // Hard finish: everything accounted for.
        if (done >= batchSize) {
          running = false;
          runFinalSec = runElapsedSec;
          runStopReason = 'BATCH_DONE';
        } else if ((done / batchSize) >= STOP_PCT &&
                   (runElapsedSec - lastDispatchSec) >= STOP_IDLE) {
          // Soft finish: ≥ 80% done and no dispatch for 30s sim time.
          // Subtract the idle wait so the reported time reflects useful work.
          running = false;
          runFinalSec = Math.max(0, runElapsedSec - STOP_IDLE);
          runStopReason = 'IDLE_STOP';
        }
      }
    }

    drawBackground(now);
    drawBoostGauge();
    drawEdges();
    drawStations();
    drawTravelers();

    sampleRates(now);
    samplePlantRate(now);
    renderGauges(now);

    const inFlight = inFlightCount();
    $('#stat-flight').text(inFlight);
    $('#stat-out').text(totalDispatched);
    $('#stat-batch').text(batchSize);
    $('#stat-rej').text(rejects.total);
    $('#rej-yellow').text(rejects.yellow);
    $('#rej-blue').text(rejects.blue);
    $('#rej-green').text(rejects.green);
    $('#rej-gray').text(rejects.gray);
    renderPlantRate();

    if (running && batchSize > 0 && (totalDispatched + rejects.total) >= batchSize) {
      // (Defensive: the watchdog above usually flips `running` first.)
      running = false;
      if (runFinalSec === null) { runFinalSec = runElapsedSec; runStopReason = 'BATCH_DONE'; }
    }
    if (running) {
      $('#ctl-status').text('RUNNING');
    } else if (batchSize === 0) {
      $('#ctl-status').text('IDLE');
    } else {
      $('#ctl-status').text(runStopReason === 'IDLE_STOP' ? 'STOPPED (idle)' : 'FINISHED');
    }
    renderRunTime();

    frameCount++;
    if (now - fpsT > 1000) {
      $('#stat-fps').text(Math.round(frameCount * 1000 / (now - fpsT)));
      frameCount = 0; fpsT = now;
    }
  }

  // ============================================================
  //  Boot
  // ============================================================
  function boot() {
    resize();
    regenerateTrees();
    requestAnimationFrame(frame);
  }

  // ============================================================
  //  Helpers / Control panel (tabbed)
  // ============================================================
  function formatRate(n) {
    return n.processTime.toFixed(1) + 's' + (n.boost > 1 ? ' x' + n.boost : '');
  }

  let activeTab = 'boost';
  const TAB_HINTS = {
    boost:       'Boost level (parallel processing slots). Conserved: adding to one node takes from the pool. Pool shown in the top gauge.',
    weight:      'Routing weights for branching dispatchers. Each value is a probability share (0-1).',
    processTime: 'Per-job process time in seconds (0-60). Lower it to speed up the station; furnaces use this as melt time.'
  };

  function buildControlPanel() {
    $('#tab-strip .tab').removeClass('active').filter('[data-tab="' + activeTab + '"]').addClass('active');
    $('#tab-hint').text(TAB_HINTS[activeTab] || '');
    const $host = $('#node-controls').empty();
    for (const id in nodes) {
      const n = nodes[id];
      if (n.type === 'intake' || n.type === 'dispatch') continue;
      const targets = edgeTargets(n.id);

      if (activeTab === 'weight' && targets.length < 2) continue;

      const $card = $('<div class="ctl-card"></div>');
      $card.append('<div class="ctl-card-h"><b>' + n.label + '</b> <span class="ctl-id">' + n.id + '</span></div>');

      if (activeTab === 'boost') {
        // Only boostable nodes get a boost slider
        if (n.type === 'bypass') continue;
        $card.append(
          '<label class="ctl-line">Boost <b class="v">' + n.boost + '</b>' +
          '<input type="range" min="1" max="' + MAX_BOOST + '" step="1" value="' + n.boost +
          '" data-node="' + n.id + '" data-knob="boost" /></label>'
        );
      } else if (activeTab === 'processTime') {
        $card.append(
          '<label class="ctl-line">Proc time <b class="v">' + n.processTime.toFixed(1) + 's</b>' +
          '<input type="range" min="0" max="' + MAX_PROCESS_TIME + '" step="0.5" value="' + n.processTime +
          '" data-node="' + n.id + '" data-knob="processTime" /></label>'
        );
      } else if (activeTab === 'weight') {
        for (const t of targets) {
          const w = (edgeWeights[n.id][t] || 0);
          $card.append(
            '<label class="ctl-line">&rarr; ' + t + ' <b class="v">' + w.toFixed(2) + '</b>' +
            '<input type="range" min="0" max="1" step="0.01" value="' + w +
            '" data-node="' + n.id + '" data-knob="weight" data-target="' + t + '" /></label>'
          );
        }
      }
      $host.append($card);
    }
  }

  function bindControlHandlers() {
    $('#tab-strip').on('click', '.tab', function () {
      activeTab = $(this).data('tab');
      buildControlPanel();
    });
    $('#node-controls').on('input', 'input[type=range]', function () {
      const $i = $(this);
      const id = $i.data('node');
      const knob = $i.data('knob');
      const v = parseFloat(this.value);
      const $v = $i.parent().find('.v').first();
      if (knob === 'boost') {
        const want = Math.max(1, Math.round(v));
        nodes[id].boost = want;
        $v.text(nodes[id].boost);
      } else if (knob === 'processTime') {
        nodes[id].processTime = Math.max(0, v);
        $v.text(v.toFixed(1) + 's');
      } else if (knob === 'weight') {
        const t = $i.data('target');
        edgeWeights[id][t] = v;
        $v.text(v.toFixed(2));
      }
    });
  }

  // ============================================================
  //  Rate sampling + Gauge device
  // ============================================================
  let lastSampleT = 0;
  function sampleRates(now) {
    if (!lastSampleT) lastSampleT = now;
    const ddt = (now - lastSampleT) / 1000;
    if (ddt < 0.25) return;          // sample 4x per real second
    lastSampleT = now;
    const alpha = 0.35;              // smoothing factor
    for (const id in nodes) {
      const n = nodes[id];
      const m = n.metrics;
      const dIn  = (n.totalIn  - m.lastIn)  / ddt;
      const dOut = (n.totalOut - m.lastOut) / ddt;
      const dInBuf  = (n.buffer.length - m.lastBuf)    / ddt;
      const dOutBuf = (n.output.length - m.lastOutBuf) / ddt;
      m.inRate   = m.inRate   * (1 - alpha) + dIn  * alpha;
      m.outRate  = m.outRate  * (1 - alpha) + dOut * alpha;
      m.inSlope  = m.inSlope  * (1 - alpha) + dInBuf  * alpha;
      m.outSlope = m.outSlope * (1 - alpha) + dOutBuf * alpha;
      m.lastIn = n.totalIn; m.lastOut = n.totalOut;
      m.lastBuf = n.buffer.length; m.lastOutBuf = n.output.length;
    }
  }

  // -------- Plant production rate + 30s trend --------
  // Ring buffer of (wallSeconds, totalDispatched) snapshots, ~1 per sec.
  const PLANT_WINDOW_SEC = 30;
  const plantSamples = [];          // [{ t, n }]
  let lastPlantSampleT = 0;
  let plantRate = 0;                // smoothed units/sec over window
  let plantTrend = 0;               // delta vs older half

  function samplePlantRate(now) {
    if (!lastPlantSampleT) lastPlantSampleT = now;
    if (now - lastPlantSampleT < 1000) return;
    lastPlantSampleT = now;
    plantSamples.push({ t: now, n: totalDispatched });
    while (plantSamples.length > 0 &&
           (now - plantSamples[0].t) > (PLANT_WINDOW_SEC + 2) * 1000) {
      plantSamples.shift();
    }
    if (plantSamples.length >= 2) {
      const a = plantSamples[0], b = plantSamples[plantSamples.length - 1];
      const ddt = (b.t - a.t) / 1000;
      plantRate = ddt > 0 ? (b.n - a.n) / ddt : 0;

      // Trend: rate over the most recent half vs the older half of the window.
      const mid = plantSamples[Math.floor(plantSamples.length / 2)];
      const dt1 = (mid.t - a.t) / 1000;
      const dt2 = (b.t - mid.t) / 1000;
      const r1 = dt1 > 0 ? (mid.n - a.n) / dt1 : 0;
      const r2 = dt2 > 0 ? (b.n - mid.n) / dt2 : 0;
      plantTrend = r2 - r1;
    } else {
      plantRate = 0; plantTrend = 0;
    }
  }

  function renderPlantRate() {
    $('#stat-rate').text(plantRate.toFixed(2));
    const $tr = $('#stat-trend');
    // Threshold to ignore noise around zero.
    const eps = Math.max(0.05, plantRate * 0.05);
    let cls = 'flat', sym = '\u2192';   // →
    if (plantTrend >  eps) { cls = 'up';   sym = '\u2191'; }   // ↑
    if (plantTrend < -eps) { cls = 'down'; sym = '\u2193'; }   // ↓
    $tr.removeClass('up down flat').addClass(cls).text(sym)
       .attr('title', '30s trend: ' + (plantTrend >= 0 ? '+' : '') + plantTrend.toFixed(2) + ' u/s');
    // Bar fill normalised to a soft cap of 5 u/s (full scale).
    const pct = Math.max(0, Math.min(100, (plantRate / 5) * 100));
    $('#stat-rate-bar .rate-bar-fill').css('width', pct + '%');
  }

  // mm:ss formatter
  function fmtMMSS(sec) {
    if (!isFinite(sec) || sec < 0) sec = 0;
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return m + ':' + (s < 10 ? '0' : '') + s;
  }

  // Header run-time field. Live ticks while running; freezes to runFinalSec
  // (with the 30s idle wait subtracted on IDLE_STOP) once stopped.
  function renderRunTime() {
    const $t = $('#stat-time');
    if (!$t.length) return;
    if (batchSize === 0) { $t.text('-').attr('title', 'No batch started'); return; }
    if (runFinalSec !== null) {
      const note = runStopReason === 'IDLE_STOP'
        ? 'Stopped: \u226580% dispatched + 30s idle (30s wait subtracted)'
        : 'Batch fully accounted';
      $t.text(fmtMMSS(runFinalSec)).attr('title', note);
      $t.toggleClass('final', true);
    } else {
      $t.text(fmtMMSS(runElapsedSec)).attr('title', 'Sim time elapsed (live)');
      $t.toggleClass('final', false);
    }
  }

  // ============================================================
  //  Conserved boost pool + gauge drawn in outer wall area
  // ============================================================
  // The pool is: sum of all boostable nodes' INIT_BOOST.
  // Current consumption = sum of all boostable nodes' actual boost.
  // Reserve = pool - consumption.  Reserve > 0 means we have spare boost.
  // Reserve < 0 means we've over-drawn.
  // At init (all boost = INIT_BOOST), reserve = 0.

  function boostableNodes() {
    const out = [];
    for (const id in nodes) {
      const n = nodes[id];
      if (n.type === 'intake' || n.type === 'dispatch' || n.type === 'bypass') continue;
      out.push(n);
    }
    return out;
  }
  function boostPoolTotal() {
    return boostableNodes().length * INIT_BOOST;
  }
  function boostUsed() {
    let s = 0;
    for (const n of boostableNodes()) s += n.boost;
    return s;
  }
  function boostPool() {
    return boostPoolTotal() - boostUsed();
  }

  // Draw a horizontal bar gauge in the TOP outer wall strip (above the factory).
  // Boost pool dial — drawn on the main canvas in the top-left wall strip.
  // Matches the visual style of the buffer dials (drawCountDial).
  // Range: -maxRange .. 0 .. +maxRange.  Centre (0) = balanced.
  let boostNeedleAngle = null;   // lazy-init on first frame

  function drawBoostGauge() {
    const pool = boostPool();
    const maxRange = boostPoolTotal();

    // Geometry: smaller dial in a white background box
    const dialW = 75, dialH = 70;
    const boxX = WALL_PAD + 8;
    const boxY = 8;
    const boxW = dialW + 8, boxH = dialH + 8;
    
    // White background box with border
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(boxX, boxY, boxW, boxH);
    ctx.strokeStyle = '#999';
    ctx.lineWidth = 1;
    ctx.strokeRect(boxX, boxY, boxW, boxH);

    const cx = boxX + boxW / 2;
    const cy = boxY + dialH * 0.88;
    const r = Math.min(dialW * 0.46, dialH * 0.85);   // ≈ 34.5 or 59.5 → 34.5

    // Arc angles — same 230° sweep as the buffer dials
    const startA = Math.PI * 1.15;
    const endA   = Math.PI * (-0.15);
    const sweep  = endA - startA + Math.PI * 2;   // ≈ 4.08 rad (≈ 234°)

    // Three zones: deficit (left third), neutral (centre third), surplus (right third)
    const z1 = 0.33, z2 = 0.67;
    ctx.lineWidth = 6; ctx.lineCap = 'butt';
    ctx.strokeStyle = '#cf4747';   // deficit = red
    ctx.beginPath(); ctx.arc(cx, cy, r, startA, startA + sweep * z1); ctx.stroke();
    ctx.strokeStyle = '#e8c060';   // neutral = amber
    ctx.beginPath(); ctx.arc(cx, cy, r, startA + sweep * z1, startA + sweep * z2); ctx.stroke();
    ctx.strokeStyle = '#5aab63';   // surplus = green
    ctx.beginPath(); ctx.arc(cx, cy, r, startA + sweep * z2, startA + sweep); ctx.stroke();

    // Tick marks: match buffer dial density (every 10% = 11 ticks total)
    // Labels at key positions: -max (0%), 0 (50%), +max (100%)
    ctx.strokeStyle = '#555'; ctx.lineWidth = 1;
    ctx.fillStyle = '#333'; ctx.font = '8px Segoe UI'; ctx.textAlign = 'center';
    for (let i = 0; i <= 10; i++) {
      const frac = i / 10;
      const a = startA + sweep * frac;
      const isMajor = (i === 0 || i === 5 || i === 10);  // -max, 0, +max
      const r1 = r - (isMajor ? 10 : 6), r2 = r - 3;
      ctx.beginPath();
      ctx.moveTo(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
      ctx.lineTo(cx + Math.cos(a) * r2, cy + Math.sin(a) * r2);
      ctx.stroke();
      if (isMajor) {
        const lr = r - 16;
        let lbl;
        if (i === 0) lbl = -maxRange;
        else if (i === 5) lbl = 0;
        else lbl = maxRange;
        ctx.fillText((lbl > 0 ? '+' : '') + lbl, cx + Math.cos(a) * lr, cy + Math.sin(a) * lr + 3);
      }
    }

    // Needle: pool maps from -maxRange..+maxRange → 0..1 fraction of sweep
    const frac = (pool / Math.max(1, maxRange) + 1) / 2;  // 0..1
    const target = startA + sweep * Math.max(0, Math.min(1, frac));
    if (boostNeedleAngle === null) boostNeedleAngle = target;
    boostNeedleAngle = boostNeedleAngle * 0.85 + target * 0.15;
    const a = boostNeedleAngle;
    const needleColor = pool === 0 ? '#666' : (pool > 0 ? '#5aab63' : '#cf4747');
    ctx.strokeStyle = needleColor; ctx.lineWidth = 2; ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + Math.cos(a) * (r - 5), cy + Math.sin(a) * (r - 5));
    ctx.stroke();

    // Hub
    ctx.fillStyle = '#1a1a1a';
    ctx.beginPath(); ctx.arc(cx, cy, 3, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#666'; ctx.lineWidth = 1;
    ctx.beginPath(); ctx.arc(cx, cy, 3, 0, Math.PI * 2); ctx.stroke();

    // Numeric readout + label below hub (larger, high contrast)
    ctx.fillStyle = needleColor;
    ctx.font = 'bold 13px Segoe UI'; ctx.textAlign = 'center';
    const sign = pool > 0 ? '+' : '';
    ctx.fillText(sign + pool, cx, cy + 11);
    ctx.fillStyle = '#555'; ctx.font = '9px Segoe UI';
    ctx.fillText('BOOST', cx, cy + 22);
  }

  // gauge state - each tile has TWO dials (IN buffer count, OUT buffer count).
  // Each dial graduates 0..100, zones: green 0-20, yellow 20-70, red 70-100.
  const COUNT_MAX = 100;
  const ZONE_GREEN = 20;
  const ZONE_YELLOW = 70;
  const selectedDevices = new Set();     // node ids
  const gaugeNeedles = {};               // id -> { inA, outA }

  function buildGaugePicker() {
    const $p = $('#gd-picker').empty();
    for (const id in nodes) {
      const n = nodes[id];
      if (n.type === 'intake' || n.type === 'dispatch') continue;
      const checked = selectedDevices.has(id) ? 'checked' : '';
      $p.append('<label><input type="checkbox" data-id="' + id + '" ' + checked + ' /> ' +
                n.label + ' <span style="color:#888;margin-left:auto">' + id + '</span></label>');
    }
  }

  function rebuildGaugeTiles() {
    const $g = $('#gd-gauges').empty();
    selectedDevices.forEach(id => {
      const n = nodes[id];
      if (!n) return;
      const $tile = $('<div class="gauge" data-id="' + id + '">'
        + '<div class="gauge-label">' + n.label + '</div>'
        + '<div class="gauge-pair">'
        +   '<div class="gauge-half"><canvas class="gc-in"></canvas><div class="gh-cap">IN <span class="vin">0</span></div></div>'
        +   '<div class="gauge-half"><canvas class="gc-out"></canvas><div class="gh-cap">OUT <span class="vout">0</span></div></div>'
        + '</div>'
        + '</div>');
      $g.append($tile);
      if (!gaugeNeedles[id]) gaugeNeedles[id] = { inA: 0, outA: 0 };
    });
  }

  function renderGauges(now) {
    $('#gd-gauges .gauge').each(function () {
      const id = this.getAttribute('data-id');
      const n = nodes[id];
      if (!n) return;
      const needle = gaugeNeedles[id];
      drawCountDial(this.querySelector('.gc-in'),  n.buffer.length, needle, 'inA',  '#4880c8');
      drawCountDial(this.querySelector('.gc-out'), n.output.length, needle, 'outA', '#e8c060');
      const inEl  = this.querySelector('.vin');
      const outEl = this.querySelector('.vout');
      inEl.textContent  = n.buffer.length;
      outEl.textContent = n.output.length;
      inEl.style.color  = bufColor(n.buffer.length);
      outEl.style.color = bufColor(n.output.length);
    });
  }

  function bufColor(c) {
    if (c > ZONE_YELLOW) return '#cf4747';
    if (c > ZONE_GREEN)  return '#e8c060';
    return '#5aab63';
  }

  // Single half-circle dial graduated 0..COUNT_MAX. Needle eases monotonically
  // toward the current count (no slope/derivative, no shake).
  function drawCountDial(cv, count, needle, key, needleColor) {
    const dpr = window.devicePixelRatio || 1;
    const cssW = cv.clientWidth || 110, cssH = cv.clientHeight || 95;
    if (cv.width !== cssW * dpr) { cv.width = cssW * dpr; cv.height = cssH * dpr; }
    const g = cv.getContext('2d');
    g.setTransform(dpr, 0, 0, dpr, 0, 0);
    g.clearRect(0, 0, cssW, cssH);

    const cx = cssW / 2, cy = cssH * 0.92, r = Math.min(cssW * 0.46, cssH * 0.85);
    const startA = Math.PI * 1.15;
    const endA   = Math.PI * (-0.15);
    const sweep  = endA - startA + Math.PI * 2;

    // Zones at 20 and 70 out of 100
    const z1 = ZONE_GREEN  / COUNT_MAX;   // 0.20
    const z2 = ZONE_YELLOW / COUNT_MAX;   // 0.70
    g.lineWidth = 8; g.lineCap = 'butt';
    g.strokeStyle = '#5aab63';
    g.beginPath(); g.arc(cx, cy, r, startA, startA + sweep * z1); g.stroke();
    g.strokeStyle = '#e8c060';
    g.beginPath(); g.arc(cx, cy, r, startA + sweep * z1, startA + sweep * z2); g.stroke();
    g.strokeStyle = '#cf4747';
    g.beginPath(); g.arc(cx, cy, r, startA + sweep * z2, startA + sweep); g.stroke();

    // Tick marks every 10 (0,10,...,100); major every 20.
    g.strokeStyle = '#666'; g.lineWidth = 1;
    g.fillStyle = '#888';
    g.font = '9px Segoe UI';
    g.textAlign = 'center';
    for (let v = 0; v <= COUNT_MAX; v += 10) {
      const a = startA + sweep * (v / COUNT_MAX);
      const major = (v % 20 === 0);
      const r1 = r - (major ? 14 : 9), r2 = r - 5;
      g.beginPath();
      g.moveTo(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
      g.lineTo(cx + Math.cos(a) * r2, cy + Math.sin(a) * r2);
      g.stroke();
      if (major) {
        const lr = r - 22;
        g.fillText(String(v), cx + Math.cos(a) * lr, cy + Math.sin(a) * lr + 3);
      }
    }

    // Needle: ease toward current count (monotonic; smooth)
    const target = startA + sweep * Math.max(0, Math.min(1, count / COUNT_MAX));
    needle[key] = needle[key] * 0.85 + target * 0.15;
    const a = needle[key];
    g.strokeStyle = needleColor; g.lineWidth = 2.5; g.lineCap = 'round';
    g.beginPath();
    g.moveTo(cx, cy);
    g.lineTo(cx + Math.cos(a) * (r - 7), cy + Math.sin(a) * (r - 7));
    g.stroke();
    g.fillStyle = '#1a1a1a';
    g.beginPath(); g.arc(cx, cy, 4, 0, Math.PI * 2); g.fill();
    g.strokeStyle = '#888';
    g.beginPath(); g.arc(cx, cy, 4, 0, Math.PI * 2); g.stroke();
  }

  function bindGaugeDevice() {
    $('#gd-toggle').on('click', () => {
      $('#gauge-device').toggleClass('collapsed');
    });
    $('#gd-pick').on('click', () => { $('#gd-picker').toggleClass('open'); });
    $('#gd-picker').on('change', 'input[type=checkbox]', function () {
      const id = this.getAttribute('data-id');
      if (this.checked) selectedDevices.add(id); else selectedDevices.delete(id);
      rebuildGaugeTiles();
    });
  }

  $(function () {
    boot();
    buildControlPanel();
    bindControlHandlers();
    buildGaugePicker();
    bindGaugeDevice();
    // Pre-seed a few gauges so the panel isn't empty on first load
    ['MC', 'HT', 'A1', 'F1'].forEach(id => { if (nodes[id]) selectedDevices.add(id); });
    buildGaugePicker();
    rebuildGaugeTiles();

    $('#ctl-toggle-panel').on('click', () => {
      $('body').toggleClass('panel-hidden');
      setTimeout(() => { resize(); regenerateTrees(); }, 220);
    });

    window.addEventListener('resize', () => { regenerateTrees(); });

    $('#ctl-speed').on('input', e => {
      speed = parseFloat(e.target.value);
      $('#stat-speed').text(speed.toFixed(1));
    });
    $('#ctl-pause').on('change', e => { pause = e.target.checked; });
    $('#ctl-start').on('click', () => {
      const n = parseInt($('#ctl-batch').val(), 10) || 100;
      startBatch(n);
    });
    $('#ctl-reset').on('click', () => { resetSim(); });

    $('#ctl-save').on('click', () => saveConfig());
    $('#ctl-load').on('click', () => loadConfig(true));
    $('#ctl-audit').on('click', () => openAudit());
    $('#audit-close').on('click', () => closeAudit());
    $('#audit-copy').on('click',  () => copyAuditText());
    $('#audit-download').on('click', () => downloadAuditText());

    // Best-effort: try to load any saved config silently on boot.
    loadConfig(false);
  });

  // ============================================================
  //  Audit - explain where every unit currently is and why.
  // ============================================================
  let lastAuditText = '';

  // For each kind, list the nodes that produce/issue it. Used to suggest
  // what's "missing upstream" when a recipe stalls for lack of an ingredient.
  function producersOf(kind) {
    const out = [];
    for (const id in nodes) {
      const n = nodes[id];
      if (n.kindOut === kind) out.push(id);
    }
    return out;
  }

  // What kinds are missing in n.buffer to start one job (recipe-based nodes)?
  function recipeShortfall(n) {
    if (!n.recipe || !n.recipe.length) return null;
    const need = n.recipe.slice();
    const have = {};
    for (const u of n.buffer) have[u.kind] = (have[u.kind] || 0) + 1;
    const missing = [];
    const present = {};
    const tally = {};
    for (const u of n.buffer) tally[u.kind] = (tally[u.kind] || 0) + 1;
    // Try to satisfy the recipe greedily; whatever can't be matched is missing.
    const remaining = Object.assign({}, tally);
    for (const k of need) {
      if ((remaining[k] || 0) > 0) {
        remaining[k] -= 1;
        present[k] = (present[k] || 0) + 1;
      } else {
        missing.push(k);
      }
    }
    return { need: need, have: tally, missing: missing, satisfied: missing.length === 0 };
  }

  // Group an array of units by colour (or 'untagged').
  function tallyByColor(arr) {
    const t = { yellow: 0, blue: 0, green: 0, gray: 0, untagged: 0 };
    for (const u of arr) {
      if (u.color && t[u.color] !== undefined) t[u.color]++;
      else t.untagged++;
    }
    return t;
  }
  function tallyByKind(arr) {
    const t = {};
    for (const u of arr) t[u.kind] = (t[u.kind] || 0) + 1;
    return t;
  }
  function tallyByCode(arr) {
    const t = {};
    for (const u of arr) {
      const c = u.code || ('(' + (u.kind || '?') + ')');
      t[c] = (t[c] || 0) + 1;
    }
    return t;
  }
  function fmtKindMap(m) {
    const ks = Object.keys(m).sort();
    if (!ks.length) return '<em>empty</em>';
    return ks.map(k => '<code>' + k + '</code>&times;' + m[k]).join(', ');
  }
  function fmtCodeMap(m, max) {
    const entries = Object.entries(m).sort((a, b) => b[1] - a[1]);
    const trimmed = (typeof max === 'number') ? entries.slice(0, max) : entries;
    if (!entries.length) return '<em>empty</em>';
    let s = trimmed.map(([k, v]) => '<code>' + escapeHtml(k) + '</code>&times;' + v).join(', ');
    if (entries.length > trimmed.length) s += ' &hellip; +' + (entries.length - trimmed.length) + ' more';
    return s;
  }
  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  // Diagnose why a node's input buffer is non-empty (units present, none
  // currently being processed). Returns a list of human-readable reasons.
  function diagnoseStuck(n) {
    const reasons = [];
    if (n.type === 'batch') {
      if (n.slots.length === 0 && n.buffer.length < n.batchSize) {
        const need = n.batchSize - n.buffer.length;
        reasons.push('Batch is partially filled (' + n.buffer.length + '/' + n.batchSize +
                     '). Needs ' + need + ' more unit(s) to start.');
      }
      return reasons;
    }
    if (n.type === 'process' || n.type === 'line') {
      const slotsFree = n.boost - n.slots.length;
      if (slotsFree <= 0) {
        reasons.push('All ' + n.boost + ' processing slot(s) busy.');
      } else if (n.recipe && n.recipe.length) {
        const sf = recipeShortfall(n);
        if (sf && !sf.satisfied) {
          const miss = {};
          for (const k of sf.missing) miss[k] = (miss[k] || 0) + 1;
          const parts = Object.keys(miss).map(k => k + '&times;' + miss[k]);
          reasons.push('Recipe needs <code>[' + n.recipe.join(', ') + ']</code> &mdash; missing ' +
                       parts.join(', ') + '. Producers upstream: ' +
                       sf.missing.map(k => k + '&rarr;{' + (producersOf(k).join(', ') || 'none') + '}').join('; '));
        }
      }
    }
    return reasons;
  }

  // Diagnose why a node's OUTPUT buffer hasn't drained.
  function diagnoseBlockedOutput(n) {
    const reasons = [];
    const targets = edgeTargets(n.id);
    if (!targets.length) return reasons;
    // edgeCooldowns are positive when an edge just fired. They normally tick to 0
    // quickly; if all are 0 and units are still in output, it just means no
    // upstream demand right now (rare). The realistic stuck case is route hint
    // pinning the unit to a target whose edge isn't configured.
    const head = n.output[0];
    if (head && head.routeHint) {
      const cands = (typeof head.routeHint === 'string') ? [head.routeHint] : head.routeHint;
      const valid = cands.filter(t => n.edgeCooldowns[t] !== undefined);
      if (!valid.length) {
        reasons.push('Head unit pinned to <code>' + cands.join('|') +
                     '</code> but no such outgoing edge from ' + n.id + '.');
      }
    }
    return reasons;
  }

  function buildAuditModel() {
    // Walk the world. Classify every unit currently outside DISPATCH.
    const inFlight = inFlightCount();
    const stations = [];          // [{id, label, ...}]
    const accountByLoc = {        // overall accounting
      buffer: 0, slot: 0, output: 0, traveler: 0, intakeOut: 0
    };
    const accountByColor = { yellow: 0, blue: 0, green: 0, gray: 0, untagged: 0 };
    const accountByKind  = {};

    for (const id in nodes) {
      const n = nodes[id];
      const inCnt  = n.buffer.length;
      const outCnt = n.output.length;
      const slotCnt = n.slots.length;
      // count auxiliary intake outputs separately (sand/box/coreraw waiting to leave)
      if (n.type === 'intake') {
        for (const u of n.output) {
          accountByLoc.intakeOut++;
          if (u.color && accountByColor[u.color] !== undefined) accountByColor[u.color]++;
          else accountByColor.untagged++;
          accountByKind[u.kind] = (accountByKind[u.kind] || 0) + 1;
        }
      } else if (n.type !== 'dispatch') {
        for (const u of n.buffer) {
          accountByLoc.buffer++;
          if (u.color && accountByColor[u.color] !== undefined) accountByColor[u.color]++;
          else accountByColor.untagged++;
          accountByKind[u.kind] = (accountByKind[u.kind] || 0) + 1;
        }
        for (const s of n.slots) {
          if (s.out) {
            accountByLoc.slot++;
            if (s.out.color && accountByColor[s.out.color] !== undefined) accountByColor[s.out.color]++;
            else accountByColor.untagged++;
            accountByKind[s.out.kind] = (accountByKind[s.out.kind] || 0) + 1;
          } else if (s.outs) {                 // batch slot holds an array
            for (const u of s.outs) {
              accountByLoc.slot++;
              if (u.color && accountByColor[u.color] !== undefined) accountByColor[u.color]++;
              else accountByColor.untagged++;
              accountByKind[u.kind] = (accountByKind[u.kind] || 0) + 1;
            }
          }
        }
        for (const u of n.output) {
          accountByLoc.output++;
          if (u.color && accountByColor[u.color] !== undefined) accountByColor[u.color]++;
          else accountByColor.untagged++;
          accountByKind[u.kind] = (accountByKind[u.kind] || 0) + 1;
        }
      }

      if (n.type === 'dispatch' || n.type === 'intake' || n.type === 'bypass') continue;
      if (inCnt === 0 && outCnt === 0 && slotCnt === 0) continue;
      stations.push({
        id: n.id, label: n.label, type: n.type,
        boost: n.boost, batchSize: n.batchSize,
        recipe: n.recipe || null,
        inCnt: inCnt, slotCnt: slotCnt, outCnt: outCnt,
        inKinds: tallyByKind(n.buffer),
        inColors: tallyByColor(n.buffer),
        outCodes: tallyByCode(n.output),
        outColors: tallyByColor(n.output),
        reasonsIn:  diagnoseStuck(n),
        reasonsOut: diagnoseBlockedOutput(n),
        totalIn: n.totalIn, totalOut: n.totalOut
      });
    }

    for (const tr of travelers) {
      accountByLoc.traveler++;
      const u = tr.unit;
      if (u.color && accountByColor[u.color] !== undefined) accountByColor[u.color]++;
      else accountByColor.untagged++;
      accountByKind[u.kind] = (accountByKind[u.kind] || 0) + 1;
    }

    return {
      batchSize: batchSize,
      dispatched: totalDispatched,
      rejects: Object.assign({}, rejects),
      inFlight: inFlight,
      accountByLoc: accountByLoc,
      accountByColor: accountByColor,
      accountByKind: accountByKind,
      stations: stations,
      running: running
    };
  }

  function renderAuditHtml(m) {
    let h = '';
    h += '<h3>Headline</h3>';
    h += '<table><tr>'
      + '<th>Batch</th><th>Dispatched</th><th>Rejected</th><th>Still in plant</th>'
      + '<th>Accounted</th><th>Status</th></tr><tr>'
      + '<td>' + m.batchSize + '</td>'
      + '<td>' + m.dispatched + '</td>'
      + '<td>' + m.rejects.total + '</td>'
      + '<td>' + m.inFlight + '</td>'
      + '<td>' + (m.dispatched + m.rejects.total + m.inFlight) + '</td>'
      + '<td>' + (m.running ? 'RUNNING' : 'IDLE/FINISHED') + '</td>'
      + '</tr></table>';

    const expected = m.batchSize;
    const accounted = m.dispatched + m.rejects.total + m.inFlight;
    if (expected > 0 && accounted !== expected) {
      const sign = (accounted > expected) ? '+' : '';
      h += '<p class="reason">Note: in-plant + dispatched + rejected = ' + accounted +
           ', batch was ' + expected + ' (delta ' + sign + (accounted - expected) +
           '). Auxiliary intakes (boxes / sand / cores) are issued 1-per-unit; any that were not yet paired into a mould remain in their intake hold or in the sand-press input buffer.</p>';
    }

    h += '<h3>Where the in-plant units sit</h3>';
    h += '<table><tr><th>Location</th><th>Units</th></tr>'
      + '<tr><td>Input buffers</td><td>'    + m.accountByLoc.buffer    + '</td></tr>'
      + '<tr><td>Active slots</td><td>'     + m.accountByLoc.slot      + '</td></tr>'
      + '<tr><td>Output pallets</td><td>'   + m.accountByLoc.output    + '</td></tr>'
      + '<tr><td>On conveyor</td><td>'      + m.accountByLoc.traveler  + '</td></tr>'
      + '<tr><td>Intake holds (un-issued)</td><td>' + m.accountByLoc.intakeOut + '</td></tr>'
      + '</table>';

    h += '<h3>By colour</h3>';
    h += '<table><tr><th>Yellow</th><th>Blue</th><th>Green</th><th>Gray</th><th>Untagged*</th></tr>'
      + '<tr><td>' + m.accountByColor.yellow + '</td>'
      + '<td>' + m.accountByColor.blue + '</td>'
      + '<td>' + m.accountByColor.green + '</td>'
      + '<td>' + m.accountByColor.gray + '</td>'
      + '<td>' + m.accountByColor.untagged + '</td></tr></table>'
      + '<p class="reason"><small>*Untagged = raw sand / scrap / cores / fresh metal that have not yet joined a box-line.</small></p>';

    h += '<h3>By kind</h3>';
    h += '<p>' + fmtKindMap(m.accountByKind) + '</p>';

    h += '<h3>Stuck stations &mdash; reasons</h3>';
    if (!m.stations.length) {
      h += '<p class="ok">All processing stations are empty.</p>';
    } else {
      h += '<table><tr><th>Station</th><th>In</th><th>Slots</th><th>Out</th>'
        + '<th>Total in / out</th><th>Diagnosis</th></tr>';
      for (const s of m.stations) {
        const allReasons = s.reasonsIn.concat(s.reasonsOut);
        const cls = allReasons.length ? ' class="warn"' : (s.outCnt > 0 ? ' class="note"' : '');
        let diag = '';
        if (s.recipe) diag += '<div><small>recipe <code>' + s.recipe.join('+') + '</code></small></div>';
        if (s.inCnt > 0) {
          diag += '<div><small>IN: ' + fmtKindMap(s.inKinds) + '</small></div>';
        }
        if (s.outCnt > 0) {
          diag += '<div><small>OUT: ' + fmtCodeMap(s.outCodes, 6) + '</small></div>';
        }
        if (allReasons.length) {
          diag += '<div class="reason">' + allReasons.map(r => '&bull; ' + r).join('<br/>') + '</div>';
        } else if (s.slotCnt > 0) {
          diag += '<div class="ok">&bull; Processing normally.</div>';
        }
        h += '<tr' + cls + '>'
          + '<td><b>' + s.label + '</b><br><small>' + s.id + ' &middot; ' + s.type +
            (s.type === 'batch' ? ' (batch=' + s.batchSize + ')' :
             (s.boost > 1 ? ' &times;' + s.boost : '')) + '</small></td>'
          + '<td>' + s.inCnt + '</td>'
          + '<td>' + s.slotCnt + '</td>'
          + '<td>' + s.outCnt + '</td>'
          + '<td>' + s.totalIn + ' / ' + s.totalOut + '</td>'
          + '<td>' + diag + '</td>'
          + '</tr>';
      }
      h += '</table>';
    }

    h += '<h3>Reject summary</h3>';
    h += '<table><tr><th>Total</th><th>Yellow</th><th>Blue</th><th>Green</th><th>Gray</th><th>Untagged</th></tr>'
      + '<tr><td>' + m.rejects.total + '</td>'
      + '<td>' + m.rejects.yellow + '</td>'
      + '<td>' + m.rejects.blue + '</td>'
      + '<td>' + m.rejects.green + '</td>'
      + '<td>' + m.rejects.gray + '</td>'
      + '<td>' + m.rejects.untagged + '</td></tr></table>';

    return h;
  }

  function renderAuditText(m) {
    const lines = [];
    lines.push('=== G9C1 RUN AUDIT ===');
    lines.push('Status        : ' + (m.running ? 'RUNNING' : 'IDLE / FINISHED'));
    lines.push('Batch         : ' + m.batchSize);
    lines.push('Dispatched    : ' + m.dispatched);
    lines.push('Rejected      : ' + m.rejects.total);
    lines.push('Still in plant: ' + m.inFlight);
    lines.push('Accounted     : ' + (m.dispatched + m.rejects.total + m.inFlight));
    lines.push('');
    lines.push('-- Where in-plant units sit --');
    lines.push('  input buffers:   ' + m.accountByLoc.buffer);
    lines.push('  active slots:    ' + m.accountByLoc.slot);
    lines.push('  output pallets:  ' + m.accountByLoc.output);
    lines.push('  on conveyor:     ' + m.accountByLoc.traveler);
    lines.push('  intake holds:    ' + m.accountByLoc.intakeOut);
    lines.push('');
    lines.push('-- By colour --');
    lines.push('  yellow=' + m.accountByColor.yellow + '  blue=' + m.accountByColor.blue +
               '  green=' + m.accountByColor.green + '  gray=' + m.accountByColor.gray +
               '  untagged=' + m.accountByColor.untagged);
    lines.push('');
    lines.push('-- By kind --');
    for (const k of Object.keys(m.accountByKind).sort()) {
      lines.push('  ' + k.padEnd(10) + ' x ' + m.accountByKind[k]);
    }
    lines.push('');
    lines.push('-- Stuck / busy stations --');
    if (!m.stations.length) {
      lines.push('  (all empty)');
    } else {
      for (const s of m.stations) {
        lines.push('* ' + s.id + '  ' + s.label + '  [' + s.type + (s.type === 'batch'
            ? ' batch=' + s.batchSize : (s.boost > 1 ? ' x' + s.boost : '')) + ']');
        lines.push('    in=' + s.inCnt + '  slots=' + s.slotCnt + '  out=' + s.outCnt +
                   '   totalIn=' + s.totalIn + ' totalOut=' + s.totalOut);
        if (s.recipe)        lines.push('    recipe: [' + s.recipe.join(' + ') + ']');
        if (s.inCnt > 0) {
          const ks = Object.keys(s.inKinds).sort().map(k => k + 'x' + s.inKinds[k]).join(', ');
          lines.push('    in-buffer kinds: ' + ks);
        }
        if (s.outCnt > 0) {
          const ks = Object.entries(s.outCodes).sort((a, b) => b[1] - a[1])
            .slice(0, 6).map(([k, v]) => k + 'x' + v).join(', ');
          lines.push('    out-buffer codes: ' + ks);
        }
        for (const r of s.reasonsIn.concat(s.reasonsOut)) {
          lines.push('    reason: ' + r.replace(/<[^>]+>/g, '').replace(/&[a-z]+;/g, ''));
        }
      }
    }
    lines.push('');
    lines.push('-- Rejects --');
    lines.push('  total=' + m.rejects.total + '  Y=' + m.rejects.yellow +
               '  B=' + m.rejects.blue + '  G=' + m.rejects.green +
               '  X=' + m.rejects.gray + '  untagged=' + m.rejects.untagged);
    return lines.join('\n');
  }

  function openAudit() {
    const m = buildAuditModel();
    $('#audit-body').html(renderAuditHtml(m));
    lastAuditText = renderAuditText(m);
    $('#audit-modal').removeClass('hidden');
  }
  function closeAudit() { $('#audit-modal').addClass('hidden'); }
  function copyAuditText() {
    if (!lastAuditText) return;
    navigator.clipboard && navigator.clipboard.writeText(lastAuditText);
  }
  function downloadAuditText() {
    if (!lastAuditText) return;
    const blob = new Blob([lastAuditText], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    const ts = new Date().toISOString().replace(/[:.]/g, '-');
    a.href = url; a.download = 'g9c1-audit-' + ts + '.txt';
    document.body.appendChild(a); a.click();
    setTimeout(() => { document.body.removeChild(a); URL.revokeObjectURL(url); }, 100);
  }

  // ============================================================
  //  Save / Load configuration (backend persistence)
  // ============================================================
  function snapshotConfig() {
    const cfg = { nodes: {}, edgeWeights: {}, ui: {} };
    for (const id in nodes) {
      const n = nodes[id];
      if (n.type === 'intake' || n.type === 'dispatch') continue;
      cfg.nodes[id] = { processTime: n.processTime, boost: n.boost };
    }
    for (const id in edgeWeights) {
      cfg.edgeWeights[id] = Object.assign({}, edgeWeights[id]);
    }
    cfg.ui.batch = parseInt($('#ctl-batch').val(), 10) || 100;
    cfg.ui.speed = parseFloat($('#ctl-speed').val()) || 1.0;
    cfg.selectedDevices = Array.from(selectedDevices);
    return cfg;
  }

  function applyConfig(cfg) {
    if (!cfg || typeof cfg !== 'object') return;
    if (cfg.nodes) {
      for (const id in cfg.nodes) {
        if (!nodes[id]) continue;
        const c = cfg.nodes[id];
        if (typeof c.processTime === 'number') nodes[id].processTime = c.processTime;
        if (typeof c.boost       === 'number') nodes[id].boost       = Math.max(1, Math.min(MAX_BOOST, c.boost));
        // Legacy compat: old configs saved as 'replicas'
        if (typeof c.replicas    === 'number' && !c.boost) nodes[id].boost = Math.max(1, Math.min(MAX_BOOST, c.replicas));
      }
    }
    if (cfg.edgeWeights) {
      for (const id in cfg.edgeWeights) {
        if (!edgeWeights[id]) continue;
        for (const t in cfg.edgeWeights[id]) {
          if (t in edgeWeights[id]) edgeWeights[id][t] = cfg.edgeWeights[id][t];
        }
      }
    }
    if (cfg.ui) {
      if (typeof cfg.ui.batch === 'number') $('#ctl-batch').val(cfg.ui.batch);
      if (typeof cfg.ui.speed === 'number') {
        speed = cfg.ui.speed;
        $('#ctl-speed').val(speed);
        $('#stat-speed').text(speed.toFixed(1));
      }
    }
    if (Array.isArray(cfg.selectedDevices)) {
      selectedDevices.clear();
      for (const id of cfg.selectedDevices) if (nodes[id]) selectedDevices.add(id);
      buildGaugePicker();
      rebuildGaugeTiles();
    }
    buildControlPanel();
  }

  function setSaveStatus(msg, isErr) {
    const $s = $('#ctl-save-status');
    $s.text(msg).css('color', isErr ? '#cf4747' : '#5aab63');
    if (msg) setTimeout(() => { $s.text(''); }, 4000);
  }

  function saveConfig() {
    const cfg = snapshotConfig();
    fetch('/api/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cfg, null, 2)
    }).then(r => r.ok ? r.json() : Promise.reject(r.statusText))
      .then(j => setSaveStatus('Saved to ' + (j.path || 'data/config.json'), false))
      .catch(e => setSaveStatus('Save failed: ' + e, true));
  }

  function loadConfig(showStatus) {
    fetch('/api/config').then(r => r.ok ? r.json() : Promise.reject(r.statusText))
      .then(cfg => {
        if (cfg && Object.keys(cfg).length) {
          applyConfig(cfg);
          if (showStatus) setSaveStatus('Loaded saved config', false);
        } else if (showStatus) {
          setSaveStatus('No saved config yet', true);
        }
      })
      .catch(e => { if (showStatus) setSaveStatus('Load failed: ' + e, true); });
  }
})();
