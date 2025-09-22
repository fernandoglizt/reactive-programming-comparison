# Estudo Comparativo de Ferramentas de Programação Reativa

**⚠️ Atenção: Este projeto está em andamento (Work in Progress).**

## Sobre o Projeto

Este repositório contém o código-fonte e os materiais de apoio para um estudo prático e comparativo entre diferentes ferramentas e linguagens de programação reativa.

O objetivo é analisar e comparar o desempenho, o uso de recursos (CPU e memória) e a complexidade de desenvolvimento de microserviços equivalentes, implementados com cada uma das tecnologias listadas abaixo. Conforme descrito no artigo de referência, cada serviço foi submetido a testes de carga para avaliar métricas como throughput e latência sob alta concorrência.

## Tecnologias Analisadas

As seguintes soluções foram implementadas e comparadas:

* **Java**: Project Reactor
* **Go**: RxGo
* **Kotlin**: Coroutines e Flow
* **Python**: RxPY

## Ambiente de Execução

Os experimentos e testes de carga foram executados na Google Cloud Platform (GCP), utilizando o Cloud Run para hospedar cada microserviço de forma independente e escalável.

## Estrutura do Repositório

* Instruções para build e execução local via Docker.
* Roteiro para implantação no Cloud Run.
* Scripts para geração de carga e coleta de métricas.

*(WIP: A estrutura detalhada e as instruções completas serão adicionadas em breve.)*
