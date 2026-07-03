import { BrowserRouter, Route, Routes } from 'react-router'
import Landing from './pages/Landing'
import Dashboard from './pages/Dashboard'
import './App.css'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/dashboard" element={<Dashboard />} />
      </Routes>
    </BrowserRouter>
  )
}
