// ── Shared phone frame for the IBD tracker prototype ──────────
// Native size 412×892 (Pixel-ish). Rendered inside DCArtboard at native size;
// the canvas itself handles zoom. We don't CSS-scale here.

const { useState, useEffect, useRef, useMemo } = React;

// ── Lucide helper ─────────────────────────────────────────────
function I({ name, size = 18, stroke = 2, color, style }) {
  const ref = useRef(null);
  useEffect(() => { if (window.lucide) window.lucide.createIcons(); });
  return <i ref={ref} data-lucide={name} style={{ width: size, height: size, color, strokeWidth: stroke, display: 'inline-flex', flexShrink: 0, ...style }}/>;
}
window.I = I;

// ── Phone shell (Android, dark) ───────────────────────────────
function Phone({ theme = 'deep-navy', children, time = "09:42", battery = 78, hideChrome = false, bg }) {
  return (
    <div data-theme={theme} style={{
      width: 412, height: 892, borderRadius: 38, overflow: 'hidden',
      background: bg || 'var(--bg)',
      border: '8px solid #18181c',
      boxShadow: '0 30px 80px rgba(0,0,0,.45), inset 0 0 0 1px rgba(255,255,255,.04)',
      display: 'flex', flexDirection: 'column', boxSizing: 'border-box',
      fontFamily: 'var(--font-sans)', color: 'var(--fg)'
    }}>
      {/* Status bar */}
      {!hideChrome && (
        <div style={{
          height: 32, display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '0 22px', background: 'transparent', color: 'var(--fg)',
          fontSize: 13, fontWeight: 600, position: 'relative', flexShrink: 0, zIndex: 5
        }}>
          <span style={{ fontVariantNumeric: 'tabular-nums' }}>{time}</span>
          <div style={{
            position: 'absolute', left: '50%', top: 6, transform: 'translateX(-50%)',
            width: 22, height: 22, borderRadius: 100, background: '#000'
          }}/>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <I name="signal" size={14}/>
            <I name="wifi" size={14}/>
            <span style={{ fontSize: 11, padding: '1px 5px', border: '1.5px solid currentColor', borderRadius: 4, fontWeight: 700 }}>{battery}</span>
          </div>
        </div>
      )}
      <div style={{ flex: 1, minHeight: 0, position: 'relative', display: 'flex', flexDirection: 'column' }}>{children}</div>
      {/* Gesture bar */}
      {!hideChrome && (
        <div style={{ height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <div style={{ width: 130, height: 4, borderRadius: 2, background: 'var(--fg)', opacity: .35 }}/>
        </div>
      )}
    </div>
  );
}
window.Phone = Phone;

// ── Rating pill colours (1–5, green→red) ──────────────────────
const RATING_COLORS = {
  1: { bg: '#10B981', fg: '#FFFFFF', soft: 'rgba(16,185,129,0.16)', label: 'No blood' },
  2: { bg: '#84CC16', fg: '#0a0a0a', soft: 'rgba(132,204,22,0.16)', label: 'Trace' },
  3: { bg: '#F59E0B', fg: '#0a0a0a', soft: 'rgba(245,158,11,0.18)', label: 'Small amount' },
  4: { bg: '#F97316', fg: '#FFFFFF', soft: 'rgba(249,115,22,0.18)', label: 'Quite a lot' },
  5: { bg: '#EF4444', fg: '#FFFFFF', soft: 'rgba(239,68,68,0.20)', label: 'Loads' },
};
window.RATING_COLORS = RATING_COLORS;

// Bristol stool scale (1–7) with plain-English aliases for the "my types" mode
const BRISTOL = [
  { n: 1, plain: 'Hard lumps',    desc: 'Separate hard lumps, like nuts',     glyph: '●●●' },
  { n: 2, plain: 'Lumpy',         desc: 'Sausage-shaped but lumpy',           glyph: '▓▓▓' },
  { n: 3, plain: 'Cracked',       desc: 'Sausage with cracks on its surface', glyph: '━━━' },
  { n: 4, plain: 'Smooth',        desc: 'Smooth and soft, like a sausage',    glyph: '━━━' },
  { n: 5, plain: 'Soft lumps',    desc: 'Soft blobs with clear-cut edges',    glyph: '◔◔◔' },
  { n: 6, plain: 'Mushy',         desc: 'Fluffy pieces, ragged edges',        glyph: '∿∿∿' },
  { n: 7, plain: 'Liquid',        desc: 'Entirely liquid, no solid pieces',   glyph: '~~~' },
];
window.BRISTOL = BRISTOL;

// ── Mock data (deterministic, shared across all 3 directions) ─
// Today = Fri 30 Jan 2026
const TODAY_LABEL = 'Fri 30 Jan';
const ENTRIES_TODAY = [
  { type: 'food',  time: '10:32', items: 'Apple, water' },
  { type: 'poop',  time: '09:40', bristol: 6, plain: 'Mushy', rating: 1, notes: '' },
  { type: 'food',  time: '08:15', items: 'Coffee, buttered toast, banana' },
];
const ENTRIES_YESTERDAY = [
  { type: 'food',  time: '21:00', items: 'Steak pie (Satterthwaites), NY cookie' },
  { type: 'food',  time: '17:30', items: 'Salt & pepper chicken, fried rice' },
  { type: 'med',   time: '14:10', items: 'Missed morning mesalazine' },
  { type: 'food',  time: '13:00', items: 'Bagels, butter. Banana' },
  { type: 'poop',  time: '08:45', bristol: 6, plain: 'Mushy', rating: 2, notes: 'A bit urgent' },
];
window.ENTRIES_TODAY = ENTRIES_TODAY;
window.ENTRIES_YESTERDAY = ENTRIES_YESTERDAY;
window.TODAY_LABEL = TODAY_LABEL;

// 30-day rating history (one peak rating per day; null = no poop logged)
// Deterministic: a recent flare two weeks ago, recovery, and a small bump 3 days ago.
const RATING_30D = [
  // 30 days ago → today (left to right)
  1, 1, 2, 1, 1, null, 1, 2, 3, 4, 3, 2, 2, 1, 1, 1, null, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 1, 2, 1
];
window.RATING_30D = RATING_30D;

// Daily frequency (poops per day) over 30 days
const FREQ_30D = [
  2, 1, 2, 1, 2, 0, 1, 3, 4, 5, 4, 3, 2, 1, 2, 1, 0, 1, 2, 2, 1, 1, 2, 3, 2, 1, 1, 1, 2, 1
];
window.FREQ_30D = FREQ_30D;

// Suspect foods (corr w/ bad days) — for the "food correlation" view
const FOOD_CORR = [
  { food: 'Fried rice',     uplift: '+2.1', days: 8,  bad: 5 },
  { food: 'Spicy chicken',  uplift: '+1.8', days: 6,  bad: 4 },
  { food: 'Beer',           uplift: '+1.4', days: 4,  bad: 3 },
  { food: 'Cheese',         uplift: '+0.6', days: 12, bad: 4 },
  { food: 'Apples',         uplift: '−0.2', days: 18, bad: 3 },
  { food: 'White rice',     uplift: '−0.4', days: 14, bad: 2 },
];
window.FOOD_CORR = FOOD_CORR;
