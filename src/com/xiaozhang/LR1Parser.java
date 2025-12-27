package com.xiaozhang;


import java.util.*;

public class LR1Parser {

    // 项目
    static class Item {
        int prodId;     // 产生式id
        int dot;        // 当前 . 的位置
        String lookahead;  // 展望符

        public Item(int pid, int d, String la) { prodId = pid; dot = d; lookahead = la; }

        public boolean equals(Object o) {
            if (!(o instanceof Item)) return false;
            Item oth = (Item)o;
            return prodId==oth.prodId && dot==oth.dot && lookahead.equals(oth.lookahead);
        }
        public int hashCode() { return Objects.hash(prodId,dot,lookahead); }
        public String toString(Grammar g) {
            Grammar.Prod p = g.prods.get(prodId);
            List<String> rhs = p.right;
            StringBuilder sb=new StringBuilder();
            sb.append(p.left).append(" -> ");
            for (int i=0;i<rhs.size();i++) {
                if (i==dot) sb.append("• ");
                sb.append(rhs.get(i)+" ");
            }
            if (dot==rhs.size()) sb.append("• ");
            sb.append(", ").append(lookahead);
            return sb.toString();
        }
    }

    Grammar G;                                          // 文法
    Map<Integer, Map<String, Integer>> action;          // 定义action表
    Map<Integer, Map<String, Integer>> goTo;            // 定义goto表
    List<Set<Item>> states;                             // LR1得到的状态集
    Map<Set<Item>, Integer> stateIds;                   // 状态对应编号
    Map<String, Set<String>> FIRST = new HashMap<>();   // 首符集
    TACGenerator tacGenerator = new TACGenerator();

    public LR1Parser(Grammar g) { G = g; }

    // 计算first集
    private void computeFirst() {
        Set<String> terms = G.terminals();
        terms.add("$");
        Set<String> nonterms = G.nonterminals();
        // 初始化终结符的first集
        FIRST.clear();
        for (String t : terms) FIRST.put(t, new HashSet<>(Arrays.asList(t)));
        // 求非终结符的first集
        for (String nt : nonterms) FIRST.put(nt, new HashSet<>());
        boolean changed=true;
        while (changed) {
            changed=false;
            for (Grammar.Prod p : G.prods) {
                // 非终结符p.left
                Set<String> firstL = FIRST.get(p.left);
                List<String> rhs = p.right;
                if (rhs.size()==0) continue;
                // 获取产生式右部的第一个字符
                String sym = rhs.get(0);
                Set<String> tmp = FIRST.get(sym);
                if (!firstL.containsAll(tmp)) {
                    firstL.addAll(tmp);
                    changed=true;
                }
            }
        }
    }

    // 求闭包 I为初始项目集
    private Set<Item> closure(Set<Item> I) {
        Set<Item> C = new HashSet<>(I);
        boolean added=true;
        while (added) {
            added=false;
            Set<Item> snapshot = new HashSet<>(C);
            for (Item it : snapshot) {
                Grammar.Prod p = G.prods.get(it.prodId);
                if (it.dot < p.right.size()) {
                    // 获取 · 右边的第一个符号
                    String B = p.right.get(it.dot);
                    // 如果B是非终结符
                    if (G.nonterminals().contains(B)) {
                        // 将非终结符B右边的全部加入beta集合中，求展望符
                        List<String> beta = new ArrayList<>();
                        for (int k = it.dot+1; k < p.right.size(); k++) beta.add(p.right.get(k));
                        beta.add(it.lookahead);
                        Set<String> firstBeta = firstOfSequence(beta);
                        // 将产生式左边为B的产生式对应的项目都加入到当前闭包中
                        for (Grammar.Prod q : G.getProdsFor(B)) {
                            for (String b : firstBeta) {
                                Item newIt = new Item(q.id, 0, b);
                                if (!C.contains(newIt)) { C.add(newIt); added=true; }
                            }
                        }
                    }
                }
            }
        }
        return C;
    }

    // 用于求展望符
    private Set<String> firstOfSequence(List<String> seq) {
        Set<String> res = new HashSet<>();
        if (seq.size()==0) return res;
        String s0 = seq.get(0);
        res.addAll(FIRST.getOrDefault(s0, new HashSet<>()));
        return res;
    }

    // 求状态I中能够识别符号X得到的项目集
    private Set<Item> gotoSet(Set<Item> I, String X) {
        Set<Item> J = new HashSet<>();
        for (Item it : I) {
            Grammar.Prod p = G.prods.get(it.prodId);
            if (it.dot < p.right.size() && p.right.get(it.dot).equals(X)) {
                J.add(new Item(it.prodId, it.dot+1, it.lookahead));
            }
        }
        return closure(J);
    }

    // 构造状态集和分析表
    public void buildStatesAndTables() {
        computeFirst();
        Item startItem = new Item(0, 0, "$");
        Set<Item> I0 = closure(new HashSet<>(Arrays.asList(startItem)));
        states = new ArrayList<>();
        stateIds = new HashMap<>();
        states.add(I0);
        stateIds.put(I0, 0);

        // 求所有状态集
        boolean added=true;
        while (added) {
            added=false;
            for (int i=0;i<states.size();i++) {
                // 求状态可能识别的符号，即 · 右边的第一个符号
                Set<Item> I = states.get(i);
                Set<String> symbols = new HashSet<>();
                for (Item it : I) {
                    Grammar.Prod p = G.prods.get(it.prodId);
                    if (it.dot < p.right.size()) symbols.add(p.right.get(it.dot));
                }
                for (String X : symbols) {
                    Set<Item> J = gotoSet(I, X);
                    if (J.isEmpty()) continue;
                    if (!stateIds.containsKey(J)) {
                        stateIds.put(J, states.size());
                        states.add(J);
                        added=true;
                    }
                }
            }
        }

        action = new HashMap<>();
        goTo = new HashMap<>();
        for (int i=0;i<states.size();i++) {
            action.put(i, new HashMap<>());
            goTo.put(i, new HashMap<>());
        }

        for (int i=0;i<states.size();i++) {
            Set<Item> I = states.get(i);
            for (Item it : I) {
                Grammar.Prod p = G.prods.get(it.prodId);
                if (it.dot < p.right.size()) {
                    String a = p.right.get(it.dot);
                    // 获取通过识别符号a得到状态 Jset
                    Set<Item> Jset = gotoSet(I, a);
                    if (!Jset.isEmpty()) {
                        int j = stateIds.get(Jset);
                        // 如果符号a是终结符，移进
                        if (G.terminals().contains(a)) {
                            action.get(i).put(a, j);
                        } else {
                            // 如果是非终结符，写入goto表
                            goTo.get(i).put(a, j);
                        }
                    }
                } else {
                    // 如果 · 在产生式右部的最后，则要进行归约或acc
                    if (p.left.equals("S'")) {
                        // 如果是产生式0，则acc
                        action.get(i).put("$", 0);
                    } else {
                        // 对展望符处的进行规约，使用-产生式id的表示
                        String la = it.lookahead;
                        action.get(i).put(la, -p.id);
                    }
                }
            }
        }
    }

    public void printStates() {
        for (int i=0;i<states.size();i++) {
            System.out.println("State " + i);
            for (Item it : states.get(i)) {
                System.out.println("  " + it.toString(G));
            }
        }
    }

    public void printTables() {
        System.out.println("ACTION table:");
        for (int i=0;i<action.size();i++) {
            System.out.println("State " + i + " -> " + action.get(i));
        }
        System.out.println("GOTO table:");
        for (int i=0;i<goTo.size();i++) {
            System.out.println("State " + i + " -> " + goTo.get(i));
        }
    }


    public static class ParseResult {
        public boolean success;
        public List<String> code;
        public String message;
        public ParseResult(boolean s, List<String> c, String m) { success=s; code=c; message=m; }
    }

    // 将词法分析器得到的结果转换成适合语法分析的形式
    private String tokToGramTerminal(Lexer.Token t) {
        if (t.type == Lexer.TokenType.IDENTIFIER) return "id";
        if (t.type == Lexer.TokenType.INT_CONST || t.type == Lexer.TokenType.FLOAT_CONST) return "num";
        if (t.type == Lexer.TokenType.OPERATOR) {
            return t.lexeme;
        }
        if (t.type == Lexer.TokenType.DELIMITER) {
            return t.lexeme;
        }
        if (t.type == Lexer.TokenType.EOF) return "$";
        if (t.type == Lexer.TokenType.KEYWORD) return t.lexeme;
        if (t.type == Lexer.TokenType.ERROR) return "err";
        return t.lexeme;
    }

    static class StackElem {
        public String sym;      // 符号栈
        public SemInfo val;      // 语义栈
        public StackElem(String s, SemInfo v){ sym=s; val=v; }
        public String toString(){ return sym + ":" + val; }
    }

    static class SemInfo {
        String place;           // 表示这个表达式或变量的地址
        List<String> code;      // 保存该表达式或语句生成的三地址码

        SemInfo(String p) {
            this.place = p;
            this.code = new ArrayList<>();
        }

        SemInfo(String p, List<String> c) {
            this.place = p;
            this.code = c != null ? c : new ArrayList<>();
        }

        @Override
        public String toString() { return place+"code:"+code.toString(); }

    }


    // 分析并生成中间代码
    public ParseResult parseAndGenerate(List<Lexer.Token> tokens, boolean verbose) {
        if (states==null) buildStatesAndTables();


        List<Lexer.Token> toks = new ArrayList<>(tokens);
        if (toks.isEmpty() || toks.get(toks.size()-1).type != Lexer.TokenType.EOF) {
            toks.add(new Lexer.Token(Lexer.TokenType.EOF, "$", -1, -1));
        }
        int ip = 0;
        Stack<Integer> stateStack = new Stack<>();  // 状态栈
        Stack<StackElem> symStack = new Stack<>();  // 分析栈
        stateStack.push(0);

        StringBuilder parseTrace = new StringBuilder();


        while (true) {
            int s = stateStack.peek();
            Lexer.Token cur = toks.get(ip);
            String a = tokToGramTerminal(cur);
            Integer act = action.get(s).get(a);
            if (verbose) {
                parseTrace.append(String.format("State=%d, 读头下的符号=%s, action=%s\n", s, cur, act));
            }
            // System.out.println(parseTrace.toString());
            if (act == null) {
                // 语法错误
                String msg = "Syntax error at " + cur.line + ":" + cur.col + " near '" + cur.lexeme + "'";
                return new ParseResult(false, null, parseTrace.toString() + msg);
            } else if (act == 0) {
                // 分析成功acc
                if (verbose) parseTrace.append("Accept.\n");
                if (!symStack.isEmpty()) {
                    SemInfo result = symStack.peek().val;
                    for (String line : result.code) {
                        tacGenerator.emit(line);
                    }
                    return new ParseResult(true,tacGenerator.getCode(), parseTrace.toString()+"OK");
                }
                return new ParseResult(true, new ArrayList<>(), parseTrace.toString()+"OK(no code)");
            } else if (act > 0) {
                // 移进
                String sym = a;
                SemInfo val = null;
                if (cur.type == Lexer.TokenType.IDENTIFIER) val = new SemInfo(cur.lexeme);
                else if (cur.type == Lexer.TokenType.INT_CONST || cur.type == Lexer.TokenType.FLOAT_CONST) val = new SemInfo(cur.lexeme);
                else val = new SemInfo(cur.lexeme);
                // System.out.println(sym+val);
                // 压栈
                symStack.push(new StackElem(sym, val));
                stateStack.push(act);
                ip++;
            } else {
                // 归约
                int prodId = -act;
                Grammar.Prod p = G.prods.get(prodId);
                int rhsLen = p.right.size();
                // System.out.println("rhsLen="+rhsLen);
                // 弹栈
                List<StackElem> popped = new ArrayList<>();
                for (int i=0;i<rhsLen;i++) {
                    if (!symStack.isEmpty()) popped.add(0, symStack.pop());
                    if (!stateStack.isEmpty()) stateStack.pop();
                }
                // System.out.println(popped);
                SemInfo newInfo = null;
                newInfo = generateTAC(prodId,popped);

                symStack.push(new StackElem(p.left, newInfo));
                StackElem peek = symStack.peek();
                // System.out.println("状态栈栈顶元素："+peek.sym+peek.val);
                int s2 = stateStack.peek();
                Integer g = goTo.get(s2).get(p.left);
                if (g == null) {
                    String msg = "Parsing error: no goto from state " + s2 + " on " + p.left;
                    return new ParseResult(false, null, msg);
                }
                stateStack.push(g);
            }
        }
    }


    private SemInfo generateTAC(int prodId, List<StackElem> popped) {
        SemInfo newInfo;

        switch (prodId) {
            case 1: {
                // R → for ( A C ; A1 ) {B}
                SemInfo init = popped.get(2).val;   // 初始化语句 S
                SemInfo cond = popped.get(3).val;   // 条件表达式 C
                SemInfo step = popped.get(5).val;   // 步进语句 S
                SemInfo body = popped.get(8).val;   // 循环体 B

                String Lbegin = tacGenerator.newLabel();
                String Lend   = tacGenerator.newLabel();
                List<String> code = new ArrayList<>();
                // A
                if (init != null) code.addAll(init.code);
                // Lbegin
                code.add(Lbegin + ":");
                // C
                if (cond != null) code.addAll(cond.code);
                code.add("ifFalse " + cond.place + " goto " + Lend);
                // B
                if (body != null) code.addAll(body.code);
                // A1
                if (step != null) code.addAll(step.code);
                // 回跳
                code.add("goto " + Lbegin);
                // Lend
                code.add(Lend + ":");
                newInfo = new SemInfo(null, code);
                break;
            }

            case 2: case 3: {
                // A → id = E | A1 → id = E;
                String idName = popped.get(0).val.place;
                SemInfo e = popped.get(2).val;

                List<String> code = new ArrayList<>(e.code);
                code.add(idName + " = " + e.place);

                newInfo = new SemInfo(idName, code);
                break;
            }

            case 4: case 5: case 6:
            case 7: case 8: case 9: {
                // 条件表达式
                SemInfo e1 = popped.get(0).val;
                String op = popped.get(1).val.place;
                SemInfo e2 = popped.get(2).val;

                List<String> code = new ArrayList<>();
                code.addAll(e1.code);
                code.addAll(e2.code);

                String cond = e1.place + " " + op + " " + e2.place;
                newInfo = new SemInfo(cond, code);
                break;
            }

            case 10: {
                // S → A
                newInfo = popped.get(0).val;
                break;
            }

            case 11: {
                // S → I
                newInfo = popped.get(0).val;
                break;
            }

            case 12: {
                // I → if ( C ) { S } else { S }
                SemInfo cond = popped.get(2).val;
                SemInfo thenStmt = popped.get(5).val;
                SemInfo elseStmt = popped.get(9).val;

                String Lelse = tacGenerator.newLabel();
                String Lend = tacGenerator.newLabel();

                List<String> code = new ArrayList<>();
                // 条件判断
                code.add("ifFalse " + cond.place + " goto " + Lelse);

                // then 分支
                code.addAll(thenStmt.code);

                code.add("goto " + Lend);

                // else 分支
                code.add(Lelse + ":");
                code.addAll(elseStmt.code);

                // 结束标签
                code.add(Lend + ":");
                String temp = tacGenerator.newTemp();
                newInfo = new SemInfo(temp,code);
                break;
            }

            case 13: case 14: {
                // E → E + T | E → E - T
                SemInfo e1 = popped.get(0).val;
                String op = popped.get(1).val.place;
                SemInfo t = popped.get(2).val;

                List<String> code = new ArrayList<>();
                code.addAll(e1.code);
                code.addAll(t.code);

                String temp = tacGenerator.newTemp();
                code.add(temp + " = " + e1.place + " " + op + " " + t.place);

                newInfo = new SemInfo(temp, code);
                break;
            }

            case 15: {
                // E → T
                newInfo = popped.get(0).val;
                break;
            }

            case 16: case 17: {
                // T → T * F | T → T / F
                SemInfo t1 = popped.get(0).val;
                String op = popped.get(1).val.place;
                SemInfo f = popped.get(2).val;

                List<String> code = new ArrayList<>();
                code.addAll(t1.code);
                code.addAll(f.code);

                String temp = tacGenerator.newTemp();
                code.add(temp + " = " + t1.place + " " + op + " " + f.place);

                newInfo = new SemInfo(temp, code);
                break;
            }

            case 18:{
                // T → F
                newInfo = popped.get(0).val;
                break;
            }

            case 19: case 20: {
                // F → id | num
                newInfo = popped.get(0).val;
                break;
            }

            case 21:{
                // F → (E)
                newInfo = popped.get(1).val;
                break;
            }

            case 22:{
                // B → B S
                // System.out.println(popped);
                SemInfo b = popped.get(0).val;
                SemInfo s = popped.get(1).val;

                List<String> code = new ArrayList<>();
                code.addAll(b.code);
                code.addAll(s.code);

                newInfo = new SemInfo(null, code);
                break;
            }
            case 23:{
                // B → S
                newInfo = popped.get(0).val;
                break;
            }
            case 24:{
                // S → R
                newInfo = popped.get(0).val;
                break;
            }
            default:
                newInfo = new SemInfo(null);
        }

        return newInfo;
    }


}
