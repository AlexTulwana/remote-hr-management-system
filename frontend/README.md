# WorkFlowTech - Frontend

React (Vite) + Tailwind CSS frontend for the WorkFlowTech HR platform.

See the root README (../README.md) for the full project overview, features, and backend setup.

## Setup

npm install
npm run dev

Runs on http://localhost:5173 (or next free port if taken). Expects the backend API running on http://localhost:8080.

## Other commands

npm run build     production build
npm run lint       oxlint

## Structure

src/
  api/         fetch wrappers per backend resource
  auth/         AuthContext, ThemeContext, ProtectedRoute
  components/    shared UI (Sidebar, TopBar, Avatar, Card, Button, ...)
  pages/         one folder per role (employee/, manager/, hr/, executive/) plus shared pages
  App.jsx        routes and role-based nav

## Design system

- Black / white / grey palette, no accent colors (green = success, red = urgent only)
- Public Sans font
- Dark mode via ThemeContext (data-theme on html)
- Collapsible sidebar (state persisted in localStorage)
