import { ReactNode } from 'react';

/** Envolve um input/select com um rótulo pequeno acima — padrão dos formulários. */
export function Campo(props: { label: string; children: ReactNode; largo?: boolean }) {
  return (
    <label className={props.largo ? 'campo campo-largo' : 'campo'}>
      <span>{props.label}</span>
      {props.children}
    </label>
  );
}
