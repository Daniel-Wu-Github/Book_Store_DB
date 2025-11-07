# Bookstore Frontend (Vite + React)

This is a minimal React frontend scaffold (Vite) that fetches `/api/books` from your backend.

Quick start:

1. Install dependencies:

```bash
cd frontend
npm install
```

2. Run dev server (proxies `/api` to `http://localhost:8080`):

```bash
npm run dev
```

Open http://localhost:5173 and the app will fetch `/api/books` via the proxy configured in `vite.config.js`.

Build for production:

```bash
npm run build
npm run preview
```

About React (very brief)
- `src/main.jsx` mounts the app into `#root`.
- `src/App.jsx` is a functional component that uses hooks: `useState` for state and `useEffect` to run the fetch once.
- JSX is used to compose HTML-like structures; values are embedded with `{ }`.

If your backend requires Basic auth for `/api/books`, use one of these approaches in development:
- Add the Authorization header in `fetch` (not recommended for production in client code), or
- Allow anonymous GET for `/api/books` in dev security configuration, or
- Use the proxy to include auth via dev server middleware (advanced).
