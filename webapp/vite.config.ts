import { execFileSync } from 'node:child_process'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Git tags are the single source of truth for the version, mirroring the Android
// app (see app/build.gradle.kts) — no version-bump commit is ever needed. The
// value is baked in at build time as __APP_VERSION__ and shown in Settings.
function git(...args: string[]): string | null {
  try {
    return execFileSync('git', args, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim() || null
  } catch {
    return null // git missing / not a repo / no matching tag
  }
}

function appVersion(): string {
  // CI can inject the version directly (e.g. the tag the release just cut).
  const injected = process.env.VITE_APP_VERSION?.trim()
  if (injected) return injected.replace(/^v/, '')

  const base = git('describe', '--tags', '--abbrev=0', '--match', 'v[0-9]*')?.replace(/^v/, '') ?? '0.0.0'
  const branch = process.env.VERSION_BRANCH?.trim() || git('rev-parse', '--abbrev-ref', 'HEAD') || 'local'
  const onReleaseTag = git('describe', '--tags', '--exact-match', '--match', 'v[0-9]*') !== null
  if (onReleaseTag || branch === 'main' || branch === 'HEAD') return base

  const safe = branch.replace(/[^A-Za-z0-9]+/g, '-').replace(/^-+|-+$/g, '').toLowerCase()
  const sha = process.env.VERSION_SHA?.trim().slice(0, 7) || git('rev-parse', '--short=7', 'HEAD') || 'nogit'
  return `${base}-${safe}.${sha}`
}

export default defineConfig({
  plugins: [react()],
  define: {
    __APP_VERSION__: JSON.stringify(appVersion()),
  },
})
