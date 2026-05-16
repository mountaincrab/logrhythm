// ── Direction 3: "Crabby" ─────────────────────────────────────
// Retro magenta theme. The crab mascot has presence. Bigger,
// rounder, more playful forms. Copy has a wink. Same data
// model + IA — just dressed for warmth.

const D3 = {};

D3.styles = `
.d3 { background: var(--bg); color: var(--fg); height: 100%; display: flex; flex-direction: column; font-size: 14px; }
.d3 .topbar { padding: 12px 18px 4px; display: flex; align-items: center; gap: 12px; }
.d3 .topbar h1 { margin: 0; font-size: 28px; font-weight: 900; letter-spacing: -0.02em; color: #FFFFFF; line-height: 1; }
.d3 .topbar .sub { margin: 4px 0 0; font-size: 12px; color: var(--fg-muted); }
.d3 .topbar .iconbtn { width: 40px; height: 40px; border-radius: 14px; background: var(--surface-raised); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; color: var(--fg-muted); }
.d3 .topbar .iconbtn.primary { background: var(--accent); color: var(--accent-fg); border: none; }

.d3 .scroll { flex: 1; min-height: 0; overflow: auto; padding: 8px 16px 20px; }
.d3 .scroll::-webkit-scrollbar { display: none; }

/* Crab greeting */
.d3 .greeting { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 22px; padding: 16px 18px 16px 86px; margin-bottom: 14px; position: relative; }
.d3 .greeting .crab { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 58px; line-height: 1; }
.d3 .greeting .bubble { font-size: 14px; line-height: 1.45; color: var(--fg); }
.d3 .greeting .bubble b { color: var(--accent-text); font-weight: 700; }
.d3 .greeting .tail { position: absolute; left: 64px; top: 28px; width: 12px; height: 12px; transform: rotate(45deg); background: var(--surface-raised); border-left: 1px solid var(--border); border-bottom: 1px solid var(--border); }

/* Big "log it" cards */
.d3 .logdock { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 18px; }
.d3 .logcard { padding: 18px; border-radius: 22px; display: flex; flex-direction: column; gap: 8px; position: relative; overflow: hidden; min-height: 130px; }
.d3 .logcard.primary { background: var(--accent); color: var(--accent-fg); grid-column: span 2; }
.d3 .logcard.primary .em { font-size: 64px; position: absolute; right: -8px; top: -8px; opacity: 0.45; line-height: 1; }
.d3 .logcard.primary .head { font-size: 12px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; opacity: 0.75; }
.d3 .logcard.primary .body { font-size: 22px; font-weight: 800; letter-spacing: -0.01em; line-height: 1.1; max-width: 220px; }
.d3 .logcard.primary .cta { margin-top: auto; font-size: 13px; font-weight: 700; display: flex; align-items: center; gap: 6px; }
.d3 .logcard.secondary { background: var(--surface-raised); border: 1px solid var(--border); }
.d3 .logcard.secondary .em { font-size: 32px; }
.d3 .logcard.secondary .head { font-size: 11px; font-weight: 700; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); margin-top: 4px; }
.d3 .logcard.secondary .body { font-size: 17px; font-weight: 800; }

/* Today */
.d3 .secthead { display: flex; align-items: baseline; justify-content: space-between; padding: 4px 4px 10px; }
.d3 .secthead h2 { margin: 0; font-size: 14px; font-weight: 800; }
.d3 .secthead .meta { font-size: 11px; color: var(--fg-muted); font-weight: 600; }

.d3 .entry { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 18px; padding: 12px 14px; margin-bottom: 8px; display: flex; align-items: center; gap: 12px; }
.d3 .entry .stamp { width: 44px; height: 44px; border-radius: 14px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; font-size: 22px; }
.d3 .entry.poop .stamp { background: var(--rsoft); }
.d3 .entry.food .stamp { background: var(--surface-high); color: var(--fg-muted); }
.d3 .entry.med  .stamp { background: rgba(255,238,0,0.16); color: var(--warning); }
.d3 .entry .body { flex: 1; min-width: 0; }
.d3 .entry .body .t { font-size: 14px; font-weight: 700; color: var(--fg); display: flex; align-items: baseline; gap: 8px; }
.d3 .entry .body .t .tm { font-family: var(--font-mono); font-size: 12px; color: var(--fg-muted); font-weight: 600; }
.d3 .entry .body .m { font-size: 12px; color: var(--fg-muted); margin-top: 2px; line-height: 1.35; }
.d3 .entry .rating { background: var(--rc); color: var(--rfg); padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 800; flex-shrink: 0; display: inline-flex; align-items: center; gap: 4px; }

/* Tab bar */
.d3 .tabbar { display: flex; background: var(--surface); border-top: 1px solid var(--border); padding: 10px 12px 10px; gap: 4px; flex-shrink: 0; }
.d3 .tabbar .tab { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; padding: 2px 0; font-size: 11px; font-weight: 700; color: var(--fg-muted); }
.d3 .tabbar .tab .pill { width: 60px; height: 32px; border-radius: 999px; display: flex; align-items: center; justify-content: center; color: inherit; }
.d3 .tabbar .tab.active { color: var(--fg); }
.d3 .tabbar .tab.active .pill { background: var(--accent); color: var(--accent-fg); }

/* Sheet */
.d3 .sheet { background: var(--bg); height: 100%; display: flex; flex-direction: column; }
.d3 .sheet .sheethead { padding: 14px 18px 8px; display: flex; align-items: center; gap: 12px; }
.d3 .sheet .sheethead h1 { margin: 0; font-size: 24px; font-weight: 900; letter-spacing: -0.02em; flex: 1; }
.d3 .sheet .sheethead .x { width: 40px; height: 40px; border-radius: 14px; background: var(--surface-raised); display: flex; align-items: center; justify-content: center; color: var(--fg-muted); border: 1px solid var(--border); }
.d3 .sheet .body { flex: 1; overflow: auto; padding: 4px 18px 20px; }
.d3 .sheet .body::-webkit-scrollbar { display: none; }

.d3 .helper { font-size: 13px; color: var(--fg-muted); margin: 0 0 12px; line-height: 1.45; }
.d3 .helper b { color: var(--accent-text); font-weight: 700; }

.d3 .field { margin-bottom: 18px; }
.d3 .lab { font-size: 11px; font-weight: 800; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); margin-bottom: 8px; display: flex; align-items: center; justify-content: space-between; }
.d3 .lab .right { font-size: 11px; color: var(--accent-text); font-weight: 700; letter-spacing: 0; text-transform: none; }

.d3 .pillrow { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.d3 .pillrow .pill { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 16px; padding: 14px 16px; display: flex; align-items: center; gap: 10px; }
.d3 .pillrow .pill .v { font-weight: 800; font-size: 16px; font-variant-numeric: tabular-nums; }
.d3 .pillrow .pill .l { font-size: 11px; color: var(--fg-muted); }

.d3 .typeseg { display: grid; grid-template-columns: 1fr 1fr; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 16px; padding: 4px; margin-bottom: 14px; }
.d3 .typeseg button { padding: 10px 0; border-radius: 12px; font-size: 13px; font-weight: 700; color: var(--fg-muted); background: transparent; }
.d3 .typeseg button.on { background: var(--accent); color: var(--accent-fg); }

/* Bristol "shapes" — illustrated dots */
.d3 .shapegrid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.d3 .shapecell { aspect-ratio: 1/1.4; border-radius: 16px; background: var(--surface-raised); border: 1.5px solid var(--border); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; position: relative; padding: 8px 4px; }
.d3 .shapecell.on { background: var(--accent-soft); border-color: var(--accent); }
.d3 .shapecell .ill { height: 38px; display: flex; align-items: center; justify-content: center; }
.d3 .shapecell .n { font-size: 14px; font-weight: 900; color: var(--fg); }
.d3 .shapecell.on .n { color: var(--accent-text); }
.d3 .shapecell .lab { font-size: 9px; color: var(--fg-muted); font-weight: 700; letter-spacing: 0.04em; text-align: center; line-height: 1.05; text-transform: uppercase; }
.d3 .shapecell.on .lab { color: var(--accent-text); }
.d3 .pickedHelper { background: var(--accent-soft); border: 1px solid var(--accent); border-radius: 14px; padding: 12px 14px; margin-top: 10px; font-size: 13px; color: var(--fg); line-height: 1.4; }
.d3 .pickedHelper b { color: var(--accent-text); }

/* Rating */
.d3 .ratingrow { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
.d3 .ratingrow .rp { aspect-ratio: 1/1.15; border-radius: 18px; background: var(--surface-raised); border: 2px solid var(--border); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; }
.d3 .ratingrow .rp.on { background: var(--rc); border-color: var(--rc); }
.d3 .ratingrow .rp .n { font-size: 26px; font-weight: 900; color: var(--fg); letter-spacing: -0.02em; line-height: 1; }
.d3 .ratingrow .rp.on .n { color: var(--rfg); }
.d3 .ratingrow .rp .drops { display: flex; gap: 1px; }
.d3 .ratingrow .rp .drops span { width: 4px; height: 5px; border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%; background: var(--rc); }
.d3 .ratingrow .rp.on .drops span { background: var(--rfg); }
.d3 .ratinghint { margin-top: 10px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 12px 14px; display: flex; align-items: center; gap: 10px; }
.d3 .ratinghint .pip { width: 28px; height: 28px; border-radius: 100px; background: var(--rc); color: var(--rfg); display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 14px; }
.d3 .ratinghint .t { font-size: 13px; font-weight: 700; color: var(--rc); }
.d3 .ratinghint .d { font-size: 12px; color: var(--fg-muted); }

.d3 .textarea { width: 100%; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 16px; padding: 14px 16px; color: var(--fg); font-size: 14px; resize: none; font-family: inherit; min-height: 88px; line-height: 1.45; }

.d3 .savebar { padding: 14px 18px 20px; border-top: 1px solid var(--border); display: flex; gap: 10px; background: var(--surface); flex-shrink: 0; }
.d3 .savebar .b { flex: 1; padding: 16px; border-radius: 16px; font-size: 15px; font-weight: 800; }
.d3 .savebar .b.cancel { background: transparent; color: var(--fg-muted); }
.d3 .savebar .b.save { background: var(--accent); color: var(--accent-fg); }
`;
window.D3 = D3;
