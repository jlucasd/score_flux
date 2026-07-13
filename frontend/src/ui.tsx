import { ReactNode } from 'react';
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

/** Máscara de moeda: digite apenas dígitos, os 2 últimos são centavos (padrão bancário). */
export function InputMoeda(props: { valor: number; onChange: (v: number) => void }) {
  return (
    <input
      inputMode="numeric"
      placeholder="R$ 0,00"
      value={props.valor === 0 ? '' : brl(props.valor)}
      onChange={(e) => {
        const digitos = e.target.value.replace(/\D/g, '');
        props.onChange(digitos ? Number(digitos) / 100 : 0);
      }}
    />
  );
}
