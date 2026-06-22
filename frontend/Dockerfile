# Stage 1: Build the React app
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve the app with Nginx
FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# Copy standard nginx configuration to allow SPA routing if needed
RUN echo 'server { \
    listen 3000; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
    location /api/auth { \
        proxy_pass http://identity-service:8081; \
    } \
    location /api/elections { \
        proxy_pass http://election-service:8082; \
    } \
    location /api/votes { \
        proxy_pass http://vote-service:8083; \
    } \
    location /api/audit { \
        proxy_pass http://audit-service:8084; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
