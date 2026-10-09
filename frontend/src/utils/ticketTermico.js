/**
 * Impressao de tickets em impressora termica (80mm) pelo navegador.
 * Funciona com a impressora instalada no computador do caixa. Para imprimir sem a janela de
 * confirmacao, abra o Chrome com --kiosk-printing (veja o README).
 */

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
const brl = (v) => `R$ ${Number(v || 0).toFixed(2).replace('.', ',')}`
const hora = (iso) => new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })

const ESTILO = `
  @page { size: 80mm auto; margin: 0; }
  * { box-sizing: border-box; }
  body { width: 72mm; margin: 0 4mm; font-family: "Courier New", monospace; font-size: 13px; color: #000; }
  .ticket { padding: 4mm 0 8mm; page-break-after: always; }
  .ticket:last-child { page-break-after: auto; }
  h1 { font-size: 17px; text-align: center; margin: 0 0 2px; }
  h2 { font-size: 15px; text-align: center; margin: 0 0 6px; letter-spacing: 1px; }
  .c { text-align: center; }
  .linha { border-top: 1px dashed #000; margin: 6px 0; }
  .item { font-size: 16px; font-weight: bold; margin: 5px 0 0; }
  .obs { font-size: 14px; margin: 0 0 0 10px; }
  .tot { display: flex; justify-content: space-between; }
  .forte { font-size: 16px; font-weight: bold; }
  .peq { font-size: 11px; }
`

function imprimirHtml(corpo) {
  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;visibility:hidden'
  document.body.appendChild(iframe)
  const doc = iframe.contentWindow.document
  doc.open()
  doc.write(`<!doctype html><html><head><meta charset="utf-8"><title>Ticket</title><style>${ESTILO}</style></head><body>${corpo}</body></html>`)
  doc.close()
  const limpar = () => setTimeout(() => iframe.remove(), 1500)
  iframe.contentWindow.onafterprint = limpar
  setTimeout(() => {
    iframe.contentWindow.focus()
    iframe.contentWindow.print()
    limpar()
  }, 250)
}

/** Um ticket por praca (cozinha, bar...) com os itens do pedido (ignora cancelados). */
export function imprimirPedido(comanda, pedido) {
  const porPraca = {}
  pedido.itens.filter((i) => !i.cancelado).forEach((i) => {
    ;(porPraca[i.praca] ||= []).push(i)
  })
  const nomes = { COZINHA: 'COZINHA', BAR: 'BAR' }
  const corpo = Object.entries(porPraca).map(([praca, itens]) => `
    <div class="ticket">
      <h1>ARAÇA BEACH</h1>
      <h2>${esc(nomes[praca] || praca)}</h2>
      <div class="c forte">${esc(comanda.rotulo)}</div>
      <div class="c peq">Comanda #${comanda.id} · Pedido ${pedido.numero} · ${esc(hora(pedido.criadoEm))}</div>
      <div class="linha"></div>
      ${itens.map((i) => `
        <div class="item">${i.quantidade}x ${esc(i.nome)}</div>
        ${i.observacao ? `<div class="obs">» ${esc(i.observacao)}</div>` : ''}`).join('')}
      <div class="linha"></div>
    </div>`).join('')
  if (corpo) imprimirHtml(corpo)
}

/** Reimpressao de um pedido apenas com o aviso de cancelamento de um item. */
export function imprimirCancelamento(comanda, item) {
  imprimirHtml(`
    <div class="ticket">
      <h1>*** CANCELADO ***</h1>
      <div class="c forte">${esc(comanda.rotulo)}</div>
      <div class="linha"></div>
      <div class="item">${item.quantidade}x ${esc(item.nome)}</div>
      ${item.motivoCancelamento ? `<div class="obs">${esc(item.motivoCancelamento)}</div>` : ''}
      <div class="linha"></div>
    </div>`)
}

/** Um unico ticket avisando cozinha/bar que a comanda inteira foi cancelada. */
export function imprimirCancelamentoComanda(comanda, itens, motivo) {
  imprimirHtml(`
    <div class="ticket">
      <h1>*** COMANDA CANCELADA ***</h1>
      <div class="c forte">${esc(comanda.rotulo)}</div>
      <div class="linha"></div>
      ${itens.map((i) => `<div class="item">${i.quantidade}x ${esc(i.nome)}</div>`).join('')}
      ${motivo ? `<div class="obs">${esc(motivo)}</div>` : ''}
      <div class="linha"></div>
    </div>`)
}

/** Pre-conta (nao fiscal) para conferencia do cliente. */
export function imprimirConta(comanda) {
  const itens = comanda.pedidos.flatMap((p) => p.itens).filter((i) => !i.cancelado)
  imprimirHtml(`
    <div class="ticket">
      <h1>ARAÇA BEACH</h1>
      <h2>CONTA</h2>
      <div class="c forte">${esc(comanda.rotulo)}</div>
      <div class="c peq">Comanda #${comanda.id} · ${new Date().toLocaleString('pt-BR')}</div>
      <div class="linha"></div>
      ${itens.map((i) => `<div class="tot"><span>${i.quantidade}x ${esc(i.nome)}</span><span>${brl(i.subtotal)}</span></div>`).join('')}
      <div class="linha"></div>
      <div class="tot"><span>Subtotal</span><span>${brl(comanda.subtotal)}</span></div>
      ${Number(comanda.taxaServico) > 0 ? `<div class="tot"><span>Serviço (${Number(comanda.taxaServicoPercentual)}%)</span><span>${brl(comanda.taxaServico)}</span></div>` : ''}
      ${Number(comanda.desconto) > 0 ? `<div class="tot"><span>Desconto</span><span>-${brl(comanda.desconto)}</span></div>` : ''}
      <div class="tot forte"><span>TOTAL</span><span>${brl(comanda.total)}</span></div>
      <div class="linha"></div>
      <div class="c peq">Documento sem valor fiscal</div>
    </div>`)
}

/** Comprovante (nao fiscal) de uma comanda da Loja da arena. */
export function imprimirComprovanteLoja(comanda) {
  const itens = comanda.itens || []
  const total = itens.reduce((soma, i) => soma + Number(i.precoUnitario) * i.quantidade, 0)
  imprimirHtml(`
    <div class="ticket">
      <h1>ARAÇA BEACH</h1>
      <h2>LOJA</h2>
      <div class="c forte">${esc(comanda.cliente?.nome || '')}</div>
      <div class="c peq">Comanda #${comanda.id} · ${new Date().toLocaleString('pt-BR')}</div>
      <div class="linha"></div>
      ${itens.map((i) => `
        <div class="tot"><span>${i.quantidade}x ${esc(i.produto?.nome)}</span><span>${brl(Number(i.precoUnitario) * i.quantidade)}</span></div>
        ${i.quantidade > 1 ? `<div class="peq" style="margin-left:10px">${brl(i.precoUnitario)} cada</div>` : ''}`).join('')}
      <div class="linha"></div>
      <div class="tot forte"><span>TOTAL</span><span>${brl(total)}</span></div>
      <div class="linha"></div>
      <div class="c peq">Obrigado pela preferência!</div>
      <div class="c peq">Documento sem valor fiscal</div>
    </div>`)
}
