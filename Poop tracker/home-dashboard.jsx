// ── Dashboard variant of the Home screen ──────────────────────
// Selected via the "Home layout" tweak. Based on D2's dashboard
// but in the refined v2 chrome (bottom log bar, Home/History/
// Settings tabs). Hero stat at top, today's mini-log below.

function V2Spark({ values, color = 'var(--accent)', height = 50, width = 340 }) {
  const pad = 4;
  const min = 1, max = 5;
  const pts = values.map((v, i) => {
    if (v == null) return null;
    const x = pad + (i / (values.length - 1)) * (width - pad * 2);
    const y = pad + (1 - (v - min) / (max - min)) * (height - pad * 2);
    return [x, y, v];
  });
  const segs = [];
  let cur = [];
  pts.forEach(p => { if (p === null) { if (cur.length) segs.push(cur); cur = []; } else cur.push(p); });
  if (cur.length) segs.push(cur);
  return (
    <svg viewBox={`0 0 ${width} ${height}`} style={{ width:'100%', height, display:'block' }}>
      <defs>
        <linearGradient id="dash-spark-fill" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.30"/>
          <stop offset="100%" stopColor={color} stopOpacity="0"/>
        </linearGradient>
      </defs>
      {segs.map((seg, si) => {
        const line = seg.map((p, i) => (i ? 'L' : 'M') + p[0] + ' ' + p[1]).join(' ');
        const area = line + ` L${seg[seg.length-1][0]} ${height-pad} L${seg[0][0]} ${height-pad} Z`;
        return (
          <g key={si}>
            <path d={area} fill="url(#dash-spark-fill)"/>
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

function V2HomeDashboard({ onOpenSheet, onSelectEntry } = {}) {
  const last7 = RATING_30D.slice(-7);
  return (
    <div className="d1 v2">
      <div className="topbar">
        <div>
          <h1>Home</h1>
          <p className="sub">Fri 30 Jan · day 487 of tracking</p>
        </div>
        <div className="iconbtn"><I name="search" size={20}/></div>
      </div>

      <div className="scroll" style={{ paddingBottom: 20 }}>
        {/* Today's rating hero */}
        <div style={{
          margin: '4px 16px 12px', background: 'var(--surface-raised)',
          border: '1px solid var(--border)', borderRadius: 20, padding: 18
        }}>
          <div style={{ display:'flex', alignItems:'baseline', justifyContent:'space-between', marginBottom: 8 }}>
            <span style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Today's rating</span>
            <span style={{ fontSize:11, fontWeight:700, color:'var(--success-text)' }}>↓ from 2 yesterday</span>
          </div>
          <div style={{ display:'flex', alignItems:'baseline', gap:10 }}>
            <span style={{ fontSize:56, fontWeight:900, letterSpacing:'-0.03em', lineHeight:1, color: RATING_COLORS[1].bg }}>1</span>
            <span style={{ fontSize:14, color:'var(--fg-muted)', fontWeight:500 }}>no blood</span>
            <span style={{ marginLeft:'auto', fontSize:11, fontWeight:700, padding:'5px 10px', borderRadius:999, background:'rgba(16,185,129,0.16)', color:'var(--success-text)' }}>Good day</span>
          </div>
          <div style={{ marginTop: 12 }}>
            <V2Spark values={last7} color="var(--accent)" height={50} width={340}/>
            <div style={{ display:'flex', justifyContent:'space-between', fontSize:10, fontFamily:'var(--font-mono)', color:'var(--fg-faint)', marginTop:2 }}>
              <span>7 days ago</span><span>today</span>
            </div>
          </div>
        </div>

        {/* Mini-stats strip */}
        <div style={{ margin: '0 16px 14px', display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:8 }}>
          {[
            { l:'30d avg', v:'1.4', c:'var(--fg)' },
            { l:'Good days', v:'18', c:'var(--success-text)' },
            { l:'Last bad', v:'21d', c: RATING_COLORS[3].bg },
          ].map(s => (
            <div key={s.l} style={{ background:'var(--surface-raised)', border:'1px solid var(--border)', borderRadius:14, padding:'10px 12px' }}>
              <div style={{ fontSize:10, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>{s.l}</div>
              <div style={{ fontSize:22, fontWeight:900, color: s.c, letterSpacing:'-0.02em', marginTop:2 }}>{s.v}</div>
            </div>
          ))}
        </div>

        {/* Today's log */}
        <div className="daygrp" style={{ paddingTop: 0 }}>
          <div className="label"><span className="d">Today · Fri 30 Jan</span><span className="sum">3 entries</span></div>
          <div className="timeline">
            {ENTRIES_TODAY.map((e, i) => (
              <D1TimeEntry key={i} e={e} onClick={() => onSelectEntry && onSelectEntry({ ...e, dateLabel: 'Fri 30 Jan' })}/>
            ))}
          </div>
        </div>

        {/* Yesterday */}
        <div className="daygrp">
          <div className="label"><span className="d">Yesterday · Thu 29 Jan</span><span className="sum">5 entries</span></div>
          <div className="timeline">
            {ENTRIES_YESTERDAY.map((e, i) => (
              <D1TimeEntry key={i} e={e} onClick={() => onSelectEntry && onSelectEntry({ ...e, dateLabel: 'Thu 29 Jan' })}/>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom log bar */}
      <div className="logbar">
        <button onClick={() => onOpenSheet && onOpenSheet('poop')}><span className="em">💩</span> Poop</button>
        <button onClick={() => onOpenSheet && onOpenSheet('food')}><span className="em">🍴</span> Food</button>
        <button onClick={() => onOpenSheet && onOpenSheet('note')}><span className="em">📝</span> Note</button>
      </div>
      <V2TabBar active="home"/>
    </div>
  );
}

window.V2HomeDashboard = V2HomeDashboard;
