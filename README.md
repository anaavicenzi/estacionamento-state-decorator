# Estacionamento — State e Decorator

## Descrição do sistema

(a preencher)

## Tecnologias e pré-requisitos

- JDK 17
- Maven 3.9+
- Java 17
- Spring Boot 3.3.5

## Como executar

```bash
mvn spring-boot:run
```

Após iniciar a aplicação, acesse: [http://localhost:8080](http://localhost:8080).

## Exemplos de uso

(a preencher)

## Patterns utilizados

### State

**Problema resolvido:** uma vaga pode assumir diferentes estados, e cada estado
possui regras próprias para as operações de ocupar, liberar e reservar. O padrão
State evita concentrar essas regras em condicionais `if/else` ou `switch` na
classe `Vaga`, mantendo cada comportamento no estado correspondente. Assim,
novos estados podem ser adicionados criando uma nova classe que implemente
`EstadoVaga`, com alterações mínimas nas classes existentes.

**Classes envolvidas:**

- `Vaga`: contexto do padrão. Mantém a referência para o estado atual e delega
  as operações de ocupar, liberar e reservar para esse estado.
- `EstadoVaga`: estado abstrato representado por uma interface. Define o
  contrato comum para as transições e para a identificação do estado.
- `VagaLivre`: estado concreto de uma vaga disponível.
- `VagaOcupada`: estado concreto de uma vaga atualmente ocupada.
- `VagaReservada`: estado concreto de uma vaga reservada.
- `TransicaoInvalidaException`: exceção lançada quando uma operação não é
  permitida no estado atual.

```mermaid
stateDiagram-v2
    [*] --> LIVRE

    LIVRE --> OCUPADA: ocupar
    LIVRE --> RESERVADA: reservar

    OCUPADA --> LIVRE: liberar

    RESERVADA --> OCUPADA: ocupar
    RESERVADA --> LIVRE: liberar
```

### Decorator

**Problema resolvido:** durante a permanência, o cliente pode contratar serviços
extras (lavagem, manobrista) em qualquer combinação e ordem. Criar uma subclasse
para cada combinação (`TarifaComLavagem`, `TarifaComLavagemEManobrista`, ...)
causaria uma explosão de classes, e usar flags booleanas com `if` em uma única
classe obrigaria a alterá-la a cada novo serviço. Com o Decorator, cada extra é
uma classe pequena que "embrulha" outro `Servico` e acrescenta sua descrição e
seu custo. As combinações são montadas em tempo de execução, e um novo serviço
(por exemplo, `RecargaEletrica`) exige apenas uma nova subclasse de
`ServicoDecorator`, sem alterar as classes existentes.

**Classes envolvidas:**

- `Servico`: componente (interface). Define `descricao()` e `custo()`.
- `TarifaBase`: componente concreto. Cobra valor por hora × horas
  (R$ 10,00/h).
- `ServicoDecorator`: decorator abstrato. Implementa `Servico` e guarda uma
  referência ao `Servico` decorado, repassando as chamadas a ele.
- `Lavagem`: decorator concreto, acrescenta R$ 30,00.
- `Manobrista`: decorator concreto, acrescenta R$ 20,00.

```mermaid
classDiagram
    class Servico {
        <<interface>>
        +descricao() String
        +custo() double
    }
    class TarifaBase {
        -int horas
        +descricao() String
        +custo() double
    }
    class ServicoDecorator {
        <<abstract>>
        #Servico servico
        +descricao() String
        +custo() double
    }
    Servico <|.. TarifaBase
    Servico <|.. ServicoDecorator
    ServicoDecorator o--> Servico : decora
    ServicoDecorator <|-- Lavagem
    ServicoDecorator <|-- Manobrista
```

**Exemplo de composição:**

```java
Servico servico = new Manobrista(new Lavagem(new TarifaBase(2)));

servico.descricao(); // "Tarifa 2h + Lavagem + Manobrista"
servico.custo();     // 20.0 + 30.0 + 20.0 = 70.0
```

Cada chamada percorre a cadeia de fora para dentro: `Manobrista` pede a
descrição/custo à `Lavagem`, que pede à `TarifaBase`; na volta, cada camada
soma a sua parte.

## Declaração de uso de IA

Modelo usado: Codex, baseado no GPT-5.

Partes em que foi utilizada: apoio à criação do esqueleto inicial do projeto,
implementação do padrão State, testes e documentação.

## Contribuição de cada integrante

| Integrante | GitHub | Contribuição |
| --- | --- | --- |
| Ana Laura | [anaavicenzi](https://github.com/anaavicenzi) | Implementação do padrão State (estados da Vaga e testes). |
| (a preencher) | (a preencher) | (a preencher) |
