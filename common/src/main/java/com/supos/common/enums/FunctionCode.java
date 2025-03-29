package com.supos.common.enums;

/**
 * FC1->Coil
 * FC2->Input
 * FC3->HoldingRegister
 * FC4->InputRegister
 */
public enum FunctionCode {

    Coil(1),

    Input(2),

    HoldingRegister(3),

    InputRegister(4);


    private int code;

    FunctionCode(int code) {
        this.code = code;
    }

    public static int getCodeByName(String name) {
        for (FunctionCode fc : FunctionCode.values()) {
            if (fc.name().equalsIgnoreCase(name)) {
                return fc.code;
            }
        }
        return 3;
    }
}
