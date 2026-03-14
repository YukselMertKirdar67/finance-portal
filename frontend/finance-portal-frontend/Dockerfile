# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Package.json ve package-lock.json kopyala
COPY package*.json ./

# Bağımlılıkları kur
RUN npm ci

# Tüm dosyaları kopyala
COPY . .

#Build arguments
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ARG VITE_API_BASE_URL=http://localhost:8080/api

#Environment variables for build
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL


# Production build
RUN npm run build

# Production stage - Nginx
FROM nginx:alpine

# Nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Build edilen dosyaları kopyala
COPY --from=builder /app/dist /usr/share/nginx/html

# Port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]