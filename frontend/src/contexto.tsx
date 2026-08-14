import { createContext, ReactNode, useContext, useEffect, useRef, useState } from 'react';

/**
 * Contexto compartilhado das abas de análise: cliente selecionado e ano de referência.
 * Mantém a seleção ao trocar de aba e faz o ano refletir em todas as abas relacionadas.
 */
interface AnaliseCtx {
  clienteId: number | null;
  setClienteId: (id: number | null) => void;
  ano: number;
  setAno: (a: number) => void;
}

const Ctx = createContext<AnaliseCtx | null>(null);

export function AnaliseProvider({ children }: { children: ReactNode }) {
  const [clienteId, setClienteId] = useState<number | null>(null);
  const [ano, setAno] = useState(new Date().getFullYear());
  return (
    <Ctx.Provider value={{ clienteId, setClienteId, ano, setAno }}>{children}</Ctx.Provider>
  );
}

export function useAnalise(): AnaliseCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useAnalise fora do AnaliseProvider');
  return ctx;
}

/**
 * Autosave com debounce: chama `salvar(valor)` ~800ms após a última mudança de `valor`.
 * Não dispara na montagem inicial (só quando o valor realmente muda pela interação).
 */
export function useAutosave<T>(valor: T, salvar: (v: T) => void, atrasoMs = 800) {
  const salvarRef = useRef(salvar);
  salvarRef.current = salvar;
  const montado = useRef(false);

  useEffect(() => {
    if (!montado.current) {
      montado.current = true;
      return;
    }
    const timer = setTimeout(() => salvarRef.current(valor), atrasoMs);
    return () => clearTimeout(timer);
  }, [valor, atrasoMs]);
}
