// ── Direction 1 screens ───────────────────────────────────────

// ── Building blocks ──
function D1RatingPill({ n, size = 'sm' }) {
  const c = RATING_COLORS[Math.round(n)] || RATING_COLORS[1];
  return (
    <span className="rpill" style={{ '--rating-c': c.bg, '--rating-soft': c.soft, '--rating-fg': c.fg }}>
      <span className="n">{n}</span> blood
    </span>
  );
}

function D1TimeEntry({ e, onClick }) {
  const cardProps = onClick ? { onClick, role: 'button', tabIndex: 0, style: { cursor: 'pointer' } } : {};
  if (e.type === 'poop') {
    const c = RATING_COLORS[Math.round(e.rating)];
    return (
      <div className="te poop" style={{ '--rating-c': c.bg }}>
        <span className="dot"/>
        <div className="card" {...cardProps}>
          <div className="row1">
            <span className="t">{e.time}</span>
            <span className="k" style={{ color: c.bg }}>· Poop</span>
          </div>
          <div className="body">Bristol <b>{e.bristol}</b> · {e.plain}{e.notes && <span style={{color:'var(--fg-muted)'}}> · {e.notes}</span>}</div>
          <div className="meta">
            <D1RatingPill n={e.rating}/>
          </div>
        </div>
      </div>
    );
  }
  if (e.type === 'food') {
    return (
      <div className="te food">
        <span className="dot"/>
        <div className="card" {...cardProps}>
          <div className="row1">
            <span className="t">{e.time}</span>
            <span className="k">· Food</span>
          </div>
          <div className="body">{e.items}</div>
        </div>
      </div>
    );
  }
  // med
  return (
    <div className="te med">
      <span className="dot" style={{ background: 'var(--warning)' }}/>
      <div className="card" {...cardProps}>
        <div className="row1">
          <span className="t">{e.time}</span>
          <span className="k" style={{ color: 'var(--warning)' }}>· Medication</span>
        </div>
        <div className="body muted">{e.items}</div>
      </div>
    </div>
  );
}

function D1TabBar({ active = 'home' }) {
  const tabs = [
    { id: 'home',     label: 'Today',    icon: 'clipboard-list' },
    { id: 'history',  label: 'History',  icon: 'calendar-days' },
    { id: 'trends',   label: 'Trends',   icon: 'trending-up' },
    { id: 'settings', label: 'Settings', icon: 'settings-2' },
  ];
  return (
    <div className="tabbar">
      {tabs.map(t => (
        <div key={t.id} className={`tab ${active === t.id ? 'active' : ''}`}>
          <span className="pill"><I name={t.icon} size={20} stroke={2}/></span>
          <span>{t.label}</span>
        </div>
      ))}
    </div>
  );
}

// ── 01. Home (timeline) ──────────────────────────────────────
function D1Home() {
  return (
    <div className="d1">
      <div className="topbar">
        <div>
          <h1>Today</h1>
          <p className="sub">Fri 30 Jan · 1 poop · rating 1</p>
        </div>
        <div className="iconbtn"><I name="search" size={20}/></div>
      </div>

      <div className="summary">
        <span className="crab">🦀</span>
        <div className="copy">
          <h2>Good morning, Chris.</h2>
          <p>Things look quiet so far. Tap the <b style={{color:'var(--accent-text)'}}>+</b> when there's news from down there.</p>
        </div>
      </div>

      <div className="quickbar">
        <button className="primary"><I name="circle-dot" size={14}/> Log poop</button>
        <button><I name="utensils" size={14}/> Log food</button>
        <button><I name="pill" size={14}/> Note</button>
      </div>

      <div className="scroll">
        <div className="daygrp">
          <div className="label"><span className="d">Today · Fri 30 Jan</span><span className="sum">3 entries</span></div>
          <div className="timeline">
            {ENTRIES_TODAY.map((e, i) => <D1TimeEntry key={i} e={e}/>)}
          </div>
        </div>
        <div className="daygrp">
          <div className="label"><span className="d">Yesterday · Thu 29 Jan</span><span className="sum">5 entries</span></div>
          <div className="timeline">
            {ENTRIES_YESTERDAY.map((e, i) => <D1TimeEntry key={i} e={e}/>)}
          </div>
        </div>
      </div>

      <button className="fab"><I name="plus" size={26} stroke={2.5} color="currentColor"/></button>
      <D1TabBar active="home"/>
    </div>
  );
}

// ── 02. Add poop ─────────────────────────────────────────────
function D1AddPoop({ mode = 'bristol' }) {
  const [stoolMode] = useState(mode);
  const [bristol] = useState(6);
  const [rating] = useState(1);
  const [showMore] = useState(false);
  const c = RATING_COLORS[rating];
  const bristolSel = BRISTOL.find(b => b.n === bristol);

  return (
    <div className="d1">
      <div className="sheet">
        <div className="sheethead">
          <div className="x"><I name="x" size={20}/></div>
          <h1>Log a poop</h1>
        </div>
        <div className="body">
          {/* When */}
          <div className="field">
            <div className="lab"><span>When</span><span className="hint">Now</span></div>
            <div className="when">
              <div className="pill"><I name="calendar" size={16} color="var(--fg-muted)"/><span className="v">Today</span></div>
              <div className="pill"><I name="clock" size={16} color="var(--fg-muted)"/><span className="v">09:40</span></div>
            </div>
          </div>

          {/* Type system toggle */}
          <div className="field">
            <div className="lab"><span>Type</span></div>
            <div className="typeseg" style={{ marginBottom: 10 }}>
              <button className={stoolMode === 'bristol' ? 'on' : ''}>Bristol scale</button>
              <button className={stoolMode === 'plain' ? 'on' : ''}>My types</button>
            </div>
            <div className="bristolgrid">
              {BRISTOL.map(b => (
                <div key={b.n} className={`b ${b.n === bristol ? 'on' : ''}`}>
                  <span className="n">{b.n}</span>
                  <span className="lab">{b.plain.split(' ')[0]}</span>
                </div>
              ))}
            </div>
            <p className="bristol-desc"><b>Type {bristolSel.n} · {bristolSel.plain}.</b> {bristolSel.desc}.</p>
          </div>

          {/* Rating */}
          <div className="field">
            <div className="lab"><span>Blood rating</span><span className="hint">1–5</span></div>
            <div className="ratingpills">
              {[1,2,3,4,5].map(n => {
                const rc = RATING_COLORS[n];
                return (
                  <button key={n} className={n === rating ? 'on' : ''} style={{ '--rc': rc.bg, '--rfg': rc.fg }}>
                    <span className="num">{n}</span>
                    <span className="swatch"/>
                  </button>
                );
              })}
            </div>
            <div className="rating-label" style={{ '--rc': c.bg }}>
              <I name="droplet" size={14} color={c.bg}/> <b>{c.label}</b> <span style={{color:'var(--fg-muted)'}}>· what you'd see if you looked</span>
            </div>
          </div>

          {/* Notes */}
          <div className="field">
            <div className="lab"><span>Notes</span><span className="hint">optional</span></div>
            <textarea className="textarea" placeholder="Urgency, pain, time of day, anything that felt different…" defaultValue=""/>
          </div>

          {/* More */}
          <div className="more-toggle">
            <I name="chevron-down" size={16}/> More — meds, caffeine, alcohol
          </div>
          {showMore && (
            <div className="more-grid">
              <div className="opt"><span className="ic"><I name="pill" size={16}/></span><div><div className="nm">Medication</div><div className="st">None today</div></div></div>
              <div className="opt"><span className="ic"><I name="coffee" size={16}/></span><div><div className="nm">Caffeine</div><div className="st">Not today</div></div></div>
              <div className="opt"><span className="ic"><I name="wine" size={16}/></span><div><div className="nm">Alcohol</div><div className="st">Not today</div></div></div>
            </div>
          )}
        </div>
        <div className="savebar">
          <button className="b cancel">Cancel</button>
          <button className="b save">Save poop</button>
        </div>
      </div>
    </div>
  );
}

// ── 03. Add food ─────────────────────────────────────────────
function D1AddFood() {
  return (
    <div className="d1">
      <div className="sheet">
        <div className="sheethead">
          <div className="x"><I name="x" size={20}/></div>
          <h1>Log food</h1>
        </div>
        <div className="body">
          <div className="field">
            <div className="lab"><span>When</span><span className="hint">Now</span></div>
            <div className="when">
              <div className="pill"><I name="calendar" size={16} color="var(--fg-muted)"/><span className="v">Today</span></div>
              <div className="pill"><I name="clock" size={16} color="var(--fg-muted)"/><span className="v">10:32</span></div>
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>What you ate</span></div>
            <textarea className="textarea" style={{minHeight:120}} defaultValue="Coffee, buttered toast, banana" placeholder="Free text — be specific where it matters (e.g. 'spicy chicken', 'whole milk')."/>
          </div>

          <div className="field">
            <div className="lab"><span>Recent</span><span className="hint">tap to add</span></div>
            <div style={{ display:'flex', flexWrap:'wrap', gap:6 }}>
              {['Banana','White rice','Plain toast','Buttered toast','Boiled chicken','Plain yoghurt','Apple','Coffee','Eggs'].map(t => (
                <span key={t} style={{ padding:'7px 12px', borderRadius:999, background:'var(--surface-raised)', border:'1px solid var(--border)', fontSize:12, color:'var(--fg-muted)'}}>{t}</span>
              ))}
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>Tag</span><span className="hint">optional</span></div>
            <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
              {[
                { l:'Breakfast', on:true, ic:'sunrise' },
                { l:'Lunch', ic:'sun' },
                { l:'Dinner', ic:'moon' },
                { l:'Snack', ic:'cookie' },
                { l:'Drink', ic:'cup-soda' },
              ].map(m => (
                <span key={m.l} style={{
                  padding:'8px 12px', borderRadius:12,
                  background: m.on ? 'var(--accent-soft)' : 'var(--surface-raised)',
                  border:'1px solid ' + (m.on ? 'var(--accent)' : 'var(--border)'),
                  color: m.on ? 'var(--accent-text)' : 'var(--fg-muted)',
                  fontSize:12, fontWeight:600, display:'inline-flex', alignItems:'center', gap:6
                }}><I name={m.ic} size={14}/>{m.l}</span>
              ))}
            </div>
          </div>
        </div>
        <div className="savebar">
          <button className="b cancel">Cancel</button>
          <button className="b save">Save food</button>
        </div>
      </div>
    </div>
  );
}

// ── 04. Trends ───────────────────────────────────────────────
function D1Trends() {
  const w = 372, h = 100, pad = 4;
  const pts = RATING_30D.map((v, i) => {
    if (v === null) return null;
    const x = pad + (i / (RATING_30D.length - 1)) * (w - pad * 2);
    const y = pad + (1 - (v - 1) / 4) * (h - pad * 2);
    return [x, y, v];
  });

  // Build line segments skipping nulls
  const segs = [];
  let cur = [];
  pts.forEach(p => {
    if (p === null) { if (cur.length) segs.push(cur); cur = []; }
    else cur.push(p);
  });
  if (cur.length) segs.push(cur);

  const heatmap = RATING_30D; // calendar heatmap, last 30 days
  const heatColor = (v) => {
    if (v === null) return 'var(--surface-high)';
    return RATING_COLORS[Math.round(v)].bg;
  };

  return (
    <div className="d1">
      <div className="topbar">
        <div>
          <h1>Trends</h1>
          <p className="sub">Last 30 days · 24 poops logged</p>
        </div>
        <div className="iconbtn"><I name="sliders-horizontal" size={20}/></div>
      </div>

      {/* Range selector */}
      <div style={{ padding:'4px 20px 0' }}>
        <div style={{ display:'inline-flex', background:'var(--surface)', borderRadius:12, padding:3, gap:0, border:'1px solid var(--border)'}}>
          {['7d','30d','90d','1y'].map((r,i) => (
            <button key={r} style={{
              padding:'7px 14px', borderRadius:9, fontSize:12, fontWeight:600,
              background: i===1 ? 'var(--surface-high)' : 'transparent',
              color: i===1 ? 'var(--fg)' : 'var(--fg-muted)'
            }}>{r}</button>
          ))}
        </div>
      </div>

      <div className="scroll" style={{ padding:'16px 0' }}>
        {/* Card: Rating over time */}
        <div className="blockcard" style={{ padding:'16px 18px' }}>
          <div className="lab">Blood rating over time</div>
          <div style={{ display:'flex', alignItems:'baseline', gap:8, marginBottom:8 }}>
            <span style={{ fontSize:28, fontWeight:800, letterSpacing:'-0.01em' }}>1.4</span>
            <span style={{ fontSize:12, color:'var(--success-text)', fontWeight:600 }}>↓ 0.6 vs prev 30d</span>
          </div>
          <svg viewBox={`0 0 ${w} ${h+20}`} style={{ width:'100%', height:120, display:'block' }}>
            {/* grid lines */}
            {[1,2,3,4,5].map(n => {
              const y = pad + (1 - (n-1)/4) * (h - pad*2);
              return <line key={n} x1={pad} x2={w-pad} y1={y} y2={y} stroke="var(--border-subtle)" strokeWidth="1"/>;
            })}
            {/* segments */}
            {segs.map((seg, si) => {
              const d = seg.map((p, i) => (i ? 'L' : 'M') + p[0] + ' ' + p[1]).join(' ');
              return <path key={si} d={d} fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>;
            })}
            {/* points colored by rating */}
            {pts.map((p, i) => p && (
              <circle key={i} cx={p[0]} cy={p[1]} r="2.4" fill={RATING_COLORS[Math.round(p[2])].bg}/>
            ))}
            {/* x labels */}
            <text x={pad} y={h+15} fill="var(--fg-faint)" fontSize="9" fontFamily="var(--font-mono)">30d ago</text>
            <text x={w-pad} y={h+15} textAnchor="end" fill="var(--fg-faint)" fontSize="9" fontFamily="var(--font-mono)">today</text>
          </svg>
        </div>

        {/* Card: Calendar heatmap */}
        <div className="blockcard" style={{ padding:'16px 18px' }}>
          <div className="lab" style={{ display:'flex', justifyContent:'space-between' }}><span>Calendar heatmap</span><span style={{textTransform:'none', letterSpacing:0, color:'var(--fg-faint)', fontWeight:500}}>worst rating per day</span></div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(15, 1fr)', gap:4 }}>
            {heatmap.map((v, i) => (
              <div key={i} style={{
                aspectRatio:'1/1', borderRadius:5, background: heatColor(v),
                opacity: v === null ? 0.5 : 1,
                border: i === heatmap.length-1 ? '1.5px solid var(--fg)' : 'none'
              }}/>
            ))}
          </div>
          <div style={{ marginTop:10, display:'flex', alignItems:'center', gap:6, fontSize:11, color:'var(--fg-muted)' }}>
            <span>none</span>
            {[1,2,3,4,5].map(n => (
              <span key={n} style={{ width:14, height:14, borderRadius:4, background: RATING_COLORS[n].bg }}/>
            ))}
            <span>worst</span>
          </div>
        </div>

        {/* Card: frequency */}
        <div className="blockcard" style={{ padding:'16px 18px' }}>
          <div className="lab">Poops per day</div>
          <div style={{ display:'flex', alignItems:'baseline', gap:8, marginBottom:10 }}>
            <span style={{ fontSize:28, fontWeight:800, letterSpacing:'-0.01em' }}>1.9</span>
            <span style={{ fontSize:12, color:'var(--fg-muted)', fontWeight:600 }}>avg / day</span>
          </div>
          <div style={{ display:'flex', gap:2, alignItems:'flex-end', height:60 }}>
            {FREQ_30D.map((v, i) => (
              <div key={i} style={{
                flex:1, height: `${Math.max(2, v/5*100)}%`, borderRadius:2,
                background: v >= 3 ? 'var(--brand-orange)' : 'var(--accent)',
                opacity: i === FREQ_30D.length-1 ? 1 : 0.7
              }}/>
            ))}
          </div>
        </div>

        {/* Card: food correlation */}
        <div className="blockcard" style={{ padding:'16px 18px' }}>
          <div className="lab" style={{ display:'flex', justifyContent:'space-between' }}>
            <span>Food correlation</span>
            <span style={{textTransform:'none', letterSpacing:0, color:'var(--fg-faint)', fontWeight:500}}>eaten in 24h before</span>
          </div>
          <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
            {FOOD_CORR.slice(0,5).map((f) => {
              const up = parseFloat(f.uplift.replace('−','-'));
              const isBad = up > 0;
              const w = Math.min(100, Math.abs(up) * 35);
              return (
                <div key={f.food} style={{ display:'flex', alignItems:'center', gap:10 }}>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ fontSize:13, fontWeight:600 }}>{f.food}</div>
                    <div style={{ fontSize:11, color:'var(--fg-muted)' }}>{f.bad} of {f.days} times preceded a bad day</div>
                  </div>
                  <div style={{ width:80, position:'relative', height:6, background:'var(--surface-high)', borderRadius:3, overflow:'hidden' }}>
                    <div style={{ position:'absolute', left: isBad ? '50%' : `${50-w/2}%`, top:0, bottom:0, width: `${w/2}%`, background: isBad ? 'var(--danger)' : 'var(--success-text)' }}/>
                    <div style={{ position:'absolute', left:'50%', top:-1, bottom:-1, width:1, background:'var(--fg-muted)', opacity:0.4 }}/>
                  </div>
                  <div style={{ width:36, textAlign:'right', fontSize:12, fontWeight:700, color: isBad ? 'var(--danger-text)' : 'var(--success-text)', fontVariantNumeric:'tabular-nums' }}>{f.uplift}</div>
                </div>
              );
            })}
          </div>
          <button style={{ width:'100%', padding:10, marginTop:10, borderRadius:10, fontSize:12, fontWeight:600, color:'var(--accent-text)', background:'transparent', border:'1px solid var(--border)' }}>See all 14 foods</button>
        </div>
      </div>
      <D1TabBar active="trends"/>
    </div>
  );
}

// ── 05. Entry detail ─────────────────────────────────────────
function D1Detail() {
  const c = RATING_COLORS[2];
  return (
    <div className="d1">
      <div className="topbar">
        <div className="iconbtn"><I name="arrow-left" size={20}/></div>
        <div style={{ flex:1 }}/>
        <div className="iconbtn"><I name="pencil" size={18}/></div>
        <div className="iconbtn"><I name="trash-2" size={18}/></div>
      </div>
      <div className="hero">
        <div className="big">08:45 · Mushy</div>
        <div className="day">Thursday, 29 January 2026</div>
      </div>

      <div className="scroll">
        <div className="blockcard" style={{ '--rc': c.bg, '--rfg': c.fg }}>
          <div className="lab">Blood rating</div>
          <div className="ratingrow">
            <div className="bigrating">2</div>
            <div>
              <div className="val" style={{ color: c.bg }}>{c.label}</div>
              <div className="val muted">A bit more than a stripe — worth noting</div>
            </div>
          </div>
        </div>

        <div className="blockcard">
          <div className="lab">Stool</div>
          <div className="val">Bristol 6 · Mushy</div>
          <div className="val muted" style={{ marginTop:2, fontSize:13 }}>Fluffy pieces with ragged edges</div>
        </div>

        <div className="blockcard">
          <div className="lab">Notes</div>
          <div className="val" style={{ fontWeight:400, fontSize:14, lineHeight:1.5 }}>A bit urgent. Felt some cramping just before. Bathroom for ~5 min.</div>
        </div>

        <div className="blockcard">
          <div className="lab">Food in the 24h before</div>
          <div style={{ display:'flex', flexDirection:'column', gap:8, marginTop:4 }}>
            {[
              { t:'Yesterday 21:00', f:'Steak pie (Satterthwaites), NY cookie', tag:'+1.8' },
              { t:'Yesterday 17:30', f:'Salt & pepper chicken, fried rice', tag:'+2.1' },
              { t:'Yesterday 13:00', f:'Bagels, butter. Banana' },
              { t:'Yesterday 09:00', f:'Coffee, eggs on toast' },
            ].map((r,i) => (
              <div key={i} style={{ display:'flex', alignItems:'baseline', gap:10, padding:'8px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
                <div style={{ width:88, fontSize:11, color:'var(--fg-muted)', fontFamily:'var(--font-mono)' }}>{r.t}</div>
                <div style={{ flex:1, fontSize:13 }}>{r.f}</div>
                {r.tag && <div style={{ fontSize:11, fontWeight:700, color:'var(--danger-text)', fontVariantNumeric:'tabular-nums' }}>{r.tag}</div>}
              </div>
            ))}
          </div>
        </div>

        <div className="blockcard">
          <div className="lab">Other context</div>
          <div style={{ display:'flex', flexWrap:'wrap', gap:6, marginTop:4 }}>
            {[
              { l:'No meds missed', on:false, ic:'pill', dim:true },
              { l:'No caffeine', on:false, ic:'coffee', dim:true },
              { l:'No alcohol', on:false, ic:'wine', dim:true },
            ].map(t => (
              <span key={t.l} style={{
                padding:'7px 11px', borderRadius:999,
                background:'var(--surface-high)', color:'var(--fg-muted)',
                fontSize:12, fontWeight:600, display:'inline-flex', alignItems:'center', gap:6
              }}><I name={t.ic} size={13}/>{t.l}</span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

// ── 06. History calendar ─────────────────────────────────────
function D1History() {
  // Build a 5-week calendar grid centered on today (30 Jan 2026)
  // Using a deterministic ratings map keyed by day-of-month for visual purposes
  const days = [];
  const ratings = {};
  RATING_30D.forEach((v, i) => { ratings[i] = v; });
  // weekday headers
  const wds = ['M','T','W','T','F','S','S'];

  // 5 weeks worth (35 cells). Start = Mon 5 Jan, end = Sun 8 Feb
  // Today = Fri 30 Jan (index 25 from Jan 5)
  const startDay = 5; // Jan 5
  for (let w=0; w<5; w++) {
    for (let d=0; d<7; d++) {
      const idx = w*7 + d;
      const day = startDay + idx;
      const month = day > 31 ? 'Feb' : 'Jan';
      const displayDay = day > 31 ? day - 31 : day;
      const dataIdx = idx + 5; // ratings start ~5 days into our 30-day series
      const r = ratings[dataIdx];
      const isToday = (month === 'Jan' && displayDay === 30);
      const isFuture = (month === 'Feb' && displayDay >= 1) || (month === 'Jan' && displayDay > 30);
      days.push({ day: displayDay, month, r, isToday, isFuture });
    }
  }

  return (
    <div className="d1">
      <div className="topbar">
        <div>
          <h1>History</h1>
          <p className="sub">January 2026</p>
        </div>
        <div style={{ display:'flex', gap:6 }}>
          <div className="iconbtn"><I name="chevron-left" size={20}/></div>
          <div className="iconbtn"><I name="chevron-right" size={20}/></div>
        </div>
      </div>

      <div className="scroll" style={{ padding:'8px 20px 16px' }}>
        {/* Calendar */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:16, padding:14 }}>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7, 1fr)', gap:6, marginBottom:8 }}>
            {wds.map((d,i) => <div key={i} style={{ textAlign:'center', fontSize:10, fontWeight:700, color:'var(--fg-faint)', letterSpacing:'0.05em' }}>{d}</div>)}
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7, 1fr)', gap:6 }}>
            {days.map((d, i) => {
              const c = d.r != null ? RATING_COLORS[Math.round(d.r)].bg : (d.isFuture ? 'transparent' : 'var(--surface-high)');
              return (
                <div key={i} style={{
                  aspectRatio:'1/1', borderRadius:10, background: c,
                  display:'flex', flexDirection:'column', alignItems:'center', justifyContent:'center', gap:0,
                  opacity: d.isFuture ? 0.3 : 1,
                  border: d.isToday ? '2px solid var(--fg)' : 'none',
                  position:'relative'
                }}>
                  <span style={{ fontSize:13, fontWeight:700, color: d.r != null ? (RATING_COLORS[Math.round(d.r)].fg) : 'var(--fg-muted)' }}>{d.day}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Legend */}
        <div style={{ marginTop:14, padding:14, background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14 }}>
          <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:8 }}>Worst rating per day</div>
          <div style={{ display:'flex', alignItems:'center', gap:8 }}>
            {[1,2,3,4,5].map(n => (
              <div key={n} style={{ display:'flex', alignItems:'center', gap:4 }}>
                <span style={{ width:14, height:14, borderRadius:4, background: RATING_COLORS[n].bg }}/>
                <span style={{ fontSize:11, color:'var(--fg-muted)', fontWeight:600 }}>{n}</span>
              </div>
            ))}
            <div style={{ display:'flex', alignItems:'center', gap:4, marginLeft:'auto' }}>
              <span style={{ width:14, height:14, borderRadius:4, background:'var(--surface-high)' }}/>
              <span style={{ fontSize:11, color:'var(--fg-muted)', fontWeight:600 }}>none logged</span>
            </div>
          </div>
        </div>

        {/* Streak block */}
        <div style={{ marginTop:14, display:'grid', gridTemplateColumns:'1fr 1fr', gap:10 }}>
          <div style={{ padding:14, background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14 }}>
            <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Current run</div>
            <div style={{ fontSize:28, fontWeight:800, letterSpacing:'-0.01em', marginTop:4 }}>14 <span style={{ fontSize:12, color:'var(--fg-muted)', fontWeight:600 }}>good days</span></div>
            <div style={{ fontSize:12, color:'var(--success-text)', marginTop:2, fontWeight:600 }}>Since 17 Jan</div>
          </div>
          <div style={{ padding:14, background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14 }}>
            <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Worst day</div>
            <div style={{ fontSize:28, fontWeight:800, letterSpacing:'-0.01em', marginTop:4 }}>9 Jan</div>
            <div style={{ fontSize:12, marginTop:2, fontWeight:600, color: RATING_COLORS[4].bg }}>Rating 4 · 5 poops</div>
          </div>
        </div>
      </div>
      <D1TabBar active="history"/>
    </div>
  );
}

// ── 07. Settings ─────────────────────────────────────────────
function D1Settings() {
  const themes = [
    { id: 'deep-navy', name: 'Deep Navy', bg: '#0A1020', surf: '#1C2340', accent: '#4F7CFF', active: true },
    { id: 'charcoal',  name: 'Charcoal',  bg: '#0A0A0A', surf: '#1E1E1E', accent: '#06B6D4' },
    { id: 'retro',     name: 'Retro',     bg: '#1A0B1E', surf: '#2E1438', accent: '#FF00CC' },
  ];

  return (
    <div className="d1">
      <div className="topbar">
        <div>
          <h1>Settings</h1>
        </div>
      </div>

      <div className="scroll" style={{ padding:'8px 20px 16px' }}>
        {/* Account */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:'14px 16px', display:'flex', alignItems:'center', gap:12, marginBottom:18 }}>
          <div style={{ width:44, height:44, borderRadius:14, background:'var(--grad-accent)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:24 }}>🦀</div>
          <div style={{ flex:1, minWidth:0 }}>
            <div style={{ fontWeight:700, fontSize:15 }}>Chris</div>
            <div style={{ fontSize:12, color:'var(--fg-muted)' }}>Tracking since 2 Aug 2024 · 487 entries</div>
          </div>
          <I name="chevron-right" size={18} color="var(--fg-muted)"/>
        </div>

        {/* Stool type system */}
        <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', margin:'4px 4px 8px' }}>Stool type system</div>
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:12, marginBottom:18 }}>
          {[
            { l:'Bristol scale', d:'Medical standard, 7 types with diagrams', on:true },
            { l:'My plain types', d:'Mushy, soft lumps, hard lumps…' },
            { l:'Both', d:'Show Bristol number + your label side-by-side' },
          ].map((o,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 4px', borderBottom: i<2 ? '1px solid var(--border-subtle)' : 'none' }}>
              <div style={{
                width:20, height:20, borderRadius:100, border:`2px solid ${o.on ? 'var(--accent)' : 'var(--fg-faint)'}`,
                display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0
              }}>
                {o.on && <span style={{ width:10, height:10, borderRadius:100, background:'var(--accent)' }}/>}
              </div>
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ fontWeight:600, fontSize:14 }}>{o.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{o.d}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Theme */}
        <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', margin:'4px 4px 8px' }}>Theme</div>
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:12, marginBottom:18, display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8 }}>
          {themes.map(t => (
            <div key={t.id} style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
              <div style={{ width:'100%', height:74, borderRadius:12, background:t.bg, position:'relative', border: t.active ? '2px solid var(--accent)' : '1px solid var(--border)', overflow:'hidden' }}>
                <div style={{ position:'absolute', top:6, left:6, right:6, height:8, borderRadius:3, background:t.surf }}/>
                <div style={{ position:'absolute', top:20, left:6, right:6, bottom:6, borderRadius:6, background:t.surf, opacity:.85 }}/>
                <div style={{ position:'absolute', bottom:10, right:10, width:14, height:14, borderRadius:100, background:t.accent }}/>
              </div>
              <div style={{ fontSize:12, fontWeight:600, color: t.active ? 'var(--accent-text)' : 'var(--fg-muted)' }}>{t.name}</div>
            </div>
          ))}
        </div>

        {/* Reminders */}
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
                <div style={{ position:'absolute', top:3, left: r.on ? 19 : 3, width:18, height:18, borderRadius:100, background:'#fff', transition:'left .15s' }}/>
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
      <D1TabBar active="settings"/>
    </div>
  );
}

Object.assign(window, { D1Home, D1AddPoop, D1AddFood, D1Trends, D1Detail, D1History, D1Settings });
