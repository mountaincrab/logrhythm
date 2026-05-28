import { useState } from 'react'
import { Check, Pencil, Trash2, Plus, LogOut } from 'lucide-react'
import AppShell from '../components/AppShell'
import { useAuth } from '../contexts/AuthContext'
import { useProfileContext } from '../contexts/ProfileContext'
import { THEMES } from '../lib/theme'

export default function SettingsPage() {
  const { user, signOut } = useAuth()
  const { profiles, activeProfile, activeProfileId, setActiveProfile, createProfile, renameProfile, deleteProfile, setTheme } =
    useProfileContext()

  const [newName, setNewName] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editName, setEditName] = useState('')

  const startEdit = (id: string, name: string) => { setEditingId(id); setEditName(name) }
  const commitEdit = async () => {
    if (editingId && editName.trim()) await renameProfile(editingId, editName.trim())
    setEditingId(null)
  }
  const addProfile = async () => {
    const name = newName.trim()
    if (!name) return
    const id = await createProfile(name)
    setNewName('')
    setActiveProfile(id)
  }
  const removeProfile = async (id: string) => {
    if (profiles.length <= 1) return
    if (!confirm('Delete this profile? Its entries stay in the database but become hidden.')) return
    await deleteProfile(id)
    if (id === activeProfileId) {
      const next = profiles.find((p) => p.id !== id)
      if (next) setActiveProfile(next.id)
    }
  }

  return (
    <AppShell title="Settings">
      <div className="max-w-2xl">
        {/* Appearance */}
        <section className="mb-9">
          <div className="ds-eyebrow mb-2">Appearance</div>
          <h2 className="text-lg font-bold mb-0.5">Theme</h2>
          <p className="text-fg-muted text-sm mb-4">Saved on the active profile ({activeProfile?.name ?? '—'}) and synced to your phone.</p>
          <div className="grid grid-cols-3 gap-2.5">
            {THEMES.map((t) => {
              const active = activeProfile?.theme === t.id
              return (
                <button
                  key={t.id}
                  onClick={() => setTheme(t.id)}
                  data-theme={t.attr}
                  className={'rounded-2xl p-3 text-left border transition-colors ' + (active ? 'border-accent bg-surface-high' : 'border-DEFAULT bg-surface-raised')}
                >
                  <div className="h-12 w-full rounded-lg mb-2.5 flex items-end p-1.5" style={{ background: t.bg }}>
                    <div className="h-2.5 w-9 rounded-full" style={{ background: t.accent }} />
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-[13px] font-semibold text-fg">{t.label}</span>
                    {active && <Check size={14} className="text-accent-text" />}
                  </div>
                </button>
              )
            })}
          </div>
        </section>

        {/* Profiles */}
        <section className="mb-9">
          <div className="ds-eyebrow mb-2">Profiles</div>
          <h2 className="text-lg font-bold mb-0.5">Who you're tracking</h2>
          <p className="text-fg-muted text-sm mb-4">Each profile keeps its own entries and theme.</p>

          <div className="flex flex-col gap-2 mb-3">
            {profiles.map((p) => {
              const isActive = p.id === activeProfileId
              return (
                <div key={p.id} className="flex items-center gap-2 bg-surface-raised border border-DEFAULT rounded-xl px-3 py-2.5">
                  {editingId === p.id ? (
                    <input
                      autoFocus
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter') commitEdit(); if (e.key === 'Escape') setEditingId(null) }}
                      onBlur={commitEdit}
                      className="flex-1 bg-transparent text-fg text-sm font-semibold outline-none border-b border-accent"
                    />
                  ) : (
                    <button onClick={() => setActiveProfile(p.id)} className="flex-1 flex items-center gap-2 text-left">
                      <span className="text-sm font-semibold text-fg">{p.name}</span>
                      {isActive && <span className="text-[10px] font-bold text-accent-text bg-accent-soft px-1.5 py-0.5 rounded-full">ACTIVE</span>}
                    </button>
                  )}
                  <button onClick={() => startEdit(p.id, p.name)} className="w-8 h-8 rounded-lg flex items-center justify-center text-fg-muted hover:text-fg">
                    <Pencil size={15} />
                  </button>
                  <button
                    onClick={() => removeProfile(p.id)}
                    disabled={profiles.length <= 1}
                    className="w-8 h-8 rounded-lg flex items-center justify-center text-fg-muted hover:text-danger-text disabled:opacity-30"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              )
            })}
          </div>

          <div className="flex gap-2">
            <input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && addProfile()}
              placeholder="New profile name"
              className="flex-1 bg-surface-raised border border-DEFAULT rounded-xl px-3 py-2.5 text-sm text-fg outline-none focus:border-accent placeholder:text-fg-faint transition-colors"
            />
            <button
              onClick={addProfile}
              disabled={!newName.trim()}
              className="px-4 rounded-xl bg-accent text-accent-fg font-semibold text-sm inline-flex items-center gap-1.5 disabled:opacity-40"
            >
              <Plus size={16} /> Add
            </button>
          </div>
        </section>

        {/* Account */}
        <section className="mb-6">
          <div className="ds-eyebrow mb-2">Account</div>
          <div className="bg-surface-raised border border-DEFAULT rounded-xl px-4 py-3.5 flex items-center justify-between gap-3">
            <div className="min-w-0">
              <div className="text-sm font-semibold text-fg truncate">{user?.displayName ?? 'Signed in'}</div>
              <div className="text-xs text-fg-muted truncate">{user?.email}</div>
            </div>
            <button onClick={signOut} className="shrink-0 inline-flex items-center gap-1.5 text-sm text-fg-muted hover:text-fg font-semibold">
              <LogOut size={16} /> Sign out
            </button>
          </div>
        </section>
      </div>
    </AppShell>
  )
}
