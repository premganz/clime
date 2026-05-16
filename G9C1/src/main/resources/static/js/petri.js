// ============================================================
//  Petri-net (Place/Transition) foundry model  — v3
//  Deterministic colour-based routing (matching factory schematic).
//  Backpressure master-switch (off by default).
//  Traveling units with lineage-code pills on edges.
//  Rich RHS panel with boost/proc-time knobs + buffer gauges.
//  Click-on-machine popup retained for quick tuning.
// ============================================================
(function () {
  'use strict';

  var MAX_BOOST = 6, INIT_BOOST = 3, OPTIMAL_CAP = 40, MAX_PROCESS_TIME = 60;
  var COLORS = ['yellow', 'blue', 'green', 'gray'];
  var COLOR_LET = { yellow: 'Y', blue: 'B', green: 'G', gray: 'X' };
  var COLOR_HEX = { yellow: '#f5d76e', blue: '#4a90d9', green: '#5aab63', gray: '#888' };
  // ---- Routing configuration (editable via Routing tab) ----
  // Skippable finishing machines and their bypass arcs.
  // true = colour DOES this step; false = colour skips (bypass arc).
  // Note: if SB is skipped, token never reaches BIN_BLASTED so HT is
  //       implicitly skipped too (structural dependency).
  var SKIPPABLE = ['T_SB', 'T_HT', 'T_MC', 'T_PT'];
  var SKIP_LABELS = { T_SB: 'Shot Blast', T_HT: 'Heat Treat', T_MC: 'Machining', T_PT: 'Paint' };
  var SKIP_CODES  = { T_SB: 'T', T_HT: 'H', T_MC: 'M', T_PT: 'F' };
  var routeCfg = {
    T_SB: { yellow: true,  blue: true,  green: true,  gray: false },
    T_HT: { yellow: true,  blue: true,  green: true,  gray: false },
    T_MC: { yellow: true,  blue: false, green: true,  gray: false },
    T_PT: { yellow: true,  blue: false, green: true,  gray: false }
  };
  // Bypass arc metadata: from-bin → to-bin (curved around machine)
  var BYPASS_ARCS = [
    { machine: 'T_SB', from: 'BIN_FETTLED',  to: 'BIN_HEATED',   curveOff: 28 },
    { machine: 'T_HT', from: 'BIN_BLASTED',  to: 'BIN_HEATED',   curveOff: 28 },
    { machine: 'T_MC', from: 'BIN_HEATED',    to: 'BIN_QC',       curveOff: 28 },
    { machine: 'T_PT', from: 'BIN_MACHINED',  to: 'BIN_QC',       curveOff: 28 }
  ];

  // Compute expected codes from routing config + fixed pipeline
  function computeExpectCode() {
    var codes = {};
    for (var ci = 0; ci < COLORS.length; ci++) {
      var c = COLORS[ci];
      var parts = ['B', COLOR_LET[c], 'S'];
      if (c !== 'green') parts.push('C');   // green skips core
      parts.push('P', 'Cl', 'Sh', 'Ft');
      if (routeCfg.T_SB[c]) parts.push('T');   // Shot Blast
      if (routeCfg.T_SB[c] && routeCfg.T_HT[c]) parts.push('H'); // Heat Treat (only if SB routed to BLASTED)
      if (routeCfg.T_MC[c]) parts.push('M');   // Machining
      if (routeCfg.T_PT[c]) parts.push('F');   // Paint
      codes[c] = parts.join('-');
    }
    return codes;
  }
  var EXPECT_CODE = computeExpectCode();

  var backpressureEnabled = false;  // master switch

  // ============================================================
  //  Places
  // ============================================================
  var places = {}, placeList = [];
  function P(id, label, optimalCap, opts) {
    opts = opts || {};
    var p = {
      id: id, label: label || id, tokens: [],
      optimalCap: optimalCap || OPTIMAL_CAP,
      col: opts.col || 0, row: opts.row || 0,
      kind: opts.kind || 'buffer', x: 0, y: 0
    };
    places[id] = p; placeList.push(p);
  }
  function gradient(pid) {
    var p = places[pid];
    if (!p || p.kind === 'source' || p.kind === 'sink') return -1;
    return (p.tokens.length - p.optimalCap) / Math.max(1, p.optimalCap);
  }

  // ============================================================
  //  Transitions
  // ============================================================
  var transitions = {}, transList = [];
  function Tr(id, label, processTime, inputs, outputs, opts) {
    opts = opts || {};
    var t = {
      id: id, label: label || id, processTime: processTime,
      inputs: inputs, outputs: outputs,
      boost: opts.boost !== undefined ? opts.boost : INIT_BOOST,
      slots: [], totalIn: 0, totalOut: 0,
      appendCode: opts.appendCode || '',
      kindOut: opts.kindOut || null,
      transmute: opts.transmute || null,
      batchSize: opts.batchSize || 1,
      isBatch: !!opts.isBatch,
      validateFn: opts.validateFn || null,
      routeFn: opts.routeFn || null,
      col: opts.col || 0, row: opts.row || 0,
      shape: opts.shape || 'machine', x: 0, y: 0
    };
    transitions[id] = t; transList.push(t);
  }

  var unitSeq = 0;
  function newUnit(kind) { return { id: ++unitSeq, kind: kind, color: null, code: '', born: 0 }; }

  // ============================================================
  //  TOPOLOGY — deterministic colour-based routing
  // ============================================================
  P('BIN_SCRAP',  'Scrap',       200, { col: 0,   row: 0,   kind: 'source' });
  P('BIN_CORE',   'Core Stock',  200, { col: 0,   row: 1.5, kind: 'source' });
  P('BIN_BOX',    'Box Store',   200, { col: 0,   row: 3,   kind: 'source' });
  P('BIN_SAND',   'Sand Store',  200, { col: 0,   row: 4,   kind: 'source' });

  P('BIN_METAL',   'Metal Pool',  80, { col: 3,    row: 0 });
  P('BIN_CORES',   'Cores',       60, { col: 3,    row: 1.5 });
  P('BIN_MOULDS',  'Moulds',      60, { col: 3,    row: 3 });
  P('BIN_BARE',    'Bare Moulds', 60, { col: 3,    row: 4 });
  P('BIN_SET',     'Set Moulds',  60, { col: 5.5,  row: 2 });
  P('BIN_POURED',  'Poured',      80, { col: 8,    row: 2 });
  P('BIN_COOLED',  'Cooled',      60, { col: 10,   row: 2 });
  P('BIN_SHAKEN',  'Shaken',      60, { col: 12,   row: 2 });
  P('BIN_FETTLED', 'Fettled',     60, { col: 12,   row: 4 });
  P('BIN_BLASTED', 'Blasted',     60, { col: 10,   row: 4 });
  P('BIN_HEATED',  'Heated',      60, { col: 8,    row: 4 });
  P('BIN_MACHINED','Machined',    60, { col: 6,    row: 4 });
  P('BIN_PAINTED', 'Painted',     60, { col: 4,    row: 4 });
  P('BIN_QC',      'QC Queue',    60, { col: 2,    row: 4 });
  P('BIN_DISPATCH','Dispatch',  9999, { col: 0,  row: 5.5, kind: 'sink' });
  P('BIN_REJECT',  'Rejects',  9999, { col: 2,  row: 5.5, kind: 'sink' });

  // Furnaces
  Tr('T_F1', 'Furnace 1', 40,
    [{ place: 'BIN_SCRAP', kind: 'scrap' }], [{ place: 'BIN_METAL' }],
    { col: 1.5, row: 0, shape: 'furnace', isBatch: true, batchSize: 40,
      transmute: { kind: 'metal' }, boost: INIT_BOOST });
  Tr('T_F2', 'Furnace 2', 40,
    [{ place: 'BIN_SCRAP', kind: 'scrap' }], [{ place: 'BIN_METAL' }],
    { col: 1.5, row: 0.7, shape: 'furnace', isBatch: true, batchSize: 40,
      transmute: { kind: 'metal' }, boost: INIT_BOOST });

  // Core Maker
  Tr('T_CM', 'Core Maker', 1.0,
    [{ place: 'BIN_CORE', kind: 'coreraw' }], [{ place: 'BIN_CORES' }],
    { col: 1.5, row: 1.5, kindOut: 'core', boost: INIT_BOOST });

  // Sand Press — green→BARE, others→MOULDS
  Tr('T_SP', 'Sand Press', 1.0,
    [{ place: 'BIN_BOX', kind: 'box' }, { place: 'BIN_SAND', kind: 'sand' }],
    [{ places: ['BIN_MOULDS', 'BIN_BARE'], mode: 'route' }],
    { col: 1.5, row: 3.5, boost: INIT_BOOST, appendCode: 'S', kindOut: 'mould',
      routeFn: function (u) { return u.color === 'green' ? 'BIN_BARE' : 'BIN_MOULDS'; } });

  // Core Setting
  Tr('T_CS', 'Core Set', 3.0,
    [{ place: 'BIN_MOULDS', kind: 'mould' }, { place: 'BIN_CORES', kind: 'core' }],
    [{ place: 'BIN_SET' }],
    { col: 4.5, row: 2, boost: INIT_BOOST, appendCode: 'C', kindOut: 'mould' });

  // Bare→Set (green skip)
  Tr('T_BM', 'Bare→Set', 0.5,
    [{ place: 'BIN_BARE', kind: 'mould' }], [{ place: 'BIN_SET' }],
    { col: 4.5, row: 3.5, boost: INIT_BOOST });

  // Pouring lines
  Tr('T_P1', 'Pour A1', 1.5,
    [{ place: 'BIN_SET', kind: 'mould' }, { place: 'BIN_METAL', kind: 'metal' }],
    [{ place: 'BIN_POURED' }],
    { col: 6.5, row: 1, boost: INIT_BOOST, appendCode: 'P', kindOut: 'cast' });
  Tr('T_P2', 'Pour A2', 1.5,
    [{ place: 'BIN_SET', kind: 'mould' }, { place: 'BIN_METAL', kind: 'metal' }],
    [{ place: 'BIN_POURED' }],
    { col: 6.5, row: 2, boost: INIT_BOOST, appendCode: 'P', kindOut: 'cast' });
  Tr('T_P3', 'Pour KW', 1.5,
    [{ place: 'BIN_SET', kind: 'mould' }, { place: 'BIN_METAL', kind: 'metal' }],
    [{ place: 'BIN_POURED' }],
    { col: 6.5, row: 3, boost: INIT_BOOST, appendCode: 'P', kindOut: 'cast' });

  // Cooler
  Tr('T_CL', 'Cooler', 10,
    [{ place: 'BIN_POURED', kind: 'cast' }], [{ place: 'BIN_COOLED' }],
    { col: 9, row: 2, isBatch: true, batchSize: 40, appendCode: 'Cl', boost: INIT_BOOST });

  // Shakeout
  Tr('T_SK', 'Shakeout', 1.5,
    [{ place: 'BIN_COOLED', kind: 'cast' }], [{ place: 'BIN_SHAKEN' }],
    { col: 11, row: 2, boost: INIT_BOOST, appendCode: 'Sh' });

  // Fettling
  Tr('T_FT', 'Fettling', 1.5,
    [{ place: 'BIN_SHAKEN', kind: 'cast' }], [{ place: 'BIN_FETTLED' }],
    { col: 12, row: 3, boost: INIT_BOOST, appendCode: 'Ft' });

  // Shot Blast — dispatches via routeCfg: process → BIN_BLASTED, skip → BIN_HEATED
  Tr('T_SB', 'Shot Blast', 3.0,
    [{ place: 'BIN_FETTLED', kind: 'cast' }],
    [{ place: 'BIN_BLASTED' }],
    { col: 11, row: 4, boost: INIT_BOOST,
      routeFn: function (u) {
        if (!routeCfg.T_SB[u.color]) return 'BIN_HEATED';
        if (u.code && u.code.charAt(0) === 'B') u.code += '-T';
        return 'BIN_BLASTED';
      } });

  // Heat Treat — dispatches via routeCfg: process → BIN_HEATED (with H), skip → BIN_HEATED (no H)
  Tr('T_HT', 'Heat Treat', 5.0,
    [{ place: 'BIN_BLASTED', kind: 'cast' }], [{ place: 'BIN_HEATED' }],
    { col: 9, row: 4, boost: INIT_BOOST,
      routeFn: function (u) {
        if (routeCfg.T_HT[u.color] && u.code && u.code.charAt(0) === 'B') u.code += '-H';
        return 'BIN_HEATED';
      } });

  // Machining — dispatches via routeCfg: process → BIN_MACHINED, skip → BIN_QC
  Tr('T_MC', 'Machining', 4.0,
    [{ place: 'BIN_HEATED', kind: 'cast' }],
    [{ place: 'BIN_MACHINED' }],
    { col: 7, row: 4, boost: INIT_BOOST,
      routeFn: function (u) {
        if (!routeCfg.T_MC[u.color]) return 'BIN_QC';
        if (u.code && u.code.charAt(0) === 'B') u.code += '-M';
        return 'BIN_MACHINED';
      } });

  // Paint — dispatches via routeCfg: process → BIN_PAINTED, skip → BIN_QC
  Tr('T_PT', 'Paint', 3.0,
    [{ place: 'BIN_MACHINED', kind: 'cast' }],
    [{ place: 'BIN_PAINTED' }],
    { col: 5, row: 4, boost: INIT_BOOST,
      routeFn: function (u) {
        if (!routeCfg.T_PT[u.color]) return 'BIN_QC';
        if (u.code && u.code.charAt(0) === 'B') u.code += '-F';
        return 'BIN_PAINTED';
      } });

  // Painted→QC
  Tr('T_PQ', 'Painted→QC', 0.5,
    [{ place: 'BIN_PAINTED', kind: 'cast' }], [{ place: 'BIN_QC' }],
    { col: 3, row: 4.5, boost: INIT_BOOST });

  // QC
  Tr('T_QC', 'QC', 1.5,
    [{ place: 'BIN_QC', kind: 'cast' }],
    [{ places: ['BIN_DISPATCH', 'BIN_REJECT'], mode: 'qc' }],
    { col: 1, row: 4.5, boost: INIT_BOOST,
      validateFn: function (u) { var e = EXPECT_CODE[u.color]; return e && u.code === e; } });

  // ============================================================
  //  Travelers — units traveling between nodes (like factory.js)
  // ============================================================
  var travelers = [];  // { fromNode, toNode, unit, t, duration }

  function spawnTraveler(fromObj, toObj, unit, duration) {
    travelers.push({ from: fromObj, to: toObj, unit: unit, t: 0, duration: Math.max(0.05, duration) });
  }

  function tickTravelers(dt) {
    for (var i = travelers.length - 1; i >= 0; i--) {
      var tr = travelers[i];
      tr.t += dt / tr.duration;
      if (tr.t >= 1) {
        // Deliver to destination
        if (tr.to && tr.to.tokens) {
          tr.to.tokens.push(tr.unit);
        }
        travelers.splice(i, 1);
      }
    }
  }

  // ============================================================
  //  Simulation state
  // ============================================================
  var running = false, paused = false, speed = 1.0, batchSize = 0;
  var lastT = 0, totalDispatched = 0;
  var rejects = { total: 0, yellow: 0, blue: 0, green: 0, gray: 0 };
  var runElapsedSec = 0, lastDispatchSec = 0;
  var runFinalSec = null, runStopReason = '';
  var STOP_PCT = 0.80, STOP_IDLE = 30;

  function resetSim() {
    var id;
    for (id in places) places[id].tokens = [];
    for (id in transitions) { transitions[id].slots = []; transitions[id].totalIn = 0; transitions[id].totalOut = 0; }
    totalDispatched = 0;
    rejects = { total: 0, yellow: 0, blue: 0, green: 0, gray: 0 };
    runElapsedSec = 0; lastDispatchSec = 0;
    runFinalSec = null; runStopReason = '';
    unitSeq = 0; travelers.length = 0;
  }

  function startBatch(n) {
    resetSim(); batchSize = n;
    for (var i = 0; i < n; i++) places['BIN_SCRAP'].tokens.push(newUnit('scrap'));
    for (var i2 = 0; i2 < n; i2++) places['BIN_SAND'].tokens.push(newUnit('sand'));
    for (var i3 = 0; i3 < n; i3++) {
      var u = newUnit('box');
      u.color = COLORS[i3 % COLORS.length];
      u.code = 'B-' + COLOR_LET[u.color];
      places['BIN_BOX'].tokens.push(u);
    }
    for (var i4 = 0; i4 < Math.ceil(n * 0.75); i4++) places['BIN_CORE'].tokens.push(newUnit('coreraw'));
    running = true;
  }

  // ============================================================
  //  Tick
  // ============================================================
  function tick(dt) {
    if (!running || paused) return;
    runElapsedSec += dt;
    for (var ti = 0; ti < transList.length; ti++) {
      var tr = transList[ti];
      for (var si = tr.slots.length - 1; si >= 0; si--) {
        var s = tr.slots[si];
        s.remaining -= dt;
        if (s.remaining <= 0) { emitSlot(tr, s); tr.slots.splice(si, 1); }
      }
      if (tr.isBatch) fireBatch(tr); else fireNormal(tr);
    }
    if (running && batchSize > 0) {
      if ((totalDispatched + rejects.total) >= batchSize) {
        running = false;
        if (!runFinalSec) { runFinalSec = runElapsedSec; runStopReason = 'BATCH_DONE'; }
      }
      if (totalDispatched >= batchSize * STOP_PCT && runElapsedSec - lastDispatchSec >= STOP_IDLE) {
        running = false;
        if (!runFinalSec) { runFinalSec = runElapsedSec - STOP_IDLE; runStopReason = 'IDLE_STOP'; }
      }
    }
  }

  function countTokens(pid, kind) {
    var p = places[pid]; if (!p) return 0;
    if (!kind) return p.tokens.length;
    var c = 0; for (var i = 0; i < p.tokens.length; i++) if (p.tokens[i].kind === kind) c++;
    return c;
  }
  function pullToken(pid, kind) {
    var p = places[pid]; if (!p) return null;
    for (var i = 0; i < p.tokens.length; i++) {
      if (!kind || p.tokens[i].kind === kind) return p.tokens.splice(i, 1)[0];
    }
    return null;
  }
  function canFire(tr) {
    for (var i = 0; i < tr.inputs.length; i++)
      if (countTokens(tr.inputs[i].place, tr.inputs[i].kind) < 1) return false;
    return true;
  }
  function canFireBatch(tr) {
    return countTokens(tr.inputs[0].place, tr.inputs[0].kind) >= tr.batchSize;
  }

  // Output routing — deterministic via routeFn, QC via validateFn
  function chooseOutput(tr, outSpec, unit) {
    if (outSpec.mode === 'qc') return (tr.validateFn && tr.validateFn(unit)) ? outSpec.places[0] : outSpec.places[1];
    if (tr.routeFn) return tr.routeFn(unit);
    return outSpec.place || (outSpec.places ? outSpec.places[0] : null);
  }

  // Throughput model: effective interval = processTime / boost.
  // Backpressure (when enabled): stretches the interval proportional to output-bin overfill.
  //   bp factor = 1 + clamp(gradient, 0, 2) — so a bin at 2× optimal doubles the tick time.
  //   Boost counteracts: divide bp factor by (boost / INIT_BOOST).
  function backpressureFactor(tr) {
    if (!backpressureEnabled) return 1;
    var outSpec0 = tr.outputs[0];
    var outPid = outSpec0.place || (outSpec0.places ? outSpec0.places[0] : null);
    if (!outPid) return 1;
    var g = Math.max(0, gradient(outPid));          // 0 when under-filled
    var boostOverride = Math.max(1, tr.boost / INIT_BOOST);
    return 1 + Math.min(g, 2) / boostOverride;      // 1× – 3× slowdown
  }
  function effectiveInterval(tr) {
    return Math.max(0.05, (tr.processTime / tr.boost) * backpressureFactor(tr));
  }

  function fireNormal(tr) {
    while (tr.slots.length < tr.boost && canFire(tr)) {
      var pulled = [], ok = true;
      for (var ii = 0; ii < tr.inputs.length; ii++) {
        var tok = pullToken(tr.inputs[ii].place, tr.inputs[ii].kind);
        if (!tok) { for (var j = 0; j < pulled.length; j++) places[pulled[j].from].tokens.push(pulled[j].tok); ok = false; break; }
        pulled.push({ from: tr.inputs[ii].place, tok: tok });
      }
      if (!ok) break;

      var primary = pulled[0].tok;
      if (pulled.length >= 2) {
        for (var pi = 0; pi < pulled.length; pi++) {
          if (pulled[pi].tok.code && pulled[pi].tok.code.charAt(0) === 'B') { primary = pulled[pi].tok; break; }
        }
      }

      var out = primary;
      if (tr.kindOut) out.kind = tr.kindOut;
      if (tr.appendCode && out.code && out.code.charAt(0) === 'B') out.code += '-' + tr.appendCode;
      var dest = chooseOutput(tr, tr.outputs[0], out);

      var jt = effectiveInterval(tr);
      tr.slots.push({ remaining: jt, jobTime: jt, units: [out], dest: dest });
      tr.totalIn++;
    }
  }

  function fireBatch(tr) {
    while (tr.slots.length < tr.boost && canFireBatch(tr)) {
      var inp = tr.inputs[0], charge = [];
      for (var i = 0; i < tr.batchSize; i++) { var tok = pullToken(inp.place, inp.kind); if (tok) charge.push(tok); }
      var outs;
      if (tr.transmute) {
        outs = [];
        for (var j = 0; j < tr.batchSize; j++) outs.push(newUnit(tr.transmute.kind));
      } else {
        outs = charge;
        if (tr.appendCode) for (var k = 0; k < outs.length; k++)
          if (outs[k].code && outs[k].code.charAt(0) === 'B') outs[k].code += '-' + tr.appendCode;
      }
      var jt = effectiveInterval(tr);
      tr.slots.push({ remaining: jt, jobTime: jt, units: outs, dest: null });
      tr.totalIn++;
    }
  }

  function emitSlot(tr, slot) {
    var outSpec = tr.outputs[0];
    for (var i = 0; i < slot.units.length; i++) {
      var unit = slot.units[i];
      var dest = slot.dest || chooseOutput(tr, outSpec, unit);
      if (outSpec.mode === 'qc') {
        if (dest === 'BIN_DISPATCH') { totalDispatched++; lastDispatchSec = runElapsedSec; }
        else { rejects.total++; if (unit.color && rejects[unit.color] !== undefined) rejects[unit.color]++; }
      }
      // Spawn traveler instead of instant deposit
      var destP = places[dest];
      if (destP) {
        spawnTraveler(tr, destP, unit, 0.3 + Math.random() * 0.15);
      } else {
        places[dest].tokens.push(unit);
      }
      tr.totalOut++;
    }
  }

  // ============================================================
  //  Canvas rendering
  // ============================================================
  var canvas, ctx, W, H, PAD = 50;

  function resize() {
    var sec = document.getElementById('petri-stage'); if (!sec) return;
    canvas = document.getElementById('petri-canvas'); if (!canvas) return;
    var dpr = window.devicePixelRatio || 1;
    W = sec.clientWidth - 10; H = sec.clientHeight - 10;
    canvas.width = W * dpr; canvas.height = H * dpr;
    canvas.style.width = W + 'px'; canvas.style.height = H + 'px';
    ctx = canvas.getContext('2d'); ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    layoutNodes();
  }

  var maxCol = 0, maxRow = 0;
  function layoutNodes() {
    maxCol = 0; maxRow = 0;
    var id;
    for (id in places) { maxCol = Math.max(maxCol, places[id].col); maxRow = Math.max(maxRow, places[id].row); }
    for (id in transitions) { maxCol = Math.max(maxCol, transitions[id].col); maxRow = Math.max(maxRow, transitions[id].row); }
    var cw = (W - PAD * 2) / Math.max(1, maxCol), ch = (H - PAD * 2) / Math.max(1, maxRow);
    for (id in places) { places[id].x = PAD + places[id].col * cw; places[id].y = PAD + places[id].row * ch; }
    for (id in transitions) { transitions[id].x = PAD + transitions[id].col * cw; transitions[id].y = PAD + transitions[id].row * ch; }
  }

  var BIN_R = 20, TRANS_W = 54, TRANS_H = 22;

  // ---- Unit drawing (pill with lineage code, matching factory.js) ----
  function unitGlyph(u) {
    if (u.code) return u.code;
    switch (u.kind) {
      case 'sand': return 's'; case 'scrap': return 'x'; case 'coreraw': return 'c';
      case 'core': return 'C'; case 'metal': return 'M'; default: return '?';
    }
  }
  function unitFill(u) {
    if (u.color && COLOR_HEX[u.color]) return COLOR_HEX[u.color];
    switch (u.kind) {
      case 'sand': return '#d4b06a'; case 'scrap': return '#666'; case 'coreraw': return '#9c8a70';
      case 'core': return '#e6d6a8'; case 'metal': return '#ff7a20'; default: return '#888';
    }
  }
  function unitTextColor(u) {
    if (u.kind === 'metal' || u.kind === 'scrap' || u.color === 'blue' || u.color === 'gray') return '#fff';
    return '#1a1a1a';
  }
  function drawUnit(x, y, unit, scale) {
    var text = unitGlyph(unit);
    var fontSize = Math.max(6, scale * 0.9);
    ctx.save();
    ctx.font = 'bold ' + fontSize + 'px Consolas, monospace';
    ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
    var padX = scale * 0.3, padY = scale * 0.2;
    var tw = ctx.measureText(text).width + padX * 2;
    var th = fontSize + padY * 2;
    ctx.fillStyle = unitFill(unit);
    ctx.strokeStyle = '#1a1a1a'; ctx.lineWidth = 0.7;
    var rr = Math.min(th, tw) / 2;
    if (ctx.roundRect) { ctx.beginPath(); ctx.roundRect(x - tw / 2, y - th / 2, tw, th, rr * 0.4); ctx.fill(); ctx.stroke(); }
    else { ctx.fillRect(x - tw / 2, y - th / 2, tw, th); ctx.strokeRect(x - tw / 2, y - th / 2, tw, th); }
    ctx.fillStyle = unitTextColor(unit);
    ctx.fillText(text, x, y + 0.5);
    ctx.restore();
  }

  function drawTravelers() {
    for (var i = 0; i < travelers.length; i++) {
      var tr = travelers[i];
      var frac = Math.max(0, Math.min(1, tr.t));
      var x = tr.from.x + (tr.to.x - tr.from.x) * frac;
      var y = tr.from.y + (tr.to.y - tr.from.y) * frac;
      drawUnit(x, y, tr.unit, 7);
    }
  }

  function drawPlace(p) {
    var x = p.x, y = p.y, cnt = p.tokens.length, g = gradient(p.id);
    ctx.beginPath(); ctx.arc(x, y, BIN_R, 0, Math.PI * 2);
    if (p.kind === 'source') ctx.fillStyle = '#dde0d8';
    else if (p.kind === 'sink') ctx.fillStyle = p.id === 'BIN_REJECT' ? '#3a2020' : '#203a20';
    else if (g > 0.3) ctx.fillStyle = '#e85050';
    else if (g > 0) ctx.fillStyle = '#f0c850';
    else ctx.fillStyle = '#ffffff';
    ctx.fill(); ctx.strokeStyle = '#666'; ctx.lineWidth = 2; ctx.stroke();
    if (p.kind === 'buffer' && p.optimalCap > 0 && cnt > 0) {
      var frac = Math.min(1, cnt / (p.optimalCap * 2));
      ctx.beginPath(); ctx.moveTo(x, y);
      ctx.arc(x, y, BIN_R - 3, -Math.PI / 2, -Math.PI / 2 + frac * Math.PI * 2);
      ctx.closePath(); ctx.fillStyle = g > 0 ? 'rgba(207,71,71,0.25)' : 'rgba(90,171,99,0.25)'; ctx.fill();
    }
    ctx.fillStyle = '#1a1a1a'; ctx.font = 'bold 11px Segoe UI'; ctx.textAlign = 'center';
    ctx.fillText(cnt, x, y + 4);
    ctx.fillStyle = '#bbb'; ctx.font = '8px Segoe UI';
    ctx.fillText(p.label, x, y + BIN_R + 11);
  }

  function drawTransition(t) {
    var x = t.x, y = t.y;
    var hw = TRANS_W / 2, hh = TRANS_H / 2;
    var active = t.slots.length > 0, selected = (selectedTr === t);
    var active = t.slots.length > 0, selected = (selectedTr === t);
    ctx.fillStyle = t.shape === 'furnace' ? '#7a3a22' : (active ? '#3a5a6a' : '#4e5660');
    ctx.fillRect(x - hw, y - hh, TRANS_W, TRANS_H);
    ctx.strokeStyle = selected ? '#f5d76e' : (active ? '#8ab8d0' : '#777');
    ctx.lineWidth = selected ? 2.5 : (active ? 2 : 1);
    ctx.strokeRect(x - hw, y - hh, TRANS_W, TRANS_H);
    if (t.inputs.length === 2) {
      ctx.fillStyle = '#5577aa';
      ctx.beginPath(); ctx.arc(x - hw + 3, y - 4, 2.5, 0, Math.PI * 2); ctx.fill();
      ctx.beginPath(); ctx.arc(x - hw + 3, y + 4, 2.5, 0, Math.PI * 2); ctx.fill();
    }
    if (active) {
      var best = 0;
      for (var si = 0; si < t.slots.length; si++) { var f = t.slots[si].jobTime > 0 ? (1 - t.slots[si].remaining / t.slots[si].jobTime) : 0; if (f > best) best = f; }
      ctx.fillStyle = '#222'; ctx.fillRect(x - hw + 2, y + hh - 4, TRANS_W - 4, 3);
      ctx.fillStyle = '#5aab63'; ctx.fillRect(x - hw + 2, y + hh - 4, (TRANS_W - 4) * best, 3);
    }
    if (t.boost > 1) {
      ctx.fillStyle = '#1a1e22'; ctx.beginPath(); ctx.arc(x + hw - 2, y - hh + 2, 7, 0, Math.PI * 2); ctx.fill();
      ctx.fillStyle = '#f5d76e'; ctx.font = 'bold 7px Segoe UI'; ctx.textAlign = 'center';
      ctx.fillText('x' + t.boost, x + hw - 2, y - hh + 5);
    }
    ctx.fillStyle = '#e6e6e6'; ctx.font = '8px Segoe UI'; ctx.textAlign = 'center';
    ctx.fillText(t.label, x, y + 3);
    ctx.fillStyle = active ? '#8ab8d0' : '#888'; ctx.font = '7px Segoe UI';
    var eff = effectiveInterval(t);
    var iText = t.isBatch ? (eff.toFixed(1) + 's/' + t.batchSize) : (eff.toFixed(2) + 's');
    if (active) iText = t.slots.length + '/' + t.boost + ' | ' + iText;
    ctx.fillText(iText, x, y + hh + 9);
  }

  function drawArrow(x1, y1, x2, y2, color, dashed) {
    var dx = x2 - x1, dy = y2 - y1, len = Math.sqrt(dx * dx + dy * dy);
    if (len < 2) return;
    var ux = dx / len, uy = dy / len;
    ctx.strokeStyle = color || '#666'; ctx.lineWidth = 1.2;
    if (dashed) ctx.setLineDash([3, 3]);
    ctx.beginPath(); ctx.moveTo(x1, y1); ctx.lineTo(x2, y2); ctx.stroke();
    if (dashed) ctx.setLineDash([]);
    ctx.fillStyle = color || '#666'; ctx.beginPath();
    ctx.moveTo(x2, y2);
    ctx.lineTo(x2 - ux * 5 + uy * 2.5, y2 - uy * 5 - ux * 2.5);
    ctx.lineTo(x2 - ux * 5 - uy * 2.5, y2 - uy * 5 + ux * 2.5);
    ctx.closePath(); ctx.fill();
  }
  function edgePts(src, dst, sR, dR) {
    var dx = dst.x - src.x, dy = dst.y - src.y, len = Math.sqrt(dx * dx + dy * dy);
    if (len < 1) return null;
    var ux = dx / len, uy = dy / len;
    return { x1: src.x + ux * sR, y1: src.y + uy * sR, x2: dst.x - ux * dR, y2: dst.y - uy * dR };
  }
  function drawEdges() {
    for (var ti = 0; ti < transList.length; ti++) {
      var tr = transList[ti];
      for (var ii = 0; ii < tr.inputs.length; ii++) {
        var p = places[tr.inputs[ii].place]; if (!p) continue;
        var e = edgePts(p, tr, BIN_R + 2, TRANS_W / 2 + 2);
        if (e) drawArrow(e.x1, e.y1, e.x2, e.y2, '#5577aa', false);
      }
      for (var oi = 0; oi < tr.outputs.length; oi++) {
        var os = tr.outputs[oi], pIds = os.places || [os.place];
        for (var pi = 0; pi < pIds.length; pi++) {
          var dp = places[pIds[pi]]; if (!dp) continue;
          var isR = os.mode === 'route' || os.mode === 'qc';
          var e2 = edgePts(tr, dp, TRANS_W / 2 + 2, BIN_R + 2);
          if (e2) drawArrow(e2.x1, e2.y1, e2.x2, e2.y2, isR ? '#aa7755' : '#55aa77', isR);
        }
      }
    }
  }

  // Draw curved bypass arcs for all skippable machines
  function drawBypassArcs() {
    for (var i = 0; i < BYPASS_ARCS.length; i++) {
      var ba = BYPASS_ARCS[i];
      var from = places[ba.from], to = places[ba.to], mach = transitions[ba.machine];
      if (!from || !to || !mach) continue;
      // Start / end points (offset from bin edge)
      var dx = to.x - from.x, dy = to.y - from.y;
      var len = Math.sqrt(dx * dx + dy * dy); if (len < 2) continue;
      var ux = dx / len, uy = dy / len;
      var x1 = from.x + ux * (BIN_R + 2), y1 = from.y + uy * (BIN_R + 2);
      var x2 = to.x   - ux * (BIN_R + 2), y2 = to.y   - uy * (BIN_R + 2);
      // Control point offset below (positive curveOff) or above
      var cpx = (x1 + x2) / 2 - uy * ba.curveOff;
      var cpy = (y1 + y2) / 2 + ux * ba.curveOff;
      ctx.strokeStyle = '#cc9944'; ctx.lineWidth = 1.2; ctx.setLineDash([4, 3]);
      ctx.beginPath(); ctx.moveTo(x1, y1); ctx.quadraticCurveTo(cpx, cpy, x2, y2); ctx.stroke();
      ctx.setLineDash([]);
      // Arrowhead
      var t = 0.96, tx = (1-t)*(1-t)*x1 + 2*(1-t)*t*cpx + t*t*x2;
      var ty = (1-t)*(1-t)*y1 + 2*(1-t)*t*cpy + t*t*y2;
      var adx = x2 - tx, ady = y2 - ty, al = Math.sqrt(adx*adx+ady*ady);
      if (al > 0) { adx /= al; ady /= al; }
      ctx.fillStyle = '#cc9944'; ctx.beginPath();
      ctx.moveTo(x2, y2);
      ctx.lineTo(x2 - adx*5 + ady*2.5, y2 - ady*5 - adx*2.5);
      ctx.lineTo(x2 - adx*5 - ady*2.5, y2 - ady*5 + adx*2.5);
      ctx.closePath(); ctx.fill();
      // Label
      ctx.fillStyle = '#cc9944'; ctx.font = '7px Segoe UI'; ctx.textAlign = 'center';
      ctx.fillText('bypass', cpx, cpy + 4);
    }
  }

  function drawAll() {
    if (!ctx) return;
    ctx.fillStyle = '#2b2f33'; ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = '#f2f1ef';
    ctx.fillRect(PAD - 12, PAD - 12, W - PAD * 2 + 24, H - PAD * 2 + 24);
    ctx.strokeStyle = '#bbb'; ctx.lineWidth = 2;
    ctx.strokeRect(PAD - 12, PAD - 12, W - PAD * 2 + 24, H - PAD * 2 + 24);
    drawEdges();
    drawBypassArcs();
    for (var i = 0; i < placeList.length; i++) drawPlace(placeList[i]);
    for (var j = 0; j < transList.length; j++) drawTransition(transList[j]);
    drawTravelers();
    drawBoostGauge();
  }

  // ============================================================
  //  Click-on-machine popup
  // ============================================================
  var selectedTr = null;
  function hitTestTransition(mx, my) {
    for (var i = 0; i < transList.length; i++) {
      var t = transList[i];
      if (Math.abs(mx - t.x) < TRANS_W / 2 + 4 && Math.abs(my - t.y) < TRANS_H / 2 + 4) return t;
    }
    return null;
  }
  function showPopup(tr) {
    selectedTr = tr;
    var popup = document.getElementById('p-machine-popup'); if (!popup) return;
    popup.style.display = 'block';
    popup.querySelector('.popup-title').textContent = tr.label + (tr.inputs.length === 2 ? '  [binary]' : '');
    popup.querySelector('.popup-id').textContent = tr.id;
    popup.querySelector('#pp-interval-val').textContent = tr.processTime.toFixed(1) + 's';
    popup.querySelector('#pp-interval').value = tr.processTime;
    popup.querySelector('#pp-boost-val').textContent = tr.boost;
    popup.querySelector('#pp-boost').value = tr.boost;
    popup.querySelector('#pp-eff').textContent = effectiveInterval(tr).toFixed(2) + 's';
    var tp = tr.boost / Math.max(0.01, tr.processTime); if (tr.isBatch) tp *= tr.batchSize;
    popup.querySelector('#pp-tput').textContent = tp.toFixed(1) + '/s';
    popup.querySelector('#pp-in').textContent = tr.totalIn;
    popup.querySelector('#pp-out').textContent = tr.totalOut;
    popup.querySelector('#pp-slots').textContent = tr.slots.length + '/' + tr.boost;
    if (tr.isBatch) { popup.querySelector('#pp-batch-row').style.display = ''; popup.querySelector('#pp-batch-size').textContent = tr.batchSize; }
    else popup.querySelector('#pp-batch-row').style.display = 'none';
    var inpStr = ''; for (var i = 0; i < tr.inputs.length; i++) inpStr += (i > 0 ? ' + ' : '') + tr.inputs[i].kind;
    popup.querySelector('#pp-inputs').textContent = inpStr;
    var os = tr.outputs[0]; popup.querySelector('#pp-outputs').textContent = (os.place || (os.places ? os.places.join(' | ') : '?')) + (os.mode ? ' (' + os.mode + ')' : '');
    var cr = canvas.getBoundingClientRect(), px = cr.left + (tr.x / W) * cr.width + 30, py = cr.top + (tr.y / H) * cr.height - 40;
    if (px + 230 > window.innerWidth - 10) px -= 290;
    if (py < 10) py = 10;
    popup.style.left = px + 'px'; popup.style.top = py + 'px';
    // Sync panel slider if visible
    syncPanelSlider(tr);
  }
  function hidePopup() { selectedTr = null; var p = document.getElementById('p-machine-popup'); if (p) p.style.display = 'none'; }
  function refreshPopupStats() {
    if (!selectedTr) return;
    var tr = selectedTr, p = document.getElementById('p-machine-popup');
    if (!p || p.style.display === 'none') return;
    p.querySelector('#pp-eff').textContent = effectiveInterval(tr).toFixed(2) + 's';
    var tp = tr.boost / Math.max(0.01, tr.processTime); if (tr.isBatch) tp *= tr.batchSize;
    p.querySelector('#pp-tput').textContent = tp.toFixed(1) + '/s';
    p.querySelector('#pp-in').textContent = tr.totalIn;
    p.querySelector('#pp-out').textContent = tr.totalOut;
    p.querySelector('#pp-slots').textContent = tr.slots.length + '/' + tr.boost;
  }

  // ============================================================
  //  Boost pool gauge (on canvas)
  // ============================================================
  function boostPoolTotal() { return transList.length * INIT_BOOST; }
  function boostUsed() { var s = 0; for (var i = 0; i < transList.length; i++) s += transList[i].boost; return s; }
  function boostPool() { return boostPoolTotal() - boostUsed(); }

  var boostNeedleAngle = null;
  function drawBoostGauge() {
    var pool = boostPool(), maxRange = boostPoolTotal();
    var boxX = PAD, boxY = 4, boxW = 80, boxH = 74;
    ctx.fillStyle = '#fff'; ctx.fillRect(boxX, boxY, boxW, boxH);
    ctx.strokeStyle = '#999'; ctx.lineWidth = 1; ctx.strokeRect(boxX, boxY, boxW, boxH);
    var cx = boxX + boxW / 2, cy = boxY + boxH * 0.84;
    var r = Math.min(boxW * 0.42, boxH * 0.72);
    var startA = Math.PI * 1.15, endA = Math.PI * -0.15, sweep = endA - startA + Math.PI * 2;
    ctx.lineWidth = 6; ctx.lineCap = 'butt';
    ctx.strokeStyle = '#cf4747'; ctx.beginPath(); ctx.arc(cx, cy, r, startA, startA + sweep * 0.33); ctx.stroke();
    ctx.strokeStyle = '#e8c060'; ctx.beginPath(); ctx.arc(cx, cy, r, startA + sweep * 0.33, startA + sweep * 0.67); ctx.stroke();
    ctx.strokeStyle = '#5aab63'; ctx.beginPath(); ctx.arc(cx, cy, r, startA + sweep * 0.67, startA + sweep); ctx.stroke();
    ctx.strokeStyle = '#666'; ctx.lineWidth = 1; ctx.fillStyle = '#333'; ctx.font = '7px Segoe UI'; ctx.textAlign = 'center';
    for (var i = 0; i <= 10; i++) {
      var f = i / 10, a = startA + sweep * f, maj = (i % 5 === 0);
      ctx.beginPath();
      ctx.moveTo(cx + Math.cos(a) * (r - (maj ? 9 : 5)), cy + Math.sin(a) * (r - (maj ? 9 : 5)));
      ctx.lineTo(cx + Math.cos(a) * (r - 2), cy + Math.sin(a) * (r - 2)); ctx.stroke();
      if (maj) { var lr = r - 14, lbl = i === 0 ? -maxRange : (i === 5 ? 0 : maxRange);
        ctx.fillText((lbl > 0 ? '+' : '') + lbl, cx + Math.cos(a) * lr, cy + Math.sin(a) * lr + 2); }
    }
    var frac = (pool / Math.max(1, maxRange) + 1) / 2;
    var target = startA + sweep * Math.max(0, Math.min(1, frac));
    if (boostNeedleAngle === null) boostNeedleAngle = target;
    boostNeedleAngle = boostNeedleAngle * 0.85 + target * 0.15;
    var nc = pool === 0 ? '#666' : (pool > 0 ? '#5aab63' : '#cf4747');
    ctx.strokeStyle = nc; ctx.lineWidth = 2; ctx.lineCap = 'round';
    ctx.beginPath(); ctx.moveTo(cx, cy); ctx.lineTo(cx + Math.cos(boostNeedleAngle) * (r - 4), cy + Math.sin(boostNeedleAngle) * (r - 4)); ctx.stroke();
    ctx.fillStyle = '#1a1a1a'; ctx.beginPath(); ctx.arc(cx, cy, 3, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = nc; ctx.font = 'bold 12px Segoe UI'; ctx.fillText((pool > 0 ? '+' : '') + pool, cx, cy + 10);
    ctx.fillStyle = '#555'; ctx.font = '7px Segoe UI'; ctx.fillText('BOOST', cx, cy + 19);
  }

  // ============================================================
  //  Buffer gauge dials (in DOM panel, matching factory.js)
  // ============================================================
  var COUNT_MAX = 100, ZONE_GREEN = 20, ZONE_YELLOW = 70;
  var gaugeNeedles = {};

  function drawCountDial(cv, count, needle, key, needleColor) {
    var dpr = window.devicePixelRatio || 1;
    var cssW = cv.clientWidth || 90, cssH = cv.clientHeight || 75;
    if (cv.width !== cssW * dpr) { cv.width = cssW * dpr; cv.height = cssH * dpr; }
    var g = cv.getContext('2d'); g.setTransform(dpr, 0, 0, dpr, 0, 0); g.clearRect(0, 0, cssW, cssH);
    var cx = cssW / 2, cy = cssH * 0.92, r = Math.min(cssW * 0.46, cssH * 0.85);
    var startA = Math.PI * 1.15, endA = Math.PI * -0.15, sweep = endA - startA + Math.PI * 2;
    var z1 = ZONE_GREEN / COUNT_MAX, z2 = ZONE_YELLOW / COUNT_MAX;
    g.lineWidth = 6; g.lineCap = 'butt';
    g.strokeStyle = '#5aab63'; g.beginPath(); g.arc(cx, cy, r, startA, startA + sweep * z1); g.stroke();
    g.strokeStyle = '#e8c060'; g.beginPath(); g.arc(cx, cy, r, startA + sweep * z1, startA + sweep * z2); g.stroke();
    g.strokeStyle = '#cf4747'; g.beginPath(); g.arc(cx, cy, r, startA + sweep * z2, startA + sweep); g.stroke();
    g.strokeStyle = '#666'; g.lineWidth = 1; g.fillStyle = '#888'; g.font = '8px Segoe UI'; g.textAlign = 'center';
    for (var v = 0; v <= COUNT_MAX; v += 10) {
      var a = startA + sweep * (v / COUNT_MAX), major = (v % 20 === 0);
      var r1 = r - (major ? 12 : 7), r2 = r - 4;
      g.beginPath(); g.moveTo(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
      g.lineTo(cx + Math.cos(a) * r2, cy + Math.sin(a) * r2); g.stroke();
      if (major) { var lr = r - 18; g.fillText(String(v), cx + Math.cos(a) * lr, cy + Math.sin(a) * lr + 3); }
    }
    var target = startA + sweep * Math.max(0, Math.min(1, count / COUNT_MAX));
    needle[key] = needle[key] * 0.85 + target * 0.15;
    g.strokeStyle = needleColor; g.lineWidth = 2; g.lineCap = 'round';
    g.beginPath(); g.moveTo(cx, cy); g.lineTo(cx + Math.cos(needle[key]) * (r - 5), cy + Math.sin(needle[key]) * (r - 5)); g.stroke();
    g.fillStyle = '#1a1a1a'; g.beginPath(); g.arc(cx, cy, 3, 0, Math.PI * 2); g.fill();
  }

  function bufColor(c) { return c > ZONE_YELLOW ? '#cf4747' : (c > ZONE_GREEN ? '#e8c060' : '#5aab63'); }

  function renderGauges() {
    $('#p-gd-gauges .gauge').each(function () {
      var id = this.getAttribute('data-id'), p = places[id];
      if (!p) return;
      if (!gaugeNeedles[id]) gaugeNeedles[id] = { a: 0 };
      var cnt = p.tokens.length;
      drawCountDial(this.querySelector('.gc'), cnt, gaugeNeedles[id], 'a', '#4880c8');
      var ve = this.querySelector('.vcnt');
      ve.textContent = cnt; ve.style.color = bufColor(cnt);
    });
  }

  function rebuildGaugeTiles() {
    var $g = $('#p-gd-gauges').empty();
    for (var i = 0; i < placeList.length; i++) {
      var p = placeList[i]; if (p.kind !== 'buffer') continue;
      $g.append('<div class="gauge" data-id="' + p.id + '">'
        + '<div class="gauge-label">' + p.label + '</div>'
        + '<canvas class="gc"></canvas>'
        + '<div class="gh-cap"><span class="vcnt">0</span></div></div>');
      if (!gaugeNeedles[p.id]) gaugeNeedles[p.id] = { a: 0 };
    }
  }

  // ============================================================
  //  RHS Control panel
  // ============================================================
  var activeTab = 'boost';
  var TAB_HINTS = {
    boost: 'Boost = parallel slots. Higher boost → faster throughput. Pool shown in the top-left gauge.',
    processTime: 'Base process time per job in seconds. Effective interval = time / boost.',
    routing: 'Tick a box to process that colour at that stage. Unticked = bypass (skip arc).'
  };

  function buildControlPanel() {
    var $strip = $('#p-tab-strip');
    $strip.find('.tab').removeClass('active').filter('[data-tab="' + activeTab + '"]').addClass('active');
    $('#p-tab-hint').text(TAB_HINTS[activeTab] || '');
    var $host = $('#p-node-controls').empty();

    if (activeTab === 'routing') {
      // Routing table: rows = skippable machines, columns = colours
      var html = '<table class="route-tbl"><thead><tr><th></th>';
      for (var ci = 0; ci < COLORS.length; ci++) {
        html += '<th><span class="rt-chip" style="background:' + COLOR_HEX[COLORS[ci]] + '">' + COLOR_LET[COLORS[ci]] + '</span></th>';
      }
      html += '</tr></thead><tbody>';
      for (var si = 0; si < SKIPPABLE.length; si++) {
        var sk = SKIPPABLE[si];
        html += '<tr><td class="rt-label">' + SKIP_LABELS[sk] + '</td>';
        for (var ci2 = 0; ci2 < COLORS.length; ci2++) {
          var col = COLORS[ci2], checked = routeCfg[sk][col] ? ' checked' : '';
          html += '<td><input type="checkbox" class="rt-cb" data-sk="' + sk + '" data-col="' + col + '"' + checked + ' /></td>';
        }
        html += '</tr>';
      }
      html += '</tbody></table>';
      html += '<div class="rt-expect"><b>Expected codes:</b>';
      for (var ci3 = 0; ci3 < COLORS.length; ci3++) {
        var cc = COLORS[ci3];
        html += '<div class="rt-ex-row"><span class="rt-chip" style="background:' + COLOR_HEX[cc] + '">' + COLOR_LET[cc] + '</span> '
          + '<code>' + EXPECT_CODE[cc] + '</code></div>';
      }
      html += '</div>';
      $host.html(html);
      return;
    }

    for (var i = 0; i < transList.length; i++) {
      var tr = transList[i];
      var $card = $('<div class="ctl-card"></div>');
      $card.append('<div class="ctl-card-h"><b>' + tr.label + '</b> <span class="ctl-id">' + tr.id + '</span></div>');
      if (activeTab === 'boost') {
        $card.append('<label class="ctl-line">Boost <b class="v">' + tr.boost + '</b>'
          + '<input type="range" min="1" max="' + MAX_BOOST + '" step="1" value="' + tr.boost
          + '" data-tr="' + tr.id + '" data-knob="boost" /></label>');
      } else if (activeTab === 'processTime') {
        $card.append('<label class="ctl-line">Time <b class="v">' + tr.processTime.toFixed(1) + 's</b>'
          + '<input type="range" min="0.5" max="' + MAX_PROCESS_TIME + '" step="0.5" value="' + tr.processTime
          + '" data-tr="' + tr.id + '" data-knob="processTime" /></label>');
      }
      $host.append($card);
    }
  }

  function syncPanelSlider(tr) {
    // When popup changes a value, sync the matching panel slider
    $('#p-node-controls input[data-tr="' + tr.id + '"][data-knob="boost"]').val(tr.boost).parent().find('.v').text(tr.boost);
    $('#p-node-controls input[data-tr="' + tr.id + '"][data-knob="processTime"]').val(tr.processTime).parent().find('.v').text(tr.processTime.toFixed(1) + 's');
  }

  // ============================================================
  //  Frame loop
  // ============================================================
  var frameCount = 0, fpsT = 0;
  function frame(now) {
    var view = document.getElementById('petri-view');
    if (!view || !view.classList.contains('active')) { requestAnimationFrame(frame); return; }
    if (!lastT) lastT = now;
    var wall = (now - lastT) / 1000; lastT = now;
    var dt = Math.min(wall * speed, 0.5);
    tick(dt); tickTravelers(dt); drawAll(); updateStats(); refreshPopupStats(); renderGauges();
    frameCount++;
    if (now - fpsT > 1000) { var el = document.getElementById('p-fps'); if (el) el.textContent = Math.round(frameCount * 1000 / (now - fpsT)); frameCount = 0; fpsT = now; }
    requestAnimationFrame(frame);
  }

  function fmtMMSS(s) { var m = Math.floor(s / 60), sec = Math.floor(s % 60); return m + ':' + (sec < 10 ? '0' : '') + sec; }

  function updateStats() {
    function el(id) { return document.getElementById(id); }
    var se = el('p-status');
    if (se) { if (running && !paused) se.textContent = 'RUNNING'; else if (paused) se.textContent = 'PAUSED'; else if (runStopReason) se.textContent = runStopReason === 'IDLE_STOP' ? 'STOPPED (idle)' : 'FINISHED'; else se.textContent = 'IDLE'; }
    var t = el('p-time'); if (t) t.textContent = runFinalSec !== null ? fmtMMSS(runFinalSec) : (running ? fmtMMSS(runElapsedSec) : '-');
    if (el('p-out')) el('p-out').textContent = totalDispatched;
    if (el('p-batch')) el('p-batch').textContent = batchSize;
    if (el('p-rej')) el('p-rej').textContent = rejects.total;
    if (el('p-rej-y')) el('p-rej-y').textContent = rejects.yellow;
    if (el('p-rej-b')) el('p-rej-b').textContent = rejects.blue;
    if (el('p-rej-g')) el('p-rej-g').textContent = rejects.green;
    if (el('p-rej-x')) el('p-rej-x').textContent = rejects.gray;
    if (el('p-speed')) el('p-speed').textContent = speed.toFixed(1);
    var inf = 0;
    for (var id in places) if (places[id].kind === 'buffer') inf += places[id].tokens.length;
    for (var id2 in transitions) for (var si = 0; si < transitions[id2].slots.length; si++) inf += transitions[id2].slots[si].units.length;
    inf += travelers.length;
    if (el('p-flight')) el('p-flight').textContent = inf;
  }

  // ============================================================
  //  Controls
  // ============================================================
  function bindControls() {
    $(document).on('click', '#p-start', function () { startBatch(parseInt($('#p-batch-input').val()) || 100); });
    $(document).on('click', '#p-reset', function () { resetSim(); running = false; hidePopup(); });
    $(document).on('input', '#p-speed-input', function () { speed = parseFloat(this.value) || 1; });
    $(document).on('change', '#p-pause', function () { paused = this.checked; });
    $(document).on('change', '#p-backpressure', function () { backpressureEnabled = this.checked; });

    // Canvas click
    $(document).on('click', '#petri-canvas', function (evt) {
      var rect = canvas.getBoundingClientRect();
      var mx = (evt.clientX - rect.left) * (W / rect.width), my = (evt.clientY - rect.top) * (H / rect.height);
      var hit = hitTestTransition(mx, my);
      if (hit) showPopup(hit); else hidePopup();
    });
    // Popup sliders
    $(document).on('input', '#pp-interval', function () {
      if (!selectedTr) return;
      selectedTr.processTime = Math.max(0.5, parseFloat(this.value));
      document.getElementById('pp-interval-val').textContent = selectedTr.processTime.toFixed(1) + 's';
      syncPanelSlider(selectedTr);
    });
    $(document).on('input', '#pp-boost', function () {
      if (!selectedTr) return;
      selectedTr.boost = Math.max(1, Math.round(parseFloat(this.value)));
      document.getElementById('pp-boost-val').textContent = selectedTr.boost;
      syncPanelSlider(selectedTr);
    });
    $(document).on('click', '#pp-close', function () { hidePopup(); });

    // Panel tab strip
    $(document).on('click', '#p-tab-strip .tab', function () { activeTab = this.getAttribute('data-tab'); buildControlPanel(); });
    // Routing table checkbox changes
    $(document).on('change', '.rt-cb', function () {
      var sk = this.getAttribute('data-sk'), col = this.getAttribute('data-col');
      routeCfg[sk][col] = this.checked;
      EXPECT_CODE = computeExpectCode();
      buildControlPanel(); // refresh expected codes display
    });
    // Panel knob changes
    $(document).on('input', '#p-node-controls input[type=range]', function () {
      var v = parseFloat(this.value), $v = $(this).parent().find('.v').first();
      var knob = this.getAttribute('data-knob'), tr = transitions[this.getAttribute('data-tr')];
      if (!tr) return;
      if (knob === 'boost') { tr.boost = Math.max(1, Math.round(v)); $v.text(tr.boost); }
      else if (knob === 'processTime') { tr.processTime = Math.max(0.5, v); $v.text(v.toFixed(1) + 's'); }
    });
  }

  // ============================================================
  //  Boot
  // ============================================================
  $(function () {
    if (!document.getElementById('petri-view')) return;
    resize();
    window.addEventListener('resize', resize);
    bindControls();
    buildControlPanel();
    rebuildGaugeTiles();
    requestAnimationFrame(frame);
  });

})();
