package com.xiaozhang;

import java.util.ArrayList;
import java.util.List;

public class TACGenerator {

    private List<String> code = new ArrayList<>();
    private int tempCount = 0;
    private int labelCount = 0;

    // 生成新的临时变量
    public String newTemp() {
        return "t" + (++tempCount);
    }

    // 生成新的标号
    public String newLabel() {
        return "L" + (++labelCount);
    }

    // 生成一条三地址码
    public void emit(String s) {
        code.add(s);
    }

    // 获取所有三地址码
    public List<String> getCode() {
        return code;
    }

    // 输出三地址码
    public void print() {
        for (String s : code) {
            System.out.println(s);
        }
    }
}
