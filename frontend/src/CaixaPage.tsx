import { useCallback, useEffect, useState } from 'react';
import { Extrato, apiCaixa, brl } from './api';
import { Campo } from './ui';

const hoje = () => new Date().toISOString().slice(0, 10);

export default function CaixaPage() {
  const [filtro, setFiltro] = useState('');
  const [extrato, setExtrato] = useState<Extrato | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [novo, setNovo] = useState({
    data: hoje(),
    classificacao: '',
    historico: '',
    valor: '',
    tipo: 'entrada',
    status: 'REALIZADO',
  });

  const carregar = useCallback(() => {
    apiCaixa.extrato(filtro || undefined).then(setExtrato).catch((e) => setErro(e.message));
  }, [filtro]);

  useEffect(carregar, [carregar]);

  const adicionar = () => {
    setErro(null);
    const valor = Number(novo.valor.replace(',', '.'));
    if (Number.isNaN(valor) || valor < 0) {
      setErro('Informe um valor válido');
      return;
    }
    apiCaixa
      .criar(
        {
          data: novo.data,
          classificacao: novo.classificacao || undefined,
          historico: novo.historico || undefined,
          entrada: novo.tipo === 'entrada' ? valor : 0,
          saida: novo.tipo === 'saida' ? valor : 0,
          status: novo.status,
        },
        filtro || undefined,
      )
      .then((e) => {
        setExtrato(e);
        setNovo({ ...novo, classificacao: '', historico: '', valor: '' });
      })
      .catch((err) => setErro(err.message));
  };

  return (
    <>
      {erro && <div className="erro">{erro}</div>}

      <section className="painel">
        <div className="cabecalho-secao">
          <h2>Fluxo de Caixa — saldo corrente</h2>
          <Campo label="Filtrar lançamentos">
            <select value={filtro} onChange={(e) => setFiltro(e.target.value)}>
              <option value="">Realizado + Previsto</option>
              <option value="REALIZADO">Somente realizado</option>
              <option value="PREVISTO">Somente previsto</option>
            </select>
          </Campo>
        </div>

        {extrato && (
          <>
            <div className="cartoes">
              <Cartao titulo="Total de entradas" valor={brl(extrato.totalEntradas)} classe="positivo" />
              <Cartao titulo="Total de saídas" valor={brl(extrato.totalSaidas)} classe="negativo" />
              <Cartao
                titulo="Saldo final"
                valor={brl(extrato.saldoFinal)}
                classe={extrato.saldoFinal < 0 ? 'negativo' : 'positivo'}
              />
            </div>

            <div className="rolagem">
              <table>
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Classificação</th>
                    <th>Histórico</th>
                    <th>Entrada</th>
                    <th>Saída</th>
                    <th>Saldo</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {extrato.linhas.map((l) => (
                    <tr key={l.id}>
                      <td>{new Date(l.data + 'T00:00:00').toLocaleDateString('pt-BR')}</td>
                      <td className="col-item">{l.classificacao ?? '—'}</td>
                      <td className="col-item">{l.historico ?? '—'}</td>
                      <td className="positivo">{l.entrada > 0 ? brl(l.entrada) : '—'}</td>
                      <td className="negativo">{l.saida > 0 ? brl(l.saida) : '—'}</td>
                      <td className={l.saldo < 0 ? 'negativo total' : 'total'}>{brl(l.saldo)}</td>
                      <td>
                        <span className={l.status === 'REALIZADO' ? 'selo selo-ok' : 'selo'}>{l.status}</span>
                      </td>
                      <td>
                        <button
                          className="botao-excluir"
                          title="Excluir lançamento"
                          onClick={() =>
                            apiCaixa.excluir(l.id, filtro || undefined).then(setExtrato).catch((e) => setErro(e.message))
                          }
                        >
                          ×
                        </button>
                      </td>
                    </tr>
                  ))}
                  {extrato.linhas.length === 0 && (
                    <tr>
                      <td colSpan={8} className="vazio">
                        Nenhum lançamento — adicione o primeiro abaixo
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}

        <div className="linha-form">
          <Campo label="Data">
            <input type="date" value={novo.data} onChange={(e) => setNovo({ ...novo, data: e.target.value })} />
          </Campo>
          <Campo label="Classificação">
            <input
              placeholder="Ex.: Pagamentos"
              className="campo-medio"
              value={novo.classificacao}
              onChange={(e) => setNovo({ ...novo, classificacao: e.target.value })}
            />
          </Campo>
          <Campo label="Histórico">
            <input
              placeholder="Descrição do lançamento"
              className="campo-medio"
              value={novo.historico}
              onChange={(e) => setNovo({ ...novo, historico: e.target.value })}
            />
          </Campo>
          <Campo label="Tipo">
            <select value={novo.tipo} onChange={(e) => setNovo({ ...novo, tipo: e.target.value })}>
              <option value="entrada">Entrada</option>
              <option value="saida">Saída</option>
            </select>
          </Campo>
          <Campo label="Valor (R$)">
            <input
              placeholder="0,00"
              className="campo-ano"
              value={novo.valor}
              onChange={(e) => setNovo({ ...novo, valor: e.target.value })}
            />
          </Campo>
          <Campo label="Situação">
            <select value={novo.status} onChange={(e) => setNovo({ ...novo, status: e.target.value })}>
              <option value="REALIZADO">Realizado</option>
              <option value="PREVISTO">Previsto</option>
            </select>
          </Campo>
          <button disabled={!novo.valor} onClick={adicionar}>
            Adicionar lançamento
          </button>
        </div>
      </section>
    </>
  );
}

function Cartao(props: { titulo: string; valor: string; classe?: string }) {
  return (
    <div className="cartao">
      <div className="cartao-titulo">{props.titulo}</div>
      <div className={`cartao-valor ${props.classe ?? ''}`}>{props.valor}</div>
    </div>
  );
}
