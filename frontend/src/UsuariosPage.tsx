import { useCallback, useEffect, useState } from 'react';
import { Usuario, apiAuth } from './api';
import { Campo } from './ui';

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [novo, setNovo] = useState({ nome: '', email: '', senha: '' });
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(() => {
    apiAuth.listarUsuarios().then(setUsuarios).catch((e) => setErro(e.message));
  }, []);

  useEffect(carregar, [carregar]);

  return (
    <section className="painel">
      <h2>Usuários do sistema</h2>
      {erro && <div className="erro">{erro}</div>}
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>E-mail</th>
            <th>Situação</th>
            <th>Criado em</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {usuarios.map((u) => (
            <tr key={u.id}>
              <td className="col-item">{u.nome}</td>
              <td>{u.email}</td>
              <td>
                <span className={u.ativo ? 'selo selo-ok' : 'selo'}>{u.ativo ? 'ATIVO' : 'INATIVO'}</span>
              </td>
              <td>{new Date(u.criadoEm).toLocaleDateString('pt-BR')}</td>
              <td>
                <button
                  className="botao-excluir"
                  title="Excluir usuário"
                  onClick={() => {
                    setErro(null);
                    apiAuth.excluirUsuario(u.id).then(carregar).catch((e) => setErro(e.message));
                  }}
                >
                  ×
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3 className="titulo-form">Cadastrar novo usuário</h3>
      <div className="linha-form">
        <Campo label="Nome completo">
          <input placeholder="Nome do usuário" value={novo.nome}
                 onChange={(e) => setNovo({ ...novo, nome: e.target.value })} />
        </Campo>
        <Campo label="E-mail (login)">
          <input type="email" placeholder="usuario@empresa.com" value={novo.email}
                 onChange={(e) => setNovo({ ...novo, email: e.target.value })} />
        </Campo>
        <Campo label="Senha (mín. 6 caracteres)">
          <input type="password" placeholder="••••••••" value={novo.senha}
                 onChange={(e) => setNovo({ ...novo, senha: e.target.value })} />
        </Campo>
        <button
          disabled={!novo.nome.trim() || !novo.email.trim() || novo.senha.length < 6}
          onClick={() => {
            setErro(null);
            apiAuth
              .criarUsuario({ nome: novo.nome.trim(), email: novo.email.trim(), senha: novo.senha })
              .then(() => {
                setNovo({ nome: '', email: '', senha: '' });
                carregar();
              })
              .catch((e) => setErro(e.message));
          }}
        >
          Cadastrar usuário
        </button>
      </div>
    </section>
  );
}
