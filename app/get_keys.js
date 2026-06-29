const { ethers } = require('ethers');
const { ClobClient } = require('@polymarket/clob-client-v2');

async function main() {
    try {
        const privateKey = '0xf5d4b038d9387c29bd10200fcc1b4235e42455384fb5afe34e87b40c071b03ac';
        const signer = new ethers.Wallet(privateKey);
        const host = 'https://clob.polymarket.com';
        const chainId = 137; // Polygon Mainnet
        
        const client = new ClobClient({
            host: host,
            chain: chainId,
            signer: signer
        });
        const creds = await client.createOrDeriveApiKey();
        
        console.log('API_KEY=' + creds.key);
        console.log('API_SECRET=' + creds.secret);
        console.log('API_PASSPHRASE=' + creds.passphrase);
    } catch (e) {
        console.error(e);
    }
}
main();
