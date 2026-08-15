import { useEffect, useState } from 'react';
import { getNomeUsuario, getToken, setSessao } from './api';
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
    </div>
    </AnaliseProvider>
  );
}

