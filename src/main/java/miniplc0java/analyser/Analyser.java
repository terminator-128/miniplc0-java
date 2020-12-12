package miniplc0java.analyser;

import miniplc0java.error.AnalyzeError;
import miniplc0java.error.CompileError;
import miniplc0java.error.ErrorCode;
import miniplc0java.error.ExpectedTokenError;
import miniplc0java.error.TokenizeError;
import miniplc0java.instruction.Instruction;
import miniplc0java.instruction.Operation;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;
import miniplc0java.util.Pos;

import java.util.*;

public final class Analyser {

    Tokenizer tokenizer;
    ArrayList<Instruction> instructions;

    /** 当前偷看的 token */
    Token peekedToken = null;

    /** 符号表 */
    HashMap<String, SymbolEntry> symbolTable = new HashMap<>();

    /** 下一个变量的栈偏移 */
    int nextOffset = 0;

    public Analyser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        this.instructions = new ArrayList<>();
    }

    public List<Instruction> analyse() throws CompileError {
        analyseProgram();
        return instructions;
    }

    /**
     * 查看下一个 Token
     *
     * @return 如peekedToken为空，则peek下一个token；如peekToken不为空，则返回
     * @throws TokenizeError 返回词法分析错误
     */
    private Token peek() throws TokenizeError {
        if (peekedToken == null) {
            peekedToken = tokenizer.nextToken();
        }
        return peekedToken;
    }

    /**
     * 获取下一个 Token
     * 
     * @return 若peekToken不为空，返回peekToken并置空（相当于前进一个Token）；如为空，则返回tokenizer.nextToken()
     * @throws TokenizeError 词法分析错误
     */
    private Token next() throws TokenizeError {
        if (peekedToken != null) {
            var token = peekedToken;
            peekedToken = null;
            return token;
        } else {
            return tokenizer.nextToken();
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则返回 true
     * 
     * @param tt token类型
     * @return 若下一个token类型为tt，则返回true；否则返回false
     * @throws TokenizeError 词法分析错误
     */
    private boolean check(TokenType tt) throws TokenizeError {
        var token = peek();
        return token.getTokenType() == tt;
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回这个 token
     * 
     * @param tt 类型
     * @return 如果匹配则返回这个 token，否则返回 null
     * @throws TokenizeError 词法分析错误
     */
    private Token nextIf(TokenType tt) throws TokenizeError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            return null;
        }
    }

    /**
     * 如果下一个 token 的类型是 tt，则前进一个 token 并返回，否则抛出异常
     * 
     * @param tt 类型
     * @return 这个 token
     * @throws CompileError 如果类型不匹配
     */
    private Token expect(TokenType tt) throws CompileError {
        var token = peek();
        if (token.getTokenType() == tt) {
            return next();
        } else {
            throw new ExpectedTokenError(tt, token);
        }
    }

    /**
     * 获取下一个变量的栈偏移
     * @return 下一个变量的栈偏移
     */
    private int getNextVariableOffset() {
        return this.nextOffset++;
    }

    /**
     * 添加一个符号
     * 
     * @param name          名字
     * @param isInitialized 是否已赋值
     * @param isConstant    是否是常量
     * @param curPos        当前 token 的位置（报错用）
     * @throws AnalyzeError 如果重复定义了则抛异常
     */
    private void addSymbol(String name, boolean isInitialized, boolean isConstant, Pos curPos) throws AnalyzeError {
        if (this.symbolTable.get(name) != null) {
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration, curPos);
        } else {
            this.symbolTable.put(name, new SymbolEntry(isConstant, isInitialized, getNextVariableOffset()));
        }
    }

    /**
     * 设置符号为已赋值
     * 
     * @param name   符号名称
     * @param curPos 当前位置（报错用）
     * @throws AnalyzeError 如果未定义则抛异常
     */
    private void initializeSymbol(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            entry.setInitialized(true);
        }
    }

    /**
     * 获取变量在栈上的偏移
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 栈偏移
     * @throws AnalyzeError 语法分析错误
     */
    private int getOffset(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.getStackOffset();
        }
    }

    /**
     * 获取变量是否是常量
     * 
     * @param name   符号名
     * @param curPos 当前位置（报错用）
     * @return 是否为常量
     * @throws AnalyzeError 语法分析错误
     */
    private boolean isConstant(String name, Pos curPos) throws AnalyzeError {
        var entry = this.symbolTable.get(name);
        if (entry == null) {
            throw new AnalyzeError(ErrorCode.NotDeclared, curPos);
        } else {
            return entry.isConstant();
        }
    }

    /** checked
     * @throws CompileError
     * <程序> ::= 'begin'<主过程>'end'
     */
    private void analyseProgram() throws CompileError {
        // 示例函数，示例如何调用子程序
        // 'begin'
        expect(TokenType.Begin);

        analyseMain();

        // 'end'
        expect(TokenType.End);
        expect(TokenType.EOF);
    }

    /** checked
     * @throws CompileError
     * <主过程> ::= <常量声明><变量声明><语句序列>
     */
    private void analyseMain() throws CompileError {
        // 常量声明
        analyseConstantDeclaration();
        // 变量声明
        analyseVariableDeclaration();
        // 语句序列
        analyseStatementSequence();
    }

    /**
     * @throws CompileError
     * <常量声明> ::= {<常量声明语句>}
     * <常量声明语句> ::= 'const'<标识符>'='<常表达式>';'
     */
    private void analyseConstantDeclaration() throws CompileError {
        // 常量声明
        while (nextIf(TokenType.Const) != null) {
            // 常量声明语句：
            // 标识符
            var nameToken = expect(TokenType.Ident);

            // 添加符号至符号表
            addSymbol((String) nameToken.getValue(), true, true, nameToken.getStartPos());

            // 等于号
            expect(TokenType.Equal);
            // 常表达式
            var value = analyseConstantExpression();
            // 分号
            expect(TokenType.Semicolon);
            // 这里直接把常量值放入栈里，位置和符号表记录的一样
            // 更高级的程序还可以把常量的值记录下来，遇到相应的变量直接替换成这个常数值
            // 我们这里就先不这么干了
            instructions.add(new Instruction(Operation.LIT, value));

        }
    }

    /**
     * @throws CompileError
     * <常表达式> ::= [<符号>]<无符号整数>
     */
    private int analyseConstantExpression() throws CompileError {
        // [<符号>]
        boolean negate = false;
        if (nextIf(TokenType.Minus) != null) {
            negate = true;
        }
        else{
            nextIf(TokenType.Plus);
        }

        // 无符号整数
        var token = expect(TokenType.Uint);
        int value = (int) token.getValue();

        if (negate) {
            value = -value;
        }
        return value;
    }

    /** checked
     * @throws CompileError
     * <变量声明> ::= {<变量声明语句>}
     */
    private void analyseVariableDeclaration() throws CompileError {
        // 变量声明
        boolean isInitialized;
        while (nextIf(TokenType.Var) != null) {
            // 变量声明语句：
            // 变量名
            var nameToken = expect(TokenType.Ident);
            // 变量初始化默认设置否
            isInitialized = false;
            // ['='<表达式>]';'
            if (nextIf(TokenType.Equal) != null) {
                // 表达式
                analyseExpression();
                isInitialized = true;
            }
            // 分号
            expect(TokenType.Semicolon);

            // 添加至符号表
            addSymbol((String) nameToken.getValue(), isInitialized, false, nameToken.getStartPos());
            // 如果没有初始化的话在栈内推入一个初始值
            if (!isInitialized){
                instructions.add(new Instruction(Operation.LIT, 0));
            }
        }
    }

    /** checked
     * @throws CompileError
     * <语句序列> ::= {<语句>}
     * <语句> ::= <赋值语句>|<输出语句>|<空语句>
     */
    private void analyseStatementSequence() throws CompileError {

        while (true) {
            if (check(TokenType.Ident)){
                analyseAssignmentStatement();
            }
            else if (check(TokenType.Print)){
                analyseOutputStatement();
            }
            else if (check(TokenType.Semicolon)){
                expect(TokenType.Semicolon);
            }
            else{
                break;
            }
        }
    }


    /**
     * @throws CompileError
     * <赋值语句> ::= <标识符>'='<表达式>';'
     */
    private void analyseAssignmentStatement() throws CompileError {
        // 标识符
        Token nameToken = expect(TokenType.Ident);
        // 等号
        expect(TokenType.Equal);
        // 表达式
        analyseExpression();
        // 分号
        expect(TokenType.Semicolon);

        // 企图从符号表中拿值
        String name = (String) nameToken.getValue();
        var symbol = symbolTable.get(name);
        // 返回符号判断
        if (symbol == null){
            // 没有这个标识符
            throw new AnalyzeError(ErrorCode.AssignToConstant, nameToken.getStartPos());
        }
        else if (symbol.isConstant()){
            // 标识符是常量
            throw new AnalyzeError(ErrorCode.AssignToConstant, nameToken.getStartPos());
        }

        // 设置符号已经初始化
        initializeSymbol(name, null);
        // 把结果保存
        var offset = getOffset(name, null);
        instructions.add(new Instruction(Operation.STO, offset));
    }

    /**
     * @throws CompileError
     * <输出语句> ::= 'print' '(' <表达式> ')' ';'
     */
    private void analyseOutputStatement() throws CompileError {
        expect(TokenType.Print);
        expect(TokenType.LParen);
        analyseExpression();
        expect(TokenType.RParen);
        expect(TokenType.Semicolon);
        instructions.add(new Instruction(Operation.WRT));
    }

    /**
     * @throws CompileError
     * <表达式> ::= <项>{<加法型运算符><项>}
     */
    private void analyseExpression() throws CompileError {
        // 项
        analyseItem();
        while (check(TokenType.Plus)||check(TokenType.Minus)){
            // 加法型运算符
            var op = next();
            // 项
            analyseItem();
            // 生成代码
            if (op.getTokenType()==TokenType.Plus){
                instructions.add(new Instruction(Operation.ADD));
            }
            else if (op.getTokenType()==TokenType.Minus){
                instructions.add(new Instruction(Operation.SUB));
            }
        }
    }

    /**
     * @throws CompileError
     * <项> ::= <因子>{<乘法型运算符><因子>}
     */
    private void analyseItem() throws CompileError {
        // 因子
        analyseFactor();
        // {<乘法型运算符><因子>}
        while (check(TokenType.Mult)||check(TokenType.Div)){
            // 读取操作符
            Token op = next();
            // 因子
            analyseFactor();
            // 生成代码
            if (op.getTokenType()==TokenType.Mult){
                instructions.add(new Instruction(Operation.MUL));
            }
            else if(op.getTokenType()==TokenType.Div){
                instructions.add(new Instruction(Operation.DIV));
            }
        }

    }

    /**
     * @throws CompileError
     * <因子> ::= [<符号>]( <标识符> | <无符号整数> | '('<表达式>')' )
     */
    private void analyseFactor() throws CompileError {
        boolean negate;
        if (nextIf(TokenType.Minus) != null) {
            negate = true;
            // 计算结果需要被 0 减
            instructions.add(new Instruction(Operation.LIT, 0));
        } else {
            nextIf(TokenType.Plus);
            negate = false;
        }

        if (check(TokenType.Ident)) {
            // 调用相应的处理函数
            // 标识符token
            var token = expect(TokenType.Ident);
            // 加载标识符的值
            String name = (String) token.getValue();
            var symbol = symbolTable.get(name);
            if (symbol==null){
                // 之前没有声明标识符
                throw new AnalyzeError(ErrorCode.NotDeclared, token.getStartPos());
            }else if (!symbol.isInitialized){
                // 标识符未初始化
                throw new AnalyzeError(ErrorCode.NotInitialized, token.getStartPos());
            }
            // 加载栈内存储的标识符的值
            var offset = getOffset(name, null);
            instructions.add(new Instruction(Operation.LOD, offset));
        } else if (check(TokenType.Uint)) {
            // 如果下一个token是整数
            instructions.add(new Instruction(Operation.LIT, (int)next().getValue()));
        } else if (nextIf(TokenType.LParen)!=null) {
            // 如果下一个语法成分为表达式
            analyseExpression();
            expect(TokenType.RParen);
        } else {
            // 都不是，摸了
            throw new ExpectedTokenError(List.of(TokenType.Ident, TokenType.Uint, TokenType.LParen), next());
        }

        if (negate) {
            instructions.add(new Instruction(Operation.SUB));
        }
    }
}
