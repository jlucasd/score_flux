// URL base da API. Em produção (Vercel) aponte para o backend via VITE_API_BASE_URL
// (ex.: https://scoreflux-api.onrender.com). Em dev fica vazio e usa o proxy do Vite.
export const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

// ---- Autenticação ----

let token: string | null = localStorage.getItem('sf_token');

export const getToken = () => token;

export const setSessao = (novoToken: string | null, nome?: string) => {
  token = novoToken;
  if (novoToken) {
    localStorage.setItem('sf_token', novoToken);
    if (nome) localStorage.setItem('sf_nome', nome);
  } else {
    localStorage.removeItem('sf_token');
    localStorage.removeItem('sf_nome');
  }
};

export const getNomeUsuario = () => localStorage.getItem('sf_nome') ?? '';

export interface Plano {
  id: number;
  nome: string;
  ano: number;
  uf: string | null;
  clienteId: number | null;
  clienteNome: string | null;
}

export interface MecanismoOpcao {
  codigo: string;
  rotulo: string;
}

export interface ItemResumo {
  id: number;
  descricao: string;
  mecanismo: string;
  mecanismoOutro: string | null;
  nota: string | null;
  ordem: number;
  valores: Record<string, number>;
  total: number;
}

export interface OrcamentoResumo {
  id: number;
  descricao: string;
  valorUnitario: number;
  quantidade: number;
  total: number;
}

export interface ResumoPlano {
  planoId: number;
  nome: string;
  ano: number;
  itens: ItemResumo[];
  totaisPorMes: Record<string, number>;
  totalGeral: number;
  orcamentos: OrcamentoResumo[];
  totalOrcamentos: number;
}

async function http<T>(url: string, options?: RequestInit): Promise<T> {
  const cabecalhos: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) cabecalhos['Authorization'] = `Bearer ${token}`;
  const res = await fetch(API_BASE + url, { headers: cabecalhos, ...options });
  if (res.status === 401 && !url.startsWith('/api/auth')) {
    setSessao(null);
    window.dispatchEvent(new Event('sf-sessao-expirada'));
    throw new Error('Sessão expirada — faça login novamente');
  }
  if (!res.ok) {
    const corpo = await res.json().catch(() => null);
    throw new Error(corpo?.erro ?? `Erro ${res.status}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  ativo: boolean;
  criadoEm: string;
}

export const apiAuth = {
  login: (email: string, senha: string) =>
    http<{ token: string; nome: string; email: string }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, senha }),
    }),
  listarUsuarios: () => http<Usuario[]>('/api/usuarios'),
  criarUsuario: (dados: { nome: string; email: string; senha: string }) =>
    http<Usuario>('/api/usuarios', { method: 'POST', body: JSON.stringify(dados) }),
  excluirUsuario: (id: number) => http<void>(`/api/usuarios/${id}`, { method: 'DELETE' }),
};

export const api = {
  listarPlanos: (uf?: string) => http<Plano[]>(uf ? `/api/planos?uf=${uf}` : '/api/planos'),
  criarPlano: (nome: string, ano: number, uf?: string, clienteId?: number | null) =>
    http<Plano>('/api/planos', {
      method: 'POST',
      body: JSON.stringify({ nome, ano, uf: uf || null, clienteId: clienteId ?? null }),
    }),
  resumo: (planoId: number) => http<ResumoPlano>(`/api/planos/${planoId}/resumo`),
  mecanismos: () => http<MecanismoOpcao[]>('/api/mecanismos'),
  criarItem: (planoId: number, dados: { descricao: string; mecanismo: string; mecanismoOutro?: string; nota?: string }) =>
    http(`/api/planos/${planoId}/itens`, { method: 'POST', body: JSON.stringify(dados) }),
  excluirItem: (itemId: number) => http<void>(`/api/itens/${itemId}`, { method: 'DELETE' }),
  salvarValor: (itemId: number, mes: number, valor: number) =>
    http<void>(`/api/itens/${itemId}/valores`, { method: 'PUT', body: JSON.stringify({ [mes]: valor }) }),
  criarOrcamento: (planoId: number, dados: { descricao: string; valorUnitario: number; quantidade: number }) =>
    http(`/api/planos/${planoId}/orcamentos`, { method: 'POST', body: JSON.stringify(dados) }),
  excluirOrcamento: (id: number) => http<void>(`/api/orcamentos/${id}`, { method: 'DELETE' }),
};

// ---- Módulo de análise de crédito ----

export interface Cliente {
  id: number;
  nome: string;
  cpfCnpj: string | null;
  tipo: string;
  municipio: string | null;
  uf: string | null;
  telefone: string | null;
  email: string | null;
  endereco: string | null;
}

export interface Demonstrativo {
  id?: number;
  exercicio: number;
  receitaBruta: number;
  lucroLiquido: number;
  caixaBancos: number;
  aplicacoes: number;
  contasReceber: number;
  estoques: number;
  outrosAtivosCirculantes: number;
  realizavelLongoPrazo: number;
  imobilizado: number;
  emprestimosCurtoPrazo: number;
  fornecedores: number;
  salariosAPagar: number;
  outrasObrigacoesCirculantes: number;
  passivoNaoCirculante: number;
  patrimonioLiquido: number;
}

export interface ExercicioIndicadores {
  exercicio: number;
  receitaBruta: number;
  patrimonioLiquido: number;
  ativoCirculante: number;
  passivoCirculante: number;
  ativoTotal: number;
  diferencaBalanco: number;
  roe: number | null;
  endividamento: number | null;
  liquidezSeca: number | null;
  liquidezCorrente: number | null;
  tesouraria: number;
  ncg: number;
  cdg: number;
  tipoFleuriet: string;
  diagnostico: string;
  descricaoDiagnostico: string;
}

export interface Indicadores {
  exercicios: ExercicioIndicadores[];
  roeMedia: number | null;
  endividamentoMedia: number | null;
  liquidezSecaMedia: number | null;
  evolucaoVendas: number | null;
}

export interface OpcaoPolitica {
  id: number;
  rotulo: string;
  nota: number;
}

export interface SubcriterioPolitica {
  id: number;
  grupo: string;
  codigo: string;
  nome: string;
  peso: number;
  automatico: boolean;
  instrumento: string | null;
  fonte: string | null;
  validacao: string | null;
  descricao: string | null;
  opcoes: OpcaoPolitica[];
}

export interface Faixa {
  scoreMinimo: number;
  rating: string;
  percentualLimite: number;
}

export interface Politica {
  id: number;
  versao: number;
  nome: string;
  inflacaoReferencia: number;
  subcriterios: SubcriterioPolitica[];
}

export interface AnaliseResumo {
  id: number;
  status: string;
  criadaEm: string;
  concluidaEm: string | null;
  score: number | null;
  rating: string | null;
  limiteCalculado: number | null;
}

export interface RespostaAnalise {
  subcriterioId: number;
  opcaoId: number;
  justificativa: string | null;
}

export interface ResultadoAnalise {
  score: number | null;
  rating: string | null;
  percentualLimite: number | null;
  baseLimite: number | null;
  limite: number | null;
}

export interface AnaliseDetalhe {
  id: number;
  clienteId: number;
  clienteNome: string;
  status: string;
  observacoes: string | null;
  criadaEm: string;
  concluidaEm: string | null;
  respostas: RespostaAnalise[];
  resultado: ResultadoAnalise;
  sugestoes: Record<string, number>;
}

export interface RelatoCampo {
  clienteId: number;
  clienteNome: string;
  clienteCpfCnpj: string | null;
  conceitoComercial: string | null;
  conceitoComercialJustificativa: string | null;
  tempoMercado: string | null;
  tempoMercadoJustificativa: string | null;
  bandeira: string | null;
  bandeiraJustificativa: string | null;
  possuiErp: boolean | null;
  possuiCobranca: boolean | null;
  unidadesNegocio: string | null;
  unidadesNegocioJustificativa: string | null;
  riscoClimatico: string | null;
  riscoClimaticoJustificativa: string | null;
  observacoes: string | null;
  atualizadoEm: string | null;
}

export type RelatoCampoDados = Omit<
  RelatoCampo, 'clienteId' | 'clienteNome' | 'clienteCpfCnpj' | 'atualizadoEm'
>;

export const apiCredito = {
  listarClientes: () => http<Cliente[]>('/api/clientes'),
  criarCliente: (dados: {
    nome: string; cpfCnpj?: string; tipo: string; municipio?: string; uf?: string;
    telefone?: string; email?: string; endereco?: string;
  }) => http<Cliente>('/api/clientes', { method: 'POST', body: JSON.stringify(dados) }),
  excluirCliente: (id: number) => http<void>(`/api/clientes/${id}`, { method: 'DELETE' }),
  demonstrativos: (clienteId: number) => http<Demonstrativo[]>(`/api/clientes/${clienteId}/demonstrativos`),
  salvarDemonstrativo: (clienteId: number, exercicio: number, dados: Partial<Demonstrativo>) =>
    http<Demonstrativo>(`/api/clientes/${clienteId}/demonstrativos/${exercicio}`, {
      method: 'PUT',
      body: JSON.stringify(dados),
    }),
  indicadores: (clienteId: number) => http<Indicadores>(`/api/clientes/${clienteId}/indicadores`),
  politica: () => http<Politica>('/api/politica'),
  faixas: () => http<Faixa[]>('/api/politica/faixas'),
  relatoCampo: (clienteId: number) => http<RelatoCampo>(`/api/clientes/${clienteId}/relato-campo`),
  salvarRelatoCampo: (clienteId: number, dados: RelatoCampoDados) =>
    http<RelatoCampo>(`/api/clientes/${clienteId}/relato-campo`, {
      method: 'PUT',
      body: JSON.stringify(dados),
    }),
  analises: (clienteId: number) => http<AnaliseResumo[]>(`/api/clientes/${clienteId}/analises`),
  criarAnalise: (clienteId: number) =>
    http<AnaliseDetalhe>(`/api/clientes/${clienteId}/analises`, { method: 'POST' }),
  analise: (id: number) => http<AnaliseDetalhe>(`/api/analises/${id}`),
  salvarRespostas: (id: number, observacoes: string, respostas: RespostaAnalise[]) =>
    http<AnaliseDetalhe>(`/api/analises/${id}/respostas`, {
      method: 'PUT',
      body: JSON.stringify({ observacoes, respostas }),
    }),
  concluirAnalise: (id: number) => http<AnaliseDetalhe>(`/api/analises/${id}/concluir`, { method: 'POST' }),
  reabrirAnalise: (id: number) => http<AnaliseDetalhe>(`/api/analises/${id}/reabrir`, { method: 'POST' }),
  excluirAnalise: (id: number) => http<void>(`/api/analises/${id}`, { method: 'DELETE' }),
};

// ---- Fluxo de Caixa (lançamentos diários) ----

export interface ExtratoLinha {
  id: number;
  data: string;
  classificacao: string | null;
  historico: string | null;
  entrada: number;
  saida: number;
  saldo: number;
  status: string;
  clienteId: number | null;
  clienteNome: string | null;
}

export interface Extrato {
  linhas: ExtratoLinha[];
  totalEntradas: number;
  totalSaidas: number;
  saldoFinal: number;
}

export const apiCaixa = {
  extrato: (status?: string) => http<Extrato>(status ? `/api/caixa?status=${status}` : '/api/caixa'),
  criar: (
    dados: { data: string; classificacao?: string; historico?: string; entrada: number; saida: number; status: string; clienteId?: number | null },
    filtro?: string,
  ) => http<Extrato>(`/api/caixa${filtro ? `?filtro=${filtro}` : ''}`, { method: 'POST', body: JSON.stringify(dados) }),
  excluir: (id: number, filtro?: string) =>
    http<Extrato>(`/api/caixa/${id}${filtro ? `?filtro=${filtro}` : ''}`, { method: 'DELETE' }),
};

// ---- Carteira ----

export interface PosicaoCarteira {
  clienteId: number;
  clienteNome: string;
  limite: number | null;
  rating: string | null;
  saldoAberto: number;
  disponivel: number | null;
  status: string;
}

export interface Carteira {
  posicoes: PosicaoCarteira[];
  totalLimite: number;
  totalSaldoAberto: number;
  totalDisponivel: number;
}

export interface MovimentoCarteira {
  id: number;
  data: string;
  tipo: string;
  valor: number;
  descricao: string | null;
}

export const apiCarteira = {
  carteira: () => http<Carteira>('/api/carteira'),
  movimentos: (clienteId: number) => http<MovimentoCarteira[]>(`/api/carteira/clientes/${clienteId}/movimentos`),
  adicionar: (clienteId: number, dados: { data: string; tipo: string; valor: number; descricao?: string }) =>
    http<Carteira>(`/api/carteira/clientes/${clienteId}/movimentos`, { method: 'POST', body: JSON.stringify(dados) }),
  excluirMovimento: (id: number) => http<Carteira>(`/api/carteira/movimentos/${id}`, { method: 'DELETE' }),
};

export interface ExtracaoPdf {
  campos: Record<string, number>;
  camposEncontrados: string[];
  exercicioDetectado: number | null;
}

/** Envia um PDF de balanço e recebe os campos extraídos (sem persistir nada). */
export async function extrairBalancoPdf(arquivo: File): Promise<ExtracaoPdf> {
  const form = new FormData();
  form.append('arquivo', arquivo);
  const res = await fetch(API_BASE + '/api/demonstrativos/extrair-pdf', {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  });
  if (res.status === 401) {
    setSessao(null);
    window.dispatchEvent(new Event('sf-sessao-expirada'));
    throw new Error('Sessão expirada — faça login novamente');
  }
  if (!res.ok) {
    const corpo = await res.json().catch(() => null);
    throw new Error(corpo?.erro ?? 'Falha ao ler o PDF');
  }
  return res.json();
}

/** Baixa o parecer PDF autenticado e abre em nova aba (link direto não envia o token). */
export async function baixarParecer(analiseId: number) {
  const res = await fetch(`${API_BASE}/api/analises/${analiseId}/parecer`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Não foi possível gerar o parecer');
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

export const MESES = ['JAN', 'FEV', 'MAR', 'ABR', 'MAI', 'JUN', 'JUL', 'AGO', 'SET', 'OUT', 'NOV', 'DEZ'];

export const UFS = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
  'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'];

export const brl = (v: number) =>
  v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

export const pct = (v: number | null) =>
  v === null || v === undefined ? '—' : `${(v * 100).toFixed(1).replace('.', ',')}%`;

export const num = (v: number | null) =>
  v === null || v === undefined ? '—' : v.toLocaleString('pt-BR', { maximumFractionDigits: 2 });
