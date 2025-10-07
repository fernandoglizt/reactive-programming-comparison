# Estudo Comparativo de Ferramentas de Programação Reativa

**⚠️ Atenção: Este projeto está em andamento (Work in Progress).**

## Sobre o Projeto

Este repositório contém o código-fonte e os materiais de apoio para um estudo prático e comparativo entre diferentes ferramentas e linguagens de programação reativa.

O objetivo é analisar e comparar o desempenho, o uso de recursos (CPU e memória) e a complexidade de desenvolvimento de microserviços equivalentes, implementados com cada uma das tecnologias listadas abaixo. Conforme descrito no artigo de referência, cada serviço é submetido a testes de carga para avaliar métricas como throughput e latência sob alta concorrência.

## Tecnologias Analisadas

- **Java**: Project Reactor (Spring WebFlux)
- **Kotlin**: Coroutines + Flow (Spring WebFlux)
- **Go**: RxGo
- **Python**: RxPY

## Ambiente de Execução

Os experimentos e testes de carga são executados localmente com Docker Compose (para desenvolvimento) e na Google Cloud Platform (GCP), utilizando o **Cloud Run** para hospedar cada microserviço de forma independente e escalável.

> **Nota:** As instruções completas de build/deploy serão finalizadas ao término do estudo (WIP).

## Estrutura do Repositório (WIP)

- Instruções para build e execução local via Docker.
- Roteiro para implantação no Cloud Run.
- Scripts para geração de carga e coleta de métricas.

---

## ✅ Regras de Paridade (Optimization Parity Checklist)

Para garantir um comparativo **justo** entre as linguagens/tecnologias, adotamos um conjunto de regras transversais. **Qualquer otimização aplicada a uma tecnologia precisa ter equivalente nas demais**.

### 1) Contrato e Semântica do Pipeline

- [ ] Mesmo **pipeline lógico** em todos os serviços: `range → map → filter → batch (lotes) → chamadas HTTP externas → reduce/fold (métricas)`.
- [ ] **Batch real** em todas as linguagens:
  - Reactor: `buffer(batch)` → listas de tamanho `batch`;
  - Kotlin Flow: operador `chunkedFlow(batch)` customizado (não confundir com `buffer(n)` de canal);
  - Go/Python: agregação explícita antes de disparar chamadas.
- [ ] **Sem desserializar o corpo** do downstream quando não é usado:
  - Validar apenas `status 2xx` e **descartar** o corpo (ex.: `exchangeToMono(...).thenReturn(status)` / `awaitExchange { ...; releaseBody() }` / `io.Copy(io.Discard, resp.Body)` / `resp.release()`).
- [ ] **Retries equivalentes**:
  - Mesma contagem por falha (`RETRY_ATTEMPTS`) e **mesmo backoff** (ex.: exponencial com limite de 300 ms);
  - Mesma política para timeout/5xx.

### 2) Cliente HTTP e Pool de Conexões

- [ ] **Keep-alive** habilitado e **limite de conexões por host** equivalente.
- [ ] **Timeouts equivalentes** em todas as stacks:
  - `CONNECT_TIMEOUT`, `RESPONSE/READ_TIMEOUT` e `WRITE_TIMEOUT`;
  - Em Netty (WebFlux): `ReadTimeoutHandler` e `WriteTimeoutHandler` adicionados no pipeline.
- [ ] **Descartar corpo** para reduzir overhead (ver item anterior).
- [ ] Cabeçalhos e comportamento idênticos (ex.: sem compressão seletiva em apenas uma linguagem).

### 3) Concorrência e Execução

- [ ] **Controle de concorrência em dois níveis (quando pertinente)**:
  - **Por lote** (quantos lotes avançam simultaneamente) e **por item** (quantas chamadas simultâneas por lote);
  - Variáveis padronizadas:
    - `BATCH_CONCURRENCY` (default: 4)
    - `ITEM_CONCURRENCY` (default: 64)
- [ ] **Limite global de inflight** equivalente entre serviços.
- [ ] **Trabalho de CPU leve** (map/filter) **fora do event-loop**:
  - Kotlin: `flowOn(Dispatchers.Default)` para etapas CPU leves;
  - Reactor: manter no event-loop; só usar `boundedElastic` se simular I/O bloqueante local.
- [ ] Evitar **materializar coleções grandes**:
  - Preferir `reduce/fold` para contadores em vez de `collectList/toList`.

### 4) Ambiente e Recursos

- [ ] **Mesmos limites** de CPU e memória por contêiner (ex.: 1 vCPU, 512 MiB).
- [ ] **Mesmo conjunto de CPUs** (cpuset) quando local, para isolar ruído.
- [ ] **Warm-up** padronizado (ex.: 30–60 s) antes da medição para estabilizar JIT/GC/loop.
- [ ] **Nível de logs mínimo** durante os testes (apenas resumo por requisição/etapa).

### 5) Carga e Metodologia

- [ ] Workload **fechado por taxa (RPS)** além de testes por VUs, para mitigar “coordinated omission”.
- [ ] **Cenários base** padronizados:
  - `io_delay_ms ∈ {10, 50, 200}` (baixa, média e alta latência do serviço externo);
  - `batch ∈ {50, 100, 500}`;
  - Rampas de taxa/concorrência idênticas entre serviços.
- [ ] **Jitter** controlado no serviço externo (`slow-io`) para variação realista e comparável.

### 6) Métricas e Coleta

- [ ] Métricas por cenário:
  - **Throughput efetivo** (req/s e eventos/s);
  - **Latências** p50/p95/p99;
  - **Erros** (timeouts/5xx);
  - **CPU% e RSS** médios/máximos por serviço.
- [ ] **2–3 repetições** por cenário; reportar **média** e **desvio**.
- [ ] Outputs dos clientes de carga e logs dos serviços salvos em arquivos (JSON/CSV/Parquet) para reprodutibilidade.

---

## 🔧 Parâmetros e Variáveis de Ambiente (padrão)

> Cada serviço expõe variáveis equivalentes. Ajuste por cenário, mantendo paridade.

| Variável | Descrição | Default |
|---|---|---|
| `PORT` | Porta do serviço | por projeto |
| `MAX_COUNT` | Limite de itens por requisição | `200000` |
| `BATCH_CONCURRENCY` | Lotes processados em paralelo | `4` |
| `ITEM_CONCURRENCY` | Itens (chamadas externas) por lote em paralelo | `64` |
| `DOWNSTREAM_TIMEOUT_MS` | Timeout por requisição ao serviço externo | `2000` |
| `RETRY_ATTEMPTS` | Tentativas adicionais em falha | `1` |
| `LOG_LEVEL` | Nível de log | `INFO` |

**Payload padrão (`POST /process`):**
```json
{
  "count": 10000,
  "batch": 100,
  "io_delay_ms": 50,
  "downstream_url": "http://slow-io:8080/slow"
}
```

---

## 🧪 Cenários de Teste Sugeridos

1. **Cenário Principal (baseline)**
   - `io_delay_ms=50`, `batch=100`
   - Rampas de taxa: 200 → 400 → 800 rps (2–3 min por degrau)
   - Warm-up: 60 s

2. **Variação de Latência**
   - `io_delay_ms ∈ {10, 200}`
   - Demais parâmetros iguais ao baseline

3. **Variação de Batch**
   - `batch ∈ {50, 500}`
   - `io_delay_ms=50`

> Em todos os cenários: repetir 2–3 vezes, coletar p50/p95/p99, throughput, erros, CPU, RSS.

---

## 🔁 Reprodutibilidade

- **Versões congeladas** no apêndice (WIP): JDK/Temurin, Kotlin, Spring Boot, Reactor, RxGo, Go, Python, RxPY, aiohttp, k6/wrk.
- **Dockerfiles** e **compose** com limites declarados de CPU/memória.
- **Scripts** de carga com parâmetros via ENV (WIP) e export de resultados em JSON.
- **ETL** simples (WIP) para consolidar resultados em CSV/Parquet e gerar gráficos.

---

## ⚠️ Observações Importantes

- As otimizações implementadas seguem o **Optimization Parity Checklist**. Caso alguma tecnologia receba uma otimização **sem equivalente** nas demais, essa alteração deve ser:
  1) Revertida, **ou**
  2) Replicada nas outras stacks, **ou**
  3) Explicitamente documentada como **fora do budget** e **excluída** da comparação principal.

- O **serviço `slow-io`** é a única dependência externa usada para simular I/O bloqueante com latência controlada. O teste busca comparar **o manejo de espera/concorrência** de cada stack, e não a eficiência de parsing/serialização de payloads.

---

## Roadmap (WIP)

- [ ] Consolidar scripts de carga (k6/wrk) e ETL de métricas.
- [ ] Publicar guias de execução local e no Cloud Run.
- [ ] Incluir gráficos comparativos e relatório final.
- [ ] Adicionar apêndice de versões/flags/commits usados.

---