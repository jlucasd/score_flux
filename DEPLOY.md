# Publicação do ScoreFlux

Arquitetura: **frontend** React/Vite na **Vercel** + **backend** Spring Boot (Docker) no
**Render** com **PostgreSQL** gerenciado. A configuração já está no repositório e foi
verificada localmente em Docker contra um Postgres real (build, boot, migrations, login, CORS).

## 1. Backend no Render (uma vez, ~3 min)

1. Acesse https://dashboard.render.com → **New** → **Blueprint**.
2. Conecte o repositório GitHub `jlucasd/score_flux`. O Render lê o `render.yaml` da raiz e
   provisiona automaticamente: o serviço web `scoreflux-api` (Docker) + o banco `scoreflux-db`.
3. Clique **Apply**. O primeiro build leva alguns minutos (Maven + imagem). No boot, o Flyway
   cria as tabelas e o usuário admin.
4. Quando ficar *Live*, copie a URL do serviço — algo como
   **`https://scoreflux-api.onrender.com`**. Teste: abrir `…/api/health` deve mostrar
   `{"status":"UP"}`.

## 2. Conectar o frontend (Vercel) ao backend

1. No projeto da Vercel: **Settings → Environment Variables**.
2. Adicione **`VITE_API_BASE_URL`** = a URL do Render (ex.: `https://scoreflux-api.onrender.com`),
   para os ambientes Production/Preview.
3. **Redeploy** o frontend (Deployments → ⋯ → Redeploy) para embutir a variável no build.

## 3. (Opcional) Restringir CORS

No Render, variável `SCOREFLUX_CORS_ORIGINS` = a URL exata da Vercel
(ex.: `https://scoreflux.vercel.app`). O padrão `*` já é seguro aqui (auth por Bearer token).

## 4. Login

Abra a URL da Vercel e entre com **`admin@scoreflux.com` / `admin123`** (troque a senha depois).

---

## Observações do plano gratuito
- O serviço web **dorme após 15 min** sem tráfego; a primeira chamada acorda em ~50s (cold start).
- O **Postgres free do Render expira em ~30 dias**. Para algo permanente: assine o Postgres do
  Render (~US$ 7/mês) ou use um Postgres gratuito persistente (**Neon**) — nesse caso, defina
  `SPRING_DATASOURCE_URL` (JDBC completa) no Render em vez de `DB_HOST/DB_PORT/DB_NAME`.

## Variáveis de ambiente do backend
| Variável | Para quê |
|---|---|
| `SPRING_PROFILES_ACTIVE=postgres` | Ativa o perfil de produção |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Conexão (montada em JDBC) |
| `SPRING_DATASOURCE_URL` | Alternativa: URL JDBC completa (Neon/Supabase) |
| `SCOREFLUX_JWT_SECRET` | Segredo do token (o Render gera automaticamente) |
| `SCOREFLUX_CORS_ORIGINS` | Origens liberadas (default `*`) |
| `PORT` | Porta (injetada pelo Render) |
