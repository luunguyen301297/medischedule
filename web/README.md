# web

Web portal cho Admin, Bác sĩ, Lễ tân, Dược sĩ. Chưa khởi tạo.

Stack đã chốt: React 18 + TypeScript + Vite + React Router + Ant Design 5 (ProComponents) + TanStack Query + orval (sinh client từ OpenAPI của backend) + @stomp/stompjs.

Khởi tạo:

```bash
npm create vite@latest . -- --template react-ts
npm i antd @ant-design/pro-components @tanstack/react-query react-router-dom axios @stomp/stompjs
npm i -D orval
```

Cổng dev: 5175 (khớp `CORS_ORIGINS` trong `.env`). Proxy `/api` tới `http://localhost:8085`.
