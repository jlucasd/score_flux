import { ReactNode, useState } from 'react';
import { brl } from './api';

/** Envolve um input/select com um rótulo pequeno acima — padrão dos formulários. */
export function Campo(props: { label: string; children: ReactNode; largo?: boolean }) {
  return (
    <label className={props.largo ? 'campo campo-largo' : 'campo'}>
      <span>{props.label}</span>
      {props.children}
    </label>
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
