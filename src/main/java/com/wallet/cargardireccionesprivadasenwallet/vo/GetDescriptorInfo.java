package com.wallet.cargardireccionesprivadasenwallet.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDescriptorInfo {

    @JsonProperty("descriptor")
    private String descriptor;

    @JsonProperty("checksum")
    private String checksum;

    @JsonProperty("isrange")
    private boolean isRange;

    @JsonProperty("issolvable")
    private boolean isSolvable;

    @JsonProperty("hasprivatekeys")
    private boolean hasPrivateKeys;
}
