const publicKeyPem = process.env.CERTIFICATE_PUBLIC_KEY || '';
// eslint-disable-next-line max-len
const privateKeyPem = process.env.CERTIFICATE_PRIVATE_KEY || '';
const qrType = process.env.QR_TYPE || 'W3C-VC';
<<<<<<< HEAD
const certDomainUrl = process.env.CERTIFICATE_DOMAIN_URL || "https://registration.uphrh.in:8082";
=======
const certDomainUrl = process.env.CERTIFICATE_DOMAIN_URL || "https://registration.uphrh.in/claim-ms";
>>>>>>> 7356437db9aaf3ae8bb93f5483599cf3b363ea6d
const smsAuthKey = "";
module.exports = {
  publicKeyPem,
  privateKeyPem,
  smsAuthKey,
  qrType,
  certDomainUrl
};
<<<<<<< HEAD

 

/*
// openssl genrsa -out key.pem; cat key.pem;
// openssl rsa -in key.pem -pubout -out pubkey.pem;
// cat pubkey.pem; rm key.pem pubkey.pem
*/
=======
>>>>>>> 7356437db9aaf3ae8bb93f5483599cf3b363ea6d
