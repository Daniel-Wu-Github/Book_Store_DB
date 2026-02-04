import React, { useEffect, useState, useCallback, createContext, useContext } from 'react'
import AnimatedBook from './AnimatedBook'

// ============ API Client ============
// Detect production (Vercel) vs local dev
const isProduction = window.location.hostname.includes('vercel.app')
const API_BASE = isProduction 
  ? 'https://bookstore-backend-api-8acfb9e7cc5c.herokuapp.com/api'
  : (import.meta.env.VITE_API_BASE_URL || '/api')

async function api(path, options = {}) {
  const res = await fetch(API_BASE + path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  return res
}

// ============ Contexts ============
const AuthContext = createContext(null)
const ToastContext = createContext(null)
const ThemeContext = createContext(null)

function useAuth() { return useContext(AuthContext) }
function useToast() { return useContext(ToastContext) }
function useTheme() { return useContext(ThemeContext) }

// ============ Toast Notifications ============
function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const addToast = (message, type = 'info') => {
    const id = Date.now()
    setToasts(prev => [...prev, { id, message, type }])
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000)
  }

  return (
    <ToastContext.Provider value={{ addToast }}>
      {children}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            {t.type === 'success' && <span className="toast-icon">✓</span>}
            {t.type === 'error' && <span className="toast-icon">✕</span>}
            {t.type === 'info' && <span className="toast-icon">ℹ</span>}
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  const toggle = () => {
    const next = theme === 'dark' ? 'light' : 'dark'
    setTheme(next)
    try { localStorage.setItem('theme', next) } catch (e) {}
  }
  return (
    <button className="theme-toggle" onClick={toggle} aria-label="Toggle theme">
      {theme === 'dark' ? '🌙' : '☀️'}
    </button>
  )
}

// ============ Icons (Simple SVG Components) ============
const Icons = {
  Book: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>,
  Users: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>,
  Orders: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>,
  Cart: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>,
  Search: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>,
  Plus: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>,
  Trash: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>,
  Edit: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>,
  Logout: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>,
  Menu: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="18" x2="21" y2="18"/></svg>,
  X: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>,
  Check: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="20 6 9 17 4 12"/></svg>,
  Mail: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>,
  Eye: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>,
  EyeOff: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>,
  Dollar: () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>,
}

// ============ Reusable Components ============
function Button({ children, variant = 'primary', size = 'md', icon: Icon, loading, disabled, className = '', ...props }) {
  return (
    <button 
      className={`btn btn-${variant} btn-${size} ${loading ? 'btn-loading' : ''} ${className}`} 
      disabled={disabled || loading}
      {...props}
    >
      {loading && <span className="spinner" />}
      {Icon && !loading && <Icon />}
      {children}
    </button>
  )
}

function Input({ label, error, icon: Icon, ...props }) {
  return (
    <div className="input-group">
      {label && <label className="input-label">{label}</label>}
      <div className="input-wrapper">
        {Icon && <span className="input-icon"><Icon /></span>}
        <input className={`input ${error ? 'input-error' : ''} ${Icon ? 'has-icon' : ''}`} {...props} />
      </div>
      {error && <span className="input-error-text">{error}</span>}
    </div>
  )
}

function Card({ children, className = '', hover = false, ...props }) {
  return <div className={`card ${hover ? 'card-hover' : ''} ${className}`} {...props}>{children}</div>
}

function Badge({ children, variant = 'default' }) {
  return <span className={`badge badge-${variant}`}>{children}</span>
}

function Modal({ isOpen, onClose, title, children, size = 'md' }) {
  if (!isOpen) return null
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className={`modal modal-${size}`} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button className="modal-close" onClick={onClose}><Icons.X /></button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <div className="empty-state">
      {Icon && <div className="empty-state-icon"><Icon /></div>}
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  )
}

function Stat({ label, value, icon: Icon }) {
  return (
    <div className="stat-card">
      <div className="stat-icon"><Icon /></div>
      <div className="stat-content">
        <span className="stat-value">{value}</span>
        <span className="stat-label">{label}</span>
      </div>
    </div>
  )
}

// ============ Auth Pages ============
function LoginPage({ onLogin, onGoRegister }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [magicEmail, setMagicEmail] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [magicLoading, setMagicLoading] = useState(false)
  const toast = useToast()

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!username || !password) { setError('Please enter your credentials'); return }
    setLoading(true)
    setError('')
    try {
      console.log('Attempting login to:', API_BASE + '/auth/login')
      const res = await api('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
      console.log('Login response status:', res.status)
      if (res.ok) { onLogin() } 
      else { 
        const text = await res.text()
        console.log('Login failed:', res.status, text)
        setError(`Login failed (${res.status}): ${text || 'Invalid credentials'}`) 
      }
    } catch (err) { 
      console.error('Login error:', err)
      setError('Connection error: ' + err.message) 
    }
    finally { setLoading(false) }
  }

  const handleSendMagic = async () => {
    if (!magicEmail || !/^[^@\n]+@[^@\n]+\.[^@\n]+$/.test(magicEmail)) { toast.addToast('Enter a valid email', 'error'); return }
    setMagicLoading(true)
    try {
      const res = await api('/auth/magic-link', { method: 'POST', body: JSON.stringify({ email: magicEmail }) })
      if (res.ok) {
        toast.addToast('Magic link sent — check your email', 'success')
      } else if (res.status === 404) {
        toast.addToast('Email not found', 'error')
      } else {
        const txt = await res.text().catch(() => '')
        toast.addToast('Failed to send link: ' + (txt || res.status), 'error')
      }
    } catch (err) {
      toast.addToast('Connection error: ' + err.message, 'error')
    } finally { setMagicLoading(false) }
  }

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-header">
          <div className="auth-logo"><AnimatedBook size={120} /></div>
          <h1>Welcome Back</h1>
          <p>Sign in to access your bookstore account</p>
        </div>
        
        <form onSubmit={handleSubmit} className="auth-form">
          <Input 
            label="Username" 
            placeholder="Enter your username"
            value={username} 
            onChange={e => setUsername(e.target.value)}
            autoComplete="username"
          />
            <Input 
              label="Email (for magic link)"
              type="email"
              placeholder="Enter email to receive login link"
              value={magicEmail}
              onChange={e => setMagicEmail(e.target.value)}
            />
            <div style={{display:'flex',gap:8}}>
              <Button type="button" onClick={handleSendMagic} loading={magicLoading} variant="secondary">Email me a login link</Button>
            </div>
          <div className="input-group">
            <label className="input-label">Password</label>
            <div className="input-wrapper">
              <input 
                type={showPassword ? 'text' : 'password'}
                className="input"
                placeholder="Enter your password"
                value={password} 
                onChange={e => setPassword(e.target.value)}
                autoComplete="current-password"
              />
              <button type="button" className="input-toggle" onClick={() => setShowPassword(!showPassword)}>
                {showPassword ? <Icons.EyeOff /> : <Icons.Eye />}
              </button>
            </div>
          </div>
          
          {error && <div className="alert alert-error">{error}</div>}
          
          <Button type="submit" loading={loading} className="btn-block">
            Sign In
          </Button>
        </form>

        <div className="auth-footer">
          <p>Don't have an account? <button className="link-btn" onClick={onGoRegister}>Create one</button></p>
        </div>
      </div>
      
      <div className="auth-side">
        <div className="auth-side-content">
          <h2>The Cozy Bookshop</h2>
          <p>Discover your next favorite read from our curated collection of books.</p>
          <div className="auth-features">
            <div className="auth-feature"><Icons.Book /> Browse thousands of titles</div>
            <div className="auth-feature"><Icons.Cart /> Easy ordering & rentals</div>
            <div className="auth-feature"><Icons.Mail /> Order confirmations via email</div>
          </div>
        </div>
      </div>
    </div>
  )
}

function RegisterPage({ onGoLogin }) {
  const [formData, setFormData] = useState({ username: '', email: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)

  const validate = () => {
    const errs = {}
    if (!formData.username || formData.username.length < 3) errs.username = 'Username must be at least 3 characters'
    if (!formData.email || !/^[^@\n]+@[^@\n]+\.[^@\n]+$/.test(formData.email)) errs.email = 'Enter a valid email'
    if (!formData.password || formData.password.length < 6) errs.password = 'Password must be at least 6 characters'
    if (formData.password !== formData.confirmPassword) errs.confirmPassword = 'Passwords do not match'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    try {
      const res = await api('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username: formData.username, email: formData.email, password: formData.password, roles: 'ROLE_USER', enabled: true }),
      })
      if (res.status === 201) { setSuccess(true) }
      else {
        const json = await res.json().catch(() => ({}))
        setErrors({ general: json.error || json.message || 'Registration failed' })
      }
    } catch (err) { setErrors({ general: 'Connection error. Please try again.' }) }
    finally { setLoading(false) }
  }

  const updateField = (field) => (e) => setFormData({ ...formData, [field]: e.target.value })

  if (success) {
    return (
      <div className="auth-page">
        <div className="auth-container">
          <div className="auth-header">
            <div className="auth-logo success">✓</div>
            <h1>Account Created!</h1>
            <p>Your account has been successfully created. You can now sign in.</p>
          </div>
          <Button onClick={onGoLogin} className="btn-block">Go to Sign In</Button>
        </div>
        <div className="auth-side">
          <div className="auth-side-content">
            <h2>The Cozy Bookshop</h2>
            <p>Welcome to our community of book lovers!</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-header">
          <div className="auth-logo"><AnimatedBook size={64} /></div>
          <h1>Create Account</h1>
          <p>Join our community of book lovers</p>
        </div>
        
        <form onSubmit={handleSubmit} className="auth-form">
          <Input label="Username" placeholder="Choose a username" value={formData.username} onChange={updateField('username')} error={errors.username} />
          <Input label="Email" type="email" placeholder="your@email.com" value={formData.email} onChange={updateField('email')} error={errors.email} />
          <Input label="Password" type="password" placeholder="At least 6 characters" value={formData.password} onChange={updateField('password')} error={errors.password} />
          <Input label="Confirm Password" type="password" placeholder="Re-enter your password" value={formData.confirmPassword} onChange={updateField('confirmPassword')} error={errors.confirmPassword} />
          
          {errors.general && <div className="alert alert-error">{errors.general}</div>}
          
          <Button type="submit" loading={loading} className="btn-block">Create Account</Button>
        </form>

        <div className="auth-footer">
          <p>Already have an account? <button className="link-btn" onClick={onGoLogin}>Sign in</button></p>
        </div>
      </div>
      
      <div className="auth-side">
        <div className="auth-side-content">
          <h2>The Cozy Bookshop</h2>
          <p>Discover your next favorite read from our curated collection of books.</p>
        </div>
      </div>
    </div>
  )
}

// ============ Book Components ============
function BookCard({ book, onAddToCart }) {
  const [qty, setQty] = useState(1)
  const [type, setType] = useState('BUY')
  const [days, setDays] = useState(7)
  const [showOptions, setShowOptions] = useState(false)

  const handleAdd = () => {
    onAddToCart(book, type, qty, days)
    setShowOptions(false)
    setQty(1)
  }

  return (
    <Card className="book-card" hover>
      <div className="book-cover">
        <span className="book-cover-text">{book.title?.charAt(0)}</span>
      </div>
      <div className="book-info">
        <h3 className="book-title">{book.title}</h3>
        <p className="book-author">by {book.author}</p>
        {book.description && <p className="book-desc">{book.description}</p>}
        <div className="book-meta">
          <div className="book-prices">
            <span className="price-buy">${book.price?.toFixed(2)}</span>
            <span className="price-rent">${book.rentPrice?.toFixed(2)}/week</span>
          </div>
          <Badge variant={book.stock > 0 ? 'success' : 'error'}>
            {book.stock > 0 ? `${book.stock} in stock` : 'Out of stock'}
          </Badge>
        </div>
      </div>
      
      {!showOptions ? (
        <Button onClick={() => setShowOptions(true)} disabled={book.stock < 1} className="book-action">
          Add to Cart
        </Button>
      ) : (
        <div className="book-options">
          <div className="book-options-row">
            <select value={type} onChange={e => setType(e.target.value)} className="input">
              <option value="BUY">Buy</option>
              <option value="RENT">Rent</option>
            </select>
            <input type="number" min="1" max={book.stock} value={qty} onChange={e => setQty(+e.target.value)} className="input qty-input" />
            {type === 'RENT' && <input type="number" min="1" value={days} onChange={e => setDays(+e.target.value)} placeholder="Days" className="input days-input" />}
          </div>
          <div className="book-options-actions">
            <Button size="sm" onClick={handleAdd}>Add</Button>
            <Button size="sm" variant="ghost" onClick={() => setShowOptions(false)}>Cancel</Button>
          </div>
        </div>
      )}
    </Card>
  )
}

function CartSidebar({ cart, onRemove, onPlaceOrder, orderStatus }) {
  const total = cart.reduce((sum, item) => {
    const price = item.itemType === 'RENT' ? item.rentPrice * item.rentalDays : item.price
    return sum + price * item.quantity
  }, 0)

  return (
    <div className="cart-sidebar">
      <div className="cart-header">
        <h3><Icons.Cart /> Cart ({cart.length})</h3>
      </div>
      
      {cart.length === 0 ? (
        <div className="cart-empty">
          <Icons.Cart />
          <p>Your cart is empty</p>
        </div>
      ) : (
        <>
          <div className="cart-items">
            {cart.map((item, i) => (
              <div key={i} className="cart-item">
                <div className="cart-item-info">
                  <span className="cart-item-title">{item.title}</span>
                  <span className="cart-item-meta">
                    {item.itemType} × {item.quantity}
                    {item.itemType === 'RENT' && ` (${item.rentalDays} days)`}
                  </span>
                </div>
                <button className="cart-item-remove" onClick={() => onRemove(i)}>
                  <Icons.X />
                </button>
              </div>
            ))}
          </div>
          
          <div className="cart-footer">
            <div className="cart-total">
              <span>Estimated Total</span>
              <span className="cart-total-value">${total.toFixed(2)}</span>
            </div>
            <Button onClick={onPlaceOrder} className="btn-block" icon={Icons.Check}>
              Place Order
            </Button>
            {orderStatus && <p className="cart-status">{orderStatus}</p>}
          </div>
        </>
      )}
    </div>
  )
}

// ============ Main Tabs ============
function BooksTab() {
  const [books, setBooks] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [cart, setCart] = useState([])
  const [orderStatus, setOrderStatus] = useState('')
  const toast = useToast()

  const loadBooks = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api('/books/search?q=' + encodeURIComponent(search))
      if (res.ok) {
        const data = await res.json()
        setBooks(data.content || data || [])
      }
    } catch (err) { toast.addToast('Failed to load books', 'error') }
    finally { setLoading(false) }
  }, [search])

  useEffect(() => { loadBooks() }, [])

  const addToCart = (book, itemType, quantity, rentalDays) => {
    setCart([...cart, { 
      bookId: book.id, 
      title: book.title, 
      price: book.price,
      rentPrice: book.rentPrice,
      itemType, 
      quantity, 
      rentalDays: itemType === 'RENT' ? rentalDays : null 
    }])
    toast.addToast(`Added "${book.title}" to cart`, 'success')
  }

  const removeFromCart = (index) => setCart(cart.filter((_, i) => i !== index))

  const placeOrder = async () => {
    if (cart.length === 0) return
    setOrderStatus('Processing...')
    try {
      const items = cart.map(c => ({ bookId: c.bookId, quantity: c.quantity, itemType: c.itemType, rentalDays: c.rentalDays }))
      const res = await api('/orders', { method: 'POST', body: JSON.stringify({ items }) })
      if (res.ok) {
        toast.addToast('Order placed successfully!', 'success')
        setCart([])
        setOrderStatus('')
        loadBooks()
      } else {
        const txt = await res.text()
        setOrderStatus('')
        toast.addToast('Order failed: ' + txt, 'error')
      }
    } catch (err) { setOrderStatus(''); toast.addToast('Error placing order', 'error') }
  }

  return (
    <div className="books-page">
      <div className="books-main">
        <div className="page-header">
          <div>
            <h1>Browse Books</h1>
            <p className="page-subtitle">Find your next favorite read</p>
          </div>
        </div>

        <div className="search-bar">
          <div className="search-input-wrapper">
            <Icons.Search />
            <input 
              placeholder="Search by title or author..." 
              value={search} 
              onChange={e => setSearch(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && loadBooks()}
            />
          </div>
          <Button onClick={loadBooks} icon={Icons.Search}>Search</Button>
        </div>

        {loading ? (
          <div className="loading-grid">
            {[1,2,3,4,5,6].map(i => <div key={i} className="skeleton-card" />)}
          </div>
        ) : books.length === 0 ? (
          <EmptyState icon={Icons.Book} title="No books found" description="Try a different search term" />
        ) : (
          <div className="book-grid">
            {books.map(book => <BookCard key={book.id} book={book} onAddToCart={addToCart} />)}
          </div>
        )}
      </div>
      
      <CartSidebar cart={cart} onRemove={removeFromCart} onPlaceOrder={placeOrder} orderStatus={orderStatus} />
    </div>
  )
}

// ============ Admin Components ============
function AdminBooksTab() {
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)
  const [editBook, setEditBook] = useState(null)
  const [newBook, setNewBook] = useState(null)
  const toast = useToast()

  const load = async () => {
    setLoading(true)
    try {
      const res = await api('/books/search?q=')
      if (res.ok) { const data = await res.json(); setBooks(data.content || data || []) }
    } catch (err) { toast.addToast('Failed to load books', 'error') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async (bookData) => {
    try {
      const res = await api('/books', { method: 'POST', body: JSON.stringify(bookData) })
      if (res.status === 201) {
        const created = await res.json()
        setBooks([...books, created])
        toast.addToast('Book created successfully', 'success')
        setNewBook(null)
      } else { toast.addToast('Failed to create book', 'error') }
    } catch (err) { toast.addToast('Error creating book', 'error') }
  }

  const handleUpdate = async (id, bookData) => {
    try {
      const res = await api('/books/' + id, { method: 'PUT', body: JSON.stringify(bookData) })
      if (res.ok) {
        const updated = await res.json()
        setBooks(books.map(b => b.id === id ? updated : b))
        toast.addToast('Book updated successfully', 'success')
        setEditBook(null)
      } else { toast.addToast('Failed to update book', 'error') }
    } catch (err) { toast.addToast('Error updating book', 'error') }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this book?')) return
    try {
      const res = await api('/books/' + id, { method: 'DELETE' })
      if (res.status === 204 || res.status === 200) {
        setBooks(books.filter(b => b.id !== id))
        toast.addToast('Book deleted', 'success')
      } else {
        const errJson = await res.json().catch(() => null)
        toast.addToast(errJson?.error || 'Delete failed', 'error')
      }
    } catch (err) { toast.addToast('Error deleting book', 'error') }
  }

  return (
    <div className="admin-page">
      <div className="page-header">
        <div>
          <h1>Manage Books</h1>
          <p className="page-subtitle">{books.length} books in catalog</p>
        </div>
        <Button onClick={() => setNewBook({ title: '', author: '', isbn: '', price: 0, rentPrice: 0, stock: 0, description: '' })} icon={Icons.Plus}>
          Add Book
        </Button>
      </div>

      <Card className="table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>Book</th>
              <th>ISBN</th>
              <th>Price</th>
              <th>Rent</th>
              <th>Stock</th>
              <th className="actions-col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="6" className="loading-cell">Loading...</td></tr>
            ) : books.length === 0 ? (
              <tr><td colSpan="6" className="empty-cell">No books found</td></tr>
            ) : books.map(b => (
              <tr key={b.id}>
                <td>
                  <div className="table-title-cell">
                    <span className="table-title">{b.title}</span>
                    <span className="table-subtitle">by {b.author}</span>
                  </div>
                </td>
                <td><code>{b.isbn}</code></td>
                <td>${b.price?.toFixed(2)}</td>
                <td>${b.rentPrice?.toFixed(2)}</td>
                <td><Badge variant={b.stock > 5 ? 'success' : b.stock > 0 ? 'warning' : 'error'}>{b.stock}</Badge></td>
                <td>
                  <div className="table-actions">
                    <Button size="sm" variant="ghost" onClick={() => setEditBook(b)} icon={Icons.Edit} />
                    <Button size="sm" variant="ghost-danger" onClick={() => handleDelete(b.id)} icon={Icons.Trash} />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Modal isOpen={!!newBook} onClose={() => setNewBook(null)} title="Add New Book">
        {newBook && <BookForm book={newBook} onSave={handleCreate} onCancel={() => setNewBook(null)} isNew />}
      </Modal>

      <Modal isOpen={!!editBook} onClose={() => setEditBook(null)} title="Edit Book">
        {editBook && <BookForm book={editBook} onSave={(data) => handleUpdate(editBook.id, data)} onCancel={() => setEditBook(null)} />}
      </Modal>
    </div>
  )
}

function BookForm({ book, onSave, onCancel, isNew }) {
  const [form, setForm] = useState({
    title: book.title || '', author: book.author || '', isbn: book.isbn || '',
    price: book.price || 0, rentPrice: book.rentPrice || 0, stock: book.stock || 0, description: book.description || ''
  })

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave({ ...form, price: parseFloat(form.price), rentPrice: parseFloat(form.rentPrice), stock: parseInt(form.stock) })
  }

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <form onSubmit={handleSubmit} className="form-grid">
      <Input label="Title" value={form.title} onChange={update('title')} required />
      <Input label="Author" value={form.author} onChange={update('author')} required />
      <Input label="ISBN" value={form.isbn} onChange={update('isbn')} />
      <div className="form-row">
        <Input label="Buy Price" type="number" step="0.01" value={form.price} onChange={update('price')} required />
        <Input label="Rent Price" type="number" step="0.01" value={form.rentPrice} onChange={update('rentPrice')} required />
        <Input label="Stock" type="number" value={form.stock} onChange={update('stock')} required />
      </div>
      <div className="input-group">
        <label className="input-label">Description</label>
        <textarea className="input textarea" value={form.description} onChange={update('description')} rows="3" />
      </div>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit">{isNew ? 'Create Book' : 'Save Changes'}</Button>
      </div>
    </form>
  )
}

function AdminUsersTab() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [newUser, setNewUser] = useState(null)
  const [visiblePasswords, setVisiblePasswords] = useState({})
  const toast = useToast()

  const load = async () => {
    setLoading(true)
    try {
      const res = await api('/admin/users')
      if (res.ok) { setUsers(await res.json()) }
      else if (res.status === 403) { toast.addToast('Access denied', 'error') }
    } catch (err) { toast.addToast('Failed to load users', 'error') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async (userData) => {
    try {
      const res = await api('/admin/users', { method: 'POST', body: JSON.stringify(userData) })
      if (res.status === 201) {
        setUsers([...users, await res.json()])
        toast.addToast('User created', 'success')
        setNewUser(null)
      } else { toast.addToast('Create failed', 'error') }
    } catch (err) { toast.addToast('Error creating user', 'error') }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this user?')) return
    try {
      const res = await api('/admin/users/' + id, { method: 'DELETE' })
      if (res.status === 204 || res.status === 200) {
        setUsers(users.filter(u => u.id !== id))
        toast.addToast('User deleted', 'success')
      } else {
        const errJson = await res.json().catch(() => null)
        toast.addToast(errJson?.error || 'Delete failed', 'error')
      }
    } catch (err) { toast.addToast('Error deleting user', 'error') }
  }

  return (
    <div className="admin-page">
      <div className="page-header">
        <div>
          <h1>Manage Users</h1>
          <p className="page-subtitle">{users.length} registered users</p>
        </div>
        <Button onClick={() => setNewUser({ username: '', email: '', password: '', roles: 'ROLE_USER', enabled: true })} icon={Icons.Plus}>
          Add User
        </Button>
      </div>

      <Card className="table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Password</th>
              <th>Roles</th>
              <th>Status</th>
              <th className="actions-col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="5" className="loading-cell">Loading...</td></tr>
            ) : users.length === 0 ? (
              <tr><td colSpan="5" className="empty-cell">No users found</td></tr>
            ) : users.map(u => (
              <tr key={u.id}>
                <td>
                  <div className="table-title-cell">
                    <span className="table-title">{u.username}</span>
                    <span className="table-subtitle">{u.email}</span>
                  </div>
                </td>
                <td>
                  <button className="password-reveal" onClick={() => setVisiblePasswords(p => ({ ...p, [u.id]: !p[u.id] }))}>
                    {visiblePasswords[u.id] ? (u.rawPassword || '(not stored)') : '••••••••'}
                    <span className="reveal-icon">{visiblePasswords[u.id] ? <Icons.EyeOff /> : <Icons.Eye />}</span>
                  </button>
                </td>
                <td><Badge variant="secondary">{u.roles}</Badge></td>
                <td><Badge variant={u.enabled ? 'success' : 'error'}>{u.enabled ? 'Active' : 'Disabled'}</Badge></td>
                <td>
                  <div className="table-actions">
                    <Button size="sm" variant="ghost-danger" onClick={() => handleDelete(u.id)} icon={Icons.Trash} />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Modal isOpen={!!newUser} onClose={() => setNewUser(null)} title="Add New User">
        {newUser && <UserForm user={newUser} onSave={handleCreate} onCancel={() => setNewUser(null)} />}
      </Modal>
    </div>
  )
}

function UserForm({ user, onSave, onCancel }) {
  const [form, setForm] = useState({ username: user.username || '', email: user.email || '', password: '', roles: user.roles || 'ROLE_USER' })

  const handleSubmit = (e) => {
    e.preventDefault()
    onSave({ ...form, enabled: true })
  }

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  return (
    <form onSubmit={handleSubmit} className="form-grid">
      <Input label="Username" value={form.username} onChange={update('username')} required />
      <Input label="Email" type="email" value={form.email} onChange={update('email')} required />
      <Input label="Password" type="password" value={form.password} onChange={update('password')} required />
      <div className="input-group">
        <label className="input-label">Roles</label>
        <select className="input" value={form.roles} onChange={update('roles')}>
          <option value="ROLE_USER">User</option>
          <option value="ROLE_ADMIN,ROLE_USER">Admin</option>
          <option value="ROLE_MANAGER,ROLE_USER">Manager</option>
        </select>
      </div>
      <div className="form-actions">
        <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit">Create User</Button>
      </div>
    </form>
  )
}

function AdminOrdersTab() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const toast = useToast()

  const load = async () => {
    setLoading(true)
    try {
      const res = await api('/admin/orders')
      if (res.ok) { setOrders(await res.json()) }
      else if (res.status === 403) { toast.addToast('Access denied', 'error') }
    } catch (err) { toast.addToast('Failed to load orders', 'error') }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [])

  const markPaid = async (id) => {
    try {
      const res = await api('/admin/orders/' + id + '/payment?status=PAID', { method: 'POST' })
      if (res.ok) { toast.addToast('Order marked as paid', 'success'); load() }
      else { toast.addToast('Failed to update order', 'error') }
    } catch (err) { toast.addToast('Error updating order', 'error') }
  }

  const resendEmail = async (id) => {
    try {
      const res = await api('/admin/orders/' + id + '/resend-email', { method: 'POST' })
      if (res.ok) { toast.addToast('Email sent', 'success'); load() }
      else { toast.addToast('Failed to send email', 'error') }
    } catch (err) { toast.addToast('Error sending email', 'error') }
  }

  const handleDelete = async (id) => {
    if (!confirm('Delete this order?')) return
    try {
      const res = await api('/admin/orders/' + id, { method: 'DELETE' })
      if (res.status === 204 || res.status === 200) {
        setOrders(orders.filter(o => o.id !== id))
        toast.addToast('Order deleted', 'success')
      } else {
        const errJson = await res.json().catch(() => null)
        toast.addToast(errJson?.error || 'Delete failed', 'error')
      }
    } catch (err) { toast.addToast('Error deleting order', 'error') }
  }

  const stats = {
    total: orders.length,
    paid: orders.filter(o => o.paymentStatus === 'PAID').length,
    pending: orders.filter(o => o.paymentStatus === 'PENDING').length,
    revenue: orders.filter(o => o.paymentStatus === 'PAID').reduce((sum, o) => sum + (o.totalAmount || 0), 0)
  }

  return (
    <div className="admin-page">
      <div className="page-header">
        <div>
          <h1>Manage Orders</h1>
          <p className="page-subtitle">{orders.length} total orders</p>
        </div>
        <Button onClick={load} variant="secondary" icon={Icons.Search}>Refresh</Button>
      </div>

      <div className="stats-grid">
        <Stat label="Total Orders" value={stats.total} icon={Icons.Orders} />
        <Stat label="Paid" value={stats.paid} icon={Icons.Check} />
        <Stat label="Pending" value={stats.pending} icon={Icons.Dollar} />
        <Stat label="Revenue" value={`$${stats.revenue.toFixed(2)}`} icon={Icons.Dollar} />
      </div>

      <Card className="table-card">
        <table className="data-table">
          <thead>
            <tr>
              <th>Order</th>
              <th>Customer</th>
              <th>Total</th>
              <th>Payment</th>
              <th>Status</th>
              <th>Emailed</th>
              <th className="actions-col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan="7" className="loading-cell">Loading...</td></tr>
            ) : orders.length === 0 ? (
              <tr><td colSpan="7" className="empty-cell">No orders found</td></tr>
            ) : orders.map(o => (
              <tr key={o.id}>
                <td><code>#{o.id}</code></td>
                <td>{o.username}</td>
                <td className="price-cell">${o.totalAmount?.toFixed(2)}</td>
                <td><Badge variant={o.paymentStatus === 'PAID' ? 'success' : 'warning'}>{o.paymentStatus}</Badge></td>
                <td><Badge variant="secondary">{o.orderStatus}</Badge></td>
                <td>{o.emailed ? <span className="check-icon"><Icons.Check /></span> : <span className="x-icon"><Icons.X /></span>}</td>
                <td>
                  <div className="table-actions">
                    {o.paymentStatus !== 'PAID' && <Button size="sm" variant="ghost" onClick={() => markPaid(o.id)} icon={Icons.Dollar} title="Mark Paid" />}
                    <Button size="sm" variant="ghost" onClick={() => resendEmail(o.id)} icon={Icons.Mail} title="Resend Email" />
                    <Button size="sm" variant="ghost-danger" onClick={() => handleDelete(o.id)} icon={Icons.Trash} title="Delete" />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  )
}

// ============ Main App Layout ============
function MainApp({ user, onLogout }) {
  const [activeTab, setActiveTab] = useState('books')
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const isAdmin = user?.roles?.includes('ADMIN') || user?.roles?.includes('MANAGER')

  const navItems = [
    { id: 'books', label: 'Browse Books', icon: Icons.Book },
    ...(isAdmin ? [
      { id: 'admin-books', label: 'Manage Books', icon: Icons.Book, admin: true },
      { id: 'admin-users', label: 'Manage Users', icon: Icons.Users, admin: true },
      { id: 'admin-orders', label: 'Manage Orders', icon: Icons.Orders, admin: true },
    ] : [])
  ]

  return (
    <div className={`app-layout ${sidebarOpen ? 'sidebar-open' : 'sidebar-closed'}`}>
      <aside className="sidebar">
        <div className="sidebar-header">
          <span className="sidebar-logo"><AnimatedBook size={32} /></span>
          <span className="sidebar-title">The Cozy Bookshop</span>
        </div>
        
        <nav className="sidebar-nav">
          {navItems.map(item => (
            <button
              key={item.id}
              className={`nav-item ${activeTab === item.id ? 'active' : ''} ${item.admin ? 'admin-item' : ''}`}
              onClick={() => setActiveTab(item.id)}
            >
              <item.icon />
              <span>{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="user-info">
            <div className="user-avatar">{user?.username?.charAt(0).toUpperCase()}</div>
            <div className="user-details">
              <span className="user-name">{user?.username}</span>
              <span className="user-role">{user?.roles?.includes('ADMIN') ? 'Admin' : 'User'}</span>
            </div>
          </div>
          <button className="logout-btn" onClick={onLogout} title="Sign out">
            <Icons.Logout />
          </button>
        </div>
      </aside>

      <div className="main-content">
        <header className="top-bar">
          <button className="menu-toggle" onClick={() => setSidebarOpen(!sidebarOpen)}>
            <Icons.Menu />
          </button>
          <div className="top-bar-right">
            <span className="welcome-text">Welcome back, <strong>{user?.username}</strong></span>
          </div>
        </header>

        <main className="page-content">
          {activeTab === 'books' && <BooksTab />}
          {activeTab === 'admin-books' && <AdminBooksTab />}
          {activeTab === 'admin-users' && <AdminUsersTab />}
          {activeTab === 'admin-orders' && <AdminOrdersTab />}
        </main>
      </div>
    </div>
  )
}

// ============ Root App ============
export default function App() {
  const [page, setPage] = useState('login')
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [theme, setTheme] = useState(() => {
    try {
      const saved = localStorage.getItem('theme')
      if (saved) return saved
    } catch (e) {}
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) return 'dark'
    return 'light'
  })

  useEffect(() => {
    try { document.documentElement.setAttribute('data-theme', theme) } catch (e) {}
  }, [theme])

  useEffect(() => {
    api('/auth/me').then(res => res.ok ? res.json() : null)
      .then(me => { if (me?.username) { setUser(me); setPage('main') } })
      .finally(() => setLoading(false))
  }, [])

  const handleLogin = async () => {
    const res = await api('/auth/me')
    if (res.ok) { const me = await res.json(); setUser(me); setPage('main') }
  }

  const handleLogout = async () => {
    await api('/auth/logout', { method: 'POST' }).catch(() => {})
    setUser(null)
    setPage('login')
  }

  if (loading) return (
    <div className="loading-screen">
      <div className="loading-spinner" />
      <p>Loading...</p>
    </div>
  )

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <ToastProvider>
        <AuthContext.Provider value={{ user }}>
          <div>
            <ThemeToggle />
            {page === 'login' && <LoginPage onLogin={handleLogin} onGoRegister={() => setPage('register')} />}
            {page === 'register' && <RegisterPage onGoLogin={() => setPage('login')} />}
            {page === 'main' && <MainApp user={user} onLogout={handleLogout} />}
          </div>
        </AuthContext.Provider>
      </ToastProvider>
    </ThemeContext.Provider>
  )
}
