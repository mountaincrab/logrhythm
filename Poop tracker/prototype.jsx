// ── Prototype app — wires screens together with state + tweaks ─

const { useState: useS, useEffect: useE } = React;

// ── Settings screen that drives the tweaks state ──────────────
function PrototypeSettings({ tweaks, setTweak, activeTab = 'settings' }) {
  const themes = [
    { id: 'deep-navy', name: 'Deep Navy', bg: '#0A1020', surf: '#1C2340', accent: '#4F7CFF' },
    { id: 'charcoal',  name: 'Charcoal',  bg: '#0A0A0A', surf: '#1E1E1E', accent: '#06B6D4' },
    { id: 'retro',     name: 'Retro',     bg: '#1A0B1E', surf: '#2E1438', accent: '#FF00CC' },
  ];

  return (
    <div className="d1 v2">
      <div className="topbar">
        <div>
          <h1>Settings</h1>
        </div>
      </div>

      <div className="scroll" style={{ padding: '8px 20px 20px' }}>
        {/* Account */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:'14px 16px', display:'flex', alignItems:'center', gap:12, marginBottom:18 }}>
          <div style={{ width:44, height:44, borderRadius:14, background:'var(--grad-accent)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:24 }}>🦀</div>
          <div style={{ flex:1, minWidth:0 }}>
            <div style={{ fontWeight:700, fontSize:15 }}>Chris</div>
            <div style={{ fontSize:12, color:'var(--fg-muted)' }}>Tracking since 2 Aug 2024 · 487 entries</div>
          </div>
          <I name="chevron-right" size={18} color="var(--fg-muted)"/>
        </div>

        {/* Stool type */}
        <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', margin:'4px 4px 8px' }}>Stool type system</div>
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:12, marginBottom:18 }}>
          {[
            { k:'bristol', l:'Bristol scale',  d:'Medical standard, 7 types with descriptions' },
            { k:'plain',   l:'My plain types', d:'Mushy, soft lumps, hard lumps…' },
            { k:'both',    l:'Both',           d:'Show Bristol number + your label side-by-side' },
          ].map((o,i) => {
            const on = tweaks.stoolType === o.k;
            return (
              <button key={o.k} onClick={() => setTweak('stoolType', o.k)} style={{
                width:'100%', display:'flex', alignItems:'center', gap:10,
                padding:'10px 4px', borderBottom: i<2 ? '1px solid var(--border-subtle)' : 'none',
                background:'transparent', textAlign:'left', cursor:'pointer'
              }}>
                <div style={{
                  width:20, height:20, borderRadius:100, border:`2px solid ${on ? 'var(--accent)' : 'var(--fg-faint)'}`,
                  display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0
                }}>
                  {on && <span style={{ width:10, height:10, borderRadius:100, background:'var(--accent)' }}/>}
                </div>
                <div style={{ flex:1, minWidth:0 }}>
                  <div style={{ fontWeight:600, fontSize:14, color:'var(--fg)' }}>{o.l}</div>
                  <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{o.d}</div>
                </div>
              </button>
            );
          })}
        </div>

        {/* Home layout */}
        <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', margin:'4px 4px 8px' }}>Home screen</div>
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:12, marginBottom:18, display:'grid', gridTemplateColumns:'1fr 1fr', gap:8 }}>
          {[
            { k:'timeline',  l:'Timeline',  d:'Day-grouped feed' },
            { k:'dashboard', l:'Dashboard', d:'Stats hero on top' },
          ].map(o => {
            const on = tweaks.homeLayout === o.k;
            return (
              <button key={o.k} onClick={() => setTweak('homeLayout', o.k)} style={{
                padding:'12px 14px', borderRadius:12, textAlign:'left',
                background: on ? 'var(--accent-soft)' : 'var(--surface)',
                border:'1px solid ' + (on ? 'var(--accent)' : 'var(--border)'),
                color: on ? 'var(--accent-text)' : 'var(--fg)',
                cursor:'pointer'
              }}>
                <div style={{ fontWeight:700, fontSize:14 }}>{o.l}</div>
                <div style={{ fontSize:11, color: on ? 'var(--accent-text)' : 'var(--fg-muted)', marginTop:2 }}>{o.d}</div>
              </button>
            );
          })}
        </div>

        {/* Theme */}
        <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', margin:'4px 4px 8px' }}>Theme</div>
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:12, marginBottom:18, display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8 }}>
          {themes.map(t => {
            const on = tweaks.theme === t.id;
            return (
              <button key={t.id} onClick={() => setTweak('theme', t.id)} style={{
                display:'flex', flexDirection:'column', alignItems:'center', gap:6,
                background:'transparent', padding:0, cursor:'pointer'
              }}>
                <div style={{ width:'100%', height:74, borderRadius:12, background:t.bg, position:'relative', border: on ? '2px solid var(--accent)' : '1px solid var(--border)', overflow:'hidden' }}>
                  <div style={{ position:'absolute', top:6, left:6, right:6, height:8, borderRadius:3, background:t.surf }}/>
                  <div style={{ position:'absolute', top:20, left:6, right:6, bottom:6, borderRadius:6, background:t.surf, opacity:.85 }}/>
                  <div style={{ position:'absolute', bottom:10, right:10, width:14, height:14, borderRadius:100, background:t.accent }}/>
                </div>
                <div style={{ fontSize:12, fontWeight:600, color: on ? 'var(--accent-text)' : 'var(--fg-muted)' }}>{t.name}</div>
              </button>
            );
          })}
        </div>

        {/* Reminders (static) */}
        <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', margin:'4px 4px 8px' }}>Reminders</div>
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, marginBottom:18 }}>
          {[
            { l:'Daily check-in', d:'09:00 · "anything to log?"', on:true },
            { l:'Evening review', d:'21:00 · summarise the day', on:false },
            { l:'Missed medication', d:'When the day ends with no med entry', on:true },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:10, padding:'12px 16px', borderBottom: i<2 ? '1px solid var(--border-subtle)' : 'none' }}>
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ fontWeight:600, fontSize:14 }}>{r.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{r.d}</div>
              </div>
              <div style={{ width:40, height:24, borderRadius:999, background: r.on ? 'var(--accent)' : 'var(--surface-high)', position:'relative' }}>
                <div style={{ position:'absolute', top:3, left: r.on ? 19 : 3, width:18, height:18, borderRadius:100, background:'#fff' }}/>
              </div>
            </div>
          ))}
        </div>

        {/* Data */}
        <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', margin:'4px 4px 8px' }}>Data</div>
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14 }}>
          {[
            { l:'Export to CSV', i:'download' },
            { l:'Import from spreadsheet', i:'upload' },
            { l:'Share with my clinician', i:'share-2' },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:10, padding:'14px 16px', borderBottom: i<2 ? '1px solid var(--border-subtle)' : 'none' }}>
              <I name={r.i} size={18} color="var(--fg-muted)"/>
              <div style={{ flex:1, fontWeight:600, fontSize:14 }}>{r.l}</div>
              <I name="chevron-right" size={18} color="var(--fg-muted)"/>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── App — routing + state ─────────────────────────────────────
const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "theme": "deep-navy",
  "stoolType": "bristol",
  "homeLayout": "timeline",
  "primaryChart": "rating"
}/*EDITMODE-END*/;

// Module-level nav singleton so the static V2TabBar can drive the
// prototype's screen state. PrototypeAppControlled wires its setters in.
const __nav = { setScreen: () => {}, setSheet: () => {} };
window.__nav = __nav;

function PrototypeAppControlled({ tweaks, setTweak }) {
  const [screen, setScreen] = useS('home');
  const [sheet, setSheet] = useS(null);
  const [selectedEntry, setSelectedEntry] = useS(null);
  const [historyTab, setHistoryTab] = useS('calendar');

  // Wire nav singleton every render so it picks up the latest setters
  __nav.setScreen = setScreen;
  __nav.setSheet  = setSheet;

  // Inject stylesheets for d1 + v2 once
  useE(() => {
    [['__d1styles', window.D1.styles], ['__v2styles', window.V2.styles]].forEach(([id, css]) => {
      if (!document.getElementById(id)) {
        const s = document.createElement('style');
        s.id = id;
        s.textContent = css;
        document.head.appendChild(s);
      }
    });
  }, []);

  // Cross-component nav from the Tweaks panel buttons
  useE(() => {
    const onOpen = (e) => { setSheet(e.detail || 'poop'); };
    const onGo   = (e) => { setSheet(null); setScreen(e.detail || 'home'); };
    window.addEventListener('proto:openSheet', onOpen);
    window.addEventListener('proto:goto', onGo);
    return () => {
      window.removeEventListener('proto:openSheet', onOpen);
      window.removeEventListener('proto:goto', onGo);
    };
  }, []);

  const openSheet = (kind) => setSheet(kind);
  const closeSheet = () => setSheet(null);
  const onSelectEntry = (e) => {
    setSelectedEntry(e);
    setSheet('detail');
  };

  // Tab navigation — clicking a tab resets sheets (handler exposed via __nav singleton)
  const goTo = (s) => { setSheet(null); setScreen(s); };

  // Compose the main screen
  let body;
  if (screen === 'home' && tweaks.homeLayout === 'timeline') {
    body = <V2Home onOpenSheet={openSheet} onSelectEntry={onSelectEntry}/>;
  } else if (screen === 'home' && tweaks.homeLayout === 'dashboard') {
    body = <V2HomeDashboard onOpenSheet={openSheet} onSelectEntry={onSelectEntry}/>;
  } else if (screen === 'history') {
    body = <V2HistoryInteractive tab={historyTab} setTab={setHistoryTab} primaryChart={tweaks.primaryChart}/>;
  } else if (screen === 'settings') {
    body = <PrototypeSettings tweaks={tweaks} setTweak={setTweak}/>;
  }

  // Sheet overlays
  let overlay = null;
  if (sheet === 'poop') overlay = <AddPoopSheet stoolMode={tweaks.stoolType} onClose={closeSheet} onSave={closeSheet}/>;
  else if (sheet === 'food') overlay = <AddFoodSheet onClose={closeSheet} onSave={closeSheet}/>;
  else if (sheet === 'note') overlay = <NoteSheet onClose={closeSheet}/>;
  else if (sheet === 'detail') overlay = <EntryDetailSheet entry={selectedEntry} onClose={closeSheet}/>;

  return (
    <Phone theme={tweaks.theme} time="09:42">
      {overlay ? overlay : body}
    </Phone>
  );
}

// Lightweight note sheet
function NoteSheet({ onClose }) {
  return (
    <div className="d1">
      <div className="sheet">
        <div className="sheethead">
          <button className="x" onClick={onClose}><I name="x" size={20}/></button>
          <h1>Quick note</h1>
        </div>
        <div className="body">
          <p style={{ margin:'4px 0 14px', color:'var(--fg-muted)', fontSize:13, lineHeight:1.5 }}>For meds, caffeine, alcohol, stress — anything that isn't a poop or a meal but might matter for the food-correlation graph.</p>
          <div className="field">
            <div className="lab"><span>What happened</span></div>
            <textarea className="textarea" style={{ minHeight:120 }} placeholder="e.g. Missed morning mesalazine. Or: two coffees today. Or: feeling stressed about the meeting."/>
          </div>
          <div className="field">
            <div className="lab"><span>Tag</span><span className="hint">optional</span></div>
            <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
              {[
                { l:'Medication', ic:'pill' },
                { l:'Caffeine',   ic:'coffee' },
                { l:'Alcohol',    ic:'wine' },
                { l:'Stress',     ic:'activity' },
                { l:'Travel',     ic:'plane' },
                { l:'Sleep',      ic:'moon' },
              ].map(t => (
                <span key={t.l} style={{
                  padding:'8px 12px', borderRadius:999, background:'var(--surface-raised)',
                  border:'1px solid var(--border)', fontSize:12, fontWeight:600,
                  display:'inline-flex', alignItems:'center', gap:6, color:'var(--fg-muted)'
                }}><I name={t.ic} size={13}/>{t.l}</span>
              ))}
            </div>
          </div>
        </div>
        <div className="savebar">
          <button className="b cancel" onClick={onClose}>Cancel</button>
          <button className="b save" onClick={onClose}>Save note</button>
        </div>
      </div>
    </div>
  );
}

// History with interactive sub-tabs + primary-chart-aware ordering
function V2HistoryInteractive({ tab, setTab, primaryChart }) {
  return (
    <div className="d1 v2">
      <div className="topbar">
        <div>
          <h1>History</h1>
          <p className="sub">{tab === 'calendar' ? '24 days logged this month' : 'Last 30 days · avg rating 1.4'}</p>
        </div>
      </div>
      <div className="subtabs">
        <button className={tab === 'calendar' ? 'on' : ''} onClick={() => setTab('calendar')} style={{cursor:'pointer'}}><I name="calendar-days" size={15}/> Calendar</button>
        <button className={tab === 'trends' ? 'on' : ''} onClick={() => setTab('trends')} style={{cursor:'pointer'}}><I name="trending-up" size={15}/> Trends</button>
      </div>
      <div className="scroll" style={{ padding: 0, paddingBottom: 16 }}>
        {tab === 'calendar' ? <V2HistoryCalendar/> : <V2TrendsInteractive primary={primaryChart}/>}
      </div>
      <V2TabBar active="history"/>
    </div>
  );
}

// Trends content with primary-chart ordering
function V2TrendsInteractive({ primary = 'rating' }) {
  const charts = ['rating', 'frequency', 'correlation'];
  const order = [primary, ...charts.filter(c => c !== primary)];

  return (
    <>
      {/* Range picker — wired with internal state */}
      <RangePicker/>
      {order.map(c => {
        if (c === 'rating')      return <RatingChartCard key={c} large={c === primary}/>;
        if (c === 'frequency')   return <FrequencyChartCard key={c} large={c === primary}/>;
        if (c === 'correlation') return <CorrelationCard key={c} large={c === primary}/>;
        return null;
      })}
    </>
  );
}

function RangePicker() {
  const [r, setR] = useS('30d');
  const ranges = ['7d','30d','90d','6mo','1y','All'];
  return (
    <div className="rangepicker">
      {ranges.map(x => (
        <button key={x} className={r === x ? 'on' : ''} onClick={() => setR(x)} style={{ cursor:'pointer' }}>{x}</button>
      ))}
    </div>
  );
}

function RatingChartCard({ large }) {
  const w = 348, h = large ? 130 : 90, pad = 6;
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
    <div className="chartcard">
      <div className="head">
        <span className="l">Blood rating</span>
        <span className="delta" style={{ color:'var(--success-text)' }}>↓ 0.6 vs prev 30d</span>
      </div>
      <div className="v">
        <span className="big" style={{ fontSize: large ? 32 : 24 }}>1.4</span><span className="unit">avg</span>
        <span style={{ marginLeft:'auto', fontSize:11, fontWeight:700, color: RATING_COLORS[1].bg }}>● 18 good</span>
        <span style={{ fontSize:11, fontWeight:700, color: RATING_COLORS[3].bg }}>● 4 mid</span>
        <span style={{ fontSize:11, fontWeight:700, color: RATING_COLORS[4].bg }}>● 2 bad</span>
      </div>
      <svg viewBox={`0 0 ${w} ${h+16}`} style={{ width:'100%', height: large ? 148 : 108, display:'block' }}>
        <defs>
          <linearGradient id="proto-r-fill" x1="0" x2="0" y1="0" y2="1">
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
            <path d={area} fill="url(#proto-r-fill)"/>
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
  );
}

function FrequencyChartCard({ large }) {
  return (
    <div className="chartcard">
      <div className="head">
        <span className="l">Poops per day</span>
        <span className="delta" style={{ color:'var(--fg-muted)' }}>avg 1.9 / day</span>
      </div>
      <div className="v" style={{ marginBottom: 6 }}>
        <span className="big" style={{ fontSize: large ? 32 : 24 }}>1.9</span><span className="unit">avg</span>
        <span style={{ marginLeft:'auto', fontSize:11, fontWeight:700, color:'var(--brand-orange)' }}>3 days ≥ 3 poops</span>
      </div>
      <div style={{ display:'flex', gap:2, alignItems:'flex-end', height: large ? 80 : 54 }}>
        {FREQ_30D.map((v, i) => (
          <div key={i} style={{ flex:1, height:`${Math.max(2, v/5*100)}%`, borderRadius:2, background: v >= 3 ? 'var(--brand-orange)' : 'var(--accent)' }}/>
        ))}
      </div>
      <div style={{ marginTop:6, display:'flex', justifyContent:'space-between', fontSize:9, color:'var(--fg-faint)', fontFamily:'var(--font-mono)' }}>
        <span>30d ago</span><span>today</span>
      </div>
    </div>
  );
}

function CorrelationCard({ large }) {
  return (
    <div className="chartcard">
      <div className="head">
        <span className="l">Food suspects</span>
        <span className="delta" style={{ color:'var(--accent-text)' }}>see all 14 →</span>
      </div>
      <div style={{ fontSize:12, color:'var(--fg-muted)', margin:'4px 0 10px', lineHeight:1.45 }}>Foods eaten in the 24h before a rating ≥ 3, ranked by how much they nudged the rating up or down.</div>
      {FOOD_CORR.slice(0, large ? 6 : 4).map((f) => {
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
  );
}

window.PrototypeAppControlled = PrototypeAppControlled;
window.TWEAK_DEFAULTS = TWEAK_DEFAULTS;
