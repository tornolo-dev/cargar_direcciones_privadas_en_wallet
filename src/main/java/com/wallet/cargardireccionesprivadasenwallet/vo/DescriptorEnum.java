package com.wallet.cargardireccionesprivadasenwallet.vo;

import lombok.Getter;

@Getter
public enum DescriptorEnum {

    PKH("pkh(", ")"),
    SH_WPKH("sh(wpkh(", "))"),
    WPKH("wpkh(", ")");

    private final String descriptor;
    private final String close;

    private DescriptorEnum(String descriptor, String close) {
        this.descriptor = descriptor;
        this.close = close;
    }

}
