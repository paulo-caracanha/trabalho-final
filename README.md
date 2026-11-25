# API de produtos e pedidos

O projeto começou com um CRUD de produtos em Java e Spring Boot. Depois foram adicionados o AppLogger e a consulta de CEP com Adapter e Proxy. Nesta etapa foi incluído um fluxo de pedidos, com desconto, confirmação, entrega, cancelamento e controle de estoque.

Cada pedido contém um produto e a quantidade comprada. O foco desta entrega é aplicar Strategy e State em regras do sistema.

## Como executar

É necessário ter JDK 21 e Maven 3.9 instalados. Na pasta em que está o `pom.xml`, execute:

```bash
mvn clean verify
mvn spring-boot:run
```

A aplicação fica disponível em `http://localhost:8080`. Para encerrar, use `Ctrl+C`.

O banco H2 fica em memória. Ao reiniciar a aplicação, os produtos e os pedidos são apagados. O arquivo `app.log` é gerado na pasta de execução e registra as operações.

Também é possível gerar e executar o JAR:

```bash
mvn clean package
java -jar target/produtos-api-1.0.0.jar
```

## Regras dos pedidos

- O produto precisa existir e a quantidade pedida deve ser um número inteiro maior que zero.
- O pedido começa como `ABERTO`. Criar o pedido ainda não reserva nem retira unidades do estoque.
- Na confirmação, o estoque é conferido novamente. Se houver unidades suficientes, elas são retiradas e o pedido passa para `CONFIRMADO`.
- Sem estoque suficiente, a operação é desfeita e o pedido continua aberto.
- Cancelar um pedido aberto não altera o estoque. Cancelar um pedido confirmado devolve as unidades.
- Somente um pedido confirmado pode ser entregue.
- Pedidos cancelados ou entregues não aceitam novas mudanças de estado.
- O nome e o preço do produto ficam registrados no pedido. Alterar ou excluir o cadastro depois não muda esse histórico.
- Um produto com pedido confirmado só pode ser excluído depois que o pedido for entregue ou cancelado.
- A confirmação e o cancelamento usam transações e bloqueio dos registros no banco, para evitar baixa ou devolução duplicada em requisições simultâneas.

### Descontos disponíveis

O campo `tipoDesconto` escolhe uma regra. Se ele não for informado, o pedido usa `SEM_DESCONTO`.

| Tipo | Regra |
| --- | --- |
| `SEM_DESCONTO` | Não aplica desconto. |
| `ATACADO` | Aplica 10% quando o pedido tem pelo menos 10 unidades. |
| `COMPRA_MAIOR` | Aplica 5% quando o subtotal é de pelo menos R$ 500,00. |

Os descontos não se acumulam. Escolher uma regra sem atender à condição retorna erro. Os valores do pedido são calculados com `BigDecimal` e arredondados para duas casas decimais.

## Padrões utilizados

### Strategy

A interface `DescontoStrategy` define o cálculo do desconto. As classes `SemDesconto`, `DescontoAtacado` e `DescontoCompraMaior` implementam as regras.

A `CalculadoraDesconto` recebe essas estratégias e procura a escolhida no pedido. O `PedidoService` usa a calculadora, sem precisar conhecer a fórmula de cada desconto.

Esse padrão foi escolhido porque as regras de desconto podem variar. Para adicionar outra, basta criar uma classe que implemente a interface e registrá-la como componente, sem alterar o cálculo dentro do Service.

### State

A interface `EstadoPedido` define as operações de confirmar, cancelar e entregar. As classes `PedidoAberto`, `PedidoConfirmado`, `PedidoCancelado` e `PedidoEntregue` representam os estados.

O `Pedido` delega a operação para o objeto correspondente ao seu estado. O enum `StatusPedido` guarda a associação entre o nome salvo no banco e a implementação. As classes de cancelado e entregue herdam os bloqueios da interface, porque esses estados não permitem novas operações.

O State evita espalhar condições de status pelo Controller. Por exemplo, entregar um pedido aberto ou confirmar um pedido cancelado resulta em erro. A movimentação do estoque continua no `PedidoService`, dentro da mesma transação.

| Estado atual | Operações permitidas |
| --- | --- |
| `ABERTO` | Confirmar ou cancelar. |
| `CONFIRMADO` | Entregar ou cancelar. |
| `ENTREGUE` | Nenhuma. |
| `CANCELADO` | Nenhuma. |

### Singleton, Adapter e Proxy

O `AppLogger` mantém construtor privado e uma única instância obtida por `getInstance()`. O `AppLoggerComponent` é injetado nos componentes do Spring e encaminha os registros para essa instância. Assim, o Singleton continua participando das operações de produtos, pedidos e CEP.

Na versão clássica, a própria classe garante a instância única. No componente, o Spring controla a instância injetada. O Singleton clássico também pode ser usado fora do Spring; neste projeto, o componente permite continuar usando injeção de dependência sem duplicar a escrita do log.

O Adapter continua nas classes `ViaCepAdapter` e `BrasilApiAdapter`, que convertem respostas diferentes para o mesmo objeto `Endereco`.

O `ConsultaCepProxy` valida o formato do CEP e guarda consultas bem-sucedidas em cache. Na repetição do mesmo CEP, ele reutiliza o resultado sem chamar o provedor novamente.

Para trocar o provedor, altere `cep.provedor` em `src/main/resources/application.properties` para `viacep` ou `brasilapi`. A consulta externa precisa de internet.

## Organização e SOLID

Os Controllers recebem as requisições e chamam os Services. O `PedidoService` coordena as operações e o estoque, as estratégias calculam descontos e os estados controlam as transições. Cada parte tem uma responsabilidade definida.

As estratégias seguem o mesmo contrato e podem ser trocadas. Uma nova regra de desconto não exige alterar as anteriores. As interfaces são pequenas e as dependências são recebidas pelo construtor. Isso mantém as regras fora dos Controllers e facilita testar cada parte.

| Princípio | Aplicação no projeto |
| --- | --- |
| SRP | Desconto, estado, persistência e requisições ficam em classes com responsabilidades diferentes. |
| OCP | Uma nova estratégia de desconto pode ser adicionada sem alterar o `PedidoService` ou a calculadora. |
| LSP | As estratégias atendem ao contrato de calcular um desconto ou rejeitar uma compra que não atende à regra escolhida. Os estados seguem o contrato de permitir ou rejeitar cada operação. |
| ISP | `DescontoStrategy`, `EstadoPedido` e `ConsultaCep` definem apenas as operações necessárias para cada função. |
| DIP | A calculadora trabalha com `DescontoStrategy` e a consulta com `ConsultaCep`. Os Services recebem suas dependências pelo construtor. |

Os pacotes principais são `controller`, `service`, `repository`, `model`, `dto`, `exception`, `pedido.desconto`, `pedido.estado`, `logger` e `cep`.

## Rotas

| Método | Rota | Operação |
| --- | --- | --- |
| POST | `/produtos` | Cadastrar produto. |
| GET | `/produtos` | Listar produtos. |
| GET | `/produtos/{id}` | Buscar produto. |
| PUT | `/produtos/{id}` | Atualizar produto. |
| DELETE | `/produtos/{id}` | Excluir produto, respeitando os pedidos confirmados. |
| POST | `/pedidos` | Criar pedido. |
| GET | `/pedidos` | Listar pedidos. |
| GET | `/pedidos/{id}` | Buscar pedido. |
| POST | `/pedidos/{id}/confirmar` | Confirmar e baixar estoque. |
| POST | `/pedidos/{id}/cancelar` | Cancelar e devolver estoque quando necessário. |
| POST | `/pedidos/{id}/entregar` | Finalizar a entrega. |
| GET | `/cep/{cep}` | Consultar um CEP com oito dígitos, sem hífen. |

Cadastros retornam `201`, consultas e transições retornam `200` e a exclusão de produto retorna `204`. Dados inválidos retornam `400`, registros inexistentes retornam `404` e violações das regras de negócio retornam `409`.

## Exemplo para testar

No Postman, importe `Produtos_API.postman_collection.json`. As requisições de cadastro guardam os IDs retornados para as operações seguintes. A coleção usa `http://localhost:8080` como endereço base. Execute as pastas na ordem em que aparecem.

Também é possível fazer as chamadas manualmente no Postman ou no Insomnia. Nas requisições com corpo, selecione JSON.

Primeiro, faça `POST /produtos`:

```json
{
  "nome": "Teclado",
  "descricao": "Teclado USB",
  "preco": 100.00,
  "quantidade": 20
}
```

Use o ID retornado em `POST /pedidos`:

```json
{
  "produtoId": 1,
  "quantidade": 10,
  "tipoDesconto": "ATACADO"
}
```

Nesse caso, o subtotal é R$ 1.000,00, o desconto é R$ 100,00 e o total é R$ 900,00. O estoque ainda continua em 20.

Use o ID do pedido em `POST /pedidos/1/confirmar`, sem corpo. O estoque passa para 10. Repetir a confirmação retorna `409` e não retira mais unidades.

Depois, faça `POST /pedidos/1/cancelar`, também sem corpo. O estoque volta para 20. Um segundo cancelamento retorna `409`.

Para testar a entrega, crie outro pedido, confirme e chame `POST /pedidos/{id}/entregar`. Depois da entrega, tentar cancelar retorna `409`.

Para testar o CEP, use `GET /cep/01001000`. Repita a chamada e confira no `app.log` o uso do cache.

## Testes

O comando `mvn clean verify` executa os testes e gera o JAR. Os testes verificam o CRUD de produtos, as regras de desconto, os estados dos pedidos, a baixa e a devolução de estoque, os acessos simultâneos, o Singleton, os Adapters e o cache do Proxy.

As respostas dos provedores de CEP são simuladas nos testes para que eles não dependam da disponibilidade de serviços externos.
