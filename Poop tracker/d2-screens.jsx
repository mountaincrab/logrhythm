// ── Direction 2 screens ───────────────────────────────────────

function D2Spark({ values, color, height = 50, width = 260 }) {
  const pad = 4;
  const vs = values.map(v => v == null ? null : v);
  const maxRaw = Math.max(...values.filter(v => v != null), 5);
  const min = 1, max = Math.max(maxRaw, 5);
  const pts = vs.map((v, i) => {
    if (v == null) return null;
    const x = pad + (i / (vs.length - 1)) * (width - pad * 2);
    const y = pad + (1 - (v - min) / (max - min)) * (height - pad * 2);
    return [x, y, v];
  });
  const segs = [];
  let cur = [];
  pts.forEach(p => { if (p === null) { if (cur.length) segs.push(cur); cur = []; } else cur.push(p); });
  if (cur.length) segs.push(cur);

  // Build area path (uses first segment for area fill)
  return (
    <svg viewBox={`0 0 ${width} ${height}`} style={{ width:'100%', height, display:'block' }}>
      <defs>
        <linearGradient id="sparkfill" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.30"/>
          <stop offset="100%" stopColor={color} stopOpacity="0"/>
        </linearGradient>
      </defs>
      {segs.map((seg, si) => {
        const line = seg.map((p, i) => (i ? 'L' : 'M') + p[0] + ' ' + p[1]).join(' ');
        const area = line + ` L${seg[seg.length-1][0]} ${height-pad} L${seg[0][0]} ${height-pad} Z`;
        return (
          <g key={si}>
            <path d={area} fill="url(#sparkfill)"/>
            <path d={line} fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </g>
        );
      })}
      {pts.map((p, i) => p && (
        <circle key={i} cx={p[0]} cy={p[1]} r="2.6" fill={RATING_COLORS[Math.round(p[2])].bg} stroke="var(--bg)" strokeWidth="1.5"/>
      ))}
    </svg>
  );
}

function D2TabBar({ active = 'home' }) {
  const tabs = [
    { id: 'home',     label: 'Today',    icon: 'layout-dashboard' },
    { id: 'history',  label: 'History',  icon: 'calendar-days' },
    { id: 'trends',   label: 'Trends',   icon: 'line-chart' },
    { id: 'settings', label: 'You',      icon: 'user' },
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

function D2LogRow({ e }) {
  if (e.type === 'poop') {
    const c = RATING_COLORS[Math.round(e.rating)];
    return (
      <div className="logrow poop" style={{ '--rc': c.bg, '--rsoft': c.soft, '--rfg': c.fg }}>
        <div className="time">{e.time}</div>
        <div className="ic"><I name="circle-dot" size={16}/></div>
        <div className="body">
          <div className="t">Bristol {e.bristol} · {e.plain}</div>
          <div className="m">{e.notes || c.label.toLowerCase()}</div>
        </div>
        <div className="rating">{e.rating}</div>
      </div>
    );
  }
  if (e.type === 'food') {
    return (
      <div className="logrow food">
        <div className="time">{e.time}</div>
        <div className="ic"><I name="utensils" size={16}/></div>
        <div className="body">
          <div className="t">{e.items}</div>
        </div>
      </div>
    );
  }
  return (
    <div className="logrow med">
      <div className="time">{e.time}</div>
      <div className="ic"><I name="pill" size={16}/></div>
      <div className="body">
        <div className="t">{e.items}</div>
      </div>
    </div>
  );
}

// ── 01. Home dashboard ───────────────────────────────────────
function D2Home() {
  // 7-day rating spark
  const last7 = RATING_30D.slice(-7);
  return (
    <div className="d2">
      <div className="topbar">
        <div className="lockup">
          <div className="crab">🦀</div>
          <div>
            <h1>Morning, Chris</h1>
            <p className="sub">Fri 30 Jan · day 487</p>
          </div>
        </div>
        <div className="iconbtn"><I name="bell" size={18}/></div>
      </div>

      <div className="scroll">
        {/* Hero */}
        <div className="hero">
          <div className="head">
            <span className="lab">Today's rating</span>
            <span className="meta">↓ from 2 yesterday</span>
          </div>
          <div className="num">
            <span className="big" style={{ color: RATING_COLORS[1].bg }}>1</span>
            <span className="unit">no blood</span>
            <span className="pill" style={{ background:'rgba(16,185,129,0.16)', color:'var(--success-text)', marginLeft:'auto' }}>Good day</span>
          </div>
          <div className="spark">
            <D2Spark values={last7} color="var(--accent)" height={50} width={340}/>
            <div style={{ display:'flex', justifyContent:'space-between', fontSize:10, fontFamily:'var(--font-mono)', color:'var(--fg-faint)', marginTop:2 }}>
              <span>7d ago</span><span>today</span>
            </div>
          </div>
        </div>

        {/* Quick dock */}
        <div className="quickdock">
          <div className="qd primary">
            <div className="ic"><I name="circle-dot" size={20}/></div>
            <span className="l">Main action</span>
            <span className="t">Log poop</span>
          </div>
          <div className="qd secondary">
            <div className="ic"><I name="utensils" size={20}/></div>
            <span className="t" style={{fontSize:13}}>Food</span>
          </div>
          <div className="qd secondary">
            <div className="ic"><I name="pill" size={20}/></div>
            <span className="t" style={{fontSize:13}}>Note</span>
          </div>
        </div>

        {/* Today */}
        <div className="section">
          <div className="head"><h2>Today</h2><span className="all">3 entries</span></div>
          {ENTRIES_TODAY.map((e, i) => <D2LogRow key={i} e={e}/>)}
        </div>

        {/* Yesterday */}
        <div className="section">
          <div className="head"><h2>Yesterday · Thu 29 Jan</h2><span className="all">See all</span></div>
          {ENTRIES_YESTERDAY.map((e, i) => <D2LogRow key={i} e={e}/>)}
        </div>
      </div>

      <D2TabBar active="home"/>
    </div>
  );
}

// ── 02. Add poop ─────────────────────────────────────────────
function D2AddPoop() {
  const bristol = 6, rating = 1;
  const c = RATING_COLORS[rating];
  return (
    <div className="d2">
      <div className="sheet">
        <div className="sheethead">
          <div className="x"><I name="x" size={20}/></div>
          <h1>Log a poop</h1>
          <span style={{ fontSize:12, color:'var(--accent-text)', fontWeight:600 }}>Now</span>
        </div>
        <div className="body">
          <div className="field">
            <div className="lab">When</div>
            <div className="when2">
              <div className="seg" style={{flex:1}}>
                <I name="calendar" size={16} color="var(--fg-muted)"/>
                <span className="v">Today</span>
              </div>
              <div className="div"/>
              <div className="seg" style={{flex:1}}>
                <I name="clock" size={16} color="var(--fg-muted)"/>
                <span className="v">09:40</span>
              </div>
            </div>
          </div>

          <div className="field">
            <div className="lab">Type system</div>
            <div className="typeseg">
              <button className="on">Bristol scale</button>
              <button>My types</button>
            </div>
            <div className="bristolList">
              {BRISTOL.map(b => (
                <div key={b.n} className={`bristolRow ${b.n === bristol ? 'on' : ''}`}>
                  <span className="n">{b.n}</span>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div className="lab">{b.plain}</div>
                    <div className="desc">{b.desc}</div>
                  </div>
                  <span className="glyph">{b.glyph}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>Blood rating</span><span style={{textTransform:'none',letterSpacing:0,fontSize:11,fontWeight:600,color:'var(--accent-text)'}}>1–5</span></div>
            <div className="ratingrow">
              {[1,2,3,4,5].map(n => {
                const rc = RATING_COLORS[n];
                return (
                  <div key={n} className={`rp ${n === rating ? 'on' : ''}`} style={{ '--rc': rc.bg, '--rfg': rc.fg }}>
                    <span className="n">{n}</span>
                    <span className="dot"/>
                  </div>
                );
              })}
            </div>
            <div className="ratinghint" style={{ '--rc': c.bg }}><b>{c.label}</b> · what you'd see in the bowl</div>
          </div>

          <div className="field">
            <div className="lab"><span>Notes</span><span style={{textTransform:'none',letterSpacing:0,fontSize:11,color:'var(--fg-muted)'}}>optional</span></div>
            <textarea className="textarea" placeholder="Urgency, pain, anything you noticed…"/>
          </div>

          <div className="field">
            <div className="lab"><span>More</span><span style={{textTransform:'none',letterSpacing:0,fontSize:11,color:'var(--accent-text)'}}>+ add a flag</span></div>
            <div style={{ display:'flex', flexWrap:'wrap', gap:6 }}>
              {[
                { l:'Missed meds', ic:'pill' },
                { l:'Caffeine', ic:'coffee' },
                { l:'Alcohol', ic:'wine' },
                { l:'Stress', ic:'activity' },
                { l:'Travel', ic:'plane' },
              ].map(t => (
                <span key={t.l} style={{ padding:'8px 12px', borderRadius:999, background:'var(--surface-raised)', border:'1px dashed var(--border)', fontSize:12, color:'var(--fg-muted)', fontWeight:600, display:'inline-flex', alignItems:'center', gap:6 }}>
                  <I name={t.ic} size={13}/>{t.l}
                </span>
              ))}
            </div>
          </div>
        </div>
        <div className="savebar">
          <button className="b cancel">Cancel</button>
          <button className="b save">Save</button>
        </div>
      </div>
    </div>
  );
}

// ── 03. Add food ─────────────────────────────────────────────
function D2AddFood() {
  return (
    <div className="d2">
      <div className="sheet">
        <div className="sheethead">
          <div className="x"><I name="x" size={20}/></div>
          <h1>Log food</h1>
          <span style={{ fontSize:12, color:'var(--accent-text)', fontWeight:600 }}>Now</span>
        </div>
        <div className="body">
          <div className="field">
            <div className="lab">When</div>
            <div className="when2">
              <div className="seg" style={{flex:1}}>
                <I name="calendar" size={16} color="var(--fg-muted)"/>
                <span className="v">Today</span>
              </div>
              <div className="div"/>
              <div className="seg" style={{flex:1}}>
                <I name="clock" size={16} color="var(--fg-muted)"/>
                <span className="v">10:32</span>
              </div>
            </div>
          </div>

          <div className="field">
            <div className="lab">Meal</div>
            <div style={{ display:'flex', gap:6 }}>
              {[
                { l:'Breakfast', ic:'sunrise', on:true },
                { l:'Lunch', ic:'sun' },
                { l:'Dinner', ic:'moon-star' },
                { l:'Snack', ic:'cookie' },
                { l:'Drink', ic:'cup-soda' },
              ].map(m => (
                <button key={m.l} style={{
                  flex:1, padding:'10px 4px', borderRadius:12,
                  background: m.on ? 'var(--accent)' : 'var(--surface-raised)',
                  color: m.on ? 'var(--accent-fg)' : 'var(--fg-muted)',
                  border: '1px solid ' + (m.on ? 'var(--accent)' : 'var(--border)'),
                  display:'flex', flexDirection:'column', alignItems:'center', gap:4,
                  fontSize:11, fontWeight:700
                }}><I name={m.ic} size={16}/>{m.l}</button>
              ))}
            </div>
          </div>

          <div className="field">
            <div className="lab">What you ate</div>
            <textarea className="textarea" style={{minHeight:120}} defaultValue="Coffee, buttered toast, banana"/>
            <div style={{ marginTop:8, fontSize:11, color:'var(--fg-muted)' }}>Tip · be specific where it matters ("spicy chicken", not just "dinner"). The food-correlation chart works better with detail.</div>
          </div>

          <div className="field">
            <div className="lab"><span>Quick add</span><span style={{textTransform:'none',letterSpacing:0,fontSize:11,color:'var(--accent-text)'}}>tap to insert</span></div>
            <div style={{ display:'flex', flexWrap:'wrap', gap:6 }}>
              {['Banana','White rice','Plain toast','Buttered toast','Boiled chicken','Plain yoghurt','Apple','Coffee','Eggs','Banana on toast'].map(t => (
                <span key={t} style={{ padding:'8px 12px', borderRadius:999, background:'var(--surface-raised)', border:'1px solid var(--border)', fontSize:12, color:'var(--fg)', fontWeight:600 }}>+ {t}</span>
              ))}
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>Flags</span><span style={{textTransform:'none',letterSpacing:0,fontSize:11,color:'var(--fg-muted)'}}>rare</span></div>
            <div style={{ display:'flex', gap:6 }}>
              {[
                { l:'Caffeinated', ic:'coffee', on:true },
                { l:'Alcohol', ic:'wine' },
                { l:'Spicy', ic:'flame' },
                { l:'New food', ic:'sparkles' },
              ].map(t => (
                <button key={t.l} style={{
                  flex:1, padding:'10px 6px', borderRadius:12,
                  background: t.on ? 'var(--accent-soft)' : 'var(--surface-raised)',
                  color: t.on ? 'var(--accent-text)' : 'var(--fg-muted)',
                  border: '1px solid ' + (t.on ? 'var(--accent)' : 'var(--border)'),
                  display:'flex', flexDirection:'column', alignItems:'center', gap:4,
                  fontSize:11, fontWeight:700
                }}><I name={t.ic} size={16}/>{t.l}</button>
              ))}
            </div>
          </div>
        </div>
        <div className="savebar">
          <button className="b cancel">Cancel</button>
          <button className="b save">Save</button>
        </div>
      </div>
    </div>
  );
}

// ── 04. Trends ───────────────────────────────────────────────
function D2Trends() {
  const w = 348, h = 130, pad = 6;
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
    <div className="d2">
      <div className="topbar">
        <div className="lockup">
          <div>
            <h1 style={{fontSize:24}}>Trends</h1>
            <p className="sub">30 days · 24 poops · avg 1.4</p>
          </div>
        </div>
        <div className="iconbtn"><I name="sliders-horizontal" size={18}/></div>
      </div>

      <div className="scroll">
        {/* Range */}
        <div style={{ display:'flex', background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:12, padding:3, gap:0, marginBottom:12 }}>
          {['7d','30d','90d','1y','All'].map((r,i) => (
            <button key={r} style={{
              flex:1, padding:'8px 0', borderRadius:9, fontSize:12, fontWeight:700,
              background: i===1 ? 'var(--accent)' : 'transparent',
              color: i===1 ? 'var(--accent-fg)' : 'var(--fg-muted)'
            }}>{r}</button>
          ))}
        </div>

        {/* Hero: rating */}
        <div className="blockcard">
          <div style={{ display:'flex', alignItems:'baseline', justifyContent:'space-between', marginBottom:6 }}>
            <div>
              <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Blood rating</div>
              <div style={{ display:'flex', alignItems:'baseline', gap:8, marginTop:6 }}>
                <span style={{ fontSize:38, fontWeight:900, letterSpacing:'-0.02em' }}>1.4</span>
                <span style={{ fontSize:12, color:'var(--fg-muted)' }}>avg</span>
                <span style={{ fontSize:12, color:'var(--success-text)', fontWeight:700, marginLeft:8 }}>↓ 0.6</span>
              </div>
            </div>
            <div style={{ display:'flex', flexDirection:'column', alignItems:'flex-end', fontSize:11, color:'var(--fg-muted)', gap:2 }}>
              <span style={{ color: RATING_COLORS[1].bg, fontWeight:700 }}>● 18 good</span>
              <span style={{ color: RATING_COLORS[3].bg, fontWeight:700 }}>● 4 mid</span>
              <span style={{ color: RATING_COLORS[4].bg, fontWeight:700 }}>● 2 bad</span>
            </div>
          </div>
          <svg viewBox={`0 0 ${w} ${h+18}`} style={{ width:'100%', height:140, display:'block' }}>
            <defs>
              <linearGradient id="dr2-fill" x1="0" x2="0" y1="0" y2="1">
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
                <path d={area} fill="url(#dr2-fill)"/>
                <path d={line} fill="none" stroke="var(--accent)" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/>
              </g>;
            })}
            {pts.map((p, i) => p && (
              <circle key={i} cx={p[0]} cy={p[1]} r="2.6" fill={RATING_COLORS[Math.round(p[2])].bg} stroke="var(--bg)" strokeWidth="1.5"/>
            ))}
            <text x={pad} y={h+12} fill="var(--fg-faint)" fontSize="9" fontFamily="var(--font-mono)">30d</text>
            <text x={(w/2)} y={h+12} textAnchor="middle" fill="var(--fg-faint)" fontSize="9" fontFamily="var(--font-mono)">15d</text>
            <text x={w-pad} y={h+12} textAnchor="end" fill="var(--fg-faint)" fontSize="9" fontFamily="var(--font-mono)">today</text>
          </svg>
        </div>

        {/* Heatmap */}
        <div className="blockcard">
          <div className="lab">
            <span style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Calendar heatmap</span>
            <span className="more">last 30 days</span>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(10, 1fr)', gap:4, marginTop:8 }}>
            {RATING_30D.map((v, i) => (
              <div key={i} style={{
                aspectRatio:'1/1', borderRadius:6,
                background: v === null ? 'var(--surface-high)' : RATING_COLORS[Math.round(v)].bg,
                opacity: v === null ? 0.5 : 1,
                border: i === RATING_30D.length-1 ? '2px solid var(--fg)' : 'none'
              }}/>
            ))}
          </div>
          <div style={{ marginTop:8, display:'flex', alignItems:'center', justifyContent:'center', gap:4, fontSize:10, color:'var(--fg-muted)' }}>
            <span>1</span>
            {[1,2,3,4,5].map(n => <span key={n} style={{ width:16, height:8, background: RATING_COLORS[n].bg }}/>)}
            <span>5</span>
          </div>
        </div>

        {/* Frequency */}
        <div className="blockcard">
          <div className="lab">
            <span style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Poops per day</span>
            <span className="more">avg 1.9</span>
          </div>
          <div style={{ display:'flex', gap:2, alignItems:'flex-end', height:64, marginTop:8 }}>
            {FREQ_30D.map((v, i) => (
              <div key={i} style={{
                flex:1, height:`${Math.max(2, v/5*100)}%`, borderRadius:2,
                background: v >= 3 ? 'var(--brand-orange)' : 'var(--accent)',
              }}/>
            ))}
          </div>
          <div style={{ marginTop:6, display:'flex', justifyContent:'space-between', fontSize:10, color:'var(--fg-faint)', fontFamily:'var(--font-mono)' }}>
            <span>30d ago</span><span>today</span>
          </div>
        </div>

        {/* Food correlation */}
        <div className="blockcard">
          <div className="lab" style={{ marginBottom:6 }}>
            <span style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Food correlation</span>
            <span className="more">eaten in 24h before</span>
          </div>
          <div style={{ display:'flex', flexDirection:'column', gap:10, marginTop:4 }}>
            {FOOD_CORR.slice(0,5).map((f) => {
              const up = parseFloat(f.uplift.replace('−','-'));
              const isBad = up > 0;
              const w = Math.min(100, Math.abs(up) * 35);
              return (
                <div key={f.food} style={{ display:'flex', alignItems:'center', gap:10 }}>
                  <div style={{ flex:1, minWidth:0 }}>
                    <div style={{ fontSize:13, fontWeight:600 }}>{f.food}</div>
                    <div style={{ fontSize:11, color:'var(--fg-muted)' }}>{f.bad} of {f.days} times before a bad day</div>
                  </div>
                  <div style={{ width:90, position:'relative', height:8, background:'var(--surface-high)', borderRadius:4, overflow:'hidden' }}>
                    <div style={{ position:'absolute', left: isBad ? '50%' : `${50-w/2}%`, top:0, bottom:0, width: `${w/2}%`, background: isBad ? 'var(--danger)' : 'var(--success-text)' }}/>
                    <div style={{ position:'absolute', left:'50%', top:-1, bottom:-1, width:1, background:'var(--fg-muted)', opacity:0.4 }}/>
                  </div>
                  <div style={{ width:38, textAlign:'right', fontSize:12, fontWeight:800, color: isBad ? 'var(--danger-text)' : 'var(--success-text)', fontVariantNumeric:'tabular-nums' }}>{f.uplift}</div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
      <D2TabBar active="trends"/>
    </div>
  );
}

// ── 05. Detail ───────────────────────────────────────────────
function D2Detail() {
  const c = RATING_COLORS[2];
  return (
    <div className="d2">
      <div className="topbar">
        <div className="iconbtn"><I name="arrow-left" size={18}/></div>
        <div style={{ flex:1, textAlign:'center', fontSize:12, color:'var(--fg-muted)', fontWeight:600 }}>Thu 29 Jan · entry</div>
        <div style={{ display:'flex', gap:6 }}>
          <div className="iconbtn"><I name="pencil" size={16}/></div>
          <div className="iconbtn"><I name="trash-2" size={16}/></div>
        </div>
      </div>

      <div className="scroll">
        {/* Big rating hero */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:'22px 20px', marginBottom:12, display:'flex', alignItems:'center', gap:18 }}>
          <div style={{ width:96, height:96, borderRadius:28, background: c.bg, color: c.fg, display:'flex', alignItems:'center', justifyContent:'center', fontSize:54, fontWeight:900, letterSpacing:'-0.03em', lineHeight:1 }}>2</div>
          <div style={{ flex:1, minWidth:0 }}>
            <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Blood rating</div>
            <div style={{ fontSize:22, fontWeight:800, letterSpacing:'-0.01em', color: c.bg, marginTop:2 }}>{c.label}</div>
            <div style={{ fontSize:13, color:'var(--fg-muted)', marginTop:2 }}>A bit more than a stripe — worth noting</div>
          </div>
        </div>

        {/* Time + type */}
        <div className="blockcard">
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:16 }}>
            <div>
              <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Time</div>
              <div style={{ fontSize:24, fontWeight:800, fontVariantNumeric:'tabular-nums', marginTop:4 }}>08:45</div>
              <div style={{ fontSize:12, color:'var(--fg-muted)' }}>Thu 29 Jan 2026</div>
            </div>
            <div>
              <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Type</div>
              <div style={{ fontSize:24, fontWeight:800, marginTop:4 }}>Bristol 6</div>
              <div style={{ fontSize:12, color:'var(--fg-muted)' }}>Mushy · fluffy + ragged</div>
            </div>
          </div>
        </div>

        {/* Notes */}
        <div className="blockcard">
          <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:6 }}>Notes</div>
          <div style={{ fontSize:14, lineHeight:1.5 }}>A bit urgent. Felt some cramping just before. Bathroom for ~5 min.</div>
        </div>

        {/* Food before */}
        <div className="blockcard">
          <div style={{ display:'flex', alignItems:'baseline', justifyContent:'space-between', marginBottom:8 }}>
            <span style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Food in the 24h before</span>
            <span style={{ fontSize:11, color:'var(--accent-text)', fontWeight:600 }}>Explain ↗</span>
          </div>
          {[
            { t:'21:00', f:'Steak pie, NY cookie', tag:'+1.8' },
            { t:'17:30', f:'S&P chicken, fried rice', tag:'+2.1' },
            { t:'13:00', f:'Bagels, butter. Banana' },
            { t:'09:00', f:'Coffee, eggs on toast' },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <div style={{ width:46, fontSize:11, color:'var(--fg-muted)', fontFamily:'var(--font-mono)', fontWeight:700 }}>{r.t}</div>
              <div style={{ flex:1, fontSize:13 }}>{r.f}</div>
              {r.tag && <div style={{ fontSize:11, fontWeight:800, color:'var(--danger-text)', background:'rgba(239,68,68,0.14)', padding:'3px 8px', borderRadius:999, fontVariantNumeric:'tabular-nums' }}>{r.tag}</div>}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── 06. History ──────────────────────────────────────────────
function D2History() {
  const wds = ['M','T','W','T','F','S','S'];
  const ratings = {};
  RATING_30D.forEach((v, i) => { ratings[i] = v; });
  const startDay = 5;
  const days = [];
  for (let w=0; w<5; w++) {
    for (let d=0; d<7; d++) {
      const idx = w*7 + d;
      const day = startDay + idx;
      const month = day > 31 ? 'Feb' : 'Jan';
      const displayDay = day > 31 ? day - 31 : day;
      const dataIdx = idx + 5;
      const r = ratings[dataIdx];
      const isToday = (month === 'Jan' && displayDay === 30);
      const isFuture = (month === 'Feb' && displayDay >= 1) || (month === 'Jan' && displayDay > 30);
      days.push({ day: displayDay, month, r, isToday, isFuture });
    }
  }

  return (
    <div className="d2">
      <div className="topbar">
        <div className="lockup">
          <div>
            <h1 style={{fontSize:22}}>History</h1>
            <p className="sub">January 2026 · 24 days logged</p>
          </div>
        </div>
        <div style={{ display:'flex', gap:6 }}>
          <div className="iconbtn"><I name="chevron-left" size={18}/></div>
          <div className="iconbtn"><I name="chevron-right" size={18}/></div>
        </div>
      </div>

      <div className="scroll">
        {/* Stat strip */}
        <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8, marginBottom:12 }}>
          {[
            { l:'Good days', v:'18', c:'var(--success-text)' },
            { l:'Mid days', v:'4', c: RATING_COLORS[3].bg },
            { l:'Bad days', v:'2', c: RATING_COLORS[4].bg },
          ].map(s => (
            <div key={s.l} style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:'12px 10px' }}>
              <div style={{ fontSize:10, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>{s.l}</div>
              <div style={{ fontSize:24, fontWeight:900, color: s.c, letterSpacing:'-0.02em', marginTop:2 }}>{s.v}</div>
            </div>
          ))}
        </div>

        {/* Calendar card */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:18, padding:16, marginBottom:12 }}>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7, 1fr)', gap:6, marginBottom:8 }}>
            {wds.map((d,i) => <div key={i} style={{ textAlign:'center', fontSize:10, fontWeight:700, color:'var(--fg-faint)', letterSpacing:'0.05em' }}>{d}</div>)}
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7, 1fr)', gap:6 }}>
            {days.map((d, i) => {
              const cc = d.r != null ? RATING_COLORS[Math.round(d.r)].bg : (d.isFuture ? 'transparent' : 'var(--surface-high)');
              return (
                <div key={i} style={{
                  aspectRatio:'1/1', borderRadius:10, background: cc,
                  display:'flex', alignItems:'center', justifyContent:'center',
                  opacity: d.isFuture ? 0.25 : 1,
                  border: d.isToday ? '2.5px solid var(--accent)' : 'none',
                  position:'relative'
                }}>
                  <span style={{ fontSize:13, fontWeight:800, color: d.r != null ? RATING_COLORS[Math.round(d.r)].fg : 'var(--fg-muted)' }}>{d.day}</span>
                </div>
              );
            })}
          </div>
          <div style={{ marginTop:12, paddingTop:12, borderTop:'1px solid var(--border-subtle)', display:'flex', alignItems:'center', gap:8, fontSize:11, color:'var(--fg-muted)' }}>
            <span>worst rating</span>
            {[1,2,3,4,5].map(n => <span key={n} style={{ width:14, height:14, borderRadius:4, background: RATING_COLORS[n].bg }}/>)}
          </div>
        </div>

        {/* Worst day callout */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:14, display:'flex', alignItems:'center', gap:12, marginBottom:10 }}>
          <div style={{ width:42, height:42, borderRadius:12, background: RATING_COLORS[4].bg, color:'#fff', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:900, fontSize:18 }}>4</div>
          <div style={{ flex:1 }}>
            <div style={{ fontSize:13, fontWeight:700 }}>Worst day this month</div>
            <div style={{ fontSize:12, color:'var(--fg-muted)' }}>Fri 9 Jan · 5 poops · ate fried rice + beer the night before</div>
          </div>
          <I name="chevron-right" size={18} color="var(--fg-muted)"/>
        </div>
      </div>
      <D2TabBar active="history"/>
    </div>
  );
}

// ── 07. Settings ─────────────────────────────────────────────
function D2Settings() {
  const themes = [
    { id: 'deep-navy', name: 'Deep Navy', bg: '#0A1020', surf: '#1C2340', accent: '#4F7CFF' },
    { id: 'charcoal',  name: 'Charcoal',  bg: '#0A0A0A', surf: '#1E1E1E', accent: '#06B6D4', active: true },
    { id: 'retro',     name: 'Retro',     bg: '#1A0B1E', surf: '#2E1438', accent: '#FF00CC' },
  ];

  return (
    <div className="d2">
      <div className="topbar">
        <div className="lockup">
          <div>
            <h1 style={{fontSize:22}}>You</h1>
            <p className="sub">Settings, data, reminders</p>
          </div>
        </div>
      </div>

      <div className="scroll">
        {/* Profile card */}
        <div className="blockcard" style={{ display:'flex', alignItems:'center', gap:14 }}>
          <div style={{ width:54, height:54, borderRadius:18, background:'var(--grad-accent)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:30 }}>🦀</div>
          <div style={{ flex:1, minWidth:0 }}>
            <div style={{ fontSize:17, fontWeight:800 }}>Chris</div>
            <div style={{ fontSize:12, color:'var(--fg-muted)' }}>487 entries · since 2 Aug 2024</div>
          </div>
          <button style={{ padding:'8px 12px', borderRadius:10, fontSize:12, fontWeight:700, background:'var(--surface-high)', color:'var(--fg)' }}>Edit</button>
        </div>

        {/* Stool type */}
        <div className="blockcard">
          <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:8 }}>Stool type system</div>
          {[
            { l:'Bristol scale', d:'7 types with diagrams', on:true, ic:'list-ordered' },
            { l:'My plain types', d:'mushy, soft lumps…', ic:'pencil' },
            { l:'Both', d:'show side-by-side', ic:'split-square-horizontal' },
          ].map((o,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:'10px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <div style={{ width:32, height:32, borderRadius:10, background: o.on ? 'var(--accent-soft)' : 'var(--surface-high)', color: o.on ? 'var(--accent-text)' : 'var(--fg-muted)', display:'flex', alignItems:'center', justifyContent:'center' }}>
                <I name={o.ic} size={16}/>
              </div>
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ fontWeight:700, fontSize:14 }}>{o.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{o.d}</div>
              </div>
              <div style={{ width:20, height:20, borderRadius:100, border:`2px solid ${o.on ? 'var(--accent)' : 'var(--fg-faint)'}`, display:'flex', alignItems:'center', justifyContent:'center' }}>
                {o.on && <span style={{ width:10, height:10, borderRadius:100, background:'var(--accent)' }}/>}
              </div>
            </div>
          ))}
        </div>

        {/* Theme */}
        <div className="blockcard">
          <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:10 }}>Theme</div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8 }}>
            {themes.map(t => (
              <div key={t.id} style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
                <div style={{ width:'100%', height:80, borderRadius:14, background:t.bg, position:'relative', border: t.active ? '2.5px solid var(--accent)' : '1px solid var(--border)', overflow:'hidden' }}>
                  <div style={{ position:'absolute', top:8, left:8, right:8, height:10, borderRadius:4, background:t.surf }}/>
                  <div style={{ position:'absolute', top:24, left:8, right:8, bottom:8, borderRadius:8, background:t.surf, opacity:.85 }}/>
                  <div style={{ position:'absolute', bottom:14, right:14, width:16, height:16, borderRadius:100, background:t.accent }}/>
                </div>
                <div style={{ fontSize:12, fontWeight:700, color: t.active ? 'var(--accent-text)' : 'var(--fg-muted)' }}>{t.name}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Reminders */}
        <div className="blockcard">
          <div style={{ fontSize:11, fontWeight:700, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:10 }}>Reminders</div>
          {[
            { l:'Morning check-in', d:'09:00', on:true },
            { l:'Evening review', d:'21:00', on:false },
            { l:'Medication', d:'when day ends with no entry', on:true },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <div style={{ flex:1 }}>
                <div style={{ fontWeight:700, fontSize:14 }}>{r.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{r.d}</div>
              </div>
              <div style={{ width:40, height:24, borderRadius:999, background: r.on ? 'var(--accent)' : 'var(--surface-high)', position:'relative' }}>
                <div style={{ position:'absolute', top:3, left: r.on ? 19 : 3, width:18, height:18, borderRadius:100, background:'#fff' }}/>
              </div>
            </div>
          ))}
        </div>

        {/* Data */}
        <div className="blockcard" style={{ padding:0 }}>
          {[
            { l:'Export to CSV', d:'all 487 entries', i:'download' },
            { l:'Import spreadsheet', d:'pick up where the sheet left off', i:'upload' },
            { l:'Share with clinician', d:'one-time PDF or live link', i:'share-2' },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:'14px 16px', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <div style={{ width:32, height:32, borderRadius:10, background:'var(--accent-soft)', color:'var(--accent-text)', display:'flex', alignItems:'center', justifyContent:'center' }}>
                <I name={r.i} size={16}/>
              </div>
              <div style={{ flex:1 }}>
                <div style={{ fontWeight:700, fontSize:14 }}>{r.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{r.d}</div>
              </div>
              <I name="chevron-right" size={18} color="var(--fg-muted)"/>
            </div>
          ))}
        </div>
      </div>
      <D2TabBar active="settings"/>
    </div>
  );
}

Object.assign(window, { D2Home, D2AddPoop, D2AddFood, D2Trends, D2Detail, D2History, D2Settings });
