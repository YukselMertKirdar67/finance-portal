# Build stage
FROM node:20-alpine AS builder

WORKDIR /app

# Package.json ve package-lock.json kopyala
COPY package*.json ./

# Bağımlılıkları kur
RUN npm ci

# Tüm dosyaları kopyala
COPY . .

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