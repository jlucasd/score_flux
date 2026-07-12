import { Fragment, useCallback, useEffect, useState } from 'react';
import { Carteira, MovimentoCarteira, PosicaoCarteira, apiCarteira, brl } from './api';
import { Campo } from './ui';

const hoje = () => new Date().toISOString().slice(0, 10);

export default function CarteiraPage() {
  const [carteira, setCarteira] = useState<Carteira | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [expandido, setExpandido] = useState<number | null>(null);

  const carregar = useCallback(() => {
    apiCarteira.carteira().then(setCarteira).catch((e) => setErro(e.message));
  }, []);

  useEffect(carregar, [carregar]);

  const selo = (status: string) => {
    if (status === 'OK') return 'selo selo-ok';
    if (status === 'BLOQUEAR') return 'selo selo-bloquear';
    return 'selo';
  };

  return (
    <>
      {erro && <div className="erro">{erro}</div>}

      <section className="painel">
        <h2>Carteira de crédito</h2>
        {carteira && (
          <>
            <div className="cartoes">
              <Cartao titulo="Limite total aprovado" valor={brl(carteira.totalLimite)} />
              <Cartao titulo="Saldo em aberto" valor={brl(carteira.totalSaldoAberto)} />
              <Cartao
                titulo="Crédito disponível"
                valor={brl(carteira.totalDisponivel)}
                classe={carteira.totalDisponivel < 0 ? 'negativo' : 'positivo'}
              />
            </div>

            <table>
              <thead>
                <tr>
                  <th>Cliente</th>
                  <th>Rating</th>
                  <th>Limite aprovado</th>
                  <th>Saldo em aberto</th>
                  <th>Disponível</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {carteira.posicoes.map((p) => (
                  <Fragment key={p.clienteId}>
                    <tr>
                      <td className="col-item">{p.clienteNome}</td>
                      <td>{p.rating ?? '—'}</td>
                      <td>{p.limite !== null ? brl(p.limite) : '—'}</td>
                      <td>{brl(p.saldoAberto)}</td>
                      <td className={p.disponivel !== null && p.disponivel < 0 ? 'negativo' : ''}>
                        {p.disponivel !== null ? brl(p.disponivel) : '—'}
                      </td>
                      <td>
                        <span className={selo(p.status)}>{p.status}</span>
                      </td>
                      <td className="celula-acao">
                        <button onClick={() => setExpandido(expandido === p.clienteId ? null : p.clienteId)}>
                          {expandido === p.clienteId ? 'Fechar' : 'Movimentos'}
                        </button>
                      </td>
                    </tr>
                    {expandido === p.clienteId && (
                      <tr>
                        <td colSpan={7} className="celula-movimentos">
                          <Movimentos posicao={p} onMudou={carregar} onErro={setErro} />
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
                {carteira.posicoes.length === 0 && (
                  <tr>
                    <td colSpan={7} className="vazio">
                      Nenhum cliente cadastrado — cadastre em "Análise de Crédito"
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
            <p className="destaque">
              <span className="selo selo-ok">OK</span> dentro do limite ·{' '}
              <span className="selo selo-bloquear">BLOQUEAR</span> saldo em aberto excede o limite ·{' '}
              <span className="selo">SEM_LIMITE</span> cliente sem análise concluída
            </p>
          </>
        )}
      </section>
    </>
  );
}

function Movimentos(props: { posicao: PosicaoCarteira; onMudou: () => void; onErro: (m: string) => void }) {
  const [movimentos, setMovimentos] = useState<MovimentoCarteira[]>([]);
  const [novo, setNovo] = useState({ data: hoje(), tipo: 'FATURAMENTO', valor: '', descricao: '' });

  const carregar = useCallback(() => {
    apiCarteira.movimentos(props.posicao.clienteId).then(setMovimentos).catch((e) => props.onErro(e.message));
  }, [props]);

  useEffect(carregar, [carregar]);

  const adicionar = () => {
    const valor = Number(novo.valor.replace(',', '.'));
    if (Number.isNaN(valor) || valor <= 0) {
      props.onErro('Informe um valor válido');
      return;
    }
    apiCarteira
      .adicionar(props.posicao.clienteId, {
        data: novo.data,
        tipo: novo.tipo,
        valor,
        descricao: novo.descricao || undefined,
      })
      .then(() => {
        setNovo({ ...novo, valor: '', descricao: '' });
        carregar();
        props.onMudou();
      })
      .catch((e) => props.onErro(e.message));
  };

  return (
    <div className="bloco-movimentos">
      <table className="tabela-interna">
        <thead>
          <tr>
            <th>Data</th>
            <th>Tipo</th>
            <th>Valor</th>
            <th>Descrição</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {movimentos.map((m) => (
            <tr key={m.id}>
              <td>{new Date(m.data + 'T00:00:00').toLocaleDateString('pt-BR')}</td>
              <td>
                <span className={m.tipo === 'FATURAMENTO' ? 'selo' : 'selo selo-ok'}>{m.tipo}</span>
              </td>
              <td>{brl(m.valor)}</td>
              <td className="col-item">{m.descricao ?? '—'}</td>
              <td>
                <button
                  className="botao-excluir"
                  title="Excluir movimento"
                  onClick={() =>
                    apiCarteira
                      .excluirMovimento(m.id)
                      .then(() => {
                        carregar();
                        props.onMudou();
                      })
                      .catch((e) => props.onErro(e.message))
                  }
                >
                  ×
                </button>
              </td>
            </tr>
          ))}
          {movimentos.length === 0 && (
            <tr>
              <td colSpan={5} className="vazio">
                Sem movimentos
              </td>
            </tr>
          )}
        </tbody>
      </table>
      <div className="linha-form">
        <Campo label="Data">
          <input type="date" value={novo.data} onChange={(e) => setNovo({ ...novo, data: e.target.value })} />
        </Campo>
        <Campo label="Tipo de movimento">
          <select value={novo.tipo} onChange={(e) => setNovo({ ...novo, tipo: e.target.value })}>
            <option value="FATURAMENTO">Faturamento (aumenta saldo)</option>
            <option value="PAGAMENTO">Pagamento (reduz saldo)</option>
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
        <Campo label="Descrição">
          <input
            placeholder="Ex.: NF 123"
            className="campo-medio"
            value={novo.descricao}
            onChange={(e) => setNovo({ ...novo, descricao: e.target.value })}
          />
        </Campo>
        <button disabled={!novo.valor} onClick={adicionar}>
          Lançar
        </button>
      </div>
    </div>
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
