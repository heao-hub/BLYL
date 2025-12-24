package com.xiaozhang;

import java.util.*;

public class Grammar {

    // 产生式集合
    public List<Prod> prods = new ArrayList<>();
    // 开始符号
    public String start;


    public static class Prod {
        String left;
        List<String> right;
        int id;

        public Prod(int id, String left, List<String> right) {
            this.id = id;
            this.left = left;
            this.right = right;
        }
        public String toString() {
            return id + ": " + left + " -> " + String.join(" ", right);
        }
    }

    public Grammar() {
        // 拓广文法
        prods.add(new Prod(0,"B'", List.of("B")));

        // for 循环
        prods.add(new Prod(1,"R", List.of("for", "(", "A","C", ";", "A1", ")", "{","B","}")));

        // 赋值语句
        prods.add(new Prod(2,"A", List.of("id", "=", "E", ";")));
        prods.add(new Prod(3,"A1", List.of("id", "=", "E")));

        // 条件
        prods.add(new Prod(4,"C", List.of("E", "<", "E")));
        prods.add(new Prod(5,"C", List.of("E", ">", "E")));
        prods.add(new Prod(6,"C", List.of("E", "<=", "E")));
        prods.add(new Prod(7,"C", List.of("E", ">=", "E")));
        prods.add(new Prod(8,"C", List.of("E", "!=", "E")));
        prods.add(new Prod(9,"C", List.of("E", "==", "E")));

        // 语句块
        prods.add(new Prod(10,"S", List.of("A")));
        prods.add(new Prod(11,"S", List.of("I")));
        prods.add(new Prod(12,"I", List.of("if","(","C",")", "{","B","}","else","{","B","}")));

        // 表达式
        prods.add(new Prod(13,"E", List.of("E", "+", "T")));
        prods.add(new Prod(14,"E", List.of("E", "-", "T")));
        prods.add(new Prod(15,"E", List.of("T")));

        prods.add(new Prod(16,"T", List.of("T", "*", "F")));
        prods.add(new Prod(17,"T", List.of("T", "/", "F")));
        prods.add(new Prod(18,"T", List.of("F")));

        prods.add(new Prod(19,"F", List.of("id")));
        prods.add(new Prod(20,"F", List.of("num")));
        prods.add(new Prod(21,"F", List.of("(", "E", ")")));

        prods.add(new Prod(22,"B", List.of("B", "S")));
        prods.add(new Prod(23,"B", List.of( "S")));
        prods.add(new Prod(24,"S", List.of( "R")));



        start = "B'";
    }



    public Set<String> nonterminals() {
        Set<String> s = new HashSet<>();
        for (Prod p : prods) s.add(p.left);
        return s;
    }

    public Set<String> terminals() {
        Set<String> s = new HashSet<>();
        for (Prod p : prods) {
            for (String r : p.right) {
                if (!nonterminals().contains(r)) s.add(r);
            }
        }
        return s;
    }

    public List<Prod> getProdsFor(String nt) {
        List<Prod> res = new ArrayList<>();
        for (Prod p : prods) if (p.left.equals(nt)) res.add(p);
        return res;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Prod p : prods) sb.append(p).append("\n");
        return sb.toString();
    }
}
