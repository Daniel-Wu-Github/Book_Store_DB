import React, { useEffect, useState, useCallback } from 'react'

// ============ API Client ============
const API_BASE = '/api'

async function api(path, options = {}) {
  const res = await fetch(API_BASE + path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  return res
}

// ============ Auth Context ============
const AuthContext = React.createContext(null)

function useAuth() {
  return React.useContext(AuthContext)
}

// ============ Components ============

function LoginPage({ onLogin, onGoRegister }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!username || !password) { setError('Enter credentials'); return }
    setLoading(true)
    setError('')
    try {
      const res = await api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      })
      if (res.ok) {
        onLogin()
      } else {
        setError('Login failed')
      }
    } catch (err) {
      setError('Error: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <h2>Login</h2>
      <form onSubmit={handleSubmit}>
        <input placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} />
        <input placeholder="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} />
        <button type="submit" disabled={loading}>{loading ? 'Logging in...' : 'Login'}</button>
      </form>
      {error && <p className="error">{error}</p>}
      <p>Don't have an account? <button className="link-btn" onClick={onGoRegister}>Register</button></p>
    </div>
  )
}

function RegisterPage({ onGoLogin }) {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!username || !email || !password) { setError('Fill all fields'); return }
    if (username.length < 3) { setError('Username must be at least 3 characters'); return }
    if (password.length < 6) { setError('Password must be at least 6 characters'); return }
    if (!/^[^@\n]+@[^@\n]+\.[^@\n]+$/.test(email)) { setError('Enter a valid email'); return }
    setLoading(true)
    setError('')
    try {
      const res = await api('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username, email, password, roles: 'ROLE_USER', enabled: true }),
      })
      if (res.status === 201) {
        setSuccess('Registered! Please login.')
        setUsername(''); setEmail(''); setPassword('')
      } else if (res.status === 409) {
        const json = await res.json().catch(() => ({}))
        setError(json.error || 'Username or email already exists')
      } else {
        const json = await res.json().catch(() => null)
        setError(json?.error || json?.message || 'Registration failed. Please try again.')
      }
    } catch (err) {
      setError('Error: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-container">
      <h2>Register</h2>
      <form onSubmit={handleSubmit}>
        <input placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} />
        <input placeholder="Email" type="email" value={email} onChange={e => setEmail(e.target.value)} />
        <input placeholder="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} />
        <button type="submit" disabled={loading}>{loading ? 'Registering...' : 'Register'}</button>
      </form>
      {error && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}
      <p>Already have an account? <button className="link-btn" onClick={onGoLogin}>Login</button></p>
    </div>
  )
}

function BooksTab() {
  const [books, setBooks] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [cart, setCart] = useState([])
  const [orderStatus, setOrderStatus] = useState('')

  const loadBooks = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await api('/books/search?q=' + encodeURIComponent(search))
      if (res.ok) {
        const data = await res.json()
        setBooks(data.content || data || [])
      } else {
        setError('Failed to load books')
      }
    } catch (err) {
      setError('Error: ' + err.message)
    } finally {
      setLoading(false)
    }
  }, [search])

  useEffect(() => { loadBooks() }, [])

  const addToCart = (book, itemType, quantity, rentalDays) => {
    setCart([...cart, { bookId: book.id, title: book.title, itemType, quantity, rentalDays: itemType === 'RENT' ? rentalDays : null }])
  }

  const removeFromCart = (index) => {
    setCart(cart.filter((_, i) => i !== index))
  }

  const placeOrder = async () => {
    if (cart.length === 0) { setOrderStatus('Cart is empty'); return }
    setOrderStatus('Placing order...')
    try {
      const items = cart.map(c => ({ bookId: c.bookId, quantity: c.quantity, itemType: c.itemType, rentalDays: c.rentalDays }))
      const res = await api('/orders', { method: 'POST', body: JSON.stringify({ items }) })
      if (res.ok) {
        const order = await res.json()
        setOrderStatus('Order placed! Payment: ' + order.paymentStatus)
        setCart([])
      } else {
        const txt = await res.text()
        setOrderStatus('Order failed: ' + txt)
      }
    } catch (err) {
      setOrderStatus('Error: ' + err.message)
    }
  }

  return (
    <div className="tab-content">
      <h2>📚 Browse Books</h2>
      <div className="search-bar">
        <input placeholder="Search books..." value={search} onChange={e => setSearch(e.target.value)} />
        <button onClick={loadBooks}>Search</button>
      </div>
      {loading && <p>Loading...</p>}
      {error && <p className="error">{error}</p>}
      <table className="data-table">
        <thead>
          <tr><th>Title</th><th>Author</th><th>Price</th><th>Rent Price</th><th>Stock</th><th>Actions</th></tr>
        </thead>
        <tbody>
          {books.map(b => (
            <BookRow key={b.id} book={b} onAdd={addToCart} />
          ))}
        </tbody>
      </table>

      <h3>🛒 Cart</h3>
      {cart.length === 0 ? <p>Cart is empty</p> : (
        <table className="data-table">
          <thead><tr><th>Title</th><th>Type</th><th>Qty</th><th>Rental Days</th><th></th></tr></thead>
          <tbody>
            {cart.map((c, i) => (
              <tr key={i}>
                <td>{c.title}</td>
                <td>{c.itemType}</td>
                <td>{c.quantity}</td>
                <td>{c.rentalDays || '-'}</td>
                <td><button onClick={() => removeFromCart(i)}>Remove</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <button onClick={placeOrder} disabled={cart.length === 0}>Place Order</button>
      {orderStatus && <p className="status">{orderStatus}</p>}
    </div>
  )
}

function BookRow({ book, onAdd }) {
  const [qty, setQty] = useState(1)
  const [type, setType] = useState('BUY')
  const [rentalDays, setRentalDays] = useState(7)

  return (
    <tr>
      <td>{book.title}</td>
      <td>{book.author}</td>
      <td>${book.price?.toFixed(2)}</td>
      <td>${book.rentPrice?.toFixed(2)}</td>
      <td>{book.stock}</td>
      <td className="actions-cell">
        <input type="number" min="1" max="99" value={qty} onChange={e => setQty(+e.target.value)} style={{width:50}} />
        <select value={type} onChange={e => setType(e.target.value)}>
          <option value="BUY">Buy</option>
          <option value="RENT">Rent</option>
        </select>
        {type === 'RENT' && <input type="number" min="1" value={rentalDays} onChange={e => setRentalDays(+e.target.value)} placeholder="Days" style={{width:50}} />}
        <button onClick={() => onAdd(book, type, qty, rentalDays)}>Add</button>
      </td>
    </tr>
  )
}

// ============ Admin Tabs ============

function AdminBooksTab() {
  const [books, setBooks] = useState([])
  const [status, setStatus] = useState('')
  const [editBook, setEditBook] = useState(null)
  const [newBook, setNewBook] = useState(null)

  const load = async () => {
    setStatus('Loading...')
    try {
      const res = await api('/books/search?q=')
      if (res.ok) {
        const data = await res.json()
        setBooks(data.content || data || [])
        setStatus('Loaded ' + (data.content?.length || data.length || 0) + ' books')
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async (bookData) => {
    try {
      const res = await api('/books', { method: 'POST', body: JSON.stringify(bookData) })
      if (res.status === 201) {
        const created = await res.json()
        setBooks([...books, created])
        setStatus('Created: ' + created.title)
        setNewBook(null)
      } else {
        setStatus('Create failed: ' + await res.text())
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  const handleUpdate = async (id, bookData) => {
    try {
      const res = await api('/books/' + id, { method: 'PUT', body: JSON.stringify(bookData) })
      if (res.ok) {
        const updated = await res.json()
        setBooks(books.map(b => b.id === id ? updated : b))
        setStatus('Updated: ' + updated.title)
        setEditBook(null)
      } else {
        setStatus('Update failed: ' + await res.text())
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this book?')) return
    try {
      const res = await api('/books/' + id, { method: 'DELETE' })
      if (res.status === 204 || res.status === 200) {
        setBooks(books.filter(b => b.id !== id))
        setStatus('Deleted')
      } else {
        const errJson = await res.json().catch(() => null)
        setStatus(errJson?.error || `Delete failed (${res.status})`)
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  return (
    <div className="tab-content">
      <h2>📖 Admin: Books</h2>
      <button onClick={load}>Refresh</button>
      <button onClick={() => setNewBook({ title: '', author: '', isbn: '', price: 0, stock: 0, description: '' })}>+ New Book</button>
      <p className="status">{status}</p>

      {newBook && <BookForm book={newBook} onSave={handleCreate} onCancel={() => setNewBook(null)} isNew />}
      {editBook && <BookForm book={editBook} onSave={(data) => handleUpdate(editBook.id, data)} onCancel={() => setEditBook(null)} />}

      <table className="data-table">
        <thead><tr><th>ID</th><th>Title</th><th>Author</th><th>Price</th><th>Rent</th><th>Stock</th><th>Actions</th></tr></thead>
        <tbody>
          {books.map(b => (
            <tr key={b.id}>
              <td>{b.id}</td>
              <td>{b.title}</td>
              <td>{b.author}</td>
              <td>${b.price?.toFixed(2)}</td>
              <td>${b.rentPrice?.toFixed(2)}</td>
              <td>{b.stock}</td>
              <td>
                <button onClick={() => setEditBook(b)}>Edit</button>
                <button onClick={() => handleDelete(b.id)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function BookForm({ book, onSave, onCancel, isNew }) {
  const [title, setTitle] = useState(book.title || '')
  const [author, setAuthor] = useState(book.author || '')
  const [isbn, setIsbn] = useState(book.isbn || '')
  const [price, setPrice] = useState(book.price || 0)
  const [stock, setStock] = useState(book.stock || 0)
  const [description, setDescription] = useState(book.description || '')

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave({ title, author, isbn, price: parseFloat(price), stock: parseInt(stock), description })
  }

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>{isNew ? 'New Book' : 'Edit Book'}</h3>
        <form onSubmit={handleSubmit}>
          <label>Title <input value={title} onChange={e => setTitle(e.target.value)} required /></label>
          <label>Author <input value={author} onChange={e => setAuthor(e.target.value)} required /></label>
          <label>ISBN <input value={isbn} onChange={e => setIsbn(e.target.value)} /></label>
          <label>Price <input type="number" step="0.01" value={price} onChange={e => setPrice(e.target.value)} required /></label>
          <label>Stock <input type="number" value={stock} onChange={e => setStock(e.target.value)} required /></label>
          <label>Description <textarea value={description} onChange={e => setDescription(e.target.value)} /></label>
          <div className="modal-actions">
            <button type="submit">Save</button>
            <button type="button" onClick={onCancel}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  )
}

function AdminUsersTab() {
  const [users, setUsers] = useState([])
  const [status, setStatus] = useState('')
  const [newUser, setNewUser] = useState(null)
  const [visiblePasswords, setVisiblePasswords] = useState({}) // track which passwords are visible

  const load = async () => {
    setStatus('Loading...')
    try {
      const res = await api('/admin/users')
      if (res.ok) {
        const data = await res.json()
        setUsers(data)
        setStatus('Loaded ' + data.length + ' users')
      } else if (res.status === 403) {
        setStatus('Access denied')
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async (userData) => {
    try {
      const res = await api('/admin/users', { method: 'POST', body: JSON.stringify(userData) })
      if (res.status === 201) {
        const created = await res.json()
        setUsers([...users, created])
        setStatus('Created: ' + created.username)
        setNewUser(null)
      } else {
        setStatus('Create failed: ' + await res.text())
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this user?')) return
    try {
      const res = await api('/admin/users/' + id, { method: 'DELETE' })
      if (res.status === 204 || res.status === 200) {
        setUsers(users.filter(u => u.id !== id))
        setStatus('Deleted')
      } else {
        const errJson = await res.json().catch(() => null)
        setStatus(errJson?.error || `Delete failed (${res.status})`)
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  const togglePasswordVisibility = (userId) => {
    setVisiblePasswords(prev => ({ ...prev, [userId]: !prev[userId] }))
  }

  const maskPassword = (password) => {
    return password ? '•'.repeat(Math.min(password.length, 12)) : '-'
  }

  return (
    <div className="tab-content">
      <h2>👥 Admin: Users</h2>
      <button onClick={load}>Refresh</button>
      <button onClick={() => setNewUser({ username: '', email: '', password: '', roles: 'ROLE_USER', enabled: true })}>+ New User</button>
      <p className="status">{status}</p>

      {newUser && <UserForm user={newUser} onSave={handleCreate} onCancel={() => setNewUser(null)} />}

      <table className="data-table">
        <thead><tr><th>ID</th><th>Username</th><th>Password</th><th>Email</th><th>Roles</th><th>Enabled</th><th>Actions</th></tr></thead>
        <tbody>
          {users.map(u => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td>{u.username}</td>
              <td>
                <span 
                  onClick={() => togglePasswordVisibility(u.id)} 
                  style={{cursor: 'pointer', fontFamily: 'monospace'}}
                  title="Click to reveal/hide password"
                >
                  {visiblePasswords[u.id] ? (u.rawPassword || '(not stored)') : maskPassword(u.rawPassword)}
                </span>
              </td>
              <td>{u.email}</td>
              <td>{u.roles}</td>
              <td>{u.enabled ? '✓' : '✗'}</td>
              <td><button onClick={() => handleDelete(u.id)}>Delete</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function UserForm({ user, onSave, onCancel }) {
  const [username, setUsername] = useState(user.username || '')
  const [email, setEmail] = useState(user.email || '')
  const [password, setPassword] = useState('')
  const [roles, setRoles] = useState(user.roles || 'ROLE_USER')

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave({ username, email, password, roles, enabled: true })
  }

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>New User</h3>
        <form onSubmit={handleSubmit}>
          <label>Username <input value={username} onChange={e => setUsername(e.target.value)} required /></label>
          <label>Email <input type="email" value={email} onChange={e => setEmail(e.target.value)} required /></label>
          <label>Password <input type="password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
          <label>Roles <input value={roles} onChange={e => setRoles(e.target.value)} placeholder="ROLE_USER,ROLE_ADMIN" /></label>
          <div className="modal-actions">
            <button type="submit">Create</button>
            <button type="button" onClick={onCancel}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  )
}

function AdminOrdersTab() {
  const [orders, setOrders] = useState([])
  const [status, setStatus] = useState('')

  const load = async () => {
    setStatus('Loading...')
    try {
      const res = await api('/admin/orders')
      if (res.ok) {
        const data = await res.json()
        setOrders(data)
        setStatus('Loaded ' + data.length + ' orders')
      } else if (res.status === 403) {
        setStatus('Access denied')
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  useEffect(() => { load() }, [])

  const markPaid = async (id) => {
    try {
      const res = await api('/admin/orders/' + id + '/payment?status=PAID', { method: 'POST' })
      if (res.ok) { setStatus('Marked paid'); load() }
      else { setStatus('Failed to mark paid') }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  const resendEmail = async (id) => {
    try {
      const res = await api('/admin/orders/' + id + '/resend-email', { method: 'POST' })
      if (res.ok) { setStatus('Email resent'); load() }
      else { setStatus('Failed to resend email') }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  const handleDeleteOrder = async (id) => {
    if (!confirm('Delete this order? This cannot be undone.')) return
    try {
      const res = await api('/admin/orders/' + id, { method: 'DELETE' })
      if (res.status === 204 || res.status === 200) {
        setOrders(orders.filter(o => o.id !== id))
        setStatus('Order deleted')
      } else {
        const errJson = await res.json().catch(() => null)
        setStatus(errJson?.error || `Delete failed (${res.status})`)
      }
    } catch (err) { setStatus('Error: ' + err.message) }
  }

  return (
    <div className="tab-content">
      <h2>📦 Admin: Orders</h2>
      <button onClick={load}>Refresh</button>
      <p className="status">{status}</p>
      <table className="data-table">
        <thead><tr><th>ID</th><th>User</th><th>Total</th><th>Payment</th><th>Status</th><th>Emailed</th><th>Actions</th></tr></thead>
        <tbody>
          {orders.map(o => (
            <tr key={o.id}>
              <td>{o.id}</td>
              <td>{o.username}</td>
              <td>${o.totalAmount}</td>
              <td>{o.paymentStatus}</td>
              <td>{o.orderStatus}</td>
              <td>{o.emailed ? '✓' : '✗'}</td>
              <td>
                <button onClick={() => markPaid(o.id)}>Mark Paid</button>
                <button onClick={() => resendEmail(o.id)}>Resend Email</button>
                <button onClick={() => handleDeleteOrder(o.id)} style={{backgroundColor: '#dc3545'}}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

// ============ Main App ============

function MainApp({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState('books')
  const isAdmin = user?.roles?.includes('ADMIN') || user?.roles?.includes('MANAGER')

  return (
    <div className="main-app">
      <header>
        <h1>📚 Bookstore</h1>
        <span>Logged in as <strong>{user?.username}</strong> ({user?.roles})</span>
        <button onClick={onLogout}>Logout</button>
      </header>
      <nav>
        <button className={activeTab === 'books' ? 'active' : ''} onClick={() => setActiveTab('books')}>Books</button>
        {isAdmin && <button className={activeTab === 'admin-books' ? 'active' : ''} onClick={() => setActiveTab('admin-books')}>Admin: Books</button>}
        {isAdmin && <button className={activeTab === 'admin-users' ? 'active' : ''} onClick={() => setActiveTab('admin-users')}>Admin: Users</button>}
        {isAdmin && <button className={activeTab === 'admin-orders' ? 'active' : ''} onClick={() => setActiveTab('admin-orders')}>Admin: Orders</button>}
      </nav>
      <main>
        {activeTab === 'books' && <BooksTab />}
        {activeTab === 'admin-books' && <AdminBooksTab />}
        {activeTab === 'admin-users' && <AdminUsersTab />}
        {activeTab === 'admin-orders' && <AdminOrdersTab />}
      </main>
    </div>
  )
}

export default function App() {
  const [page, setPage] = useState('login') // login, register, main
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // Check if already logged in
  useEffect(() => {
    api('/auth/me').then(res => {
      if (res.ok) return res.json()
      return null
    }).then(me => {
      if (me && me.username) {
        setUser(me)
        setPage('main')
      }
    }).finally(() => setLoading(false))
  }, [])

  const handleLogin = async () => {
    const res = await api('/auth/me')
    if (res.ok) {
      const me = await res.json()
      setUser(me)
      setPage('main')
    }
  }

  const handleLogout = () => {
    setUser(null)
    setPage('login')
  }

  if (loading) return <div className="loading">Loading...</div>

  return (
    <AuthContext.Provider value={{ user }}>
      {page === 'login' && <LoginPage onLogin={handleLogin} onGoRegister={() => setPage('register')} />}
      {page === 'register' && <RegisterPage onGoLogin={() => setPage('login')} />}
      {page === 'main' && <MainApp user={user} onLogout={handleLogout} />}
    </AuthContext.Provider>
  )
}
