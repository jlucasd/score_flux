import { useEffect, useState } from 'react';
import { Cliente, RelatoCampo, RelatoCampoDados, apiCredito } from './api';
import { Campo } from './ui';
import { useAnalise } from './contexto';

const DOCUMENTACAO = [
  'Carta de referência bancária (ao menos de um Banco que seja cliente)',
  'Contrato Social ou a última Alteração Contratual consolidada / Estatuto no caso de Cooperativas',
  'Demonstrações Contábeis (Balanço Patrimonial + DRE) dos últimos 02 exercícios (assinadas pelo Contador responsável)',
  'Balancete de Verificação do Exercício (até o último mês fechado — assinado pelo contador responsável)',
  'Na ausência dos itens 3 e 4, Faturamento dos últimos 24 meses, assinado pelo responsável financeiro da empresa',
  'No caso de Venda Direta ao Produtor, os itens 2, 3 e 4 devem ser substituídos pela Declaração de Imposto de Renda e Recibo de Entrega do produtor',
];

const extrairDados = (r: RelatoCampo): RelatoCampoDados => ({
  titulo: r.titulo,
  conceitoComercial: r.conceitoComercial,
  conceitoComercialJustificativa: r.conceitoComercialJustificativa,
  tempoMercado: r.tempoMercado,
  tempoMercadoJustificativa: r.tempoMercadoJustificativa,
  bandeira: r.bandeira,
  bandeiraJustificativa: r.bandeiraJustificativa,
  possuiErp: r.possuiErp,
  possuiCobranca: r.possuiCobranca,
  unidadesNegocio: r.unidadesNegocio,
  unidadesNegocioJustificativa: r.unidadesNegocioJustificativa,
  riscoClimatico: r.riscoClimatico,
  riscoClimaticoJustificativa: r.riscoClimaticoJustificativa,
  observacoes: r.observacoes,
});

const relatoVazio = (clienteId: number, clienteNome: string): RelatoCampo => ({
  id: null, titulo: null, clienteId, clienteNome, clienteCpfCnpj: null,
  conceitoComercial: null, conceitoComercialJustificativa: null,
  tempoMercado: null, tempoMercadoJustificativa: null,
  bandeira: null, bandeiraJustificativa: null,
  possuiErp: null, possuiCobranca: null,
  unidadesNegocio: null, unidadesNegocioJustificativa: null,
  riscoClimatico: null, riscoClimaticoJustificativa: null,
  observacoes: null, atualizadoEm: null,
});

export default function RelatoCampoPage() {
  const { clienteId, setClienteId } = useAnalise();
  const [clientes, setClientes] = useState<Cliente[]>([]);
  const [relatos, setRelatos] = useState<RelatoCampo[]>([]);
  const [relatoAtual, setRelatoAtual] = useState<RelatoCampo | null>(null);
  const [editando, setEditando] = useState(false);
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

  useEffect(() => {
    if (clienteId === null) {
      setRelatos([]);
      setRelatoAtual(null);
      setEditando(false);
      return;
    }
    setAviso(null);
    apiCredito.listarRelatos(clienteId).then((lista) => {
      setRelatos(lista);
      setRelatoAtual(null);
      setEditando(false);
    }).catch((e) => setErro(e.message));
  }, [clienteId]);

  const cliente = clientes.find((c) => c.id === clienteId);

  const novoRelato = () => {
    if (!cliente) return;
    setRelatoAtual(relatoVazio(clienteId!, cliente.nome));
    setEditando(true);
  };

  const abrirRelato = (r: RelatoCampo) => {
    setRelatoAtual({ ...r });
    setEditando(true);
  };

  const mudar = (campo: keyof RelatoCampoDados, valor: string | boolean | null) => {
    setRelatoAtual((atual) => (atual ? { ...atual, [campo]: valor } : atual));
  };

  const salvar = () => {
    if (!relatoAtual || clienteId === null) return;
    setErro(null);
    const dados = extrairDados(relatoAtual);

    const promessa = relatoAtual.id
      ? apiCredito.salvarRelatoCampo(relatoAtual.id, dados)
      : apiCredito.criarRelato(clienteId, dados);

    promessa
      .then(() => {
        setAviso('Relato de campo salvo com sucesso');
        setEditando(false);
        setRelatoAtual(null);
        apiCredito.listarRelatos(clienteId).then(setRelatos);
      })
      .catch((e) => setErro(e.message));
  };

  const excluir = (id: number) => {
    setErro(null);
    apiCredito.excluirRelato(id).then(() => {
      setAviso('Relato excluído');
      if (relatoAtual?.id === id) {
        setRelatoAtual(null);
        setEditando(false);
      }
      apiCredito.listarRelatos(clienteId!).then(setRelatos);
    }).catch((e) => setErro(e.message));
  };

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
          {clienteId !== null && (
            <button onClick={novoRelato}>+ Novo relato</button>
          )}
        </div>
        <p className="dica">
          Relato de campo estruturado — cada cliente pode ter múltiplos relatos.
        </p>
      </section>

      {!editando && relatos.length > 0 && (
        <section className="painel">
          <h2>Relatos de {cliente?.nome}</h2>
          <table>
            <thead>
              <tr>
                <th className="col-item">Título</th>
                <th>Conceito</th>
                <th>Atualizado em</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {relatos.map((r, idx) => (
                <tr key={r.id}>
                  <td className="col-item">
                    <a href="#" onClick={(e) => { e.preventDefault(); abrirRelato(r); }}>
                      {r.titulo || `Relato ${idx + 1}`}
                    </a>
                  </td>
                  <td>{r.conceitoComercial ?? '—'}</td>
                  <td>
                    {r.atualizadoEm
                      ? new Date(r.atualizadoEm + 'Z').toLocaleString('pt-BR', {
                          dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo',
                        })
                      : '—'}
                  </td>
                  <td>
                    <button className="botao-excluir" title="Excluir relato"
                      onClick={() => excluir(r.id!)}>×</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {!editando && relatos.length === 0 && clienteId !== null && (
        <section className="painel">
          <p className="dica">Nenhum relato cadastrado para este cliente. Clique em "+ Novo relato" para começar.</p>
        </section>
      )}

      {editando && relatoAtual && (
        <section className="painel">
          <div className="linha-form">
            <button className="botao-secundario" onClick={() => { setEditando(false); setRelatoAtual(null); }}>
              Voltar à lista
            </button>
            <h2 style={{ margin: 0 }}>{relatoAtual.id ? 'Editar relato' : 'Novo relato'}</h2>
          </div>

          <div className="linha-form" style={{ marginTop: '1rem' }}>
            <Campo label="Título do relato">
              <input
                placeholder="Ex: Visita jan/2025"
                value={relatoAtual.titulo ?? ''}
                onChange={(e) => mudar('titulo', e.target.value || null)}
              />
            </Campo>
          </div>

          <Pergunta
            titulo="1.1 Conceito Comercial"
            guia="Em sua avaliação, baseado em informações e percepções colhidas a campo, que conceito comercial goza o cliente em seu âmbito de atuação. Justifique sua avaliação."
            opcoes={['Excelente', 'Bom', 'Regular', 'Sem informações']}
            valor={relatoAtual.conceitoComercial}
            justificativa={relatoAtual.conceitoComercialJustificativa}
            onValor={(v) => mudar('conceitoComercial', v)}
            onJustificativa={(v) => mudar('conceitoComercialJustificativa', v)}
          />

          <Pergunta
            titulo="2.3 Tempo de Mercado"
            guia="A data de abertura da empresa, que poderá ser verificada no cartão do CNPJ ou contrato social, por vezes não representa a data inicial da empresa no mercado, podendo ser fruto de uma sucessão de negócios no mesmo ramo de atividade. Sendo esse o caso, justifique seu apontamento de período superior ao constante dos atos formais do cliente."
            opcoes={['Acima de 10 anos', 'De 6 a 10 anos', 'De 4 a 5 anos', 'De 0 a 3 anos']}
            valor={relatoAtual.tempoMercado}
            justificativa={relatoAtual.tempoMercadoJustificativa}
            onValor={(v) => mudar('tempoMercado', v)}
            onJustificativa={(v) => mudar('tempoMercadoJustificativa', v)}
          />

          <Pergunta
            titulo="4.1 Principal Fornecedor (Bandeira)"
            guia="O cliente é fornecedor exclusivo de algum produto/marca com destaque no mercado? Representa alguma bandeira? Se sim, classifique-a como de 1º ou 2º nível. Aponte a situação apurada."
            opcoes={['1º Nível', '2º Nível', 'Não Possui']}
            valor={relatoAtual.bandeira}
            justificativa={relatoAtual.bandeiraJustificativa}
            onValor={(v) => mudar('bandeira', v)}
            onJustificativa={(v) => mudar('bandeiraJustificativa', v)}
          />

          <div className="subcriterio">
            <div className="subcriterio-titulo">4.2 Cliente Possui ERP / Sistema de Cobrança</div>
            <p className="dica">O cliente possui sistema de controle integrado (ERP)? Possui sistema de cobrança?</p>
            <div className="par-radios">
              <div className="campo">
                <span>ERP</span>
                <RadiosSimNao nome="possui-erp" valor={relatoAtual.possuiErp} onChange={(v) => mudar('possuiErp', v)} />
              </div>
              <div className="campo">
                <span>Cobrança</span>
                <RadiosSimNao
                  nome="possui-cobranca"
                  valor={relatoAtual.possuiCobranca}
                  onChange={(v) => mudar('possuiCobranca', v)}
                />
              </div>
            </div>
          </div>

          <Pergunta
            titulo="4.3 Número de Unidades de Negócio"
            guia="O cliente possui filiais ou pertence a algum grupo econômico que podem ser consideradas unidades de negócios? Justifique sua resposta."
            opcoes={['Mais de 12', 'De 7 a 12', 'De 4 a 6', 'De 1 a 3']}
            valor={relatoAtual.unidadesNegocio}
            justificativa={relatoAtual.unidadesNegocioJustificativa}
            onValor={(v) => mudar('unidadesNegocio', v)}
            onJustificativa={(v) => mudar('unidadesNegocioJustificativa', v)}
          />

          <Pergunta
            titulo="4.4 Risco Produtivo e Climático da Região"
            guia="Baseado em informações de mercado, como você avalia o Risco Produtivo e Climático da Região de atuação do cliente. Justifique sua resposta."
            opcoes={['Muito Baixo', 'Baixo', 'Médio', 'Alto', 'Muito Alto']}
            valor={relatoAtual.riscoClimatico}
            justificativa={relatoAtual.riscoClimaticoJustificativa}
            onValor={(v) => mudar('riscoClimatico', v)}
            onJustificativa={(v) => mudar('riscoClimaticoJustificativa', v)}
          />

          <div className="subcriterio">
            <div className="subcriterio-titulo">Observações</div>
            <textarea
              rows={3}
              placeholder="Observações gerais do relato de campo"
              value={relatoAtual.observacoes ?? ''}
              onChange={(e) => mudar('observacoes', e.target.value || null)}
            />
          </div>

          <div className="linha-form">
            <button onClick={salvar}>Salvar relato</button>
            {relatoAtual.atualizadoEm && (
              <span className="dica">
                Última atualização:{' '}
                {new Date(relatoAtual.atualizadoEm + 'Z').toLocaleString('pt-BR', {
                  dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Sao_Paulo',
                })}
              </span>
            )}
          </div>
        </section>
      )}

      <section className="painel">
        <h2>Documentação a ser solicitada</h2>
        <ol className="lista-documentacao">
          {DOCUMENTACAO.map((doc, i) => (
            <li key={i}>{doc}</li>
          ))}
        </ol>
      </section>
    </>
  );
}

function Pergunta(props: {
  titulo: string;
  guia: string;
  opcoes: string[];
  valor: string | null;
  justificativa: string | null;
  onValor: (v: string) => void;
  onJustificativa: (v: string | null) => void;
}) {
  return (
    <div className="subcriterio">
      <div className="subcriterio-titulo">{props.titulo}</div>
      <p className="dica">{props.guia}</p>
      <div className="opcoes">
        {props.opcoes.map((op) => (
          <label key={op} className={props.valor === op ? 'opcao marcada' : 'opcao'}>
            <input
              type="radio"
              name={props.titulo}
              checked={props.valor === op}
              onChange={() => props.onValor(op)}
            />
            {op}
          </label>
        ))}
      </div>
      <textarea
        className="justificativa"
        rows={2}
        placeholder="Justificativa"
        value={props.justificativa ?? ''}
        onChange={(e) => props.onJustificativa(e.target.value || null)}
      />
    </div>
  );
}

function RadiosSimNao(props: {
  nome: string;
  valor: boolean | null;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="opcoes">
      {[
        ['Sim', true] as const,
        ['Não', false] as const,
      ].map(([rotulo, v]) => (
        <label key={rotulo} className={props.valor === v ? 'opcao marcada' : 'opcao'}>
          <input
            type="radio"
            name={props.nome}
            checked={props.valor === v}
            onChange={() => props.onChange(v)}
          />
          {rotulo}
        </label>
      ))}
    </div>
  );
}
