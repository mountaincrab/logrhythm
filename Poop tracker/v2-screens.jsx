// ── Refined v2 — incorporates feedback ────────────────────────
// Sober (D1) base. Home loses the greeting + FAB; gets a
// bottom log-bar. History combines calendar + trends in tabs.
// Entry detail uses the compact D2-style hero.
//
// All v2 screens reuse the D1 stylesheet (.d1) plus a small
// patch (.v2) for the new bottom dock and history tabs.

const V2 = {};
V2.styles = `
.v2 .logbar { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; padding: 10px 16px calc(env(safe-area-inset-bottom, 0px) + 10px); background: var(--surface); border-top: 1px solid var(--border); flex-shrink: 0; }
.v2 .logbar button { padding: 12px 8px; border-radius: 14px; background: var(--surface-raised); border: 1px solid var(--border); color: var(--fg-muted); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; font-size: 11px; font-weight: 700; }
.v2 .logbar button .em { font-size: 22px; line-height: 1; }
.v2 .logbar button:active { background: var(--surface-high); }

/* History — segmented sub-tabs */
.v2 .subtabs { display: flex; background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 3px; margin: 8px 16px 14px; }
.v2 .subtabs button { flex: 1; padding: 9px 0; border-radius: 9px; font-size: 13px; font-weight: 700; color: var(--fg-muted); background: transparent; display: inline-flex; align-items: center; justify-content: center; gap: 6px; }
.v2 .subtabs button.on { background: var(--accent); color: var(--accent-fg); }

.v2 .monthhead { padding: 4px 20px 8px; display: flex; align-items: center; gap: 8px; }
.v2 .monthhead .name { font-size: 18px; font-weight: 800; letter-spacing: -0.015em; flex: 1; }
.v2 .monthhead .nav { display: flex; gap: 6px; }
.v2 .monthhead .nav .b { width: 36px; height: 36px; border-radius: 11px; background: var(--surface-raised); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; color: var(--fg-muted); }

.v2 .calcard { margin: 0 16px 12px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 16px; padding: 14px; }
.v2 .calcard .wd { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; margin-bottom: 6px; }
.v2 .calcard .wd div { text-align: center; font-size: 10px; font-weight: 800; color: var(--fg-faint); letter-spacing: 0.05em; }
.v2 .calcard .grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.v2 .calday { aspect-ratio: 1/1; border-radius: 11px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1px; position: relative; }
.v2 .calday .num { font-size: 13px; font-weight: 700; line-height: 1; }
.v2 .calday .meta { font-size: 9px; font-weight: 600; opacity: 0.7; line-height: 1; }
.v2 .calday.empty { background: var(--surface); color: var(--fg-muted); }
.v2 .calday.empty .num { color: var(--fg-muted); }
.v2 .calday.future { background: transparent; }
.v2 .calday.future .num { color: var(--fg-disabled); }
.v2 .calday.today { box-shadow: inset 0 0 0 2px var(--accent); }
.v2 .calday.rated { color: var(--cfg); background: var(--cbg); }

.v2 .legend { margin: 0 16px 14px; padding: 12px 14px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 12px; display: flex; align-items: center; gap: 8px; font-size: 11px; color: var(--fg-muted); }
.v2 .legend .sw { width: 14px; height: 14px; border-radius: 4px; }

/* Trends-tab cards */
.v2 .rangepicker { margin: 0 16px 14px; display: flex; background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 3px; }
.v2 .rangepicker button { flex: 1; padding: 8px 0; border-radius: 9px; font-size: 12px; font-weight: 700; color: var(--fg-muted); background: transparent; }
.v2 .rangepicker button.on { background: var(--surface-high); color: var(--fg); }
.v2 .chartcard { margin: 0 16px 12px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 16px; padding: 16px; }
.v2 .chartcard .head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 4px; }
.v2 .chartcard .head .l { font-size: 11px; font-weight: 800; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); }
.v2 .chartcard .head .delta { font-size: 11px; font-weight: 700; }
.v2 .chartcard .v { display: flex; align-items: baseline; gap: 8px; margin: 4px 0 8px; }
.v2 .chartcard .v .big { font-size: 32px; font-weight: 900; letter-spacing: -0.02em; line-height: 1; }
.v2 .chartcard .v .unit { font-size: 12px; color: var(--fg-muted); }

/* Entry detail compact */
.v2 .detail-hero { margin: 0 16px 12px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 20px; padding: 18px; display: flex; align-items: center; gap: 16px; }
.v2 .detail-hero .big { width: 88px; height: 88px; border-radius: 24px; background: var(--rc); color: var(--rfg); display: flex; align-items: center; justify-content: center; font-size: 48px; font-weight: 900; letter-spacing: -0.03em; line-height: 1; flex-shrink: 0; }
.v2 .detail-hero .meta .l { font-size: 11px; font-weight: 800; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); }
.v2 .detail-hero .meta .lab { font-size: 20px; font-weight: 800; color: var(--rc); letter-spacing: -0.01em; margin-top: 2px; line-height: 1.2; }
.v2 .detail-hero .meta .desc { font-size: 12px; color: var(--fg-muted); margin-top: 2px; line-height: 1.4; }

.v2 .twocol { margin: 0 16px 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.v2 .twocol .c { background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 12px 14px; }
.v2 .twocol .c .l { font-size: 11px; font-weight: 800; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); }
.v2 .twocol .c .v { font-size: 18px; font-weight: 800; margin-top: 4px; }
.v2 .twocol .c .d { font-size: 11px; color: var(--fg-muted); margin-top: 1px; }

.v2 .notescard { margin: 0 16px 12px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 14px 16px; }
.v2 .notescard .l { font-size: 11px; font-weight: 800; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); margin-bottom: 4px; }
.v2 .notescard .v { font-size: 14px; line-height: 1.5; }

.v2 .foodcard { margin: 0 16px 12px; background: var(--surface-raised); border: 1px solid var(--border); border-radius: 14px; padding: 14px 16px; }
.v2 .foodcard .head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 4px; }
.v2 .foodcard .head .l { font-size: 11px; font-weight: 800; letter-spacing: 0.10em; text-transform: uppercase; color: var(--fg-muted); }
.v2 .foodcard .head .more { font-size: 11px; color: var(--accent-text); font-weight: 700; }
.v2 .foodcard .row { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-top: 1px solid var(--border-subtle); }
.v2 .foodcard .row:first-of-type { border-top: 0; }
.v2 .foodcard .row .t { width: 52px; font-size: 11px; color: var(--fg-muted); font-family: var(--font-mono); font-weight: 700; }
.v2 .foodcard .row .f { flex: 1; font-size: 13px; }
.v2 .foodcard .row .tag { font-size: 11px; font-weight: 800; color: var(--danger-text); background: rgba(239,68,68,0.14); padding: 3px 8px; border-radius: 999px; font-variant-numeric: tabular-nums; }
`;

window.V2 = V2;

// ── V2 home ─────────────────────────────────────────────────
function V2TabBar({ active = 'home', onChange }) {
  const tabs = [
    { id: 'home',     label: 'Home',     icon: 'clipboard-list' },
    { id: 'history',  label: 'History',  icon: 'calendar-days' },
    { id: 'settings', label: 'Settings', icon: 'settings-2' },
  ];
  // If a click handler is wired up (via prop or the prototype's __nav singleton),
  // make the tabs clickable; otherwise stay static.
  const click = onChange || (typeof window !== 'undefined' && window.__nav && window.__nav.setScreen
    ? (id) => { window.__nav.setSheet && window.__nav.setSheet(null); window.__nav.setScreen(id); }
    : null);
  return (
    <div className="tabbar">
      {tabs.map(t => {
        const props = click ? { onClick: () => click(t.id), style: { background:'transparent', cursor:'pointer' } } : {};
        const Tag = click ? 'button' : 'div';
        return (
          <Tag key={t.id} className={`tab ${active === t.id ? 'active' : ''}`} {...props}>
            <span className="pill"><I name={t.icon} size={20} stroke={2}/></span>
            <span>{t.label}</span>
          </Tag>
        );
      })}
    </div>
  );
}

function V2Home({ onOpenSheet, onSelectEntry } = {}) {
  return (
    <div className="d1 v2">
      <div className="topbar">
        <div>
          <h1>Home</h1>
          <p className="sub">Fri 30 Jan · 1 poop · rating 1</p>
        </div>
        <div className="iconbtn"><I name="search" size={20}/></div>
      </div>

      <div className="scroll" style={{ paddingBottom: 20 }}>
        <div className="daygrp">
          <div className="label"><span className="d">Today · Fri 30 Jan</span><span className="sum">3 entries</span></div>
          <div className="timeline">
            {ENTRIES_TODAY.map((e, i) => <D1TimeEntry key={i} e={e} onClick={() => onSelectEntry && onSelectEntry({ ...e, dateLabel: 'Fri 30 Jan' })}/>)}
          </div>
        </div>
        <div className="daygrp">
          <div className="label"><span className="d">Yesterday · Thu 29 Jan</span><span className="sum">5 entries</span></div>
          <div className="timeline">
            {ENTRIES_YESTERDAY.map((e, i) => <D1TimeEntry key={i} e={e} onClick={() => onSelectEntry && onSelectEntry({ ...e, dateLabel: 'Thu 29 Jan' })}/>)}
          </div>
        </div>
        <div className="daygrp">
          <div className="label"><span className="d">Wed 28 Jan</span><span className="sum">3 entries</span></div>
          <div className="timeline">
            <D1TimeEntry e={{ type:'poop', time:'14:20', bristol:6, plain:'Mushy', rating:3, notes:'Some pain' }} onClick={() => onSelectEntry && onSelectEntry({ type:'poop', time:'14:20', bristol:6, plain:'Mushy', rating:3, notes:'Some pain', dateLabel:'Wed 28 Jan' })}/>
            <D1TimeEntry e={{ type:'food', time:'12:30', items:'Chicken sandwich, crisps' }} onClick={() => onSelectEntry && onSelectEntry({ type:'food', time:'12:30', items:'Chicken sandwich, crisps', dateLabel:'Wed 28 Jan' })}/>
            <D1TimeEntry e={{ type:'poop', time:'07:30', bristol:6, plain:'Mushy', rating:1 }} onClick={() => onSelectEntry && onSelectEntry({ type:'poop', time:'07:30', bristol:6, plain:'Mushy', rating:1, dateLabel:'Wed 28 Jan' })}/>
          </div>
        </div>
      </div>

      {/* Bottom log bar — replaces FAB */}
      <div className="logbar">
        <button onClick={() => onOpenSheet && onOpenSheet('poop')}><span className="em">💩</span> Poop</button>
        <button onClick={() => onOpenSheet && onOpenSheet('food')}><span className="em">🍴</span> Food</button>
        <button onClick={() => onOpenSheet && onOpenSheet('note')}><span className="em">📝</span> Note</button>
      </div>

      <V2TabBar active="home"/>
    </div>
  );
}

// ── V2 history (Calendar / Trends sub-tabs) ────────────────
function V2HistoryCalendar() {
  const wds = ['M','T','W','T','F','S','S'];
  // Build full month grid for Jan 2026
  // Jan 1 2026 = Thursday. Weeks Mon-Sun. So week 1 starts Mon 29 Dec.
  // We'll show only days of Jan + leading/trailing blanks for shape.
  const firstWeekday = 3; // 0=Mon, Thu=3 (Jan 1 is Thu)
  const daysInJan = 31;
  // Build counts/ratings indexed by day-of-month (1..31) — invent a plausible distribution from RATING_30D
  // Map RATING_30D (last 30 days back from Jan 30) to actual days 1..30, today = 30
  const byDay = {};
  RATING_30D.forEach((v, i) => {
    const day = i + 1; // 30 entries → days 1..30
    byDay[day] = v;
  });
  const countByDay = {};
  FREQ_30D.forEach((v, i) => { countByDay[i + 1] = v; });

  // Build cells: blanks before, then 1..31, then trailing blanks to fill 6 rows of 7
  const totalCells = Math.ceil((firstWeekday + daysInJan) / 7) * 7;
  const cells = [];
  for (let i = 0; i < totalCells; i++) {
    const dayNum = i - firstWeekday + 1;
    if (dayNum < 1) cells.push({ kind: 'blank' });
    else if (dayNum > daysInJan) cells.push({ kind: 'future', day: dayNum - daysInJan });
    else {
      const r = byDay[dayNum];
      const count = countByDay[dayNum];
      const isFuture = dayNum > 30;
      cells.push({ kind: isFuture ? 'future' : (r != null ? 'rated' : 'empty'), day: dayNum, r, count });
    }
  }

  return (
    <>
      <div className="monthhead">
        <span className="name">January 2026</span>
        <div className="nav">
          <div className="b"><I name="chevron-left" size={18}/></div>
          <div className="b"><I name="chevron-right" size={18}/></div>
        </div>
      </div>

      <div className="calcard">
        <div className="wd">{wds.map((d,i) => <div key={i}>{d}</div>)}</div>
        <div className="grid">
          {cells.map((c, i) => {
            if (c.kind === 'blank') return <div key={i} className="calday" style={{ visibility:'hidden' }}/>;
            if (c.kind === 'future') return <div key={i} className="calday future"><span className="num">{c.day}</span></div>;
            if (c.kind === 'empty')  return <div key={i} className="calday empty"><span className="num">{c.day}</span></div>;
            const rc = RATING_COLORS[Math.round(c.r)];
            const isToday = c.day === 30;
            return (
              <div key={i} className={`calday rated ${isToday ? 'today' : ''}`} style={{ '--cbg': rc.bg, '--cfg': rc.fg }}>
                <span className="num">{c.day}</span>
                <span className="meta">{c.count > 0 ? `${c.count}×` : ''}</span>
              </div>
            );
          })}
        </div>
      </div>

      <div className="legend">
        <span style={{ marginRight: 'auto' }}>Worst rating per day</span>
        {[1,2,3,4,5].map(n => (
          <div key={n} style={{ display:'flex', alignItems:'center', gap:3 }}>
            <span className="sw" style={{ background: RATING_COLORS[n].bg }}/>
            <span style={{fontWeight:700, color:'var(--fg)'}}>{n}</span>
          </div>
        ))}
        <span style={{ display:'flex', alignItems:'center', gap:3, marginLeft: 4 }}>
          <span className="sw" style={{ background: 'var(--surface)' }}/>
          <span>none</span>
        </span>
      </div>
    </>
  );
}

function V2HistoryTrends() {
  const w = 348, h = 110, pad = 6;
  const pts = RATING_30D.map((v, i) => {
    if (v === null) return null;
    const x = pad + (i / (RATING_30D.length - 1)) * (w - pad * 2);
    const y = pad + (1 - (v - 1) / 4) * (h - pad * 2);
    return [x, y, v];
  });
  const segs = [];
  let cur = [];
  pts.forEach(p => { if (p === null) { if (cur.length) segs.push(cur); cur = []; } else cur.push(p); });
  if (cur.length) segs.push(cur);

  return (
    <>
      {/* Range */}
      <div className="rangepicker">
        {['7d','30d','90d','6mo','1y','All'].map((r,i) => (
          <button key={r} className={i===1 ? 'on' : ''}>{r}</button>
        ))}
      </div>

      {/* Blood rating */}
      <div className="chartcard">
        <div className="head">
          <span className="l">Blood rating</span>
          <span className="delta" style={{ color:'var(--success-text)' }}>↓ 0.6 vs prev 30d</span>
        </div>
        <div className="v">
          <span className="big">1.4</span><span className="unit">avg</span>
          <span style={{ marginLeft:'auto', fontSize:11, fontWeight:700, color: RATING_COLORS[1].bg }}>● 18 good</span>
          <span style={{ fontSize:11, fontWeight:700, color: RATING_COLORS[3].bg }}>● 4 mid</span>
          <span style={{ fontSize:11, fontWeight:700, color: RATING_COLORS[4].bg }}>● 2 bad</span>
        </div>
        <svg viewBox={`0 0 ${w} ${h+16}`} style={{ width:'100%', height:128, display:'block' }}>
          <defs>
            <linearGradient id="v2-rfill" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.30"/>
              <stop offset="100%" stopColor="var(--accent)" stopOpacity="0"/>
            </linearGradient>
          </defs>
          {[1,2,3,4,5].map(n => {
            const y = pad + (1 - (n-1)/4) * (h - pad*2);
            return <g key={n}>
              <line x1={pad} x2={w-pad} y1={y} y2={y} stroke="var(--border-subtle)" strokeWidth="1"/>
              <text x={w-pad+2} y={y+3} fontSize="9" fill="var(--fg-faint)" fontFamily="var(--font-mono)">{n}</text>
            </g>;
          })}
          {segs.map((seg, si) => {
            const line = seg.map((p, i) => (i ? 'L' : 'M') + p[0] + ' ' + p[1]).join(' ');
            const area = line + ` L${seg[seg.length-1][0]} ${h-pad} L${seg[0][0]} ${h-pad} Z`;
            return <g key={si}>
              <path d={area} fill="url(#v2-rfill)"/>
              <path d={line} fill="none" stroke="var(--accent)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
            </g>;
          })}
          {pts.map((p, i) => p && (
            <circle key={i} cx={p[0]} cy={p[1]} r="2.6" fill={RATING_COLORS[Math.round(p[2])].bg} stroke="var(--bg)" strokeWidth="1.5"/>
          ))}
          <text x={pad} y={h+12} fill="var(--fg-faint)" fontSize="9" fontFamily="var(--font-mono)">30d ago</text>
          <text x={w-pad} y={h+12} textAnchor="end" fill="var(--fg-faint)" fontSize="9" fontFamily="var(--font-mono)">today</text>
        </svg>
      </div>

      {/* Frequency */}
      <div className="chartcard">
        <div className="head">
          <span className="l">Poops per day</span>
          <span className="delta" style={{ color:'var(--fg-muted)' }}>avg 1.9 / day</span>
        </div>
        <div className="v" style={{ marginBottom: 6 }}>
          <span className="big">1.9</span><span className="unit">avg</span>
          <span style={{ marginLeft:'auto', fontSize:11, fontWeight:700, color:'var(--brand-orange)' }}>3 days ≥ 3 poops</span>
        </div>
        <div style={{ display:'flex', gap:2, alignItems:'flex-end', height:70 }}>
          {FREQ_30D.map((v, i) => (
            <div key={i} style={{
              flex:1, height:`${Math.max(2, v/5*100)}%`, borderRadius:2,
              background: v >= 3 ? 'var(--brand-orange)' : 'var(--accent)',
            }}/>
          ))}
        </div>
        <div style={{ marginTop:6, display:'flex', justifyContent:'space-between', fontSize:9, color:'var(--fg-faint)', fontFamily:'var(--font-mono)' }}>
          <span>30d ago</span><span>today</span>
        </div>
      </div>

      {/* Food correlation */}
      <div className="chartcard">
        <div className="head">
          <span className="l">Food suspects</span>
          <span className="delta" style={{ color:'var(--accent-text)' }}>see all 14 →</span>
        </div>
        <div style={{ fontSize:12, color:'var(--fg-muted)', margin:'4px 0 10px', lineHeight:1.45 }}>Foods eaten in the 24h before a rating ≥ 3, ranked by how much they nudged the rating up or down.</div>
        {FOOD_CORR.slice(0,4).map((f) => {
          const up = parseFloat(f.uplift.replace('−','-'));
          const isBad = up > 0;
          const wlen = Math.min(100, Math.abs(up) * 35);
          return (
            <div key={f.food} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderTop: '1px solid var(--border-subtle)' }}>
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ fontSize:13, fontWeight:700 }}>{f.food}</div>
                <div style={{ fontSize:11, color:'var(--fg-muted)' }}>{f.bad} of {f.days} times before a bad day</div>
              </div>
              <div style={{ width:80, position:'relative', height:8, background:'var(--surface-high)', borderRadius:4, overflow:'hidden' }}>
                <div style={{ position:'absolute', left: isBad ? '50%' : `${50-wlen/2}%`, top:0, bottom:0, width: `${wlen/2}%`, background: isBad ? 'var(--danger)' : 'var(--success-text)' }}/>
                <div style={{ position:'absolute', left:'50%', top:-1, bottom:-1, width:1, background:'var(--fg-muted)', opacity:0.4 }}/>
              </div>
              <div style={{ width:38, textAlign:'right', fontSize:13, fontWeight:800, color: isBad ? 'var(--danger-text)' : 'var(--success-text)', fontVariantNumeric:'tabular-nums' }}>{f.uplift}</div>
            </div>
          );
        })}
      </div>
    </>
  );
}

function V2History({ tab = 'calendar' }) {
  return (
    <div className="d1 v2">
      <div className="topbar">
        <div>
          <h1>History</h1>
          <p className="sub">{tab === 'calendar' ? '24 days logged this month' : 'Last 30 days · avg rating 1.4'}</p>
        </div>
      </div>
      <div className="subtabs">
        <button className={tab === 'calendar' ? 'on' : ''}><I name="calendar-days" size={15}/> Calendar</button>
        <button className={tab === 'trends' ? 'on' : ''}><I name="trending-up" size={15}/> Trends</button>
      </div>
      <div className="scroll" style={{ padding: 0 }}>
        {tab === 'calendar' ? <V2HistoryCalendar/> : <V2HistoryTrends/>}
      </div>
      <V2TabBar active="history"/>
    </div>
  );
}

// ── V2 entry detail (compact, D2-style hero) ───────────────
function V2Detail() {
  const c = RATING_COLORS[2];
  return (
    <div className="d1 v2">
      <div className="topbar">
        <div className="iconbtn"><I name="arrow-left" size={20}/></div>
        <div style={{ flex:1, textAlign:'center' }}>
          <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Entry</div>
          <div style={{ fontSize:13, fontWeight:700 }}>Thu 29 Jan · 08:45</div>
        </div>
        <div style={{ display:'flex', gap:6 }}>
          <div className="iconbtn"><I name="pencil" size={18}/></div>
          <div className="iconbtn"><I name="trash-2" size={18}/></div>
        </div>
      </div>

      <div className="scroll" style={{ padding: 0 }}>
        {/* Hero — big rating number + label */}
        <div className="detail-hero" style={{ '--rc': c.bg, '--rfg': c.fg }}>
          <div className="big">2</div>
          <div className="meta">
            <div className="l">Blood rating</div>
            <div className="lab">{c.label}</div>
            <div className="desc">More than a stripe — worth keeping an eye on.</div>
          </div>
        </div>

        {/* Time + type */}
        <div className="twocol">
          <div className="c">
            <div className="l">Time</div>
            <div className="v" style={{ fontVariantNumeric:'tabular-nums' }}>08:45</div>
            <div className="d">Thu 29 Jan 2026</div>
          </div>
          <div className="c">
            <div className="l">Stool</div>
            <div className="v">Bristol 6</div>
            <div className="d">Mushy · fluffy + ragged</div>
          </div>
        </div>

        {/* Notes */}
        <div className="notescard">
          <div className="l">Notes</div>
          <div className="v">A bit urgent. Felt some cramping just before. Bathroom for ~5 min.</div>
        </div>

        {/* Food 24h before */}
        <div className="foodcard">
          <div className="head">
            <span className="l">Food in the 24h before</span>
            <span className="more">Explain ↗</span>
          </div>
          {[
            { t:'21:00', f:'Steak pie, NY cookie', tag:'+1.8' },
            { t:'17:30', f:'S&P chicken, fried rice', tag:'+2.1' },
            { t:'13:00', f:'Bagels, butter. Banana' },
            { t:'09:00', f:'Coffee, eggs on toast' },
          ].map((r,i) => (
            <div key={i} className="row">
              <div className="t">{r.t}</div>
              <div className="f">{r.f}</div>
              {r.tag && <div className="tag">{r.tag}</div>}
            </div>
          ))}
        </div>

        {/* Other context (kept terse) */}
        <div className="notescard" style={{ marginBottom: 20 }}>
          <div className="l">Other</div>
          <div style={{ display:'flex', flexWrap:'wrap', gap:6, marginTop:4 }}>
            {['No meds missed','No caffeine','No alcohol'].map(t => (
              <span key={t} style={{ padding:'5px 10px', borderRadius:999, background:'var(--surface-high)', color:'var(--fg-muted)', fontSize:11, fontWeight:600 }}>{t}</span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { V2Home, V2History, V2HistoryCalendar, V2HistoryTrends, V2Detail, V2TabBar });
