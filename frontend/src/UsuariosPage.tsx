import { useCallback, useEffect, useState } from 'react';
import { Perfil, Usuario, apiAuth } from './api';
import { Campo, ConfirmationModal, useMsgTemp } from './ui';

const PERFIS: Perfil[] = ['ADMINISTRADOR', 'COMERCIAL', 'FINANCEIRO'];

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [novo, setNovo] = useState({ nome: '', email: '', senha: '', perfil: 'COMERCIAL' as Perfil });
  const [erro, setErro] = useMsgTemp();
  const [sucesso, setSucesso] = useMsgTemp();
  const [avisoExclusao, setAvisoExclusao] = useMsgTemp();
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<Usuario | null>(null);

  const carregar = useCallback(() => {
    apiAuth.listarUsuarios().then(setUsuarios).catch((e) => setErro(e.message));
  }, []);

  useEffect(carregar, [carregar]);

  return (
    <section className="painel">
      <h2>Usuários do sistema</h2>
      {erro && <div className="erro">{erro}<button className="fechar-msg" onClick={() => setErro(null)}>×</button></div>}
      {sucesso && <div className="aviso">{sucesso}<button className="fechar-msg" onClick={() => setSucesso(null)}>×</button></div>}
      {avisoExclusao && <div className="aviso-exclusao">{avisoExclusao}<button className="fechar-msg" onClick={() => setAvisoExclusao(null)}>×</button></div>}
      <table>
        <thead>
          <tr>
            <th>Nome</th>
            <th>E-mail</th>
            <th>Perfil</th>
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
              <td>{u.perfil}</td>
              <td>
                <span className={u.ativo ? 'selo selo-ok' : 'selo'}>{u.ativo ? 'ATIVO' : 'INATIVO'}</span>
              </td>
              <td>{new Date(u.criadoEm).toLocaleDateString('pt-BR')}</td>
              <td>
                <button
                  className="botao-excluir"
                  title="Excluir usuário"
                  onClick={() => setUsuarioParaExcluir(u)}
                >
                  ×
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <ConfirmationModal
        isOpen={!!usuarioParaExcluir}
        onClose={() => setUsuarioParaExcluir(null)}
        onConfirm={() => {
          if (usuarioParaExcluir) {
            const id = usuarioParaExcluir.id;
            setUsuarioParaExcluir(null);
            setErro(null);
            apiAuth.excluirUsuario(id).then(() => { carregar(); setAvisoExclusao('Usuário excluído com sucesso'); }).catch((e) => setErro(e.message));
          }
        }}
        title="Excluir usuário"
        message={`Tem certeza que deseja excluir o usuário "${usuarioParaExcluir?.nome}"? Esta ação não pode ser desfeita.`}
      />

      <h3 className="titulo-form">Cadastrar novo usuário</h3>
      <div className="linha-form">
        <Campo label="Nome completo">
          <input className="campo-largo" placeholder="Nome do usuário" value={novo.nome}
                 onChange={(e) => setNovo({ ...novo, nome: e.target.value })} />
        </Campo>
        <Campo label="E-mail (login)">
          <input className="campo-largo" type="email" placeholder="usuario@empresa.com" value={novo.email}
                 onChange={(e) => setNovo({ ...novo, email: e.target.value })} />
        </Campo>
        <Campo label="Senha (mín. 6 caracteres)">
          <input className="campo-medio" type="password" placeholder="••••••••" value={novo.senha}
                 onChange={(e) => setNovo({ ...novo, senha: e.target.value })} />
        </Campo>
        <Campo label="Perfil">
          <select className="campo-medio" value={novo.perfil} onChange={(e) => setNovo({ ...novo, perfil: e.target.value as Perfil })}>
            {PERFIS.map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
        </Campo>
        <button
          disabled={!novo.nome.trim() || !novo.email.trim() || novo.senha.length < 6}
          onClick={() => {
            setErro(null);
            apiAuth
              .criarUsuario({ nome: novo.nome.trim(), email: novo.email.trim(), senha: novo.senha, perfil: novo.perfil })
              .then(() => {
                setNovo({ nome: '', email: '', senha: '', perfil: 'COMERCIAL' });
                carregar();
                setSucesso('Usuário cadastrado com sucesso');
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
