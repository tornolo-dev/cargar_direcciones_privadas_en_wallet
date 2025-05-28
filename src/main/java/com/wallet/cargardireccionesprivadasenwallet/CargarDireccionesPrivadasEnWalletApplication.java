package com.wallet.cargardireccionesprivadasenwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.wallet.cargardireccionesprivadasenwallet"})
public class CargarDireccionesPrivadasEnWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(CargarDireccionesPrivadasEnWalletApplication.class, args);
    }

}
