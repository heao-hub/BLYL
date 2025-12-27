package com.xiaozhang;


import java.util.List;

public class Main {
    public static void main(String[] args) {
        String input1 = "for(i = 1;i <= 10;i = i + 1){ if(a<1){a = 2;if(b > 1){b = 23;} else{b = 30;}} else {a = 0;} }"; // if嵌套

        String input2 = "for(i = 1;i <= 10; i = i + 1){ for(j = 0 ;j < i;j = j + 1){a = j + 1;}}"; // 嵌套
        String input3E = "for(@i = 1;i <= 10; i = i + 1){ }"; // 词法错误
        String input4E = "for(i = 1;i <= 10; i = i + 1){ if(b > 1){b = 23 else{b = 30;}}"; // 语法错误


        String input = input2;
        System.out.println("Input: " + input);

        // 词法分析
        Lexer lexer = new Lexer(input);
        List<Lexer.Token> tokens = lexer.tokenize();

        System.out.println("Tokens:");
        for (Lexer.Token t : tokens) System.out.println(t + "  ");
        System.out.println("\n");

        // 构造文法和 LR(1) 解析器
        Grammar G = new Grammar();
        LR1Parser parser = new LR1Parser(G);
        parser.buildStatesAndTables();

        parser.printStates();
        parser.printTables();

        // 进行解析与三地址码生成
      LR1Parser.ParseResult res = parser.parseAndGenerate(tokens, true);

        if (!res.success) {
            System.out.println("Parse failed:\n" + res.message);
        } else {
            int count = 1;
            System.out.println("Parse succeeded. Generated three-address code:");
            for (String line : res.code) System.out.println((count++)+":  " + line);
            System.out.println("\n(Trace and parser messages)\n" + res.message);
        }
    }
}
