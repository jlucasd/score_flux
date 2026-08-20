import { useState } from 'react';
import { apiAuth, setSessao } from './api';
import { useMsgTemp } from './ui';

export default function LoginPage(props: { onLogin: () => void }) {
  const [email, setEmail] = useState(() => localStorage.getItem('sf_email') ?? '');
  const [senha, setSenha] = useState('');
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [lembrar, setLembrar] = useState(() => !!localStorage.getItem('sf_email'));
  const [erro, setErro] = useMsgTemp();
  const [aviso, setAviso] = useMsgTemp();
  const [carregando, setCarregando] = useState(false);
  const [modoReset, setModoReset] = useState(false);

  const entrar = async () => {
    setErro(null);
    setCarregando(true);
    try {
      const sessao = await apiAuth.login(email.trim(), senha, lembrar);
      if (lembrar) {
        localStorage.setItem('sf_email', email.trim());
      } else {
        localStorage.removeItem('sf_email');
      }
      setSessao(sessao.token, sessao.nome, sessao.perfil);
      props.onLogin();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  };

  const resetar = async () => {
    setErro(null);
    setAviso(null);
    setCarregando(true);
    try {
      const res = await apiAuth.resetSenha(email.trim());
      setAviso(res.mensagem);
      setModoReset(false);
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setCarregando(false);
    }
  };

  return (
    <div className="login-container">
      {/* Left Panel with Image */}
      <div className="login-image-panel">
        <img src="/assets/agro-login.jpg" alt="Agronegócio" className="login-image" />
        <div className="login-overlay">
          <h1>ScoreFlux</h1>
          <p>
            Plataforma avançada de análise de crédito e fluxo de caixa desenvolvida
            especificamente para o mercado do agronegócio.
          </p>
        </div>
      </div>

      {/* Right Panel with Form */}
      <div className="login-form-panel">
        <div className="login-form-wrapper">
          <div className="login-logo">
            <img src="/assets/logo.png" alt="ScoreFlux" style={{ height: 60 }} />
          </div>

          <h2>{modoReset ? 'Recuperar senha' : 'Bem-vindo(a)!'}</h2>
          <p>
            {modoReset
              ? 'Informe seu e-mail para receber uma nova senha de acesso.'
              : 'Acesse sua conta para acompanhar as análises.'}
          </p>

          {erro && <div className="erro">{erro}</div>}
          {aviso && <div className="aviso">{aviso}</div>}

          <div className="campo" style={{ marginBottom: '1.25rem' }}>
            <label>E-mail</label>
            <input
              type="email"
              value={email}
              autoFocus
              onChange={(e) => setEmail(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && (modoReset ? resetar() : entrar())}
              placeholder="voce@empresa.com"
            />
          </div>

          {!modoReset && (
            <>
              <div className="campo" style={{ marginBottom: '1.25rem' }}>
                <label>Senha</label>
                <div style={{ position: 'relative' }}>
                  <input
                    type={mostrarSenha ? 'text' : 'password'}
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && entrar()}
                    placeholder="••••••••"
                    style={{ paddingRight: '2.5rem' }}
                  />
                  <button
                    type="button"
                    onClick={() => setMostrarSenha(!mostrarSenha)}
                    tabIndex={-1}
                    style={{
                      position: 'absolute', right: '-7.5rem', top: '40%', transform: 'translateY(-50%)',
                      background: 'none', border: 'none', color: 'var(--color-text-muted)',
                      padding: '0.25rem', boxShadow: 'none'
                    }}
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: '2rem' }}>
                      {mostrarSenha ? 'visibility_off' : 'visibility'}
                    </span>
                  </button>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem', fontWeight: 600, color: 'var(--color-text-secondary)', cursor: 'pointer', whiteSpace: 'nowrap' }}>
                  <input
                    type="checkbox"
                    checked={lembrar}
                    onChange={(e) => setLembrar(e.target.checked)}
                    style={{ width: 'auto', height: 'auto', accentColor: 'var(--color-primary)' }}
                  />
                  Lembrar de mim
                </label>

                <button
                  className="botao-link"
                  onClick={() => { setModoReset(true); setErro(null); setAviso(null); }}
                  style={{ fontSize: '0.875rem' }}
                >
                  Esqueci minha senha
                </button>
              </div>
            </>
          )}

          <button
            style={{ width: '100%', padding: '0.875rem' }}
            disabled={modoReset ? (!email.trim() || carregando) : (!email.trim() || !senha || carregando)}
            onClick={modoReset ? resetar : entrar}
          >
            {carregando ? (modoReset ? 'Enviando…' : 'Entrando…') : (modoReset ? 'Enviar nova senha' : 'Entrar na Plataforma')}
          </button>

          {modoReset && (
            <button
              className="botao-secundario"
              style={{ width: '100%', marginTop: '1rem', padding: '0.875rem' }}
              onClick={() => { setModoReset(false); setErro(null); setAviso(null); }}
            >
              Voltar ao login
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
