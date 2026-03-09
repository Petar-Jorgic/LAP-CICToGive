# CIC ToGive - Verschenke-Plattform

Interne IBM CIC Plattform zum Verschenken von Gegenstaenden. Entwickelt mit React, Spring Boot und PostgreSQL. Gehostet auf OpenShift.

## Architektur

- **Frontend**: React 19 + TypeScript + Vite + TailwindCSS + DaisyUI
- **Backend**: Spring Boot 3.2 + Spring Security + JPA/Hibernate
- **Datenbank**: PostgreSQL
- **Auth**: IBM W3ID SSO (OIDC) mit OAuth2 Resource Server
- **Storage**: PVC (PersistentVolumeClaim) auf OpenShift
- **CI/CD**: Tekton Pipeline mit GitHub Webhook

## Lokale Entwicklung

```bash
# Repository klonen
git clone git@github.ibm.com:cic-austria/cic-to-give.git
cd cic-to-give

# Backend starten
cd backend
mvn spring-boot:run

# Frontend starten (neues Terminal)
cd Frontend
npm install
npm run dev
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080

## OpenShift Deployment

Die App laeuft auf dem IBM Cloud OpenShift Cluster im Namespace `cic-togive-dev`.

```bash
# Manuelles Deployment
./deploy.sh

# Oder automatisch: Push auf main triggert die Tekton Pipeline
git push ibm main
```

### Pipeline

Bei jedem Push auf `main` wird automatisch:
1. Code von GitHub geclont
2. Backend + Frontend Images parallel gebaut (Buildah)
3. Deployments neu ausgerollt

### Kubernetes Manifeste

```
k8s/
  postgresql.yaml   # PostgreSQL + PVC + Secret
  backend.yaml      # Backend + Uploads PVC + BuildConfig
  frontend.yaml     # Frontend + Nginx + Route
  pipeline.yaml     # Tekton Pipeline + Webhook
```

## Projektstruktur

```
cic-to-give/
  Frontend/
    src/
      components/     # React Komponenten
      pages/          # Seiten (Home, Profile, etc.)
      hooks/          # React Query Hooks
      services/       # API Service Layer
      auth.config.ts  # W3ID OIDC Konfiguration
    Dockerfile.prod   # Multi-stage Build (Node -> Nginx)
    nginx.conf        # Reverse Proxy fuer /api/
  backend/
    src/main/java/com/lap/
      config/         # Security, W3ID Auth, Storage
      controller/     # REST Controller
      service/        # Business Logic
      entity/         # JPA Entities
      repository/     # Spring Data Repositories
    Dockerfile.prod   # Multi-stage Build (Maven -> JRE)
  k8s/                # OpenShift Manifeste
  deploy.sh           # Deploy Script
```

## Features

- W3ID SSO Login (IBM Mitarbeiter)
- Artikel erstellen mit Bildern (max 5 pro Artikel)
- Artikel suchen und filtern nach Kategorie
- Artikel reservieren
- Kommentare zu Artikeln
- Profilmanagement mit Avatar
- Responsive Design

## Umgebungsvariablen (Backend)

| Variable | Beschreibung |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL URL |
| `SPRING_DATASOURCE_USERNAME` | DB User |
| `SPRING_DATASOURCE_PASSWORD` | DB Passwort |
| `W3ID_CLIENT_ID` | W3ID OIDC Client ID |
| `W3ID_CLIENT_SECRET` | W3ID OIDC Client Secret |
