import { useEffect, useState } from 'react';
import { apiAuth, getNomeUsuario, getToken, setSessao } from './api';
import AnaliseIndicadoresPage from './AnaliseIndicadoresPage';
import CreditoPage from './CreditoPage';
import CarteiraPage from './CarteiraPage';
import LoginPage from './LoginPage';
import NcgPage from './NcgPage';
import ParametrosPage from './ParametrosPage';
import PoliticaCreditoPage from './PoliticaCreditoPage';
import RelatoCampoPage from './RelatoCampoPage';
import UsuariosPage from './UsuariosPage';
import { AnaliseProvider } from './contexto';

type Aba =
  | 'parametros' | 'politica' | 'relato' | 'indicadores' | 'ncg'
  | 'credito' | 'carteira' | 'usuarios';

export default function App() {
  const [logado, setLogado] = useState(!!getToken());
  const [aba, setAba] = useState<Aba>('credito');
  const [modalSenha, setModalSenha] = useState(false);

  useEffect(() => {
    const expirar = () => setLogado(false);
    window.addEventListener('sf-sessao-expirada', expirar);
    return () => window.removeEventListener('sf-sessao-expirada', expirar);
  }, []);

  if (!logado) return <LoginPage onLogin={() => setLogado(true)} />;

  const abas: [Aba, string][] = [
    ['credito', 'Cadastro do Cliente'],
    ['relato', 'Relato de Campo'],
    ['politica', 'Política de Crédito'],
    ['indicadores', 'Análise de Indicadores'],
    ['ncg', 'Balanço'],
    ['parametros', 'Parâmetros'],
    ['carteira', 'Carteira'],
    ['usuarios', 'Usuários'],
  ];

  return (
    <AnaliseProvider>
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
          <button className="botao-link-sidebar" onClick={() => setModalSenha(true)}>Alterar senha</button>
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
        {aba === 'carteira' && <CarteiraPage />}
        {aba === 'usuarios' && <UsuariosPage />}
      </main>
      {modalSenha && <ModalAlterarSenha onFechar={() => setModalSenha(false)} />}
    </div>
    </AnaliseProvider>
  );
}

function ModalAlterarSenha(props: { onFechar: () => void }) {
  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState(false);
  const [mostrar, setMostrar] = useState(false);

  const salvar = async () => {
    setErro(null);
    if (novaSenha !== confirmar) {
      setErro('As senhas não coincidem');
      return;
    }
    try {
      await apiAuth.alterarSenha(senhaAtual, novaSenha);
      setSucesso(true);
    } catch (e) {
      setErro((e as Error).message);
    }
  };

  return (
    <div className="modal-fundo" onClick={props.onFechar}>
      <div className="modal-corpo" onClick={(e) => e.stopPropagation()}>
        <h2>Alterar senha</h2>
        {sucesso ? (
          <>
            <div className="aviso">Senha alterada com sucesso!</div>
            <button onClick={props.onFechar}>Fechar</button>
          </>
        ) : (
          <>
            {erro && <div className="erro">{erro}</div>}
            <label>
              Senha atual
              <div className="campo-senha">
                <input
                  type={mostrar ? 'text' : 'password'}
                  value={senhaAtual}
                  onChange={(e) => setSenhaAtual(e.target.value)}
                  placeholder="Senha atual"
                />
                <button type="button" className="botao-olho" onClick={() => setMostrar(!mostrar)} tabIndex={-1}>
                  <OlhoSvg aberto={!mostrar} />
                </button>
              </div>
            </label>
            <label>
              Nova senha (mín. 6 caracteres)
              <div className="campo-senha">
                <input
                  type={mostrar ? 'text' : 'password'}
                  value={novaSenha}
                  onChange={(e) => setNovaSenha(e.target.value)}
                  placeholder="Nova senha"
                />
              </div>
            </label>
            <label>
              Confirmar nova senha
              <div className="campo-senha">
                <input
                  type={mostrar ? 'text' : 'password'}
                  value={confirmar}
                  onChange={(e) => setConfirmar(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && salvar()}
                  placeholder="Repita a nova senha"
                />
              </div>
            </label>
            <div className="modal-acoes">
              <button onClick={salvar} disabled={!senhaAtual || novaSenha.length < 6 || !confirmar}>
                Salvar
              </button>
              <button className="botao-secundario" onClick={props.onFechar}>Cancelar</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function OlhoSvg(props: { aberto: boolean }) {
  if (props.aberto) {
    return (
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
        <circle cx="12" cy="12" r="3"/>
      </svg>
    );
  }
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
      <line x1="1" y1="1" x2="23" y2="23"/>
      <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
    </svg>
  );
}
