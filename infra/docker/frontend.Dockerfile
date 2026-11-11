FROM node:24-alpine@sha256:ebfe2f90462722a7a4de65e91990e97fe0d401c70e0e762c5b53302f905ec1c1 AS build
WORKDIR /workspace/frontend
COPY frontend/package*.json frontend/.npmrc ./
RUN npm ci
COPY frontend/ ./
COPY scripts/ /workspace/scripts/
COPY contracts/ /workspace/contracts/
# Prebuild verifies both sealed resources and the backend translation loader's source contract.
COPY backend/src/main/ /workspace/backend/src/main/
COPY docs/evidence/ /workspace/docs/evidence/
COPY docs/current/support.md /workspace/docs/current/support.md
ARG VITE_CRAFT_SUPPORT_ENABLED=true
ENV VITE_CRAFT_SUPPORT_ENABLED=$VITE_CRAFT_SUPPORT_ENABLED
# Public assets may arrive with restrictive workspace permissions; Nginx runs as UID 101.
RUN npm run build && chmod -R a+rX dist
FROM nginxinc/nginx-unprivileged:stable-alpine@sha256:04a3275f25d766cff8926d2e57b2ff34a783d6b12a702dc98bb82226d2d9a508
COPY --chmod=644 infra/docker/chaos-repeat-limits.conf /etc/nginx/chaos-repeat-limits.conf
COPY --chmod=644 infra/docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist/ /usr/share/nginx/html/
USER 101
EXPOSE 8080
