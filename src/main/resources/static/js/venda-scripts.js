document.addEventListener("DOMContentLoaded",()=>{console.log("Página de vendas carregada!");let e=e=>String(e??"").replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;").replaceAll('"',"&quot;").replaceAll("'","&#39;"),a=document.getElementById("formVenda");if(!a)return void console.error("Formulário de venda não encontrado.");async function t(e,a=null){let o=document.querySelector('meta[name="_csrf"]').content,n=document.querySelector('meta[name="_csrf_header"]').content,r=a?`/vendas/editar/${a}`:"/vendas/nova",d=await fetch(r,{method:a?"PUT":"POST",headers:{[n]:o,"Content-Type":"application/json"},body:JSON.stringify(e)}),l=await d.json();if(!d.ok)throw Error(l.error||`Erro ${d.status}: Falha na requisi\xe7\xe3o`);return l}function o(e,t="danger"){let n=document.createElement("div");n.className=`alert alert-${t} alert-dismissible fade show`,n.role="alert";let r=document.createTextNode(e),d=document.createElement("button");d.type="button",d.className="btn-close",d.setAttribute("data-bs-dismiss","alert"),d.setAttribute("aria-label","Fechar"),n.appendChild(r),n.appendChild(d),a.insertAdjacentElement("beforebegin",n),setTimeout(()=>n.remove(),5e3)}async function n(){try{let e=await fetch("/vendas/total-hoje");if(!e.ok)throw Error("Erro ao buscar total de vendas");let a=await e.json();document.querySelectorAll(".venda-metric").forEach(e=>{e.textContent=`R$ ${parseFloat(a.totalVendasHoje).toFixed(2)}`})}catch(e){console.error("Erro ao atualizar dashboard:",e),o("Falha ao atualizar o dashboard.","warning")}}async function r(){try{let a=await fetch("/vendas/listar");if(!a.ok)throw Error("Erro ao buscar vendas");let t=await a.json(),o=document.querySelector("#tabelaVendas tbody");o.innerHTML="",t.forEach(a=>{let t=document.createElement("tr"),n=(a.quantidade*a.valorUnitarioVenda).toFixed(2);t.innerHTML=`
                    <td>${new Date(a.dataVenda).toLocaleString("pt-BR")}</td>
                    <td>${e(a.produtoSabor||"Desconhecido")}</td>
                    <td>${a.quantidade}</td>
                    <td>${e(a.vendido)}</td>
                    <td>${e(a.formaPagamento)}</td>
                    <td>R$ ${parseFloat(a.valorUnitarioVenda).toFixed(2)}</td>
                    <td>R$ ${n}</td>
                    <td>
                        <button class="btn btn-sm btn-primary me-1" onclick="editVenda('${e(a.id)}')">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteVenda('${e(a.id)}')">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                `,o.appendChild(t)})}catch(e){console.error("Erro ao carregar vendas:",e),o("Falha ao carregar as vendas.","warning")}}console.log("Formulário de venda encontrado."),a.addEventListener("submit",async e=>{e.preventDefault(),console.log("Formulário de venda submetido.");try{let e=function(e){return{produtoId:e.querySelector('[name="produtoId"]').value,quantidade:e.querySelector('[name="quantidade"]').value,vendido:e.querySelector('[name="vendido"]').value,valorUnitarioVenda:e.querySelector('[name="valorUnitarioVenda"]').value,formaPagamento:e.querySelector('[name="formaPagamento"]').value,dataVenda:e.querySelector('[name="dataVenda"]').value}}(a);if(!function({produtoId:e,quantidade:a,vendido:t,valorUnitarioVenda:n,formaPagamento:r,dataVenda:d}){if(!e||!a||!t||!n||!r||!d)return!1;let l=parseInt(a),i=parseFloat(n);return isNaN(l)||l<1?(o("Quantidade inválida! Deve ser pelo menos 1.","warning"),!1):isNaN(i)||i<=0?(o("Valor unitário inválido! Deve ser maior que zero.","warning"),!1):!!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(d)||(o("Formato de data inválido! Use o formato correto.","warning"),!1)}(e))return void o("Por favor, preencha todos os campos corretamente.","danger");let d=function(e){return{produtoId:parseInt(e.produtoId),quantidade:parseInt(e.quantidade),vendido:e.vendido,valorUnitarioVenda:parseFloat(e.valorUnitarioVenda),formaPagamento:e.formaPagamento,dataVenda:`${e.dataVenda}:00`}}(e),l=document.getElementById("vendaId")?.value,i=await t(d,l);if(i.success)(function(e,a,t=null){let o=!!t,n=new Date(a).toLocaleDateString("pt-BR"),r=parseFloat(e).toFixed(2),d=document.createElement("div");d.innerHTML=`
            <div class="modal fade" id="successModal" tabindex="-1" aria-labelledby="successModalLabel" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header" style="background-color: #9370DB; color: white;">
                            <h5 class="modal-title" id="successModalLabel">${o?"Venda Atualizada!":"Venda Registrada!"}</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fechar"></button>
                        </div>
                        <div class="modal-body">
                            <p>Voc\xea vendeu no dia: ${n}.</p>
                            <p>R$ ${r}. </p>
                            <h6>Parab\xe9ns!</h6>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Fechar</button>
                            ${o?"":'<button type="button" class="btn btn-ice" onclick="window.location.href=\'/vendas/nova\'">Nova Venda</button>'}
                        </div>
                    </div>
                </div>
            </div>
        `,document.body.appendChild(d),new bootstrap.Modal(d.querySelector("#successModal")).show(),d.addEventListener("hidden.bs.modal",()=>d.remove())})(i.totalVenda,d.dataVenda,l),await n(),await r(),l?window.location.href="/vendas/nova":a.reset();else throw Error(i.error||"Erro ao processar venda.")}catch(e){console.error("Erro ao processar venda:",e),o(`Ocorreu um erro: ${e.message}`,"danger")}}),r(),window.editVenda=function(e){window.location.href=`/vendas/editar/${e}`},window.deleteVenda=async function(e){if(confirm("Tem certeza que deseja deletar esta venda?"))try{let a=document.querySelector('meta[name="_csrf"]').content,t=document.querySelector('meta[name="_csrf_header"]').content;if(!(await fetch(`/vendas/deletar/${e}`,{method:"DELETE",headers:{[t]:a}})).ok)throw Error("Erro ao deletar venda");o("Venda deletada com sucesso!","success"),await r()}catch(e){console.error("Erro ao deletar venda:",e),o(`Erro ao deletar venda: ${e.message}`,"danger")}}});
//# sourceMappingURL=venda-scripts.js.map
