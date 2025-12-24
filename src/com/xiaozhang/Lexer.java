package com.xiaozhang;
import java.util.*;

public class Lexer {

    private static final Set<String> KEYWORDS = Set.of(
            "int","float","char","if","else","while","for","return",
            "void","double","main","do","include"
    );

    private static final Set<Character> DELIMS = Set.of(
            '(',')','{','}',';',',','[',']'
    );

    private final String code;
    private int index = 0;
    private int line = 1, col = 1;

    private final DFA dfa = new DFA();

    public Lexer(String code) {
        this.code = code == null ? "" : code;
    }

    public Token nextToken() {
        while (index < code.length() &&
                Character.isWhitespace(code.charAt(index))) {
            consumeWhitespace();
        }

        if (index >= code.length())
            return new Token(TokenType.EOF, "$", line, col);

        int startLine = line, startCol = col;
        int startIdx = index;

        DFA.DFAState state = DFA.DFAState.START;
        DFA.DFAState lastAccept = null;
        int lastAcceptIdx = -1;

        while (index < code.length()) {
            char ch = code.charAt(index);

            DFA.DFAState next = transition(state, ch);
            if (next == null) break;

            state = next;
            index++;
            col++;

            if (dfa.get(state).accept) {
                lastAccept = state;
                lastAcceptIdx = index;
            }
        }

        if (lastAccept == null) {
            index++;
            col++;
            return new Token(TokenType.ERROR,
                    "Illegal token", startLine, startCol);
        }

        String lexeme = code.substring(startIdx, lastAcceptIdx);
        index = lastAcceptIdx;

        TokenType type = dfa.get(lastAccept).tokenType;

        if (type == TokenType.IDENTIFIER && KEYWORDS.contains(lexeme))
            type = TokenType.KEYWORD;

        return new Token(type, lexeme, startLine, startCol);
    }

    private DFA.DFAState transition(DFA.DFAState s, char c) {
        switch (s) {
            case START:
                if (Character.isLetter(c) || c == '_') return DFA.DFAState.ID;
                if (Character.isDigit(c)) return DFA.DFAState.INT;
                if (c == '.') return DFA.DFAState.DOT;
                if ("+-*/=!<>|&%".indexOf(c) >= 0) return DFA.DFAState.OP;
                if (DELIMS.contains(c)) return DFA.DFAState.DELIM;
                return null;

            case ID:
                if (Character.isLetterOrDigit(c) || c == '_')
                    return DFA.DFAState.ID;
                return null;

            case INT:
                if (Character.isDigit(c)) return DFA.DFAState.INT;
                if (c == '.') return DFA.DFAState.FLOAT;
                return null;

            case DOT:
                if (Character.isDigit(c)) return DFA.DFAState.FLOAT;
                return null;

            case FLOAT:
                if (Character.isDigit(c)) return DFA.DFAState.FLOAT;
                return null;

            case OP:
                if ("=<>|&+-".indexOf(c) >= 0)
                    return DFA.DFAState.OP;
                return null;

            case DELIM:
                return null;
        }
        return null;
    }

    private void consumeWhitespace() {
        char c = code.charAt(index++);
        if (c == '\n') {
            line++;
            col = 1;
        } else col++;
    }

    public List<Token> tokenize() {
        List<Token> list = new ArrayList<>();
        Token t;
        do {
            t = nextToken();
            list.add(t);
        } while (t.type != TokenType.EOF);
        return list;
    }

    // ===== Token =====
    public enum TokenType {
        KEYWORD, IDENTIFIER, INT_CONST, FLOAT_CONST,
        OPERATOR, DELIMITER, ERROR, EOF
    }

    public static class Token {
        public TokenType type;
        public String lexeme;
        public int line, col;

        public Token(TokenType t, String s, int l, int c) {
            type = t;
            lexeme = s;
            line = l;
            col = c;
        }

        public String toString() {
            return "(" + lexeme + "," + type + ") @" + line + ":" + col;
        }
    }
}