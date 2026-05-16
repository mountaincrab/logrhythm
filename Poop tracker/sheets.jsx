// ── Interactive sheets for the prototype ──────────────────────
// AddPoopSheet, AddFoodSheet, EntryDetailSheet — each takes
// onClose and (where relevant) onSave handlers. Stoolmode is
// driven by the "Stool type" tweak.

const { useState: usePSt } = React;

// Plain-English types map onto Bristol numbers
const PLAIN_TYPES = [
  { n: 1, label: 'Hard lumps',   key: 'hard-lumps' },
  { n: 2, label: 'Lumpy log',    key: 'lumpy' },
  { n: 4, label: 'Smooth log',   key: 'smooth' },
  { n: 5, label: 'Soft lumps',   key: 'soft-lumps' },
  { n: 6, label: 'Mushy',        key: 'mushy' },
  { n: 7, label: 'Liquid',       key: 'liquid' },
];

function AddPoopSheet({ onClose, onSave, stoolMode = 'bristol' }) {
  const [bristol, setBristol] = usePSt(6);
  const [rating, setRating] = usePSt(1);
  const [showMore, setShowMore] = usePSt(false);
  const [flags, setFlags] = usePSt({ meds: false, caffeine: false, alcohol: false });
  const [notes, setNotes] = usePSt('');
  const c = RATING_COLORS[rating];
  const bristolSel = BRISTOL.find(b => b.n === bristol);

  return (
    <div className="d1">
      <div className="sheet">
        <div className="sheethead">
          <button className="x" onClick={onClose} aria-label="Close"><I name="x" size={20}/></button>
          <h1>Log a poop</h1>
        </div>
        <div className="body">
          <div className="field">
            <div className="lab"><span>When</span><span className="hint">Now</span></div>
            <div className="when">
              <div className="pill"><I name="calendar" size={16} color="var(--fg-muted)"/><span className="v">Today</span></div>
              <div className="pill"><I name="clock" size={16} color="var(--fg-muted)"/><span className="v">{new Date().toTimeString().slice(0,5)}</span></div>
            </div>
          </div>

          {/* Type — adapts to stoolMode */}
          {stoolMode === 'bristol' && (
            <div className="field">
              <div className="lab"><span>Type</span><span className="hint">Bristol scale</span></div>
              <div className="bristolgrid">
                {BRISTOL.map(b => (
                  <button key={b.n} className={`b ${b.n === bristol ? 'on' : ''}`} onClick={() => setBristol(b.n)}>
                    <span className="n">{b.n}</span>
                    <span className="lab">{b.plain.split(' ')[0]}</span>
                  </button>
                ))}
              </div>
              <p className="bristol-desc"><b>Type {bristolSel.n} · {bristolSel.plain}.</b> {bristolSel.desc}.</p>
            </div>
          )}

          {stoolMode === 'plain' && (
            <div className="field">
              <div className="lab"><span>Type</span><span className="hint">your plain types</span></div>
              <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8 }}>
                {PLAIN_TYPES.map(t => (
                  <button key={t.key} onClick={() => setBristol(t.n)} style={{
                    padding:'14px 12px', borderRadius:12, textAlign:'left',
                    background: t.n === bristol ? 'var(--accent-soft)' : 'var(--surface-raised)',
                    border: '1px solid ' + (t.n === bristol ? 'var(--accent)' : 'var(--border)'),
                    color: t.n === bristol ? 'var(--accent-text)' : 'var(--fg)',
                    fontSize:14, fontWeight:700,
                  }}>{t.label}</button>
                ))}
              </div>
            </div>
          )}

          {stoolMode === 'both' && (
            <div className="field">
              <div className="lab"><span>Type</span><span className="hint">Bristol + my label</span></div>
              <div className="bristolgrid">
                {BRISTOL.map(b => (
                  <button key={b.n} className={`b ${b.n === bristol ? 'on' : ''}`} onClick={() => setBristol(b.n)}>
                    <span className="n">{b.n}</span>
                    <span className="lab">{b.plain.split(' ')[0]}</span>
                  </button>
                ))}
              </div>
              <p className="bristol-desc"><b>Bristol {bristolSel.n} \u2014 {bristolSel.plain}.</b> {bristolSel.desc}.</p>
            </div>
          )}

          {/* Rating */}
          <div className="field">
            <div className="lab"><span>Blood rating</span><span className="hint">1–5</span></div>
            <div className="ratingpills">
              {[1,2,3,4,5].map(n => {
                const rc = RATING_COLORS[n];
                return (
                  <button key={n} className={n === rating ? 'on' : ''} style={{ '--rc': rc.bg, '--rfg': rc.fg }} onClick={() => setRating(n)}>
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
            <textarea className="textarea" placeholder="Urgency, pain, anything that felt different…" value={notes} onChange={e => setNotes(e.target.value)}/>
          </div>

          {/* More */}
          <button className="more-toggle" onClick={() => setShowMore(s => !s)} style={{ background:'transparent', cursor:'pointer' }}>
            <I name={showMore ? 'chevron-up' : 'chevron-down'} size={16}/> {showMore ? 'Hide' : 'More'} — meds, caffeine, alcohol
          </button>
          {showMore && (
            <div className="more-grid">
              {[
                { k:'meds',     label:'Missed meds', sub: flags.meds ? 'Missed' : 'None today', ic:'pill' },
                { k:'caffeine', label:'Caffeine',    sub: flags.caffeine ? 'Yes' : 'Not today',    ic:'coffee' },
                { k:'alcohol',  label:'Alcohol',     sub: flags.alcohol ? 'Yes' : 'Not today',     ic:'wine' },
              ].map(f => (
                <button key={f.k} className={`opt ${flags[f.k] ? 'on' : ''}`} onClick={() => setFlags(s => ({ ...s, [f.k]: !s[f.k] }))} style={{ background:'transparent', textAlign:'left' }}>
                  <span className="ic"><I name={f.ic} size={16}/></span>
                  <div><div className="nm">{f.label}</div><div className="st">{f.sub}</div></div>
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="savebar">
          <button className="b cancel" onClick={onClose}>Cancel</button>
          <button className="b save" onClick={() => onSave && onSave({ bristol, rating, notes, flags })}>Save poop</button>
        </div>
      </div>
    </div>
  );
}

function AddFoodSheet({ onClose, onSave }) {
  const [items, setItems] = usePSt('');
  const [meal, setMeal] = usePSt('breakfast');
  return (
    <div className="d1">
      <div className="sheet">
        <div className="sheethead">
          <button className="x" onClick={onClose} aria-label="Close"><I name="x" size={20}/></button>
          <h1>Log food</h1>
        </div>
        <div className="body">
          <div className="field">
            <div className="lab"><span>When</span><span className="hint">Now</span></div>
            <div className="when">
              <div className="pill"><I name="calendar" size={16} color="var(--fg-muted)"/><span className="v">Today</span></div>
              <div className="pill"><I name="clock" size={16} color="var(--fg-muted)"/><span className="v">{new Date().toTimeString().slice(0,5)}</span></div>
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>What you ate</span></div>
            <textarea className="textarea" style={{ minHeight: 120 }} placeholder="Free text — be specific where it matters (e.g. 'spicy chicken', 'whole milk')." value={items} onChange={e => setItems(e.target.value)}/>
          </div>

          <div className="field">
            <div className="lab"><span>Recent</span><span className="hint">tap to add</span></div>
            <div style={{ display:'flex', flexWrap:'wrap', gap:6 }}>
              {['Banana','White rice','Plain toast','Buttered toast','Boiled chicken','Plain yoghurt','Apple','Coffee','Eggs'].map(t => (
                <button key={t} onClick={() => setItems(prev => prev ? `${prev}, ${t.toLowerCase()}` : t)} style={{
                  padding:'7px 12px', borderRadius:999, background:'var(--surface-raised)',
                  border:'1px solid var(--border)', fontSize:12, color:'var(--fg-muted)', cursor:'pointer'
                }}>{t}</button>
              ))}
            </div>
          </div>

          <div className="field">
            <div className="lab"><span>Tag</span><span className="hint">optional</span></div>
            <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
              {[
                { k:'breakfast', l:'Breakfast', ic:'sunrise' },
                { k:'lunch',     l:'Lunch',     ic:'sun' },
                { k:'dinner',    l:'Dinner',    ic:'moon' },
                { k:'snack',     l:'Snack',     ic:'cookie' },
                { k:'drink',     l:'Drink',     ic:'cup-soda' },
              ].map(m => (
                <button key={m.k} onClick={() => setMeal(m.k)} style={{
                  padding:'8px 12px', borderRadius:12,
                  background: meal === m.k ? 'var(--accent-soft)' : 'var(--surface-raised)',
                  border:'1px solid ' + (meal === m.k ? 'var(--accent)' : 'var(--border)'),
                  color: meal === m.k ? 'var(--accent-text)' : 'var(--fg-muted)',
                  fontSize:12, fontWeight:600, display:'inline-flex', alignItems:'center', gap:6, cursor:'pointer'
                }}><I name={m.ic} size={14}/>{m.l}</button>
              ))}
            </div>
          </div>
        </div>
        <div className="savebar">
          <button className="b cancel" onClick={onClose}>Cancel</button>
          <button className="b save" onClick={() => onSave && onSave({ items, meal })}>Save food</button>
        </div>
      </div>
    </div>
  );
}

function EntryDetailSheet({ entry, onClose }) {
  const e = entry || { type: 'poop', time: '08:45', bristol: 6, plain: 'Mushy', rating: 2, notes: 'A bit urgent. Felt some cramping just before. Bathroom for ~5 min.' };
  if (e.type !== 'poop') {
    // Food entry detail — simpler
    return (
      <div className="d1 v2">
        <div className="topbar">
          <button className="iconbtn" onClick={onClose} style={{cursor:'pointer'}}><I name="arrow-left" size={20}/></button>
          <div style={{ flex:1, textAlign:'center' }}>
            <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Food</div>
            <div style={{ fontSize:13, fontWeight:700 }}>{e.time}</div>
          </div>
          <div style={{ width: 40 }}/>
        </div>
        <div className="scroll" style={{ padding: '4px 16px 20px' }}>
          <div className="notescard">
            <div className="l">What you ate</div>
            <div className="v">{e.items}</div>
          </div>
        </div>
      </div>
    );
  }
  const c = RATING_COLORS[Math.round(e.rating)];
  return (
    <div className="d1 v2">
      <div className="topbar">
        <button className="iconbtn" onClick={onClose} style={{cursor:'pointer'}}><I name="arrow-left" size={20}/></button>
        <div style={{ flex:1, textAlign:'center' }}>
          <div style={{ fontSize:11, fontWeight:800, letterSpacing:'0.10em', textTransform:'uppercase', color:'var(--fg-muted)' }}>Entry</div>
          <div style={{ fontSize:13, fontWeight:700 }}>{e.dateLabel || 'Thu 29 Jan'} · {e.time}</div>
        </div>
        <div style={{ display:'flex', gap:6 }}>
          <div className="iconbtn"><I name="pencil" size={18}/></div>
          <div className="iconbtn"><I name="trash-2" size={18}/></div>
        </div>
      </div>

      <div className="scroll" style={{ padding: 0 }}>
        <div className="detail-hero" style={{ '--rc': c.bg, '--rfg': c.fg }}>
          <div className="big">{e.rating}</div>
          <div className="meta">
            <div className="l">Blood rating</div>
            <div className="lab">{c.label}</div>
            <div className="desc">{e.rating === 1 ? 'All clear — no flare signal here.' : e.rating === 2 ? 'More than a stripe — worth keeping an eye on.' : 'Worth flagging — talk to your clinician if this keeps up.'}</div>
          </div>
        </div>

        <div className="twocol">
          <div className="c">
            <div className="l">Time</div>
            <div className="v" style={{ fontVariantNumeric:'tabular-nums' }}>{e.time}</div>
            <div className="d">{e.dateLabel || 'Thu 29 Jan 2026'}</div>
          </div>
          <div className="c">
            <div className="l">Stool</div>
            <div className="v">Bristol {e.bristol}</div>
            <div className="d">{e.plain} · {(BRISTOL.find(b => b.n === e.bristol) || {}).desc || ''}</div>
          </div>
        </div>

        {e.notes && (
          <div className="notescard">
            <div className="l">Notes</div>
            <div className="v">{e.notes}</div>
          </div>
        )}

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

Object.assign(window, { AddPoopSheet, AddFoodSheet, EntryDetailSheet });
