import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'
import heroImage from './assets/hero.png'
import deunaQrImage from './assets/deuna-qr.png'
import { api, tokenKey } from './api/client.js'
import { useLocation, useNavigate } from 'react-router-dom'

const productPlaceholder = 'https://images.unsplash.com/photo-1513519245088-0e12902e5a38?auto=format&fit=crop&w=700&q=85'
const routePages = {
  '/': 'Inicio',
  '/explorar': 'Explorar',
  '/favoritos': 'Favoritos',
  '/mis-publicaciones': 'Mis publicaciones',
  '/mensajes': 'Mensajes',
  '/mis-compras': 'Mis compras',
  '/ayuda': 'Ayuda',
  '/cuenta': 'Mi cuenta',
  '/administracion': 'Administración',
  '/configuracion': 'Configuración',
}
const mediaUrl = (value) => value?.startsWith('/uploads/')
  ? `${api.defaults.baseURL.replace(/\/api$/, '')}${value}`
  : value || productPlaceholder

async function apiRequest(path, options = {}) {
  try {
    const requestData = options.data ?? options.body
    const headers = { ...options.headers }
    if (typeof requestData === 'string') headers['Content-Type'] = 'application/json'
    const response = await api.request({
      url: path,
      ...options,
      headers,
      data: requestData,
    })
    return response.data
  } catch (error) {
    if (error.code === 'ECONNABORTED') throw new Error('La solicitud tardó demasiado. Intenta nuevamente.')
    if (!error.response) throw new Error('No se pudo conectar con el servidor. Comprueba que el backend esté encendido.')
    throw new Error(error.response.data?.error || `Error del servidor (${error.response.status})`)
  }
}

const categories = [
  { name: 'Tecnología', icon: '⌘', color: 'violet' },
  { name: 'Libros', icon: '▤', color: 'blue' },
  { name: 'Ropa', icon: '♧', color: 'orange' },
  { name: 'Hogar', icon: '⌂', color: 'teal' },
  { name: 'Deportes', icon: '⚽', color: 'pink' },
  { name: 'Otros', icon: '✳', color: 'yellow' },
]
const categoryNames = { 1: 'Tecnología', 2: 'Libros', 3: 'Ropa', 4: 'Hogar', 5: 'Deportes', 6: 'Otros' }

const navItems = [
  ['Inicio', '⌂'],
  ['Explorar', '⌕'],
  ['Mis publicaciones', '▣'],
  ['Mensajes', '▱'],
  ['Favoritos', '♡'],
  ['Mis compras', '$'],
  ['Ayuda', '?'],
]

function App() {
  const navigate = useNavigate()
  const location = useLocation()
  const [activeNav, setActiveNav] = useState('Inicio')
  const currentNav = routePages[location.pathname] || activeNav
  const [products, setProducts] = useState([])
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState('Todas')
  const [cart, setCart] = useState([])
  const [favorites, setFavorites] = useState([])
  const [modal, setModal] = useState('')
  const [toast, setToast] = useState('')
  const [authMode, setAuthMode] = useState('login')
  const [authUser, setAuthUser] = useState(null)
  const [authError, setAuthError] = useState('')
  const [authLoading, setAuthLoading] = useState(false)
  const [authResolved, setAuthResolved] = useState(() => !window.localStorage.getItem(tokenKey))
  const [dataError, setDataError] = useState('')
  const [paymentProduct, setPaymentProduct] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('card_simulation')
  const [paymentBusy, setPaymentBusy] = useState(false)
  const [paymentResult, setPaymentResult] = useState(null)
  const [transferProofNumber, setTransferProofNumber] = useState('')
  const [transferProofFile, setTransferProofFile] = useState(null)
  const [cardNumber, setCardNumber] = useState('')
  const [cardHolder, setCardHolder] = useState('')
  const [cardExpiry, setCardExpiry] = useState('')
  const [cardCvv, setCardCvv] = useState('')
  const [paymentConfig, setPaymentConfig] = useState({
    feePercent: 0,
    feeFixed: 0,
    banks: [
      { name: 'Banco Pichincha', account: '220000001234', holder: 'UPEC Market Demo' },
      { name: 'Banco Guayaquil', account: '210000009876', holder: 'UPEC Market Demo' },
    ],
  })
  const [selectedBank, setSelectedBank] = useState(0)
  const [conversations, setConversations] = useState([])
  const [selectedConversation, setSelectedConversation] = useState(null)
  const [chatMessages, setChatMessages] = useState([])
  const [chatDraft, setChatDraft] = useState('')
  const [orders, setOrders] = useState([])
  const [myProducts, setMyProducts] = useState([])
  const [pendingProducts, setPendingProducts] = useState([])
  const [adminUsers, setAdminUsers] = useState([])
  const [adminSettings, setAdminSettings] = useState([])
  const [settingsBusy, setSettingsBusy] = useState(false)
  const [settingsError, setSettingsError] = useState('')
  const [settingsPassword, setSettingsPassword] = useState('')
  const [settingKey, setSettingKey] = useState('')
  const [settingValue, setSettingValue] = useState('')
  const [adminLoading, setAdminLoading] = useState(() => location.pathname === '/administracion')
  const [categoryRecords, setCategoryRecords] = useState([])
  const [editingProduct, setEditingProduct] = useState(null)
  const [profileBusy, setProfileBusy] = useState(false)
  const [profileError, setProfileError] = useState('')
  const isAdmin = authUser?.roles?.includes('ADMIN') || authUser?.roles?.includes('SUPER_ADMIN')
  const isSuperAdmin = authUser?.roles?.includes('SUPER_ADMIN')
  const showToast = useCallback((message) => {
    setToast(message)
    window.setTimeout(() => setToast(''), 2600)
  }, [])

  useEffect(() => {
    const token = window.localStorage.getItem(tokenKey)
    if (!token) return
    api.get('/auth/me', { headers: { Authorization: `Bearer ${token}` } })
      .then((response) => response.data)
      .then(setAuthUser)
      .catch(() => {
        window.localStorage.removeItem(tokenKey)
        setAuthUser(null)
        setCart([])
      })
      .finally(() => setAuthResolved(true))
  }, [])

  useEffect(() => {
    const handleExpiredSession = () => {
      setAuthUser(null)
      setCart([])
      setAuthError('Tu sesión expiró. Inicia sesión nuevamente.')
    }
    window.addEventListener('upec-session-expired', handleExpiredSession)
    return () => window.removeEventListener('upec-session-expired', handleExpiredSession)
  }, [])
  useEffect(() => {
    apiRequest('/categories')
      .then((items) => setCategoryRecords(items.map((item) => ({ ...item, name: categoryNames[item.id] || item.name }))))
      .catch(() => setCategoryRecords([]))
    apiRequest('/payment-config')
      .then(setPaymentConfig)
      .catch(() => {})
    apiRequest('/products')
      .then((items) => {
        setProducts(items.map((item) => ({
          ...item,
          category: categoryNames[item.categoryId] || item.category || 'Otros',
          condition: ({ new: 'Nuevo', like_new: 'Como nuevo', good: 'Buen estado', fair: 'Uso visible' })[item.conditionType] || item.conditionType,
          image: mediaUrl(item.imageUrl),
          seller: item.sellerName,
          location: 'Campus UPEC',
          rating: item.sellerRating ? Number(item.sellerRating).toFixed(1) : 'Nuevo',
        })))
        setDataError('')
      })
      .catch((error) => setDataError(error.message))
  }, [])

  useEffect(() => {
    if (!authUser) return
    apiRequest('/cart')
      .then((items) => setCart(items.map((item) => ({
        ...item,
        id: item.productId,
        price: Number(item.price),
        image: mediaUrl(item.imageUrl),
        seller: item.sellerName,
        location: 'Campus UPEC',
      }))))
      .catch((error) => showToast(error.message))
  }, [authUser, showToast])

  useEffect(() => {
    if (!selectedConversation || !authUser) return undefined
    let active = true
    const loadMessages = () => apiRequest(`/messages?withUserId=${selectedConversation.userId}`)
      .then((items) => { if (active) setChatMessages(items) })
      .catch((error) => { if (active) showToast(error.message) })
    loadMessages()
    const intervalId = window.setInterval(loadMessages, 3000)
    return () => { active = false; window.clearInterval(intervalId) }
  }, [selectedConversation, authUser, showToast])
  const filteredProducts = useMemo(() => products.filter((product) => {
    const matchesQuery = `${product.title} ${product.category} ${product.seller}`.toLowerCase().includes(query.toLowerCase())
    const matchesCategory = category === 'Todas' || product.category === category
    const matchesFavorites = currentNav !== 'Favoritos' || favorites.includes(product.id)
    return matchesQuery && matchesCategory && matchesFavorites
  }), [products, query, category, currentNav, favorites])

  const submitAuth = async (event) => {
    event.preventDefault()
    setAuthLoading(true)
    setAuthError('')
    const form = new FormData(event.currentTarget)
    const body = {
      email: form.get('email')?.toString().trim(),
      password: form.get('password')?.toString(),
    }
    if (authMode === 'register') body.fullName = form.get('fullName')?.toString().trim()
    try {
      const { data } = await api.post(`/auth/${authMode === 'login' ? 'login' : 'register'}`, body)
      window.localStorage.setItem(tokenKey, data.token)
      setAuthUser(data.user)
      setModal('')
      showToast(authMode === 'login' ? `Bienvenido, ${data.user.fullName}` : 'Cuenta creada correctamente')
    } catch (error) {
      setAuthError(error.response?.data?.error || error.message)
    } finally {
      setAuthLoading(false)
    }
  }

  const signOut = async () => {
    try {
      if (window.localStorage.getItem(tokenKey)) await api.post('/auth/logout')
    } finally {
      window.localStorage.removeItem(tokenKey)
      setAuthUser(null)
      setCart([])
      showToast('Sesión cerrada')
    }
  }

  const refreshProducts = async () => {
    const items = await apiRequest('/products')
    setProducts(items.map((item) => ({
      ...item,
      category: categoryNames[item.categoryId] || item.category || 'Otros',
      condition: ({ new: 'Nuevo', like_new: 'Como nuevo', good: 'Buen estado', fair: 'Uso visible' })[item.conditionType] || item.conditionType,
      image: mediaUrl(item.imageUrl),
      seller: item.sellerName,
      location: 'Campus UPEC',
      rating: item.sellerRating ? Number(item.sellerRating).toFixed(1) : 'Nuevo',
    })))
  }

  const addToCart = async (product) => {
    if (!authUser) {
      setAuthMode('login')
      setModal('auth')
      return
    }
    try {
      await apiRequest(`/cart/${product.id}`, { method: 'POST' })
      const items = await apiRequest('/cart')
      setCart(items.map((item) => ({ ...item, id: item.productId, price: Number(item.price), image: mediaUrl(item.imageUrl), seller: item.sellerName })))
      showToast('Producto añadido al carrito; todavía no está reservado')
    } catch (error) {
      showToast(error.message)
    }
  }

  const removeFromCart = async (productId) => {
    try {
      await apiRequest(`/cart/${productId}`, { method: 'DELETE' })
      setCart((items) => items.filter((product) => product.id !== productId))
    } catch (error) {
      showToast(error.message)
    }
  }

  const completeSimulatedPayment = async (event) => {
    event.preventDefault()
    if (!paymentProduct || paymentBusy) return
    if (paymentMethod === 'card_simulation') {
      const digits = cardNumber.replace(/\D/g, '')
      if (digits.length < 12 || !cardHolder.trim() || !/^\d{2}\/\d{2}$/.test(cardExpiry) || !/^\d{3,4}$/.test(cardCvv)) {
        showToast('Completa los datos ficticios de la tarjeta.')
        return
      }
    }
    setPaymentBusy(true)
    try {
      await apiRequest(`/cart/${paymentProduct.id}/reserve`, { method: 'POST' })
      const transaction = await apiRequest('/transactions', {
        method: 'POST',
        body: JSON.stringify({ productId: paymentProduct.id, type: 'purchase' }),
      })
      let result
      if (paymentMethod === 'transfer_simulation') {
        if (!transferProofNumber.trim()) {
          throw new Error('Ingresa el número de comprobante.')
        }
        if (!(transferProofFile instanceof File)) {
          throw new Error('Selecciona el comprobante de transferencia.')
        }
        const proof = new FormData()
        proof.append('proofNumber', transferProofNumber.trim())
        proof.append('file', transferProofFile)
        await apiRequest(`/transactions/${transaction.id}/payment-proof`, {
          method: 'POST',
          data: proof,
        })
        result = {
          paymentMethod,
          reference: transferProofNumber.trim(),
          status: 'pending_verification',
        }
      } else {
        result = await apiRequest(`/transactions/${transaction.id}/simulate-payment`, {
          method: 'POST',
          body: JSON.stringify({ method: paymentMethod }),
        })
      }
      setCart((items) => items.filter((item) => item.id !== paymentProduct.id))
      await refreshProducts()
      setPaymentResult({ paymentMethod: result.paymentMethod, reference: result.reference, status: result.status })
      setPaymentProduct(null)
      setModal('payment-success')
      setOrders((items) => [{ id: transaction.id, productId: paymentProduct.id, title: paymentProduct.title, status: result.status === 'pending_verification' ? 'pending_payment' : 'paid', amount: paymentProduct.price, paymentMethod: result.paymentMethod, reference: result.reference }, ...items])
    } catch (error) {
      showToast(error.message)
    } finally {
      setPaymentBusy(false)
    }
  }

  const startChat = async (seller) => {
    if (!authUser) {
      setAuthMode('login')
      setModal('auth')
      return
    }
    if (seller.sellerId === authUser.id) {
      showToast('Esta es tu propia publicación')
      return
    }
    const conversation = { userId: seller.sellerId, fullName: seller.sellerName }
    setSelectedConversation(conversation)
    setChatMessages([])
    setModal('')
    navigate('/mensajes')
  }

  const sendChatMessage = async (event) => {
    event.preventDefault()
    if (!selectedConversation || !chatDraft.trim()) return
    try {
      await apiRequest('/messages', {
        method: 'POST',
        body: JSON.stringify({ recipientId: selectedConversation.userId, body: chatDraft.trim() }),
      })
      setChatDraft('')
      setChatMessages(await apiRequest(`/messages?withUserId=${selectedConversation.userId}`))
    } catch (error) {
      showToast(error.message)
    }
  }

  const openProductEditor = (product = null) => {
    setEditingProduct(product)
    setModal('publish')
  }

  const saveAccount = async (event) => {
    event.preventDefault()
    setProfileBusy(true)
    setProfileError('')
    const form = new FormData(event.currentTarget)
    try {
      let updated = await apiRequest('/auth/me', {
        method: 'PATCH',
        body: JSON.stringify({
          fullName: form.get('fullName')?.toString().trim(),
          payoutBank: form.get('payoutBank')?.toString().trim() || null,
          payoutAccount: form.get('payoutAccount')?.toString().trim() || null,
          payoutHolder: form.get('payoutHolder')?.toString().trim() || null,
          payoutIdentifier: form.get('payoutIdentifier')?.toString().trim() || null,
        }),
      })
      const photo = form.get('photo')
      if (photo instanceof File && photo.size > 0) {
        const upload = new FormData()
        upload.append('file', photo)
        updated = await apiRequest('/auth/me/photo', { method: 'POST', data: upload })
      }
      setAuthUser(updated)
      setModal('')
      showToast('Datos de cuenta actualizados')
    } catch (error) {
      setProfileError(error.message)
    } finally {
      setProfileBusy(false)
    }
  }

  const openSettings = useCallback(async () => {
    try {
      setSettingsError('')
      setAdminSettings(await apiRequest('/admin/settings'))
    } catch (error) {
      setSettingsError(error.message)
    }
  }, [])

  const saveSetting = async (event) => {
    event.preventDefault()
    setSettingsBusy(true)
    setSettingsError('')
    try {
      await apiRequest(`/admin/settings/${encodeURIComponent(settingKey.trim())}`, {
        method: 'PUT',
        body: JSON.stringify({ password: settingsPassword, value: settingValue }),
      })
      setSettingsPassword('')
      setSettingKey('')
      setSettingValue('')
      await openSettings()
      showToast('Configuración guardada')
    } catch (error) {
      setSettingsError(error.message)
    } finally {
      setSettingsBusy(false)
    }
  }

  const moderateProduct = async (id, status) => {
    try {
      await apiRequest(`/admin/products/${id}/moderation`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      setPendingProducts((items) => items.filter((item) => item.id !== id))
      if (status === 'approved') await refreshProducts()
      showToast(status === 'approved' ? 'Publicación aprobada' : 'Publicación rechazada')
    } catch (error) {
      showToast(error.message)
    }
  }

  const deleteProduct = async (id) => {
    if (!window.confirm('¿Deseas eliminar esta publicación? Esta acción no se puede deshacer.')) return
    try {
      await apiRequest(`/products/${id}`, { method: 'DELETE' })
      setMyProducts((items) => items.filter((item) => item.id !== id))
      setProducts((items) => items.filter((item) => item.id !== id))
      await refreshProducts()
      showToast('Publicación eliminada')
    } catch (error) {
      showToast(error.message)
    }
  }

  const parseUserRoles = (value) => {
    if (!value) return []
    if (Array.isArray(value)) return value.filter(Boolean)
    return String(value).split(',').map((role) => role.trim()).filter(Boolean)
  }

  const updateUserStatus = async (id, status) => {
    try {
      await apiRequest(`/admin/users/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      setAdminUsers((items) => items.map((user) => user.id === id ? { ...user, accountStatus: status } : user))
      showToast(status === 'active' ? 'Usuario activado' : status === 'restricted' ? 'Usuario restringido' : 'Usuario suspendido')
    } catch (error) {
      showToast(error.message)
    }
  }

  const toggleAdminRole = async (user) => {
    if (!isSuperAdmin) {
      showToast('Solo el Super Admin puede cambiar roles de administración')
      return
    }
    const currentRoles = parseUserRoles(user.roles)
    const nextRoles = currentRoles.includes('ADMIN')
      ? currentRoles.filter((role) => role !== 'ADMIN')
      : [...new Set([...currentRoles, 'ADMIN'])]
    try {
      await apiRequest(`/admin/users/${user.id}/roles`, {
        method: 'PATCH',
        body: JSON.stringify({ roles: nextRoles.length ? nextRoles : ['BUYER', 'SELLER'] }),
      })
      setAdminUsers((items) => items.map((item) => item.id === user.id ? { ...item, roles: nextRoles.length ? nextRoles.join(',') : 'BUYER,SELLER' } : item))
      showToast(nextRoles.includes('ADMIN') ? 'Rol de administrador otorgado' : 'Rol de administrador retirado')
    } catch (error) {
      showToast(error.message)
    }
  }

  const toggleFavorite = (id) => {
    setFavorites((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])
  }

  const chooseNav = (label) => {
    setCategory('Todas')
    if (['Mis publicaciones', 'Mensajes', 'Mis compras', 'Administración', 'Configuración'].includes(label) && !authUser) {
      setAuthMode('login')
      setModal('auth')
      return
    }
    if (['Administración', 'Configuración'].includes(label) && !isAdmin) {
      showToast('No tienes permisos de administración')
      return
    }
    if (label === 'Administración') setAdminLoading(true)
    if (label === 'Mensajes') setSelectedConversation(null)
    const destinations = {
      Inicio: '/',
      Explorar: '/explorar',
      'Mis publicaciones': '/mis-publicaciones',
      Mensajes: '/mensajes',
      Favoritos: '/favoritos',
      'Mis compras': '/mis-compras',
      Ayuda: '/ayuda',
      Administración: '/administracion',
      Configuración: '/configuracion',
      'Mi cuenta': '/cuenta',
    }
    if (destinations[label]) navigate(destinations[label])
  }

  const submitProduct = async (event) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const title = form.get('title')?.toString().trim()
    const price = Number(form.get('price'))
    if (!title || !Number.isFinite(price) || price <= 0) return
    const selectedCategory = form.get('category')?.toString()
    const chosenCategory = categoryRecords.find((item) => categoryNames[item.id] === selectedCategory)
      || categoryRecords.find((item) => item.name === selectedCategory)
    if (!chosenCategory) {
      showToast('No se pudo determinar la categoría. Recarga e inténtalo otra vez.')
      return
    }
    try {
      const payload = {
        title,
        description: form.get('description')?.toString().trim() || title,
        categoryId: chosenCategory.id,
        price,
        conditionType: ({ Nuevo: 'new', 'Como nuevo': 'like_new', 'Buen estado': 'good' })[form.get('condition')] || 'good',
        listingType: 'sale',
      }
      const saved = await apiRequest(editingProduct ? `/products/${editingProduct.id}` : '/products', {
        method: editingProduct ? 'PUT' : 'POST',
        body: JSON.stringify({
          ...payload,
          // Keep the existing image when no file is chosen; the selected file is uploaded separately.
          imageUrl: null,
        }),
      })
      const image = form.get('image')
      if (image instanceof File && image.size > 0) {
        const upload = new FormData()
        upload.append('file', image)
        try {
          await apiRequest(`/products/${saved.id}/image`, { method: 'POST', data: upload })
        } catch (error) {
          showToast(`Publicación guardada, pero no se pudo cargar la imagen: ${error.message}`)
        }
      }
      const mine = await apiRequest('/products/mine')
      setMyProducts(mine)
      setEditingProduct(null)
      setModal('')
      navigate('/mis-publicaciones')
      showToast(editingProduct ? 'Cambios enviados para revisión' : 'Publicación enviada a revisión')
      refreshProducts().catch((error) => showToast(`Publicación guardada; no se pudo actualizar el catálogo: ${error.message}`))
    } catch (error) {
      showToast(error.message)
    }
  }

  const title = currentNav === 'Favoritos' ? 'Tus favoritos' : currentNav === 'Explorar' ? 'Explora el catálogo' : 'Catálogo de productos'

  useEffect(() => {
    if (!authResolved) return
    const page = routePages[location.pathname]
    if (!page) {
      navigate('/', { replace: true })
      return
    }
    const requiresSession = ['/mis-publicaciones', '/mensajes', '/mis-compras', '/cuenta', '/administracion', '/configuracion'].includes(location.pathname)
    if (requiresSession && !authUser) {
      navigate('/', { replace: true })
      return
    }
    if (['/administracion', '/configuracion'].includes(location.pathname) && !isAdmin) {
      navigate('/', { replace: true })
      return
    }
    if (location.pathname === '/mis-publicaciones') {
      apiRequest('/products/mine').then(setMyProducts).catch((error) => showToast(error.message))
    }
    if (location.pathname === '/mensajes') {
      apiRequest('/messages/conversations').then(setConversations).catch((error) => showToast(error.message))
    }
    if (location.pathname === '/mis-compras') {
      apiRequest('/transactions').then(setOrders).catch((error) => showToast(error.message))
    }
    if (location.pathname === '/administracion') {
      Promise.all([
        apiRequest('/admin/products/pending'),
        apiRequest('/admin/users'),
      ])
        .then(([pending, users]) => {
          setPendingProducts(pending)
          setAdminUsers(users)
        })
        .catch((error) => showToast(error.message))
        .finally(() => setAdminLoading(false))
    }
    if (location.pathname === '/configuracion') {
      apiRequest('/admin/settings').then(setAdminSettings).catch((error) => setSettingsError(error.message))
    }
  }, [location.pathname, authResolved, authUser, isAdmin, navigate, showToast])

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="topbar wrap">
          <button className="brand" onClick={() => chooseNav('Inicio')} aria-label="UPEC Market, inicio">
            <span className="brand-mark"><span>🛒</span></span>
            <span className="brand-copy"><strong>UPEC Market</strong><small>Marketplace para estudiantes UPEC</small></span>
          </button>

          <label className="search-box">
            <input value={query} onChange={(event) => { setQuery(event.target.value); setActiveNav('Explorar'); navigate('/explorar', { replace: true }) }} placeholder="Buscar productos, categorías..." aria-label="Buscar productos" />
            <span aria-hidden="true">⌕</span>
          </label>

          <div className="header-actions">
            <button className="icon-action cart-action" onClick={() => setModal('cart')} aria-label={`Carrito, ${cart.length} productos`}>
              <span>🛍</span>{cart.length > 0 && <b>{cart.length}</b>}
            </button>
            {authUser ? <>
              <button className="user-greeting" onClick={() => navigate('/cuenta')} aria-label={`Editar cuenta de ${authUser.fullName}`}>
                <img className="header-avatar" src={mediaUrl(authUser.profilePhotoUrl)} alt="" />Mi cuenta
              </button>
              <button className="login-button" onClick={signOut}>Salir</button>
            </> : <>
              <button className="login-button" onClick={() => { setAuthMode('login'); setAuthError(''); setModal('auth') }}>Ingresar</button>
              <button className="signup-button" onClick={() => { setAuthMode('register'); setAuthError(''); setModal('auth') }}>Crear cuenta</button>
            </>}
          </div>
        </div>
        <nav className="nav-row">
          <div className="wrap nav-items">
            {[...navItems, ...(isAdmin ? [['Administración', '⚙'], ['Configuración', '⚙']] : [])].map(([label, icon]) => (
              <button key={label} className={`nav-link ${currentNav === label ? 'active' : ''}`} onClick={() => chooseNav(label)}>
                <span aria-hidden="true">{icon}</span>{label}
                {label === 'Mensajes' && <i className="nav-dot" />}
              </button>
            ))}
          </div>
        </nav>
      </header>

      <main>
        {currentNav === 'Inicio' && !query && (
          <>
            <section className="hero" style={{ backgroundImage: `linear-gradient(90deg, rgba(10,18,34,.72), rgba(10,18,34,.22)), url(${heroImage})` }}>
              <div className="wrap hero-content">
                <p className="hero-kicker">EL MARKETPLACE DE TU COMUNIDAD</p>
                <h1>Compra, vende e intercambia<br /><em>entre estudiantes de la UPEC</em></h1>
                <p className="hero-description">Encuentra lo que necesitas, vende lo que ya no usas e intercambia de forma segura con tu comunidad.</p>
                <button className="primary-button" onClick={() => chooseNav('Explorar')}>Explorar productos <span>→</span></button>
              </div>
              <div className="hero-pagination"><i className="selected" /><i /><i /></div>
            </section>

            <section className="benefits wrap" aria-label="Beneficios">
              <Benefit icon="♧" title="Comunidad UPEC" text="Registro con correo institucional." />
              <Benefit icon="$" title="Ahorra dinero" text="Encuentra productos a precios accesibles." />
              <Benefit icon="♻" title="Reutiliza y cuida el planeta" text="Dale una segunda vida a los productos." />
              <Benefit icon="♙" title="Comunidad UPEC" text="Conecta con estudiantes de todas las carreras." />
            </section>
          </>
        )}

        <div className="wrap content-area">
          {(currentNav === 'Inicio' || currentNav === 'Explorar' || currentNav === 'Favoritos') && (
            <>
              {currentNav === 'Inicio' && !query && (
                <section className="categories-section">
                  <div className="section-heading"><div><p className="eyebrow">ENCUENTRA LO QUE BUSCAS</p><h2>Categorías</h2></div><button className="text-button" onClick={() => chooseNav('Explorar')}>Ver todas <span>→</span></button></div>
                  <div className="category-grid">
                    {categories.map((item) => <button key={item.name} className="category-card" onClick={() => { setCategory(item.name); setActiveNav('Explorar'); navigate('/explorar') }}>
                      <span className={`category-icon ${item.color}`}>{item.icon}</span><strong>{item.name}</strong><small>Descubre productos</small>
                    </button>)}
                  </div>
                </section>
              )}

              <section className="catalog-section">
                <div className="section-heading">
                  <div><p className="eyebrow">{currentNav === 'Favoritos' ? 'GUARDADOS PARA DESPUÉS' : 'SEGUNDA VIDA, GRANDES HALLAZGOS'}</p><h2>{title}{category !== 'Todas' ? <span className="heading-filter"> / {category}</span> : ''}</h2></div>
                  {currentNav === 'Inicio' && <button className="text-button" onClick={() => chooseNav('Explorar')}>Ver catálogo <span>→</span></button>}
                </div>
                {currentNav === 'Explorar' && <div className="filter-row">
                  {['Todas', ...categories.map((item) => item.name)].map((item) => <button key={item} className={`filter-chip ${category === item ? 'selected' : ''}`} onClick={() => setCategory(item)}>{item}</button>)}
                  <span className="result-count">{filteredProducts.length} productos</span>
                </div>}
                {dataError && products.length === 0 && <div className="api-warning" role="status">No se pudo cargar el catálogo: {dataError}</div>}
                {filteredProducts.length ? <div className="product-grid">
                  {filteredProducts.map((product) => <ProductCard key={product.id} product={product} favorite={favorites.includes(product.id)} inCart={cart.some((item) => item.id === product.id)} onFavorite={() => toggleFavorite(product.id)} onAdd={() => addToCart(product)} onOpen={() => setModal(`product-${product.id}`)} />)}
                </div> : <div className="empty-state"><span>⌕</span><h3>No encontramos productos</h3><p>Prueba otra búsqueda o explora todas las categorías.</p><button className="primary-button" onClick={() => { setQuery(''); setCategory('Todas'); setActiveNav('Explorar') }}>Ver catálogo completo</button></div>}
              </section>
            </>
          )}
        </div>

        {currentNav === 'Mensajes' && <section className="wrap module-page">
          <p className="eyebrow">MENSAJERÍA DE UPEC MARKET</p><h1>Conversaciones</h1>
          <p className="module-intro">Tus mensajes se guardan en la base de datos y se actualizan automáticamente.</p>
          <div className="messages-layout">
            <aside className="messages-sidebar">
              <h2>Bandeja de entrada</h2>
              {conversations.length ? conversations.map((conversation) => <button className={`conversation-row ${selectedConversation?.userId === conversation.userId ? 'selected' : ''}`} key={conversation.userId} onClick={() => { setSelectedConversation(conversation); setChatMessages([]) }}>
                <span className="seller-avatar">{conversation.fullName?.[0] || 'U'}</span><span className="conversation-copy"><strong>{conversation.fullName}</strong><small>{conversation.lastMessage || 'Abrir conversación'}</small></span>{Number(conversation.unreadCount) > 0 && <b className="unread-badge">{conversation.unreadCount}</b>}
              </button>) : <div className="module-empty"><strong>Aún no tienes conversaciones</strong><p>Abre un producto y elige “Chat con vendedor” para iniciar una conversación.</p><button className="secondary-button" onClick={() => navigate('/explorar')}>Explorar productos</button></div>}
            </aside>
            <section className="messages-thread">
              {selectedConversation ? <>
                <div className="chat-heading"><span className="seller-avatar">{selectedConversation.fullName?.[0] || 'U'}</span><div><strong>{selectedConversation.fullName}</strong><small>Chat UPEC Market · actualización automática</small></div></div>
                <div className="chat-messages">{chatMessages.length ? chatMessages.map((message) => <div className={`chat-bubble ${message.senderId === authUser?.id ? 'mine' : ''}`} key={message.id}><p>{message.body}</p><small>{new Date(message.createdAt).toLocaleString('es-EC', { dateStyle: 'short', timeStyle: 'short' })}</small></div>) : <div className="chat-empty">Envía el primer mensaje para coordinar con esta persona.</div>}</div>
                <form className="chat-compose" onSubmit={sendChatMessage}><input value={chatDraft} onChange={(event) => setChatDraft(event.target.value)} maxLength="5000" placeholder="Escribe un mensaje..." aria-label="Mensaje" /><button className="primary-button" disabled={!chatDraft.trim()}>Enviar</button></form>
              </> : <div className="module-empty"><strong>Selecciona una conversación</strong><p>Elige una conversación de la bandeja para ver y enviar mensajes.</p></div>}
            </section>
          </div>
        </section>}

        {currentNav === 'Mis publicaciones' && <section className="wrap module-page">
          <div className="module-heading"><div><p className="eyebrow">TU ESPACIO DE VENTA</p><h1>Mis publicaciones</h1><p className="module-intro">Las publicaciones nuevas o editadas quedan pendientes hasta que un administrador las apruebe.</p></div><button className="primary-button" onClick={() => openProductEditor()}>+ Publicar producto</button></div>
          {myProducts.length ? <div className="module-list">{myProducts.map((item) => <article className="module-list-row" key={item.id}>
            {item.imageUrl && <img className="module-product-image" src={mediaUrl(item.imageUrl)} alt="" />}
            <div className="module-row-copy"><strong>{item.title}</strong><small>{categoryNames[item.categoryId] || item.category} · ${Number(item.price).toFixed(2)}</small><small>{item.status === 'pending' ? 'Visible solo para ti y moderadores hasta su aprobación.' : ''}</small></div>
            <span className={`order-status ${item.status}`}>{productStatus(item.status)}</span>
            <div className="admin-product-actions">
              {['pending', 'approved', 'rejected'].includes(item.status) && <button className="edit-product-button" onClick={() => openProductEditor(item)}>Editar</button>}
              {['pending', 'approved', 'rejected'].includes(item.status) && <button className="reject-button" onClick={() => deleteProduct(item.id)}>Eliminar</button>}
            </div>
          </article>)}</div> : <div className="module-empty"><strong>Aún no tienes publicaciones.</strong><p>Al enviar un producto, aparecerá aquí con el estado de revisión.</p><button className="primary-button" onClick={() => openProductEditor()}>Publicar producto</button></div>}
        </section>}

        {currentNav === 'Mis compras' && <section className="wrap module-page">
          <p className="eyebrow">REGISTRO DE TRANSACCIONES</p><h1>Mis compras y ventas</h1><p className="module-intro">Consulta el historial y el estado de tus transacciones.</p>
          {orders.length ? <div className="module-list">{orders.map((order) => <article className="module-list-row" key={order.id}><div className="module-row-copy"><strong>{order.title}</strong><small>{order.type === 'exchange' ? 'Intercambio' : 'Compra'} · {order.createdAt ? new Date(order.createdAt).toLocaleDateString('es-EC') : 'Ahora'}</small></div><span className={`order-status ${order.status}`}>{transactionStatus(order.status)}</span><b>${Number(order.amount || 0).toFixed(2)}</b></article>)}</div> : <div className="module-empty"><strong>No tienes transacciones registradas todavía.</strong></div>}
        </section>}

        {currentNav === 'Administración' && <section className="wrap module-page">
          <div className="module-heading"><div><p className="eyebrow">MODERACIÓN</p><h1>Publicaciones pendientes</h1><p className="module-intro">Las publicaciones aprobadas pasan al catálogo público.</p></div><button className="secondary-button" onClick={() => navigate('/configuracion')}>Configuración global</button></div>
          {pendingProducts.length ? <div className="module-list">{pendingProducts.map((item) => <article className="admin-product module-admin-product" key={item.id}>
            {item.imageUrl && <img className="module-product-image" src={mediaUrl(item.imageUrl)} alt="" />}
            <div><strong>{item.title}</strong><small>{categoryNames[item.categoryId] || item.category} · {item.sellerName} · ${Number(item.price).toFixed(2)}</small><p>{item.description}</p></div>
            <div className="admin-product-actions"><button className="approve-button" onClick={() => moderateProduct(item.id, 'approved')}>Aprobar</button><button className="reject-button" onClick={() => moderateProduct(item.id, 'rejected')}>Rechazar</button></div>
          </article>)}</div> : <div className="module-empty"><strong>{adminLoading ? 'Cargando publicaciones...' : 'No hay publicaciones pendientes.'}</strong></div>}

          <div className="module-heading" style={{ marginTop: '2rem' }}><div><p className="eyebrow">USUARIOS</p><h2>Gestión de cuentas</h2><p className="module-intro">Los administradores restringen cuentas y el Super Admin puede asignar permisos de administración.</p></div></div>
          {adminUsers.length ? <div className="module-list">{adminUsers.map((user) => {
            const roles = parseUserRoles(user.roles)
            return <article className="module-list-row" key={user.id}>
              <div className="module-row-copy">
                <strong>{user.fullName || user.email}</strong>
                <small>{user.email}</small>
                <small>Roles: {roles.length ? roles.join(', ') : 'Sin roles'}</small>
                {(user.payoutBank || user.payoutAccount) && <small>Cobro: {user.payoutBank || 'Banco'} · {user.payoutAccount || 'Sin cuenta'} · {user.payoutHolder || ''}</small>}
              </div>
              <div className="admin-product-actions">
                <select
                  value={user.accountStatus || 'active'}
                  onChange={(event) => updateUserStatus(user.id, event.target.value)}
                  aria-label={`Cambiar estado de ${user.fullName || user.email}`}
                >
                  <option value="active">Activo</option>
                  <option value="restricted">Restringido</option>
                  <option value="suspended">Suspendido</option>
                </select>
                {isSuperAdmin && <button className={roles.includes('ADMIN') ? 'secondary-button' : 'approve-button'} onClick={() => toggleAdminRole(user)}>{roles.includes('ADMIN') ? 'Quitar admin' : 'Otorgar admin'}</button>}
              </div>
            </article>
          })}</div> : <div className="module-empty"><strong>No hay usuarios registrados.</strong></div>}
        </section>}

        {currentNav === 'Configuración' && <section className="wrap module-page">
          <p className="eyebrow">PANEL DE ADMINISTRACIÓN</p><h1>Configuración global</h1><p className="module-intro">Cada cambio exige confirmar tu contraseña y queda registrado en la auditoría.</p>
          <div className="settings-layout">
            <form className="settings-form" onSubmit={saveSetting}>
              <h2>{adminSettings.some((setting) => setting.settingKey === settingKey) ? 'Editar configuración' : 'Agregar configuración'}</h2>
              <label>Clave<input value={settingKey} onChange={(event) => setSettingKey(event.target.value)} required maxLength="100" pattern="[a-zA-Z0-9._-]+" placeholder="Ej. marketplace.name" /></label>
              <label>Valor<input value={settingValue} onChange={(event) => setSettingValue(event.target.value)} required maxLength="5000" placeholder="Valor de configuración" /></label>
              <label>Confirma tu contraseña<input type="password" value={settingsPassword} onChange={(event) => setSettingsPassword(event.target.value)} required maxLength="72" autoComplete="current-password" /></label>
              {settingsError && <p className="auth-error" role="alert">{settingsError}</p>}
              <button className="primary-button" disabled={settingsBusy}>{settingsBusy ? 'Guardando...' : 'Guardar cambio seguro'}</button>
            </form>
            <div className="settings-list"><h2>Valores configurados</h2>{adminSettings.length ? adminSettings.map((setting) => <article className="setting-row" key={setting.settingKey}><div><strong>{setting.settingKey}</strong><small>{setting.settingValue}</small></div><button className="edit-product-button" onClick={() => { setSettingKey(setting.settingKey); setSettingValue(setting.settingValue === '[REDACTED]' ? '' : setting.settingValue) }}>Editar</button></article>) : <p className="module-empty">{settingsError || 'No hay valores configurados todavía.'}</p>}</div>
          </div>
        </section>}

        {currentNav === 'Mi cuenta' && <section className="wrap module-page">
          <p className="eyebrow">PERFIL PERSONAL</p><h1>Editar cuenta</h1><p className="module-intro">Actualiza tu nombre y foto de perfil.</p>
          <form className="account-page-form" onSubmit={saveAccount}>
            <img className="account-page-avatar" src={mediaUrl(authUser?.profilePhotoUrl)} alt="Foto de perfil actual" />
            <label>Nombre completo<input name="fullName" required maxLength="120" defaultValue={authUser?.fullName || ''} /></label>
            <label>Correo institucional<input value={authUser?.email || ''} readOnly /></label>
            <label>Subir nueva foto<input name="photo" type="file" accept="image/jpeg,image/png,image/webp" /><small>JPG, PNG o WebP · máximo 5 MB</small></label>
            <div className="payout-fields">
              <p className="eyebrow">DATOS PARA COBROS</p>
              <p className="form-note">El vendedor registra aquí dónde debe enviarse el dinero después de validar una compra.</p>
              <label>Banco<input name="payoutBank" maxLength="80" defaultValue={authUser?.payoutBank || ''} placeholder="Banco Pichincha / Guayaquil" /></label>
              <label>Número de cuenta<input name="payoutAccount" maxLength="120" defaultValue={authUser?.payoutAccount || ''} placeholder="Cuenta ficticia o real registrada por el usuario" /></label>
              <label>Titular<input name="payoutHolder" maxLength="120" defaultValue={authUser?.payoutHolder || ''} placeholder="Nombre del titular" /></label>
              <label>Identificador de cobro<input name="payoutIdentifier" maxLength="120" defaultValue={authUser?.payoutIdentifier || ''} placeholder="Usuario, celular o identificador de billetera" /></label>
            </div>
            {profileError && <p className="auth-error" role="alert">{profileError}</p>}
            <button className="primary-button" disabled={profileBusy}>{profileBusy ? 'Guardando...' : 'Guardar cambios'}</button>
          </form>
        </section>}

        {currentNav === 'Ayuda' && <section className="wrap module-page">
          <p className="eyebrow">SOPORTE UPEC MARKET</p><h1>Ayuda y preguntas frecuentes</h1>
          <div className="faq-list module-faq"><details><summary>¿Quién puede usar UPEC Market?</summary><p>Estudiantes con correo institucional @upec.edu.ec.</p></details><details><summary>¿Cómo publico un producto?</summary><p>Envía el producto a revisión. Un administrador debe aprobarlo antes de publicarse en el catálogo.</p></details><details><summary>¿Por qué no veo mi publicación en el catálogo?</summary><p>Las publicaciones nuevas o editadas quedan pendientes de aprobación. Puedes verlas en “Mis publicaciones”.</p></details><details><summary>¿Cuándo se reserva un producto?</summary><p>Agregar un producto al carrito no lo bloquea. La reserva temporal se activa al iniciar la compra.</p></details><details><summary>¿Cómo contacto al equipo?</summary><p>Escríbenos a soporte@upec.edu.ec desde tu correo institucional.</p></details></div><a className="primary-button" href="mailto:soporte@upec.edu.ec">Contactar soporte</a>
        </section>}

        {['Inicio', 'Explorar', 'Favoritos'].includes(currentNav) && <section className="trust-banner">
          <div className="wrap trust-content"><div><p className="eyebrow">COMPRA CON CONFIANZA</p><h2>Tu comunidad, más cerca que nunca.</h2><p>Conecta con estudiantes UPEC y encuentra oportunidades en cada publicación.</p></div><button className="outline-button" onClick={() => { setAuthMode('register'); setModal('auth') }}>Únete a la comunidad <span>→</span></button></div>
        </section>}
      </main>

      <footer className="site-footer"><div className="wrap footer-inner"><div className="footer-brand"><span className="brand-mark"><span>🛒</span></span><strong>UPEC Market</strong><span>Un marketplace creado para nuestra comunidad.</span></div><div className="footer-links"><button onClick={() => navigate('/ayuda')}>Ayuda y preguntas frecuentes</button><button onClick={() => navigate('/ayuda')}>Contacto</button><span>© 2025 UPEC Market</span></div></div></footer>

      {modal && <Modal onClose={() => setModal('')}>
        {modal === 'auth' && <div className="modal-content auth-modal">
          <div className="modal-brand"><span className="brand-mark"><span>🛒</span></span></div>
          <p className="eyebrow">BIENVENIDO A UPEC MARKET</p><h2>{authMode === 'login' ? 'Inicia sesión' : 'Crea tu cuenta'}</h2><p className="modal-subtitle">Conecta con estudiantes de tu comunidad.</p>
          <form onSubmit={submitAuth}>
            {authMode === 'register' && <label>Nombre completo<input name="fullName" required maxLength="120" placeholder="Tu nombre y apellido" /></label>}
            <label>Correo institucional<input name="email" type="email" required pattern=".+@upec\.edu\.ec" placeholder="usuario@upec.edu.ec" /></label>
            <label>Contraseña<input name="password" type="password" required minLength="8" maxLength="72" placeholder="Mínimo 8 caracteres" /></label>
            {authError && <p className="auth-error" role="alert">{authError}</p>}
            <button className="primary-button full-button" disabled={authLoading}>{authLoading ? 'Conectando...' : authMode === 'login' ? 'Ingresar' : 'Crear cuenta institucional'}</button>
          </form>
          <p className="switch-auth">{authMode === 'login' ? '¿Aún no tienes cuenta?' : '¿Ya tienes una cuenta?'} <button onClick={() => setAuthMode(authMode === 'login' ? 'register' : 'login')}>{authMode === 'login' ? 'Regístrate' : 'Inicia sesión'}</button></p>
          <p className="form-note">Solo se permiten correos con dominio @upec.edu.ec</p>
        </div>}

        {modal === 'publish' && <div className="modal-content"><p className="eyebrow">{editingProduct ? 'EDITA TU PUBLICACIÓN' : 'COMPARTE CON LA COMUNIDAD'}</p><h2>{editingProduct ? 'Editar producto' : 'Publica un producto'}</h2><p className="modal-subtitle">Tu publicación será revisada antes de aparecer en el catálogo.</p>
          <form className="stack-form" onSubmit={submitProduct}>
            <label>Nombre del producto<input name="title" required maxLength="160" placeholder="Ej. Libro de cálculo" defaultValue={editingProduct?.title || ''} /></label>
            <div className="form-two"><label>Categoría<select name="category" defaultValue={categoryNames[editingProduct?.categoryId] || 'Tecnología'}>{categories.map((item) => <option key={item.name}>{item.name}</option>)}</select></label><label>Precio (USD)<input name="price" type="number" min="0" step="0.01" required placeholder="0.00" defaultValue={editingProduct?.price ?? ''} /></label></div>
            <label>Estado<select name="condition" defaultValue={({ new: 'Nuevo', like_new: 'Como nuevo', good: 'Buen estado', fair: 'Buen estado' })[editingProduct?.conditionType] || 'Como nuevo'}><option>Como nuevo</option><option>Buen estado</option><option>Nuevo</option></select></label>
            <label>Descripción<textarea name="description" rows="3" required placeholder="Cuéntales más sobre tu producto..." defaultValue={editingProduct?.description || ''} /></label>
            {editingProduct?.imageUrl && <img className="h-36 w-full rounded-lg object-cover" src={mediaUrl(editingProduct.imageUrl)} alt="Imagen actual del producto" />}
            <label>Imagen del producto<input name="image" type="file" accept="image/jpeg,image/png,image/webp" className="file:mr-3 file:rounded-md file:border-0 file:bg-upec-mint file:px-3 file:py-2 file:text-xs file:font-bold file:text-upec-green" /><small className="form-note">Puedes elegir JPG, PNG o WebP (máximo 5 MB). La edición de imagen requiere nueva aprobación.</small></label>
            <button className="primary-button full-button">{editingProduct ? 'Guardar y enviar a revisión' : 'Enviar a revisión'} <span>→</span></button>
          </form>
        </div>}

        {modal === 'cart' && <div className="modal-content"><p className="eyebrow">TU SELECCIÓN</p><h2>Carrito de compras</h2><p className="modal-subtitle">Añadir al carrito no reserva el producto. Activa una reserva al coordinar con el vendedor.</p>
          {cart.length ? <><div className="cart-list">{cart.map((item) => <div className="cart-item" key={item.id}><img src={item.image} alt="" /><div><strong>{item.title}</strong><small>{item.seller} · {item.location || 'Campus UPEC'}</small></div><b>${Number(item.price).toFixed(2)}</b><button aria-label={`Quitar ${item.title}`} onClick={() => removeFromCart(item.id)}>×</button><button className="cart-pay-button" onClick={() => { setPaymentProduct(item); setModal('payment') }}>Pagar</button></div>)}</div><div className="cart-total"><span>Total estimado</span><strong>${cart.reduce((sum, item) => sum + Number(item.price), 0).toFixed(2)}</strong></div><p className="form-note">Pago de demostración: no se realizará ningún cobro real.</p></> : <div className="empty-cart"><span>🛍</span><p>Tu carrito está vacío</p><button className="text-button" onClick={() => setModal('')}>Explorar productos →</button></div>}
        </div>}

        {modal === 'payment' && paymentProduct && <div className="modal-content"><p className="eyebrow">ENTORNO DE PRUEBA</p><h2>Pago simulado</h2><p className="modal-subtitle">No ingreses datos bancarios: esta pantalla no cobra dinero real. Elige un método ficticio para registrar la compra de prueba.</p>
          <div className="payment-summary"><span>{paymentProduct.title}</span><strong>${(Number(paymentProduct.price) + Number(paymentConfig.feeFixed || 0) + Number(paymentProduct.price) * Number(paymentConfig.feePercent || 0) / 100).toFixed(2)} USD</strong></div>
          <form className="payment-options" onSubmit={completeSimulatedPayment}>
            {[['card_simulation', 'Tarjeta de crédito/débito simulada', 'Respuesta aprobada de prueba'], ['transfer_simulation', 'Transferencia bancaria simulada', 'Confirmación inmediata ficticia'], ['deuna_simulation', 'Deuna! simulado', 'Sin conexión con Deuna! real']].map(([value, label, description]) => <label key={value} className={`payment-option ${paymentMethod === value ? 'chosen' : ''}`}><input type="radio" name="paymentMethod" value={value} checked={paymentMethod === value} onChange={() => setPaymentMethod(value)} /><span><strong>{label}</strong><small>{description}</small></span><i>✓</i></label>)}
            {paymentMethod === 'card_simulation' && <div className="payment-details card-details">
              <div className="fake-card"><span>UPEC MARKET DEMO</span><strong>{cardNumber ? cardNumber.replace(/\D/g, '').replace(/(\d{4})(?=\d)/g, '$1 ') : '•••• •••• •••• 4242'}</strong><div><small>{cardHolder.trim() || 'ESTUDIANTE UPEC'}</small><small>{cardExpiry || '12/30'}</small></div></div>
              <div className="form-two">
                <label>Número de tarjeta<input inputMode="numeric" autoComplete="cc-number" value={cardNumber} onChange={(event) => setCardNumber(event.target.value)} maxLength="19" placeholder="4242 4242 4242 4242" required /></label>
                <label>Nombre del titular<input autoComplete="cc-name" value={cardHolder} onChange={(event) => setCardHolder(event.target.value)} maxLength="80" placeholder="Nombre de prueba" required /></label>
                <label>Fecha de vencimiento<input inputMode="numeric" value={cardExpiry} onChange={(event) => setCardExpiry(event.target.value)} maxLength="5" placeholder="12/30" required /></label>
                <label>CVV<input inputMode="numeric" autoComplete="cc-csc" value={cardCvv} onChange={(event) => setCardCvv(event.target.value)} maxLength="4" placeholder="123" required /></label>
              </div>
              <small>Datos simulados. No registres una tarjeta real.</small>
            </div>}
            {paymentMethod === 'transfer_simulation' && <div className="payment-details transfer-details">
              <label>Banco para realizar la transferencia<select value={selectedBank} onChange={(event) => setSelectedBank(Number(event.target.value))}>{paymentConfig.banks.map((bank, index) => <option key={`${bank.name}-${index}`} value={index}>{bank.name}</option>)}</select></label>
              <div className="bank-demo"><span>Cuenta de UPEC Market</span><strong>{paymentConfig.banks[selectedBank]?.name || 'Banco'} · {paymentConfig.banks[selectedBank]?.holder || 'UPEC Market Demo'}</strong></div>
              <div className="form-two">
                <label>Banco<input value={paymentConfig.banks[selectedBank]?.name || ''} readOnly /></label>
                <label>Cuenta<input value={paymentConfig.banks[selectedBank]?.account || ''} readOnly /></label>
                <label>Beneficiario<input value={paymentConfig.banks[selectedBank]?.holder || 'UPEC Market Demo'} readOnly /></label>
                <label>Referencia<input value={`UPEC-${paymentProduct.id.toString().padStart(6, '0')}`} readOnly /></label>
              </div>
              <label>Número de comprobante<input value={transferProofNumber} onChange={(event) => setTransferProofNumber(event.target.value)} required maxLength="120" placeholder="Ej. 1234567890" /></label>
              <label>Comprobante de transferencia<input type="file" accept=".jpg,.jpeg,.png,.webp,.pdf,image/jpeg,image/png,image/webp,application/pdf" onChange={(event) => setTransferProofFile(event.currentTarget.files?.[0] || null)} required /></label>
              <small>Datos bancarios ficticios · no realices ninguna transferencia.</small>
            </div>}
            {paymentMethod === 'deuna_simulation' && <div className="payment-details qr-details">
              <div className="fake-qr" role="img" aria-label="Código QR proporcionado para Deuna!">
                <img src={deunaQrImage} alt="QR de pago Deuna!" />
              </div>
              <div><strong>Escanea el QR para pagar</strong><small>Este entorno es demostrativo. Conecta una pasarela real antes de cobrar.</small><b>{paymentConfig.deunaLabel || 'UPEC MARKET'}</b></div>
            </div>}
            <div className="simulation-notice">Modo demostración · No se procesa ni almacena información de pago.</div>
            <button className="primary-button full-button" disabled={paymentBusy}>{paymentBusy ? 'Procesando simulación...' : `Confirmar pago de $${Number(paymentProduct.price).toFixed(2)}`}</button>
          </form>
        </div>}

        {modal === 'payment-success' && <div className="modal-content success-modal"><span className="success-mark">✓</span><p className="eyebrow">{paymentResult?.status === 'pending_verification' ? 'COMPROBANTE ENVIADO' : 'TRANSACCIÓN DE PRUEBA'}</p><h2>{paymentResult?.status === 'pending_verification' ? 'Pago pendiente de verificación' : 'Pago simulado exitoso'}</h2><p className="modal-subtitle">{paymentResult?.status === 'pending_verification' ? 'El comprobante fue registrado. El pago se verifica antes de liberar el saldo al vendedor.' : 'No se realizó ningún cobro real. La compra quedó registrada en tu historial.'}</p>{paymentResult && <div className="payment-receipt"><span>Método</span><strong>{{ card_simulation: 'Tarjeta', transfer_simulation: 'Transferencia', deuna_simulation: 'Deuna!' }[paymentResult.paymentMethod]}</strong><span>Referencia / comprobante</span><strong>{paymentResult.reference}</strong>{paymentResult.status === 'pending_verification' && <><span>Estado</span><strong>Pendiente de verificación</strong></>}</div>}<button className="primary-button full-button" onClick={() => { setPaymentResult(null); setTransferProofNumber(''); setTransferProofFile(null); setCardNumber(''); setCardHolder(''); setCardExpiry(''); setCardCvv(''); setModal(''); navigate('/mis-compras') }}>Ver mis compras</button><button className="secondary-button full-button" onClick={() => { setPaymentResult(null); setTransferProofNumber(''); setTransferProofFile(null); setCardNumber(''); setCardHolder(''); setCardExpiry(''); setCardCvv(''); setModal('') }}>Volver al catálogo</button></div>}

        {modal.startsWith('product-') && (() => {
          const product = products.find((item) => item.id === Number(modal.slice(8)))
          if (!product) return null
          return <div className="product-detail"><img src={product.image} alt={product.title} /><div className="product-detail-info"><span className="product-tag">{product.category}</span><h2>{product.title}</h2><p className="detail-price">${Number(product.price).toFixed(2)}</p><p>{product.condition} · Disponible en {product.location}</p><div className="seller-card"><span className="seller-avatar">{product.seller?.[0] || 'U'}</span><div><strong>{product.seller}</strong><small>★ {product.rating} · Estudiante UPEC</small></div><span className="verified">✓ Verificado</span></div><p className="detail-note">Coordina la entrega y los detalles directamente con el vendedor. El producto no se reserva al añadirlo al carrito.</p><button className="primary-button full-button" onClick={() => { addToCart(product); setModal('') }}>Añadir al carrito <span>→</span></button><button className="secondary-button full-button" onClick={() => startChat(product)}>Chat con vendedor</button></div></div>
        })()}
      </Modal>}
      {toast && <div className="toast" role="status"><span>✓</span>{toast}</div>}
    </div>
  )
}

function Benefit({ icon, title, text }) {
  return <div className="benefit-item"><span className="benefit-icon">{icon}</span><span><strong>{title}</strong><small>{text}</small></span></div>
}

function ProductCard({ product, favorite, inCart, onFavorite, onAdd, onOpen }) {
  return <article className="product-card">
    <button className="product-image-button" onClick={onOpen} aria-label={`Ver ${product.title}`}><img className="product-image" src={product.image} alt={product.title} loading="lazy" /><span className="condition-tag">{product.pending ? 'En revisión' : product.condition}</span></button>
    <button className={`favorite-button ${favorite ? 'is-favorite' : ''}`} onClick={onFavorite} aria-label={favorite ? 'Quitar de favoritos' : 'Añadir a favoritos'}>{favorite ? '♥' : '♡'}</button>
    <div className="product-info"><span className="product-category">{product.category}</span><button className="product-title" onClick={onOpen}>{product.title}</button><div className="product-meta"><span>★ {product.rating}</span><span>·</span><span>{product.location}</span></div><div className="product-bottom"><strong>${product.price.toFixed(2)}</strong><button className={`add-button ${inCart ? 'added' : ''}`} onClick={onAdd} aria-label="Añadir al carrito">{inCart ? '✓' : '+'}</button></div></div>
  </article>
}

function Modal({ children, onClose }) {
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
    <section className="modal-panel" role="dialog" aria-modal="true"><button className="modal-close" onClick={onClose} aria-label="Cerrar">×</button>{children}</section>
  </div>
}

function transactionStatus(status) {
  return ({
    pending_payment: 'Pendiente de pago',
    paid: 'Pagado',
    in_transit: 'En camino',
    delivered: 'Entregado',
    exchanged: 'Intercambiado',
    cancelled: 'Cancelado',
  })[status] || status
}

function productStatus(status) {
  return ({
    pending: 'Pendiente',
    approved: 'Aprobado',
    rejected: 'Rechazado',
    reserved: 'Reservado',
    sold: 'Vendido',
    exchanged: 'Intercambiado',
    deleted: 'Eliminado',
  })[status] || status
}

export default App
