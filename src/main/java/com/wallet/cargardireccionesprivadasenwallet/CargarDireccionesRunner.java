package com.wallet.cargardireccionesprivadasenwallet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.cargardireccionesprivadasenwallet.vo.DescriptorEnum;
import com.wallet.cargardireccionesprivadasenwallet.vo.GetDescriptorInfo;
import com.wallet.cargardireccionesprivadasenwallet.vo.PrivatePublicKey;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.KeyChainGroup;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.mail.internet.AddressException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CargarDireccionesRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {

        NetworkParameters params = MainNetParams.get();

        // Load file from resources
        File file = new ClassPathResource("seeds_cargar.txt").getFile();

        // Read file into a List
        List<String> mnemonicWordList = Files.readAllLines(file.toPath());

        for(String mnemonicWord: mnemonicWordList) {

            System.out.println(mnemonicWord);
            
            // Creamos la wallet
            crearWallet(mnemonicWord);

            List<PrivatePublicKey> listLegacyP2PKH = generateLegacyP2PKH(mnemonicWord, params, 50);
            cargarDireccionesPrivadasEnWallet(listLegacyP2PKH.stream().map(PrivatePublicKey::getPrivateKey).toList(), mnemonicWord, DescriptorEnum.PKH);

            List<PrivatePublicKey> listLegacyNestedSegwit = generateNestedSegwit(mnemonicWord, params, 50);
            cargarDireccionesPrivadasEnWallet(listLegacyNestedSegwit.stream().map(PrivatePublicKey::getPrivateKey).toList(), mnemonicWord, DescriptorEnum.SH_WPKH);

            List<PrivatePublicKey> listLegacyNativeSegwit = generateNativeSegwit(mnemonicWord, params, 50);
            cargarDireccionesPrivadasEnWallet(listLegacyNativeSegwit.stream().map(PrivatePublicKey::getPrivateKey).toList(), mnemonicWord, DescriptorEnum.WPKH);

            // Reescaneamos la wallet
            rescanWallet(mnemonicWord);
        }
    }

    private void rescanWallet(String mnemonicWord) throws IOException, InterruptedException {

        String command = "D:\\bitcoin-27.0-win64\\bitcoin-27.0\\bin\\bitcoin-cli.exe -rpcconnect=192.168.1.191 -rpcport=8332 -rpcuser=bitcoin -rpcpassword=123456 -rpcwallet=\"" + mnemonicWord + "\" rescanblockchain";
        Process exec = Runtime.getRuntime().exec(command);


        // Wait for the process to complete within 5 seconds
        if (exec.waitFor(8, TimeUnit.HOURS)) {
            exec.destroy(); // Terminate the process if timeout occurs
            System.out.println("Timeout occurred, process terminated.");
        } else {
            System.out.println("Process completed successfully.");
        }

        try (InputStream is = exec.getErrorStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr);) {

            String line;
            StringBuilder texto = new StringBuilder();
            while ((line = br.readLine()) != null) {
                texto.append(line);
            }

            if(!texto.isEmpty()) {
                System.out.println("Error al rescan la wallet " + mnemonicWord);
                System.out.println("El error es  " + texto);
                throw new RuntimeException("Error al rescan la wallet " + mnemonicWord);
            }
        }
    }

    private void crearWallet(String mnemonicWord) throws IOException {

        String command = "D:\\bitcoin-27.0-win64\\bitcoin-27.0\\bin\\bitcoin-cli.exe -rpcconnect=192.168.1.191 -rpcport=8332 -rpcuser=bitcoin -rpcpassword=123456 createwallet \"" + mnemonicWord + "\"";
        Process exec = Runtime.getRuntime().exec(command);

        try (InputStream is = exec.getErrorStream();
             InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr);) {

            String line;
            StringBuilder texto = new StringBuilder();
            while ((line = br.readLine()) != null) {
                texto.append(line);
            }

            if(!texto.isEmpty()) {
                System.out.println("Error al crear la wallet " + mnemonicWord);
                System.out.println("El error es  " + texto);
                throw new RuntimeException("Error al crear la wallet " + mnemonicWord);
            }
        }
    }

    private List<PrivatePublicKey> generateLegacyP2PKH(String mnemonicWord, NetworkParameters params, int numberOfAddress) throws IOException, AddressException {

        List<PrivatePublicKey> keys = new ArrayList<>();

        // Generate a mnemonic word list (BIP39)
        List<String> mnemonicWords = Arrays.stream(mnemonicWord.split(" ")).toList();

        // Convert mnemonic to seed
        DeterministicSeed seed = new DeterministicSeed(mnemonicWords, null, "", System.currentTimeMillis());
        DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(seed).build();

        // Generate 10 addresses
        for (int i = 0; i < numberOfAddress; i++) {
            List<ChildNumber> derivationPath = List.of(
                    new ChildNumber(44, true), // BIP44
                    new ChildNumber(0, true),  // Coin type (0 for Bitcoin)
                    new ChildNumber(0, true),  // Account index
                    new ChildNumber(0, false), // External chain (receiving addresses)
                    new ChildNumber(i, false)  // First address index
            );

            DeterministicKey key = keyChain.getKeyByPath(derivationPath, true);

            Address publicKey = LegacyAddress.fromPubKeyHash(params, key.getPubKeyHash());
            String privateKeyAsWiF = key.getPrivateKeyAsWiF(params);

            keys.add(PrivatePublicKey.builder().privateKey(privateKeyAsWiF).publicKey(publicKey.toString()).build());
        }

        return keys;
    }

    private List<PrivatePublicKey> generateNestedSegwit(String mnemonicWord, NetworkParameters params, int numberOfAddress) {

        List<PrivatePublicKey> keys = new ArrayList<>();

        // Generate a mnemonic word list (BIP39)
        List<String> mnemonicWords = Arrays.stream(mnemonicWord.split(" ")).toList();

        // Convert mnemonic to seed
        DeterministicSeed seed = new DeterministicSeed(mnemonicWords, null, "", System.currentTimeMillis());
        DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(seed).build();

        // Generate 10 addresses
        for (int i = 0; i < numberOfAddress; i++) {
            List<ChildNumber> derivationPath = List.of(
                    new ChildNumber(49, true), // BIP44
                    new ChildNumber(0, true),  // Coin type (0 for Bitcoin)
                    new ChildNumber(0, true),  // Account index
                    new ChildNumber(0, false), // External chain (receiving addresses)
                    new ChildNumber(i, false)  // First address index
            );

            DeterministicKey key = keyChain.getKeyByPath(derivationPath, true);

            // Create a P2WPKH script (SegWit)
            Script segwitScript = ScriptBuilder.createP2WPKHOutputScript(key);

            // Wrap the SegWit script inside a P2SH script
            Script p2shScript = ScriptBuilder.createP2SHOutputScript(segwitScript);

            // Generate the P2SH address manually
            Address publicKey = LegacyAddress.fromScriptHash(params, p2shScript.getPubKeyHash());
            String privateKeyAsWiF = key.getPrivateKeyAsWiF(params);

            keys.add(PrivatePublicKey.builder().privateKey(privateKeyAsWiF).publicKey(publicKey.toString()).build());
        }

        return keys;
    }

    private List<PrivatePublicKey> generateNativeSegwit(String mnemonicWord, NetworkParameters params, int numberOfAddress) {

        List<PrivatePublicKey> keys = new ArrayList<>();

        // Generate a mnemonic word list (BIP39)
        List<String> mnemonicWords = Arrays.stream(mnemonicWord.split(" ")).toList();

        // Generate seed from mnemonic
        DeterministicSeed seed = new DeterministicSeed(mnemonicWords, null, "", System.currentTimeMillis());

        // Create KeyChainGroup with P2WPKH script type
        KeyChainGroup keyChainGroup = KeyChainGroup.builder(params)
                .fromSeed(seed, org.bitcoinj.script.Script.ScriptType.P2WPKH)
                .build();

        // Generate 10 addresses
        for (int i = 0; i < numberOfAddress; i++) {
            List<ChildNumber> derivationPath =
                    List.of(new ChildNumber(84, true),
                            new ChildNumber(0, true),
                            new ChildNumber(0, true),
                            new ChildNumber(0, false), // External chain (receiving addresses)
                            new ChildNumber(i, false));

            // Get the first key
            var key = keyChainGroup.getActiveKeyChain().getKeyByPath(derivationPath, true);

            // Generate P2WPKH address
            SegwitAddress publicKey = SegwitAddress.fromKey(params, key);
            String privateKeyAsWiF = key.getPrivateKeyAsWiF(params);

            keys.add(PrivatePublicKey.builder().privateKey(privateKeyAsWiF).publicKey(publicKey.toString()).build());

        }

        return keys;
    }

    private void cargarDireccionesPrivadasEnWallet(List<String> privateKeyAsWiFList, String mnemonic, DescriptorEnum descriptorEnum) throws IOException {

        JsonFactory factory = JsonFactory.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .build();

        ObjectMapper objectMapper = new ObjectMapper(factory);

        for(String privateKey: privateKeyAsWiFList) {

            String commandChecksum = "D:\\bitcoin-27.0-win64\\bitcoin-27.0\\bin\\bitcoin-cli.exe -rpcconnect=192.168.1.191 -rpcport=8332 -rpcuser=bitcoin -rpcpassword=123456 getdescriptorinfo \"" + descriptorEnum.getDescriptor() + privateKey + descriptorEnum.getClose() + "\"";

            Process proc = Runtime.getRuntime().exec(commandChecksum);

            GetDescriptorInfo getDescriptorInfo = null;

            try (InputStream is = proc.getInputStream();
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr);) {

                String line;
                StringBuilder texto = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    texto.append(line);
                }

                getDescriptorInfo = objectMapper.readValue(texto.toString(), GetDescriptorInfo.class);
            }

            // \\\" Para Linux
            // \" Para Windows
            String command = "D:\\bitcoin-27.0-win64\\bitcoin-27.0\\bin\\bitcoin-cli.exe -rpcwallet=\"" + mnemonic + "\" -rpcconnect=192.168.1.191 -rpcport=8332 -rpcuser=bitcoin -rpcpassword=123456 importdescriptors "
                    + "\"[{\\\"desc\\\": \\\"" + descriptorEnum.getDescriptor() + privateKey + descriptorEnum.getClose() + "#" + getDescriptorInfo.getChecksum() + "\\\", \\\"timestamp\\\": \\\"now\\\"}]\"";


            Process exec = Runtime.getRuntime().exec(command);

            try (InputStream is = exec.getErrorStream();
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader br = new BufferedReader(isr);) {

                String line;
                StringBuilder texto = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    texto.append(line);
                }

                if(!texto.isEmpty()) {
                    System.out.println("Error al procesar la wallet " + mnemonic);
                    System.out.println("El error es  " + texto);
                    throw new RuntimeException("Error al procesar la wallet " + mnemonic);
                }
            }
        }
    }
}
