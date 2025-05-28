package com.wallet.cargardireccionesprivadasenwallet.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PrivatePublicKey {

    private String privateKey;
    private String publicKey;
}
