FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps

COPY . .
RUN npm run build

FROM node:20-alpine

RUN npm install -g serve

COPY --from=build /app/dist /app/dist

CMD ["serve", "-s", "/app/dist"]
