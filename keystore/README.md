# keystore/

Holds the Play upload key. Both real files here are `.gitignore`'d (AC-1.11,
owner decision #6): `upload-keystore.jks` and `keystore.properties`. Only this
README and `keystore.properties.template` are tracked.

**Gitignoring a file inside the repo is not a backup.** It still lives on
exactly one disk. If that disk is lost with no other copy, Picklelog can never
be updated under the same Play listing again — a new `applicationId` and a new
store listing is the only remaining path. Keep a second copy somewhere else: a
password manager's file attachment or an encrypted drive. That is outside what
RFC-001 automates, and it is still worth doing today.
