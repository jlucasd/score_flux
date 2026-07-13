import { useEffect, useState } from 'react';
import {
  Cliente, Demonstrativo, Faixa, Politica, SubcriterioPolitica,
  apiCredito, brl, num, pct,
} from './api';
import { Campo } from './ui';

const fmtPeso = (v: number) =>
  v.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 3 });

export default function PoliticaCreditoPage() {
  const [politica, setPolitica] = useState<Politica | null>(null);
  const [faixas, setFaixas] = useState<Faixa[]>([]);
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [ultimoDemo, setUltimoDemo] = useState<Demonstrativo | null>(null);
  // Peso atribuído (0–100) digitado por opção, exatamente como na planilha — nada é persistido.
  const [atribuidos, setAtribuidos] = useState<Record<number, number>>({});
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    apiCredito.politica().then(setPolitica).catch((e) => setErro(e.message));
    apiCredito.faixas().then(setFaixas).catch((e) => setErro(e.message));
    apiCredito.listarClientes().then(setClientes).catch((e) => setErro(e.message));
  }, []);

  useEffect(() => {
    if (clienteId === null) {
      setUltimoDemo(null);
      return;
    }
    apiCredito
      .demonstrativos(clienteId)
      .then((lista) => {
        const ordenados = [...lista].sort((a, b) => a.exercicio - b.exercicio);
        setUltimoDemo(ordenados.length > 0 ? ordenados[ordenados.length - 1] : null);
      })
      .catch((e) => setErro(e.message));
  }, [clienteId]);

  /** Fórmula da planilha: pesoCliente = Σ opções (peso do subcritério × pesoAtribuído / 100). */
  const pesoCliente = (sub: SubcriterioPolitica) =>
    sub.opcoes.reduce((t, op) => t + sub.peso * ((atribuidos[op.id] ?? 0) / 100), 0);

  if (!politica) return <>{erro && <div className="erro">{erro}</div>}</>;

  const grupos = [...new Set(politica.subcriterios.map((s) => s.grupo))];
  const totalSulGesso = politica.subcriterios.reduce((t, s) => t + s.peso, 0);
  const totalCliente = politica.subcriterios.reduce((t, s) => t + pesoCliente(s), 0);
  const score = totalCliente * 100;
  const scoreFmt = score.toFixed(2).replace('.', ',');

  const faixaAtual: Faixa | null =
    faixas.find((f) => score >= f.scoreMinimo) ?? (faixas.length > 0 ? faixas[faixas.length - 1] : null);

  const pl = ultimoDemo ? ultimoDemo.patrimonioLiquido : null;
  const faturamento = ultimoDemo ? ultimoDemo.receitaBruta : null;
  const media = pl !== null && faturamento !== null ? (pl + faturamento) / 2 : null;
  const limite = media !== null && faixaAtual ? media * faixaAtual.percentualLimite : null;

  return (
    <>
      {erro && <div className="erro">{erro}</div>}

      <section className="painel">
        <h2>Avaliação de Risco de Crédito de Cooperativas e Revendas</h2>
        <p className="dica">
          Simulador fiel à aba da planilha — os valores digitados aqui não são gravados; a análise
          oficial (com histórico e parecer) fica no menu Análise de Crédito.
        </p>
      </section>

      {grupos.map((grupo) => {
        const subs = politica.subcriterios.filter((s) => s.grupo === grupo);
        const numero = subs[0].codigo.split('.')[0];
        const somaSul = subs.reduce((t, s) => t + s.peso, 0);
        const somaCli = subs.reduce((t, s) => t + pesoCliente(s), 0);
        return (
          <section key={grupo} className="painel">
            <div className="grupo-analise">
              <h3>{numero}. {grupo}</h3>
              {subs.map((sub) => (
                <div key={sub.id} className="subcriterio">
                  <div className="subcriterio-titulo">
                    {sub.codigo} {sub.nome}
                    <span className="peso">Peso SulGesso: {fmtPeso(sub.peso)}</span>
                    <span className="peso-cliente">Peso Cliente: {fmtPeso(pesoCliente(sub))}</span>
                  </div>
                  {sub.descricao && <p className="descricao-criterio">{sub.descricao}</p>}
                  <table className="tabela-opcoes">
                    <thead>
                      <tr>
                        <th className="col-item">Opções Disponíveis</th>
                        <th>Peso</th>
                        <th>Peso Atribuído</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sub.opcoes.map((op) => (
                        <tr key={op.id}>
                          <td className="col-item">{op.rotulo}</td>
                          <td>{op.nota}</td>
                          <td className="celula-valor">
                            <input
                              type="number"
                              min={0}
                              max={100}
                              value={atribuidos[op.id] ?? 0}
                              onChange={(e) => {
                                const v = Math.max(0, Math.min(100, Number(e.target.value) || 0));
                                setAtribuidos((atual) => ({ ...atual, [op.id]: v }));
                              }}
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ))}
              <p className="destaque">
                Total da seção — Peso SulGesso: <strong>{fmtPeso(somaSul)}</strong> · Peso Cliente:{' '}
                <strong className="peso-cliente">{fmtPeso(somaCli)}</strong>
              </p>
            </div>
          </section>
        );
      })}

      <section className="painel">
        <h2>Total Geral e Score</h2>
        <div className="cartoes">
          <div className="cartao" title="Somatório dos pesos da política (deve fechar em 1,00)">
            <div className="cartao-titulo">TOTAL GERAL — Peso SulGesso</div>
            <div className="cartao-valor">{fmtPeso(totalSulGesso)}</div>
          </div>
          <div className="cartao" title="Somatório dos pesos atribuídos ao cliente">
            <div className="cartao-titulo">TOTAL GERAL — Peso Cliente</div>
            <div className="cartao-valor">{fmtPeso(totalCliente)}</div>
          </div>
          <div className="cartao" title="Peso Cliente total × 100">
            <div className="cartao-titulo">Score (0–100)</div>
            <div className="cartao-valor">{scoreFmt}</div>
          </div>
        </div>

        <table className="tabela-fleuriet">
          <thead>
            <tr>
              <th>Score mínimo</th>
              <th>Rating</th>
              <th>% Média entre PL e Faturamento</th>
            </tr>
          </thead>
          <tbody>
            {faixas.map((f) => (
              <tr key={f.rating} className={f === faixaAtual ? 'linha-faixa-atual' : ''}>
                <td>{num(f.scoreMinimo)}</td>
                <td>{f.rating}</td>
                <td>{pct(f.percentualLimite)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="painel">
        <h2>Rating do Cliente / Limite de Crédito</h2>
        <div className="linha-form">
          <Campo label="Cliente">
            <select
              value={clienteId ?? ''}
              onChange={(e) => setClienteId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">— selecione —</option>
              {clientes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nome} {c.uf ? `(${c.uf})` : ''}
                </option>
              ))}
            </select>
          </Campo>
        </div>
        <div className="cartoes">
          <div className="cartao" title="Patrimônio Líquido do último exercício">
            <div className="cartao-titulo">Patrimônio Líquido</div>
            <div className="cartao-valor">{pl !== null ? brl(pl) : '—'}</div>
          </div>
          <div className="cartao" title="Receita Bruta do último exercício">
            <div className="cartao-titulo">Faturamento</div>
            <div className="cartao-valor">{faturamento !== null ? brl(faturamento) : '—'}</div>
          </div>
          <div className="cartao" title="(PL + Faturamento) / 2">
            <div className="cartao-titulo">Média (PL + Faturamento)</div>
            <div className="cartao-valor">{media !== null ? brl(media) : '—'}</div>
          </div>
          <div className="cartao" title="Faixa atual do score simulado">
            <div className="cartao-titulo">Rating</div>
            <div className="cartao-valor">{faixaAtual ? faixaAtual.rating : '—'}</div>
          </div>
          <div className="cartao" title="Média × % da faixa atual do score">
            <div className="cartao-titulo">Limite de Crédito</div>
            <div className="cartao-valor">{limite !== null ? brl(limite) : '—'}</div>
          </div>
        </div>
      </section>
    </>
  );
}
