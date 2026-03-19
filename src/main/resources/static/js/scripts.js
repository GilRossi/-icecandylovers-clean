document.addEventListener("DOMContentLoaded",function(){console.log("Dashboard carregada!");let e=document.querySelector('meta[name="_csrf"]').content,t=document.querySelector('meta[name="_csrf_header"]').content,o=[];function n(){fetch("/ingredientes/listar").then(e=>e.json()).then(e=>{console.log("Ingredientes carregados:",o=e)}).catch(e=>console.error("Erro ao carregar ingredientes:",e))}function a(){document.querySelectorAll(".btn-editar-geladinho").forEach(e=>{e.addEventListener("click",()=>(function(e){let t=e.getAttribute("data-id");console.log("Buscando detalhes do geladinho ID:",t),fetch(`/geladinhos/detalhes/${t}`).then(e=>e.json()).then(e=>{console.log("Dados do geladinho recebidos:",e),function(e){console.log("Preenchendo formulário com os dados do geladinho:",e),document.getElementById("editarGeladinhoId").value=e.id||"",document.getElementById("editarGeladinhoSabor").value=e.sabor||"",document.getElementById("editarGeladinhoEstoqueInicial").value=e.estoqueInicial||"",document.getElementById("editarGeladinhoEstoqueAtual").value=e.estoqueAtual||"",document.getElementById("editarGeladinhoPrecoCusto").value=parseFloat(e.precoCusto)||0,document.getElementById("editarGeladinhoPrecoCustoUnitario").value=parseFloat(e.precoCustoUnitario)||0;let t=document.getElementById("editarIngredientesContainer");t.innerHTML="",e.ingredientes&&Array.isArray(e.ingredientes)?(console.log("Ingredientes encontrados:",e.ingredientes),e.ingredientes.forEach((e,o)=>{let n=r(o,e);t.insertAdjacentHTML("beforeend",n)})):console.log("Nenhum ingrediente encontrado ou dados mal formatados.")}(e),new bootstrap.Modal(document.getElementById("editarGeladinhoModal")).show()}).catch(e=>alert("Erro ao carregar detalhes do geladinho."))})(e))})}function d(){let e=document.getElementById("editarIngredientesContainer"),t=r(e.children.length,{nome:"Novo Ingrediente",custoPorUnidade:0,unidadeMedida:"unidades",estoqueAtual:0});e.insertAdjacentHTML("beforeend",t)}function r(e,t){return`
            <div class="ingrediente-item row g-2 mb-2" data-index="${e}">
                <div class="col-5">
                    <label>
                        <select class="form-select" name="ingredientes[${e}].ingredienteId" required>
                            <option value="">Selecione um ingrediente</option>
                            ${o.map(e=>`
                                <option value="${e.id}">${e.nome} - ${e.custoPorUnidade.toFixed(2)} R$ (${e.unidadeMedida}, ${e.estoqueAtual} em estoque)</option>
                            `).join("")}
                        </select>
                    </label>
                </div>
                <div class="col-5">
                    <label>
                        <input class="form-control"
                               type="number"
                               name="ingredientes[${e}].quantidade"
                               placeholder="Quantidade"
                               min="0"
                               step="0.1"
                               required>
                    </label>
                </div>
                <div class="col-2">
                    <i class="bi bi-x-circle-fill text-danger"
                       onclick="removerIngrediente(this)"
                       style="cursor:pointer;"></i>
                </div>
            </div>
        `}function i(){fetch("/geladinhos/listar").then(e=>e.json()).then(o=>(function(o){console.log("Renderizando tabela de geladinhos...");let n=document.getElementById("tabelaGeladinhos");if(n){if(n.innerHTML="",!Array.isArray(o))return void console.error("Dados recebidos não são um array:",o);o.forEach(e=>{let t=e.ingredientes&&Array.isArray(e.ingredientes)?e.ingredientes.map(e=>`<span class="badge bg-light text-dark me-1 fw-bold">${e.nome}</span>`).join(""):'<span class="text-muted">Nenhum ingrediente</span>',o=e.estoqueAtual<10?`<span class="text-danger fw-bold">${e.estoqueAtual}</span>`:e.estoqueAtual,a=`
                <tr>
                    <td>${e.sabor||"N/A"}</td>
                    <td class="mobile-hide">${t}</td>
                    <td>${o}</td>
                    <td>${e.precoCustoUnitario?e.precoCustoUnitario.toFixed(2):"N/A"}</td>
                    <td>
                        <button class="btn btn-warning btn-sm btn-editar-geladinho" data-id="${e.id}">
                            <i class="bi bi-pencil"></i>
                            <span class="d-none d-md-inline">Editar</span>
                        </button>
                        <button class="btn btn-danger btn-sm btn-deletar-geladinho" data-id="${e.id}">
                            <i class="bi bi-trash"></i>
                            <span class="d-none d-md-inline">Deletar</span>
                        </button>
                    </td>
                </tr>
            `;n.insertAdjacentHTML("beforeend",a)}),a(),document.querySelectorAll(".btn-deletar-geladinho").forEach(o=>{o.addEventListener("click",function(){var n;let a=o.getAttribute("data-id");confirm("Tem certeza que deseja deletar este geladinho?")&&(n=a,fetch(`/geladinhos/deletar/${n}`,{method:"DELETE",headers:{[t]:e}}).then(e=>{if(!e.ok)throw Error("Erro ao deletar o geladinho");return e.json()}).then(()=>{alert("Geladinho deletado com sucesso!"),i()}).catch(e=>{alert("Erro ao deletar geladinho."),console.error(e)}))})})}})(o)).catch(e=>console.error("Erro ao atualizar a tabela:",e))}document.querySelector("#novoIngredienteModal .btn-secondary-icl").addEventListener("click",function(){bootstrap.Modal.getInstance(document.getElementById("novoIngredienteModal")).hide(),new bootstrap.Modal(document.getElementById("editarGeladinhoModal")).show()}),n(),a(),function(){let o=document.getElementById("formEditarGeladinho");o&&o.addEventListener("submit",function(o){o.preventDefault(),function(){let o=new FormData(document.getElementById("formEditarGeladinho")),n=[];document.querySelectorAll(".ingrediente-item").forEach((e,t)=>{let o=e.querySelector("select").value,a=e.querySelector("input").value;n.push({ingredienteId:o,quantidade:a})});let a={id:o.get("id"),sabor:o.get("sabor"),estoqueInicial:o.get("estoqueInicial"),estoqueAtual:o.get("estoqueAtual"),precoCusto:o.get("precoCusto"),precoCustoUnitario:o.get("precoCustoUnitario"),ingredientes:n};console.log("Enviando dados para atualizar geladinho:",a),fetch("/geladinhos/editar",{method:"POST",headers:{"Content-Type":"application/json",[t]:e},body:JSON.stringify(a)}).then(e=>{if(!e.ok)throw Error("Erro ao salvar as alterações");return console.log("Resposta recebida do servidor para atualização de geladinho"),e.json()}).then(e=>{console.log("Dados retornados após atualizar geladinho:",e);let t=document.getElementById("successToast");if(!t){console.error("Elemento successToast não encontrado!"),alert("Geladinho atualizado com sucesso!");return}let o=t.querySelector(".toast-body");if(!o){console.error("Elemento toast-body não encontrado!"),alert("Geladinho atualizado com sucesso!");return}o.textContent="Geladinho atualizado com sucesso!";let n=new bootstrap.Toast(t);console.log("Exibindo toast para geladinho atualizado"),n.show(),i(),bootstrap.Modal.getInstance(document.getElementById("editarGeladinhoModal")).hide()}).catch(e=>{alert("Erro ao salvar as alterações: "+e.message),console.error("Detalhes do erro ao salvar geladinho:",e)})}()});let n=document.getElementById("addIngredienteBtnEditar");n&&n.addEventListener("click",d)}(),i(),document.getElementById("salvarNovoIngredienteBtn").addEventListener("click",function(){let o={nome:document.getElementById("novoIngredienteNome").value.trim(),custoPorUnidade:parseFloat(document.getElementById("novoIngredienteCusto").value),unidadeMedida:document.getElementById("novoIngredienteUnidade").value,estoqueInicial:parseFloat(document.getElementById("novoIngredienteEstoque").value)};if(!o.nome||isNaN(o.custoPorUnidade)||!o.unidadeMedida||isNaN(o.estoqueInicial))return void alert("Por favor, preencha todos os campos corretamente!");console.log("Enviando dados para salvar ingrediente:",o),fetch("/ingredientes/salvar",{method:"POST",headers:{"Content-Type":"application/json",[t]:e},body:JSON.stringify(o)}).then(e=>e.ok?(console.log("Resposta recebida do servidor para novo ingrediente"),e.json()):e.text().then(t=>{throw Error(`Erro ${e.status}: ${t}`)})).then(e=>{console.log("Dados retornados após salvar ingrediente:",e);let t=document.getElementById("successToast");if(!t){console.error("Elemento successToast não encontrado!"),alert("Novo ingrediente cadastrado com sucesso!");return}let o=t.querySelector(".toast-body");if(!o){console.error("Elemento toast-body não encontrado!"),alert("Novo ingrediente cadastrado com sucesso!");return}o.textContent="Novo ingrediente cadastrado com sucesso!";let a=new bootstrap.Toast(t);console.log("Exibindo toast para novo ingrediente"),a.show(),n(),bootstrap.Modal.getInstance(document.getElementById("novoIngredienteModal")).hide(),new bootstrap.Modal(document.getElementById("editarGeladinhoModal")).show()}).catch(e=>{alert("Erro ao salvar ingrediente: "+e.message),console.error("Detalhes do erro ao salvar ingrediente:",e)})})});
//# sourceMappingURL=scripts.js.map
