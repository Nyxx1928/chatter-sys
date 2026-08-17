# Build stage
FROM node:20-alpine AS build
WORKDIR /app

RUN npm install -g npm@10.9.2

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

ARG SENTRY_ORG
ARG SENTRY_PROJECT
ARG SENTRY_AUTH_TOKEN
ENV SENTRY_ORG=${SENTRY_ORG}
ENV SENTRY_PROJECT=${SENTRY_PROJECT}
ENV SENTRY_AUTH_TOKEN=${SENTRY_AUTH_TOKEN}

# Runtime stage
FROM node:20-alpine

# Patch OS-level vulnerabilities before installing app dependencies.
# Alpine images often trail the latest package releases by a few days;
# 'apk upgrade' brings all installed packages to their latest patched
# versions so trivy scans (Phase 7 CI) don't flag HIGH/CRITICAL CVEs
# that upstream has already fixed but not yet shipped in a new tag.
RUN apk upgrade --no-cache

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000

COPY --from=build /app/package.json ./package.json
COPY --from=build /app/package-lock.json ./package-lock.json
COPY --from=build /app/.next ./.next
COPY --from=build /app/public ./public

RUN npm ci --omit=dev
RUN rm -rf /usr/local/lib/node_modules/npm /usr/local/bin/npm /usr/local/bin/npx

EXPOSE 3000

CMD ["node", "node_modules/next/dist/bin/next", "start"]
