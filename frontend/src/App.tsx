import { useCallback, useEffect, useState } from 'react';
import {
  api, apiCredito, brl, Cliente, getNomeUsuario, getToken, MESES, MecanismoOpcao, Plano,
  ResumoPlano, setSessao, UFS,
} from './api';
import AnaliseIndicadoresPage from './AnaliseIndicadoresPage';
import CreditoPage from './CreditoPage';
import CaixaPage from './CaixaPage';
import CarteiraPage from './CarteiraPage';
import LoginPage from './LoginPage';
import NcgPage from './NcgPage';
import ParametrosPage from './ParametrosPage';
import PoliticaCreditoPage from './PoliticaCreditoPage';
import RelatoCampoPage from './RelatoCampoPage';
import UsuariosPage from './UsuariosPage';
import { Campo } from './ui';

type Aba =
  | 'parametros' | 'politica' | 'relato' | 'indicadores' | 'ncg'
  | 'credito' | 'fluxo' | 'caixa' | 'carteira' | 'usuarios';

export default function App() {
  const [logado, setLogado] = useState(!!getToken());
  const [aba, setAba] = useState<Aba>('parametros');

  useEffect(() => {
    const expirar = () => setLogado(false);
    window.addEventListener('sf-sessao-expirada', expirar);
    return () => window.removeEventListener('sf-sessao-expirada', expirar);
  }, []);

  if (!logado) return <LoginPage onLogin={() => setLogado(true)} />;

  const abas: [Aba, string][] = [
    ['parametros', 'Parâmetros'],
    ['politica', 'Política Crédito'],
    ['relato', 'Relato de Campo'],
    ['indicadores', 'Análise Indicadores'],
    ['ncg', 'NCG / Tesouraria'],
    ['credito', 'Análise de Crédito'],
    ['fluxo', 'Fluxo de Pagamentos'],
    ['caixa', 'Fluxo de Caixa'],
    ['carteira', 'Carteira'],
    ['usuarios', 'Usuários'],
  ];

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>ScoreFlux</h1>
        <span className="subtitulo">Crédito e fluxo de caixa agro</span>
        <nav className="menu-lateral">
          {abas.map(([chave, rotulo]) => (
            <button key={chave} className={aba === chave ? 'aba ativa' : 'aba'} onClick={() => setAba(chave)}>
              {rotulo}
            </button>
          ))}
        </nav>
        <div className="sidebar-rodape">
          <span className="subtitulo">{getNomeUsuario()}</span>
          <button
            className="botao-sair"
            onClick={() => {
              setSessao(null);
              setLogado(false);
            }}
          >
            Sair
          </button>
        </div>
      </aside>
      <main className="conteudo">
        {aba === 'parametros' && <ParametrosPage />}
        {aba === 'politica' && <PoliticaCreditoPage />}
        {aba === 'relato' && <RelatoCampoPage />}
        {aba === 'indicadores' && <AnaliseIndicadoresPage />}
        {aba === 'ncg' && <NcgPage />}
        {aba === 'credito' && <CreditoPage />}
        {aba === 'fluxo' && <FluxoPage />}
        {aba === 'caixa' && <CaixaPage />}
        {aba === 'carteira' && <CarteiraPage />}
        {aba === 'usuarios' && <UsuariosPage />}
      </main>
    </div>
  );
}

function FluxoPage() {
  const [planos, setPlanos] = useState<Plano[]>([]);
  const [planoId, setPlanoId] = useState<number | null>(null);
  const [filtroUf, setFiltroUf] = useState('');
  const [resumo, setResumo] = useState<ResumoPlano | null>(null);
  const [mecanismos, setMecanismos] = useState<MecanismoOpcao[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [erro, setErro] = useState<string | null>(null);

  const carregarPlanos = useCallback(async () => {
    const lista = await api.listarPlanos(filtroUf || undefined);
    setPlanos(lista);
    // Se o plano selecionado saiu do filtro, seleciona o primeiro da lista (ou nenhum)
    setPlanoId((atual) => {
      if (atual !== null && lista.some((p) => p.id === atual)) return atual;
      return lista.length > 0 ? lista[0].id : null;
    });
  }, [filtroUf]);

  const carregarResumo = useCallback(async () => {
    if (planoId === null) {
      setResumo(null);
      return;
    }
    setResumo(await api.resumo(planoId));
  }, [planoId]);

  useEffect(() => {
    carregarPlanos().catch((e) => setErro(e.message));
    api.mecanismos().then(setMecanismos).catch((e) => setErro(e.message));
    apiCredito.listarClientes().then(setClientes).catch((e) => setErro(e.message));
  }, [carregarPlanos]);

  useEffect(() => {
    carregarResumo().catch((e) => setErro(e.message));
  }, [carregarResumo]);

  const executar = (acao: () => Promise<unknown>) => {
    setErro(null);
    acao()
      .then(carregarResumo)
      .catch((e) => setErro(e.message));
  };

  return (
    <>
      {erro && <div className="erro">{erro}</div>}

      <SeletorPlano
        planos={planos}
        planoId={planoId}
        filtroUf={filtroUf}
        clientes={clientes}
        onFiltrarUf={setFiltroUf}
        onSelecionar={setPlanoId}
        onCriar={async (nome, ano, uf, clienteId) => {
          setErro(null);
          try {
            const novo = await api.criarPlano(nome, ano, uf, clienteId);
            setFiltroUf(novo.uf ?? '');
            setPlanoId(novo.id);
            await carregarPlanos();
          } catch (e) {
            setErro((e as Error).message);
          }
        }}
      />

      {!resumo && planos.length === 0 && (
        <section className="painel vazio">
          Nenhum plano {filtroUf ? `no estado ${filtroUf}` : 'cadastrado'} — crie um acima.
        </section>
      )}

      {resumo && (
        <>
          <GradeFluxo resumo={resumo} mecanismos={mecanismos} executar={executar} planoId={planoId!} />
          <Dashboard resumo={resumo} />
          <Orcamentos resumo={resumo} executar={executar} planoId={planoId!} />
        </>
      )}
    </>
  );
}

function SeletorPlano(props: {
  planos: Plano[];
  planoId: number | null;
  filtroUf: string;
  clientes: Cliente[];
  onFiltrarUf: (uf: string) => void;
  onSelecionar: (id: number) => void;
  onCriar: (nome: string, ano: number, uf: string, clienteId: number | null) => void;
}) {
  const [nome, setNome] = useState('');
  const [ano, setAno] = useState(new Date().getFullYear());
  const [uf, setUf] = useState('');
  const [clienteId, setClienteId] = useState('');

  return (
    <section className="painel">
      <div className="linha-form">
        <Campo label="Filtrar por estado (UF)">
          <select value={props.filtroUf} onChange={(e) => props.onFiltrarUf(e.target.value)}>
            <option value="">Todos os estados</option>
            {UFS.map((sigla) => (
              <option key={sigla} value={sigla}>{sigla}</option>
            ))}
          </select>
        </Campo>
        <Campo label="Plano selecionado">
          <select
            value={props.planoId ?? ''}
            onChange={(e) => props.onSelecionar(Number(e.target.value))}
          >
            {props.planos.length === 0 && <option value="">— nenhum plano —</option>}
            {props.planos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nome} ({p.ano}){p.uf ? ` — ${p.uf}` : ''}{p.clienteNome ? ` · ${p.clienteNome}` : ''}
              </option>
            ))}
          </select>
        </Campo>
      </div>
      <div className="linha-form">
        <Campo label="Nome do novo plano" largo>
          <input
            placeholder="Ex.: Safra 2026"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
          />
        </Campo>
        <Campo label="Ano">
          <input
            type="number"
            className="campo-ano"
            value={ano}
            onChange={(e) => setAno(Number(e.target.value))}
          />
        </Campo>
        <Campo label="Cliente vinculado (opcional)">
          <select
            value={clienteId}
            onChange={(e) => setClienteId(e.target.value)}
            title="Herda a UF do cliente se a UF ficar em branco"
          >
            <option value="">— nenhum —</option>
            {props.clientes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}{c.uf ? ` (${c.uf})` : ''}
              </option>
            ))}
          </select>
        </Campo>
        <Campo label="UF do plano">
          <select value={uf} onChange={(e) => setUf(e.target.value)}>
            <option value="">—</option>
            {UFS.map((sigla) => (
              <option key={sigla} value={sigla}>{sigla}</option>
            ))}
          </select>
        </Campo>
        <button
          disabled={!nome.trim()}
          onClick={() => {
            props.onCriar(nome.trim(), ano, uf, clienteId ? Number(clienteId) : null);
            setNome('');
            setClienteId('');
          }}
        >
          Criar plano
        </button>
      </div>
    </section>
  );
}

function GradeFluxo(props: {
  resumo: ResumoPlano;
  mecanismos: MecanismoOpcao[];
  planoId: number;
  executar: (acao: () => Promise<unknown>) => void;
}) {
  const { resumo, mecanismos, planoId, executar } = props;
  const [novoItem, setNovoItem] = useState({ descricao: '', mecanismo: 'BOLETO', mecanismoOutro: '', nota: '' });
  // Rascunho da célula em edição; grava no blur (como sair da célula na planilha)
  const [edicao, setEdicao] = useState<{ itemId: number; mes: number; texto: string } | null>(null);

  const rotuloMecanismo = (item: { mecanismo: string; mecanismoOutro: string | null }) => {
    if (item.mecanismo === 'OUTRO' && item.mecanismoOutro) return item.mecanismoOutro;
    return mecanismos.find((m) => m.codigo === item.mecanismo)?.rotulo ?? item.mecanismo;
  };

  const confirmarEdicao = () => {
    if (!edicao) return;
    const valor = Number(edicao.texto.replace(',', '.'));
    const { itemId, mes } = edicao;
    setEdicao(null);
    if (Number.isNaN(valor) || valor < 0) return;
    executar(() => api.salvarValor(itemId, mes, valor));
  };

  return (
    <section className="painel">
      <h2>
        {resumo.nome} — {resumo.ano}
      </h2>
      <div className="rolagem">
        <table>
          <thead>
            <tr>
              <th className="col-item">Item</th>
              <th>Mecanismo</th>
              {MESES.map((m) => (
                <th key={m}>{m}</th>
              ))}
              <th>Total</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {resumo.itens.map((item) => (
              <tr key={item.id}>
                <td className="col-item" title={item.nota ?? ''}>
                  {item.descricao}
                  {item.nota && <span className="marca-nota"> *</span>}
                </td>
                <td>{rotuloMecanismo(item)}</td>
                {MESES.map((_, i) => {
                  const mes = i + 1;
                  const emEdicao = edicao?.itemId === item.id && edicao?.mes === mes;
                  const valor = item.valores[String(mes)] ?? 0;
                  return (
                    <td key={mes} className="celula-valor">
                      <input
                        value={emEdicao ? edicao.texto : valor === 0 ? '' : String(valor)}
                        placeholder="–"
                        onFocus={() =>
                          setEdicao({ itemId: item.id, mes, texto: valor === 0 ? '' : String(valor) })
                        }
                        onChange={(e) =>
                          setEdicao({ itemId: item.id, mes, texto: e.target.value })
                        }
                        onBlur={confirmarEdicao}
                        onKeyDown={(e) => e.key === 'Enter' && (e.target as HTMLInputElement).blur()}
                      />
                    </td>
                  );
                })}
                <td className="total">{brl(item.total)}</td>
                <td>
                  <button
                    className="botao-excluir"
                    title="Excluir item"
                    onClick={() => executar(() => api.excluirItem(item.id))}
                  >
                    ×
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td className="col-item">TOTAIS</td>
              <td></td>
              {MESES.map((_, i) => (
                <td key={i} className="total">
                  {brl(resumo.totaisPorMes[String(i + 1)] ?? 0)}
                </td>
              ))}
              <td className="total total-geral">{brl(resumo.totalGeral)}</td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <div className="linha-form">
        <Campo label="Novo item" largo>
          <input
            placeholder="Ex.: 01 Trator"
            value={novoItem.descricao}
            onChange={(e) => setNovoItem({ ...novoItem, descricao: e.target.value })}
          />
        </Campo>
        <Campo label="Mecanismo de pagamento">
          <select
            value={novoItem.mecanismo}
            onChange={(e) => setNovoItem({ ...novoItem, mecanismo: e.target.value })}
          >
            {mecanismos.map((m) => (
              <option key={m.codigo} value={m.codigo}>
                {m.rotulo}
              </option>
            ))}
          </select>
        </Campo>
        {novoItem.mecanismo === 'OUTRO' && (
          <Campo label="Qual mecanismo?">
            <input
              placeholder="Descreva"
              value={novoItem.mecanismoOutro}
              onChange={(e) => setNovoItem({ ...novoItem, mecanismoOutro: e.target.value })}
            />
          </Campo>
        )}
        <Campo label="Nota (opcional)">
          <input
            placeholder="Observação do item"
            value={novoItem.nota}
            onChange={(e) => setNovoItem({ ...novoItem, nota: e.target.value })}
          />
        </Campo>
        <button
          disabled={!novoItem.descricao.trim()}
          onClick={() => {
            executar(() =>
              api.criarItem(planoId, {
                descricao: novoItem.descricao.trim(),
                mecanismo: novoItem.mecanismo,
                mecanismoOutro: novoItem.mecanismoOutro || undefined,
                nota: novoItem.nota || undefined,
              }),
            );
            setNovoItem({ descricao: '', mecanismo: 'BOLETO', mecanismoOutro: '', nota: '' });
          }}
        >
          Adicionar item
        </button>
      </div>
    </section>
  );
}

function Dashboard(props: { resumo: ResumoPlano }) {
  const totais = MESES.map((_, i) => props.resumo.totaisPorMes[String(i + 1)] ?? 0);
  const maximo = Math.max(...totais, 1);

  return (
    <section className="painel">
      <h2>Pagamentos por mês</h2>
      <div className="grafico">
        {totais.map((valor, i) => (
          <div key={i} className="coluna-grafico">
            <div className="valor-barra">{valor > 0 ? brl(valor) : ''}</div>
            <div className="barra" style={{ height: `${(valor / maximo) * 160}px` }} />
            <div className="rotulo-barra">{MESES[i]}</div>
          </div>
        ))}
      </div>
      <p className="destaque">
        Total do ano: <strong>{brl(props.resumo.totalGeral)}</strong>
      </p>
    </section>
  );
}

function Orcamentos(props: {
  resumo: ResumoPlano;
  planoId: number;
  executar: (acao: () => Promise<unknown>) => void;
}) {
  const { resumo, planoId, executar } = props;
  const [novo, setNovo] = useState({ descricao: '', valorUnitario: '', quantidade: '1' });

  return (
    <section className="painel">
      <h2>Orçamentos recebidos</h2>
      <table className="tabela-orcamentos">
        <thead>
          <tr>
            <th>Máquina / descrição</th>
            <th>Valor unitário</th>
            <th>Qtd.</th>
            <th>Total</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {resumo.orcamentos.map((o) => (
            <tr key={o.id}>
              <td>{o.descricao}</td>
              <td>{brl(o.valorUnitario)}</td>
              <td>{o.quantidade}</td>
              <td className="total">{brl(o.total)}</td>
              <td>
                <button
                  className="botao-excluir"
                  title="Excluir orçamento"
                  onClick={() => executar(() => api.excluirOrcamento(o.id))}
                >
                  ×
                </button>
              </td>
            </tr>
          ))}
          {resumo.orcamentos.length === 0 && (
            <tr>
              <td colSpan={5} className="vazio">
                Nenhum orçamento cadastrado
              </td>
            </tr>
          )}
        </tbody>
        <tfoot>
          <tr>
            <td colSpan={3}>Total</td>
            <td className="total total-geral">{brl(resumo.totalOrcamentos)}</td>
            <td></td>
          </tr>
        </tfoot>
      </table>

      <div className="linha-form">
        <Campo label="Descrição" largo>
          <input
            placeholder="Ex.: Escavadeira"
            value={novo.descricao}
            onChange={(e) => setNovo({ ...novo, descricao: e.target.value })}
          />
        </Campo>
        <Campo label="Valor unitário (R$)">
          <input
            placeholder="0,00"
            value={novo.valorUnitario}
            onChange={(e) => setNovo({ ...novo, valorUnitario: e.target.value })}
          />
        </Campo>
        <Campo label="Quantidade">
          <input
            type="number"
            min={1}
            className="campo-ano"
            value={novo.quantidade}
            onChange={(e) => setNovo({ ...novo, quantidade: e.target.value })}
          />
        </Campo>
        <button
          disabled={!novo.descricao.trim() || !novo.valorUnitario}
          onClick={() => {
            const valor = Number(novo.valorUnitario.replace(',', '.'));
            const qtd = Math.max(1, Number(novo.quantidade) || 1);
            if (Number.isNaN(valor) || valor < 0) return;
            executar(() =>
              api.criarOrcamento(planoId, {
                descricao: novo.descricao.trim(),
                valorUnitario: valor,
                quantidade: qtd,
              }),
            );
            setNovo({ descricao: '', valorUnitario: '', quantidade: '1' });
          }}
        >
          Adicionar orçamento
        </button>
      </div>
    </section>
  );
}
