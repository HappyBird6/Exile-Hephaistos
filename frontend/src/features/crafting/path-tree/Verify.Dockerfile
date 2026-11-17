FROM node:24-alpine@sha256:ebfe2f90462722a7a4de65e91990e97fe0d401c70e0e762c5b53302f905ec1c1 AS dependencies
WORKDIR /app
COPY frontend/package*.json frontend/.npmrc ./
RUN npm ci
FROM dependencies AS verify
COPY frontend/ ./
COPY contracts/ /contracts/
COPY scripts/ /scripts/
COPY backend/src/main/ /backend/src/main/
COPY docs/legacy/evidence/ /docs/legacy/evidence/
COPY docs/v0.5.5/ /docs/v0.5.5/
COPY README.md /README.md
RUN ln -s /app /frontend
CMD ["sh", "-c", "npm run lint && npm run typecheck && npm run format:check && npm run test -- --run && npm run build"]
