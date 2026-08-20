import { useEffect, useState } from 'react';
import { apiAuth, getNomeUsuario, getPerfil, getToken, setSessao } from './api';
import { useMsgTemp } from './ui';
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
  const [menuAberto, setMenuAberto] = useState(false);
  const [menuUsuarioAberto, setMenuUsuarioAberto] = useState(false);
  const [boasVindas, setBoasVindas] = useState(false);

  useEffect(() => {
    const expirar = () => setLogado(false);
    window.addEventListener('sf-sessao-expirada', expirar);
    return () => window.removeEventListener('sf-sessao-expirada', expirar);
  }, []);

  if (!logado) return <LoginPage onLogin={() => { setLogado(true); setBoasVindas(true); setTimeout(() => setBoasVindas(false), 4000); }} />;

  const perfil = getPerfil();
  
  // Estrutura das abas com ícones
  const abasOperacionais: { chave: Aba; rotulo: string; icone: string }[] = [
    { chave: 'credito', rotulo: 'Cadastro do Cliente', icone: 'person_add' },
    { chave: 'relato', rotulo: 'Relato de Campo', icone: 'summarize' },
    { chave: 'politica', rotulo: 'Política de Crédito', icone: 'policy' },
    { chave: 'indicadores', rotulo: 'Análise de Indicadores', icone: 'monitoring' },
    { chave: 'ncg', rotulo: 'Balanço', icone: 'account_balance' },
    { chave: 'carteira', rotulo: 'Carteira', icone: 'folder_shared' },
  ];

  const abasSistema: { chave: Aba; rotulo: string; icone: string }[] = [
    ...(perfil === 'ADMINISTRADOR' ? [{ chave: 'parametros' as Aba, rotulo: 'Parâmetros', icone: 'settings_applications' }] : []),
    ...(perfil === 'ADMINISTRADOR' ? [{ chave: 'usuarios' as Aba, rotulo: 'Usuários', icone: 'manage_accounts' }] : []),
  ];

  const tituloAtual = [...abasOperacionais, ...abasSistema].find(a => a.chave === aba)?.rotulo || 'ScoreFlux';

  return (
    <AnaliseProvider>
      <div id="app-root">
        
        {/* Sidebar Mobile Overlay */}
        {menuAberto && (
          <div 
            className="modal-overlay" 
            style={{ zIndex: 35 }} 
            onClick={() => setMenuAberto(false)} 
          />
        )}

        <aside className={`app-sidebar ${menuAberto ? 'aberto' : ''}`}>
          <div className="app-sidebar-header">
            <img src="/assets/logo-sidebar.png" alt="ScoreFlux" style={{ height: 36 }} />
            <button className="menu-toggle" onClick={() => setMenuAberto(false)} style={{ marginLeft: 'auto' }}>
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>

          <nav className="app-nav">
            <div className="app-nav-group">Operacional</div>
            {abasOperacionais.map((item) => (
              <button
                key={item.chave}
                className={`nav-link ${aba === item.chave ? 'ativo' : ''}`}
                onClick={() => { setAba(item.chave); setMenuAberto(false); }}
                style={{ width: '100%', background: aba === item.chave ? 'var(--color-primary-bg)' : 'transparent', textAlign: 'left', boxShadow: 'none' }}
              >
                <span className={`material-symbols-outlined ${aba === item.chave ? 'filled' : ''}`}>
                  {item.icone}
                </span>
                {item.rotulo}
              </button>
            ))}

            {abasSistema.length > 0 && (
              <>
                <div className="app-nav-group" style={{ marginTop: '0.5rem' }}>Sistema</div>
                {abasSistema.map((item) => (
                  <button
                    key={item.chave}
                    className={`nav-link ${aba === item.chave ? 'ativo' : ''}`}
                    onClick={() => { setAba(item.chave); setMenuAberto(false); }}
                    style={{ width: '100%', border: 'none', background: aba === item.chave ? 'var(--color-primary-bg)' : 'transparent', textAlign: 'left', boxShadow: 'none' }}
                  >
                    <span className={`material-symbols-outlined ${aba === item.chave ? 'filled' : ''}`}>
                      {item.icone}
                    </span>
                    {item.rotulo}
                  </button>
                ))}
              </>
            )}
          </nav>

          <div className="app-sidebar-footer" style={{ position: 'relative' }}>
            <button 
              className="user-menu" 
              onClick={() => setMenuUsuarioAberto(!menuUsuarioAberto)}
            >
              <div className="user-avatar">
                {getNomeUsuario()?.charAt(0).toUpperCase() || 'U'}
              </div>
              <div className="user-info">
                <div className="user-name">{getNomeUsuario()}</div>
                <div className="user-role">{perfil}</div>
              </div>
              <span className="material-symbols-outlined" style={{ color: 'var(--color-text-muted)' }}>
                {menuUsuarioAberto ? 'expand_less' : 'expand_more'}
              </span>
            </button>

            {menuUsuarioAberto && (
              <div style={{
                position: 'absolute', bottom: '100%', left: '1rem', right: '1rem', marginBottom: '0.5rem',
                backgroundColor: 'var(--color-surface)', borderRadius: 'var(--radius-md)', 
                boxShadow: 'var(--shadow-lg)', border: '1px solid var(--color-border)', overflow: 'hidden',
                animation: 'fade-in-up 0.2s ease-out'
              }}>
                <button 
                  onClick={() => { setModalSenha(true); setMenuUsuarioAberto(false); }}
                  style={{ width: '100%', padding: '0.75rem 1rem', background: 'none', border: 'none', color: 'var(--color-text-primary)', textAlign: 'left', borderRadius: 0, boxShadow: 'none', display: 'flex', gap: '0.5rem', alignItems: 'center' }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: '1.25rem' }}>lock</span>
                  Alterar senha
                </button>
                <button 
                  onClick={() => { setSessao(null); setLogado(false); }}
                  style={{ width: '100%', padding: '0.75rem 1rem', background: 'none', border: 'none', borderTop: '1px solid var(--color-border)', color: 'var(--color-danger)', textAlign: 'left', borderRadius: 0, boxShadow: 'none', display: 'flex', gap: '0.5rem', alignItems: 'center' }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: '1.25rem' }}>logout</span>
                  Sair
                </button>
              </div>
            )}
          </div>
        </aside>

        <main className="app-main">
          <header className="app-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <button className="menu-toggle" onClick={() => setMenuAberto(true)}>
                <span className="material-symbols-outlined">menu</span>
              </button>
              <h1 className="page-title">{tituloAtual}</h1>
            </div>
            {/* Opcional: área para ações globais do header */}
          </header>

          <div className="app-content">
            {boasVindas && (
              <div className="aviso" style={{ cursor: 'pointer' }} onClick={() => setBoasVindas(false)}>
                Bem-vindo(a), {getNomeUsuario()}! Bom ter você de volta.
              </div>
            )}
            {aba === 'parametros' && <ParametrosPage />}
            {aba === 'politica' && <PoliticaCreditoPage />}
            {aba === 'relato' && <RelatoCampoPage />}
            {aba === 'indicadores' && <AnaliseIndicadoresPage />}
            {aba === 'ncg' && <NcgPage />}
            {aba === 'credito' && <CreditoPage />}
            {aba === 'carteira' && <CarteiraPage />}
            {aba === 'usuarios' && <UsuariosPage />}
          </div>
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
  const [erro, setErro] = useMsgTemp();
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
    <div className="modal-overlay" onClick={props.onFechar}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ padding: '2rem' }}>
        <h2 style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span className="material-symbols-outlined" style={{ color: 'var(--color-primary)' }}>lock</span>
          Alterar senha
        </h2>
        
        {sucesso ? (
          <>
            <div className="aviso">Senha alterada com sucesso!</div>
            <button onClick={props.onFechar} style={{ width: '100%', marginTop: '1rem' }}>
              Fechar
            </button>
          </>
        ) : (
          <>
            {erro && <div className="erro">{erro}</div>}
            
            <div className="campo" style={{ marginBottom: '1rem' }}>
              <label>Senha atual</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={mostrar ? 'text' : 'password'}
                  value={senhaAtual}
                  onChange={(e) => setSenhaAtual(e.target.value)}
                  placeholder="Senha atual"
                  style={{ paddingRight: '2.5rem' }}
                />
                <button
                  type="button"
                  onClick={() => setMostrar(!mostrar)}
                  tabIndex={-1}
                  style={{
                    position: 'absolute', right: '-7.5rem', top: '40%', transform: 'translateY(-50%)',
                    background: 'none', border: 'none', color: 'var(--color-text-muted)',
                    padding: '0.25rem', boxShadow: 'none'
                  }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: '1.25rem' }}>
                    {mostrar ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>
            </div>

            <div className="campo" style={{ marginBottom: '1rem' }}>
              <label>Nova senha (mín. 6 caracteres)</label>
              <input
                type={mostrar ? 'text' : 'password'}
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                placeholder="Nova senha"
              />
            </div>

            <div className="campo" style={{ marginBottom: '1.5rem' }}>
              <label>Confirmar nova senha</label>
              <input
                type={mostrar ? 'text' : 'password'}
                value={confirmar}
                onChange={(e) => setConfirmar(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && salvar()}
                placeholder="Repita a nova senha"
              />
            </div>

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
              <button className="botao-secundario" onClick={props.onFechar}>Cancelar</button>
              <button onClick={salvar} disabled={!senhaAtual || novaSenha.length < 6 || !confirmar}>
                Salvar Nova Senha
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
