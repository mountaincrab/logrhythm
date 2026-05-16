// ── Direction 3 screens ───────────────────────────────────────

// Tiny abstract glyph for each Bristol type
function D3Shape({ n, color = 'currentColor' }) {
  const c = color;
  switch (n) {
    case 1: return (
      <svg viewBox="0 0 40 30" width="36" height="28">
        <circle cx="10" cy="15" r="4.5" fill={c}/>
        <circle cx="20" cy="15" r="4.5" fill={c}/>
        <circle cx="30" cy="15" r="4.5" fill={c}/>
      </svg>
    );
    case 2: return (
      <svg viewBox="0 0 40 30" width="36" height="28">
        <rect x="3" y="8" width="34" height="14" rx="7" fill={c}/>
        <circle cx="12" cy="15" r="6" fill={c}/>
        <circle cx="22" cy="15" r="6.5" fill={c}/>
        <circle cx="32" cy="15" r="5.5" fill={c}/>
      </svg>
    );
    case 3: return (
      <svg viewBox="0 0 40 30" width="36" height="28">
        <rect x="3" y="10" width="34" height="10" rx="5" fill={c}/>
        <rect x="11" y="10" width="1.5" height="10" fill="var(--accent-fg)" opacity="0.6"/>
        <rect x="21" y="10" width="1.5" height="10" fill="var(--accent-fg)" opacity="0.6"/>
        <rect x="29" y="10" width="1.5" height="10" fill="var(--accent-fg)" opacity="0.6"/>
      </svg>
    );
    case 4: return (
      <svg viewBox="0 0 40 30" width="36" height="28">
        <rect x="3" y="11" width="34" height="8" rx="4" fill={c}/>
      </svg>
    );
    case 5: return (
      <svg viewBox="0 0 40 30" width="36" height="28">
        <ellipse cx="10" cy="15" rx="6" ry="5" fill={c}/>
        <ellipse cx="22" cy="14" rx="7" ry="6" fill={c}/>
        <ellipse cx="33" cy="16" rx="5" ry="4.5" fill={c}/>
      </svg>
    );
    case 6: return (
      <svg viewBox="0 0 40 30" width="36" height="28">
        <path d="M3 17 Q7 11 12 16 T22 17 T32 16 T38 14 L38 22 Q34 24 30 21 T20 22 T10 21 T3 22 Z" fill={c}/>
        <circle cx="8" cy="11" r="2" fill={c}/>
        <circle cx="16" cy="9" r="1.5" fill={c}/>
        <circle cx="26" cy="11" r="2" fill={c}/>
        <circle cx="34" cy="9" r="1.5" fill={c}/>
      </svg>
    );
    case 7: return (
      <svg viewBox="0 0 40 30" width="36" height="28">
        <path d="M5 22 Q10 14 18 16 Q26 8 32 14 Q40 12 36 22 Q26 26 18 24 Q10 26 5 22 Z" fill={c}/>
      </svg>
    );
    default: return null;
  }
}

function D3TabBar({ active = 'home' }) {
  const tabs = [
    { id: 'home',     label: 'Today',    icon: 'home' },
    { id: 'history',  label: 'Past',     icon: 'calendar' },
    { id: 'trends',   label: 'Trends',   icon: 'trending-up' },
    { id: 'settings', label: 'You',      icon: 'user-circle-2' },
  ];
  return (
    <div className="tabbar">
      {tabs.map(t => (
        <div key={t.id} className={`tab ${active === t.id ? 'active' : ''}`}>
          <span className="pill"><I name={t.icon} size={20}/></span>
          <span>{t.label}</span>
        </div>
      ))}
    </div>
  );
}

function D3Entry({ e }) {
  if (e.type === 'poop') {
    const c = RATING_COLORS[Math.round(e.rating)];
    return (
      <div className="entry poop" style={{ '--rc': c.bg, '--rsoft': c.soft, '--rfg': c.fg }}>
        <span className="stamp">💩</span>
        <div className="body">
          <div className="t">Bristol {e.bristol} · {e.plain} <span className="tm">{e.time}</span></div>
          <div className="m">{e.notes || c.label}</div>
        </div>
        <span className="rating">●{e.rating}</span>
      </div>
    );
  }
  if (e.type === 'food') {
    return (
      <div className="entry food">
        <span className="stamp">🍴</span>
        <div className="body">
          <div className="t">{e.items.split(',')[0]}{e.items.split(',').length > 1 && ' …'} <span className="tm">{e.time}</span></div>
          <div className="m">{e.items}</div>
        </div>
      </div>
    );
  }
  return (
    <div className="entry med">
      <span className="stamp">💊</span>
      <div className="body">
        <div className="t">Medication <span className="tm">{e.time}</span></div>
        <div className="m">{e.items}</div>
      </div>
    </div>
  );
}

// ── 01. Home ─────────────────────────────────────────────────
function D3Home() {
  return (
    <div className="d3">
      <div className="topbar">
        <div style={{ flex:1 }}>
          <h1>Hey, Chris.</h1>
          <p className="sub">Fri 30 Jan · let's see how you're doing</p>
        </div>
        <div className="iconbtn"><I name="bell" size={18}/></div>
      </div>

      <div className="scroll">
        {/* Mascot greeting bubble */}
        <div className="greeting">
          <span className="tail"/>
          <span className="crab">🦀</span>
          <div className="bubble">
            <b>One poop, no blood</b> — solid start. The mushy <b>Bristol 6</b> is the only thing to keep an eye on. Yesterday's late pie might want a word.
          </div>
        </div>

        {/* Big logging dock */}
        <div className="logdock">
          <div className="logcard primary">
            <span className="em">💩</span>
            <div className="head">Main thing</div>
            <div className="body">Log a poop</div>
            <div className="cta">Tap to start <I name="arrow-right" size={14}/></div>
          </div>
          <div className="logcard secondary">
            <span className="em">🍴</span>
            <div className="head">Add</div>
            <div className="body">Food</div>
          </div>
          <div className="logcard secondary">
            <span className="em">📝</span>
            <div className="head">Add</div>
            <div className="body">Note</div>
          </div>
        </div>

        {/* Today */}
        <div className="secthead">
          <h2>Today</h2>
          <span className="meta">{ENTRIES_TODAY.length} entries · ends 23:59</span>
        </div>
        {ENTRIES_TODAY.map((e, i) => <D3Entry key={i} e={e}/>)}

        {/* Yesterday */}
        <div className="secthead" style={{ marginTop: 16 }}>
          <h2>Yesterday</h2>
          <span className="meta">Thu 29 Jan</span>
        </div>
        {ENTRIES_YESTERDAY.map((e, i) => <D3Entry key={i} e={e}/>)}
      </div>

      <D3TabBar active="home"/>
    </div>
  );
}

// ── 02. Add poop ─────────────────────────────────────────────
function D3AddPoop() {
  const bristol = 6, rating = 1;
  const c = RATING_COLORS[rating];
  const b = BRISTOL.find(x => x.n === bristol);

  return (
    <div className="d3">
      <div className="sheet">
        <div className="sheethead">
          <div className="x"><I name="x" size={20}/></div>
          <h1>One went down 🦀</h1>
        </div>
        <div className="body">
          <p className="helper">Fill what you can, skip what you can't. <b>Type</b> and <b>rating</b> are the two that matter for spotting flare-ups.</p>

          <div className="field">
            <div className="lab"><span>When did it happen?</span><span className="right">Now</span></div>
            <div className="pillrow">
              <div className="pill"><I name="calendar" size={16} color="var(--fg-muted)"/><div><div className="v">Today</div><div className="l">Fri 30 Jan</div></div></div>
              <div className="pill"><I name="clock" size={16} color="var(--fg-muted)"/><div><div className="v">09:40</div><div className="l">12 min ago</div></div></div>
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>What did it look like?</span></div>
            <div className="typeseg">
              <button className="on">Bristol 1–7</button>
              <button>My types</button>
            </div>
            <div className="shapegrid">
              {BRISTOL.map(bb => (
                <div key={bb.n} className={`shapecell ${bb.n === bristol ? 'on' : ''}`}>
                  <div className="ill">
                    <D3Shape n={bb.n} color={bb.n === bristol ? 'var(--accent-text)' : 'var(--fg-muted)'}/>
                  </div>
                  <span className="n">{bb.n}</span>
                  <span className="lab">{bb.plain.split(' ')[0]}</span>
                </div>
              ))}
            </div>
            <div className="pickedHelper">
              <b>Type {b.n} · {b.plain}</b> — {b.desc.toLowerCase()}. Common during a flare; not great news but not alarming on its own.
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>Was there blood?</span><span className="right">1 = none · 5 = loads</span></div>
            <div className="ratingrow">
              {[1,2,3,4,5].map(n => {
                const rc = RATING_COLORS[n];
                return (
                  <div key={n} className={`rp ${n === rating ? 'on' : ''}`} style={{ '--rc': rc.bg, '--rfg': rc.fg }}>
                    <span className="n">{n}</span>
                    <span className="drops">
                      {Array.from({length:n}).map((_,i) => <span key={i}/>)}
                    </span>
                  </div>
                );
              })}
            </div>
            <div className="ratinghint" style={{ '--rc': c.bg, '--rfg': c.fg }}>
              <span className="pip">{rating}</span>
              <div>
                <div className="t">{c.label}</div>
                <div className="d">All clear — no flare signals here. </div>
              </div>
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>Anything else?</span><span className="right" style={{color:'var(--fg-muted)'}}>optional</span></div>
            <textarea className="textarea" placeholder="Urgency, cramping, what time you noticed — your future self will thank you for it."/>
          </div>

          <div className="field">
            <div className="lab"><span>One-off flags</span></div>
            <div style={{ display:'flex', flexWrap:'wrap', gap:6 }}>
              {[
                { l:'Missed meds', em:'💊' },
                { l:'Had coffee', em:'☕' },
                { l:'Had a drink', em:'🍷' },
                { l:'Spicy meal', em:'🌶️' },
                { l:'Stressful day', em:'😬' },
              ].map(t => (
                <span key={t.l} style={{
                  padding:'9px 12px', borderRadius:999, background:'var(--surface-raised)',
                  border:'1px dashed var(--border-strong)', fontSize:12, fontWeight:700,
                  display:'inline-flex', alignItems:'center', gap:6
                }}><span>{t.em}</span> {t.l}</span>
              ))}
            </div>
          </div>
        </div>
        <div className="savebar">
          <button className="b cancel">Cancel</button>
          <button className="b save">Save it</button>
        </div>
      </div>
    </div>
  );
}

// ── 03. Add food ─────────────────────────────────────────────
function D3AddFood() {
  return (
    <div className="d3">
      <div className="sheet">
        <div className="sheethead">
          <div className="x"><I name="x" size={20}/></div>
          <h1>What's on the menu? 🍴</h1>
        </div>
        <div className="body">
          <p className="helper">Free text is fine. <b>Be specific</b> about anything new or unusual — that's where the food-correlation chart earns its keep.</p>

          <div className="field">
            <div className="lab"><span>When</span><span className="right">Now</span></div>
            <div className="pillrow">
              <div className="pill"><I name="calendar" size={16} color="var(--fg-muted)"/><div><div className="v">Today</div><div className="l">Fri 30 Jan</div></div></div>
              <div className="pill"><I name="clock" size={16} color="var(--fg-muted)"/><div><div className="v">10:32</div><div className="l">just now</div></div></div>
            </div>
          </div>

          <div className="field">
            <div className="lab">Which meal?</div>
            <div style={{ display:'flex', gap:6 }}>
              {[
                { l:'Breakfast', em:'🥣', on:true },
                { l:'Lunch', em:'🥪' },
                { l:'Dinner', em:'🍝' },
                { l:'Snack', em:'🍪' },
                { l:'Drink', em:'🥤' },
              ].map(m => (
                <button key={m.l} style={{
                  flex:1, padding:'12px 6px', borderRadius:16,
                  background: m.on ? 'var(--accent)' : 'var(--surface-raised)',
                  color: m.on ? 'var(--accent-fg)' : 'var(--fg-muted)',
                  border: '1px solid ' + (m.on ? 'var(--accent)' : 'var(--border)'),
                  display:'flex', flexDirection:'column', alignItems:'center', gap:4,
                  fontSize:11, fontWeight:800
                }}><span style={{fontSize:18}}>{m.em}</span>{m.l}</button>
              ))}
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>What you ate</span></div>
            <textarea className="textarea" style={{minHeight:130}} defaultValue="Coffee, buttered toast, banana"/>
          </div>

          <div className="field">
            <div className="lab"><span>From your usual</span><span className="right">tap to add</span></div>
            <div style={{ display:'flex', flexWrap:'wrap', gap:6 }}>
              {[
                { l:'Banana', em:'🍌' },
                { l:'White rice', em:'🍚' },
                { l:'Plain toast', em:'🍞' },
                { l:'Eggs', em:'🥚' },
                { l:'Plain yoghurt', em:'🥄' },
                { l:'Apple', em:'🍎' },
                { l:'Coffee', em:'☕' },
                { l:'Chicken', em:'🍗' },
              ].map(t => (
                <span key={t.l} style={{
                  padding:'8px 12px', borderRadius:999, background:'var(--surface-raised)',
                  border:'1px solid var(--border)', fontSize:12, fontWeight:700,
                  display:'inline-flex', alignItems:'center', gap:6
                }}><span>{t.em}</span>{t.l}</span>
              ))}
            </div>
          </div>
        </div>
        <div className="savebar">
          <button className="b cancel">Cancel</button>
          <button className="b save">Save it</button>
        </div>
      </div>
    </div>
  );
}

// ── 04. Trends ───────────────────────────────────────────────
function D3Trends() {
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
    <div className="d3">
      <div className="topbar">
        <div style={{ flex:1 }}>
          <h1>Trends</h1>
          <p className="sub">A picture's worth 1,000 wipes 🦀</p>
        </div>
        <div className="iconbtn"><I name="sliders-horizontal" size={18}/></div>
      </div>

      <div className="scroll">
        <div style={{ display:'flex', background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:4, gap:0, marginBottom:14 }}>
          {['7d','30d','90d','All'].map((r,i) => (
            <button key={r} style={{
              flex:1, padding:'9px 0', borderRadius:10, fontSize:12, fontWeight:800,
              background: i===1 ? 'var(--accent)' : 'transparent',
              color: i===1 ? 'var(--accent-fg)' : 'var(--fg-muted)'
            }}>{r}</button>
          ))}
        </div>

        {/* Hero rating */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:18, marginBottom:12 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:6 }}>
            <span style={{ fontSize:22 }}>🩸</span>
            <span style={{ fontSize:13, fontWeight:800, letterSpacing:'0.05em', textTransform:'uppercase' }}>Blood rating</span>
          </div>
          <div style={{ display:'flex', alignItems:'baseline', gap:10, marginBottom:6 }}>
            <span style={{ fontSize:48, fontWeight:900, letterSpacing:'-0.03em', lineHeight:1 }}>1.4</span>
            <span style={{ fontSize:12, color:'var(--fg-muted)', fontWeight:600 }}>avg / 30d</span>
            <span style={{ marginLeft:'auto', fontSize:12, fontWeight:800, color:'var(--success-text)' }}>↓ 0.6 better</span>
          </div>
          <svg viewBox={`0 0 ${w} ${h+16}`} style={{ width:'100%', height:140, display:'block', marginTop:6 }}>
            <defs>
              <linearGradient id="d3-fill" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.36"/>
                <stop offset="100%" stopColor="var(--accent)" stopOpacity="0"/>
              </linearGradient>
            </defs>
            {[1,2,3,4,5].map(n => {
              const y = pad + (1 - (n-1)/4) * (h - pad*2);
              return <line key={n} x1={pad} x2={w-pad} y1={y} y2={y} stroke="var(--border-subtle)" strokeWidth="1" strokeDasharray={n===1?'':'2 4'}/>;
            })}
            {segs.map((seg, si) => {
              const line = seg.map((p, i) => (i ? 'L' : 'M') + p[0] + ' ' + p[1]).join(' ');
              const area = line + ` L${seg[seg.length-1][0]} ${h-pad} L${seg[0][0]} ${h-pad} Z`;
              return <g key={si}>
                <path d={area} fill="url(#d3-fill)"/>
                <path d={line} fill="none" stroke="var(--accent)" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"/>
              </g>;
            })}
            {pts.map((p, i) => p && (
              <circle key={i} cx={p[0]} cy={p[1]} r="3" fill={RATING_COLORS[Math.round(p[2])].bg} stroke="var(--bg)" strokeWidth="1.5"/>
            ))}
          </svg>
        </div>

        {/* Heatmap */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:18, marginBottom:12 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:10 }}>
            <span style={{ fontSize:22 }}>📅</span>
            <span style={{ fontSize:13, fontWeight:800, letterSpacing:'0.05em', textTransform:'uppercase' }}>Heatmap</span>
            <span style={{ marginLeft:'auto', fontSize:11, color:'var(--fg-muted)' }}>worst per day</span>
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(10, 1fr)', gap:5 }}>
            {RATING_30D.map((v, i) => (
              <div key={i} style={{
                aspectRatio:'1/1', borderRadius:8,
                background: v === null ? 'var(--surface-high)' : RATING_COLORS[Math.round(v)].bg,
                opacity: v === null ? 0.5 : 1,
                border: i === RATING_30D.length-1 ? '2px solid var(--fg)' : 'none'
              }}/>
            ))}
          </div>
        </div>

        {/* Frequency */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:18, marginBottom:12 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:6 }}>
            <span style={{ fontSize:22 }}>🔢</span>
            <span style={{ fontSize:13, fontWeight:800, letterSpacing:'0.05em', textTransform:'uppercase' }}>Poops per day</span>
            <span style={{ marginLeft:'auto', fontSize:12, fontWeight:700, color:'var(--fg-muted)' }}>avg 1.9</span>
          </div>
          <div style={{ display:'flex', gap:2, alignItems:'flex-end', height:60, marginTop:8 }}>
            {FREQ_30D.map((v, i) => (
              <div key={i} style={{
                flex:1, height:`${Math.max(2, v/5*100)}%`, borderRadius:2,
                background: v >= 3 ? 'var(--warning)' : 'var(--accent)',
              }}/>
            ))}
          </div>
        </div>

        {/* Food correlation */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:18, marginBottom:12 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:8 }}>
            <span style={{ fontSize:22 }}>🥘</span>
            <span style={{ fontSize:13, fontWeight:800, letterSpacing:'0.05em', textTransform:'uppercase' }}>Food suspects</span>
            <span style={{ marginLeft:'auto', fontSize:11, color:'var(--fg-muted)' }}>eaten 24h before</span>
          </div>
          <div style={{ fontSize:12, color:'var(--fg-muted)', marginBottom:10, lineHeight:1.45 }}>Foods you ate before a bad day, ranked by how much they nudge your rating. Correlation isn't causation — but it's a place to start.</div>
          {FOOD_CORR.slice(0,4).map((f) => {
            const up = parseFloat(f.uplift.replace('−','-'));
            const isBad = up > 0;
            const wlen = Math.min(100, Math.abs(up) * 35);
            return (
              <div key={f.food} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderTop:'1px solid var(--border-subtle)' }}>
                <div style={{ flex:1, minWidth:0 }}>
                  <div style={{ fontSize:14, fontWeight:700 }}>{f.food}</div>
                  <div style={{ fontSize:11, color:'var(--fg-muted)' }}>{f.bad} of {f.days} times preceded a bad day</div>
                </div>
                <div style={{ width:80, position:'relative', height:8, background:'var(--surface-high)', borderRadius:4, overflow:'hidden' }}>
                  <div style={{ position:'absolute', left: isBad ? '50%' : `${50-wlen/2}%`, top:0, bottom:0, width: `${wlen/2}%`, background: isBad ? 'var(--danger)' : 'var(--success-text)' }}/>
                  <div style={{ position:'absolute', left:'50%', top:-1, bottom:-1, width:1, background:'var(--fg-muted)', opacity:0.4 }}/>
                </div>
                <div style={{ width:38, textAlign:'right', fontSize:13, fontWeight:900, color: isBad ? 'var(--danger-text)' : 'var(--success-text)', fontVariantNumeric:'tabular-nums' }}>{f.uplift}</div>
              </div>
            );
          })}
        </div>
      </div>
      <D3TabBar active="trends"/>
    </div>
  );
}

// ── 05. Detail ───────────────────────────────────────────────
function D3Detail() {
  const c = RATING_COLORS[2];
  return (
    <div className="d3">
      <div className="topbar">
        <div className="iconbtn"><I name="arrow-left" size={18}/></div>
        <div style={{ flex:1, textAlign:'center' }}>
          <div style={{ fontSize:12, color:'var(--fg-muted)', fontWeight:700, letterSpacing:'0.05em', textTransform:'uppercase' }}>Entry</div>
          <div style={{ fontSize:13, fontWeight:700 }}>Thu 29 Jan · 08:45</div>
        </div>
        <div style={{ display:'flex', gap:6 }}>
          <div className="iconbtn"><I name="pencil" size={16}/></div>
          <div className="iconbtn"><I name="trash-2" size={16}/></div>
        </div>
      </div>

      <div className="scroll">
        {/* Big mascot reaction */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:24, padding:'20px 18px 20px 100px', marginBottom:12, position:'relative' }}>
          <span style={{ position:'absolute', left:14, top:14, fontSize:64, lineHeight:1 }}>🦀</span>
          <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>The verdict</div>
          <div style={{ fontSize:17, fontWeight:800, marginTop:6, lineHeight:1.35 }}>Trace blood, mushy. Could be the fried rice — that's twice now in a fortnight.</div>
        </div>

        {/* Rating hero */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:'18px', marginBottom:12 }}>
          <div style={{ display:'flex', alignItems:'center', gap:18 }}>
            <div style={{ width:100, height:100, borderRadius:30, background: c.bg, color: c.fg, display:'flex', alignItems:'center', justifyContent:'center', fontSize:58, fontWeight:900, letterSpacing:'-0.03em', lineHeight:1, flexShrink:0 }}>2</div>
            <div style={{ flex:1, minWidth:0 }}>
              <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Blood rating</div>
              <div style={{ fontSize:22, fontWeight:900, color: c.bg, marginTop:4, lineHeight:1.1 }}>{c.label}</div>
              <div style={{ fontSize:13, color:'var(--fg-muted)', marginTop:2, lineHeight:1.4 }}>More than a stripe, less than a stream. Worth keeping an eye on.</div>
            </div>
          </div>
        </div>

        {/* Stool */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:'18px', marginBottom:12 }}>
          <div style={{ display:'flex', alignItems:'center', gap:18 }}>
            <div style={{ width:100, height:80, borderRadius:20, background: 'var(--surface-high)', display:'flex', alignItems:'center', justifyContent:'center', flexShrink:0 }}>
              <D3Shape n={6} color="var(--accent)"/>
            </div>
            <div style={{ flex:1, minWidth:0 }}>
              <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Stool</div>
              <div style={{ fontSize:22, fontWeight:900, marginTop:4 }}>Bristol 6 · Mushy</div>
              <div style={{ fontSize:13, color:'var(--fg-muted)', marginTop:2 }}>Fluffy pieces with ragged edges</div>
            </div>
          </div>
        </div>

        {/* Notes */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:'18px', marginBottom:12 }}>
          <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:6 }}>What you wrote</div>
          <div style={{ fontSize:14, lineHeight:1.5 }}>"A bit urgent. Felt some cramping just before. Bathroom for ~5 min."</div>
        </div>

        {/* Food before */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:'18px', marginBottom:12 }}>
          <div style={{ display:'flex', alignItems:'center', gap:10, marginBottom:10 }}>
            <span style={{ fontSize:18 }}>🍴</span>
            <span style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Eaten in the 24h before</span>
          </div>
          {[
            { t:'21:00', f:'Steak pie, NY cookie', tag:'+1.8', em:'🥧' },
            { t:'17:30', f:'S&P chicken, fried rice', tag:'+2.1', em:'🍚' },
            { t:'13:00', f:'Bagels with butter, banana', em:'🥯' },
            { t:'09:00', f:'Coffee, eggs on toast', em:'☕' },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:10, padding:'10px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <span style={{ fontSize:22 }}>{r.em}</span>
              <div style={{ flex:1 }}>
                <div style={{ fontSize:13, fontWeight:700 }}>{r.f}</div>
                <div style={{ fontSize:11, color:'var(--fg-muted)', fontFamily:'var(--font-mono)' }}>{r.t}</div>
              </div>
              {r.tag && <div style={{ fontSize:12, fontWeight:900, color:'var(--danger-text)', background:'rgba(255,68,0,0.16)', padding:'4px 9px', borderRadius:999, fontVariantNumeric:'tabular-nums' }}>{r.tag}</div>}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── 06. History ──────────────────────────────────────────────
function D3History() {
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
    <div className="d3">
      <div className="topbar">
        <div style={{ flex:1 }}>
          <h1>History</h1>
          <p className="sub">Where you've been 🦀</p>
        </div>
        <div style={{ display:'flex', gap:6 }}>
          <div className="iconbtn"><I name="chevron-left" size={18}/></div>
          <div className="iconbtn"><I name="chevron-right" size={18}/></div>
        </div>
      </div>

      <div className="scroll">
        {/* Greeting */}
        <div className="greeting" style={{ marginBottom: 14 }}>
          <span className="tail"/>
          <span className="crab">🦀</span>
          <div className="bubble">
            <b>14 good days in a row.</b> That's the longest run since November — keep going.
          </div>
        </div>

        {/* Calendar */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:'16px 18px', marginBottom:12 }}>
          <div style={{ fontSize:13, fontWeight:800, letterSpacing:'0.05em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:10 }}>January 2026</div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7, 1fr)', gap:6, marginBottom:8 }}>
            {wds.map((d,i) => <div key={i} style={{ textAlign:'center', fontSize:10, fontWeight:800, color:'var(--fg-faint)', letterSpacing:'0.05em' }}>{d}</div>)}
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(7, 1fr)', gap:6 }}>
            {days.map((d, i) => {
              const cc = d.r != null ? RATING_COLORS[Math.round(d.r)].bg : (d.isFuture ? 'transparent' : 'var(--surface-high)');
              return (
                <div key={i} style={{
                  aspectRatio:'1/1', borderRadius:12, background: cc,
                  display:'flex', alignItems:'center', justifyContent:'center',
                  opacity: d.isFuture ? 0.25 : 1,
                  border: d.isToday ? '2.5px solid var(--accent)' : 'none',
                }}>
                  <span style={{ fontSize:13, fontWeight:800, color: d.r != null ? RATING_COLORS[Math.round(d.r)].fg : 'var(--fg-muted)' }}>{d.day}</span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Stat cards */}
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8, marginBottom:10 }}>
          <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:18, padding:14 }}>
            <div style={{ fontSize:24 }}>🌱</div>
            <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginTop:4 }}>Current run</div>
            <div style={{ fontSize:26, fontWeight:900, letterSpacing:'-0.02em', marginTop:2 }}>14 days</div>
            <div style={{ fontSize:11, color:'var(--success-text)', fontWeight:700 }}>since 17 Jan</div>
          </div>
          <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:18, padding:14 }}>
            <div style={{ fontSize:24 }}>⚠️</div>
            <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginTop:4 }}>Worst day</div>
            <div style={{ fontSize:26, fontWeight:900, letterSpacing:'-0.02em', marginTop:2 }}>9 Jan</div>
            <div style={{ fontSize:11, fontWeight:700, color: RATING_COLORS[4].bg }}>Rating 4 · 5 poops</div>
          </div>
        </div>

        {/* Legend */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:12, display:'flex', alignItems:'center', gap:8, fontSize:11, color:'var(--fg-muted)' }}>
          <span>worst rating</span>
          {[1,2,3,4,5].map(n => (
            <div key={n} style={{ display:'flex', alignItems:'center', gap:3 }}>
              <span style={{ width:14, height:14, borderRadius:4, background: RATING_COLORS[n].bg }}/>
              <span style={{fontWeight:700}}>{n}</span>
            </div>
          ))}
        </div>
      </div>
      <D3TabBar active="history"/>
    </div>
  );
}

// ── 07. Settings ─────────────────────────────────────────────
function D3Settings() {
  const themes = [
    { id: 'deep-navy', name: 'Deep Navy', bg: '#0A1020', surf: '#1C2340', accent: '#4F7CFF' },
    { id: 'charcoal',  name: 'Charcoal',  bg: '#0A0A0A', surf: '#1E1E1E', accent: '#06B6D4' },
    { id: 'retro',     name: 'Retro',     bg: '#1A0B1E', surf: '#2E1438', accent: '#FF00CC', active: true },
  ];

  return (
    <div className="d3">
      <div className="topbar">
        <div style={{ flex:1 }}>
          <h1>You</h1>
          <p className="sub">The whole tracker, shaped your way</p>
        </div>
      </div>

      <div className="scroll">
        {/* Crab profile card */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:'16px 18px', marginBottom:14, display:'flex', alignItems:'center', gap:14 }}>
          <div style={{ width:60, height:60, borderRadius:20, background:'var(--grad-accent)', display:'flex', alignItems:'center', justifyContent:'center', fontSize:34 }}>🦀</div>
          <div style={{ flex:1, minWidth:0 }}>
            <div style={{ fontSize:18, fontWeight:900 }}>Chris</div>
            <div style={{ fontSize:12, color:'var(--fg-muted)' }}>487 entries · day 487 of tracking</div>
          </div>
          <I name="chevron-right" size={20} color="var(--fg-muted)"/>
        </div>

        {/* Stool type */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:16, marginBottom:14 }}>
          <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:10 }}>How you describe shape</div>
          {[
            { l:'Bristol 1–7', d:'Standard scale with diagrams', on:true, em:'🩺' },
            { l:'My plain words', d:'Mushy, soft lumps, hard lumps', em:'✏️' },
            { l:'Both side-by-side', d:'See number + your label', em:'⚖️' },
          ].map((o,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:'10px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <span style={{ fontSize:24 }}>{o.em}</span>
              <div style={{ flex:1, minWidth:0 }}>
                <div style={{ fontWeight:800, fontSize:14 }}>{o.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{o.d}</div>
              </div>
              <div style={{ width:22, height:22, borderRadius:100, border:`2px solid ${o.on ? 'var(--accent)' : 'var(--fg-faint)'}`, display:'flex', alignItems:'center', justifyContent:'center' }}>
                {o.on && <span style={{ width:10, height:10, borderRadius:100, background:'var(--accent)' }}/>}
              </div>
            </div>
          ))}
        </div>

        {/* Theme */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:16, marginBottom:14 }}>
          <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:12 }}>Skin</div>
          <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8 }}>
            {themes.map(t => (
              <div key={t.id} style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:6 }}>
                <div style={{ width:'100%', height:82, borderRadius:16, background:t.bg, position:'relative', border: t.active ? '2.5px solid var(--accent)' : '1px solid var(--border)', overflow:'hidden' }}>
                  <div style={{ position:'absolute', top:8, left:8, right:8, height:10, borderRadius:4, background:t.surf }}/>
                  <div style={{ position:'absolute', top:24, left:8, right:8, bottom:8, borderRadius:8, background:t.surf, opacity:.85 }}/>
                  <div style={{ position:'absolute', bottom:14, right:14, width:18, height:18, borderRadius:100, background:t.accent }}/>
                </div>
                <div style={{ fontSize:12, fontWeight:800, color: t.active ? 'var(--accent-text)' : 'var(--fg-muted)' }}>{t.name}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Reminders */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:16, marginBottom:14 }}>
          <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)', marginBottom:10 }}>Pokes</div>
          {[
            { l:'Morning check-in', d:'09:00 · "anything to log?"', on:true, em:'🌅' },
            { l:'Evening review', d:'21:00 · how was the day?', on:false, em:'🌙' },
            { l:'Forgot meds?', d:'When the day ends quiet', on:true, em:'💊' },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:'10px 0', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <span style={{ fontSize:22 }}>{r.em}</span>
              <div style={{ flex:1 }}>
                <div style={{ fontWeight:800, fontSize:14 }}>{r.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{r.d}</div>
              </div>
              <div style={{ width:42, height:24, borderRadius:999, background: r.on ? 'var(--accent)' : 'var(--surface-high)', position:'relative' }}>
                <div style={{ position:'absolute', top:3, left: r.on ? 21 : 3, width:18, height:18, borderRadius:100, background:'#fff' }}/>
              </div>
            </div>
          ))}
        </div>

        {/* Data */}
        <div style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:22, padding:0, marginBottom:14, overflow:'hidden' }}>
          {[
            { l:'Export to CSV', d:'All 487 entries', em:'📤' },
            { l:'Send to clinician', d:'PDF with last 90 days', em:'🩺' },
            { l:'Import old spreadsheet', d:'pick up where the sheet left off', em:'📥' },
          ].map((r,i) => (
            <div key={i} style={{ display:'flex', alignItems:'center', gap:12, padding:'14px 18px', borderTop: i ? '1px solid var(--border-subtle)' : 'none' }}>
              <span style={{ fontSize:22 }}>{r.em}</span>
              <div style={{ flex:1 }}>
                <div style={{ fontWeight:800, fontSize:14 }}>{r.l}</div>
                <div style={{ fontSize:12, color:'var(--fg-muted)' }}>{r.d}</div>
              </div>
              <I name="chevron-right" size={18} color="var(--fg-muted)"/>
            </div>
          ))}
        </div>
      </div>
      <D3TabBar active="settings"/>
    </div>
  );
}

Object.assign(window, { D3Home, D3AddPoop, D3AddFood, D3Trends, D3Detail, D3History, D3Settings });
