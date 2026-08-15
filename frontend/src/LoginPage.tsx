import { useState } from 'react';
import { apiAuth, setSessao } from './api';

export default function LoginPage(props: { onLogin: () => void }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [modoReset, setModoReset] = useState(false);

  const entrar = async () => {
    setErro(null);
    setCarregando(true);
    try {
      const sessao = await apiAuth.login(email.trim(), senha);
      setSessao(sessao.token, sessao.nome);
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

  if (modoReset) {
    return (
      <div className="tela-login">
        <div className="cartao-login">
          <h1>ScoreFlux</h1>
          <p className="login-saudacao">Recuperar senha</p>
          <p className="subtitulo">
            Informe seu e-mail de acesso. Uma nova senha será enviada para você.
          </p>
          {erro && <div className="erro">{erro}</div>}
          {aviso && <div className="aviso">{aviso}</div>}
          <label>
            E-mail
            <input
              type="email"
              value={email}
              autoFocus
              onChange={(e) => setEmail(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && resetar()}
              placeholder="voce@empresa.com"
            />
          </label>
          <button disabled={!email.trim() || carregando} onClick={resetar}>
            {carregando ? 'Enviando…' : 'Enviar nova senha'}
          </button>
          <button className="botao-link-login" onClick={() => { setModoReset(false); setErro(null); setAviso(null); }}>
            Voltar ao login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="tela-login">
      <div className="cartao-login">
        <h1>ScoreFlux</h1>
        <p className="login-saudacao">Bem-vindo(a)!</p>
        <p className="subtitulo">
          Acesse sua conta para acompanhar a análise de crédito e o fluxo de caixa.
        </p>
        {erro && <div className="erro">{erro}</div>}
        {aviso && <div className="aviso">{aviso}</div>}
        <label>
          E-mail
          <input
            type="email"
            value={email}
            autoFocus
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && entrar()}
            placeholder="voce@empresa.com"
          />
        </label>
        <label>
          Senha
          <div className="campo-senha">
            <input
              type={mostrarSenha ? 'text' : 'password'}
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && entrar()}
              placeholder="••••••••"
            />
            <button
              type="button"
              className="botao-olho"
              onClick={() => setMostrarSenha(!mostrarSenha)}
              title={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
              tabIndex={-1}
            >
              {mostrarSenha ? (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                  <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                  <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
                </svg>
              ) : (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              )}
            </button>
          </div>
        </label>
        <button disabled={!email.trim() || !senha || carregando} onClick={entrar}>
          {carregando ? 'Entrando…' : 'Entrar'}
        </button>
        <button className="botao-link-login" onClick={() => { setModoReset(true); setErro(null); setAviso(null); }}>
          Esqueci minha senha
        </button>
        <p className="login-tagline">Crédito seguro, decisões inteligentes.</p>
      </div>
    </div>
  );
}
