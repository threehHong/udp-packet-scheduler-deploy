# 1. Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 2. Run
FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production

COPY --from=build /app ./

EXPOSE 3000
CMD ["npm", "run", "start"]
