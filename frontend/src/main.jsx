import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App'
import './index.css'

const container = document.getElementById('root')
if (!container) {
	console.error('Root element #root not found. App not mounted.')
} else {
	createRoot(container).render(<App />)
}
