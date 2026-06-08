## 1.0.0 (2026-06-08)


### Features

* ajout du cd ([d90f34a](https://github.com/Yorobro/E-JDR-Frontend/commit/d90f34aaaece8c6a3f138d536323b689bd30e6bd))
* ajout du système de version ([a297282](https://github.com/Yorobro/E-JDR-Frontend/commit/a2972829e80d43fc8da491492003635814e33867))
* ajout gestion de version plus détailler ([eadf401](https://github.com/Yorobro/E-JDR-Frontend/commit/eadf401171e81f4fa98ce4f89100a7661e161911))
* **application:** add auth abstractions (repository, service, use case interfaces) ([96dbe35](https://github.com/Yorobro/E-JDR-Frontend/commit/96dbe3554066ca58e19eac33df6e2e6faaa4f786))
* **application:** add auth use cases (login, register, restore, logout) with tests ([ad1d402](https://github.com/Yorobro/E-JDR-Frontend/commit/ad1d402efc5fd1673cd4cafaac2170108a0f6dae))
* **application:** add DefaultSessionService with tests ([54b593a](https://github.com/Yorobro/E-JDR-Frontend/commit/54b593a7862c491046d447387767523f289f4b24))
* **di:** wire infrastructure and application Koin modules ([b5e57a2](https://github.com/Yorobro/E-JDR-Frontend/commit/b5e57a2fa8a6883458fde8103ef384bb259df161))
* **domain:** add Result, DomainError, AuthError and pure auth entities ([b03d171](https://github.com/Yorobro/E-JDR-Frontend/commit/b03d171faff980e452a49b4dfe943a10aabcc46a))
* **infrastructure:** add AppConfig (base URL, data dir, logging flag) ([1564e75](https://github.com/Yorobro/E-JDR-Frontend/commit/1564e75afc0c4a42f07660397ab1f58dbb75a2e2))
* **infrastructure:** add auth DTOs, AuthHttpMapper and Ktor client factory ([09add69](https://github.com/Yorobro/E-JDR-Frontend/commit/09add6950289bb42d986a121c5af215d7c2c5baa))
* **infrastructure:** add AuthHttpRepository against /auth endpoints ([b425f1d](https://github.com/Yorobro/E-JDR-Frontend/commit/b425f1d634cf71146483984d8a1c33728e148b00))
* **infrastructure:** add KeyStore-backed AES-GCM cookie cipher with tests ([5649719](https://github.com/Yorobro/E-JDR-Frontend/commit/564971946bea88ef7df6e514e58a69cb5a58efd6))
* **infrastructure:** add SecureCookiesStorage persisting encrypted refresh_token ([995eab3](https://github.com/Yorobro/E-JDR-Frontend/commit/995eab33cc368235bca2da72e6b1f61dda8a211c))
* **presentation:** add AppNumberField with tested numeric filtering ([2065b26](https://github.com/Yorobro/E-JDR-Frontend/commit/2065b269c498ccdd24500b1ff1b38d6c0ecf2196))
* **presentation:** add AppPasswordField with visibility toggle ([b7cfff7](https://github.com/Yorobro/E-JDR-Frontend/commit/b7cfff75759895d99fab1a6b1a6956b85d4e1c94))
* **presentation:** add AppText atom ([a931c57](https://github.com/Yorobro/E-JDR-Frontend/commit/a931c57ec7abab0f81f032b384ef864a9897099a))
* **presentation:** add design system theme (colors, typography, dimens) ([67a03ec](https://github.com/Yorobro/E-JDR-Frontend/commit/67a03ec091b24b1be230325fcea717546917fda0))
* **presentation:** add dumb LoginForm and RegisterForm components ([b642f71](https://github.com/Yorobro/E-JDR-Frontend/commit/b642f7112cc7a32790abd0b7d1dd17b5537efce5))
* **presentation:** add LabeledField and FieldGroup molecules ([bb87f98](https://github.com/Yorobro/E-JDR-Frontend/commit/bb87f98be798742185f1af186deb558bcaabdcd5))
* **presentation:** add navigation, app root with auto-login and wire main ([bda11e6](https://github.com/Yorobro/E-JDR-Frontend/commit/bda11e62415eb60a6cc4a9328c0f88461aee9014))
* **presentation:** add remaining design system atoms ([716b6fb](https://github.com/Yorobro/E-JDR-Frontend/commit/716b6fb4e8acfcef0d2c255110b58140c6684a41))
* **presentation:** add shared atomic-design components (button, textfield, error) ([2f07da5](https://github.com/Yorobro/E-JDR-Frontend/commit/2f07da593e00e089c17bd03fb7dbe19e223d9867))
* **presentation:** add smart LoginPage and RegisterPage calling use cases ([9e72d0c](https://github.com/Yorobro/E-JDR-Frontend/commit/9e72d0c780c5b041a062b8cc0f7ab2d955af6e3a))
* **presentation:** add top bar and app scaffold ([120f924](https://github.com/Yorobro/E-JDR-Frontend/commit/120f924b7e90c7e74b2fdd0fc2d7e1c40c265dc9))
* **presentation:** add user page ([fb8adc2](https://github.com/Yorobro/E-JDR-Frontend/commit/fb8adc20efcba5087b4f9084d0b6a2ffa041b840))
* **presentation:** rework AppButton with 5 themed variants ([b689b3b](https://github.com/Yorobro/E-JDR-Frontend/commit/b689b3bbbf5eddbc7732444dd9a64dda123794ee))
* **presentation:** rework AppTextField as themed outlined field ([904b3f4](https://github.com/Yorobro/E-JDR-Frontend/commit/904b3f485db0b102ee175db9914a97c9a428c45f))
* **security,quality:** durcissement KeyStore, contrat d'erreur, detekt/Kover + résilience ([f8c8af6](https://github.com/Yorobro/E-JDR-Frontend/commit/f8c8af6d68f92eb02e5c5fe77220beca1c0fa777))


### Bug Fixes

* cicd ([42dca68](https://github.com/Yorobro/E-JDR-Frontend/commit/42dca6807acdfcfa1b29fd8dd4b9535ba9f37f05))
* deploy ([a68bd3c](https://github.com/Yorobro/E-JDR-Frontend/commit/a68bd3c04e1d58cea98405dbc170b42508af8229))
* deploy certain files ([64b12d3](https://github.com/Yorobro/E-JDR-Frontend/commit/64b12d3c8675afd5d80692889c67d1889b5e2821))
* **github-script:** remove duplicate 'core' declaration ([cf054c7](https://github.com/Yorobro/E-JDR-Frontend/commit/cf054c7ce8649d132eeb84241489aef006f42144))
* **github-script:** use context.payload.release and guard missing tag ([4869520](https://github.com/Yorobro/E-JDR-Frontend/commit/48695201a1556762871f6c92fa0e5a97785a3a46))
* **presentation:** correct expected value in numeric filter test ([aa3becd](https://github.com/Yorobro/E-JDR-Frontend/commit/aa3becdd5d41dd1439fc826d18de34720b2753bc))

## 1.0.0 (2026-06-08)


### Features

* ajout du cd ([d90f34a](https://github.com/Yorobro/E-JDR-Frontend/commit/d90f34aaaece8c6a3f138d536323b689bd30e6bd))
* ajout du système de version ([a297282](https://github.com/Yorobro/E-JDR-Frontend/commit/a2972829e80d43fc8da491492003635814e33867))
* ajout gestion de version plus détailler ([eadf401](https://github.com/Yorobro/E-JDR-Frontend/commit/eadf401171e81f4fa98ce4f89100a7661e161911))
* **application:** add auth abstractions (repository, service, use case interfaces) ([96dbe35](https://github.com/Yorobro/E-JDR-Frontend/commit/96dbe3554066ca58e19eac33df6e2e6faaa4f786))
* **application:** add auth use cases (login, register, restore, logout) with tests ([ad1d402](https://github.com/Yorobro/E-JDR-Frontend/commit/ad1d402efc5fd1673cd4cafaac2170108a0f6dae))
* **application:** add DefaultSessionService with tests ([54b593a](https://github.com/Yorobro/E-JDR-Frontend/commit/54b593a7862c491046d447387767523f289f4b24))
* **di:** wire infrastructure and application Koin modules ([b5e57a2](https://github.com/Yorobro/E-JDR-Frontend/commit/b5e57a2fa8a6883458fde8103ef384bb259df161))
* **domain:** add Result, DomainError, AuthError and pure auth entities ([b03d171](https://github.com/Yorobro/E-JDR-Frontend/commit/b03d171faff980e452a49b4dfe943a10aabcc46a))
* **infrastructure:** add AppConfig (base URL, data dir, logging flag) ([1564e75](https://github.com/Yorobro/E-JDR-Frontend/commit/1564e75afc0c4a42f07660397ab1f58dbb75a2e2))
* **infrastructure:** add auth DTOs, AuthHttpMapper and Ktor client factory ([09add69](https://github.com/Yorobro/E-JDR-Frontend/commit/09add6950289bb42d986a121c5af215d7c2c5baa))
* **infrastructure:** add AuthHttpRepository against /auth endpoints ([b425f1d](https://github.com/Yorobro/E-JDR-Frontend/commit/b425f1d634cf71146483984d8a1c33728e148b00))
* **infrastructure:** add KeyStore-backed AES-GCM cookie cipher with tests ([5649719](https://github.com/Yorobro/E-JDR-Frontend/commit/564971946bea88ef7df6e514e58a69cb5a58efd6))
* **infrastructure:** add SecureCookiesStorage persisting encrypted refresh_token ([995eab3](https://github.com/Yorobro/E-JDR-Frontend/commit/995eab33cc368235bca2da72e6b1f61dda8a211c))
* **presentation:** add AppNumberField with tested numeric filtering ([2065b26](https://github.com/Yorobro/E-JDR-Frontend/commit/2065b269c498ccdd24500b1ff1b38d6c0ecf2196))
* **presentation:** add AppPasswordField with visibility toggle ([b7cfff7](https://github.com/Yorobro/E-JDR-Frontend/commit/b7cfff75759895d99fab1a6b1a6956b85d4e1c94))
* **presentation:** add AppText atom ([a931c57](https://github.com/Yorobro/E-JDR-Frontend/commit/a931c57ec7abab0f81f032b384ef864a9897099a))
* **presentation:** add design system theme (colors, typography, dimens) ([67a03ec](https://github.com/Yorobro/E-JDR-Frontend/commit/67a03ec091b24b1be230325fcea717546917fda0))
* **presentation:** add dumb LoginForm and RegisterForm components ([b642f71](https://github.com/Yorobro/E-JDR-Frontend/commit/b642f7112cc7a32790abd0b7d1dd17b5537efce5))
* **presentation:** add LabeledField and FieldGroup molecules ([bb87f98](https://github.com/Yorobro/E-JDR-Frontend/commit/bb87f98be798742185f1af186deb558bcaabdcd5))
* **presentation:** add navigation, app root with auto-login and wire main ([bda11e6](https://github.com/Yorobro/E-JDR-Frontend/commit/bda11e62415eb60a6cc4a9328c0f88461aee9014))
* **presentation:** add remaining design system atoms ([716b6fb](https://github.com/Yorobro/E-JDR-Frontend/commit/716b6fb4e8acfcef0d2c255110b58140c6684a41))
* **presentation:** add shared atomic-design components (button, textfield, error) ([2f07da5](https://github.com/Yorobro/E-JDR-Frontend/commit/2f07da593e00e089c17bd03fb7dbe19e223d9867))
* **presentation:** add smart LoginPage and RegisterPage calling use cases ([9e72d0c](https://github.com/Yorobro/E-JDR-Frontend/commit/9e72d0c780c5b041a062b8cc0f7ab2d955af6e3a))
* **presentation:** add top bar and app scaffold ([120f924](https://github.com/Yorobro/E-JDR-Frontend/commit/120f924b7e90c7e74b2fdd0fc2d7e1c40c265dc9))
* **presentation:** add user page ([fb8adc2](https://github.com/Yorobro/E-JDR-Frontend/commit/fb8adc20efcba5087b4f9084d0b6a2ffa041b840))
* **presentation:** rework AppButton with 5 themed variants ([b689b3b](https://github.com/Yorobro/E-JDR-Frontend/commit/b689b3bbbf5eddbc7732444dd9a64dda123794ee))
* **presentation:** rework AppTextField as themed outlined field ([904b3f4](https://github.com/Yorobro/E-JDR-Frontend/commit/904b3f485db0b102ee175db9914a97c9a428c45f))
* **security,quality:** durcissement KeyStore, contrat d'erreur, detekt/Kover + résilience ([f8c8af6](https://github.com/Yorobro/E-JDR-Frontend/commit/f8c8af6d68f92eb02e5c5fe77220beca1c0fa777))


### Bug Fixes

* cicd ([42dca68](https://github.com/Yorobro/E-JDR-Frontend/commit/42dca6807acdfcfa1b29fd8dd4b9535ba9f37f05))
* deploy ([a68bd3c](https://github.com/Yorobro/E-JDR-Frontend/commit/a68bd3c04e1d58cea98405dbc170b42508af8229))
* deploy certain files ([64b12d3](https://github.com/Yorobro/E-JDR-Frontend/commit/64b12d3c8675afd5d80692889c67d1889b5e2821))
* **github-script:** remove duplicate 'core' declaration ([cf054c7](https://github.com/Yorobro/E-JDR-Frontend/commit/cf054c7ce8649d132eeb84241489aef006f42144))
* **presentation:** correct expected value in numeric filter test ([aa3becd](https://github.com/Yorobro/E-JDR-Frontend/commit/aa3becdd5d41dd1439fc826d18de34720b2753bc))

## 1.0.0 (2026-06-08)


### Features

* ajout du cd ([d90f34a](https://github.com/Yorobro/E-JDR-Frontend/commit/d90f34aaaece8c6a3f138d536323b689bd30e6bd))
* ajout du système de version ([a297282](https://github.com/Yorobro/E-JDR-Frontend/commit/a2972829e80d43fc8da491492003635814e33867))
* ajout gestion de version plus détailler ([eadf401](https://github.com/Yorobro/E-JDR-Frontend/commit/eadf401171e81f4fa98ce4f89100a7661e161911))
* **application:** add auth abstractions (repository, service, use case interfaces) ([96dbe35](https://github.com/Yorobro/E-JDR-Frontend/commit/96dbe3554066ca58e19eac33df6e2e6faaa4f786))
* **application:** add auth use cases (login, register, restore, logout) with tests ([ad1d402](https://github.com/Yorobro/E-JDR-Frontend/commit/ad1d402efc5fd1673cd4cafaac2170108a0f6dae))
* **application:** add DefaultSessionService with tests ([54b593a](https://github.com/Yorobro/E-JDR-Frontend/commit/54b593a7862c491046d447387767523f289f4b24))
* **di:** wire infrastructure and application Koin modules ([b5e57a2](https://github.com/Yorobro/E-JDR-Frontend/commit/b5e57a2fa8a6883458fde8103ef384bb259df161))
* **domain:** add Result, DomainError, AuthError and pure auth entities ([b03d171](https://github.com/Yorobro/E-JDR-Frontend/commit/b03d171faff980e452a49b4dfe943a10aabcc46a))
* **infrastructure:** add AppConfig (base URL, data dir, logging flag) ([1564e75](https://github.com/Yorobro/E-JDR-Frontend/commit/1564e75afc0c4a42f07660397ab1f58dbb75a2e2))
* **infrastructure:** add auth DTOs, AuthHttpMapper and Ktor client factory ([09add69](https://github.com/Yorobro/E-JDR-Frontend/commit/09add6950289bb42d986a121c5af215d7c2c5baa))
* **infrastructure:** add AuthHttpRepository against /auth endpoints ([b425f1d](https://github.com/Yorobro/E-JDR-Frontend/commit/b425f1d634cf71146483984d8a1c33728e148b00))
* **infrastructure:** add KeyStore-backed AES-GCM cookie cipher with tests ([5649719](https://github.com/Yorobro/E-JDR-Frontend/commit/564971946bea88ef7df6e514e58a69cb5a58efd6))
* **infrastructure:** add SecureCookiesStorage persisting encrypted refresh_token ([995eab3](https://github.com/Yorobro/E-JDR-Frontend/commit/995eab33cc368235bca2da72e6b1f61dda8a211c))
* **presentation:** add AppNumberField with tested numeric filtering ([2065b26](https://github.com/Yorobro/E-JDR-Frontend/commit/2065b269c498ccdd24500b1ff1b38d6c0ecf2196))
* **presentation:** add AppPasswordField with visibility toggle ([b7cfff7](https://github.com/Yorobro/E-JDR-Frontend/commit/b7cfff75759895d99fab1a6b1a6956b85d4e1c94))
* **presentation:** add AppText atom ([a931c57](https://github.com/Yorobro/E-JDR-Frontend/commit/a931c57ec7abab0f81f032b384ef864a9897099a))
* **presentation:** add design system theme (colors, typography, dimens) ([67a03ec](https://github.com/Yorobro/E-JDR-Frontend/commit/67a03ec091b24b1be230325fcea717546917fda0))
* **presentation:** add dumb LoginForm and RegisterForm components ([b642f71](https://github.com/Yorobro/E-JDR-Frontend/commit/b642f7112cc7a32790abd0b7d1dd17b5537efce5))
* **presentation:** add LabeledField and FieldGroup molecules ([bb87f98](https://github.com/Yorobro/E-JDR-Frontend/commit/bb87f98be798742185f1af186deb558bcaabdcd5))
* **presentation:** add navigation, app root with auto-login and wire main ([bda11e6](https://github.com/Yorobro/E-JDR-Frontend/commit/bda11e62415eb60a6cc4a9328c0f88461aee9014))
* **presentation:** add remaining design system atoms ([716b6fb](https://github.com/Yorobro/E-JDR-Frontend/commit/716b6fb4e8acfcef0d2c255110b58140c6684a41))
* **presentation:** add shared atomic-design components (button, textfield, error) ([2f07da5](https://github.com/Yorobro/E-JDR-Frontend/commit/2f07da593e00e089c17bd03fb7dbe19e223d9867))
* **presentation:** add smart LoginPage and RegisterPage calling use cases ([9e72d0c](https://github.com/Yorobro/E-JDR-Frontend/commit/9e72d0c780c5b041a062b8cc0f7ab2d955af6e3a))
* **presentation:** add top bar and app scaffold ([120f924](https://github.com/Yorobro/E-JDR-Frontend/commit/120f924b7e90c7e74b2fdd0fc2d7e1c40c265dc9))
* **presentation:** add user page ([fb8adc2](https://github.com/Yorobro/E-JDR-Frontend/commit/fb8adc20efcba5087b4f9084d0b6a2ffa041b840))
* **presentation:** rework AppButton with 5 themed variants ([b689b3b](https://github.com/Yorobro/E-JDR-Frontend/commit/b689b3bbbf5eddbc7732444dd9a64dda123794ee))
* **presentation:** rework AppTextField as themed outlined field ([904b3f4](https://github.com/Yorobro/E-JDR-Frontend/commit/904b3f485db0b102ee175db9914a97c9a428c45f))
* **security,quality:** durcissement KeyStore, contrat d'erreur, detekt/Kover + résilience ([f8c8af6](https://github.com/Yorobro/E-JDR-Frontend/commit/f8c8af6d68f92eb02e5c5fe77220beca1c0fa777))


### Bug Fixes

* cicd ([42dca68](https://github.com/Yorobro/E-JDR-Frontend/commit/42dca6807acdfcfa1b29fd8dd4b9535ba9f37f05))
* deploy ([a68bd3c](https://github.com/Yorobro/E-JDR-Frontend/commit/a68bd3c04e1d58cea98405dbc170b42508af8229))
* deploy certain files ([64b12d3](https://github.com/Yorobro/E-JDR-Frontend/commit/64b12d3c8675afd5d80692889c67d1889b5e2821))
* **presentation:** correct expected value in numeric filter test ([aa3becd](https://github.com/Yorobro/E-JDR-Frontend/commit/aa3becdd5d41dd1439fc826d18de34720b2753bc))
