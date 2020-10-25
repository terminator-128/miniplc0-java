# miniplc0-java

> miniplc0 实验的 Java版本代码

### 编译过程

> **编译**（compilation）是将指定语言的源代码输入翻译成目标语言说出，通常来说是目标语言的某种汇编指令集。

mini plc0 本身相当简单，但是他依然规定了程序最基本的一些功能：

- 常量/变量定义
- 四则运算
- 赋值运算
- 输出数值

### 词法分析

>  **词法分析**是编译器读入源代码文本并将代码划分为拥有具体含义的单词序列的过程，这些单词被称为token。

#### token

```java
// tokenizer/TokenType.java
package miniplc0java.tokenizer;

public enum TokenType {
    None,        // 仅仅为了内部实现方便，不应该出现在分析过程
    Uint,        // 无符号整数
    Ident,       // 标识符
    Begin,       // 关键字 BEGIN
    End,         // 关键字 END
    Var,         // 关键字 VAR
    Const,       // 关键字 CONST
    Print,       // 关键字 PRINT
    Plus,        // 符号 +
    Minus,       // 符号 -
    Mult,        // 符号 *
    Div,         // 符号 /
    Equal,       // 符号 =
    Semicolon,   // 符号 ;
    LParen,      // 符号 (
    RParen,      // 符号 )
    EOF          // 文件结尾
}
```

#### 状态图

![mini-plc0 词法分析状态图](https://github.com/BUAA-SE-Compiling/miniplc0-handbook/raw/master/img/dfa.png)



### 语法分析

> **语法分析**（syntactic analysis）是编译器根据词法分析结果，检查代码是否符合语法规则的过程，通常会生成一棵描述源代码程序结构的树

#### 文法规则

```markdown
<字母> ::=
     'a'|'b'|'c'|'d'|'e'|'f'|'g'|'h'|'i'|'j'|'k'|'l'|'m'|'n'|'o'|'p'|'q'|'r'|'s'|'t'|'u'|'v'|'w'|'x'|'y'|'z'
    |'A'|'B'|'C'|'D'|'E'|'F'|'G'|'H'|'I'|'J'|'K'|'L'|'M'|'N'|'O'|'P'|'Q'|'R'|'S'|'T'|'U'|'V'|'W'|'X'|'Y'|'Z'
<数字> ::= '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'
<符号> ::= '+'|'-'

<无符号整数> ::= <数字>{<数字>}
<标识符> ::= <字母>{<字母>|<数字>}

<关键字> ::= 'begin' | 'end' | 'const' | 'var' | 'print'

<程序> ::= 'begin'<主过程>'end'
<主过程> ::= <常量声明><变量声明><语句序列>

<常量声明> ::= {<常量声明语句>}
<常量声明语句> ::= 'const'<标识符>'='<常表达式>';'
<常表达式> ::= [<符号>]<无符号整数>

<变量声明> ::= {<变量声明语句>}
<变量声明语句> ::= 'var'<标识符>['='<表达式>]';'

<语句序列> ::= {<语句>}
<语句> ::= <赋值语句>|<输出语句>|<空语句>
<赋值语句> ::= <标识符>'='<表达式>';'
<输出语句> ::= 'print' '(' <表达式> ')' ';'
<空语句> ::= ';'

<表达式> ::= <项>{<加法型运算符><项>}
<项> ::= <因子>{<乘法型运算符><因子>}
<因子> ::= [<符号>]( <标识符> | <无符号整数> | '('<表达式>')' )

<加法型运算符> ::= '+'|'-'
<乘法型运算符> ::= '*'|'/'
```

### 语义规则

> **语义分析**（semantic analysis）是编译器根据语法分析结果检查代码是否呼和语义规则的过程。此过程一般会构建符号表，冰箱语法树添加额外的语义信息，有时还会执行中间代码的生成。

- 不能重复定义：同一个标识符，不能被重复声明
- 不能使用没有声明过得标识符
- 不能给常量赋值：被声明为常量的标识符，不能出现在赋值语句的左侧
- 不能使用未初始化的变量：不能参与表达式的运算，也不能出现在赋值语句的右侧
- 无符号整数数字变量的值必须在值域 $[0,\ 2^{31}-1]$。

### 语法制导翻译

> **语法制导翻译**（syntax-directed-translation）可以视为一边进行语法分析，一边进行语义分析。

此实验中才用的就是：基于递归下降分析的语法制导翻译。

> **递归下降分析**本身构建语法树的顺序就是之后在语法书上做遍历的顺序，父节点相对于子节点的生成树顺序决定了遍历的顺序（前序、后序等）。
>
> 如果让递归下降分析的父节点生成放在所有子节点生成后，并且同时进行语义分析，那么得到的动作指令序列（或中间代码）刚好满足逆**波兰表达式**。因此在语义规则不太复杂的情况下，甚至可以省略语法树的构建。

### 符号表管理

> **符号表**（symbol table）是存储了已经被声明过的标志符的具体信息的数据结构。
>
> 在语义分析阶段构造符号表，并根据分析的需要对符号表进行增加、删除、修改、查询，既是所谓的**符号表管理**。

对于编译型语言，符号表的声明周期往往和语义分析相同，最终得到的目标代码/可执行程序中，通常不会包含有关代码中名字的信息。

符号表的形态往往取决与实际情况，在我们的mini实验中，符号表值是一个哈希表（`std::unordered_map`）。而C0是拥有多级作用域的语言，如果对其采用一趟扫描的编译，通常会采用栈式符号表；而对于多趟扫描的话，树形符号表会更实用一些。

### 错误处理

> **错误处理**（error handling）是贯穿整个编译流程的一环，他主要负责对个编译子程序出现的错误进行记录、报告甚至是纠正。

此实验中出现错误，直接报出异常。

### 代码优化

> **代码优化**（Code optimization）是为了提高诸如运行速度和内存占用等的程序性能，对编译的中间结果/目标代码进行一系列优化的行为。

### 代码生成

> **代码生成**（code generation）是将指定代码的源文件最终翻译成目标语言文件，通常来说目标语言是某种指令集。

此试验（miniplc0）采用栈式虚拟机为目标平台。

> 实验用栈式虚拟机标准地址：[Here](https://vm.buaasecompiling.cn/)