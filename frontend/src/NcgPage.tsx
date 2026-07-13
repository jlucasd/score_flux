import { useCallback, useEffect, useRef, useState } from 'react';
import { Cliente, Demonstrativo, apiCredito, brl, extrairBalancoPdf, num } from './api';
import { Campo, InputMoeda } from './ui';

interface Linha {
  chave: keyof Demonstrativo;
  rotulo: string;
}

const ATIVO_CIRCULANTE: Linha[] = [
  { chave: 'caixaBancos', rotulo: 'Caixa e Bancos' },
  { chave: 'aplicacoes', rotulo: 'Aplicações / Investimentos' },
  { chave: 'contasReceber', rotulo: 'Clientes' },
  { chave: 'estoques', rotulo: 'Estoques' },
  { chave: 'outrosAtivosCirculantes', rotulo: 'Outras Apropriações' },
];
const ATIVO_LONGO: Linha[] = [
  { chave: 'realizavelLongoPrazo', rotulo: 'Realizável Longo Prazo' },
  { chave: 'imobilizado', rotulo: 'Imobilizado' },
];
const PASSIVO_CIRCULANTE: Linha[] = [
  { chave: 'emprestimosCurtoPrazo', rotulo: 'Empréstimos' },
  { chave: 'fornecedores', rotulo: 'Fornecedores' },
  { chave: 'salariosAPagar', rotulo: 'Salários a Pagar' },
  { chave: 'outrasObrigacoesCirculantes', rotulo: 'Outras Obrigações' },
];
const PASSIVO_LONGO: Linha[] = [
  { chave: 'passivoNaoCirculante', rotulo: 'Não Circulante' },
  { chave: 'patrimonioLiquido', rotulo: 'Patrimônio Líquido' },
];

const TIPOS_FLEURIET = [
  ['Tipo I', '+', '−', '+', 'Excelente', 'Operação gera caixa e estrutura financeira sólida'],
  ['Tipo II', '+', '+', '+', 'Sólida', 'CDG cobre a NCG com folga'],
  ['Tipo III', '+', '+', '−', 'Atenção', 'Dependência de financiamento de curto prazo'],
  ['Tipo IV', '−', '+', '−', 'Insatisfatória', 'Operação consome caixa e estrutura inadequada'],
  ['Tipo V', '−', '−', '+', 'Estrutura Fraca', 'Caixa positivo, mas longo prazo financiado no curto'],
  ['Tipo VI', '−', '−', '−', 'Crítica', 'Desequilíbrio grave e risco de insolvência'],
];

const vazio = (exercicio: number): Demonstrativo => ({
  exercicio,
  receitaBruta: 0, lucroLiquido: 0,
  caixaBancos: 0, aplicacoes: 0, contasReceber: 0, estoques: 0, outrosAtivosCirculantes: 0,
  realizavelLongoPrazo: 0, imobilizado: 0,
  emprestimosCurtoPrazo: 0, fornecedores: 0, salariosAPagar: 0, outrasObrigacoesCirculantes: 0,
  passivoNaoCirculante: 0, patrimonioLiquido: 0,
});

const soma = (d: Demonstrativo, linhas: Linha[]) =>
  linhas.reduce((total, l) => total + (Number(d[l.chave]) || 0), 0);

/** Fórmulas da aba NCG, ao vivo (mesma lógica do backend, para reagir à digitação). */
function calcular(d: Demonstrativo) {
  const af = d.caixaBancos + d.aplicacoes;
  const pf = d.emprestimosCurtoPrazo;
  const ao = d.contasReceber + d.estoques + d.outrosAtivosCirculantes;
  const po = d.fornecedores + d.salariosAPagar + d.outrasObrigacoesCirculantes;
  const alp = d.realizavelLongoPrazo + d.imobilizado;
  const plp = d.passivoNaoCirculante + d.patrimonioLiquido;

  const ac = af + ao;
  const pc = pf + po;
  const tesouraria = af - pf;
  const ncg = ao - po;
  const cdg = plp - alp;
  const liquidezCorrente = pc === 0 ? null : ac / pc;
  const diferencaBalanco = ac + alp - (pc + plp);

  const cdgPos = cdg >= 0;
  const ncgPos = ncg >= 0;
  const tPos = tesouraria >= 0;
  let tipo: [string, string, string];
  if (cdgPos && !ncgPos && tPos) tipo = ['Tipo I', 'Excelente', 'Operação gera caixa e estrutura financeira sólida'];
  else if (cdgPos && ncgPos && tPos) tipo = ['Tipo II', 'Sólida', 'CDG cobre a NCG com folga'];
  else if (cdgPos && ncgPos) tipo = ['Tipo III', 'Atenção', 'Dependência de financiamento de curto prazo'];
  else if (!cdgPos && ncgPos) tipo = ['Tipo IV', 'Insatisfatória', 'Operação consome caixa e estrutura inadequada'];
  else if (!cdgPos && tPos) tipo = ['Tipo V', 'Estrutura Fraca', 'Caixa positivo, mas longo prazo financiado no curto'];
  else tipo = ['Tipo VI', 'Crítica', 'Desequilíbrio grave e risco de insolvência'];

  return { ac, pc, tesouraria, ncg, cdg, liquidezCorrente, diferencaBalanco, tipo };
}

export default function NcgPage() {
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [colunas, setColunas] = useState<Demonstrativo[]>([]);
  const [erro, setErro] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  useEffect(() => {
    apiCredito
      .listarClientes()
      .then((lista) => {
        setClientes(lista);
        setClienteId((atual) => atual ?? (lista.length > 0 ? lista[0].id : null));
      })
      .catch((e) => setErro(e.message));
  }, []);

  const carregar = useCallback(() => {
    if (clienteId === null) return;
    const anoAtual = new Date().getFullYear();
    apiCredito
      .demonstrativos(clienteId)
      .then((lista) => {
        setColunas(lista.length > 0
          ? lista.slice(-2).map((d) => ({ ...d }))
          : [vazio(anoAtual - 1), vazio(anoAtual)]);
      })
      .catch((e) => setErro(e.message));
  }, [clienteId]);

  useEffect(carregar, [carregar]);

  const atualizarCampo = (indice: number, chave: keyof Demonstrativo, valor: number) => {
    setColunas((atual) => atual.map((c, i) => (i === indice ? { ...c, [chave]: valor } : c)));
  };

  const salvar = (indice: number) => {
    if (clienteId === null) return;
    const d = colunas[indice];
    setErro(null);
    apiCredito
      .salvarDemonstrativo(clienteId, d.exercicio, d)
      .then(() => setAviso(`Exercício ${d.exercicio} salvo`))
      .catch((e) => setErro(e.message));
  };

  const importar = (indice: number, arquivo: File) => {
    setErro(null);
    setAviso(null);
    extrairBalancoPdf(arquivo)
      .then((r) => {
        setColunas((atual) =>
          atual.map((c, i) => {
            if (i !== indice) return c;
            const novo = { ...c, ...r.campos } as Demonstrativo;
            if (r.exercicioDetectado) novo.exercicio = r.exercicioDetectado;
            return novo;
          }),
        );
        setAviso(
          `PDF lido: ${r.camposEncontrados.length} campo(s) preenchido(s)` +
            (r.exercicioDetectado ? ` · exercício detectado: ${r.exercicioDetectado}` : '') +
            '. Confira os valores antes de salvar.',
        );
      })
      .catch((e) => setErro(e.message));
  };

  const ordenadas = [...colunas].sort((a, b) => a.exercicio - b.exercicio);

  return (
    <>
      {erro && <div className="erro">{erro}</div>}
      {aviso && <div className="aviso">{aviso}</div>}

      <section className="painel">
        <div className="linha-form">
          <Campo label="Cliente analisado">
            <select value={clienteId ?? ''} onChange={(e) => setClienteId(Number(e.target.value))}>
              {clientes.length === 0 && <option value="">— nenhum cliente —</option>}
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nome} {c.uf ? `(${c.uf})` : ''}
                </option>
              ))}
            </select>
          </Campo>
        </div>
        <p className="dica">
          Estrutura da aba "NCG" da planilha: balanço reclassificado por exercício. Preencha à mão ou
          importe um PDF de balanço/balancete — a leitura é uma sugestão, sempre confira antes de salvar.
        </p>
      </section>

      {clienteId !== null && (
        <div className="grade-ncg">
          {colunas.map((d, i) => (
            <CartaoExercicio
              key={i}
              demonstrativo={d}
              indice={i}
              onAtualizar={atualizarCampo}
              onSalvar={salvar}
              onImportar={importar}
            />
          ))}
        </div>
      )}

      {ordenadas.length > 0 && (
        <section className="painel">
          <h2>Cálculos</h2>
          <p className="dica">Atualizam automaticamente conforme os valores digitados acima — como as fórmulas da planilha.</p>
          <table className="tabela-fleuriet">
            <thead>
              <tr>
                <th>Exercício</th>
                <th>Liq. Corrente (AC/PC)</th>
                <th>Tesouraria (AF−PF)</th>
                <th>NCG (AO−PO)</th>
                <th>CDG (PLP−ALP)</th>
                <th>Diagnóstico</th>
              </tr>
            </thead>
            <tbody>
              {ordenadas.map((d) => {
                const c = calcular(d);
                return (
                  <tr key={d.exercicio}>
                    <td>{d.exercicio}</td>
                    <td>{c.liquidezCorrente === null ? '—' : num(c.liquidezCorrente)}</td>
                    <td className={c.tesouraria < 0 ? 'negativo' : 'positivo'}>{brl(c.tesouraria)}</td>
                    <td>{brl(c.ncg)}</td>
                    <td className={c.cdg < 0 ? 'negativo' : 'positivo'}>{brl(c.cdg)}</td>
                    <td>
                      <span className="selo" title={c.tipo[2]}>
                        {c.tipo[0]} — {c.tipo[1]}
                      </span>
                      {c.diferencaBalanco !== 0 && (
                        <span className="alerta-balanco" title="Ativo Total ≠ Passivo + PL">
                          balanço não fecha ({brl(c.diferencaBalanco)})
                        </span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </section>
      )}

      {ordenadas.length >= 2 && (
        <section className="painel">
          <h2>Efeito Tesoura — CDG × NCG × Tesouraria</h2>
          <p className="dica">
            Quando a NCG cresce mais que o CDG, a tesoura "abre" e a Tesouraria fica negativa —
            a empresa passa a depender de financiamento de curto prazo.
          </p>
          <GraficoTesoura dados={ordenadas.map((d) => ({ ano: d.exercicio, ...calcular(d) }))} />
        </section>
      )}

      <section className="painel">
        <h2>Possíveis resultados (referência)</h2>
        <table className="tabela-fleuriet">
          <thead>
            <tr>
              <th>Tipo</th>
              <th>CDG</th>
              <th>NCG</th>
              <th>Tesouraria</th>
              <th>Diagnóstico</th>
              <th>Descrição</th>
            </tr>
          </thead>
          <tbody>
            {TIPOS_FLEURIET.map((linha) => (
              <tr key={linha[0]}>
                {linha.map((celula, j) => (
                  <td key={j} className={j === 5 ? 'col-item' : ''}>{celula}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </>
  );
}

function CartaoExercicio(props: {
  demonstrativo: Demonstrativo;
  indice: number;
  onAtualizar: (indice: number, chave: keyof Demonstrativo, valor: number) => void;
  onSalvar: (indice: number) => void;
  onImportar: (indice: number, arquivo: File) => void;
}) {
  const { demonstrativo: d, indice } = props;
  const inputArquivo = useRef<HTMLInputElement>(null);

  const ac = soma(d, ATIVO_CIRCULANTE);
  const totalAtivo = ac + soma(d, ATIVO_LONGO);
  const pc = soma(d, PASSIVO_CIRCULANTE);
  const totalPassivo = pc + soma(d, PASSIVO_LONGO);

  const bloco = (titulo: string, linhas: Linha[], subtotal?: [string, number]) => (
    <table className="tabela-ncg">
      <thead>
        <tr>
          <th colSpan={2}>{titulo}</th>
        </tr>
      </thead>
      <tbody>
        {linhas.map((l) => (
          <tr key={l.chave}>
            <td className="col-item">{l.rotulo}</td>
            <td className="celula-valor">
              <InputMoeda
                valor={Number(d[l.chave]) || 0}
                onChange={(v) => props.onAtualizar(indice, l.chave, v)}
              />
            </td>
          </tr>
        ))}
        {subtotal && (
          <tr>
            <td className="col-item total">{subtotal[0]}</td>
            <td className="total">{brl(subtotal[1])}</td>
          </tr>
        )}
      </tbody>
    </table>
  );

  return (
    <section className="painel">
      <div className="cabecalho-secao">
        <Campo label="Exercício">
          <input
            type="number"
            className="campo-exercicio"
            value={d.exercicio}
            onChange={(e) => props.onAtualizar(indice, 'exercicio', Number(e.target.value) || 0)}
          />
        </Campo>
        <div className="acoes">
          <input
            ref={inputArquivo}
            type="file"
            accept="application/pdf"
            style={{ display: 'none' }}
            onChange={(e) => {
              const arquivo = e.target.files?.[0];
              if (arquivo) props.onImportar(indice, arquivo);
              e.target.value = '';
            }}
          />
          <button className="botao-secundario" onClick={() => inputArquivo.current?.click()}>
            Importar PDF
          </button>
          <button onClick={() => props.onSalvar(indice)}>Salvar {d.exercicio}</button>
        </div>
      </div>

      <div className="bloco-ap">
        <div>
          {bloco('ATIVO — Circulante', ATIVO_CIRCULANTE, ['Ativo Circulante', ac])}
          {bloco('ATIVO — Longo Prazo', ATIVO_LONGO, ['Total do Ativo', totalAtivo])}
        </div>
        <div>
          {bloco('PASSIVO — Circulante', PASSIVO_CIRCULANTE, ['Passivo Circulante', pc])}
          {bloco('PASSIVO — Longo Prazo e PL', PASSIVO_LONGO, ['Total do Passivo', totalPassivo])}
        </div>
      </div>
      {totalAtivo !== totalPassivo && (
        <p className="alerta-balanco">
          Balanço não fecha: Ativo {brl(totalAtivo)} × Passivo + PL {brl(totalPassivo)}
        </p>
      )}
    </section>
  );
}

// ---- Gráfico de linhas (efeito tesoura): eixo Y em R$, eixo X em anos ----

const SERIES: { chave: 'cdg' | 'ncg' | 'tesouraria'; rotulo: string; cor: string }[] = [
  { chave: 'cdg', rotulo: 'CDG', cor: '#1c6b33' },
  { chave: 'ncg', rotulo: 'NCG', cor: '#b8860b' },
  { chave: 'tesouraria', rotulo: 'Tesouraria', cor: '#a1352a' },
];

function GraficoTesoura(props: { dados: { ano: number; cdg: number; ncg: number; tesouraria: number }[] }) {
  const { dados } = props;
  const largura = 680;
  const altura = 320;
  const margem = { esq: 90, dir: 30, topo: 34, base: 40 };

  const valores = dados.flatMap((d) => [d.cdg, d.ncg, d.tesouraria, 0]);
  let min = Math.min(...valores);
  let max = Math.max(...valores);
  if (min === max) max = min + 1;
  const folga = (max - min) * 0.12;
  min -= folga;
  max += folga;

  const x = (i: number) =>
    margem.esq + (dados.length === 1 ? 0 : (i * (largura - margem.esq - margem.dir)) / (dados.length - 1));
  const y = (v: number) =>
    margem.topo + ((max - v) * (altura - margem.topo - margem.base)) / (max - min);

  const compacto = (v: number) =>
    'R$ ' + v.toLocaleString('pt-BR', { notation: 'compact', maximumFractionDigits: 1 });

  const ticks = [0, 1, 2, 3, 4].map((i) => min + ((max - min) * i) / 4);

  return (
    <div className="grafico-tesoura">
      <div className="legenda-grafico">
        {SERIES.map((s) => (
          <span key={s.chave}>
            <i style={{ background: s.cor }} /> {s.rotulo}
          </span>
        ))}
      </div>
      <svg viewBox={`0 0 ${largura} ${altura}`} role="img" aria-label="Gráfico do efeito tesoura">
        {/* linhas de grade + rótulos do eixo Y (R$) */}
        {ticks.map((t, i) => (
          <g key={i}>
            <line x1={margem.esq} x2={largura - margem.dir} y1={y(t)} y2={y(t)}
                  stroke="#e3eae3" strokeWidth={1} />
            <text x={margem.esq - 8} y={y(t) + 4} textAnchor="end" fontSize={11} fill="#5a6b5a">
              {compacto(t)}
            </text>
          </g>
        ))}
        {/* linha do zero */}
        {min < 0 && max > 0 && (
          <line x1={margem.esq} x2={largura - margem.dir} y1={y(0)} y2={y(0)}
                stroke="#8a978a" strokeWidth={1.5} strokeDasharray="5 4" />
        )}
        {/* eixo X: anos */}
        {dados.map((d, i) => (
          <text key={d.ano} x={x(i)} y={altura - 12} textAnchor="middle" fontSize={12}
                fontWeight={600} fill="#3c4b3c">
            {d.ano}
          </text>
        ))}
        {/* séries */}
        {SERIES.map((s) => (
          <g key={s.chave}>
            <polyline
              fill="none"
              stroke={s.cor}
              strokeWidth={2.5}
              points={dados.map((d, i) => `${x(i)},${y(d[s.chave])}`).join(' ')}
            />
            {dados.map((d, i) => (
              <g key={i}>
                <circle cx={x(i)} cy={y(d[s.chave])} r={4.5} fill={s.cor}>
                  <title>{`${s.rotulo} ${d.ano}: ${brl(d[s.chave])}`}</title>
                </circle>
                <text x={x(i)} y={y(d[s.chave]) - 10} textAnchor="middle" fontSize={10.5} fill={s.cor}>
                  {compacto(d[s.chave])}
                </text>
              </g>
            ))}
          </g>
        ))}
      </svg>
    </div>
  );
}
