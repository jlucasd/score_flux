-- Ajuste da aba "Parâmetros": a Fonte "RTVs" passa a ser "Comercial".
UPDATE subcriterio SET fonte = 'Comercial' WHERE fonte = 'RTVs';
