import { ReactNode, useEffect, useState } from 'react';
import { brl } from './api';

/** useState que limpa o valor automaticamente após `ms` milissegundos. */
export function useMsgTemp(ms = 5000): [string | null, React.Dispatch<React.SetStateAction<string | null>>] {
  const [msg, setMsg] = useState<string | null>(null);
  useEffect(() => {
    if (msg) {
      const t = setTimeout(() => setMsg(null), ms);
      return () => clearTimeout(t);
    }
  }, [msg, ms]);
  return [msg, setMsg];
}

/** Envolve um input/select com um rótulo pequeno acima — padrão dos formulários. */
export function Campo(props: { label: string; children: ReactNode }) {
  return (
    <label className="campo">
      <span>{props.label}</span>
      {props.children}
    </label>
  );
}

/** Modal de confirmação no padrão do sistema */
export function ConfirmationModal(props: {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
}) {
  if (!props.isOpen) return null;

  return (
    <div className="modal-overlay" onClick={props.onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ padding: '1.5rem', maxWidth: '420px' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem', marginBottom: '1.25rem' }}>
          <div style={{
            width: '40px', height: '40px', borderRadius: '50%',
            backgroundColor: 'var(--color-danger-bg)', color: 'var(--color-danger)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0
          }}>
            <span className="material-symbols-outlined" style={{ fontSize: '1.5rem' }}>warning</span>
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: '1.125rem', fontWeight: 700, color: 'var(--color-text-primary)' }}>{props.title}</h3>
            <p style={{ margin: '0.375rem 0 0 0', fontSize: '0.875rem', color: 'var(--color-text-secondary)', lineHeight: 1.4 }}>{props.message}</p>
          </div>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
          <button className="botao-secundario" onClick={props.onClose}>
            {props.cancelText ?? 'Cancelar'}
          </button>
          <button onClick={props.onConfirm} style={{ backgroundColor: 'var(--color-danger)', color: 'white', borderColor: 'transparent', boxShadow: 'var(--shadow-danger)' }}>
            {props.confirmText ?? 'Sim, excluir'}
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * Interpreta um número no padrão brasileiro: ponto = milhar, vírgula = decimal,
 * aceita negativo (prefixo "-" ou entre parênteses). Ex.: "-1.250,75" → -1250.75.
 */
export function parseNumeroBR(texto: string): number {
  const limpo = texto.trim().replace(/r\$/gi, '').replace(/\s/g, '');
  if (!limpo) return 0;
  const negativo = limpo.startsWith('-') || /^\(.*\)$/.test(limpo);
  const semSinal = limpo.replace(/[()\-]/g, '');
  // Vírgula é o separador decimal; pontos são de milhar.
  const normalizado = semSinal.replace(/\./g, '').replace(',', '.');
  const valor = Number(normalizado);
  if (Number.isNaN(valor)) return 0;
  return negativo ? -valor : valor;
}

const arredonda2 = (v: number) => Math.round(v * 100) / 100;
const paraEdicao = (v: number) => (v === 0 ? '' : String(v).replace('.', ','));

/**
 * Campo monetário: digita-se naturalmente (aceita negativos e vírgula decimal) e,
 * ao sair do campo (TAB) ou pressionar Enter, os centavos são completados e o valor
 * é formatado em R$. Mantém sempre o número correto internamente.
 */
export function InputMoeda(props: {
  valor: number;
  onChange: (v: number) => void;
  permiteNegativo?: boolean;
}) {
  const [focado, setFocado] = useState(false);
  const [texto, setTexto] = useState('');
  const permiteNegativo = props.permiteNegativo ?? true;

  const exibicao = focado ? texto : props.valor === 0 ? '' : brl(props.valor);

  const confirmar = () => {
    setFocado(false);
    props.onChange(arredonda2(parseNumeroBR(texto)));
  };

  return (
    <input
      inputMode={permiteNegativo ? 'text' : 'decimal'}
      placeholder="R$ 0,00"
      value={exibicao}
      onFocus={() => {
        setFocado(true);
        setTexto(paraEdicao(props.valor));
      }}
      onChange={(e) => {
        const t = e.target.value;
        setTexto(t);
        props.onChange(parseNumeroBR(t));
      }}
      onBlur={confirmar}
      onKeyDown={(e) => {
        if (e.key === 'Enter') (e.target as HTMLInputElement).blur();
      }}
    />
  );
}
