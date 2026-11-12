'use strict';

/* =========================================================================
 * Configuração
 * ========================================================================= */

const CONFIG = {
    // 'auto'  -> usa a API se GET /api/vagas responder; senão usa dados mockados.
    // 'api'   -> sempre a API real.
    // 'mock'  -> sempre os dados mockados.
    // Também é possível forçar o mock abrindo a página com ?mock na URL.
    modo: 'auto',
};

// Valores só para exibição (rótulos dos botões e tabela de preços).
// Quem calcula a cobrança é sempre a API: TarifaBase.VALOR_HORA, Lavagem.VALOR e
// Manobrista.VALOR no backend. Se mudarem lá, atualize aqui também.
const TARIFA_HORA = 10;

// Serviços extras (Decorator). Para a Parte 2 (ex.: RecargaEletrica),
// basta acrescentar uma linha aqui com o tipo aceito pelo backend.
const SERVICOS = [
    { tipo: 'LAVAGEM', nome: 'Lavagem', valor: 30 },
    { tipo: 'MANOBRISTA', nome: 'Manobrista', valor: 20 },
];

const ROTULOS_ESTADO = {
    LIVRE: 'Livre',
    OCUPADA: 'Ocupada',
    RESERVADA: 'Reservada',
};

class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

/* =========================================================================
 * API real (fetch)
 * ========================================================================= */

async function request(method, url, body) {
    const opcoes = { method, headers: {} };
    if (body !== undefined) {
        opcoes.headers['Content-Type'] = 'application/json';
        opcoes.body = JSON.stringify(body);
    }

    const resposta = await fetch(url, opcoes);
    const texto = await resposta.text();
    let dados = null;
    if (texto) {
        try {
            dados = JSON.parse(texto);
        } catch {
            dados = texto;
        }
    }

    if (!resposta.ok) {
        const mensagem = (dados && (dados.erro || dados.message)) || `Erro ${resposta.status}`;
        throw new ApiError(resposta.status, mensagem);
    }
    return dados;
}

const apiReal = {
    listarVagas: () => request('GET', '/api/vagas'),
    criarVaga: (numero) => request('POST', '/api/vagas', { numero }),
    reservar: (id) => request('POST', `/api/vagas/${id}/reservar`),
    liberar: (id) => request('POST', `/api/vagas/${id}/liberar`),
    registrarEntrada: (vagaId, placa, modelo) =>
        request('POST', '/api/permanencias', { placa, modelo, vagaId }),
    adicionarServico: (id, tipo) => request('POST', `/api/permanencias/${id}/servicos`, { tipo }),
    registrarSaida: (id) => request('POST', `/api/permanencias/${id}/saida`),
    listarPermanencias: () => request('GET', '/api/permanencias'),
};

/* =========================================================================
 * API mockada (mesmas funções, dados em memória)
 * Reproduz as regras da spec só para o frontend funcionar sem o backend.
 * ========================================================================= */

function criarApiMock() {
    const VALOR_HORA = TARIFA_HORA;
    const HORA = 3600 * 1000;
    const TRANSICOES = {
        LIVRE: { ocupar: 'OCUPADA', reservar: 'RESERVADA' },
        OCUPADA: { liberar: 'LIVRE' },
        RESERVADA: { ocupar: 'OCUPADA', liberar: 'LIVRE' },
    };

    const agora = Date.now();
    const db = {
        vagas: [
            { id: 1, numero: 1, estado: 'LIVRE' },
            { id: 2, numero: 2, estado: 'OCUPADA' },
            { id: 3, numero: 3, estado: 'RESERVADA' },
            { id: 4, numero: 4, estado: 'LIVRE' },
        ],
        permanencias: [
            {
                id: 1, vagaId: 2, placa: 'ABC1D23', modelo: 'Onix',
                entrada: new Date(agora - 2.5 * HORA).toISOString(), saida: null,
                servicos: ['LAVAGEM'],
            },
            {
                id: 2, vagaId: 4, placa: 'XYZ9K87', modelo: 'HB20',
                entrada: new Date(agora - 5 * HORA).toISOString(),
                saida: new Date(agora - 1 * HORA).toISOString(),
                servicos: ['MANOBRISTA'],
            },
        ],
        proximaVaga: 5,
        proximaPermanencia: 3,
    };

    // Mensagens iguais às do backend (TransicaoInvalidaException, EstacionamentoService...).
    function transitar(vaga, operacao) {
        const proximo = TRANSICOES[vaga.estado] && TRANSICOES[vaga.estado][operacao];
        if (!proximo) {
            throw new ApiError(409, `Não é possível ${operacao} uma vaga no estado ${vaga.estado}`);
        }
        vaga.estado = proximo;
    }

    function acharVaga(id) {
        const vaga = db.vagas.find((v) => v.id === Number(id));
        if (!vaga) throw new ApiError(404, `Vaga ${id} não encontrada`);
        return vaga;
    }

    function acharPermanencia(id) {
        const permanencia = db.permanencias.find((p) => p.id === Number(id));
        if (!permanencia) throw new ApiError(404, `Permanência ${id} não encontrada`);
        return permanencia;
    }

    // Fração de hora conta como hora cheia, com mínimo de 1h (igual a Permanencia.horasAte).
    function horas(p) {
        const fim = p.saida ? new Date(p.saida) : new Date();
        return Math.max(1, Math.ceil((fim - new Date(p.entrada)) / HORA));
    }

    // Mesmo formato do PermanenciaResponse da API.
    function paraDto(p) {
        const h = horas(p);
        const extras = p.servicos.map((tipo) => SERVICOS.find((s) => s.tipo === tipo));
        const vaga = db.vagas.find((v) => v.id === p.vagaId);
        const valor = h * VALOR_HORA + extras.reduce((soma, s) => soma + s.valor, 0);
        return {
            id: p.id,
            vagaId: p.vagaId,
            vagaNumero: vaga ? vaga.numero : null,
            placa: p.placa,
            modelo: p.modelo,
            entrada: p.entrada,
            saida: p.saida,
            ativa: !p.saida,
            servicos: [...p.servicos],
            descricao: [`Tarifa ${h}h`, ...extras.map((s) => s.nome)].join(' + '),
            valorAtual: valor,
            valorTotal: p.saida ? valor : null,
        };
    }

    // Simula a latência da rede e devolve cópias, como uma API faria.
    const comAtraso = (fn) => (...args) =>
        new Promise((resolve, reject) => {
            setTimeout(() => {
                try {
                    resolve(JSON.parse(JSON.stringify(fn(...args))));
                } catch (e) {
                    reject(e instanceof ApiError ? e : new ApiError(500, e.message));
                }
            }, 150);
        });

    return {
        listarVagas: comAtraso(() => db.vagas),
        criarVaga: comAtraso((numero) => {
            if (!Number.isInteger(numero) || numero <= 0) {
                throw new ApiError(400, 'Número da vaga deve ser um inteiro positivo');
            }
            if (db.vagas.some((v) => v.numero === numero)) {
                throw new ApiError(409, `Já existe uma vaga com o número ${numero}`);
            }
            const vaga = { id: db.proximaVaga++, numero, estado: 'LIVRE' };
            db.vagas.push(vaga);
            return vaga;
        }),
        reservar: comAtraso((id) => {
            const vaga = acharVaga(id);
            transitar(vaga, 'reservar');
            return vaga;
        }),
        liberar: comAtraso((id) => {
            const vaga = acharVaga(id);
            const ativa = db.permanencias.find((p) => p.vagaId === vaga.id && !p.saida);
            if (ativa) {
                throw new ApiError(409, `Vaga ${vaga.numero} está ocupada pela permanência ${ativa.id}; `
                    + 'registre a saída para liberá-la');
            }
            transitar(vaga, 'liberar');
            return vaga;
        }),
        registrarEntrada: comAtraso((vagaId, placa, modelo) => {
            const vaga = acharVaga(vagaId);
            const jaEstacionado = db.permanencias.find((p) => p.placa === placa && !p.saida);
            if (jaEstacionado) {
                const numero = db.vagas.find((v) => v.id === jaEstacionado.vagaId).numero;
                throw new ApiError(409, `Veículo ${placa} já está estacionado na vaga ${numero}`);
            }
            transitar(vaga, 'ocupar');
            const p = {
                id: db.proximaPermanencia++, vagaId: vaga.id, placa, modelo,
                entrada: new Date().toISOString(), saida: null, servicos: [],
            };
            db.permanencias.push(p);
            return paraDto(p);
        }),
        adicionarServico: comAtraso((id, tipo) => {
            if (!SERVICOS.some((s) => s.tipo === tipo)) {
                throw new ApiError(400, `Tipo de serviço inválido: ${tipo}`);
            }
            const p = acharPermanencia(id);
            if (p.saida) throw new ApiError(409, `Permanência ${id} já foi finalizada`);
            if (p.servicos.includes(tipo)) {
                throw new ApiError(409, `Serviço ${tipo} já foi adicionado à permanência ${id}`);
            }
            p.servicos.push(tipo);
            return paraDto(p);
        }),
        registrarSaida: comAtraso((id) => {
            const p = acharPermanencia(id);
            if (p.saida) throw new ApiError(409, `Permanência ${id} já foi finalizada`);
            transitar(acharVaga(p.vagaId), 'liberar');
            p.saida = new Date().toISOString();
            return paraDto(p);
        }),
        listarPermanencias: comAtraso(() => db.permanencias.map(paraDto)),
    };
}

/* =========================================================================
 * Conversão das respostas da API (VagaResponse e PermanenciaResponse)
 * ========================================================================= */

// VagaResponse: { id, numero, estado }
function normalizarVaga(v) {
    return {
        id: v.id,
        numero: v.numero,
        estado: String(v.estado ?? '').toUpperCase(),
    };
}

// PermanenciaResponse: { id, vagaId, vagaNumero, placa, modelo, entrada, saida,
// ativa, servicos, descricao, valorAtual, valorTotal }.
// valorAtual = cobrança parcial (ativa) ou final; valorTotal = null enquanto ativa.
function normalizarPermanencia(p) {
    return {
        id: p.id,
        vagaId: p.vagaId,
        vagaNumero: p.vagaNumero,
        placa: p.placa ?? '',
        modelo: p.modelo ?? '',
        entrada: p.entrada ?? null,
        saida: p.saida ?? null,
        ativa: p.ativa ?? !p.saida,
        servicos: Array.isArray(p.servicos) ? p.servicos : [],
        descricao: p.descricao ?? null,
        valorAtual: p.valorAtual ?? null,
        valorTotal: p.valorTotal ?? null,
    };
}

// Valor a exibir: o total final quando existe, senão a cobrança parcial.
function valorExibido(p) {
    return p.valorTotal ?? p.valorAtual;
}

/* =========================================================================
 * Estado da tela
 * ========================================================================= */

const tela = {
    vagas: [],
    permanencias: [],
    selecionada: null,     // id da vaga aberta no card lateral
    historicoId: null,     // id da permanência finalizada aberta no card
    recibo: null,          // permanência recém-finalizada (recibo no topo do card)
    filtro: 'todas',
    carregando: true,      // primeira carga: mostra esqueletos
    offline: false,
    ultimaAtualizacao: null,
    ocupado: false,        // há uma ação em andamento
    saidaPendente: null,   // permanência aguardando confirmação de saída
};

let api = apiReal;
let usandoMock = false;

const $ = (seletor) => document.querySelector(seletor);

/* =========================================================================
 * Utilitários
 * ========================================================================= */

const formatoMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const formatoDataHora = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
const formatoHora = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

function moeda(valor) {
    return valor == null ? '—' : formatoMoeda.format(Number(valor));
}

function comoData(valor) {
    if (!valor) return null;
    const data = new Date(valor);
    return Number.isNaN(data.getTime()) ? null : data;
}

function dataHora(valor) {
    const data = comoData(valor);
    return data ? formatoDataHora.format(data) : '—';
}

function hora(valor) {
    const data = comoData(valor);
    return data ? formatoHora.format(data) : '—';
}

// 8100000 ms -> "2h15"; 2700000 -> "45 min"
function duracao(ms) {
    const minutos = Math.max(0, Math.floor(ms / 60000));
    const h = Math.floor(minutos / 60);
    const m = minutos % 60;
    if (h === 0) return `${m} min`;
    return `${h}h${String(m).padStart(2, '0')}`;
}

function escapar(texto) {
    return String(texto ?? '').replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
    })[c]);
}

function rotulo(estado) {
    return ROTULOS_ESTADO[estado] || (estado ? estado.charAt(0) + estado.slice(1).toLowerCase() : '—');
}

function servicoPorTipo(tipo) {
    return SERVICOS.find((s) => s.tipo === tipo) || null;
}

function nomeServico(tipo) {
    const servico = servicoPorTipo(tipo);
    return servico ? servico.nome : tipo.charAt(0) + tipo.slice(1).toLowerCase().replace(/_/g, ' ');
}

function plural(n, um, varios) {
    return `${n} ${n === 1 ? um : varios}`;
}

// "Tarifa 3h + Lavagem" -> 3
function horasDaDescricao(descricao) {
    const achado = /Tarifa\s+(\d+)h/i.exec(descricao || '');
    return achado ? Number(achado[1]) : null;
}

// Monta as linhas do recibo a partir do que a API devolve (descricao, servicos, total).
// A tarifa é o total menos os extras, então as linhas sempre somam o total da API.
function linhasCobranca(p) {
    const total = valorExibido(p);
    const extras = p.servicos.map((tipo) => ({ nome: nomeServico(tipo), valor: servicoPorTipo(tipo)?.valor ?? null }));
    const somaExtras = extras.reduce((soma, e) => soma + (e.valor ?? 0), 0);
    const horas = horasDaDescricao(p.descricao);
    const tarifa = total != null ? total - somaExtras : null;
    const rotuloTarifa = horas && tarifa === horas * TARIFA_HORA
        ? `Tarifa ${horas}h × ${moeda(TARIFA_HORA)}`
        : `Tarifa${horas ? ` ${horas}h` : ''}`;
    return [{ nome: rotuloTarifa, valor: tarifa }, ...extras.map((e) => ({ nome: `+ ${e.nome}`, valor: e.valor }))];
}

function htmlLinhasCobranca(p) {
    return linhasCobranca(p)
        .map((l) => `<div><span>${escapar(l.nome)}</span><span>${moeda(l.valor)}</span></div>`)
        .join('');
}

function htmlPlaca(placa, pequena = false) {
    return `
        <div class="placa${pequena ? ' placa--pequena' : ''}" role="img" aria-label="Placa ${escapar(placa)}">
            <div class="placa__faixa" aria-hidden="true">BRASIL</div>
            <div class="placa__numero" aria-hidden="true">${escapar(placa)}</div>
        </div>`;
}

const ICONE_CHECK = '<svg aria-hidden="true" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"><path d="m5 12 5 5L20 7"/></svg>';
const ICONE_ERRO = '<svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v5"/><path d="M12 16h.01"/></svg>';
const ICONE_CONE_PEQUENO = '<svg aria-hidden="true" style="flex:none" width="26" height="30" viewBox="0 0 34 40"><path d="M17 3 27 33H7z" fill="#fbbf24"/><path d="M12 18h10M10 25h14" stroke="#ffffff" stroke-width="3"/><rect x="3" y="33" width="28" height="5" rx="2" fill="#fbbf24"/></svg>';
const ICONE_CONE = '<svg class="vaga__icone" aria-hidden="true" width="40" height="47" viewBox="0 0 34 40"><path d="M17 3 27 33H7z" fill="#fbbf24"/><path d="M12 18h10M10 25h14" stroke="#ffffff" stroke-width="3"/><rect x="3" y="33" width="28" height="5" rx="2" fill="#fbbf24"/></svg>';

// Carro visto de cima; "baixo" inverte para a frente apontar para o corredor.
function iconeCarro(fileira) {
    const vidros = fileira === 'baixo'
        ? '<rect x="8" y="11" width="24" height="11" rx="3" fill="#1e293b"/><rect x="8" y="37" width="24" height="9" rx="3" fill="#1e293b"/><rect x="9" y="24" width="22" height="11" rx="2" fill="#ef5f5f"/>'
        : '<rect x="8" y="40" width="24" height="11" rx="3" fill="#1e293b"/><rect x="8" y="16" width="24" height="9" rx="3" fill="#1e293b"/><rect x="9" y="27" width="22" height="11" rx="2" fill="#ef5f5f"/>';
    return `<svg class="vaga__icone" aria-hidden="true" width="48" height="74" viewBox="0 0 40 62">
        <rect x="1" y="9" width="5" height="11" rx="2" fill="#020617"/><rect x="34" y="9" width="5" height="11" rx="2" fill="#020617"/>
        <rect x="1" y="42" width="5" height="11" rx="2" fill="#020617"/><rect x="34" y="42" width="5" height="11" rx="2" fill="#020617"/>
        <rect x="4" y="1" width="32" height="60" rx="11" fill="#f87171"/>${vidros}</svg>`;
}

/* =========================================================================
 * Consultas ao estado da tela
 * ========================================================================= */

function vagaPorId(id) {
    return tela.vagas.find((v) => v.id === id) || null;
}

function vagaSelecionada() {
    return vagaPorId(tela.selecionada);
}

function permanenciaAtiva(vagaId) {
    return tela.permanencias.find((p) => p.vagaId === vagaId && p.ativa) || null;
}

function numeroDaVaga(p) {
    if (p.vagaNumero != null) return p.vagaNumero;
    const vaga = vagaPorId(p.vagaId);
    return vaga ? vaga.numero : '?';
}

function contar(estado) {
    return tela.vagas.filter((v) => v.estado === estado).length;
}

/* =========================================================================
 * Aviso flutuante (toast)
 * ========================================================================= */

let timerToast;
function avisar(mensagem, tipo = 'sucesso', codigo = null) {
    const toast = $('#toast');
    const etiqueta = codigo || (tipo === 'erro' ? 'Erro' : 'OK');
    toast.innerHTML = `<span class="toast__codigo">${escapar(etiqueta)}</span><span>${escapar(mensagem)}</span>`;
    toast.className = `toast toast--${tipo}`;
    toast.hidden = false;
    clearTimeout(timerToast);
    timerToast = setTimeout(() => { toast.hidden = true; }, tipo === 'erro' ? 6000 : 3000);
}

function avisarErro(erro) {
    avisar(erro.message, 'erro', erro.status ? String(erro.status) : null);
}

/* =========================================================================
 * Carregamento, atualização automática e conexão
 * ========================================================================= */

// Falha de rede (servidor fora do ar) ou erro 5xx: trata como sem conexão.
function ehFalhaDeConexao(erro) {
    return !(erro instanceof ApiError) || erro.status >= 500;
}

async function recarregar() {
    try {
        const [vagas, permanencias] = await Promise.all([api.listarVagas(), api.listarPermanencias()]);
        tela.vagas = (vagas || []).map(normalizarVaga).sort((a, b) => a.numero - b.numero);
        tela.permanencias = (permanencias || [])
            .map(normalizarPermanencia)
            .sort((a, b) => new Date(b.entrada) - new Date(a.entrada));
        tela.offline = false;
        tela.ultimaAtualizacao = Date.now();

        // Seleção que deixou de existir (ex.: backend reiniciado) é descartada.
        if (tela.selecionada != null && !vagaSelecionada()) tela.selecionada = null;
        if (tela.historicoId != null && !tela.permanencias.some((p) => p.id === tela.historicoId)) tela.historicoId = null;
        if (tela.selecionada == null && tela.historicoId == null && tela.vagas.length) {
            tela.selecionada = tela.vagas[0].id;
        }
    } catch (erro) {
        if (!ehFalhaDeConexao(erro)) throw erro;
        tela.offline = true;
    } finally {
        tela.carregando = false;
        renderizar();
    }
}

// Executa uma ação na API, mostra o resultado e recarrega a tela.
// O botão que disparou a ação fica "em andamento" e ignora cliques repetidos.
async function executar(acao, mensagemSucesso, botao) {
    if (tela.ocupado) return null;
    tela.ocupado = true;
    document.body.setAttribute('aria-busy', 'true');
    if (botao) botao.setAttribute('aria-busy', 'true');
    try {
        const resultado = await acao();
        if (mensagemSucesso) avisar(typeof mensagemSucesso === 'function' ? mensagemSucesso(resultado) : mensagemSucesso);
        await recarregar();
        return resultado ?? true; // true = sucesso sem corpo na resposta
    } catch (erro) {
        if (ehFalhaDeConexao(erro)) {
            tela.offline = true;
            renderizar();
            avisar('Sem conexão com o servidor. Tente de novo.', 'erro');
        } else {
            avisarErro(erro);
        }
        return null;
    } finally {
        tela.ocupado = false;
        document.body.removeAttribute('aria-busy');
        if (botao && botao.isConnected) botao.removeAttribute('aria-busy');
    }
}

const INTERVALO_ATUALIZACAO = 30000;

function iniciarAtualizacaoAutomatica() {
    setInterval(() => {
        // Não atrapalha uma ação em andamento nem um modal aberto.
        if (tela.ocupado || modalAberto) return;
        recarregar();
    }, INTERVALO_ATUALIZACAO);
    // O texto "Atualizado há X s" anda sozinho.
    setInterval(renderizarAtualizacao, 5000);
}

/* =========================================================================
 * Ações
 * ========================================================================= */

async function criarVaga(evento) {
    evento.preventDefault();
    const campo = $('#numero-vaga');
    const erro = $('#erro-vaga');
    const numero = Number(campo.value);

    if (!campo.value || !Number.isInteger(numero) || numero < 1) {
        erro.innerHTML = `${ICONE_ERRO}Informe um número de vaga inteiro maior que zero.`;
        campo.setAttribute('aria-invalid', 'true');
        campo.focus();
        return;
    }
    erro.textContent = '';
    campo.removeAttribute('aria-invalid');

    // Número repetido é validado pela API (409).
    const criada = await executar(() => api.criarVaga(numero), `Vaga ${numero} criada`, evento.submitter);
    if (criada) {
        if (criada.id != null) {
            tela.selecionada = normalizarVaga(criada).id;
            tela.historicoId = null;
            tela.recibo = null;
        }
        campo.value = '';
        renderizar();
    }
}

function validarEntrada(formulario) {
    const campoPlaca = formulario.querySelector('[name="placa"]');
    const campoModelo = formulario.querySelector('[name="modelo"]');
    const erroPlaca = formulario.querySelector('[data-erro="placa"]');
    const erroModelo = formulario.querySelector('[data-erro="modelo"]');

    const placa = campoPlaca.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
    const modelo = campoModelo.value.trim();
    let valido = true;

    // Aceita placa antiga (ABC1234) e Mercosul (ABC1D23).
    if (!/^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$/.test(placa)) {
        erroPlaca.innerHTML = `${ICONE_ERRO}Placa inválida. Use o formato ABC1234 ou ABC1D23.`;
        campoPlaca.setAttribute('aria-invalid', 'true');
        campoPlaca.focus();
        valido = false;
    }
    if (!modelo) {
        erroModelo.innerHTML = `${ICONE_ERRO}Informe o modelo do veículo.`;
        campoModelo.setAttribute('aria-invalid', 'true');
        if (valido) campoModelo.focus();
        valido = false;
    }
    return valido ? { placa, modelo } : null;
}

async function registrarEntrada(formulario, botao) {
    const vaga = vagaSelecionada();
    const dados = validarEntrada(formulario);
    if (!vaga || !dados) return;
    await executar(
        () => api.registrarEntrada(vaga.id, dados.placa, dados.modelo),
        `Entrada de ${dados.placa} na vaga ${vaga.numero}`,
        botao,
    );
}

function pedirConfirmacaoSaida(permanenciaId, gatilho) {
    const p = tela.permanencias.find((x) => x.id === permanenciaId);
    if (!p) return;
    tela.saidaPendente = p;
    const entrada = comoData(p.entrada);
    $('#resumo-saida').innerHTML = `
        <div class="resumo-saida">
            <div class="veiculo">
                ${htmlPlaca(p.placa, true)}
                <p class="texto-suave">${escapar(p.modelo)} · vaga ${numeroDaVaga(p)}<br>
                Entrou às ${hora(p.entrada)}${entrada ? ` · há ${duracao(Date.now() - entrada)}` : ''}</p>
            </div>
            <div class="resumo-saida__total">
                <span class="texto-suave">${escapar(p.descricao || 'Tarifa')}</span>
                <strong>${moeda(valorExibido(p))}</strong>
            </div>
        </div>`;
    $('#d-saida').textContent = `A permanência será finalizada e a vaga ${numeroDaVaga(p)} ficará livre. `
        + 'Essa ação não pode ser desfeita.';
    abrirModal('#modal-saida', gatilho);
}

async function confirmarSaida() {
    const p = tela.saidaPendente;
    if (!p) return;
    const botao = $('#btn-confirmar-saida');
    const resposta = await executar(() => api.registrarSaida(p.id), null, botao);
    fecharModal();
    tela.saidaPendente = null;
    if (!resposta) return;

    // A API devolve a permanência finalizada; a lista recarregada é só reserva.
    tela.recibo = typeof resposta === 'object'
        ? normalizarPermanencia(resposta)
        : tela.permanencias.find((x) => x.id === p.id);
    tela.historicoId = null;
    avisar(`Saída registrada: ${moeda(valorExibido(tela.recibo))}`);
    renderizar();
}

/* =========================================================================
 * Renderização
 * ========================================================================= */

function renderizar() {
    renderizarTopo();
    renderizarContadores();
    renderizarPlanta();
    renderizarPainel();
    renderizarPermanencias();
    $('#banner-offline').hidden = !tela.offline;
    $('#conteudo').classList.toggle('conteudo--offline', tela.offline);
}

function renderizarTopo() {
    const selo = $('#selo');
    let classe = 'api';
    let texto = 'API conectada';
    if (tela.carregando) { classe = 'carregando'; texto = 'Conectando…'; }
    else if (tela.offline) { classe = 'offline'; texto = 'Sem conexão'; }
    else if (usandoMock) { classe = 'mock'; texto = 'Dados mockados'; }
    selo.className = `selo selo--${classe}`;
    $('#selo-texto').textContent = texto;

    // Semáforo: nenhuma livre = vermelho; menos da metade = amarelo; metade ou mais = verde.
    // Reservada não conta como livre.
    const total = tela.vagas.length;
    const livres = contar('LIVRE');
    let cor = '';
    let situacao = 'sem vagas cadastradas';
    if (!tela.carregando && total > 0) {
        if (livres === 0) { cor = 'vermelho'; situacao = 'lotado'; }
        else if (livres * 2 < total) { cor = 'amarelo'; situacao = 'poucas vagas'; }
        else { cor = 'verde'; situacao = 'vagas disponíveis'; }
    }
    const semaforo = $('#semaforo');
    semaforo.className = `semaforo${cor ? ` semaforo--${cor}` : ''}`;
    $('#semaforo-numero').textContent = tela.carregando ? '—' : `${livres}/${total}`;
    semaforo.setAttribute('aria-label', tela.carregando
        ? 'Carregando situação do pátio'
        : `Situação do pátio: ${situacao}, ${livres} de ${total} vagas livres`);

    renderizarAtualizacao();
}

function renderizarAtualizacao() {
    const alvo = $('#atualizado');
    const texto = $('#atualizado-texto');
    alvo.classList.toggle('atualizado--offline', tela.offline);
    alvo.classList.toggle('atualizado--carregando', tela.carregando);
    if (tela.carregando) { texto.textContent = 'Carregando…'; return; }
    if (!tela.ultimaAtualizacao) { texto.textContent = 'Sem dados'; return; }
    const segundos = Math.round((Date.now() - tela.ultimaAtualizacao) / 1000);
    const quando = segundos < 5 ? 'agora' : segundos < 60 ? `há ${segundos} s` : `há ${Math.floor(segundos / 60)} min`;
    texto.textContent = tela.offline ? `Última atualização ${quando}` : `Atualizado ${quando}`;
}

function renderizarContadores() {
    if (tela.carregando) { $('#contadores').innerHTML = ''; return; }
    $('#contadores').innerHTML = [
        ['LIVRE', 'livre', 'livres'],
        ['OCUPADA', 'ocupada', 'ocupadas'],
        ['RESERVADA', 'reservada', 'reservadas'],
    ].map(([estado, um, varios]) => `<span class="contador contador--${estado}">${plural(contar(estado), um, varios)}</span>`).join('');
}

function htmlVaga(v, fileira) {
    const ativa = permanenciaAtiva(v.id);
    const conhecido = ['LIVRE', 'OCUPADA', 'RESERVADA'].includes(v.estado);
    let icone = '';
    if (v.estado === 'OCUPADA') icone = iconeCarro(fileira);
    if (v.estado === 'RESERVADA') icone = ICONE_CONE;
    const textoRotulo = ativa ? ativa.placa : rotulo(v.estado).toLowerCase();
    return `
        <button type="button" class="vaga ${conhecido ? `vaga--${v.estado}` : 'vaga--desconhecido'}" data-vaga="${v.id}"
                aria-pressed="${v.id === tela.selecionada}"
                aria-label="Vaga ${v.numero}, ${escapar(rotulo(v.estado))}${ativa ? `, ${escapar(ativa.placa)}` : ''}">
            <span class="vaga__numero">${v.numero}</span>
            ${icone}
            <span class="vaga__rotulo">${escapar(textoRotulo)}</span>
        </button>`;
}

function renderizarPlanta() {
    const planta = $('#planta');
    if (tela.carregando) {
        planta.className = 'planta';
        const linha = '<div><span class="esqueleto"></span><span class="esqueleto"></span><span class="esqueleto"></span></div>';
        planta.innerHTML = `<div class="planta__esqueleto" role="status" aria-label="Carregando vagas">${linha}${linha}</div>`;
        return;
    }
    if (!tela.vagas.length) {
        planta.className = 'planta planta--vazia';
        planta.innerHTML = `
            <div class="planta__vazio">
                <div class="planta__vazio-icone" aria-hidden="true">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M9 17V7h4a3 3 0 0 1 0 6H9"/></svg>
                </div>
                <p>O pátio ainda está vazio</p>
                <p class="texto-suave">Digite o número da primeira vaga e clique em Nova vaga.</p>
            </div>`;
        return;
    }
    // Metade das vagas de cada lado do corredor.
    const meio = Math.ceil(tela.vagas.length / 2);
    const cima = tela.vagas.slice(0, meio).map((v) => htmlVaga(v, 'cima')).join('');
    const baixo = tela.vagas.slice(meio).map((v) => htmlVaga(v, 'baixo')).join('');
    planta.className = 'planta';
    planta.innerHTML = `
        <div class="planta__area" role="group" aria-label="Planta do estacionamento">
            <div class="fileira fileira--cima">${cima}</div>
            <div class="corredor" aria-hidden="true"></div>
            <div class="fileira fileira--baixo">${baixo}</div>
        </div>`;
}

/* ---------- Card lateral ---------- */

function htmlRecibo(r) {
    const entrada = comoData(r.entrada);
    const saida = comoData(r.saida);
    return `
        <div class="recibo">
            <div class="recibo__topo">
                <span class="recibo__titulo"><span class="recibo__icone" aria-hidden="true">${ICONE_CHECK}</span>Saída registrada</span>
                <button type="button" class="btn-icone" data-acao="fechar-recibo" aria-label="Fechar recibo">
                    <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 6l12 12M18 6 6 18"/></svg>
                </button>
            </div>
            <p class="recibo__meta"><strong class="mono">${escapar(r.placa)}</strong> · ${escapar(r.modelo)} · vaga ${numeroDaVaga(r)}<br>
                ${dataHora(r.entrada)} → ${hora(r.saida)}${entrada && saida ? ` · ${duracao(saida - entrada)} de permanência` : ''}</p>
            <div class="linhas-cobranca">${htmlLinhasCobranca(r)}</div>
            <div class="total-linha"><span>Total pago</span><strong>${moeda(valorExibido(r))}</strong></div>
        </div>`;
}

function htmlCabecalhoVaga(vaga) {
    return `
        <div class="painel__cabecalho">
            <div>
                <span class="painel__sobretitulo">Vaga selecionada</span>
                <h2 class="painel__titulo">Vaga ${vaga.numero}</h2>
            </div>
            <span class="chip-estado chip-estado--${escapar(vaga.estado)}">${escapar(rotulo(vaga.estado))}</span>
        </div>`;
}

function htmlCamposEntrada(compacto) {
    const classe = compacto ? 'campo campo--compacto' : 'campo';
    return `
        <label class="${classe}">Placa
            <input name="placa" maxlength="8" autocomplete="off" placeholder="ABC1D23">
        </label>
        <p class="erro-campo" data-erro="placa" aria-live="polite"></p>
        <label class="${classe}">Modelo
            <input name="modelo" maxlength="40" autocomplete="off" placeholder="Gol">
        </label>
        <p class="erro-campo" data-erro="modelo" aria-live="polite"></p>`;
}

function htmlVagaDisponivel(vaga) {
    // Os botões de estado aparecem só onde a transição faz sentido:
    // livre -> Reservar; reservada -> Liberar (cancelar reserva).
    const rodape = vaga.estado === 'RESERVADA'
        ? `<div class="rodape-reserva rodape-reserva--ativa">
                ${ICONE_CONE_PEQUENO}
                <span>Reserva ativa. O cliente chegou? Registre a entrada acima.</span>
                <button type="button" class="btn btn--vidro" data-acao="liberar">Liberar vaga</button>
           </div>`
        : `<div class="rodape-reserva">
                <span>Segurar a vaga para um cliente?</span>
                <button type="button" class="btn btn--reserva" data-acao="reservar">Reservar</button>
           </div>`;
    return `
        <form class="form-entrada" data-form="entrada" novalidate>
            <h3>Registrar entrada</h3>
            ${htmlCamposEntrada(false)}
            <button type="submit" class="btn btn--primario btn--grande">Registrar entrada na vaga ${vaga.numero}</button>
            ${rodape}
        </form>`;
}

function htmlVagaOcupada(vaga, p) {
    const entrada = comoData(p.entrada);
    const decorrido = entrada ? Date.now() - entrada : 0;
    // Próxima hora cobrada: a fração conta como hora cheia, com mínimo de 1h.
    const horasCobradas = Math.max(1, Math.ceil(decorrido / 3600000));
    const proximaHora = entrada ? new Date(entrada.getTime() + horasCobradas * 3600000) : null;

    const disponiveis = SERVICOS.filter((s) => !p.servicos.includes(s.tipo));
    const cartoes = [
        ...p.servicos.map((tipo) => `
            <div class="servico servico--adicionado">
                <span class="servico__nome">${ICONE_CHECK}${escapar(nomeServico(tipo))}</span>
                <span class="servico__info">Adicionado${servicoPorTipo(tipo) ? ` · ${moeda(servicoPorTipo(tipo).valor)}` : ''}</span>
            </div>`),
        ...disponiveis.map((s) => `
            <button type="button" class="servico servico--disponivel" data-acao="servico" data-tipo="${s.tipo}" data-permanencia="${p.id}">
                <span class="servico__nome">+ ${escapar(s.nome)}</span>
                <span class="servico__info">Adicionar · ${moeda(s.valor)}</span>
            </button>`),
    ].join('');

    return `
        <div class="painel__corpo">
            <div class="veiculo">
                ${htmlPlaca(p.placa)}
                <div>
                    <p class="veiculo__modelo">${escapar(p.modelo)}</p>
                    <p class="texto-suave">Entrou às ${hora(p.entrada)}${entrada ? ` · há ${duracao(decorrido)}` : ''}</p>
                </div>
            </div>
            <div class="servicos">
                <span class="texto-suave">Serviços extras</span>
                <div class="servicos__grade">${cartoes}</div>
                ${disponiveis.length ? '' : '<span class="texto-suave">Todos os serviços extras já foram adicionados.</span>'}
            </div>
            <div class="caixa-valor">
                <span class="caixa-valor__rotulo">Valor até agora</span>
                <span class="caixa-valor__valor">${moeda(valorExibido(p))}</span>
                <span class="caixa-valor__detalhe">${escapar(p.descricao || 'Tarifa')}${proximaHora ? ` · próxima hora às ${hora(proximaHora)}` : ''}</span>
            </div>
            <button type="button" class="btn btn--perigo btn--grande" data-acao="saida" data-permanencia="${p.id}">Registrar saída e cobrar</button>
            <details class="nova-entrada">
                <summary>Tentar nova entrada nesta vaga</summary>
                <form data-form="entrada" novalidate>
                    <div class="campos">${htmlCamposEntrada(true)}</div>
                    <button type="submit" class="btn btn--vidro">Registrar entrada na vaga ${vaga.numero}</button>
                </form>
            </details>
        </div>`;
}

function htmlHistorico(p) {
    const entrada = comoData(p.entrada);
    const saida = comoData(p.saida);
    const vaga = vagaPorId(p.vagaId);
    return `
        <div class="painel__corpo">
            <div class="painel__cabecalho">
                <div>
                    <span class="painel__sobretitulo">Histórico</span>
                    <h2 class="painel__titulo painel__titulo--menor">Permanência finalizada</h2>
                </div>
                <span class="chip-estado chip-estado--FINALIZADA">Finalizada</span>
            </div>
            <div class="veiculo">
                ${htmlPlaca(p.placa)}
                <div>
                    <p class="veiculo__modelo">${escapar(p.modelo)} · vaga ${numeroDaVaga(p)}</p>
                    <p class="texto-suave">${dataHora(p.entrada)} → ${hora(p.saida)}${entrada && saida ? ` · ${duracao(saida - entrada)}` : ''}</p>
                </div>
            </div>
            <div class="historico__caixa">
                <div class="linhas-cobranca">${htmlLinhasCobranca(p)}</div>
                <div class="total-linha"><span>Total pago</span><strong>${moeda(valorExibido(p))}</strong></div>
            </div>
            ${vaga ? `<button type="button" class="btn btn--vidro btn--grande" style="margin-top:auto" data-acao="ver-vaga" data-vaga="${vaga.id}">Ver a vaga ${vaga.numero} agora</button>` : ''}
        </div>`;
}

function htmlPainelCarregando() {
    return `
        <div class="painel__corpo" aria-hidden="true">
            <span class="esqueleto" style="width:40%;height:12px"></span>
            <span class="esqueleto" style="width:55%;height:28px"></span>
            <span class="esqueleto" style="height:56px;margin-top:12px"></span>
            <span class="esqueleto" style="height:56px"></span>
            <span class="esqueleto" style="height:54px;background:rgba(96,165,250,0.14)"></span>
            <span class="esqueleto" style="height:44px;margin-top:auto"></span>
        </div>`;
}

// Guarda o que o usuário estava digitando para a atualização automática não apagar.
function capturarFormulario() {
    const painel = $('#painel');
    const ativo = document.activeElement;
    const campos = {};
    painel.querySelectorAll('input[name]').forEach((campo) => {
        const onde = campo.closest('details') ? 'detalhe' : 'principal';
        campos[`${onde}:${campo.name}`] = campo.value;
    });
    return {
        campos,
        detalheAberto: painel.querySelector('details')?.open ?? false,
        foco: painel.contains(ativo) && ativo.name
            ? `${ativo.closest('details') ? 'detalhe' : 'principal'}:${ativo.name}`
            : null,
        rolagem: painel.scrollTop,
    };
}

function restaurarFormulario(estado, mesmaVaga) {
    const painel = $('#painel');
    if (!mesmaVaga) return;
    const detalhe = painel.querySelector('details');
    if (detalhe && estado.detalheAberto) detalhe.open = true;
    painel.querySelectorAll('input[name]').forEach((campo) => {
        const chave = `${campo.closest('details') ? 'detalhe' : 'principal'}:${campo.name}`;
        if (chave in estado.campos) campo.value = estado.campos[chave];
        if (chave === estado.foco) campo.focus();
    });
    painel.scrollTop = estado.rolagem;
}

let ultimoPainel = null;

function renderizarPainel() {
    const painel = $('#painel');
    const chave = `${tela.selecionada}|${tela.historicoId}`;
    const estado = capturarFormulario();

    if (tela.carregando) {
        painel.innerHTML = htmlPainelCarregando();
        return;
    }

    let html = tela.recibo ? htmlRecibo(tela.recibo) : '';
    const historico = tela.historicoId != null ? tela.permanencias.find((p) => p.id === tela.historicoId) : null;
    const vaga = vagaSelecionada();

    if (historico) {
        html += htmlHistorico(historico);
    } else if (!vaga) {
        html += `
            <div class="painel__vazio">
                <p>Nenhuma vaga selecionada</p>
                <p class="texto-suave">Os detalhes, a entrada e a saída de cada vaga aparecem aqui.</p>
            </div>`;
    } else {
        const ativa = permanenciaAtiva(vaga.id);
        html += htmlCabecalhoVaga(vaga);
        html += ativa ? htmlVagaOcupada(vaga, ativa) : htmlVagaDisponivel(vaga);
    }

    painel.innerHTML = html;
    restaurarFormulario(estado, chave === ultimoPainel);
    ultimoPainel = chave;
}

/* ---------- Lista de permanências ---------- */

function renderizarPermanencias() {
    const alvo = $('#permanencias');
    document.querySelectorAll('.filtro').forEach((b) => {
        b.setAttribute('aria-pressed', String(b.dataset.filtro === tela.filtro));
    });

    if (tela.carregando) {
        $('#qtd-permanencias').textContent = '';
        alvo.innerHTML = `<div class="lista-esqueleto" aria-hidden="true">${'<span class="esqueleto"></span>'.repeat(4)}</div>`;
        return;
    }

    const total = tela.permanencias.length;
    const lista = tela.permanencias.filter((p) => {
        if (tela.filtro === 'ativas') return p.ativa;
        if (tela.filtro === 'finalizadas') return !p.ativa;
        return true;
    });
    $('#qtd-permanencias').textContent = tela.filtro === 'todas'
        ? plural(total, 'registro', 'registros')
        : `${lista.length} de ${plural(total, 'registro', 'registros')}`;

    if (!lista.length) {
        const vazio = {
            todas: ['Nenhuma permanência ainda', 'Quando um veículo entrar, ele aparece aqui com a cobrança em tempo real.'],
            ativas: ['Nenhuma permanência ativa agora', 'Todos os veículos já saíram. As finalizadas continuam no histórico.'],
            finalizadas: ['Nenhuma permanência finalizada ainda', 'Assim que um veículo sair, o recibo fica guardado aqui.'],
        }[tela.filtro];
        alvo.innerHTML = `
            <div class="lista-vazia">
                <p>${vazio[0]}</p>
                <p class="texto-suave">${vazio[1]}</p>
                ${tela.filtro !== 'todas' && total ? '<button type="button" class="btn btn--vidro" data-acao="ver-todas">Ver todas</button>' : ''}
            </div>`;
        return;
    }

    alvo.innerHTML = `
        <table class="tabela">
            <thead>
                <tr>
                    <th scope="col">Veículo</th><th scope="col">Vaga</th><th scope="col">Entrada → saída</th>
                    <th scope="col">Cobrança</th><th scope="col" class="num">Total</th><th scope="col">Status</th>
                </tr>
            </thead>
            <tbody>
                ${lista.map((p) => `
                    <tr data-permanencia="${p.id}" tabindex="0" aria-selected="${p.id === tela.historicoId}"
                        aria-label="${escapar(p.placa)}, vaga ${numeroDaVaga(p)}, ${p.ativa ? 'ativa' : 'finalizada'}">
                        <td><strong class="mono">${escapar(p.placa)}</strong><span class="modelo">${escapar(p.modelo)}</span></td>
                        <td>${numeroDaVaga(p)}</td>
                        <td class="suave">${dataHora(p.entrada)} → ${p.saida ? hora(p.saida) : '—'}</td>
                        <td class="suave">${escapar(p.descricao || '—')}</td>
                        <td class="num">${moeda(valorExibido(p))}${p.ativa ? '<span class="nota">parcial</span>' : ''}</td>
                        <td><span class="status status--${p.ativa ? 'ativa' : 'fim'}">${p.ativa ? 'Ativa' : 'Finalizada'}</span></td>
                    </tr>`).join('')}
            </tbody>
        </table>`;
}

/* =========================================================================
 * Modais (preços e confirmação de saída)
 * ========================================================================= */

let modalAberto = null;
let gatilhoModal = null;

function abrirModal(seletor, gatilho) {
    const modal = $(seletor);
    modalAberto = modal;
    gatilhoModal = gatilho || document.activeElement;
    modal.hidden = false;
    const foco = modal.querySelector('#btn-confirmar-saida') ? modal.querySelector('[data-fechar-modal]') : modal.querySelector('.btn-icone, button');
    foco?.focus();
}

function fecharModal() {
    if (!modalAberto) return;
    modalAberto.hidden = true;
    modalAberto = null;
    tela.saidaPendente = null;
    if (gatilhoModal && gatilhoModal.isConnected) gatilhoModal.focus();
    gatilhoModal = null;
}

function prepararModalPrecos() {
    $('#lista-precos').innerHTML = [
        { nome: 'Tarifa por hora', detalhe: 'Mínimo de 1h; fração conta como hora cheia', valor: TARIFA_HORA },
        ...SERVICOS.map((s) => ({ nome: s.nome, detalhe: 'Valor fixo, por permanência', valor: s.valor })),
    ].map((item) => `
        <div class="preco">
            <div><p class="preco__nome">${escapar(item.nome)}</p><p class="texto-suave">${escapar(item.detalhe)}</p></div>
            <span class="preco__valor">${moeda(item.valor)}</span>
        </div>`).join('');
    const lavagem = servicoPorTipo('LAVAGEM');
    $('#exemplo-preco').innerHTML = lavagem
        ? `Exemplo: 2h30 com lavagem = 3h × ${moeda(TARIFA_HORA)} + ${moeda(lavagem.valor)} = <strong>${moeda(3 * TARIFA_HORA + lavagem.valor)}</strong>`
        : '';
}

/* =========================================================================
 * Eventos
 * ========================================================================= */

function selecionarVaga(id) {
    tela.selecionada = id;
    tela.historicoId = null;
    tela.recibo = null;
    renderizar();
}

function ligarEventos() {
    $('#form-vaga').addEventListener('submit', criarVaga);
    $('#numero-vaga').addEventListener('input', (e) => {
        $('#erro-vaga').textContent = '';
        e.target.removeAttribute('aria-invalid');
    });

    $('#planta').addEventListener('click', (e) => {
        const botao = e.target.closest('[data-vaga]');
        if (botao) selecionarVaga(Number(botao.dataset.vaga));
    });

    const painel = $('#painel');
    painel.addEventListener('click', (e) => {
        const botao = e.target.closest('[data-acao]');
        if (!botao) return;
        const vaga = vagaSelecionada();

        switch (botao.dataset.acao) {
            case 'reservar':
                executar(() => api.reservar(vaga.id), `Vaga ${vaga.numero} reservada`, botao);
                break;
            case 'liberar':
                executar(() => api.liberar(vaga.id), `Vaga ${vaga.numero} liberada`, botao);
                break;
            case 'servico': {
                const tipo = botao.dataset.tipo;
                executar(
                    () => api.adicionarServico(Number(botao.dataset.permanencia), tipo),
                    `${nomeServico(tipo)} adicionado`,
                    botao,
                );
                break;
            }
            case 'saida':
                pedirConfirmacaoSaida(Number(botao.dataset.permanencia), botao);
                break;
            case 'fechar-recibo':
                tela.recibo = null;
                renderizarPainel();
                break;
            case 'ver-vaga':
                selecionarVaga(Number(botao.dataset.vaga));
                break;
            default:
                break;
        }
    });

    painel.addEventListener('submit', (e) => {
        const form = e.target.closest('[data-form="entrada"]');
        if (!form) return;
        e.preventDefault();
        registrarEntrada(form, e.submitter);
    });

    painel.addEventListener('input', (e) => {
        if (!e.target.name) return;
        e.target.removeAttribute('aria-invalid');
        const erro = e.target.closest('form')?.querySelector(`[data-erro="${e.target.name}"]`);
        if (erro) erro.textContent = '';
    });

    const lista = $('#permanencias');
    const abrirLinha = (linha) => {
        const p = tela.permanencias.find((x) => x.id === Number(linha.dataset.permanencia));
        if (!p) return;
        if (p.ativa) {
            selecionarVaga(p.vagaId);
        } else {
            // Finalizada: reabre o recibo dela no card.
            tela.historicoId = p.id;
            tela.selecionada = null;
            tela.recibo = null;
            renderizar();
        }
    };
    lista.addEventListener('click', (e) => {
        if (e.target.closest('[data-acao="ver-todas"]')) {
            tela.filtro = 'todas';
            renderizarPermanencias();
            return;
        }
        const linha = e.target.closest('tr[data-permanencia]');
        if (linha) abrirLinha(linha);
    });
    lista.addEventListener('keydown', (e) => {
        const linha = e.target.closest('tr[data-permanencia]');
        if (linha && (e.key === 'Enter' || e.key === ' ')) {
            e.preventDefault();
            abrirLinha(linha);
        }
    });

    document.querySelectorAll('.filtro').forEach((botao) => {
        botao.addEventListener('click', () => {
            tela.filtro = botao.dataset.filtro;
            renderizarPermanencias();
        });
    });

    $('#btn-atualizar').addEventListener('click', (e) => executar(() => Promise.resolve(), null, e.currentTarget));
    $('#btn-reconectar').addEventListener('click', (e) => executar(() => Promise.resolve(), null, e.currentTarget));

    $('#btn-precos').addEventListener('click', (e) => abrirModal('#modal-precos', e.currentTarget));
    $('#btn-confirmar-saida').addEventListener('click', confirmarSaida);

    document.querySelectorAll('.modal').forEach((modal) => {
        modal.addEventListener('click', (e) => {
            // Fecha ao clicar fora da caixa ou num botão de fechar.
            if (e.target === modal || e.target.closest('[data-fechar-modal]')) {
                if (!tela.ocupado) fecharModal();
            }
        });
    });

    document.addEventListener('keydown', (e) => {
        if (!modalAberto) return;
        if (e.key === 'Escape' && !tela.ocupado) {
            fecharModal();
            return;
        }
        // Mantém o foco do teclado dentro do modal aberto.
        if (e.key === 'Tab') {
            const focaveis = [...modalAberto.querySelectorAll('button:not([disabled])')];
            if (!focaveis.length) return;
            const primeiro = focaveis[0];
            const ultimo = focaveis[focaveis.length - 1];
            if (e.shiftKey && document.activeElement === primeiro) { e.preventDefault(); ultimo.focus(); }
            else if (!e.shiftKey && document.activeElement === ultimo) { e.preventDefault(); primeiro.focus(); }
        }
    });
}

/* =========================================================================
 * Inicialização
 * ========================================================================= */

async function detectarMock() {
    if (new URLSearchParams(location.search).has('mock') || CONFIG.modo === 'mock') return true;
    if (CONFIG.modo === 'api') return false;
    try {
        const resposta = await fetch('/api/vagas', { method: 'GET' });
        return !resposta.ok;
    } catch {
        return true;
    }
}

async function iniciar() {
    ligarEventos();
    prepararModalPrecos();
    renderizar(); // esqueletos enquanto carrega

    usandoMock = await detectarMock();
    api = usandoMock ? criarApiMock() : apiReal;

    try {
        await recarregar();
    } catch (erro) {
        avisarErro(erro);
    }
    iniciarAtualizacaoAutomatica();
}

document.addEventListener('DOMContentLoaded', iniciar);