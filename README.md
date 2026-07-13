# ScoreFlux (nome provisório)

Sistema de análise de crédito e fluxo de caixa para o setor agro. Este repositório nasce das
planilhas de referência (`Fluxo de Pagamentos - Estimativa.xlsx` e `PADRAO_PLANILHA1.1.xlsx`),
replicando a lógica de cálculo em código.

## Módulos

| Módulo | Status | Origem na planilha |
|---|---|---|
| Fluxo de Pagamentos (Parte A) | ✅ implementado | Aba "Fluxo de pagamento" |
| Cadastro de clientes analisados | ✅ implementado | — |
| Demonstrativos + Indicadores (ROE, endividamento, liquidez seca, evolução vendas) | ✅ implementado | Aba "Análise Indicadores" |
| CDG / NCG / Tesouraria (Fleuriet, Tipo I–VI) | ✅ implementado | Aba "NCG" |
| Score de crédito + rating AAA–H + limite | ✅ implementado | Abas "Parâmetros" e "Política Crédito" |
| Relato de Campo | ✅ embutido na análise (respostas estruturadas + justificativas + observações) | Aba "Relato de Campo" |
| Usuários + login (e-mail/senha, JWT) | ✅ implementado | — |
| Filtro de planos por estado (UF) + vínculo plano→cliente | ✅ implementado | — |
| Fluxo de Caixa: lançamentos diários com saldo corrente (realizado/previsto) | ✅ implementado | Abas "Extrato Bancário" e "Contas Receber e Pagar" |
| Carteira: limite × saldo em aberto → OK/BLOQUEAR | ✅ implementado | Abas "TESTE2" (controle de clientes) |
| Parecer de crédito em PDF | ✅ implementado | — |
| Tela NCG/Tesouraria (balanço reclassificado + importação de PDF) | ✅ implementado | Aba "NCG" |
| Tela Parâmetros (dados estáticos da política) | ✅ implementado | Aba "Parâmetros" |
| Tela Política Crédito (simulador com peso atribuído → peso cliente ao vivo) | ✅ implementado | Aba "Política Crédito" |
| Tela Relato de Campo (formulário persistente por cliente) | ✅ implementado | Aba "Relato de Campo" |
| Tela Análise Indicadores (aba fiel, máscara R$, manual + PDF) | ✅ implementado | Aba "Análise Indicadores" |

### Agentes e skills do projeto (automação de desenvolvimento)

- `CLAUDE.md` — convenções carregadas em toda sessão do Claude Code.
- `.claude/agents/scoreflux-backend.md` e `.claude/agents/scoreflux-frontend.md` — agentes
  especialistas para delegar alterações de backend/frontend.
- `.claude/skills/nova-tela-scoreflux/` — checklist de módulo completo (migration → API → página → verificação).
- `.claude/skills/verificar-scoreflux/` — como subir/reiniciar/diagnosticar o sistema localmente.

### Importação de balanço por PDF (tela NCG)

`POST /api/demonstrativos/extrair-pdf` (multipart) lê o PDF com PDFBox e extrai contas por
correspondência de nomes (formato numérico brasileiro; parênteses = negativo). A extração é
uma **sugestão**: preenche o formulário para revisão do usuário e nada é salvo até confirmar.

### Autenticação

Toda a API exige `Authorization: Bearer <token>` (exceto `/api/auth/login`). No primeiro
boot é criado o usuário **admin@scoreflux.com / admin123** — troque a senha. Novos usuários
são cadastrados na aba "Usuários". O token JWT expira em 12h; o segredo vem da propriedade
`scoreflux.jwt.secret` (trocar em produção).

### Como funciona a análise de crédito

1. Cadastre o **cliente analisado** e os **demonstrativos** de 2 exercícios (contas granulares — agregados são derivados).
2. Os **indicadores** (ROE, endividamento, liquidez seca, evolução de vendas) e o **diagnóstico Fleuriet** (T, NCG, CDG, Tipo I–VI) são calculados na hora.
3. Crie uma **análise**: 14 subcritérios da política (seed = política SulGesso v1, pesos 55/15/20/10). Os subcritérios 3.x têm **sugestão automática pelo balanço**.
4. Score (0–100) → **rating AAA–H** → **limite = média(PL, Faturamento) × % do rating**, ao vivo durante o rascunho.
5. **Concluir** congela o snapshot (auditoria); análise concluída só muda após "Reabrir". A política é versionada — análises antigas guardam a versão usada.
6. **Baixar parecer (PDF)** gera o laudo com cliente, resultado, indicadores, Fleuriet e critérios avaliados (`GET /api/analises/{id}/parecer`).

### Fluxo de Caixa e Carteira

- **Fluxo de Caixa**: lançamentos diários (data, classificação, histórico, entrada/saída, status realizado/previsto) com **saldo corrente** acumulado (`F(n) = F(n-1) + entrada − saída`) e filtro realizado/previsto.
- **Carteira**: por cliente, o **limite** vem da última análise concluída; **saldo em aberto** = Σ faturamentos − Σ pagamentos; **disponível** = limite − aberto; **status** OK / BLOQUEAR / SEM_LIMITE.

## Como rodar

### Backend (Java 21 + Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

Sobe em `http://localhost:8080`. Por padrão usa **H2 em arquivo** (`backend/data/scoreflux`),
sem precisar instalar nada. Com PostgreSQL instalado (banco `scoreflux`, usuário/senha
`scoreflux`), rode com:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

As migrations (Flyway, em `src/main/resources/db/migration`) são as mesmas para os dois bancos.

### Frontend (React + TypeScript)

```bash
cd frontend
npm install
npm run dev
```

Abre em `http://localhost:5173` (o `/api` é proxy para o backend na 8080).

### Testes

```bash
cd backend && mvn test
```

## Documentação

- [docs/01-mapeamento-planilhas.md](docs/01-mapeamento-planilhas.md) — estrutura, fórmulas e relacionamentos das planilhas (base do modelo de dados)
- [docs/02-pesquisa-mercado.md](docs/02-pesquisa-mercado.md) — concorrentes, referências e sugestões de nome
- [docs/03-modelo-negocio.md](docs/03-modelo-negocio.md) — avaliação SaaS, cliente-alvo, cobrança, diferenciação
- [docs/04-especificacao-sistema.md](docs/04-especificacao-sistema.md) — módulos, modelo de dados, stack, roadmap MVP
- [docs/resumo-executivo.md](docs/resumo-executivo.md) — 1 página para sócios/clientes (PDF em `outputs/`)
- `outputs/plano-faturamento.pdf` — planos, premissas e projeção de faturamento

## Decisões registradas

- Toda tabela tem vínculo com `empresa` desde a 1ª migration — costura para multi-cliente (SaaS) sem superengenharia agora.
- Nenhum total é persistido: os cálculos (equivalentes às fórmulas da planilha) vivem em `ResumoCalculator` e são cobertos por testes.
- Ano flexível JAN–DEZ (a planilha original era FEV–DEZ/2024).
- Mecanismo de pagamento: lista fixa (TED, PIX, Boleto, Financiamento, Leasing, Integralização, Dinheiro) + "Outro" com texto livre.
- Rateio "CAVAMAXI [50%]" da planilha: regra ainda não confirmada — só o total 100% foi implementado.
