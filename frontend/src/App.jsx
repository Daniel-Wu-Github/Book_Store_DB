import React, { useEffect, useState } from 'react'

export default function App() {
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    async function load() {
      try {
        const res = await fetch('/api/books')
        // diagnostic: store status and raw body if not ok
        if (!res.ok) {
          const txt = await res.text().catch(()=>'<no-body>')
          setError(`HTTP ${res.status}: ${txt}`)
          console.error('fetch /api/books failed', res.status, txt)
          return
        }
        const data = await res.json()
        console.debug('fetch /api/books success', data)
        // Spring returns a Page<T> for pageable endpoints. Support both raw arrays and Page responses.
        if (Array.isArray(data)) {
          setBooks(data)
        } else if (data && Array.isArray(data.content)) {
          setBooks(data.content)
        } else {
          // unexpected shape — show it for debugging
          setError('Unexpected response shape: ' + JSON.stringify(data))
          console.warn('Unexpected /api/books response shape', data)
        }
      } catch (e) {
        console.error('fetch /api/books exception', e)
        setError(String(e))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  return (
    <div style={{ padding: 20, fontFamily: 'Arial, Helvetica, sans-serif' }}>
      <h1>Bookstore — React</h1>
  {loading && <p>Loading…</p>}
  {error && <pre style={{ color: 'red', whiteSpace: 'pre-wrap' }}>Error: {error}</pre>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {books.map(b => (
          <li key={b.id} style={{ border: '1px solid #eee', marginBottom: 8, padding: 8, borderRadius: 6 }}>
            <strong>{b.title}</strong> — <em>{b.author}</em>
            <div>ISBN: {b.isbn} — ${b.price}</div>
            <div style={{ marginTop: 6 }}>{b.description}</div>
          </li>
        ))}
      </ul>
    </div>
  )
}
