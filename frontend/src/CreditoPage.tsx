import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AnaliseDetalhe, AnaliseResumo, Cliente, Demonstrativo, Indicadores, Politica,
  apiCredito, baixarParecer, brl, num, pct,
} from './api';
import { Campo } from './ui';

const CAMPOS_DEMONSTRATIVO: { chave: keyof Demonstrativo; rotulo: string; grupo: string }[] = [
  { chave: 'receitaBruta', rotulo: 'Receita Bruta', grupo: 'DRE' },
  { chave: 'lucroLiquido', rotulo: 'Lucro Líquido', grupo: 'DRE' },
  { chave: 'caixaBancos', rotulo: 'Caixa e Bancos', grupo: 'Ativo Circulante' },
  { chave: 'aplicacoes', rotulo: 'Aplicações Financeiras', grupo: 'Ativo Circulante' },
  { chave: 'contasReceber', rotulo: 'Clientes (a receber)', grupo: 'Ativo Circulante' },
  { chave: 'estoques', rotulo: 'Estoques', grupo: 'Ativo Circulante' },
  { chave: 'outrosAtivosCirculantes', rotulo: 'Outros Ativos Circ.', grupo: 'Ativo Circulante' },
  { chave: 'realizavelLongoPrazo', rotulo: 'Realizável Longo Prazo', grupo: 'Ativo Não Circulante' },
  { chave: 'imobilizado', rotulo: 'Imobilizado', grupo: 'Ativo Não Circulante' },
  { chave: 'emprestimosCurtoPrazo', rotulo: 'Empréstimos Curto Prazo', grupo: 'Passivo Circulante' },
  { chave: 'fornecedores', rotulo: 'Fornecedores', grupo: 'Passivo Circulante' },
  { chave: 'salariosAPagar', rotulo: 'Salários a Pagar', grupo: 'Passivo Circulante' },
  { chave: 'outrasObrigacoesCirculantes', rotulo: 'Outras Obrigações Circ.', grupo: 'Passivo Circulante' },
  { chave: 'passivoNaoCirculante', rotulo: 'Passivo Não Circulante', grupo: 'Longo Prazo e PL' },
  { chave: 'patrimonioLiquido', rotulo: 'Patrimônio Líquido', grupo: 'Longo Prazo e PL' },
];

const demonstrativoVazio = (exercicio: number): Demonstrativo => ({
  exercicio,
  receitaBruta: 0, lucroLiquido: 0,
  caixaBancos: 0, aplicacoes: 0, contasReceber: 0, estoques: 0, outrosAtivosCirculantes: 0,
  realizavelLongoPrazo: 0, imobilizado: 0,
  emprestimosCurtoPrazo: 0, fornecedores: 0, salariosAPagar: 0, outrasObrigacoesCirculantes: 0,
  passivoNaoCirculante: 0, patrimonioLiquido: 0,
});

export default function CreditoPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [politica, setPolitica] = useState<Politica | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const carregarClientes = useCallback(async () => {
    const lista = await apiCredito.listarClientes();
    setClientes(lista);
    setClienteId((atual) => atual ?? (lista.length > 0 ? lista[0].id : null));
  }, []);

  useEffect(() => {
    carregarClientes().catch((e) => setErro(e.message));
    apiCredito.politica().then(setPolitica).catch((e) => setErro(e.message));
  }, [carregarClientes]);

  const cliente = clientes.find((c) => c.id === clienteId) ?? null;

  return (
    <>
      {erro && <div className="erro">{erro}</div>}
      <ClientesPainel
        clientes={clientes}
        clienteId={clienteId}
        onSelecionar={setClienteId}
        onCriado={async (novo) => {
          await carregarClientes();
          setClienteId(novo.id);
        }}
        onErro={setErro}
      />
      {cliente && politica && (
        <ClienteDetalhe key={cliente.id} cliente={cliente} politica={politica} onErro={setErro} />
      )}
    </>
  );
}

function ClientesPainel(props: {
  clientes: Cliente[];
  clienteId: number | null;
  onSelecionar: (id: number) => void;
  onCriado: (c: Cliente) => void;
  onErro: (m: string) => void;
}) {
  const formVazio = {
    nome: '', cpfCnpj: '', tipo: 'PRODUTOR', municipio: '', uf: '',
    telefone: '', email: '', endereco: '',
  };
  const [novo, setNovo] = useState(formVazio);

  return (
    <section className="painel">
      <div className="linha-form">
        <Campo label="Cliente analisado">
          <select value={props.clienteId ?? ''} onChange={(e) => props.onSelecionar(Number(e.target.value))}>
            {props.clientes.length === 0 && <option value="">— nenhum cliente —</option>}
            {props.clientes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome} {c.uf ? `(${c.uf})` : ''}
              </option>
            ))}
          </select>
        </Campo>
      </div>
      <h3 className="titulo-form">Cadastrar novo cliente</h3>
      <div className="linha-form">
        <Campo label="Nome / Razão Social *" largo>
          <input placeholder="Nome completo ou razão social" value={novo.nome}
                 onChange={(e) => setNovo({ ...novo, nome: e.target.value })} />
        </Campo>
        <Campo label="CPF/CNPJ">
          <input placeholder="00.000.000/0000-00" className="campo-medio" value={novo.cpfCnpj}
                 onChange={(e) => setNovo({ ...novo, cpfCnpj: e.target.value })} />
        </Campo>
        <Campo label="Tipo">
          <select value={novo.tipo} onChange={(e) => setNovo({ ...novo, tipo: e.target.value })}>
            <option value="PRODUTOR">Produtor</option>
            <option value="REVENDA">Revenda</option>
            <option value="COOPERATIVA">Cooperativa</option>
            <option value="OUTRO">Outro</option>
          </select>
        </Campo>
        <Campo label="Telefone">
          <input placeholder="(00) 00000-0000" className="campo-medio" value={novo.telefone}
                 onChange={(e) => setNovo({ ...novo, telefone: e.target.value })} />
        </Campo>
        <Campo label="E-mail">
          <input type="email" placeholder="cliente@email.com" className="campo-medio" value={novo.email}
                 onChange={(e) => setNovo({ ...novo, email: e.target.value })} />
        </Campo>
      </div>
      <div className="linha-form">
        <Campo label="Endereço" largo>
          <input placeholder="Rua, número, bairro" className="campo-endereco" value={novo.endereco}
                 onChange={(e) => setNovo({ ...novo, endereco: e.target.value })} />
        </Campo>
        <Campo label="Município">
          <input placeholder="Município" className="campo-medio" value={novo.municipio}
                 onChange={(e) => setNovo({ ...novo, municipio: e.target.value })} />
        </Campo>
        <Campo label="UF">
          <input placeholder="SC" className="campo-uf" maxLength={2} value={novo.uf}
                 onChange={(e) => setNovo({ ...novo, uf: e.target.value.toUpperCase() })} />
        </Campo>
        <button
          disabled={!novo.nome.trim()}
          onClick={() =>
            apiCredito
              .criarCliente({
                nome: novo.nome.trim(),
                cpfCnpj: novo.cpfCnpj || undefined,
                tipo: novo.tipo,
                municipio: novo.municipio || undefined,
                uf: novo.uf || undefined,
                telefone: novo.telefone || undefined,
                email: novo.email || undefined,
                endereco: novo.endereco || undefined,
              })
              .then((c) => {
                setNovo(formVazio);
                props.onCriado(c);
              })
              .catch((e) => props.onErro(e.message))
          }
        >
          Cadastrar cliente
        </button>
      </div>
    </section>
  );
}

function ClienteDetalhe(props: { cliente: Cliente; politica: Politica; onErro: (m: string) => void }) {
  const { cliente, politica, onErro } = props;
  const [indicadores, setIndicadores] = useState<Indicadores | null>(null);
  const [analises, setAnalises] = useState<AnaliseResumo[]>([]);
  const [analiseAberta, setAnaliseAberta] = useState<AnaliseDetalhe | null>(null);

  const recarregar = useCallback(() => {
    apiCredito.indicadores(cliente.id).then(setIndicadores).catch((e) => onErro(e.message));
    apiCredito.analises(cliente.id).then(setAnalises).catch((e) => onErro(e.message));
  }, [cliente.id, onErro]);

  useEffect(recarregar, [recarregar]);

  return (
    <>
      <DemonstrativosPainel cliente={cliente} onSalvo={recarregar} onErro={onErro} />
      {indicadores && <IndicadoresPainel indicadores={indicadores} />}
      <AnalisesPainel
        cliente={cliente}
        analises={analises}
        onAbrir={(a) => setAnaliseAberta(a)}
        onMudou={recarregar}
        onErro={onErro}
      />
      {analiseAberta && (
        <AnaliseEditor
          key={analiseAberta.id}
          analise={analiseAberta}
          politica={politica}
          onAtualizada={(a) => {
            setAnaliseAberta(a);
            recarregar();
          }}
          onFechar={() => setAnaliseAberta(null)}
          onErro={onErro}
        />
      )}
    </>
  );
}

function DemonstrativosPainel(props: { cliente: Cliente; onSalvo: () => void; onErro: (m: string) => void }) {
  const anoAtual = new Date().getFullYear();
  const [colunas, setColunas] = useState<Demonstrativo[]>([
    demonstrativoVazio(anoAtual - 1),
    demonstrativoVazio(anoAtual),
  ]);

  useEffect(() => {
    apiCredito
      .demonstrativos(props.cliente.id)
      .then((lista) => {
        if (lista.length > 0) {
          setColunas(lista.slice(-2).map((d) => ({ ...d })));
        }
      })
      .catch((e) => props.onErro(e.message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.cliente.id]);

  const atualizar = (indice: number, chave: keyof Demonstrativo, texto: string) => {
    const valor = Number(texto.replace(',', '.'));
    setColunas((atual) =>
      atual.map((c, i) => (i === indice ? { ...c, [chave]: Number.isNaN(valor) ? 0 : valor } : c)),
    );
  };

  const salvar = (indice: number) => {
    const d = colunas[indice];
    apiCredito
      .salvarDemonstrativo(props.cliente.id, d.exercicio, d)
      .then(() => props.onSalvo())
      .catch((e) => props.onErro(e.message));
  };

  const grupos = [...new Set(CAMPOS_DEMONSTRATIVO.map((c) => c.grupo))];

  return (
    <section className="painel">
      <h2>Demonstrativos — {props.cliente.nome}</h2>
      <div className="rolagem">
        <table className="tabela-demonstrativo">
          <thead>
            <tr>
              <th className="col-item">Conta</th>
              {colunas.map((c, i) => (
                <th key={i}>
                  <input
                    type="number"
                    className="campo-exercicio"
                    value={c.exercicio}
                    onChange={(e) => {
                      const ano = Number(e.target.value);
                      setColunas((atual) =>
                        atual.map((col, j) => (j === i ? { ...col, exercicio: ano } : col)),
                      );
                    }}
                  />
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {grupos.map((grupo) => (
              <GrupoLinhas key={grupo} grupo={grupo} colunas={colunas} onAtualizar={atualizar} />
            ))}
          </tbody>
          <tfoot>
            <tr>
              <td className="col-item">Salvar exercício</td>
              {colunas.map((_, i) => (
                <td key={i} className="celula-acao">
                  <button onClick={() => salvar(i)}>Salvar {colunas[i].exercicio}</button>
                </td>
              ))}
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  );
}

function GrupoLinhas(props: {
  grupo: string;
  colunas: Demonstrativo[];
  onAtualizar: (indice: number, chave: keyof Demonstrativo, texto: string) => void;
}) {
  const campos = CAMPOS_DEMONSTRATIVO.filter((c) => c.grupo === props.grupo);
  return (
    <>
      <tr className="linha-grupo">
        <td colSpan={props.colunas.length + 1}>{props.grupo}</td>
      </tr>
      {campos.map((campo) => (
        <tr key={campo.chave}>
          <td className="col-item">{campo.rotulo}</td>
          {props.colunas.map((coluna, i) => (
            <td key={i} className="celula-valor">
              <input
                value={coluna[campo.chave] === 0 ? '' : String(coluna[campo.chave])}
                placeholder="0"
                onChange={(e) => props.onAtualizar(i, campo.chave, e.target.value)}
              />
            </td>
          ))}
        </tr>
      ))}
    </>
  );
}

function IndicadoresPainel(props: { indicadores: Indicadores }) {
  const { indicadores } = props;
  if (indicadores.exercicios.length === 0) return null;

  return (
    <section className="painel">
      <h2>Indicadores e diagnóstico</h2>
      <div className="cartoes">
        <Cartao titulo="ROE (média)" valor={pct(indicadores.roeMedia)} dica="Lucro Líquido / Patrimônio Líquido" />
        <Cartao titulo="Endividamento (média)" valor={pct(indicadores.endividamentoMedia)}
                dica="(PC + PNC) / Ativo Total" />
        <Cartao titulo="Liquidez Seca (média)" valor={num(indicadores.liquidezSecaMedia)}
                dica="(AC − Estoques) / PC" />
        <Cartao titulo="Evolução de Vendas" valor={pct(indicadores.evolucaoVendas)}
                dica="Variação da Receita Bruta entre os 2 últimos exercícios" />
      </div>
      <table className="tabela-fleuriet">
        <thead>
          <tr>
            <th>Exercício</th>
            <th>Tesouraria (T)</th>
            <th>NCG</th>
            <th>CDG</th>
            <th>Liquidez Corrente</th>
            <th>Diagnóstico (Fleuriet)</th>
          </tr>
        </thead>
        <tbody>
          {indicadores.exercicios.map((ex) => (
            <tr key={ex.exercicio}>
              <td>{ex.exercicio}</td>
              <td className={ex.tesouraria < 0 ? 'negativo' : 'positivo'}>{brl(ex.tesouraria)}</td>
              <td>{brl(ex.ncg)}</td>
              <td className={ex.cdg < 0 ? 'negativo' : 'positivo'}>{brl(ex.cdg)}</td>
              <td>{num(ex.liquidezCorrente)}</td>
              <td>
                <span className="selo" title={ex.descricaoDiagnostico}>
                  {ex.tipoFleuriet} — {ex.diagnostico}
                </span>
                {ex.diferencaBalanco !== 0 && (
                  <span className="alerta-balanco" title="Ativo Total ≠ Passivo + PL">
                    balanço não fecha ({brl(ex.diferencaBalanco)})
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}

function Cartao(props: { titulo: string; valor: string; dica: string }) {
  return (
    <div className="cartao" title={props.dica}>
      <div className="cartao-titulo">{props.titulo}</div>
      <div className="cartao-valor">{props.valor}</div>
    </div>
  );
}

function AnalisesPainel(props: {
  cliente: Cliente;
  analises: AnaliseResumo[];
  onAbrir: (a: AnaliseDetalhe) => void;
  onMudou: () => void;
  onErro: (m: string) => void;
}) {
  return (
    <section className="painel">
      <div className="cabecalho-secao">
        <h2>Análises de crédito</h2>
        <button
          onClick={() =>
            apiCredito
              .criarAnalise(props.cliente.id)
              .then((a) => {
                props.onMudou();
                props.onAbrir(a);
              })
              .catch((e) => props.onErro(e.message))
          }
        >
          Nova análise
        </button>
      </div>
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Criada em</th>
            <th>Status</th>
            <th>Score</th>
            <th>Rating</th>
            <th>Limite</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {props.analises.map((a) => (
            <tr key={a.id}>
              <td>{a.id}</td>
              <td>{new Date(a.criadaEm).toLocaleDateString('pt-BR')}</td>
              <td>
                <span className={a.status === 'CONCLUIDA' ? 'selo selo-ok' : 'selo'}>{a.status}</span>
              </td>
              <td>{a.score ?? '—'}</td>
              <td>{a.rating ?? '—'}</td>
              <td>{a.limiteCalculado !== null ? brl(a.limiteCalculado) : '—'}</td>
              <td className="celula-acao">
                <button onClick={() => apiCredito.analise(a.id).then(props.onAbrir).catch((e) => props.onErro(e.message))}>
                  Abrir
                </button>
                <button
                  className="botao-excluir"
                  title="Excluir análise"
                  onClick={() =>
                    apiCredito.excluirAnalise(a.id).then(props.onMudou).catch((e) => props.onErro(e.message))
                  }
                >
                  ×
                </button>
              </td>
            </tr>
          ))}
          {props.analises.length === 0 && (
            <tr>
              <td colSpan={7} className="vazio">Nenhuma análise ainda — clique em "Nova análise"</td>
            </tr>
          )}
        </tbody>
      </table>
    </section>
  );
}

function AnaliseEditor(props: {
  analise: AnaliseDetalhe;
  politica: Politica;
  onAtualizada: (a: AnaliseDetalhe) => void;
  onFechar: () => void;
  onErro: (m: string) => void;
}) {
  const { analise, politica } = props;
  const somenteLeitura = analise.status === 'CONCLUIDA';

  const [respostas, setRespostas] = useState<Record<number, number>>(() =>
    Object.fromEntries(analise.respostas.map((r) => [r.subcriterioId, r.opcaoId])),
  );
  const [justificativas, setJustificativas] = useState<Record<number, string>>(() =>
    Object.fromEntries(analise.respostas.map((r) => [r.subcriterioId, r.justificativa ?? ''])),
  );
  const [observacoes, setObservacoes] = useState(analise.observacoes ?? '');

  const grupos = useMemo(
    () => [...new Set(politica.subcriterios.map((s) => s.grupo))],
    [politica.subcriterios],
  );

  const salvar = (depois?: 'concluir') => {
    const payload = Object.entries(respostas).map(([subId, opcaoId]) => ({
      subcriterioId: Number(subId),
      opcaoId,
      justificativa: justificativas[Number(subId)] || null,
    }));
    apiCredito
      .salvarRespostas(analise.id, observacoes, payload)
      .then((a) => (depois === 'concluir' ? apiCredito.concluirAnalise(a.id) : Promise.resolve(a)))
      .then(props.onAtualizada)
      .catch((e) => props.onErro(e.message));
  };

  const aplicarSugestoes = () => {
    setRespostas((atual) => {
      const novo = { ...atual };
      for (const [subId, opcaoId] of Object.entries(analise.sugestoes)) {
        novo[Number(subId)] = opcaoId;
      }
      return novo;
    });
  };

  const temSugestoes = Object.keys(analise.sugestoes).length > 0;
  const resultado = analise.resultado;

  return (
    <section className="painel painel-analise">
      <div className="cabecalho-secao">
        <h2>
          Análise #{analise.id} — {analise.clienteNome}{' '}
          <span className={somenteLeitura ? 'selo selo-ok' : 'selo'}>{analise.status}</span>
        </h2>
        <div>
          {!somenteLeitura && temSugestoes && (
            <button onClick={aplicarSugestoes} title="Preenche os subcritérios 3.x com base nos demonstrativos">
              Preencher pelo balanço
            </button>
          )}{' '}
          <button
            className="botao-secundario"
            onClick={() => baixarParecer(analise.id).catch((e) => props.onErro((e as Error).message))}
            title="Gera o parecer de crédito em PDF"
          >
            Baixar parecer (PDF)
          </button>{' '}
          <button onClick={props.onFechar}>Fechar</button>
        </div>
      </div>

      <div className="resultado-analise">
        <Cartao titulo="Score" valor={resultado.score !== null ? String(resultado.score) : '—'} dica="0 a 100" />
        <Cartao titulo="Rating" valor={resultado.rating ?? '—'} dica="AAA (melhor) a H (pior)" />
        <Cartao titulo="% do rating" valor={pct(resultado.percentualLimite)} dica="Percentual aplicado sobre a base" />
        <Cartao titulo="Base (média PL + Faturamento)"
                valor={resultado.baseLimite !== null ? brl(resultado.baseLimite) : 'sem demonstrativo'}
                dica="Média entre Patrimônio Líquido e Receita Bruta do último exercício" />
        <Cartao titulo="Limite de crédito" valor={resultado.limite !== null ? brl(resultado.limite) : '—'}
                dica="Base × % do rating" />
      </div>

      {grupos.map((grupo) => (
        <div key={grupo} className="grupo-analise">
          <h3>{grupo}</h3>
          {politica.subcriterios
            .filter((s) => s.grupo === grupo)
            .map((sub) => (
              <div key={sub.id} className="subcriterio">
                <div className="subcriterio-titulo">
                  {sub.codigo} {sub.nome}
                  <span className="peso">peso {(sub.peso * 100).toFixed(1).replace('.', ',')}%</span>
                  {analise.sugestoes[sub.id] !== undefined && !somenteLeitura && (
                    <span className="selo selo-sugestao" title="Há sugestão calculada pelos demonstrativos">
                      sugestão do balanço
                    </span>
                  )}
                </div>
                <div className="opcoes">
                  {sub.opcoes.map((op) => (
                    <label key={op.id} className={respostas[sub.id] === op.id ? 'opcao marcada' : 'opcao'}>
                      <input
                        type="radio"
                        name={`sub-${sub.id}`}
                        disabled={somenteLeitura}
                        checked={respostas[sub.id] === op.id}
                        onChange={() => setRespostas({ ...respostas, [sub.id]: op.id })}
                      />
                      {op.rotulo} <span className="nota">({op.nota})</span>
                    </label>
                  ))}
                </div>
                <input
                  className="justificativa"
                  placeholder="Justificativa (opcional)"
                  disabled={somenteLeitura}
                  value={justificativas[sub.id] ?? ''}
                  onChange={(e) => setJustificativas({ ...justificativas, [sub.id]: e.target.value })}
                />
              </div>
            ))}
        </div>
      ))}

      <div className="grupo-analise">
        <h3>Observações gerais</h3>
        <textarea
          rows={3}
          disabled={somenteLeitura}
          value={observacoes}
          onChange={(e) => setObservacoes(e.target.value)}
          placeholder="Observações do analista / relato de campo complementar"
        />
      </div>

      <div className="linha-form">
        {!somenteLeitura && (
          <>
            <button onClick={() => salvar()}>Salvar rascunho</button>
            <button onClick={() => salvar('concluir')}>Salvar e concluir análise</button>
          </>
        )}
        {somenteLeitura && (
          <button
            onClick={() =>
              apiCredito.reabrirAnalise(analise.id).then(props.onAtualizada).catch((e) => props.onErro(e.message))
            }
          >
            Reabrir análise
          </button>
        )}
      </div>
    </section>
  );
}
