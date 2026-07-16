import { useState } from 'react';
import { apiAuth, setSessao } from './api';

export default function LoginPage(props: { onLogin: () => void }) {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

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

  return (
    <div className="tela-login">
      <div className="cartao-login">
        <h1>ScoreFlux</h1>
        <p className="login-saudacao">Bem-vindo(a)!</p>
        <p className="subtitulo">
          Acesse sua conta para acompanhar a análise de crédito e o fluxo de caixa.
        </p>
        {erro && <div className="erro">{erro}</div>}
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
          <input
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && entrar()}
            placeholder="••••••••"
          />
        </label>
        <button disabled={!email.trim() || !senha || carregando} onClick={entrar}>
          {carregando ? 'Entrando…' : 'Entrar'}
        </button>
        <p className="login-tagline">Crédito seguro, decisões inteligentes.</p>
      </div>
    </div>
  );
}
