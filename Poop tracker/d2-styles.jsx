// ── Direction 2: "At a Glance" ────────────────────────────────
// Charcoal + cyan. Dashboard-first home. Bigger numbers,
// trend chart front and centre. Still friendly through copy,
// but the visual personality is "fitness-tracker for guts".

const D2 = {};

D2.styles = `
.d2 { background: var(--bg); color: var(--fg); height: 100%; display: flex; flex-direction: column; font-size: 14px; }
.d2 .topbar { padding: 12px 18px 4px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.d2 .topbar .lockup { display: flex; align-items: center; gap: 10px; }
.d2 .topbar .crab { width: 36px; height: 36px; border-radius: 12px; background: var(--accent-soft); display: flex; align-items: center; justify-content: center; font-size: 20px; }
.d2 .topbar h1 { margin: 0; font-size: 20px; font-weight: 800; letter-spacing: -0.01em; }
.d2 .topbar .sub { margin: 0; font-size: 12px; color: var(--fg-muted); }
.d2 .topbar .iconbtn { width: 38px; height: 38px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: var(--fg-muted); background: var(--surface-raised); border: 1px solid var(--border); }

.d2 .scroll { flex: 1; min-height: 0; overflow: auto; padding: 12px 16px 20px; }
.d2 .scroll::-webkit-scrollbar { display: none; }

/* Hero stat card */
.d2 .hero { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 22px; padding: 18px; margin-bottom: 12px; }
.d2 .hero .head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 14px; }
.d2 .hero .head .lab { font-size: 11px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); }
.d2 .hero .head .meta { font-size: 12px; color: var(--success-text); font-weight: 600; }
.d2 .hero .num { display: flex; align-items: baseline; gap: 10px; }
.d2 .hero .num .big { font-size: 64px; font-weight: 900; line-height: 1; letter-spacing: -0.03em; }
.d2 .hero .num .unit { font-size: 14px; color: var(--fg-muted); font-weight: 500; }
.d2 .hero .num .pill { font-size: 12px; font-weight: 700; padding: 4px 9px; border-radius: 999px; }
.d2 .hero .spark { margin-top: 14px; height: 50px; }

.d2 .quickdock { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 8px; margin-bottom: 14px; }
.d2 .qd { padding: 14px; border-radius: 18px; display: flex; flex-direction: column; gap: 4px; }
.d2 .qd.primary { background: var(--accent); color: var(--accent-fg); }
.d2 .qd.primary .l { color: rgba(255,255,255,0.85); }
.d2 .qd .ic { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; }
.d2 .qd .l { font-size: 11px; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; opacity: 0.8; }
.d2 .qd .t { font-size: 15px; font-weight: 700; }
.d2 .qd.secondary { background: var(--surface-raised); border: 1px solid var(--border); color: var(--fg); }
.d2 .qd.secondary .ic { color: var(--accent-text); }

/* Today's log mini list */
.d2 .section { margin: 6px 0 14px; }
.d2 .section .head { display: flex; align-items: baseline; justify-content: space-between; padding: 0 4px 8px; }
.d2 .section .head h2 { margin: 0; font-size: 14px; font-weight: 700; color: var(--fg); }
.d2 .section .head .all { font-size: 12px; font-weight: 600; color: var(--accent-text); }

.d2 .logrow { display: flex; align-items: center; gap: 12px; padding: 12px 14px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; margin-bottom: 6px; }
.d2 .logrow .time { width: 50px; font-size: 13px; font-weight: 700; color: var(--fg-muted); font-variant-numeric: tabular-nums; flex-shrink: 0; }
.d2 .logrow .ic { width: 34px; height: 34px; border-radius: 11px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.d2 .logrow.poop .ic { color: var(--rc); background: var(--rsoft); }
.d2 .logrow.food .ic { color: var(--fg-muted); background: var(--surface-high); }
.d2 .logrow.med  .ic { color: var(--warning); background: rgba(251,191,36,0.14); }
.d2 .logrow .body { flex: 1; min-width: 0; }
.d2 .logrow .body .t { font-size: 13px; font-weight: 700; color: var(--fg); }
.d2 .logrow .body .m { font-size: 12px; color: var(--fg-muted); margin-top: 1px; line-height: 1.35; }
.d2 .logrow .rating { font-size: 14px; font-weight: 800; color: var(--rfg); background: var(--rc); width: 28px; height: 28px; border-radius: 100px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }

.d2 .daydivider { padding: 18px 8px 6px; font-size: 11px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-faint); }

.d2 .tabbar { display: flex; background: var(--surface); border-top: 1px solid var(--border); padding: 6px 8px 4px; gap: 4px; flex-shrink: 0; }
.d2 .tabbar .tab { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; padding: 6px 0; font-size: 11px; font-weight: 600; color: var(--fg-muted); }
.d2 .tabbar .tab .pill { width: 56px; height: 30px; border-radius: 999px; display: flex; align-items: center; justify-content: center; color: inherit; }
.d2 .tabbar .tab.active { color: var(--fg); }
.d2 .tabbar .tab.active .pill { background: var(--accent-soft); color: var(--accent-text); }

/* Add poop / food */
.d2 .sheet { background: var(--bg); height: 100%; display: flex; flex-direction: column; }
.d2 .sheet .sheethead { padding: 14px 18px 6px; display: flex; align-items: center; gap: 12px; }
.d2 .sheet .sheethead h1 { margin: 0; font-size: 22px; font-weight: 800; letter-spacing: -0.015em; flex: 1; }
.d2 .sheet .sheethead .x { width: 38px; height: 38px; border-radius: 12px; background: var(--surface-raised); display: flex; align-items: center; justify-content: center; color: var(--fg-muted); border: 1px solid var(--border); }
.d2 .sheet .body { flex: 1; overflow: auto; padding: 8px 18px 18px; }
.d2 .sheet .body::-webkit-scrollbar { display: none; }
.d2 .field { margin-bottom: 18px; }
.d2 .lab { font-size: 11px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }

.d2 .when2 { display: flex; gap: 8px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 12px 14px; }
.d2 .when2 .seg { display: flex; align-items: center; gap: 8px; }
.d2 .when2 .seg .v { font-weight: 700; font-variant-numeric: tabular-nums; }
.d2 .when2 .div { width: 1px; background: var(--border); margin: 0 6px; }

.d2 .typeseg { display: grid; grid-template-columns: 1fr 1fr; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 3px; margin-bottom: 12px; }
.d2 .typeseg button { padding: 10px 0; border-radius: 11px; font-size: 13px; font-weight: 600; color: var(--fg-muted); background: transparent; }
.d2 .typeseg button.on { background: var(--accent); color: var(--accent-fg); }

/* Big visual bristol picker — illustrative rows */
.d2 .bristolList { display: flex; flex-direction: column; gap: 6px; }
.d2 .bristolRow { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 10px 14px; display: flex; align-items: center; gap: 12px; }
.d2 .bristolRow .n { width: 28px; height: 28px; border-radius: 100px; background: var(--surface-high); color: var(--fg); display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; flex-shrink: 0; }
.d2 .bristolRow.on { border-color: var(--accent); background: var(--accent-soft); }
.d2 .bristolRow.on .n { background: var(--accent); color: var(--accent-fg); }
.d2 .bristolRow .lab { font-size: 14px; font-weight: 700; }
.d2 .bristolRow .desc { font-size: 12px; color: var(--fg-muted); }
.d2 .bristolRow .glyph { font-family: var(--font-mono); letter-spacing: 0.2em; color: var(--accent-text); font-weight: 600; flex-shrink: 0; }

/* Rating row */
.d2 .ratingrow { display: flex; gap: 4px; }
.d2 .ratingrow .rp { flex: 1; aspect-ratio: 1/1.1; border-radius: 14px; background: var(--surface-raised); border: 1.5px solid var(--border); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; }
.d2 .ratingrow .rp.on { background: var(--rc); border-color: var(--rc); }
.d2 .ratingrow .rp .n { font-size: 24px; font-weight: 900; color: var(--fg); }
.d2 .ratingrow .rp.on .n { color: var(--rfg); }
.d2 .ratingrow .rp .dot { width: 7px; height: 7px; border-radius: 100px; background: var(--rc); }
.d2 .ratingrow .rp.on .dot { display: none; }
.d2 .ratinghint { font-size: 12px; color: var(--fg-muted); margin-top: 8px; }
.d2 .ratinghint b { color: var(--rc); font-weight: 700; }

.d2 .textarea { width: 100%; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 12px 14px; color: var(--fg); font-size: 14px; resize: none; font-family: inherit; min-height: 88px; }

.d2 .savebar { padding: 12px 18px 18px; border-top: 1px solid var(--border); display: flex; gap: 8px; background: var(--surface); flex-shrink: 0; }
.d2 .savebar .b { flex: 1; padding: 14px; border-radius: 14px; font-size: 15px; font-weight: 700; }
.d2 .savebar .b.cancel { background: transparent; color: var(--fg-muted); border: 1px solid var(--border); }
.d2 .savebar .b.save { background: var(--accent); color: var(--accent-fg); }

.d2 .blockcard { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 18px; padding: 16px; margin-bottom: 10px; }
.d2 .blockcard .lab { display: flex; align-items: baseline; justify-content: space-between; }
.d2 .blockcard .lab .more { font-size: 11px; color: var(--accent-text); font-weight: 600; letter-spacing: 0; text-transform: none; }
`;
window.D2 = D2;
