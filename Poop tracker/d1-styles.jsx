// ── Direction 1: "Quiet Crab" ─────────────────────────────────
// Closest match to Crab Do's existing visual language.
// Deep Navy, indigo accent, sober timeline-by-day home.
// Friendly touches kept to copywriting + a small mascot in the header.

const D1 = {};

// ── Local styles, scoped to .d1 ───────────────────────────────
D1.styles = `
.d1 { background: var(--bg); color: var(--fg); height: 100%; display: flex; flex-direction: column; font-size: 14px; }
.d1 .topbar { padding: 12px 20px 4px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.d1 .topbar h1 { margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.015em; }
.d1 .topbar .sub { margin: 2px 0 0; font-size: 13px; color: var(--fg-muted); }
.d1 .topbar .iconbtn { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: var(--fg-muted); background: var(--surface); }
.d1 .scroll { flex: 1; min-height: 0; overflow: auto; }
.d1 .scroll::-webkit-scrollbar { display: none; }
.d1 .daygrp { padding: 14px 20px 6px; }
.d1 .daygrp .label { display: flex; align-items: baseline; justify-content: space-between; gap: 8px; margin-bottom: 10px; }
.d1 .daygrp .label .d { font-size: 11px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); }
.d1 .daygrp .label .sum { font-size: 11px; color: var(--fg-faint); font-weight: 600; }
.d1 .timeline { position: relative; padding-left: 22px; }
.d1 .timeline::before { content: ''; position: absolute; left: 7px; top: 6px; bottom: 6px; width: 1px; background: var(--border); }
.d1 .te { position: relative; display: flex; gap: 0; margin-bottom: 8px; }
.d1 .te .dot { position: absolute; left: -19px; top: 16px; width: 11px; height: 11px; border-radius: 100px; background: var(--surface-high); border: 2px solid var(--bg); }
.d1 .te.poop .dot { background: var(--rating-c); }
.d1 .te .card { flex: 1; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 12px 14px; min-width: 0; }
.d1 .te .row1 { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.d1 .te .row1 .t { font-size: 13px; font-weight: 700; color: var(--fg); font-variant-numeric: tabular-nums; }
.d1 .te .row1 .k { font-size: 11px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; color: var(--fg-faint); }
.d1 .te .body { color: var(--fg); font-size: 14px; line-height: 1.4; }
.d1 .te .body.muted { color: var(--fg-muted); }
.d1 .te .meta { margin-top: 6px; display: flex; align-items: center; gap: 10px; font-size: 12px; color: var(--fg-muted); }
.d1 .rpill { display: inline-flex; align-items: center; gap: 6px; font-weight: 700; font-size: 12px; padding: 3px 9px; border-radius: 999px; background: var(--rating-soft); color: var(--rating-c); }
.d1 .rpill .n { background: var(--rating-c); color: var(--rating-fg); width: 18px; height: 18px; border-radius: 100px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 800; }
.d1 .fab { position: absolute; right: 16px; bottom: 76px; width: 60px; height: 60px; border-radius: 22px; background: var(--accent); color: var(--accent-fg); display: flex; align-items: center; justify-content: center; box-shadow: var(--shadow-fab); }
.d1 .tabbar { display: flex; background: var(--surface); border-top: 1px solid var(--border); padding: 6px 8px 4px; gap: 4px; flex-shrink: 0; }
.d1 .tabbar .tab { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; padding: 6px 0; font-size: 11px; font-weight: 600; color: var(--fg-muted); }
.d1 .tabbar .tab .pill { width: 56px; height: 30px; border-radius: 999px; display: flex; align-items: center; justify-content: center; color: inherit; }
.d1 .tabbar .tab.active { color: var(--fg); }
.d1 .tabbar .tab.active .pill { background: var(--accent-soft); color: var(--accent-text); }

.d1 .summary { margin: 10px 20px 4px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 18px; padding: 16px; display: flex; align-items: center; gap: 14px; }
.d1 .summary .crab { font-size: 36px; line-height: 1; flex-shrink: 0; }
.d1 .summary .copy { flex: 1; min-width: 0; }
.d1 .summary .copy h2 { margin: 0; font-size: 17px; font-weight: 700; color: var(--fg); }
.d1 .summary .copy p { margin: 2px 0 0; font-size: 13px; color: var(--fg-muted); line-height: 1.4; }

.d1 .quickbar { display: flex; gap: 8px; padding: 12px 20px 4px; flex-wrap: wrap; }
.d1 .quickbar button { padding: 8px 12px; border-radius: 999px; background: var(--surface-raised); color: var(--fg); border: 1px solid var(--border); font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; }
.d1 .quickbar button.primary { background: var(--accent); color: var(--accent-fg); border-color: transparent; }

/* Add poop sheet */
.d1 .sheet { background: var(--bg); height: 100%; display: flex; flex-direction: column; }
.d1 .sheet .sheethead { padding: 14px 20px 8px; display: flex; align-items: center; gap: 12px; }
.d1 .sheet .sheethead h1 { margin: 0; font-size: 22px; font-weight: 800; letter-spacing: -0.015em; flex: 1; }
.d1 .sheet .sheethead .x { width: 38px; height: 38px; border-radius: 12px; background: var(--surface); display: flex; align-items: center; justify-content: center; color: var(--fg-muted); }
.d1 .sheet .body { flex: 1; overflow: auto; padding: 4px 20px 24px; }
.d1 .sheet .body::-webkit-scrollbar { display: none; }
.d1 .field { margin-bottom: 18px; }
.d1 .field .lab { font-size: 11px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }
.d1 .field .lab .hint { font-size: 11px; color: var(--accent-text); font-weight: 600; letter-spacing: 0; text-transform: none; }
.d1 .when { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.d1 .when .pill { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; display: flex; align-items: center; gap: 8px; }
.d1 .when .pill .v { font-weight: 700; font-variant-numeric: tabular-nums; }

.d1 .typeseg { display: grid; grid-template-columns: 1fr 1fr; background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 3px; }
.d1 .typeseg button { padding: 8px 0; border-radius: 9px; font-size: 12px; font-weight: 600; color: var(--fg-muted); background: transparent; }
.d1 .typeseg button.on { background: var(--surface-high); color: var(--fg); }
.d1 .bristolgrid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.d1 .bristolgrid .b { aspect-ratio: 1/1.1; border-radius: 12px; background: var(--surface-raised); border: 1px solid var(--border); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; }
.d1 .bristolgrid .b.on { background: var(--accent-soft); border-color: var(--accent); }
.d1 .bristolgrid .b .n { font-size: 18px; font-weight: 800; color: var(--fg); }
.d1 .bristolgrid .b.on .n { color: var(--accent-text); }
.d1 .bristolgrid .b .lab { font-size: 9px; font-weight: 600; color: var(--fg-muted); }
.d1 .bristol-desc { margin-top: 8px; font-size: 12px; color: var(--fg-muted); line-height: 1.4; }
.d1 .bristol-desc b { color: var(--fg); font-weight: 600; }

.d1 .ratingpills { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
.d1 .ratingpills button { aspect-ratio: 1/1; border-radius: 14px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; background: var(--surface-raised); border: 1.5px solid var(--border); position: relative; }
.d1 .ratingpills button .num { font-size: 22px; font-weight: 800; color: var(--fg); }
.d1 .ratingpills button.on { background: var(--rc); border-color: var(--rc); }
.d1 .ratingpills button.on .num { color: var(--rfg); }
.d1 .ratingpills button .swatch { position: absolute; top: 6px; right: 6px; width: 8px; height: 8px; border-radius: 100px; background: var(--rc); }
.d1 .ratingpills button.on .swatch { display: none; }
.d1 .rating-label { margin-top: 8px; font-size: 13px; color: var(--fg); display: flex; align-items: center; gap: 6px; }
.d1 .rating-label b { color: var(--rc); font-weight: 700; }

.d1 .textarea { width: 100%; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 12px; padding: 12px 14px; color: var(--fg); font-size: 14px; resize: none; font-family: inherit; min-height: 80px; }

.d1 .more-toggle { display: flex; align-items: center; gap: 8px; color: var(--accent-text); font-size: 13px; font-weight: 600; padding: 6px 0 14px; }
.d1 .more-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.d1 .more-grid .opt { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 12px; padding: 10px 12px; display: flex; align-items: center; gap: 10px; }
.d1 .more-grid .opt .ic { width: 32px; height: 32px; border-radius: 10px; background: var(--surface-high); display: flex; align-items: center; justify-content: center; color: var(--fg-muted); flex-shrink: 0; }
.d1 .more-grid .opt .nm { font-size: 13px; font-weight: 600; }
.d1 .more-grid .opt .st { font-size: 11px; color: var(--fg-muted); }
.d1 .more-grid .opt.on { background: var(--accent-soft); border-color: var(--accent); }
.d1 .more-grid .opt.on .ic { background: var(--accent); color: var(--accent-fg); }
.d1 .more-grid .opt.on .st { color: var(--accent-text); }

.d1 .savebar { padding: 12px 20px 18px; border-top: 1px solid var(--border); display: flex; gap: 8px; background: var(--surface); flex-shrink: 0; }
.d1 .savebar .b { flex: 1; padding: 14px; border-radius: 14px; font-size: 15px; font-weight: 700; }
.d1 .savebar .b.cancel { background: var(--surface-high); color: var(--fg-muted); }
.d1 .savebar .b.save { background: var(--accent); color: var(--accent-fg); box-shadow: var(--shadow-fab); }

/* Detail */
.d1 .hero { padding: 4px 20px 14px; }
.d1 .hero .when { background: transparent; padding: 0; display: flex; align-items: baseline; gap: 8px; }
.d1 .hero .big { font-size: 30px; font-weight: 800; letter-spacing: -0.015em; line-height: 1.1; }
.d1 .hero .day { font-size: 13px; color: var(--fg-muted); margin-top: 2px; }
.d1 .blockcard { margin: 0 20px 12px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 16px; padding: 14px 16px; }
.d1 .blockcard .lab { font-size: 11px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); margin-bottom: 8px; }
.d1 .blockcard .val { font-size: 16px; font-weight: 600; color: var(--fg); }
.d1 .blockcard .val.muted { color: var(--fg-muted); font-weight: 400; }
.d1 .blockcard .ratingrow { display: flex; align-items: center; gap: 12px; }
.d1 .blockcard .bigrating { width: 56px; height: 56px; border-radius: 18px; background: var(--rc); color: var(--rfg); display: flex; align-items: center; justify-content: center; font-size: 26px; font-weight: 900; }

/* Trends + History (left to D1-screens.jsx as inline styles to keep this file readable) */
`;

window.D1 = D1;
