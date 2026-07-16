import { Fragment, useEffect, useState } from 'react';
import { Politica, apiCredito } from './api';

/** Peso em porcentagem: 0,10 → 10% · 0,025 → 2,5% · 1,00 → 100%. */
const fmtPeso = (v: number) =>
  (v * 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 }) + '%';

export default function ParametrosPage() {
  const [politica, setPolitica] = useState<Politica | null>(null);
  const [erro, setErro] = useState<string | null>(null);

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
          Dados de referência da política vigente — para simular pontuação use o menu Política Crédito.
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
                  <th>Peso</th>
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
                        <td>{fmtPeso(somaGrupo)}</td>
                        <td colSpan={3}></td>
                      </tr>
                      {subs.map((s) => (
                        <tr key={s.id}>
                          <td>{s.codigo}</td>
                          <td className="col-item">{s.nome}</td>
                          <td>{fmtPeso(s.peso)}</td>
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
                  <td className="total">{fmtPeso(total)}</td>
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
