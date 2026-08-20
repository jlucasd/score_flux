import { Fragment, useEffect, useState } from 'react';
import { Politica, apiCredito } from './api';
import { useMsgTemp } from './ui';

const fmtPeso = (v: number) =>
  (v * 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 }) + '%';

function CelulaPeso(props: { subcriterioId: number; peso: number; onAtualizar: (pol: Politica) => void }) {
  const [editando, setEditando] = useState(false);
  const [valor, setValor] = useState('');

  const iniciar = () => {
    setValor((props.peso * 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 }));
    setEditando(true);
  };

  const confirmar = () => {
    setEditando(false);
    const num = parseFloat(valor.replace(',', '.'));
    if (isNaN(num)) return;
    const novoPeso = num / 100;
    if (novoPeso === props.peso) return;
    apiCredito.atualizarPeso(props.subcriterioId, novoPeso).then(props.onAtualizar);
  };

  if (editando) {
    return (
      <td>
        <input
          className="campo-peso"
          value={valor}
          autoFocus
          onChange={(e) => setValor(e.target.value)}
          onBlur={confirmar}
          onKeyDown={(e) => { if (e.key === 'Enter') confirmar(); if (e.key === 'Escape') setEditando(false); }}
          style={{ width: '5rem', textAlign: 'right' }}
        />
      </td>
    );
  }
  return (
    <td style={{ cursor: 'pointer', textAlign: 'right', fontWeight: 700 }} onClick={iniciar} title="Clique para editar">
      {fmtPeso(props.peso)}
    </td>
  );
}

export default function ParametrosPage() {
  const [politica, setPolitica] = useState<Politica | null>(null);
  const [erro, setErro] = useMsgTemp();

  useEffect(() => {
    apiCredito.politica().then(setPolitica).catch((e) => setErro(e.message));
  }, []);

  const grupos = politica ? [...new Set(politica.subcriterios.map((s) => s.grupo))] : [];
  const total = politica ? politica.subcriterios.reduce((t, s) => t + s.peso, 0) : 0;

  return (
    <>
      {erro && <div className="erro">{erro}</div>}

      <section className="painel">
        <h2>Bases da Política de Análise de Crédito</h2>
        {politica && (
          <p className="subtitulo">
            {politica.nome} — versão {politica.versao}
          </p>
        )}
        <p className="dica">
          Dados de referência da política vigente. Clique sobre o peso para editá-lo.
        </p>
      </section>

      {politica && (
        <section className="painel">
          <div className="rolagem">
            <table>
              <thead>
                <tr>
                  <th>Índice</th>
                  <th className="col-item">Critério</th>
                  <th style={{ textAlign: 'right' }}>Peso</th>
                  <th>Instrumento de Análise</th>
                  <th>Fonte</th>
                  <th>Validação</th>
                </tr>
              </thead>
              <tbody>
                {grupos.map((grupo) => {
                  const subs = politica.subcriterios.filter((s) => s.grupo === grupo);
                  const numero = subs[0].codigo.split('.')[0];
                  const somaGrupo = subs.reduce((t, s) => t + s.peso, 0);
                  return (
                    <Fragment key={grupo}>
                      <tr className="linha-grupo">
                        <td>{numero}</td>
                        <td>{grupo.toUpperCase()}</td>
                        <td style={{ textAlign: 'right', fontWeight: 700 }}>{fmtPeso(somaGrupo)}</td>
                        <td colSpan={3}></td>
                      </tr>
                      {subs.map((s) => (
                        <tr key={s.id}>
                          <td>{s.codigo}</td>
                          <td className="col-item" style={{ fontWeight: 700 }}>{s.nome}</td>
                          <CelulaPeso subcriterioId={s.id} peso={s.peso} onAtualizar={setPolitica} />
                          <td className="col-texto">{s.instrumento ?? '—'}</td>
                          <td className="col-texto">{s.fonte ?? '—'}</td>
                          <td className="col-texto">{s.validacao ?? '—'}</td>
                        </tr>
                      ))}
                    </Fragment>
                  );
                })}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={2} className="col-item">TOTAL</td>
                  <td className="total" style={{ textAlign: 'right' }}>{fmtPeso(total)}</td>
                  <td colSpan={3}></td>
                </tr>
              </tfoot>
            </table>
          </div>
        </section>
      )}
    </>
  );
}
