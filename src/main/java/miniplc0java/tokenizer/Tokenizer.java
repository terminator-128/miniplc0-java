package miniplc0java.tokenizer;

import miniplc0java.error.CompileError;
import miniplc0java.error.TokenizeError;
import miniplc0java.error.ErrorCode;
import miniplc0java.util.Pos;

import java.security.Key;

public class Tokenizer {

    private StringIter it;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了
    /**
     * 获取下一个 Token
     * 
     * @return 获取下一个token
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }

        char peek = it.peekChar();
        if (Character.isDigit(peek)) {
            return lexUInt();
        } else if (Character.isAlphabetic(peek)) {
            return lexIdentOrKeyword();
        } else {
            return lexOperatorOrUnknown();
        }
    }

    private Token lexUInt() throws TokenizeError {
        // 请填空：
        StringBuilder str_val = new StringBuilder();
        // 直到查看下一个字符不是数字为止:
        do {
            // -- 前进一个字符，并存储这个字符
            str_val.append(it.nextChar());
        }while(Character.isDigit(it.peekChar()));
        //
        // 解析存储的字符串为无符号整数
        try {
            int int_val = Integer.parseInt(str_val.toString());
            return new Token(TokenType.Uint, int_val, it.previousPos(), it.currentPos());
        }
        catch (NumberFormatException e){
            // 解析成功则返回无符号整数类型的token，否则返回编译错误
            throw new Error("Can't recognize parse the string value: "+str_val+" into integer");
        }
    }

    private Token lexIdentOrKeyword() throws TokenizeError {
        // 请填空：
        // 直到查看下一个字符不是数字或字母为止:
        StringBuilder str_val = new StringBuilder();
        Pos prePos = it.currentPos();

        do {
            // -- 前进一个字符，并存储这个字符
            str_val.append(it.nextChar());
        }while (Character.isAlphabetic(it.peekChar()));
        //
        // 尝试将存储的字符串解释为关键字
        //
        // Token 的 Value 应填写标识符或关键字的字符串
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        return switch (str_val.toString()) {
            case "begin" -> new Token(TokenType.Begin, "begin", prePos, it.currentPos());
            case "end" -> new Token(TokenType.End, "end", prePos, it.currentPos());
            case "var" -> new Token(TokenType.Var, "var", prePos, it.currentPos());
            case "const" -> new Token(TokenType.Const, "const", prePos, it.currentPos());
            case "print" -> new Token(TokenType.Print, "print", prePos, it.currentPos());
            default -> new Token(TokenType.Ident, str_val.toString(), prePos, it.currentPos());
        };
    }

    private Token lexOperatorOrUnknown() throws TokenizeError {
        // 操作符 token
        return switch (it.nextChar()) {
            case '+' -> new Token(TokenType.Plus, '+', it.previousPos(), it.currentPos());
            case '-' -> new Token(TokenType.Minus, '-', it.previousPos(), it.currentPos());
            case '*' -> new Token(TokenType.Mult, '*', it.previousPos(), it.currentPos());
            case '/' -> new Token(TokenType.Div, '/', it.previousPos(), it.currentPos());
            case '=' -> new Token(TokenType.Equal, '=', it.previousPos(), it.currentPos());
            case ';' -> new Token(TokenType.Semicolon, ';', it.previousPos(), it.currentPos());
            case '(' -> new Token(TokenType.LParen, '(', it.previousPos(), it.currentPos());
            case ')' -> new Token(TokenType.RParen, ')', it.previousPos(), it.currentPos());
            default -> throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        };
    }

    private void skipSpaceCharacters() {
        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }
}
