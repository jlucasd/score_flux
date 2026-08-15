import { useCallback, useEffect, useRef, useState } from 'react';
import { Cliente, Demonstrativo, apiCredito, brl, extrairBalancoPdf, num, pct } from './api';
import { Campo, InputMoeda } from './ui';
import { useAnalise, useAutosave } from './contexto';

const ajustarAgregado = (
  d: Demonstrativo,
  novoTotal: number,
  somaAtual: number,
  campoResidual: keyof Demonstrativo,
): Partial<Demonstrativo> => {
  const diff = novoTotal - somaAtual;
  return { [campoResidual]: Number(d[campoResidual]) + diff };
};

const CAMPOS_MONETARIOS: (keyof Demonstrativo)[] = [
  'receitaBruta', 'lucroLiquido', 'caixaBancos', 'aplicacoes', 'contasReceber', 'estoques',
  'outrosAtivosCirculantes', 'realizavelLongoPrazo', 'imobilizado', 'emprestimosCurtoPrazo',
  'fornecedores', 'salariosAPagar', 'outrasObrigacoesCirculantes', 'passivoNaoCirculante', 'patrimonioLiquido',
];
const temConteudo = (d: Demonstrativo) => d.id != null || CAMPOS_MONETARIOS.some((k) => Number(d[k]) !== 0);

const vazio = (exercicio: number): Demonstrativo => ({
  exercicio,
  receitaBruta: 0, lucroLiquido: 0,
  caixaBancos: 0, aplicacoes: 0, contasReceber: 0, estoques: 0, outrosAtivosCirculantes: 0,
  realizavelLongoPrazo: 0, imobilizado: 0,
  emprestimosCurtoPrazo: 0, fornecedores: 0, salariosAPagar: 0, outrasObrigacoesCirculantes: 0,
  passivoNaoCirculante: 0, patrimonioLiquido: 0,
});

// ---- Derivados e indicadores (mesmas fórmulas da aba "Análise Indicadores") ----

const calcPC = (d: Demonstrativo) =>
  d.emprestimosCurtoPrazo + d.fornecedores + d.salariosAPagar + d.outrasObrigacoesCirculantes;
const calcAC = (d: Demonstrativo) =>
  d.caixaBancos + d.aplicacoes + d.contasReceber + d.estoques + d.outrosAtivosCirculantes;
const calcAT = (d: Demonstrativo) => calcAC(d) + d.realizavelLongoPrazo + d.imobilizado;

const roe = (d: Demonstrativo) =>
  d.patrimonioLiquido === 0 ? null : d.lucroLiquido / d.patrimonioLiquido;
const endividamento = (d: Demonstrativo) => {
  const at = calcAT(d);
  return at === 0 ? null : (calcPC(d) + d.passivoNaoCirculante) / at;
};
const liquidezSeca = (d: Demonstrativo) => {
  const pc = calcPC(d);
  return pc === 0 ? null : (calcAC(d) - d.estoques) / pc;
};

const mediaCalculaveis = (valores: (number | null)[]) => {
  const validos = valores.filter((v): v is number => v !== null);
  return validos.length === 0 ? null : validos.reduce((a, b) => a + b, 0) / validos.length;
};

export default function AnaliseIndicadoresPage() {
  const { clienteId, setClienteId, ano, setAno } = useAnalise();
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [colunas, setColunas] = useState<Demonstrativo[]>([]);
  const [sujo, setSujo] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  useEffect(() => {
    apiCredito
      .listarClientes()
      .then((lista) => {
        setClientes(lista);
        if (clienteId === null && lista.length > 0) setClienteId(lista[0].id);
      })
      .catch((e) => setErro(e.message));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const carregar = useCallback(() => {
    if (clienteId === null) {
      setColunas([]);
      return;
    }
    apiCredito
      .demonstrativos(clienteId)
      .then((lista) => {
        const porAno = new Map(lista.map((d) => [d.exercicio, d]));
        setColunas([ano - 1, ano].map((y) => {
          const existente = porAno.get(y);
          return existente ? { ...existente } : vazio(y);
        }));
        setSujo(false);
      })
      .catch((e) => setErro(e.message));
  }, [clienteId, ano]);

  useEffect(carregar, [carregar]);

  const atualizar = (indice: number, chave: keyof Demonstrativo, valor: number) => {
    setColunas((atual) => atual.map((c, i) => (i === indice ? { ...c, [chave]: valor } : c)));
    setSujo(true);
  };

  const atualizarPatch = (indice: number, patch: Partial<Demonstrativo>) => {
    setColunas((atual) => atual.map((c, i) => (i === indice ? { ...c, ...patch } : c)));
    setSujo(true);
  };

  const salvarColuna = (d: Demonstrativo) => {
    if (clienteId === null || !temConteudo(d)) return Promise.resolve();
    return apiCredito.salvarDemonstrativo(clienteId, d.exercicio, d);
  };

  const salvar = (indice: number) => {
    setErro(null);
    salvarColuna(colunas[indice])
      .then(() => { setSujo(false); setAviso(`Exercício ${colunas[indice].exercicio} salvo`); })
      .catch((e) => setErro(e.message));
  };

  useAutosave(colunas, (cols) => {
    if (!sujo || clienteId === null) return;
    Promise.all(cols.map(salvarColuna))
      .then(() => { setSujo(false); setAviso('Salvo automaticamente'); })
      .catch((e) => setErro(e.message));
  });

  const importar = (indice: number, arquivo: File) => {
    setErro(null);
    setAviso(null);
    extrairBalancoPdf(arquivo)
      .then((r) => {
        setColunas((atual) =>
          atual.map((c, i) => (i === indice ? { ...c, ...(r.campos as Partial<Demonstrativo>) } : c)),
        );
        setSujo(true);
        setAviso(`PDF lido: ${r.camposEncontrados.length} campo(s) preenchido(s). Confira antes de salvar.`);
      })
      .catch((e) => setErro(e.message));
  };

  const [ano1, ano2] = colunas;
  const evolucao =
    ano1 && ano2 && ano1.receitaBruta !== 0 ? ano2.receitaBruta / ano1.receitaBruta - 1 : null;
  const mediaBrl = (valores: number[]) =>
    brl(valores.reduce((a, b) => a + b, 0) / Math.max(valores.length, 1));

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
          <Campo label="Ano de referência">
            <input
              type="number"
              className="campo-ano"
              value={ano}
              onChange={(e) => setAno(Number(e.target.value) || ano)}
            />
          </Campo>
        </div>
        <p className="dica">
          Mesma base de dados da tela Balanço — os totais derivados vêm das contas granulares do balanço.
          Os valores são <strong>salvos automaticamente</strong>.
        </p>
      </section>

      {clienteId !== null && colunas.length === 2 && (
        <section className="painel">
          <h2>Análise Indicadores</h2>
          <div className="rolagem">
            <table className="tabela-demonstrativo">
              <thead>
                <tr>
                  <th className="col-item">Conta / Indicador</th>
                  {colunas.map((c, i) => (
                    <th key={i}>{c.exercicio}</th>
                  ))}
                  <th>Média</th>
                </tr>
              </thead>
              <tbody>
                <LinhaIndicador
                  rotulo="Evolução de venda"
                  valores={colunas.map((d) => brl(d.receitaBruta))}
                  media={evolucao === null ? '—' : pct(evolucao)}
                />
                <LinhaConta rotulo="Receita Bruta (em R$)" chave="receitaBruta"
                            colunas={colunas} media={mediaBrl} onAtualizar={atualizar} />
                <LinhaIndicador
                  rotulo="Rentabilidade sobre o PL - ROE"
                  valores={colunas.map((d) => pct(roe(d)))}
                  media={pct(mediaCalculaveis(colunas.map(roe)))}
                />
                <LinhaConta rotulo="Lucro Líquido (em R$)" chave="lucroLiquido"
                            colunas={colunas} media={mediaBrl} onAtualizar={atualizar} />
                <LinhaConta rotulo="Patrimônio Líquido (em R$)" chave="patrimonioLiquido"
                            colunas={colunas} media={mediaBrl} onAtualizar={atualizar} />
                <LinhaIndicador
                  rotulo="Grau Endividamento sobre o Ativo Total"
                  valores={colunas.map((d) => pct(endividamento(d)))}
                  media={pct(mediaCalculaveis(colunas.map(endividamento)))}
                />
                <LinhaAgregado rotulo="Passivo Circulante (em R$)" colunas={colunas}
                               calc={calcPC} campoResidual="outrasObrigacoesCirculantes" media={mediaBrl} onAtualizar={atualizarPatch} />
                <LinhaConta rotulo="Passivo Não Circulante (em R$)" chave="passivoNaoCirculante"
                            colunas={colunas} media={mediaBrl} onAtualizar={atualizar} />
                <LinhaAgregado rotulo="Ativo Total (em R$)" colunas={colunas}
                               calc={calcAT} campoResidual="imobilizado" media={mediaBrl} onAtualizar={atualizarPatch} />
                <LinhaIndicador
                  rotulo="Grau Liquidez Seca"
                  valores={colunas.map((d) => num(liquidezSeca(d)))}
                  media={num(mediaCalculaveis(colunas.map(liquidezSeca)))}
                />
                <LinhaAgregado rotulo="Ativo Circulante" colunas={colunas}
                               calc={calcAC} campoResidual="outrosAtivosCirculantes" media={mediaBrl} onAtualizar={atualizarPatch} />
                <LinhaConta rotulo="Estoques (em R$)" chave="estoques"
                            colunas={colunas} media={mediaBrl} onAtualizar={atualizar} />
                <LinhaAgregado rotulo="Passivo Circulante" colunas={colunas}
                               calc={calcPC} campoResidual="outrasObrigacoesCirculantes" media={mediaBrl} onAtualizar={atualizarPatch} />
              </tbody>
              <tfoot>
                <tr>
                  <td className="col-item">Ações</td>
                  {colunas.map((d, i) => (
                    <CelulaAcoes
                      key={i}
                      exercicio={d.exercicio}
                      onImportar={(f) => importar(i, f)}
                      onSalvar={() => salvar(i)}
                      onLimpar={() => {
                        setColunas((atual) => atual.map((c, j) => (j === i ? { ...vazio(c.exercicio), id: c.id } : c)));
                        setSujo(true);
                      }}
                    />
                  ))}
                  <td></td>
                </tr>
              </tfoot>
            </table>
          </div>
        </section>
      )}
    </>
  );
}

/** Linha de indicador calculado ao vivo (destacada, como as linhas de fórmula da aba). */
function LinhaIndicador(props: { rotulo: string; valores: string[]; media: string }) {
  return (
    <tr className="linha-indicador">
      <td className="col-item">{props.rotulo}</td>
      {props.valores.map((v, i) => (
        <td key={i}>{v}</td>
      ))}
      <td>{props.media}</td>
    </tr>
  );
}

/** Linha de conta editável com máscara de moeda. */
function LinhaConta(props: {
  rotulo: string;
  chave: keyof Demonstrativo;
  colunas: Demonstrativo[];
  media: (valores: number[]) => string;
  onAtualizar: (indice: number, chave: keyof Demonstrativo, valor: number) => void;
}) {
  return (
    <tr>
      <td className="col-item">{props.rotulo}</td>
      {props.colunas.map((d, i) => (
        <td key={i} className="celula-valor">
          <InputMoeda
            valor={Number(d[props.chave]) || 0}
            onChange={(v) => props.onAtualizar(i, props.chave, v)}
          />
        </td>
      ))}
      <td>{props.media(props.colunas.map((d) => Number(d[props.chave]) || 0))}</td>
    </tr>
  );
}

function LinhaAgregado(props: {
  rotulo: string;
  colunas: Demonstrativo[];
  calc: (d: Demonstrativo) => number;
  campoResidual: keyof Demonstrativo;
  media: (valores: number[]) => string;
  onAtualizar: (indice: number, patch: Partial<Demonstrativo>) => void;
}) {
  return (
    <tr>
      <td className="col-item">{props.rotulo}</td>
      {props.colunas.map((d, i) => (
        <td key={i} className="celula-valor">
          <InputMoeda
            valor={props.calc(d)}
            onChange={(v) => props.onAtualizar(i, ajustarAgregado(d, v, props.calc(d), props.campoResidual))}
          />
        </td>
      ))}
      <td>{props.media(props.colunas.map(props.calc))}</td>
    </tr>
  );
}

function CelulaAcoes(props: {
  exercicio: number;
  onImportar: (arquivo: File) => void;
  onSalvar: () => void;
  onLimpar: () => void;
}) {
  const inputArquivo = useRef<HTMLInputElement>(null);
  return (
    <td className="celula-acao">
      <input
        ref={inputArquivo}
        type="file"
        accept="application/pdf"
        style={{ display: 'none' }}
        onChange={(e) => {
          const arquivo = e.target.files?.[0];
          if (arquivo) props.onImportar(arquivo);
          e.target.value = '';
        }}
      />
      <button className="botao-secundario" onClick={() => inputArquivo.current?.click()}>
        Importar PDF
      </button>{' '}
      <button className="botao-secundario" onClick={props.onLimpar}>
        Limpar
      </button>{' '}
      <button onClick={props.onSalvar}>Salvar {props.exercicio}</button>
    </td>
  );
}
