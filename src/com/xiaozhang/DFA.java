package com.xiaozhang;
import java.util.*;

public class DFA {
    enum DFAState {
        START,

        ID,           // 标识符
        INT,          // 整数
        FLOAT,        // 浮点数
        DOT,          // 以 . 开头的浮点
        OP,           // 运算符
        DELIM,        // 分隔符

        ERROR,
        ACCEPT
    }


    Map<DFAState, DFAStateNode> states = new HashMap<>();
    DFAState start = DFAState.START;

    DFA() {
        init();
    }

    private void init() {
        // START
        DFAStateNode start = new DFAStateNode(DFAState.START);
        states.put(DFAState.START, start);

        // ID
        DFAStateNode id = new DFAStateNode(DFAState.ID);
        id.accept = true;
        id.tokenType = Lexer.TokenType.IDENTIFIER;
        states.put(DFAState.ID, id);

        // INT
        DFAStateNode intS = new DFAStateNode(DFAState.INT);
        intS.accept = true;
        intS.tokenType = Lexer.TokenType.INT_CONST;
        states.put(DFAState.INT, intS);

        // FLOAT
        DFAStateNode floatS = new DFAStateNode(DFAState.FLOAT);
        floatS.accept = true;
        floatS.tokenType = Lexer.TokenType.FLOAT_CONST;
        states.put(DFAState.FLOAT, floatS);

        // DOT
        DFAStateNode dot = new DFAStateNode(DFAState.DOT);
        states.put(DFAState.DOT, dot);

        // OP
        DFAStateNode op = new DFAStateNode(DFAState.OP);
        op.accept = true;
        op.tokenType = Lexer.TokenType.OPERATOR;
        states.put(DFAState.OP, op);

        // DELIM
        DFAStateNode delim = new DFAStateNode(DFAState.DELIM);
        delim.accept = true;
        delim.tokenType = Lexer.TokenType.DELIMITER;
        states.put(DFAState.DELIM, delim);
    }

    DFAStateNode get(DFAState s) {
        return states.get(s);
    }




    class DFAStateNode {
        DFAState state;
        Map<Character, DFAState> trans = new HashMap<>();
        boolean accept;
        Lexer.TokenType tokenType;

        DFAStateNode(DFAState s) {
            state = s;
        }

        void add(char c, DFAState to) {
            trans.put(c, to);
        }

        DFAState next(char c) {
            return trans.get(c);
        }
    }
}