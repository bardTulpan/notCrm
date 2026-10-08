import { useEffect, useState } from 'react'
import { studentsApi } from '../api/students'

// One fetch per (student, photo version) for the whole session. The token travels in a header, which a plain
// <img src> can't send, so photos come through axios as blobs; the backend marks them cacheable, so a reload
// is served from the browser cache until the version changes.
const cache = new Map<string, Promise<string | null>>()

function load(studentId: string, version: string): Promise<string | null> {
  const key = `${studentId}:${version}`
  let entry = cache.get(key)
  if (!entry) {
    entry = studentsApi
      .avatar(studentId, version)
      .then((blob) => URL.createObjectURL(blob))
      .catch(() => {
        cache.delete(key) // let a later render try again
        return null
      })
    cache.set(key, entry)
  }
  return entry
}

/** Object URL of the student's photo, or null while loading / when there is none. */
export function useAvatarUrl(studentId: string, version: string | null): string | null {
  const [loaded, setLoaded] = useState<{ key: string; url: string | null } | null>(null)
  const key = version ? `${studentId}:${version}` : null

  useEffect(() => {
    if (!version || !key) return
    let alive = true
    load(studentId, version).then((url) => {
      if (alive) setLoaded({ key, url })
    })
    return () => {
      alive = false
    }
  }, [studentId, version, key])

  return key && loaded?.key === key ? loaded.url : null
}
