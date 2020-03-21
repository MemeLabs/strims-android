#!/bin/sh
gpg --quiet --batch --yes --decrypt --passphrase="tFeozZt8okDTajTLNoSmo+0xPUqUBAuesl/vGFLKFTw=" \
--output keystore.properties keystore.properties.gpg

gpg --quiet --batch --yes --decrypt --passphrase="hDiAbX4N8YR9Uzt1gEq66MIJLZsayA5SAjv31ME9oKQ=" \
--output ./app/app-sign-key.jks ./app/app-sign-key.jks.gpg

