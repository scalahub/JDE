# JSON dApp Environment (JDE) 

[![Build Status](https://www.travis-ci.com/scalahub/jde.svg?branch=main)](https://www.travis-ci.com/scalahub/jde)

#### What is JDE?

- A tool for developing the offchain part of an Ergo dApp. 
- Enables one to create a transaction by specifying a **script** in Json. 
- Can be used for many existing dApps, such as *Oracle-pools*, *Timestamping* and *Auctions*.

#### But what is it exactly?

- JDE allows us to define the input and data inputs of a transaction, along with some auxiliary boxes that are neither inputs nor data-inputs.  Auxiliary boxes are used only during computation.
- Each such box is defined using a **box definition**. A box definition is a sequence of **instructions** to filter boxes. Currently, we can filter using tokens, registers and nanoErgs. 
- Currently, boxes can be searched either by address or by box-id. With box-id, there can be at most one box, so we can unambiguously define a box. 
  However, when matching with address, there can be multiple boxes. These are handled as follows: 
  - The boxes are first filtered using the instructions. 
  - The resulting boxes are then sorted by value in decreasing order
  - The first box (if any) is selected as the matched box.
- An error is thrown if no boxes match a definition.

JDE is more verbose than, for example, Scala. As an example, the Scala code `c = a + b` must be written in JDE as
`{"name":"c", "first":"a", "op":"Add", "second":"b"}`.
That said, the only thing needed to use JDE is the ability to write Json (and possibly use a pen and paper).

JDE is used in [KioskWeb](https:/github.com/scalahub/KioskWeb) wallet within a tool called ["Tx Builder"](https://kioskweb.org/session/#kiosk.Wallet.txBuilder).

#### A complete example

Before describing further, it is instructive to see a complete example in action. 

- If you want to use the precompiled binary, skip to the next step. 

  Clone the repository and issue `sbt assembly` in the new folder to generate the jar.
  Usually, this will be the file:
  
  `target/scala-2.12/JDE-assembly-0.1.jar`
  
- Once you have the jar, issue the command to invoke JDE:

  `java -jar <jarFile> <jde_script_json>`

The following shows a sample transcript of running JDE:

```
java -jar target/scala-2.12/JDE-assembly-0.1.jar src/test/resources/timestamp.json 

{
  "dataInputBoxIds" : [ "506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7" ],
  "inputBoxIds" : [ "4c17e0e9f72122164aa3530453675625dc69941ed3da9de6b0a8659db929709a" ],
  "inputNanoErgs" : 1500000,
  "inputTokens" : [ [ "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 995 ] ],
  "outputs" : [ {
    "address" : "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi",
    "value" : 1500000,
    "registers" : [ ],
    "tokens" : [ [ "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 994 ] ]
  }, {
    "address" : "4MQyMKvMbnCJG3aJ",
    "value" : 2000000,
    "registers" : [ "0e20506dfb0a34d44f2baef77d99f9da03b1f122bdc4c7c31791a0c706e23f1207e7", "04c08633" ],
    "tokens" : [ [ "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea", 1 ] ]
  } ]
}

```

The program outputs an unsigned transaction. More specifically, it outputs an instance of the [`CompileResult`](src/main/scala/jde/compiler/package.scala#L15) class serialized in JSON. The inputs/data-inputs/outputs of the transaction are defined using the script contained in the file [timestamp.json](src/test/resources/timestamp.json). This script is used to timestamp a box using the dApp described [here](https://www.ergoforum.org/t/a-trustless-timestamping-service-for-boxes/432/9?u=scalahub) and has the following code:
```
{
  "constants": [
    {
      "name": "myBoxId",
      "type": "CollByte",
      "value": "ae57e4add0f181f5d1e8fd462969e4cc04f13b0da183676660d280ad0b64563f"
    },
    {
      "name": "emissionAddress",
      "type": "Address",
      "value": "2z93aPPTpVrZJHkQN54V7PatEfg3Ac1zKesFxUz8TGGZwPT4Rr5q6tBwsjEjounQU4KNZVqbFAUsCNipEKZmMdx2WTqFEyUURcZCW2CrSqKJ8YNtSVDGm7eHcrbPki9VRsyGpnpEQvirpz6GKZgghcTRDwyp1XtuXoG7XWPC4bT1U53LhiM3exE2iUDgDkme2e5hx9dMyBUi9TSNLNY1oPy2MjJ5seYmGuXCTRPLqrsi"
    },
    {
      "name": "timestampAddress",
      "type": "Address",
      "value": "4MQyMKvMbnCJG3aJ"
    },
    {
      "name": "myTokenId",
      "type": "CollByte",
      "value": "dbea46d988e86b1e60181b69936a3b927c3a4871aa6ed5258d3e4df155750bea"
    },
    {
      "name": "minTokenAmount",
      "type": "Long",
      "value": "2"
    },
    {
      "name": "one",
      "type": "Long",
      "value": "1"
    },
    {
      "name": "minStorageRent",
      "type": "Long",
      "value": "2000000"
    }
  ],
  "dataInputs": [
    {
      "id": {
        "value": "myBoxId"
      }
    }
  ],
  "inputs": [
    {
      "address": {
        "value": "emissionAddress"
      },
      "tokens": [
        {
          "index": 0,
          "id": {
            "value": "myTokenId"
          },
          "amount": {
            "name": "inputTokenAmount",
            "value": "minTokenAmount",
            "filter": "Ge"
          }
        }
      ],
      "nanoErgs": {
        "name": "inputNanoErgs"
      }
    }
  ],
  "outputs": [
    {
      "address": {
        "value": "emissionAddress"
      },
      "tokens": [
        {
          "index": 0,
          "id": {
            "value": "myTokenId"
          },
          "amount": {
            "value": "balanceTokenAmount"
          }
        }
      ],
      "nanoErgs": {
        "value": "inputNanoErgs"
      }
    },
    {
      "address": {
        "value": "timestampAddress"
      },
      "registers": [
        {
          "value": "myBoxId",
          "num": "R4",
          "type": "CollByte"
        },
        {
          "value": "HEIGHT",
          "num": "R5",
          "type": "Int"
        }
      ],
      "tokens": [
        {
          "index": 0,
          "id": {
            "value": "myTokenId"
          },
          "amount": {
            "value": "one"
          }
        }
      ],
      "nanoErgs": {
        "value": "minStorageRent"
      }
    }
  ],
  "binaryOps": [
    {
      "name": "balanceTokenAmount",
      "first": "inputTokenAmount",
      "op": "Sub",
      "second": "one"
    }
  ],
  "fee": 2000000
}
```
The following sections describe the syntax of the JSON scripting language in more detail. 

#### Protocol

Each JSON script internally maps to a Scala object, an instance of the [**Protocol**](src/main/scala/jde/compiler/model/package.scala#L13-L30) class.
A *Protocol* is the highest level of abstraction in JDE and can be thought of as a sequence of instructions for interacting with a dApp.
As can be seen from the source code, such an instance contains the following fields: 
- Optional sequence of `Constant` declarations, using which we can encode arbitrary values into the script.
- Optional sequence of box definitions, `auxInputs`. 
  These are for accessing arbitrary boxes without having to use them as data inputs (or inputs).
- Optional sequence of box definitions, `dataInputs`, defining data-inputs of the transaction.
- Mandatory sequence of box definitions, `inputs`, defining inputs of the transaction. 
- Mandatory sequence of box definitions, `outputs`, defining outputs of the transaction.
- Optional sequence of `Unary` operations, used to convert one object to another (example ErgoTree to Address).
- Optional sequence of `Binary` operations, used to compose two objects into a third object (of same types).
- Optional sequence of `Branch` instructions, used for run-time control-flow.
- Optional sequence of `PostCondition` instructions which must evaluate to true.

#### Declarations

The next level of abstraction is a [**Declaration**](src/main/scala/jde/compiler/Declaration.scala). 

We can classify declarations into three types:
- **Constants**: These specify initial values. Examples:  
  - `{"name":"myInt", "type":"int", "value":"123"}`
  - `{"name":"myAddress", "type":"address", "value":"9fcrXXaJgrGKC8iu98Y2spstDDxNccXSR9QjbfTvtuv7vJ3NQLk"}`
- **Box declarations**: These are used to define or search for boxes. There are four types: **Address**, **Id**, **Register**, and **Long** (see below).
- **Instructions**: These specify binary/unary operations, post-conditions and branches:
  - Binary Op: `{"name":"mySum", "first":"someValue", "op":"Add", "second":"otherValue"}`
  - Unary Op: `{"name":"myErgoTree", "from":"myGroupElement", "op":"ProveDlog"}`
  - Post-condition: `{"first":"someLong", "second":"otherLong", "op":"Ge"}`
  - Branch: `{"name":"result", "ifTrue":"someValue", "ifFalse":"otherValue", "condition": {"first":"someLong", "second":"otherLong", "op":"Ge"} }`
  
See [this page](src/main/scala/jde/compiler/model/package.scala) for the source code of all declarations and [this page](src/main/scala/jde/compiler/model/Enums.scala) for the enumerations used.
The schema can be inferred from the source. 

#### Box Declarations

There are four type of box declarations:
- **Address**: The address of the box.
- **Id**: Box Id or token Id.
- **Register**: Register contents.
- **Long**: NanoErgs or token quantity.

#### Names and Values
A box declaration can contain one or both of the following fields:
- A `name` field (i.e., the declaration defines a new variable that will be referenced elsewhere), or
- A `value` field (i.e., the declaration references another variable that is already defined elsewhere).

Looking at the source of the [**Long**](src/main/scala/jde/compiler/model/package.scala#L86-L100) declaration, we see an additional field, [`filter`](src/main/scala/jde/compiler/model/Enums.scala#L18) (which cen be any of `Ge, Le, Gt, Lt, Ne`). 
This field is used for matching using inequalities (example, if the number of certain tokens is greater than some value).

The following are some example declarations:
1. `{"name":"myAddress"}`
2. `{"value":"myAddress"}`
3. `{"name":"actualNanoErgs", "value":"someMinValue", "filter":"Ge"}`

- The first defines the address `myAddress`.
- The second references that address.
- The third defines the (Long) value `actualNanoErgs` and references `someMinValue`.
  An error occurs if `actualNanoErgs < someMinValue`. 

#### Targets and Pointers

For clarity, we use the following terminology when describing box declarations:
- A declaration that defines a variable is a "target".
- A declaration that references a variable is a "pointer".

We can then rewrite the rules for box declarations as follows:
- It can be either a target or a pointer but not both, with **Long** being the exception.
- An input can contain both pointers and targets.
- An output can only contain pointers.

The following rules apply for pointers and targets in an input:
- A pointer is a "search filter", i.e., used to fetch boxes from the blockchain.
For example, in `"boxId":{"value":"myBoxId"}`, the value contained in `myBoxId` (of type `KioskCollByte`) 
 will be used for fetching a box with that id.
- A target maps to some data in a box that has already been fetched from the blockchain.
For example, in `"address":{"name":"myAddress"}`, the address of the box will be stored in a variable called `myAddress`.

#### Input rule
The following rule applies for each input:
- It must have at least one of `boxId` or `address` declarations defined.

#### Token rules
A [**Token**](src/main/scala/jde/compiler/model/package.scala#L102-L106) is internally defined as 
`case class Token(index: Option[Int], id: Option[Id], amount: Option[Long])`. 
The main rule to follow here is that if `index` is empty then `id` must be defined, and that too as a pointer (i.e., it must have a `value` field). 
This is because the token index must be somehow determinable (either via an explicit `index` field or by matching the tokenId of a pointer.)

To illustrate this, the following are some valid token definitions:
1. `{"index":0, "id":{"name":"myTokenId"}, "amount":{"value":"otherTokenAmount"}}`.
   - Matches the token at index `0` if the amount is same as that of pointer `otherTokenAmount`. 
   - Creates a new target called `myTokenId` with the matched tokenId.
2. `{"index":0, "id":{"name":"myTokenId"}, "amount":{"name":"myTokenAmount"}}`. 
   - Matches the token at index `0`
   - Creates a new target called `myTokenId` containing the matched tokenId.
   - Creates a new target called `myTokenAmount` containing the matched token amount.
3. `{"id":{"value":"otherTokenId"}`. 
   - Matches the token at some index if the tokenId is same as that of pointer `otherTokenId`.
4. `{"id":{"value":"otherTokenId"}, "amount":{"value":"otherTokenAmount"}}}`. 
   - Matches the token at some index if both conditions hold:
     - The tokenId is the same as that of pointer `otherTokenId`. 
     - The amount is the same as that of `otherTokenAmount`.
5. `{"id":{"value":"otherTokenId"}, "amount":{"value":"otherTokenAmount", "filter":"Ge"}}`. 
   - Matches the token at some index if both conditions hold:
     - The tokenId is the same as that of pointer `otherTokenId`. 
     - The amount is >= the value returned by `otherTokenAmount`.
6. `{"id":{"value":"otherTokenId"}, "amount":{"name":"myTokenAmount"}}`. 
   - Matches the token at some index if the tokenId is the same as that of pointer `otherTokenId`. 
   - Creates a new target called `myTokenAmount` containing the matched token amount.

The following is an invalid token definition:
- `{"id":{"name":"myTokenId"}, "amount":{"name":"myTokenAmount"}}`. 

This is because if `id` is a target (i.e., has a `name` field) then `index` must be defined.

#### Strict token matching 

To ensure that the matched input has exactly those tokens defined in the search criteria and nothing more, use the `Strict` flag for that input definition:

```
"inputs": [ 
  { 
    "address": { ... },
    "tokens": [ ... ],
    "registers": [ ... ],
    "options": ["Strict"]
  }
]
```

For instance, to select a box with no tokens, skip `tokens` field (or set it to empty array) and add the `Strict` option. 

This option applies to tokens only.

#### Order of evaluation
Declarations are evaluated in the following order:
- Constants
- Computation boxes (`boxes`)  (from low to high index)
- Data-input boxes (from low to high index) 
- Input boxes (from low to high index)
- Post-conditions
- Output boxes
- Binary Ops, Unary Ops are "Lazy" (i.e., evaluated only if needed)

#### Referencing rules
- The order of evaluation determines what can and cannot be referenced. A pointer can only refer to a target that has been evaluated previously. 
  - Thus, a pointer in inputs can refer to a target in data-inputs, but a pointer in data-inputs cannot refer to a target in inputs.
  - Similarly, a pointer in the second input can refer to a target in the first input, but a pointer in the first input
    cannot refer to a target in the second input.
- It is not possible for a pointer to refer to a target in the same input or data-input.
- As mentioned earlier, an output cannot contain targets. It can only contain pointers.

#### Defining and matching multiple items

There may be cases where we need to map a single variable to mutiple objects. As an example, in the oracle-pool the pool
box addresses oscillate between *Live-epoch* and *Epoch-preparation*. In this case, we would prefer to use a single
variable `poolAddress` to handle both values.

The [`Constant`](src/main/scala/jde/compiler/model/package.scala#L52-L62) declaration has the additional field `values` that allows us to
define multiple items, sort of like an array, but much more restricted.

```
"constants": [
  {
    "name": "poolAddresses",
    "type": "Address",
    "values": [
      "epochPreparationAddress",
      "liveEpochAddress"
    ]
  }
]
```
For sanity, the  `values` field must have at least two elements. If we need to define a single object, we must instead use the `value` field.
Additionally, we cannot have both `value` and `values` fields.

We can use then this to match one of many addresses as follows:

```
"address": {
  "value": "poolAddresses"
}
```

Note: when matching multiple addresses, we can use `name` to store the actual address matched:

```
"address": {
  "name": "actualPoolAddress",
  "value": "poolAddresses"
}
```

#### Matching multiple inputs

Each input definition matches at most one input by default. If multiple inputs are matched, the first one is selected. In order to select all matched inputs use the `Multi` option for that input definiton:

```
"inputs": [
  {
    "address": { ... },
    "tokens": [ ... ],
    "registers": [ ... ],
    "options": ["Multi"]
  }
]
```

Multiple objects defined via `Constant` or matched using the `Multi` option are internally stored as an instance of the [`Multiple`](src/main/scala/jde/compiler/package.scala#L31-L59) class.

#### Internals of JDE

JDE is built on top of [Kiosk](https://github.com/scalahub/Kiosk) and each instance of a **Declaration** in JDE maps to an instance of some [`Kiosktype[_]`](https://github.com/scalahub/Kiosk/blob/master/src/main/scala/kiosk/ergo/package.scala#L32-L40). Speficially:
 
- **Address** maps to `KioskErgoTree`.
- **Id** maps to `KioskCollByte` of size 32.
- **Register** maps to `KioskType[_]`.
- **Long** maps to `KioskLong`.

#### Output of JDE

The JDE compiler takes as input an instance of **Protocol** and outputs and instance of 
[**CompileResult**](src/main/scala/jde/compiler/package.scala#L15), which contains the following details:

1. A sequence of box ids called `dataInputBoxIds` to use as data inputs.
2. A sequence of box ids called `inputBoxIds` to use as inputs. 
3. The sum of the nanoErgs of inputs, called `inputNanoErgs`.
4. A sequence of `(String, Long)` called `inputTokens` indicating the tokens in the inputs.
5. A sequence of `KioskBox` called `outputs` objects containing the built outputs.
6. An optional `Long` called `fee` indicating fee if any is specified in the script.

If the outputs contain more nanoErgs or tokens than specified above, then the wallet must add its own input box ids to cover the missing funds.
This is what happens in KioskWallet, which is a thin wrapper on JDE. 
