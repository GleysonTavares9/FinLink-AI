# FinLink AI - Pluggy API Integration

This project integrates the **Pluggy API** to securely generate Connect Tokens on the server side and open the Pluggy Connect widget in the frontend.

## 🔐 Credentials & Environment Setup

To keep credentials secure and prevent exposure in browser bundles or Git repositories:

1. Copy `.env.example` to `.env.local` (or edit [.env.local](file:///C:/Users/GleysonMiranda/.gemini/antigravity-ide/scratch/finlink-ai/.env.local)):
   ```env
   CLIENT_ID=your_real_client_id_here
   CLIENT_SECRET=your_real_client_secret_here
   ```
2. Get your credentials at the [Pluggy Dashboard](https://dashboard.pluggy.ai).
3. `.env.local` is ignored in `.gitignore` to prevent leaking credentials.

## 📁 Key Files

- [route.ts](file:///C:/Users/GleysonMiranda/.gemini/antigravity-ide/scratch/finlink-ai/app/api/connect-token/route.ts): Server-side Next.js route handler that authenticates with `pluggy-sdk` and issues a `connectToken`.
- [PluggyConnectButton.tsx](file:///C:/Users/GleysonMiranda/.gemini/antigravity-ide/scratch/finlink-ai/components/PluggyConnectButton.tsx): Client component that triggers the token generation and launches Pluggy Connect.
- [.env.local](file:///C:/Users/GleysonMiranda/.gemini/antigravity-ide/scratch/finlink-ai/.env.local): Local environment file for your credentials.
